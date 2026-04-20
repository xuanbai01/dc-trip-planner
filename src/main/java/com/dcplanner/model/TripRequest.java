package com.dcplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TripRequest {

    private StartLocation startLocation;
    private String tripDuration;
    private String tripStartDate;       // "YYYY-MM-DD" e.g. "2026-03-29"
    private TimeWindow availableTimePerDay;
    private Preferences preferences;

    public TripRequest() {}

    public StartLocation getStartLocation() { return startLocation; }
    public String getTripDuration() { return tripDuration; }
    public String getTripStartDate() { return tripStartDate; }
    public TimeWindow getAvailableTimePerDay() { return availableTimePerDay; }
    public Preferences getPreferences() { return preferences; }

    public void setStartLocation(StartLocation startLocation) { this.startLocation = startLocation; }
    public void setTripDuration(String tripDuration) { this.tripDuration = tripDuration; }
    public void setTripStartDate(String tripStartDate) { this.tripStartDate = tripStartDate; }
    public void setAvailableTimePerDay(TimeWindow availableTimePerDay) { this.availableTimePerDay = availableTimePerDay; }
    public void setPreferences(Preferences preferences) { this.preferences = preferences; }

    // --- Nested classes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StartLocation {
        private String metroStation;

        public StartLocation() {}
        public String getMetroStation() { return metroStation; }
        public void setMetroStation(String metroStation) { this.metroStation = metroStation; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeWindow {
        private String start;  // "09:00"
        private String end;    // "18:00"

        public TimeWindow() {}
        public String getStart() { return start; }
        public String getEnd() { return end; }
        public void setStart(String start) { this.start = start; }
        public void setEnd(String end) { this.end = end; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Preferences {
        private List<String> types;               // ["museum", "coffee_shop"]
        private double maxBudget;
        private boolean accessibilityRequired;
        private String optimizationStrategy;      // minimize_travel_time | maximize_attractions | balanced

        public Preferences() {}

        public List<String> getTypes() { return types; }
        public double getMaxBudget() { return maxBudget; }
        public boolean isAccessibilityRequired() { return accessibilityRequired; }
        public String getOptimizationStrategy() { return optimizationStrategy; }

        public void setTypes(List<String> types) { this.types = types; }
        public void setMaxBudget(double maxBudget) { this.maxBudget = maxBudget; }
        public void setAccessibilityRequired(boolean accessibilityRequired) { this.accessibilityRequired = accessibilityRequired; }
        public void setOptimizationStrategy(String optimizationStrategy) { this.optimizationStrategy = optimizationStrategy; }
    }
}
