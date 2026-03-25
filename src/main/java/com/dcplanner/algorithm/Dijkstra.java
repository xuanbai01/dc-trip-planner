package com.dcplanner.algorithm;

import java.util.*;

/**
 * Dijkstra's shortest path algorithm on the MetroGraph.
 * Returns the shortest path (by travel time) between two stations.
 * Fare is calculated once based on total path length via MetroFareCalculator.
 */
public class Dijkstra {

    private final MetroGraph graph;

    public Dijkstra(MetroGraph graph) {
        this.graph = graph;
    }

    public RouteResult findShortestPath(String sourceId, String destinationId) {
        if (!graph.containsStation(sourceId) || !graph.containsStation(destinationId)) {
            return RouteResult.empty();
        }

        if (sourceId.equals(destinationId)) {
            return new RouteResult(List.of(sourceId), 0, 0.0);
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();

        for (String id : graph.getAllStationIds()) {
            dist.put(id, Integer.MAX_VALUE);
        }
        dist.put(sourceId, 0);

        PriorityQueue<NodeEntry> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.time));
        pq.offer(new NodeEntry(sourceId, 0));

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();

            if (visited.contains(current.stationId)) continue;
            visited.add(current.stationId);

            if (current.stationId.equals(destinationId)) break;

            for (MetroGraph.EdgeEntry neighbor : graph.getNeighbors(current.stationId)) {
                if (visited.contains(neighbor.getTargetStation())) continue;

                int newDist = current.time + neighbor.getTravelTimeMinutes();
                if (newDist < dist.get(neighbor.getTargetStation())) {
                    dist.put(neighbor.getTargetStation(), newDist);
                    prev.put(neighbor.getTargetStation(), current.stationId);
                    pq.offer(new NodeEntry(neighbor.getTargetStation(), newDist));
                }
            }
        }

        if (dist.get(destinationId) == Integer.MAX_VALUE) {
            return RouteResult.empty();
        }

        List<String> path = reconstructPath(prev, sourceId, destinationId);
        double fare = MetroFareCalculator.calculate(path.size());

        return new RouteResult(path, dist.get(destinationId), fare);
    }

    private List<String> reconstructPath(Map<String, String> prev, String source, String destination) {
        LinkedList<String> path = new LinkedList<>();
        String current = destination;
        while (current != null) {
            path.addFirst(current);
            current = prev.get(current);
        }
        if (!path.getFirst().equals(source)) {
            return Collections.emptyList();
        }
        return path;
    }

    private static class NodeEntry {
        final String stationId;
        final int time;

        NodeEntry(String stationId, int time) {
            this.stationId = stationId;
            this.time = time;
        }
    }

    public static class RouteResult {
        private final List<String> stationPath;
        private final int totalTravelTimeMinutes;
        private final double totalFare;
        private final boolean found;

        public RouteResult(List<String> stationPath, int totalTravelTimeMinutes, double totalFare) {
            this.stationPath = stationPath;
            this.totalTravelTimeMinutes = totalTravelTimeMinutes;
            this.totalFare = totalFare;
            this.found = !stationPath.isEmpty();
        }

        public static RouteResult empty() {
            return new RouteResult(Collections.emptyList(), 0, 0.0);
        }

        public List<String> getStationPath() { return stationPath; }
        public int getTotalTravelTimeMinutes() { return totalTravelTimeMinutes; }
        public double getTotalFare() { return totalFare; }
        public boolean isFound() { return found; }

        @Override
        public String toString() {
            return "RouteResult{path=" + stationPath + ", time=" + totalTravelTimeMinutes + "min, fare=$" + totalFare + "}";
        }
    }
}
