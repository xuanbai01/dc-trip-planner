package com.dcplanner.algorithm;

import com.dcplanner.model.MetroEdge;
import com.dcplanner.model.MetroStation;

import java.util.*;

/**
 * Weighted directed graph representing the DC Metro network.
 * Nodes are station IDs. Edge weights are travel time in minutes.
 */
public class MetroGraph {

    // adjacency list: stationId -> list of (neighborId, edge)
    private final Map<String, List<EdgeEntry>> adjacency = new HashMap<>();
    private final Map<String, MetroStation> stationIndex = new HashMap<>();

    public MetroGraph(List<MetroStation> stations, List<MetroEdge> edges) {
        for (MetroStation station : stations) {
            stationIndex.put(station.getId(), station);
            adjacency.put(station.getId(), new ArrayList<>());
        }
        for (MetroEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>())
                     .add(new EdgeEntry(edge.getTo(), edge.getTravelTimeMinutes(), edge.getFare()));
        }
    }

    public List<EdgeEntry> getNeighbors(String stationId) {
        return adjacency.getOrDefault(stationId, Collections.emptyList());
    }

    public MetroStation getStation(String stationId) {
        return stationIndex.get(stationId);
    }

    public Set<String> getAllStationIds() {
        return Collections.unmodifiableSet(stationIndex.keySet());
    }

    public boolean containsStation(String stationId) {
        return stationIndex.containsKey(stationId);
    }

    // --- Inner class ---

    public static class EdgeEntry {
        private final String targetStation;
        private final int travelTimeMinutes;
        private final double fare;

        public EdgeEntry(String targetStation, int travelTimeMinutes, double fare) {
            this.targetStation = targetStation;
            this.travelTimeMinutes = travelTimeMinutes;
            this.fare = fare;
        }

        public String getTargetStation() { return targetStation; }
        public int getTravelTimeMinutes() { return travelTimeMinutes; }
        public double getFare() { return fare; }
    }
}
