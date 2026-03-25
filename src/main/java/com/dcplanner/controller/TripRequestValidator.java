package com.dcplanner.controller;

import com.dcplanner.model.Attraction;
import com.dcplanner.model.TripRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * Validates a TripRequest before it reaches the service layer.
 * Throws IllegalArgumentException with a descriptive message on failure.
 */
public class TripRequestValidator {

    private static final Set<String> VALID_DURATIONS = Set.of("half_day", "1_day", "2_days");
    private static final Set<String> VALID_STRATEGIES = Set.of("minimize_travel_time", "maximize_attractions");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TripRequestValidator() {}

    public static void validate(TripRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        // Start location
        if (request.getStartLocation() == null || request.getStartLocation().getMetroStation() == null
                || request.getStartLocation().getMetroStation().isBlank()) {
            throw new IllegalArgumentException("startLocation.metroStation is required");
        }

        // Trip duration
        if (request.getTripDuration() == null || request.getTripDuration().isBlank()) {
            throw new IllegalArgumentException("tripDuration is required. Valid values: " + VALID_DURATIONS);
        }
        if (!VALID_DURATIONS.contains(request.getTripDuration().toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid tripDuration '" + request.getTripDuration() + "'. Valid values: " + VALID_DURATIONS
            );
        }

        // Time window
        if (request.getAvailableTimePerDay() == null) {
            throw new IllegalArgumentException("availableTimePerDay is required");
        }
        LocalTime start = parseTime(request.getAvailableTimePerDay().getStart(), "availableTimePerDay.start");
        LocalTime end   = parseTime(request.getAvailableTimePerDay().getEnd(),   "availableTimePerDay.end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("availableTimePerDay.start must be before end");
        }

        // Optional: tripStartDate format
        if (request.getTripStartDate() != null && !request.getTripStartDate().isBlank()) {
            try {
                LocalDate.parse(request.getTripStartDate(), DATE_FMT);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid tripStartDate format. Expected YYYY-MM-DD, got: " + request.getTripStartDate());
            }
        }

        // Optional preferences
        if (request.getPreferences() != null) {
            validatePreferences(request.getPreferences());
        }
    }

    private static void validatePreferences(TripRequest.Preferences prefs) {
        // Validate attraction types
        if (prefs.getTypes() != null) {
            for (String type : prefs.getTypes()) {
                try {
                    Attraction.Type.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        "Invalid attraction type '" + type + "'. Valid values: MUSEUM, LANDMARK, COFFEE_SHOP, CINEMA"
                    );
                }
            }
        }

        // Validate budget
        if (prefs.getMaxBudget() < 0) {
            throw new IllegalArgumentException("maxBudget cannot be negative");
        }

        // Validate strategy
        if (prefs.getOptimizationStrategy() != null && !prefs.getOptimizationStrategy().isBlank()) {
            if (!VALID_STRATEGIES.contains(prefs.getOptimizationStrategy().toLowerCase())) {
                throw new IllegalArgumentException(
                    "Invalid optimizationStrategy '" + prefs.getOptimizationStrategy() + "'. Valid values: " + VALID_STRATEGIES
                );
            }
        }
    }

    private static LocalTime parseTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required and must be in HH:mm format");
        }
        try {
            return LocalTime.parse(value, TIME_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " format. Expected HH:mm, got: " + value);
        }
    }
}
