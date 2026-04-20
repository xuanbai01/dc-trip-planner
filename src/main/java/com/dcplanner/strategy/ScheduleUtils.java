package com.dcplanner.strategy;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Shared helper methods used by both optimization strategies.
 */
public class ScheduleUtils {

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    public static final int WALKING_MINUTES_PER_STOP = 10; // flat walking estimate from Metro to attraction

    private ScheduleUtils() {}

    /**
     * Parse a "HH:mm" string to LocalTime.
     */
    public static LocalTime parseTime(String t) {
        return LocalTime.parse(t, TIME_FMT);
    }

    /**
     * Format a LocalTime to "HH:mm".
     */
    public static String formatTime(LocalTime t) {
        return t.format(TIME_FMT);
    }

    /**
     * Check whether an attraction is open during a given time window.
     * The attraction must be open before the proposed start and close after the proposed end.
     */
    public static boolean isOpen(Attraction attraction, String dayOfWeek, LocalTime proposedStart, LocalTime proposedEnd) {
        if (attraction.getOpeningHours() == null) return false;
        Attraction.OpeningHours hours = attraction.getOpeningHours().get(dayOfWeek.toLowerCase());
        if (hours == null) return false;
        String openStr = hours.getOpen();
        String closeStr = hours.getClose();
        if (openStr == null || closeStr == null) return false;
        if ("closed".equalsIgnoreCase(openStr)) return false;

        LocalTime open = parseTime(openStr);
        LocalTime close = parseTime(closeStr);

        return !proposedStart.isBefore(open) && !proposedEnd.isAfter(close);
    }

    /**
     * Build a TimeBlock and TravelInfo given travel result and timing.
     */
    public static Itinerary.TimeBlock buildTimeBlock(
            Attraction attraction,
            LocalTime arrivalTime,
            Dijkstra.RouteResult route
    ) {
        LocalTime departureTime = arrivalTime.plusMinutes(attraction.getEstimatedVisitDurationMinutes());

        Itinerary.TravelInfo travel = null;
        if (route != null && route.isFound()) {
            travel = new Itinerary.TravelInfo(
                    route.getStationPath(),
                    route.getTotalTravelTimeMinutes() + WALKING_MINUTES_PER_STOP,
                    route.getTotalFare()
            );
        }

        return new Itinerary.TimeBlock(
                formatTime(arrivalTime),
                formatTime(departureTime),
                attraction,
                travel
        );
    }

    /**
     * Compute travel time in minutes from current station to next station.
     * Returns 0 if it's the first stop (no travel needed).
     */
    public static int getTravelMinutes(Dijkstra dijkstra, String fromStation, String toStation) {
        if (fromStation == null || fromStation.equals(toStation)) return 0;
        Dijkstra.RouteResult result = dijkstra.findShortestPath(fromStation, toStation);
        return result.isFound() ? result.getTotalTravelTimeMinutes() + WALKING_MINUTES_PER_STOP : 999;
    }
}
