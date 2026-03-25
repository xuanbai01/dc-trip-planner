package com.dcplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetroStation {

    private String id;
    private String name;
    private List<String> lines;
    private double latitude;
    private double longitude;

    public MetroStation() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getLines() { return lines; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLines(List<String> lines) { this.lines = lines; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return "MetroStation{id='" + id + "', name='" + name + "', lines=" + lines + "}";
    }
}
