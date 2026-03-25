package com.dcplanner.strategy;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;

import java.util.List;

/**
 * Strategy interface for itinerary optimization.
 */
public interface OptimizationStrategy {

    /**
     * Build a single day's list of time blocks from the available attractions.
     *
     * @param candidates       Filtered list of attractions eligible for inclusion
     * @param request          The original trip request
     * @param dijkstra         Dijkstra instance for Metro routing
     * @param dayStartStation  The Metro station where this day begins
     * @param dayOffset        0 = first day, 1 = second day (used for day-of-week resolution)
     */
    List<Itinerary.TimeBlock> buildDaySchedule(
            List<Attraction> candidates,
            TripRequest request,
            Dijkstra dijkstra,
            String dayStartStation,
            int dayOffset
    );
}
