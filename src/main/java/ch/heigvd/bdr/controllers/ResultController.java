
package ch.heigvd.bdr.controllers;

import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.misc.StringHelper;
import io.javalin.http.Context;
import ch.heigvd.bdr.dao.ResultDAO;
import ch.heigvd.bdr.models.*;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ResultController implements ResourceControllerInterface {
    // Manages cache for all results
    private final ConcurrentHashMap<Integer, LocalDateTime> resultCache = new ConcurrentHashMap<>();
    private final ResultDAO resultDAO = new ResultDAO();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Show all results
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/results", methods = HttpMethod.GET, operationId = "getAllResults", summary = "Get all results for a given user", description = "Returns a list of all results for a given user. Supports RFC 1123 formatted If-Modified-Since header for cache validation.", tags = "Results", headers = {
            @OpenApiParam(name = "X-User-ID", required = true, type = UUID.class, example = "1"),
            @OpenApiParam(name = "If-Modified-Since", required = false, description = "RFC 1123 formatted timestamp for conditional request")
    }, responses = {
            @OpenApiResponse(status = "200", description = "List of results", content = @OpenApiContent(from = Result[].class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "RFC 1123 formatted timestamp of last modification")
            }),
            @OpenApiResponse(status = "304", description = "Resource not modified since If-Modified-Since timestamp"),
            @OpenApiResponse(status = "400", description = "Invalid If-Modified-Since header format"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        // Validate user ID header
        String userId = ctx.header("X-User-ID");
        if (userId == null || !StringHelper.isInteger(userId)) {
            ctx.status(400).json(Map.of("message", "Missing header X-User-ID"));
            return;
        }

        int id = Integer.parseInt(userId);
        User user = userDAO.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("message", "User not found"));
            return;
        }

        // Check if we have a valid If-Modified-Since header
        LocalDateTime lastKnownModification = UtilsController.getLastModifiedHeader(ctx);
        if (lastKnownModification != null) {
            // If we have a cache entry for this user's task list
            if (resultCache.containsKey(id)) {
                // Check if the list has been modified since the client's last fetch
                if (UtilsController.isModifiedSince(resultCache.get(id), lastKnownModification)) {
                    ctx.status(304).json(Map.of("message", "Not modified"));
                    return;
                }
            }
        }

        List<Result> results = resultDAO.getResultsByUserID(user.getId());

        LocalDateTime now = LocalDateTime.now();
        resultCache.put(id, now);

        ctx.header("Last-Modified", now.toString());

        ctx.json(results);
    }

    /**
     * Create a result
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/results", methods = HttpMethod.POST, operationId = "createResult", summary = "Create a new result", description = "Creates a new result.", tags = "Results", requestBody = @OpenApiRequestBody(description = "Result details", content = @OpenApiContent(from = Result.class)), responses = {
            @OpenApiResponse(status = "201", description = "Goal created successfully", content = @OpenApiContent(from = Result.class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted creation timestamp")
            }),
            @OpenApiResponse(status = "400", description = "Bad request"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        Result result = ctx.bodyAsClass(Result.class);
        resultCache.put(result.getId(), LocalDateTime.now());
        ctx.header("Last-Modified", LocalDateTime.now().toString());
        ctx.status(201).json(resultDAO.create(result));
    }

    /**
     * Show a specific result
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/results/{id}", methods = HttpMethod.GET, operationId = "getResultById", summary = "Get result by ID", description = """
            Fetches a result by its ID. Supports conditional retrieval using If-Modified-Since header.
            The timestamp comparison ignores nanoseconds for cache validation.
            Returns 304 Not Modified if the resource hasn't changed since the specified timestamp.
            """, tags = "Results", headers = {
            @OpenApiParam(name = "If-Modified-Since", required = false, description = "RFC 1123 formatted timestamp. Returns 304 if resource unchanged since this time.")
    }, pathParams = @OpenApiParam(name = "id", description = "Result ID", required = true, type = UUID.class), responses = {
            @OpenApiResponse(status = "200", description = "Result found", content = @OpenApiContent(from = Result.class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted timestamp of last modification")
            }),
            @OpenApiResponse(status = "304", description = "Result not modified since If-Modified-Since timestamp"),
            @OpenApiResponse(status = "400", description = "Invalid If-Modified-Since header format"),
            @OpenApiResponse(status = "404", description = "Result not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));

        UtilsController.checkModif(ctx, resultCache, id);

        Result result = resultDAO.findById(id);

        if (result != null) {
            UtilsController.sendResponse(ctx, resultCache, result.getId());
            ctx.json(result);
        } else {
            ctx.status(404).json(Map.of("message", "Result not found"));
        }
    }

    /**
     * Update a result
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/results/{id}", methods = HttpMethod.PUT, operationId = "updateResult", summary = "Update result by ID", description = "Updates a result by its ID.", tags = "Results", pathParams = @OpenApiParam(name = "id", description = "Result ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated result details", content = @OpenApiContent(from = Result.class)), responses = {
            @OpenApiResponse(status = "200", description = "Result updated successfully", content = @OpenApiContent(from = Result.class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted update timestamp")
            }),
            @OpenApiResponse(status = "404", description = "Result not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void update(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Result result = ctx.bodyAsClass(Result.class);
        result.setId(id);
        Result updatedResult = resultDAO.update(result);
        if (updatedResult != null) {
            resultCache.put(id, LocalDateTime.now());
            ctx.header("Last-Modified", LocalDateTime.now().toString());
            ctx.json(updatedResult);
        } else {
            ctx.status(404).json(Map.of("message", "Result not found"));
        }
    }

    /**
     * Delete a result
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/results/{id}", methods = HttpMethod.DELETE, operationId = "deleteResult", summary = "Delete result by ID", description = "Deletes a result by its ID.", tags = "Results", pathParams = @OpenApiParam(name = "id", description = "Result ID", required = true, type = UUID.class), responses = {
            @OpenApiResponse(status = "204", description = "Result deleted successfully"),
            @OpenApiResponse(status = "404", description = "Result not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));
        if (resultDAO.delete(id)) {
            resultCache.remove(id);
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("message", "Result not found"));
        }
    }
}
