package com.dcplanner.controller;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;

/**
 * Centralized exception handling for all endpoints.
 * Ensures every error returns a consistent JSON shape: {"error": "message"}
 */
public class GlobalExceptionHandler {

    private GlobalExceptionHandler() {}

    public static void register(Javalin app) {

        // Known bad input
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse(e.getMessage()));
        });

        // Malformed JSON body
        app.exception(com.fasterxml.jackson.core.JsonParseException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("Malformed JSON in request body"));
        });

        // JSON maps to wrong types (e.g. string where number expected)
        app.exception(com.fasterxml.jackson.databind.exc.InvalidFormatException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("Invalid field value: " + e.getOriginalMessage()));
        });

        // Unknown enum value (e.g. bad attraction type)
        app.exception(com.fasterxml.jackson.databind.exc.InvalidDefinitionException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("Invalid value: " + e.getMessage()));
        });

        // Catch-all for unexpected server errors
        app.exception(Exception.class, (e, ctx) -> {
            System.err.println("Unhandled exception: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("Internal server error: " + e.getMessage()));
        });

        // 404 for unknown routes
        app.error(404, ctx -> {
            if (ctx.resultString() == null || ctx.resultString().contains("Not Found")) {
                ctx.json(new ErrorResponse("Endpoint not found: " + ctx.method() + " " + ctx.path()));
            }
        });
    }

    public record ErrorResponse(String error) {}
}
