package com.dcplanner.strategy;

import com.dcplanner.model.TripRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Resolves the day of week string used for opening hours lookups.
 *
 * If the request contains a tripStartDate (YYYY-MM-DD), derive from that.
 * Otherwise default to Saturday — the most common use case for weekend trips.
 */
public class DateUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {}

    /**
     * Returns a lowercase day name for a given day offset from the trip start.
     * e.g. dayOffset=0 → "saturday", dayOffset=1 → "sunday"
     */
    public static String resolveDayOfWeek(TripRequest request, int dayOffset) {
        if (request.getTripStartDate() != null && !request.getTripStartDate().isBlank()) {
            try {
                LocalDate startDate = LocalDate.parse(request.getTripStartDate(), DATE_FMT);
                return startDate.plusDays(dayOffset)
                                .getDayOfWeek()
                                .name()
                                .toLowerCase();
            } catch (Exception e) {
                System.err.println("Invalid tripStartDate format: " + request.getTripStartDate() + ". Defaulting to saturday.");
            }
        }
        // Default: treat day 0 as Saturday, day 1 as Sunday
        return dayOffset == 0 ? "saturday" : "sunday";
    }
}
