package com.dcplanner.algorithm;

import com.dcplanner.model.MetroEdge;
import com.dcplanner.model.MetroStation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DijkstraTest {

    private MetroGraph graph;
    private Dijkstra dijkstra;

    /**
     * Test graph:
     *
     *  A --2min--> B --2min--> C
     *              |
     *              4min
     *              |
     *              D
     *
     * All edges bidirectional.
     */
    @BeforeEach
    void setUp() {
        List<MetroStation> stations = List.of(
                station("A"), station("B"), station("C"), station("D")
        );

        List<MetroEdge> edges = List.of(
                edge("A", "B", 2),
                edge("B", "A", 2),
                edge("B", "C", 2),
                edge("C", "B", 2),
                edge("B", "D", 4),
                edge("D", "B", 4)
        );

        graph = new MetroGraph(stations, edges);
        dijkstra = new Dijkstra(graph);
    }

    @Test
    void sameStation_returnsPathOfOne_zeroTime() {
        Dijkstra.RouteResult result = dijkstra.findShortestPath("A", "A");
        assertTrue(result.isFound());
        assertEquals(List.of("A"), result.getStationPath());
        assertEquals(0, result.getTotalTravelTimeMinutes());
        assertEquals(0.0, result.getTotalFare());
    }

    @Test
    void directNeighbor_returnsCorrectPath() {
        Dijkstra.RouteResult result = dijkstra.findShortestPath("A", "B");
        assertTrue(result.isFound());
        assertEquals(List.of("A", "B"), result.getStationPath());
        assertEquals(2, result.getTotalTravelTimeMinutes());
    }

    @Test
    void twoHopPath_returnsCorrectPathAndTime() {
        Dijkstra.RouteResult result = dijkstra.findShortestPath("A", "C");
        assertTrue(result.isFound());
        assertEquals(List.of("A", "B", "C"), result.getStationPath());
        assertEquals(4, result.getTotalTravelTimeMinutes());
    }

    @Test
    void shortestPath_prefersLessTime() {
        // A to D: A→B→D = 6 min. No other path.
        Dijkstra.RouteResult result = dijkstra.findShortestPath("A", "D");
        assertTrue(result.isFound());
        assertEquals(List.of("A", "B", "D"), result.getStationPath());
        assertEquals(6, result.getTotalTravelTimeMinutes());
    }

    @Test
    void reverseDirection_works() {
        Dijkstra.RouteResult result = dijkstra.findShortestPath("C", "A");
        assertTrue(result.isFound());
        assertEquals(List.of("C", "B", "A"), result.getStationPath());
        assertEquals(4, result.getTotalTravelTimeMinutes());
    }

    @Test
    void unknownSource_returnsNotFound() {
        Dijkstra.RouteResult result = dijkstra.findShortestPath("UNKNOWN", "A");
        assertFalse(result.isFound());
    }

    @Test
    void unknownDestination_returnsNotFound() {
        Dijkstra.RouteResult result = dijkstra.findShortestPath("A", "UNKNOWN");
        assertFalse(result.isFound());
    }

    @Test
    void disconnectedStation_returnsNotFound() {
        // Add an island station with no edges
        List<MetroStation> stations = List.of(
                station("A"), station("B"), station("ISLAND")
        );
        List<MetroEdge> edges = List.of(
                edge("A", "B", 2),
                edge("B", "A", 2)
        );
        MetroGraph isolatedGraph = new MetroGraph(stations, edges);
        Dijkstra isolatedDijkstra = new Dijkstra(isolatedGraph);

        Dijkstra.RouteResult result = isolatedDijkstra.findShortestPath("A", "ISLAND");
        assertFalse(result.isFound());
    }

    @Test
    void fare_calculatedFromPathLength_notPerHop() {
        // A→B→C = 2 hops = path size 3 → should be $2.25, not $4.50
        Dijkstra.RouteResult result = dijkstra.findShortestPath("A", "C");
        assertEquals(2.25, result.getTotalFare());
    }

    // --- Helpers ---

    private MetroStation station(String id) {
        MetroStation s = new MetroStation();
        s.setId(id);
        s.setName(id);
        s.setLines(List.of("red"));
        s.setLatitude(0);
        s.setLongitude(0);
        return s;
    }

    private MetroEdge edge(String from, String to, int time) {
        MetroEdge e = new MetroEdge();
        e.setFrom(from);
        e.setTo(to);
        e.setTravelTimeMinutes(time);
        e.setFare(2.25);
        return e;
    }
}
