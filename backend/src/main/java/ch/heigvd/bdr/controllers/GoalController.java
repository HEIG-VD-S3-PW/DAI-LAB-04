
package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import ch.heigvd.bdr.dao.GoalDAO;
import ch.heigvd.bdr.models.Goal;
import io.javalin.openapi.*;
import java.util.UUID;

public class GoalController implements ResourceControllerInterface {
  private final GoalDAO goalDAO = new GoalDAO();

  @OpenApi(path = "/goals", methods = HttpMethod.GET, operationId = "getAllGoals", summary = "Get all goals", description = "Returns a list of all goals.", tags = "Goals", responses = {
      @OpenApiResponse(status = "200", description = "List of all goals", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) {
    try {
      ctx.json(goalDAO.findAll());
    } catch (Exception e) {
      ctx.status(500).json("Error fetching goals: " + e.getMessage());
    }
  }

  @OpenApi(path = "/goals", methods = HttpMethod.POST, operationId = "createGoal", summary = "Create a new goal", description = "Creates a new goal.", tags = "Goals", requestBody = @OpenApiRequestBody(description = "Goal details", content = @OpenApiContent(from = Goal.class)), responses = {
      @OpenApiResponse(status = "201", description = "Goal created successfully", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) {
    try {
      Goal goal = ctx.bodyAsClass(Goal.class);
      ctx.status(201).json(goalDAO.create(goal));
    } catch (Exception e) {
      ctx.status(400).json("Error creating goal: " + e.getMessage());
    }
  }

  @OpenApi(path = "/goals/{id}", methods = HttpMethod.GET, operationId = "getGoalById", summary = "Get goal by ID", description = "Fetches a goal by its ID.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Goal found", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "404", description = "Goal not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      Goal goal = goalDAO.findById(id);
      if (goal != null) {
        ctx.json(goal);
      } else {
        ctx.status(404).json("Goal not found");
      }
    } catch (NumberFormatException e) {
      ctx.status(400).json("Invalid ID format");
    } catch (Exception e) {
      ctx.status(500).json("Unexpected error: " + e.getMessage());
    }
  }

  @OpenApi(path = "/goals/{id}", methods = HttpMethod.PUT, operationId = "updateGoal", summary = "Update goal by ID", description = "Updates a goal by its ID.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated goal details", content = @OpenApiContent(from = Goal.class)), responses = {
      @OpenApiResponse(status = "200", description = "Goal updated successfully", content = @OpenApiContent(from = Goal.class)),
      @OpenApiResponse(status = "404", description = "Goal not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void update(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      Goal goal = ctx.bodyAsClass(Goal.class);
      goal.setId(id);
      Goal updatedGoal = goalDAO.update(goal);
      if (updatedGoal != null) {
        ctx.json(updatedGoal);
      } else {
        ctx.status(404).json("Goal not found");
      }
    } catch (NumberFormatException e) {
      ctx.status(400).json("Invalid ID format");
    } catch (Exception e) {
      ctx.status(500).json("Unexpected error: " + e.getMessage());
    }
  }

  @OpenApi(path = "/goals/{id}", methods = HttpMethod.DELETE, operationId = "deleteGoal", summary = "Delete goal by ID", description = "Deletes a goal by its ID.", tags = "Goals", pathParams = @OpenApiParam(name = "id", description = "Goal ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Goal deleted successfully"),
      @OpenApiResponse(status = "404", description = "Goal not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void delete(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      if (goalDAO.delete(id)) {
        ctx.status(204);
      } else {
        ctx.status(404).json("Goal not found");
      }
    } catch (NumberFormatException e) {
      ctx.status(400).json("Invalid ID format");
    } catch (Exception e) {
      ctx.status(500).json("Unexpected error: " + e.getMessage());
    }
  }
}
