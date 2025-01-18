
package ch.heigvd.bdr.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ch.heigvd.bdr.dao.GoalDAO;
import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.misc.StringHelper;
import ch.heigvd.bdr.models.*;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;

public class GoalController implements ResourceControllerInterface {
  // Manages the cache for all goals
  private final ConcurrentHashMap<Integer, LocalDateTime> goalCache = new ConcurrentHashMap<>();
  private final GoalDAO goalDAO = new GoalDAO();
  private final UserDAO userDAO = new UserDAO();

  /**
   * Get all the goals
   * @param ctx: context to use
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws IOException
   */
    @OpenApi(path = "/goals", methods = HttpMethod.GET, operationId = "getAllGoals", summary = "Get all goals for a given user", description = "Returns a list of all goals. Supports RFC 1123 formatted If-Modified-Since header for cache validation.", tags = "Goals", headers = {
      @OpenApiParam(name = "X-User-ID", required = true, type = UUID.class, example = "1"),
      @OpenApiParam(name = "If-Modified-Since", required = false, description = "RFC 1123 formatted timestamp for conditional request")
  }, responses = {
      @OpenApiResponse(status = "200", description = "List of goals", content = @OpenApiContent(from = Goal[].class), headers = {
          @OpenApiParam(name = "Last-Modified", description = "RFC 1123 formatted timestamp of last modification")
      }),
      @OpenApiResponse(status = "304", description = "Resource not modified since If-Modified-Since timestamp"),
      @OpenApiResponse(status = "400", description = "Invalid If-Modified-Since header format"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    String userId = ctx.header("X-User-ID");
    if (userId == null || !StringHelper.isInteger(userId)) {
      ctx.status(400).json(Map.of("message", "Missing X-User-ID header"));
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
      // If we have a cache entry for this user's goal list
      if (goalCache.containsKey(id)) {
        // Check if the list has been modified since the client's last fetch
        if (UtilsController.isModifiedSince(goalCache.get(id), lastKnownModification)) {
          ctx.status(304).json(Map.of("message", "Not modified"));
          return;
        }
      }
    }

    List<Goal> goals = goalDAO.getGoalsByUserID(user.getId());

    LocalDateTime now = LocalDateTime.now();
    goalCache.put(id, now);

    ctx.header("Last-Modified", now.toString());

    ctx.json(goals);
  }

  /**
   * Create a goal
   * @param ctx: context to use
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws IOException
   */
  @OpenApi(path = "/goals", methods = HttpMethod.POST, operationId = "createGoal", summary = "Create a new goal", description = "Creates a new goal and sets its Last-Modified timestamp in the cache.", tags = "Goals", requestBody = @OpenApiRequestBody(description = "Goal details", content = @OpenApiContent(from = Goal.class)), responses = {
      @OpenApiResponse(status = "201", description = "Goal created successfully", content = @OpenApiContent(from = Goal.class), headers = {
          @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted creation timestamp")
      }),
      @OpenApiResponse(status = "400", description = "Bad request"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    Goal goal = ctx.bodyAsClass(Goal.class);
    goalCache.put(goal.getId(), LocalDateTime.now());
    ctx.header("Last-Modified", LocalDateTime.now().toString());
    ctx.status(201).json(goalDAO.create(goal));
  }

  /**
   * Show a specific goal
   * @param ctx: context to use
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws IOException
   */
  @OpenApi(path = "/goals/{id}", methods = HttpMethod.GET, operationId = "getGoalById", summary = "Get goal by ID", description = """
      Fetches a goal by its ID. Supports conditional retrieval using If-Modified-Since header.
      The timestamp comparison ignores nanoseconds for cache validation.
      Returns 304 Not Modified if the resource hasn't changed since the specified timestamp.
      """, tags = "Goals", headers = {
      @OpenApiParam(name = "If-Modified-Since", required = false, description = "RFC 1123 formatted timestamp. Returns 304 if resource unchanged since this time.")
  }, pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Goal found", content = @OpenApiContent(from = Goal.class), headers = {
          @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted timestamp of last modification")
      }),
      @OpenApiResponse(status = "304", description = "Goal not modified since If-Modified-Since timestamp"),
      @OpenApiResponse(status = "400", description = "Invalid If-Modified-Since header format"),
      @OpenApiResponse(status = "404", description = "Goal not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));

    UtilsController.checkModif(ctx, goalCache, id);

    Goal goal = goalDAO.findById(id);

    if (goal != null) {
      UtilsController.sendResponse(ctx, goalCache, goal.getId());
      ctx.json(goal);
    } else {
      ctx.status(404).json(Map.of("message", "Goal not found"));
    }
  }

  /**
   * Update a goal
   * @param ctx: context to use
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws IOException
   */
  @OpenApi(path = "/goals/{id}", methods = HttpMethod.PUT, operationId = "updateGoal", summary = "Update goal by ID", description = "Updates a goal by its ID and updates its Last-Modified timestamp in the cache.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated goal details", content = @OpenApiContent(from = Goal.class)), responses = {
      @OpenApiResponse(status = "200", description = "Goal updated successfully", content = @OpenApiContent(from = Goal.class), headers = {
          @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted update timestamp")
      }),
      @OpenApiResponse(status = "400", description = "Bad request"),
      @OpenApiResponse(status = "404", description = "Goal not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void update(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Goal goal = ctx.bodyAsClass(Goal.class);
    goal.setId(id);
    Goal updatedGoal = goalDAO.update(goal);
    if (updatedGoal != null) {
      goalCache.put(id, LocalDateTime.now());
      ctx.header("Last-Modified", LocalDateTime.now().toString());
      ctx.json(updatedGoal);
    } else {
      ctx.status(404).json(Map.of("message", "Goal not found"));
    }
  }

  /**
   * Delete a goal
   * @param ctx: context to use
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws IOException
   */
  @OpenApi(path = "/goals/{id}", methods = HttpMethod.DELETE, operationId = "deleteGoal", summary = "Delete goal by ID", description = "Deletes a goal by its ID and removes its entry from the cache.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "204", description = "Goal deleted successfully"),
      @OpenApiResponse(status = "404", description = "Goal not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    if (goalDAO.delete(id)) {
      goalCache.remove(id);
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Goal not found"));
    }
  }
}
