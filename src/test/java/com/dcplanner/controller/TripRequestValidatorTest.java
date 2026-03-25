package com.dcplanner.controller;

import com.dcplanner.model.TripRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TripRequestValidatorTest {

    // --- Valid request ---

    @Test
    void validRequest_passesValidation() {
        assertDoesNotThrow(() -> TripRequestValidator.validate(buildValid()));
    }

    @Test
    void validRequest_noPreferences_passesValidation() {
        TripRequest r = buildValid();
        r.setPreferences(null);
        assertDoesNotThrow(() -> TripRequestValidator.validate(r));
    }

    // --- Start location ---

    @Test
    void nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(null));
    }

    @Test
    void missingStartLocation_throwsException() {
        TripRequest r = buildValid();
        r.setStartLocation(null);
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void blankMetroStation_throwsException() {
        TripRequest r = buildValid();
        r.getStartLocation().setMetroStation("  ");
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    // --- Trip duration ---

    @Test
    void missingTripDuration_throwsException() {
        TripRequest r = buildValid();
        r.setTripDuration(null);
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void invalidTripDuration_throwsException() {
        TripRequest r = buildValid();
        r.setTripDuration("3_days");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> TripRequestValidator.validate(r)
        );
        assertTrue(ex.getMessage().contains("tripDuration"));
    }

    @Test
    void allValidDurations_pass() {
        for (String duration : List.of("half_day", "1_day", "2_days")) {
            TripRequest r = buildValid();
            r.setTripDuration(duration);
            assertDoesNotThrow(() -> TripRequestValidator.validate(r), "Failed for: " + duration);
        }
    }

    // --- Time window ---

    @Test
    void missingTimeWindow_throwsException() {
        TripRequest r = buildValid();
        r.setAvailableTimePerDay(null);
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void badTimeFormat_throwsException() {
        TripRequest r = buildValid();
        r.getAvailableTimePerDay().setStart("9am");
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void startAfterEnd_throwsException() {
        TripRequest r = buildValid();
        r.getAvailableTimePerDay().setStart("18:00");
        r.getAvailableTimePerDay().setEnd("09:00");
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void startEqualsEnd_throwsException() {
        TripRequest r = buildValid();
        r.getAvailableTimePerDay().setStart("09:00");
        r.getAvailableTimePerDay().setEnd("09:00");
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    // --- Trip start date ---

    @Test
    void validDate_passesValidation() {
        TripRequest r = buildValid();
        r.setTripStartDate("2026-03-29");
        assertDoesNotThrow(() -> TripRequestValidator.validate(r));
    }

    @Test
    void invalidDateFormat_throwsException() {
        TripRequest r = buildValid();
        r.setTripStartDate("March 29 2026");
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void nullDate_isOptional_passes() {
        TripRequest r = buildValid();
        r.setTripStartDate(null);
        assertDoesNotThrow(() -> TripRequestValidator.validate(r));
    }

    // --- Preferences ---

    @Test
    void invalidAttractionType_throwsException() {
        TripRequest r = buildValid();
        r.getPreferences().setTypes(List.of("zoo"));
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> TripRequestValidator.validate(r)
        );
        assertTrue(ex.getMessage().contains("zoo"));
    }

    @Test
    void validAttractionTypes_pass() {
        TripRequest r = buildValid();
        r.getPreferences().setTypes(List.of("museum", "landmark", "coffee_shop", "cinema"));
        assertDoesNotThrow(() -> TripRequestValidator.validate(r));
    }

    @Test
    void negativeBudget_throwsException() {
        TripRequest r = buildValid();
        r.getPreferences().setMaxBudget(-10.0);
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void invalidStrategy_throwsException() {
        TripRequest r = buildValid();
        r.getPreferences().setOptimizationStrategy("random_strategy");
        assertThrows(IllegalArgumentException.class, () -> TripRequestValidator.validate(r));
    }

    @Test
    void validStrategies_pass() {
        for (String strategy : List.of("minimize_travel_time", "maximize_attractions")) {
            TripRequest r = buildValid();
            r.getPreferences().setOptimizationStrategy(strategy);
            assertDoesNotThrow(() -> TripRequestValidator.validate(r), "Failed for: " + strategy);
        }
    }

    // --- Builder ---

    private TripRequest buildValid() {
        TripRequest r = new TripRequest();

        TripRequest.StartLocation loc = new TripRequest.StartLocation();
        loc.setMetroStation("union_station");
        r.setStartLocation(loc);

        r.setTripDuration("1_day");

        TripRequest.TimeWindow window = new TripRequest.TimeWindow();
        window.setStart("09:00");
        window.setEnd("18:00");
        r.setAvailableTimePerDay(window);

        TripRequest.Preferences prefs = new TripRequest.Preferences();
        prefs.setTypes(List.of("museum"));
        prefs.setMaxBudget(20.0);
        prefs.setAccessibilityRequired(false);
        prefs.setOptimizationStrategy("maximize_attractions");
        r.setPreferences(prefs);

        return r;
    }
}
