package com.dcplanner.repository;

import com.dcplanner.model.Attraction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AttractionRepository {

    private final List<Attraction> attractions;

    public AttractionRepository() {
        this.attractions = loadAttractions();
    }

    private List<Attraction> loadAttractions() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/data/attractions.json");
            if (is == null) {
                System.err.println("attractions.json not found in resources/data/");
                return Collections.emptyList();
            }
            return mapper.readValue(is, new TypeReference<List<Attraction>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load attractions.json", e);
        }
    }

    public List<Attraction> findAll() {
        return Collections.unmodifiableList(attractions);
    }

    public Optional<Attraction> findById(String id) {
        return attractions.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst();
    }

    public List<Attraction> findByType(Attraction.Type type) {
        return attractions.stream()
                .filter(a -> a.getType() == type)
                .collect(Collectors.toList());
    }

    public List<Attraction> findByTypes(List<Attraction.Type> types) {
        return attractions.stream()
                .filter(a -> types.contains(a.getType()))
                .collect(Collectors.toList());
    }

    public List<Attraction> findAccessible() {
        return attractions.stream()
                .filter(Attraction::isAccessibilityFriendly)
                .collect(Collectors.toList());
    }

    public List<Attraction> findByMaxFee(double maxFee) {
        return attractions.stream()
                .filter(a -> a.getEntranceFee() <= maxFee)
                .collect(Collectors.toList());
    }

    /**
     * Multi-filter method used by AttractionService.
     * Passing null for a parameter means "no filter on this field".
     */
    public List<Attraction> findFiltered(List<Attraction.Type> types, Double maxFee, Boolean accessibilityRequired) {
        return attractions.stream()
                .filter(a -> types == null || types.isEmpty() || types.contains(a.getType()))
                .filter(a -> maxFee == null || a.getEntranceFee() <= maxFee)
                .filter(a -> accessibilityRequired == null || !accessibilityRequired || a.isAccessibilityFriendly())
                .collect(Collectors.toList());
    }
}
