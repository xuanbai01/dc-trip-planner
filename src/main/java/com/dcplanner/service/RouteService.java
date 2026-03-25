package com.dcplanner.service;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.algorithm.MetroGraph;
import com.dcplanner.repository.MetroRepository;

/**
 * Exposes Metro routing to controllers and other services.
 * Owns the MetroGraph and Dijkstra instances.
 */
public class RouteService {

    private final MetroGraph graph;
    private final Dijkstra dijkstra;

    public RouteService(MetroRepository metroRepository) {
        this.graph = new MetroGraph(
                metroRepository.findAllStations(),
                metroRepository.findAllEdges()
        );
        this.dijkstra = new Dijkstra(graph);
    }

    public Dijkstra.RouteResult getRoute(String fromStationId, String toStationId) {
        return dijkstra.findShortestPath(fromStationId, toStationId);
    }

    public Dijkstra getDijkstra() {
        return dijkstra;
    }

    public MetroGraph getGraph() {
        return graph;
    }
}
