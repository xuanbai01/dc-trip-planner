package com.dcplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetroEdge {

    private String from;
    private String to;
    private int travelTimeMinutes;
    private double fare;

    public MetroEdge() {}

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public int getTravelTimeMinutes() { return travelTimeMinutes; }
    public double getFare() { return fare; }

    public void setFrom(String from) { this.from = from; }
    public void setTo(String to) { this.to = to; }
    public void setTravelTimeMinutes(int travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }
    public void setFare(double fare) { this.fare = fare; }

    @Override
    public String toString() {
        return "MetroEdge{from='" + from + "', to='" + to + "', time=" + travelTimeMinutes + "min, fare=$" + fare + "}";
    }
}
