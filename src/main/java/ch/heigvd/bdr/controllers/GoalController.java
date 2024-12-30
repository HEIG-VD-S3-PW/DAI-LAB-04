
package ch.heigvd.bdr.controllers;

import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.models.Team;
import ch.heigvd.bdr.models.User;
import io.javalin.http.Context;
import ch.heigvd.bdr.dao.GoalDAO;
import ch.heigvd.bdr.models.Goal;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GoalController implements ResourceControllerInterface {
  private final GoalDAO goalDAO = new GoalDAO();
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/goals", methods = HttpMethod.GET, operationId = "getAllGoals", summary = "Get all goals", description = "Returns a list of all goals.", tags = "Goals", responses = {
      @OpenApiResponse(status = "200", description = "List of all goals", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    // ctx.json(goalDAO.findAll());

    int userId = Integer.parseInt(Objects.requireNonNull(ctx.header("X-User-ID")));

    if (userId == 0) {
      ctx.status(400).json(Map.of("message", "Missing X-User-ID header"));
      return;
    }

    User user = userDAO.findById(userId);
    if (user == null) {
      ctx.status(404).json(Map.of("message", "User not found"));
      return;
    }

    List<Goal> goals = userDAO.getGoals(user.getId());
    ctx.json(goals);
  }

  @OpenApi(path = "/goals", methods = HttpMethod.POST, operationId = "createGoal", summary = "Create a new goal", description = "Creates a new goal.", tags = "Goals", requestBody = @OpenApiRequestBody(description = "Goal details", content = @OpenApiContent(from = Goal.class)), responses = {
      @OpenApiResponse(status = "201", description = "Goal created successfully", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    Goal goal = ctx.bodyAsClass(Goal.class);
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
    Goal goal = goalDAO.findById(id);
    if (goal != null) {
      ctx.json(goal);
    } else {
      ctx.status(404).json(Map.of("message", "Goal not found"));
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
      ctx.json(updatedGoal);
    } else {
      ctx.status(404).json(Map.of("message", "Goal not found"));
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
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Goal not found"));
    }
  }
}
