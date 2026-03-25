package com.dcplanner.factory;

import com.dcplanner.model.Attraction;

import java.util.List;
import java.util.Map;

/**
 * Factory for constructing and validating Attraction objects.
 *
 * Responsibilities:
 * - Validate that required fields are present
 * - Apply defaults for optional fields
 * - Provide typed creation methods per attraction type
 *
 * In production this would typically parse from raw maps or builder objects.
 * Here it acts as a validation + normalization layer on top of Jackson-deserialized data.
 */
public class AttractionFactory {

    private AttractionFactory() {}

    /**
     * Validate and normalize an attraction loaded from JSON.
     * Throws IllegalArgumentException if required fields are missing.
     */
    public static Attraction fromJson(Attraction raw) {
        validate(raw);
        return normalize(raw);
    }

    /**
     * Convenience factory method for creating a museum attraction programmatically.
     */
    public static Attraction createMuseum(
            String id, String name, double lat, double lng,
            String nearestStation, double fee, int durationMinutes,
            Map<String, Attraction.OpeningHours> hours, List<String> tags) {

        Attraction a = new Attraction();
        a.setId(id);
        a.setName(name);
        a.setType(Attraction.Type.MUSEUM);
        a.setLatitude(lat);
        a.setLongitude(lng);
        a.setNearestMetroStation(nearestStation);
        a.setEntranceFee(fee);
        a.setEstimatedVisitDurationMinutes(durationMinutes);
        a.setOpeningHours(hours);
        a.setAccessibilityFriendly(true); // default for museums
        a.setTags(tags);
        validate(a);
        return a;
    }

    /**
     * Convenience factory method for creating a landmark attraction programmatically.
     */
    public static Attraction createLandmark(
            String id, String name, double lat, double lng,
            String nearestStation, int durationMinutes,
            Map<String, Attraction.OpeningHours> hours, List<String> tags) {

        Attraction a = new Attraction();
        a.setId(id);
        a.setName(name);
        a.setType(Attraction.Type.LANDMARK);
        a.setLatitude(lat);
        a.setLongitude(lng);
        a.setNearestMetroStation(nearestStation);
        a.setEntranceFee(0.0); // landmarks are typically free
        a.setEstimatedVisitDurationMinutes(durationMinutes);
        a.setOpeningHours(hours);
        a.setAccessibilityFriendly(true);
        a.setTags(tags);
        validate(a);
        return a;
    }

    /**
     * Convenience factory method for creating a coffee shop attraction programmatically.
     */
    public static Attraction createCoffeeShop(
            String id, String name, double lat, double lng,
            String nearestStation, Map<String, Attraction.OpeningHours> hours, List<String> tags) {

        Attraction a = new Attraction();
        a.setId(id);
        a.setName(name);
        a.setType(Attraction.Type.COFFEE_SHOP);
        a.setLatitude(lat);
        a.setLongitude(lng);
        a.setNearestMetroStation(nearestStation);
        a.setEntranceFee(0.0);
        a.setEstimatedVisitDurationMinutes(30); // default for coffee shops
        a.setOpeningHours(hours);
        a.setAccessibilityFriendly(true);
        a.setTags(tags);
        validate(a);
        return a;
    }

    // --- Private helpers ---

    private static void validate(Attraction a) {
        if (a.getId() == null || a.getId().isBlank()) {
            throw new IllegalArgumentException("Attraction must have an id");
        }
        if (a.getName() == null || a.getName().isBlank()) {
            throw new IllegalArgumentException("Attraction '" + a.getId() + "' must have a name");
        }
        if (a.getType() == null) {
            throw new IllegalArgumentException("Attraction '" + a.getId() + "' must have a type");
        }
        if (a.getNearestMetroStation() == null || a.getNearestMetroStation().isBlank()) {
            throw new IllegalArgumentException("Attraction '" + a.getId() + "' must have a nearestMetroStation");
        }
        if (a.getOpeningHours() == null || a.getOpeningHours().isEmpty()) {
            throw new IllegalArgumentException("Attraction '" + a.getId() + "' must have openingHours");
        }
        if (a.getEstimatedVisitDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Attraction '" + a.getId() + "' must have a positive estimatedVisitDurationMinutes");
        }
    }

    private static Attraction normalize(Attraction a) {
        // Default tags to empty list if null
        if (a.getTags() == null) {
            a.setTags(List.of());
        }
        // Clamp fee to 0 if negative
        if (a.getEntranceFee() < 0) {
            a.setEntranceFee(0.0);
        }
        return a;
    }
}
