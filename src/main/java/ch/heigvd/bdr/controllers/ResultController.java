
package ch.heigvd.bdr.controllers;

import ch.heigvd.bdr.dao.UserDAO;
import io.javalin.http.Context;
import ch.heigvd.bdr.dao.ResultDAO;
import ch.heigvd.bdr.models.*;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ResultController implements ResourceControllerInterface {
  private final ResultDAO resultDAO = new ResultDAO();
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/results", methods = HttpMethod.GET, operationId = "getAllResults", summary = "Get all results", description = "Returns a list of all results.", tags = "Results", headers = {
      @OpenApiParam(name = "X-User-ID", required = true, type = UUID.class, example = "1"),
  }, responses = {
      @OpenApiResponse(status = "200", description = "List of all results", content = @OpenApiContent(from = Result.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
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

    List<Result> results = resultDAO.getResultsByUserID(user.getId());
    ctx.json(results);
  }

  @OpenApi(path = "/results", methods = HttpMethod.POST, operationId = "createResult", summary = "Create a new result", description = "Creates a new result.", tags = "Results", requestBody = @OpenApiRequestBody(description = "Result details", content = @OpenApiContent(from = Result.class)), responses = {
      @OpenApiResponse(status = "201", description = "Result created successfully", content = @OpenApiContent(from = Result.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    Result result = ctx.bodyAsClass(Result.class);
    ctx.status(201).json(resultDAO.create(result));
  }

  @OpenApi(path = "/results/{id}", methods = HttpMethod.GET, operationId = "getResultById", summary = "Get result by ID", description = "Fetches a result by its ID.", tags = "Results", pathParams = @OpenApiParam(name = "id", description = "Result ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Result found", content = @OpenApiContent(from = Result.class)),
      @OpenApiResponse(status = "404", description = "Result not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Result result = resultDAO.findById(id);
    if (result != null) {
      ctx.json(result);
    } else {
      ctx.status(404).json(Map.of("message", "Result not found"));
    }
  }

  @OpenApi(path = "/results/{id}", methods = HttpMethod.PUT, operationId = "updateResult", summary = "Update result by ID", description = "Updates a result by its ID.", tags = "Results", pathParams = @OpenApiParam(name = "id", description = "Result ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated result details", content = @OpenApiContent(from = Result.class)), responses = {
      @OpenApiResponse(status = "200", description = "Result updated successfully", content = @OpenApiContent(from = Result.class)),
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
      ctx.json(updatedResult);
    } else {
      ctx.status(404).json(Map.of("message", "Result not found"));
    }
  }

  @OpenApi(path = "/results/{id}", methods = HttpMethod.DELETE, operationId = "deleteResult", summary = "Delete result by ID", description = "Deletes a result by its ID.", tags = "Results", pathParams = @OpenApiParam(name = "id", description = "Result ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "204", description = "Result deleted successfully"),
      @OpenApiResponse(status = "404", description = "Result not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    if (resultDAO.delete(id)) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Result not found"));
    }
  }
}
