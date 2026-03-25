package com.dcplanner.strategy;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;

import java.time.LocalTime;
import java.util.*;

/**
 * Strategy: Maximize Attractions
 *
 * Scores each candidate attraction by preference match and travel cost,
 * then greedily selects the highest-scoring attraction that fits in the time window.
 */
public class MaximizeAttractionsStrategy implements OptimizationStrategy {

    @Override
    public List<Itinerary.TimeBlock> buildDaySchedule(
            List<Attraction> candidates,
            TripRequest request,
            Dijkstra dijkstra,
            String dayStartStation,
            int dayOffset
    ) {
        String dayOfWeek = DateUtils.resolveDayOfWeek(request, dayOffset);
        List<Itinerary.TimeBlock> schedule = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        LocalTime currentTime = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getStart());
        LocalTime endTime = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getEnd());
        String currentStation = dayStartStation;

        List<String> preferredTypes = request.getPreferences() != null
                ? request.getPreferences().getTypes()
                : Collections.emptyList();

        while (true) {
            Attraction best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            Dijkstra.RouteResult bestRoute = null;
            LocalTime bestArrival = null;

            for (Attraction candidate : candidates) {
                if (visited.contains(candidate.getId())) continue;

                String destStation = candidate.getNearestMetroStation();
                int travelMinutes = ScheduleUtils.getTravelMinutes(dijkstra, currentStation, destStation);
                if (travelMinutes == 999) continue;

                LocalTime arrivalTime = currentTime.plusMinutes(travelMinutes);
                LocalTime departureTime = arrivalTime.plusMinutes(candidate.getEstimatedVisitDurationMinutes());

                if (departureTime.isAfter(endTime)) continue;
                if (!ScheduleUtils.isOpen(candidate, dayOfWeek, arrivalTime, departureTime)) continue;

                double score = scoreAttraction(candidate, preferredTypes, travelMinutes);

                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                    bestRoute = dijkstra.findShortestPath(currentStation, destStation);
                    bestArrival = arrivalTime;
                }
            }

            if (best == null) break;

            schedule.add(ScheduleUtils.buildTimeBlock(best, bestArrival, bestRoute));

            currentTime = bestArrival.plusMinutes(best.getEstimatedVisitDurationMinutes());
            currentStation = best.getNearestMetroStation();
            visited.add(best.getId());
        }

        return schedule;
    }

    private double scoreAttraction(Attraction attraction, List<String> preferredTypes, int travelMinutes) {
        double score = 0;

        if (preferredTypes != null && !preferredTypes.isEmpty()) {
            String typeName = attraction.getType().name().toLowerCase();
            if (preferredTypes.stream().anyMatch(t -> t.equalsIgnoreCase(typeName))) {
                score += 50;
            }
        }

        // Prefer shorter visit durations (fit more stops)
        score += Math.max(0, 120 - attraction.getEstimatedVisitDurationMinutes());

        // Penalize long travel
        score -= travelMinutes * 0.5;

        return score;
    }
}
