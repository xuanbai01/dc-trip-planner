package com.dcplanner.strategy;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;

import java.time.LocalTime;
import java.util.*;

/**
 * Strategy: Minimize Travel Time
 *
 * Greedy nearest-neighbor approach.
 * At each step, picks the unvisited attraction reachable in the least Metro travel time
 * that still fits in the remaining time window.
 */
public class MinimizeTravelTimeStrategy implements OptimizationStrategy {

    // Assume Saturday for simplicity; in a future iteration pass actual day
    private static final String DEFAULT_DAY = "saturday";

    @Override
    public List<Itinerary.TimeBlock> buildDaySchedule(
            List<Attraction> candidates,
            TripRequest request,
            Dijkstra dijkstra,
            String dayStartStation
    ) {
        List<Itinerary.TimeBlock> schedule = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        LocalTime currentTime = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getStart());
        LocalTime endTime = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getEnd());
        String currentStation = dayStartStation;

        while (true) {
            Attraction best = null;
            int bestTravelTime = Integer.MAX_VALUE;
            Dijkstra.RouteResult bestRoute = null;

            for (Attraction candidate : candidates) {
                if (visited.contains(candidate.getId())) continue;

                String destStation = candidate.getNearestMetroStation();
                int travelMinutes = ScheduleUtils.getTravelMinutes(dijkstra, currentStation, destStation);

                if (travelMinutes == 999) continue; // unreachable

                LocalTime arrivalTime = currentTime.plusMinutes(travelMinutes);
                LocalTime departureTime = arrivalTime.plusMinutes(candidate.getEstimatedVisitDurationMinutes());

                if (departureTime.isAfter(endTime)) continue; // doesn't fit

                if (!ScheduleUtils.isOpen(candidate, DEFAULT_DAY, arrivalTime, departureTime)) continue;

                if (travelMinutes < bestTravelTime) {
                    bestTravelTime = travelMinutes;
                    best = candidate;
                    bestRoute = dijkstra.findShortestPath(currentStation, destStation);
                }
            }

            if (best == null) break; // nothing more fits

            LocalTime arrivalTime = currentTime.plusMinutes(bestTravelTime);
            schedule.add(ScheduleUtils.buildTimeBlock(best, arrivalTime, bestRoute));

            currentTime = arrivalTime.plusMinutes(best.getEstimatedVisitDurationMinutes());
            currentStation = best.getNearestMetroStation();
            visited.add(best.getId());
        }

        return schedule;
    }
}
