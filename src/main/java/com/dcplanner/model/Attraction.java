package com.dcplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attraction {

    public enum Type {
        MUSEUM, COFFEE_SHOP, CINEMA, LANDMARK
    }

    private String id;
    private String name;
    private Type type;
    private double latitude;
    private double longitude;
    private String nearestMetroStation;
    private Map<String, OpeningHours> openingHours; // key: "monday", "tuesday", etc.
    private double entranceFee;
    private int estimatedVisitDurationMinutes;
    private boolean accessibilityFriendly;
    private List<String> tags;

    public Attraction() {}

    // --- Getters ---

    public String getId() { return id; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getNearestMetroStation() { return nearestMetroStation; }
    public Map<String, OpeningHours> getOpeningHours() { return openingHours; }
    public double getEntranceFee() { return entranceFee; }
    public int getEstimatedVisitDurationMinutes() { return estimatedVisitDurationMinutes; }
    public boolean isAccessibilityFriendly() { return accessibilityFriendly; }
    public List<String> getTags() { return tags; }

    // --- Setters ---

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(Type type) { this.type = type; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setNearestMetroStation(String nearestMetroStation) { this.nearestMetroStation = nearestMetroStation; }
    public void setOpeningHours(Map<String, OpeningHours> openingHours) { this.openingHours = openingHours; }
    public void setEntranceFee(double entranceFee) { this.entranceFee = entranceFee; }
    public void setEstimatedVisitDurationMinutes(int estimatedVisitDurationMinutes) { this.estimatedVisitDurationMinutes = estimatedVisitDurationMinutes; }
    public void setAccessibilityFriendly(boolean accessibilityFriendly) { this.accessibilityFriendly = accessibilityFriendly; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // --- Nested class ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours {
        private String open;
        private String close;

        public OpeningHours() {}

        public String getOpen() { return open; }
        public String getClose() { return close; }
        public void setOpen(String open) { this.open = open; }
        public void setClose(String close) { this.close = close; }
    }

    @Override
    public String toString() {
        return "Attraction{id='" + id + "', name='" + name + "', type=" + type + "}";
    }
}
