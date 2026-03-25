package com.dcplanner.service;

import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;
import com.dcplanner.repository.AttractionRepository;
import com.dcplanner.repository.MetroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TripPlannerService using the real JSON data files.
 * These tests verify the full planning pipeline end-to-end.
 */
class TripPlannerServiceTest {

    private TripPlannerService tripPlannerService;

    @BeforeEach
    void setUp() {
        AttractionRepository attractionRepository = new AttractionRepository();
        MetroRepository metroRepository = new MetroRepository();
        AttractionService attractionService = new AttractionService(attractionRepository);
        RouteService routeService = new RouteService(metroRepository);

        tripPlannerService = new TripPlannerService(attractionRepository, attractionService, routeService);
    }

    // --- Basic itinerary generation ---

    @Test
    void oneDayTrip_returnsOneDayItinerary() {
        TripRequest request = buildRequest("1_day", "maximize_attractions", List.of("museum", "landmark"));
        Itinerary itinerary = tripPlannerService.planTrip(request);

        assertNotNull(itinerary);
        assertEquals(1, itinerary.getDays().size());
    }

    @Test
    void twoDayTrip_returnsTwoDayItinerary() {
        TripRequest request = buildRequest("2_days", "maximize_attractions", List.of("museum", "landmark"));
        Itinerary itinerary = tripPlannerService.planTrip(request);

        assertNotNull(itinerary);
        assertEquals(2, itinerary.getDays().size());
    }

    @Test
    void halfDayTrip_returnsOneDayItinerary() {
        TripRequest request = buildRequest("half_day", "minimize_travel_time", List.of("museum"));
        Itinerary itinerary = tripPlannerService.planTrip(request);

        assertNotNull(itinerary);
        assertEquals(1, itinerary.getDays().size());
    }

    // --- Stops are within time window ---

    @Test
    void allStops_withinTimeWindow() {
        TripRequest request = buildRequest("1_day", "maximize_attractions", List.of("museum", "landmark"));
        Itinerary itinerary = tripPlannerService.planTrip(request);

        for (Itinerary.TimeBlock block : itinerary.getDays().get(0).getTimeBlocks()) {
            assertTrue(block.getStartTime().compareTo("09:00") >= 0,
                    "Stop starts before window: " + block.getStartTime());
            assertTrue(block.getEndTime().compareTo("18:00") <= 0,
                    "Stop ends after window: " + block.getEndTime());
        }
    }

    // --- No duplicate attractions across days ---

    @Test
    void twoDayTrip_noAttractionRepeatedAcrossDays() {
        TripRequest request = buildRequest("2_days", "maximize_attractions", List.of("museum", "landmark"));
        Itinerary itinerary = tripPlannerService.planTrip(request);

        List<String> day1Ids = itinerary.getDays().get(0).getTimeBlocks().stream()
                .map(b -> b.getAttraction().getId())
                .toList();
        List<String> day2Ids = itinerary.getDays().get(1).getTimeBlocks().stream()
                .map(b -> b.getAttraction().getId())
                .toList();

        for (String id : day1Ids) {
            assertFalse(day2Ids.contains(id), "Attraction repeated across days: " + id);
        }
    }

    // --- Cost calculations ---

    @Test
    void totalCost_equalsSumOfDayCosts() {
        TripRequest request = buildRequest("2_days", "maximize_attractions", List.of("museum", "landmark"));
        Itinerary itinerary = tripPlannerService.planTrip(request);

        double sumOfDays = itinerary.getDays().stream()
                .mapToDouble(Itinerary.Day::getDayCost)
                .sum();

        assertEquals(sumOfDays, itinerary.getTotalCost(), 0.001);
    }

    @Test
    void totalAttractions_equalsSumOfStopsAcrossDays() {
        TripRequest request = buildRequest("2_days", "maximize_attractions", List.of("museum", "landmark"));
        Itinerary itinerary = tripPlannerService.planTrip(request);

        int sumOfStops = itinerary.getDays().stream()
                .mapToInt(d -> d.getTimeBlocks().size())
                .sum();

        assertEquals(sumOfStops, itinerary.getTotalAttractions());
    }

    // --- Strategy differences ---

    @Test
    void maximizeAttractions_producesAtLeastOneStop() {
        TripRequest request = buildRequest("1_day", "maximize_attractions", List.of("museum"));
        Itinerary itinerary = tripPlannerService.planTrip(request);
        assertTrue(itinerary.getTotalAttractions() > 0);
    }

    @Test
    void minimizeTravelTime_producesAtLeastOneStop() {
        TripRequest request = buildRequest("1_day", "minimize_travel_time", List.of("museum"));
        Itinerary itinerary = tripPlannerService.planTrip(request);
        assertTrue(itinerary.getTotalAttractions() > 0);
    }

    // --- Validation edge cases ---

    @Test
    void nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> tripPlannerService.planTrip(null));
    }

    @Test
    void missingStartStation_throwsException() {
        TripRequest request = buildRequest("1_day", "maximize_attractions", List.of("museum"));
        request.getStartLocation().setMetroStation(null);
        assertThrows(IllegalArgumentException.class, () -> tripPlannerService.planTrip(request));
    }

    @Test
    void veryTightTimeWindow_returnsEmptyOrFewStops() {
        // 30 minute window — almost nothing should fit
        TripRequest request = buildRequest("1_day", "maximize_attractions", List.of("museum"));
        request.getAvailableTimePerDay().setStart("12:00");
        request.getAvailableTimePerDay().setEnd("12:30");
        Itinerary itinerary = tripPlannerService.planTrip(request);

        // Should not crash — just return 0 or 1 stops
        assertNotNull(itinerary);
        assertTrue(itinerary.getTotalAttractions() <= 1);
    }

    // --- Date-based day of week ---

    @Test
    void sundayDate_excludesClosedAttractions() {
        // Capitol building and Library of Congress are closed Sundays
        // A Sunday trip requesting landmarks should still return a valid itinerary
        // (just without those two)
        TripRequest request = buildRequest("1_day", "maximize_attractions", List.of("landmark"));
        request.setTripStartDate("2026-03-29"); // Sunday
        Itinerary itinerary = tripPlannerService.planTrip(request);

        assertNotNull(itinerary);
        List<String> stopIds = itinerary.getDays().get(0).getTimeBlocks().stream()
                .map(b -> b.getAttraction().getId())
                .toList();

        assertFalse(stopIds.contains("capitol_building"), "Capitol should be excluded on Sunday");
        assertFalse(stopIds.contains("library_of_congress"), "Library of Congress should be excluded on Sunday");
    }

    // --- Helpers ---

    private TripRequest buildRequest(String duration, String strategy, List<String> types) {
        TripRequest r = new TripRequest();

        TripRequest.StartLocation loc = new TripRequest.StartLocation();
        loc.setMetroStation("union_station");
        r.setStartLocation(loc);

        r.setTripDuration(duration);

        TripRequest.TimeWindow window = new TripRequest.TimeWindow();
        window.setStart("09:00");
        window.setEnd("18:00");
        r.setAvailableTimePerDay(window);

        TripRequest.Preferences prefs = new TripRequest.Preferences();
        prefs.setTypes(types);
        prefs.setMaxBudget(50.0);
        prefs.setAccessibilityRequired(false);
        prefs.setOptimizationStrategy(strategy);
        r.setPreferences(prefs);

        return r;
    }
}
