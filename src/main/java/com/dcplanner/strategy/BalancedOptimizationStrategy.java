package com.dcplanner.strategy;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;

import java.util.ArrayList;
import java.util.List;

public class BalancedOptimizationStrategy implements OptimizationStrategy {

    private static final double ALPHA = 0.5;
    private static final double BETA = 0.5;

    private final OptimizationStrategy minimizeTravelStrategy = new MinimizeTravelTimeStrategy();
    private final OptimizationStrategy maximizeAttractionsStrategy = new MaximizeAttractionsStrategy();

    @Override
    public List<Itinerary.TimeBlock> buildDaySchedule(
            List<Attraction> candidates,
            TripRequest request,
            Dijkstra dijkstra,
            String dayStartStation,
            int dayOffset
    ) {
        List<Itinerary.TimeBlock> minimizeTravelSchedule = minimizeTravelStrategy.buildDaySchedule(
                new ArrayList<>(candidates),
                request,
                dijkstra,
                dayStartStation,
                dayOffset
        );

        List<Itinerary.TimeBlock> maximizeAttractionsSchedule = maximizeAttractionsStrategy.buildDaySchedule(
                new ArrayList<>(candidates),
                request,
                dijkstra,
                dayStartStation,
                dayOffset
        );

        double minimizeScore = scoreSchedule(minimizeTravelSchedule);
        double maximizeScore = scoreSchedule(maximizeAttractionsSchedule);

        return minimizeScore <= maximizeScore ? minimizeTravelSchedule : maximizeAttractionsSchedule;
    }

    private double scoreSchedule(List<Itinerary.TimeBlock> schedule) {
        int totalTravelTimeMinutes = 0;
        for (Itinerary.TimeBlock block : schedule) {
            if (block.getTravel() != null) {
                totalTravelTimeMinutes += block.getTravel().getWalkingMinutes();
            }
        }

        int totalAttractions = schedule.size();
        return ALPHA * totalTravelTimeMinutes + BETA * (-totalAttractions);
    }
}
