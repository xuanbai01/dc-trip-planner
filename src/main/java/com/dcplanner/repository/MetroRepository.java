package com.dcplanner.repository;

import com.dcplanner.model.MetroEdge;
import com.dcplanner.model.MetroStation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MetroRepository {

    private final List<MetroStation> stations;
    private final List<MetroEdge> edges;

    public MetroRepository() {
        this.stations = loadStations();
        this.edges = loadEdges();
    }

    private List<MetroStation> loadStations() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/data/metro_stations.json");
            if (is == null) {
                System.err.println("metro_stations.json not found in resources/data/");
                return Collections.emptyList();
            }
            return mapper.readValue(is, new TypeReference<List<MetroStation>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load metro_stations.json", e);
        }
    }

    private List<MetroEdge> loadEdges() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/data/metro_edges.json");
            if (is == null) {
                System.err.println("metro_edges.json not found in resources/data/");
                return Collections.emptyList();
            }
            return mapper.readValue(is, new TypeReference<List<MetroEdge>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load metro_edges.json", e);
        }
    }

    public List<MetroStation> findAllStations() {
        return Collections.unmodifiableList(stations);
    }

    public List<MetroEdge> findAllEdges() {
        return Collections.unmodifiableList(edges);
    }

    public Optional<MetroStation> findStationById(String id) {
        return stations.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }
}
