package com.dcplanner.strategy;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;

import java.util.List;

/**
 * Strategy interface for itinerary optimization.
 * Different implementations represent different ways to
 * select and order attractions within a time window.
 */
public interface OptimizationStrategy {

    /**
     * Build a single day's list of time blocks from the available attractions.
     *
     * @param candidates   Filtered list of attractions eligible for inclusion
     * @param request      The original trip request (contains time window, preferences)
     * @param dijkstra     Dijkstra instance for computing Metro travel between stops
     * @param dayStartStation  The Metro station where this day begins
     * @return             Ordered list of time blocks for the day
     */
    List<Itinerary.TimeBlock> buildDaySchedule(
            List<Attraction> candidates,
            TripRequest request,
            Dijkstra dijkstra,
            String dayStartStation
    );
}
