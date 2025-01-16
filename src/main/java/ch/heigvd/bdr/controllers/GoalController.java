
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
import io.javalin.http.NotFoundResponse;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;

public class GoalController implements ResourceControllerInterface {
  private final ConcurrentHashMap<Integer, LocalDateTime> goalCache = new ConcurrentHashMap<>();
  private final GoalDAO goalDAO = new GoalDAO();
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/goals", methods = HttpMethod.GET, operationId = "getAllGoals", summary = "Get all goals", description = "Returns a list of all goals.", tags = "Goals", headers = {
      @OpenApiParam(name = "X-User-ID", required = true, type = UUID.class, example = "1"),
  }, responses = {
      @OpenApiResponse(status = "200", description = "List of all goals", content = @OpenApiContent(from = Goal.class)),
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

    List<Goal> goals = goalDAO.getGoalsByUserID(user.getId());
    for (Goal g : goals) {
      if (!goalCache.containsKey(g.getId())) {
        goalCache.put(g.getId(), LocalDateTime.now());
      }
    }
    ctx.json(goals);
  }

  @OpenApi(path = "/goals", methods = HttpMethod.POST, operationId = "createGoal", summary = "Create a new goal", description = "Creates a new goal.", tags = "Goals", requestBody = @OpenApiRequestBody(description = "Goal details", content = @OpenApiContent(from = Goal.class)), responses = {
      @OpenApiResponse(status = "201", description = "Goal created successfully", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    Goal goal = ctx.bodyAsClass(Goal.class);
    goalCache.put(goal.getId(), LocalDateTime.now());
    ctx.header("Last-Modified", LocalDateTime.now().toString());
    ctx.status(201).json(goalDAO.create(goal));
  }

  @OpenApi(path = "/goals/{id}", methods = HttpMethod.GET, operationId = "getGoalById", summary = "Get goal by ID", description = "Fetches a goal by its ID.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Goal found", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "404", description = "Goal not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));

    if (UtilsController.checkModif(ctx, goalCache, id) == -1) {
      return;
    }

    Goal goal = goalDAO.findById(id);

    if (goal != null) {
      UtilsController.sendResponse(ctx, goalCache, goal.getId());
      ctx.json(goal);
    } else {
      throw new NotFoundResponse();
    }
  }

  @OpenApi(path = "/goals/{id}", methods = HttpMethod.PUT, operationId = "updateGoal", summary = "Update goal by ID", description = "Updates a goal by its ID.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated goal details", content = @OpenApiContent(from = Goal.class)), responses = {
      @OpenApiResponse(status = "200", description = "Goal updated successfully", content = @OpenApiContent(from = Goal.class)),
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
      throw new NotFoundResponse();
    }
  }

  @OpenApi(path = "/goals/{id}", methods = HttpMethod.DELETE, operationId = "deleteGoal", summary = "Delete goal by ID", description = "Deletes a goal by its ID.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Goal deleted successfully"),
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
      throw new NotFoundResponse();
    }
  }
}
