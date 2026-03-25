package com.dcplanner.service;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.repository.AttractionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calculates total trip cost: Metro fares + attraction entrance fees.
 */
public class CostService {

    private final AttractionRepository attractionRepository;
    private final RouteService routeService;

    public CostService(AttractionRepository attractionRepository, RouteService routeService) {
        this.attractionRepository = attractionRepository;
        this.routeService = routeService;
    }

    /**
     * Estimate total cost for a list of attraction IDs visited in order,
     * starting from a given Metro station.
     */
    public CostEstimate estimateCost(List<String> attractionIds, String startStationId) {
        double totalMetroFare = 0.0;
        double totalEntranceFees = 0.0;
        List<String> warnings = new ArrayList<>();

        String currentStation = startStationId;

        for (String attractionId : attractionIds) {
            Optional<Attraction> opt = attractionRepository.findById(attractionId);
            if (opt.isEmpty()) {
                warnings.add("Attraction not found: " + attractionId);
                continue;
            }

            Attraction attraction = opt.get();
            totalEntranceFees += attraction.getEntranceFee();

            String destStation = attraction.getNearestMetroStation();
            if (!destStation.equals(currentStation)) {
                Dijkstra.RouteResult route = routeService.getRoute(currentStation, destStation);
                if (route.isFound()) {
                    totalMetroFare += route.getTotalFare();
                } else {
                    warnings.add("No Metro route found from " + currentStation + " to " + destStation);
                }
            }

            currentStation = destStation;
        }

        return new CostEstimate(totalMetroFare, totalEntranceFees, totalMetroFare + totalEntranceFees, warnings);
    }

    // --- Response object ---

    public static class CostEstimate {
        private final double metroFare;
        private final double entranceFees;
        private final double totalCost;
        private final List<String> warnings;

        public CostEstimate(double metroFare, double entranceFees, double totalCost, List<String> warnings) {
            this.metroFare = metroFare;
            this.entranceFees = entranceFees;
            this.totalCost = totalCost;
            this.warnings = warnings;
        }

        public double getMetroFare() { return metroFare; }
        public double getEntranceFees() { return entranceFees; }
        public double getTotalCost() { return totalCost; }
        public List<String> getWarnings() { return warnings; }
    }
}
