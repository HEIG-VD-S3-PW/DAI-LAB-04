package ch.heigvd.bdr.controllers;


import io.javalin.http.Context;
import io.javalin.openapi.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import ch.heigvd.bdr.DatabaseUtil;

public class HealthController {

    @OpenApi(
            path = "/health",
            methods = HttpMethod.GET,
            summary = "Check API health",
            description = "Checks if the API and database connection are working properly",
            tags = {"Health"},
            responses = {
                    @OpenApiResponse(status = "200", description = "API is healthy"),
                    @OpenApiResponse(status = "503", description = "API is unhealthy")
            }
    )
    public void checkHealth(Context ctx) {
        try {
            try (Connection conn = DatabaseUtil.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    ctx.status(200).json(Map.of("status", "UP", "database", "UP", "timestamp", System.currentTimeMillis()));
                } else {
                    throw new SQLException("Database connection is closed");
                }
            }
        } catch (Exception e) {
            ctx.status(503).json(Map.of("status", "DOWN", "database", "DOWN", "error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }
}