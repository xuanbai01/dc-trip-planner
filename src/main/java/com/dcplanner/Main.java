package com.dcplanner;

import com.dcplanner.controller.AttractionController;
import com.dcplanner.controller.MetroController;
import com.dcplanner.controller.TripController;
import com.dcplanner.repository.AttractionRepository;
import com.dcplanner.repository.MetroRepository;
import com.dcplanner.service.AttractionService;
import com.dcplanner.service.CostService;
import com.dcplanner.service.RouteService;
import com.dcplanner.service.TripPlannerService;
import com.dcplanner.controller.GlobalExceptionHandler;
import io.javalin.Javalin;

public class Main {

    public static void main(String[] args) {

        // --- Repositories (load JSON data) ---
        AttractionRepository attractionRepository = new AttractionRepository();
        MetroRepository metroRepository = new MetroRepository();

        System.out.println("Loaded " + attractionRepository.findAll().size() + " attractions");
        System.out.println("Loaded " + metroRepository.findAllStations().size() + " metro stations");
        System.out.println("Loaded " + metroRepository.findAllEdges().size() + " metro edges");

        // --- Services ---
        AttractionService attractionService = new AttractionService(attractionRepository);
        RouteService routeService = new RouteService(metroRepository);
        CostService costService = new CostService(attractionRepository, routeService);
        TripPlannerService tripPlannerService = new TripPlannerService(
                attractionRepository, attractionService, routeService
        );

        // --- Controllers ---
        AttractionController attractionController = new AttractionController(attractionService);
        MetroController metroController = new MetroController(routeService);
        TripController tripController = new TripController(tripPlannerService, costService);

        // --- Javalin server ---
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(7070);

        // Register global exception handlers
        GlobalExceptionHandler.register(app);

        // Register routes
        attractionController.registerRoutes(app);
        metroController.registerRoutes(app);
        tripController.registerRoutes(app);

        // Health check
        app.get("/health", ctx -> ctx.json("{\"status\":\"ok\"}"));

        System.out.println("DC Trip Planner API running on http://localhost:7070");
        System.out.println("Endpoints:");
        System.out.println("  GET  /health");
        System.out.println("  GET  /attractions");
        System.out.println("  GET  /attractions/{id}");
        System.out.println("  GET  /metro/stations");
        System.out.println("  GET  /metro/route?from={id}&to={id}");
        System.out.println("  POST /trip/plan");
        System.out.println("  GET  /trip/cost?attractionIds={ids}&startStation={id}");
    }
}
