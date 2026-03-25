package com.dcplanner.controller;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.service.RouteService;
import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * GET /metro/route?from={stationId}&to={stationId}
 * GET /metro/stations
 */
public class MetroController {

    private final RouteService routeService;

    public MetroController(RouteService routeService) {
        this.routeService = routeService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/metro/route", this::getRoute);
        app.get("/metro/stations", this::getStations);
    }

    private void getRoute(Context ctx) {
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");

        if (from == null || to == null) {
            ctx.status(400).json(errorBody("Both 'from' and 'to' query params are required"));
            return;
        }

        Dijkstra.RouteResult result = routeService.getRoute(from, to);

        if (!result.isFound()) {
            ctx.status(404).json(errorBody("No route found between " + from + " and " + to));
            return;
        }

        ctx.json(new RouteResponse(
                result.getStationPath(),
                result.getTotalTravelTimeMinutes(),
                result.getTotalFare()
        ));
    }

    private void getStations(Context ctx) {
        ctx.json(routeService.getGraph().getAllStationIds());
    }

    private record ErrorResponse(String error) {}
    private record RouteResponse(java.util.List<String> stations, int travelTimeMinutes, double fare) {}

    private ErrorResponse errorBody(String message) {
        return new ErrorResponse(message);
    }
}
