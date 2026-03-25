package com.dcplanner.service;

import com.dcplanner.model.Attraction;
import com.dcplanner.repository.AttractionRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Thin service layer over AttractionRepository.
 * Handles type conversion from raw strings to Attraction.Type enum.
 */
public class AttractionService {

    private final AttractionRepository repository;

    public AttractionService(AttractionRepository repository) {
        this.repository = repository;
    }

    public List<Attraction> getAll() {
        return repository.findAll();
    }

    public Optional<Attraction> getById(String id) {
        return repository.findById(id);
    }

    /**
     * Filtered query. All params are optional (null = no filter).
     *
     * @param types               comma-separated type strings e.g. "museum,coffee_shop"
     * @param maxFee              max entrance fee
     * @param accessibilityRequired  if true, only return accessible attractions
     */
    public List<Attraction> getFiltered(String types, Double maxFee, Boolean accessibilityRequired) {
        List<Attraction.Type> typeList = null;

        if (types != null && !types.isBlank()) {
            typeList = Arrays.stream(types.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(Attraction.Type::valueOf)
                    .collect(Collectors.toList());
        }

        return repository.findFiltered(typeList, maxFee, accessibilityRequired);
    }

    /**
     * Convert a list of type name strings to Attraction.Type enums.
     * Used by TripPlannerService.
     */
    public List<Attraction.Type> parseTypes(List<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) return List.of();
        return typeNames.stream()
                .map(String::toUpperCase)
                .map(Attraction.Type::valueOf)
                .collect(Collectors.toList());
    }
}
