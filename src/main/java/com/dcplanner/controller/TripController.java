package com.dcplanner.controller;

import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;
import com.dcplanner.service.CostService;
import com.dcplanner.service.TripPlannerService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Arrays;
import java.util.List;

/**
 * POST /trip/plan      - Generate a full itinerary
 * GET  /trip/cost      - Estimate cost for a list of attraction IDs
 */
public class TripController {

    private final TripPlannerService tripPlannerService;
    private final CostService costService;

    public TripController(TripPlannerService tripPlannerService, CostService costService) {
        this.tripPlannerService = tripPlannerService;
        this.costService = costService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/trip/plan", this::planTrip);
        app.get("/trip/cost", this::estimateCost);
    }

    private void planTrip(Context ctx) {
        TripRequest request;
        try {
            request = ctx.bodyAsClass(TripRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(errorBody("Invalid request body: " + e.getMessage()));
            return;
        }

        try {
            TripRequestValidator.validate(request);
            Itinerary itinerary = tripPlannerService.planTrip(request);
            ctx.json(itinerary);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(errorBody(e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(errorBody("Failed to plan trip: " + e.getMessage()));
        }
    }

    private void estimateCost(Context ctx) {
        String attractionIdsParam = ctx.queryParam("attractionIds");
        String startStation = ctx.queryParam("startStation");

        if (attractionIdsParam == null || startStation == null) {
            ctx.status(400).json(errorBody("'attractionIds' and 'startStation' query params are required"));
            return;
        }

        List<String> ids = Arrays.asList(attractionIdsParam.split(","));

        try {
            CostService.CostEstimate estimate = costService.estimateCost(ids, startStation);
            ctx.json(estimate);
        } catch (Exception e) {
            ctx.status(500).json(errorBody("Cost estimation failed: " + e.getMessage()));
        }
    }

    private record ErrorResponse(String error) {}

    private ErrorResponse errorBody(String message) {
        return new ErrorResponse(message);
    }
}
