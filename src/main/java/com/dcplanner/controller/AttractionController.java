package com.dcplanner.controller;

import com.dcplanner.service.AttractionService;
import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * GET /attractions
 *
 * Query params (all optional):
 *   type              - museum, coffee_shop, cinema, landmark (comma-separated)
 *   maxFee            - decimal
 *   accessible        - true/false
 */
public class AttractionController {

    private final AttractionService attractionService;

    public AttractionController(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/attractions", this::getAttractions);
        app.get("/attractions/{id}", this::getAttractionById);
    }

    private void getAttractions(Context ctx) {
        String types = ctx.queryParam("type");
        String maxFeeParam = ctx.queryParam("maxFee");
        String accessibleParam = ctx.queryParam("accessible");

        Double maxFee = null;
        Boolean accessible = null;

        if (maxFeeParam != null) {
            try {
                maxFee = Double.parseDouble(maxFeeParam);
            } catch (NumberFormatException e) {
                ctx.status(400).json(errorBody("maxFee must be a number"));
                return;
            }
        }

        if (accessibleParam != null) {
            accessible = Boolean.parseBoolean(accessibleParam);
        }

        try {
            ctx.json(attractionService.getFiltered(types, maxFee, accessible));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(errorBody(e.getMessage()));
        }
    }

    private void getAttractionById(Context ctx) {
        String id = ctx.pathParam("id");
        attractionService.getById(id)
                .ifPresentOrElse(
                        ctx::json,
                        () -> ctx.status(404).json(errorBody("Attraction not found: " + id))
                );
    }

    private record ErrorResponse(String error) {}

    private ErrorResponse errorBody(String message) {
        return new ErrorResponse(message);
    }
}
