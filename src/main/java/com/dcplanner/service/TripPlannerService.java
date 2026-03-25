package com.dcplanner.service;

import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;
import com.dcplanner.repository.AttractionRepository;
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
        validateRequest(request);

        // 1. Filter candidates based on user preferences
        List<Attraction> candidates = filterCandidates(request);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No attractions match your filters. Try relaxing your preferences.");
        }

        // 2. Select strategy
        OptimizationStrategy strategy = selectStrategy(request);

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

        return new Itinerary(days);
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

        return attractionRepository.findFiltered(types, maxBudget, accessibilityRequired);
    }

    private OptimizationStrategy selectStrategy(TripRequest request) {
        if (request.getPreferences() == null) {
            return new MinimizeTravelTimeStrategy();
        }

        String strategyName = request.getPreferences().getOptimizationStrategy();
        if ("maximize_attractions".equalsIgnoreCase(strategyName)) {
            return new MaximizeAttractionsStrategy();
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
