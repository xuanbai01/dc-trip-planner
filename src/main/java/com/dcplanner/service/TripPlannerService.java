package com.dcplanner.service;

import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;
import com.dcplanner.repository.AttractionRepository;
import com.dcplanner.strategy.BalancedOptimizationStrategy;
import com.dcplanner.strategy.MaximizeAttractionsStrategy;
import com.dcplanner.strategy.MinimizeTravelTimeStrategy;
import com.dcplanner.strategy.OptimizationStrategy;

import java.util.ArrayList;
import java.util.List;

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

        // Track which attractions have been used across days to avoid repeats
        List<Attraction> remainingCandidates = new ArrayList<>(candidates);

        for (int i = 1; i <= numDays; i++) {
            int dayOffset = i - 1;
            List<Itinerary.TimeBlock> blocks = strategy.buildDaySchedule(
                    remainingCandidates,
                    request,
                    routeService.getDijkstra(),
                    startStation,
                    dayOffset
            );

            days.add(new Itinerary.Day(i, blocks));

            // Remove used attractions from the pool for subsequent days
            blocks.forEach(b -> remainingCandidates.removeIf(a -> a.getId().equals(b.getAttraction().getId())));

            // Next day starts from last stop's station (or original start if no stops)
            if (!blocks.isEmpty()) {
                startStation = blocks.get(blocks.size() - 1).getAttraction().getNearestMetroStation();
            }
        }

        Itinerary itinerary = new Itinerary(days);

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
