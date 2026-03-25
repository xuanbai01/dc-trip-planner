package com.dcplanner.factory;

import com.dcplanner.model.Attraction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttractionFactoryTest {

    // --- fromJson (validation + normalization) ---

    @Test
    void validAttraction_passesValidation() {
        Attraction a = buildValid();
        assertDoesNotThrow(() -> AttractionFactory.fromJson(a));
    }

    @Test
    void missingId_throwsException() {
        Attraction a = buildValid();
        a.setId(null);
        assertThrows(IllegalArgumentException.class, () -> AttractionFactory.fromJson(a));
    }

    @Test
    void blankId_throwsException() {
        Attraction a = buildValid();
        a.setId("  ");
        assertThrows(IllegalArgumentException.class, () -> AttractionFactory.fromJson(a));
    }

    @Test
    void missingName_throwsException() {
        Attraction a = buildValid();
        a.setName(null);
        assertThrows(IllegalArgumentException.class, () -> AttractionFactory.fromJson(a));
    }

    @Test
    void missingType_throwsException() {
        Attraction a = buildValid();
        a.setType(null);
        assertThrows(IllegalArgumentException.class, () -> AttractionFactory.fromJson(a));
    }

    @Test
    void missingMetroStation_throwsException() {
        Attraction a = buildValid();
        a.setNearestMetroStation(null);
        assertThrows(IllegalArgumentException.class, () -> AttractionFactory.fromJson(a));
    }

    @Test
    void missingOpeningHours_throwsException() {
        Attraction a = buildValid();
        a.setOpeningHours(null);
        assertThrows(IllegalArgumentException.class, () -> AttractionFactory.fromJson(a));
    }

    @Test
    void zeroDuration_throwsException() {
        Attraction a = buildValid();
        a.setEstimatedVisitDurationMinutes(0);
        assertThrows(IllegalArgumentException.class, () -> AttractionFactory.fromJson(a));
    }

    @Test
    void negativeFee_normalizedToZero() {
        Attraction a = buildValid();
        a.setEntranceFee(-5.0);
        Attraction result = AttractionFactory.fromJson(a);
        assertEquals(0.0, result.getEntranceFee());
    }

    @Test
    void nullTags_normalizedToEmptyList() {
        Attraction a = buildValid();
        a.setTags(null);
        Attraction result = AttractionFactory.fromJson(a);
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
    }

    // --- createMuseum ---

    @Test
    void createMuseum_setsTypeCorrectly() {
        Attraction a = AttractionFactory.createMuseum(
                "test_museum", "Test Museum", 38.89, -77.02,
                "smithsonian", 0.0, 90, sampleHours(), List.of("history")
        );
        assertEquals(Attraction.Type.MUSEUM, a.getType());
    }

    @Test
    void createMuseum_defaultsAccessibilityToTrue() {
        Attraction a = AttractionFactory.createMuseum(
                "test_museum", "Test Museum", 38.89, -77.02,
                "smithsonian", 0.0, 90, sampleHours(), List.of()
        );
        assertTrue(a.isAccessibilityFriendly());
    }

    // --- createLandmark ---

    @Test
    void createLandmark_defaultsFeeToZero() {
        Attraction a = AttractionFactory.createLandmark(
                "test_landmark", "Test Landmark", 38.89, -77.02,
                "smithsonian", 45, sampleHours(), List.of()
        );
        assertEquals(0.0, a.getEntranceFee());
    }

    // --- createCoffeeShop ---

    @Test
    void createCoffeeShop_defaultsDurationTo30() {
        Attraction a = AttractionFactory.createCoffeeShop(
                "test_coffee", "Test Coffee", 38.89, -77.02,
                "metro_center", sampleHours(), List.of()
        );
        assertEquals(30, a.getEstimatedVisitDurationMinutes());
    }

    // --- Helpers ---

    private Attraction buildValid() {
        Attraction a = new Attraction();
        a.setId("test_id");
        a.setName("Test Attraction");
        a.setType(Attraction.Type.MUSEUM);
        a.setLatitude(38.89);
        a.setLongitude(-77.02);
        a.setNearestMetroStation("smithsonian");
        a.setEntranceFee(0.0);
        a.setEstimatedVisitDurationMinutes(60);
        a.setOpeningHours(sampleHours());
        a.setAccessibilityFriendly(true);
        a.setTags(List.of("test"));
        return a;
    }

    private Map<String, Attraction.OpeningHours> sampleHours() {
        Attraction.OpeningHours hours = new Attraction.OpeningHours();
        hours.setOpen("09:00");
        hours.setClose("17:00");
        return Map.of(
                "monday", hours, "tuesday", hours, "wednesday", hours,
                "thursday", hours, "friday", hours, "saturday", hours, "sunday", hours
        );
    }
}
