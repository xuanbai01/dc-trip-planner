package com.dcplanner.service;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;
import com.dcplanner.repository.AttractionRepository;
import com.dcplanner.strategy.BalancedOptimizationStrategy;
import com.dcplanner.strategy.DateUtils;
import com.dcplanner.strategy.MaximizeAttractionsStrategy;
import com.dcplanner.strategy.MinimizeTravelTimeStrategy;
import com.dcplanner.strategy.OptimizationStrategy;
import com.dcplanner.strategy.ScheduleUtils;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the full trip planning process.
 *
 * Responsibilities:
 * 1. Filter attractions based on user preferences
 * 2. Select the correct optimization strategy
 * 3. Build a day-by-day itinerary across 1-2 days
 */
public class TripPlannerService {

    private static final double BUDGET_SOFT_HEADROOM = 0.15;

    private final AttractionRepository attractionRepository;
    private final AttractionService attractionService;
    private final RouteService routeService;

    public TripPlannerService(
            AttractionRepository attractionRepository,
            AttractionService attractionService,
            RouteService routeService
    ) {
        this.attractionRepository = attractionRepository;
        this.attractionService = attractionService;
        this.routeService = routeService;
    }

    public Itinerary planTrip(TripRequest request) {
        long startTimeMs = System.currentTimeMillis();
        validateRequest(request);

        // 2. Select strategy first — balanced uses a wider candidate pool (see below)
        OptimizationStrategy strategy = selectStrategy(request);
        boolean isBalanced = strategy instanceof BalancedOptimizationStrategy;

        // 1. Filter candidates based on user preferences.
        // Balanced strategy receives the full pool (budget + accessibility only, no type
        // filter) so its efficiency score can naturally prefer type-matched attractions
        // while still considering off-type stops that cost almost nothing to include.
        // The other two strategies receive a type-filtered pool as before.
        List<Attraction> candidates = isBalanced
                ? filterCandidatesForBalanced(request)
                : filterCandidates(request);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No attractions match your filters. Try relaxing your preferences.");
        }

        // 3. Determine number of days
        int numDays = parseDays(request.getTripDuration());

        // 4. Build day-by-day schedule
        String startStation = request.getStartLocation().getMetroStation();
        List<Itinerary.Day> days = new ArrayList<>();
        List<String> mustIncludeWarnings = new ArrayList<>();

        // Track which attractions have been used across days to avoid repeats
        List<Attraction> remainingCandidates = new ArrayList<>(candidates);

        // Must-include attractions are user-picked anchors. They are pre-scheduled
        // on day 1 in the order the user provided, and the strategy fills the
        // remaining time. Must-includes bypass the candidate filters — if the user
        // picked it, honour it (subject only to time-window fit and opening hours).
        List<Attraction> mustIncludes = resolveMustIncludes(request);

        for (int i = 1; i <= numDays; i++) {
            int dayOffset = i - 1;
            List<Itinerary.TimeBlock> blocks;

            if (i == 1 && !mustIncludes.isEmpty()) {
                // Pre-remove anchors from the candidate pool so the gap-fill strategy
                // doesn't pick them before we get to the anchor step.
                for (Attraction a : mustIncludes) {
                    remainingCandidates.removeIf(x -> x.getId().equals(a.getId()));
                }

                MustIncludeResult pre = preScheduleMustIncludes(
                        mustIncludes, request, startStation, dayOffset,
                        strategy, remainingCandidates);

                mustIncludeWarnings.addAll(pre.warnings);

                // Fill the rest of day 1 from where the last anchor left us.
                TripRequest shadowRequest = pre.blocks.isEmpty()
                        ? request
                        : requestWithAdjustedStart(request, pre.endTime);
                String strategyStart = pre.blocks.isEmpty() ? startStation : pre.endStation;

                List<Itinerary.TimeBlock> strategyBlocks = strategy.buildDaySchedule(
                        remainingCandidates,
                        shadowRequest,
                        routeService.getDijkstra(),
                        strategyStart,
                        dayOffset
                );

                blocks = new ArrayList<>(pre.blocks);
                blocks.addAll(strategyBlocks);
            } else {
                blocks = strategy.buildDaySchedule(
                        remainingCandidates,
                        request,
                        routeService.getDijkstra(),
                        startStation,
                        dayOffset
                );
            }

            days.add(new Itinerary.Day(i, blocks));

            // Remove used attractions from the pool for subsequent days
            blocks.forEach(b -> remainingCandidates.removeIf(a -> a.getId().equals(b.getAttraction().getId())));

            // Next day starts from last stop's station (or original start if no stops)
            if (!blocks.isEmpty()) {
                startStation = blocks.get(blocks.size() - 1).getAttraction().getNearestMetroStation();
            }
        }

        Itinerary itinerary = new Itinerary(days);
        itinerary.getWarnings().addAll(mustIncludeWarnings);

        int totalAttractions = 0;
        int totalTravelTimeMinutes = 0;
        for (Itinerary.Day day : days) {
            totalAttractions += day.getTimeBlocks().size();
            for (Itinerary.TimeBlock block : day.getTimeBlocks()) {
                if (block.getTravel() != null) {
                    totalTravelTimeMinutes += block.getTravel().getWalkingMinutes();
                }
            }
        }
        itinerary.setTotalAttractions(totalAttractions);
        itinerary.setTotalTravelTimeMinutes(totalTravelTimeMinutes);

        applyBudgetRules(request, itinerary);

        long endTimeMs = System.currentTimeMillis();
        itinerary.setExecutionTimeMs(endTimeMs - startTimeMs);
        return itinerary;
    }

    private List<Attraction> filterCandidates(TripRequest request) {
        TripRequest.Preferences prefs = request.getPreferences();

        List<Attraction.Type> types = null;
        Double maxBudget = null;
        Boolean accessibilityRequired = null;

        if (prefs != null) {
            if (prefs.getTypes() != null && !prefs.getTypes().isEmpty()) {
                types = attractionService.parseTypes(prefs.getTypes());
            }
            maxBudget = prefs.getMaxBudget() > 0 ? prefs.getMaxBudget() : null;
            accessibilityRequired = prefs.isAccessibilityRequired() ? true : null;
        }

        Double maxFeeFilter = maxBudget == null ? null : maxBudget * (1.0 + BUDGET_SOFT_HEADROOM);
        return attractionRepository.findFiltered(types, maxFeeFilter, accessibilityRequired);
    }

    /**
     * Candidate pool for the balanced strategy: no type filter applied.
     *
     * Pre-filtering by type causes all candidates to share the same type-match
     * bonus, making the efficiency score collapse to minimizing travel time —
     * indistinguishable from MinimizeTravelTimeStrategy. Passing the full pool
     * lets the TYPE_MATCH_BONUS in BalancedOptimizationStrategy handle preference
     * ranking while naturally allowing high-efficiency off-type stops (e.g. a free
     * 30-min coffee shop at the same station between two museums) to be included.
     */
    private List<Attraction> filterCandidatesForBalanced(TripRequest request) {
        TripRequest.Preferences prefs = request.getPreferences();

        Double maxBudget = null;
        Boolean accessibilityRequired = null;

        if (prefs != null) {
            maxBudget = prefs.getMaxBudget() > 0 ? prefs.getMaxBudget() : null;
            accessibilityRequired = prefs.isAccessibilityRequired() ? true : null;
        }

        Double maxFeeFilter = maxBudget == null ? null : maxBudget * (1.0 + BUDGET_SOFT_HEADROOM);
        return attractionRepository.findFiltered(null, maxFeeFilter, accessibilityRequired);
    }

    private void applyBudgetRules(TripRequest request, Itinerary itinerary) {
        Double nominalBudget = nominalMaxBudget(request);
        if (nominalBudget == null) {
            return;
        }

        double total = itinerary.getTotalCost();
        double softCap = nominalBudget * (1.0 + BUDGET_SOFT_HEADROOM);
        if (total > softCap) {
            throw new IllegalArgumentException(String.format(
                    "Trip cost $%.2f exceeds budget (allowed up to $%.2f with flexibility).",
                    total, softCap));
        }
        if (total > nominalBudget) {
            itinerary.getWarnings().add(String.format("Budget exceeded by $%.2f", total - nominalBudget));
        }
    }

    private Double nominalMaxBudget(TripRequest request) {
        if (request.getPreferences() == null) {
            return null;
        }
        double b = request.getPreferences().getMaxBudget();
        return b > 0 ? b : null;
    }

    private OptimizationStrategy selectStrategy(TripRequest request) {
        if (request.getPreferences() == null) {
            return new MinimizeTravelTimeStrategy();
        }

        String strategyName = request.getPreferences().getOptimizationStrategy();
        if ("maximize_attractions".equalsIgnoreCase(strategyName)) {
            return new MaximizeAttractionsStrategy();
        }
        if ("balanced".equalsIgnoreCase(strategyName)) {
            return new BalancedOptimizationStrategy();
        }

        // Default
        return new MinimizeTravelTimeStrategy();
    }

    private int parseDays(String tripDuration) {
        if (tripDuration == null) return 1;
        return switch (tripDuration.toLowerCase()) {
            case "half_day" -> 1;
            case "2_days"   -> 2;
            default         -> 1; // "1_day" and anything unrecognized
        };
    }

    /**
     * Resolve the user's picked attraction IDs into Attraction objects.
     * Must-includes bypass type/budget/accessibility filters — the user explicitly
     * asked for these, so we honour the choice. Missing IDs are silently dropped
     * (validated at the request level, but defensive here in case).
     */
    private List<Attraction> resolveMustIncludes(TripRequest request) {
        if (request.getPreferences() == null) return Collections.emptyList();
        List<String> ids = request.getPreferences().getMustIncludeAttractionIds();
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        List<Attraction> resolved = new ArrayList<>();
        for (String id : ids) {
            Optional<Attraction> a = attractionRepository.findById(id);
            a.ifPresent(resolved::add);
        }
        return resolved;
    }

    // If the wait before an anchor is at least this long, we try to fill it with
    // the user's chosen strategy instead of leaving an idle gap.
    private static final long GAP_FILL_THRESHOLD_MINUTES = 45;

    /**
     * Pre-schedule must-include anchors in user-provided order.
     *
     * For each anchor:
     *   1. Compute the earliest feasible arrival — respecting opening hours.
     *      If the anchor opens later than we'd naturally arrive, the arrival is
     *      pushed forward to the opening time instead of failing the visit.
     *   2. If the resulting pre-anchor gap is large enough, run the user's
     *      strategy to fill it with other nearby attractions so the anchor
     *      isn't preceded by idle time.
     *   3. Visit the anchor (or skip with a warning if it's truly infeasible:
     *      cannot reach it, or cannot be open at any feasible time within the
     *      day).
     *
     * This method mutates {@code remainingCandidates} by removing any attractions
     * that the gap-fill strategy consumes. Anchors themselves must already be
     * removed by the caller.
     */
    private MustIncludeResult preScheduleMustIncludes(
            List<Attraction> mustIncludes,
            TripRequest request,
            String dayStartStation,
            int dayOffset,
            OptimizationStrategy strategy,
            List<Attraction> remainingCandidates
    ) {
        Dijkstra dijkstra = routeService.getDijkstra();
        String dayOfWeek = DateUtils.resolveDayOfWeek(request, dayOffset);
        LocalTime endTime = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getEnd());

        LocalTime currentTime = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getStart());
        String currentStation = dayStartStation;

        List<Itinerary.TimeBlock> blocks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (Attraction a : mustIncludes) {
            String destStation = a.getNearestMetroStation();

            int travel = ScheduleUtils.getTravelMinutes(dijkstra, currentStation, destStation);
            if (travel == 999) {
                warnings.add("Could not reach " + a.getName() + " by metro — skipped.");
                continue;
            }

            // Respect opening hours: if the anchor opens later than we'd arrive,
            // push arrival forward to the opening time instead of failing.
            LocalTime openTime = openingTimeFor(a, dayOfWeek);
            LocalTime arrival = currentTime.plusMinutes(travel);
            if (openTime != null && arrival.isBefore(openTime)) {
                arrival = openTime;
            }

            // If there's a meaningful gap before the anchor, fill it with the
            // chosen strategy so we don't sit idle. The strategy returns whatever
            // fits in [currentTime, arrival]; if nothing fits, we just wait.
            long gapMinutes = Duration.between(currentTime, arrival).toMinutes();
            if (gapMinutes >= GAP_FILL_THRESHOLD_MINUTES) {
                TripRequest gapRequest = requestWithWindow(request, currentTime, arrival);
                List<Itinerary.TimeBlock> gapBlocks = strategy.buildDaySchedule(
                        remainingCandidates, gapRequest, dijkstra, currentStation, dayOffset);

                if (!gapBlocks.isEmpty()) {
                    blocks.addAll(gapBlocks);
                    for (Itinerary.TimeBlock gb : gapBlocks) {
                        String id = gb.getAttraction().getId();
                        remainingCandidates.removeIf(x -> x.getId().equals(id));
                    }
                    // Strategy moved us: reset position from the last filled block
                    // and recompute the anchor's arrival time from there.
                    Itinerary.TimeBlock last = gapBlocks.get(gapBlocks.size() - 1);
                    currentStation = last.getAttraction().getNearestMetroStation();
                    currentTime = ScheduleUtils.parseTime(last.getEndTime());

                    travel = ScheduleUtils.getTravelMinutes(dijkstra, currentStation, destStation);
                    if (travel == 999) {
                        warnings.add("Could not reach " + a.getName() + " after gap fill — skipped.");
                        continue;
                    }
                    arrival = currentTime.plusMinutes(travel);
                    if (openTime != null && arrival.isBefore(openTime)) {
                        arrival = openTime;
                    }
                }
            }

            LocalTime departure = arrival.plusMinutes(a.getEstimatedVisitDurationMinutes());

            if (departure.isAfter(endTime)) {
                warnings.add("Not enough time for " + a.getName() + " — skipped.");
                continue;
            }
            if (!ScheduleUtils.isOpen(a, dayOfWeek, arrival, departure)) {
                warnings.add(a.getName() + " is closed during the available window — skipped.");
                continue;
            }

            Dijkstra.RouteResult route = travel > 0
                    ? dijkstra.findShortestPath(currentStation, destStation)
                    : null;
            blocks.add(ScheduleUtils.buildTimeBlock(a, arrival, route));

            currentTime = departure;
            currentStation = destStation;
        }

        return new MustIncludeResult(blocks, currentTime, currentStation, warnings);
    }

    /**
     * Returns the opening time of an attraction on the given day, or null if
     * the attraction is closed or has no hours for that day.
     */
    private LocalTime openingTimeFor(Attraction a, String dayOfWeek) {
        if (a.getOpeningHours() == null) return null;
        Attraction.OpeningHours hours = a.getOpeningHours().get(dayOfWeek.toLowerCase());
        if (hours == null || hours.getOpen() == null) return null;
        if ("closed".equalsIgnoreCase(hours.getOpen())) return null;
        return ScheduleUtils.parseTime(hours.getOpen());
    }

    /**
     * Shallow-copy the request with a specific start AND end time so the
     * strategy treats this as a sub-window of the day.
     */
    private TripRequest requestWithWindow(TripRequest original, LocalTime newStart, LocalTime newEnd) {
        TripRequest copy = new TripRequest();
        copy.setStartLocation(original.getStartLocation());
        copy.setTripDuration(original.getTripDuration());
        copy.setTripStartDate(original.getTripStartDate());
        copy.setPreferences(original.getPreferences());

        TripRequest.TimeWindow tw = new TripRequest.TimeWindow();
        tw.setStart(ScheduleUtils.formatTime(newStart));
        tw.setEnd(ScheduleUtils.formatTime(newEnd));
        copy.setAvailableTimePerDay(tw);

        return copy;
    }

    /**
     * Shallow-copy the request with a later start time so the strategy treats the
     * post-must-include moment as "day start". Everything else (end time, types,
     * strategy name, etc.) is preserved by reference.
     */
    private TripRequest requestWithAdjustedStart(TripRequest original, LocalTime newStart) {
        TripRequest copy = new TripRequest();
        copy.setStartLocation(original.getStartLocation());
        copy.setTripDuration(original.getTripDuration());
        copy.setTripStartDate(original.getTripStartDate());
        copy.setPreferences(original.getPreferences());

        TripRequest.TimeWindow tw = new TripRequest.TimeWindow();
        tw.setStart(ScheduleUtils.formatTime(newStart));
        tw.setEnd(original.getAvailableTimePerDay().getEnd());
        copy.setAvailableTimePerDay(tw);

        return copy;
    }

    private record MustIncludeResult(
            List<Itinerary.TimeBlock> blocks,
            LocalTime endTime,
            String endStation,
            List<String> warnings
    ) {}

    private void validateRequest(TripRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Trip request cannot be null.");
        }
        if (request.getStartLocation() == null || request.getStartLocation().getMetroStation() == null) {
            throw new IllegalArgumentException("Start Metro station is required.");
        }
        if (request.getAvailableTimePerDay() == null
                || request.getAvailableTimePerDay().getStart() == null
                || request.getAvailableTimePerDay().getEnd() == null) {
            throw new IllegalArgumentException("Available time window (start/end) is required.");
        }
    }
}
