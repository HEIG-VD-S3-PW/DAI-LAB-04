package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import ch.heigvd.bdr.dao.TaskDAO;
import ch.heigvd.bdr.models.Task;
import io.javalin.openapi.*;

public class TaskController implements ResourceControllerInterface {
  private final TaskDAO taskDAO = new TaskDAO();

  @OpenApi(path = "/tasks", methods = HttpMethod.GET, operationId = "getAllTasks", summary = "Get all tasks", description = "Returns a list of all tasks.", tags = "Tasks", responses = {
      @OpenApiResponse(status = "200", description = "List of tasks", content = @OpenApiContent(from = Task.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    ctx.json(taskDAO.findAll());
  }

  @OpenApi(path = "/tasks", methods = HttpMethod.GET, operationId = "getAllTasks", summary = "Get all tasks", description = "Returns a list of all tasks.", tags = "Tasks", responses = {
      @OpenApiResponse(status = "200", description = "List of tasks", content = @OpenApiContent(from = Task.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    try {
      Task task = ctx.bodyAsClass(Task.class);
      ctx.status(201).json(taskDAO.create(task));
    } catch (Exception e) {
      ctx.status(400).json("Error creating task: " + e.getMessage());
    }
  }

  @OpenApi(path = "/tasks/{id}", methods = HttpMethod.GET, operationId = "getTaskById", summary = "Get task by ID", description = "Fetches a task by its ID.", tags = "Tasks", pathParams = @OpenApiParam(name = "id", description = "Task ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Task found", content = @OpenApiContent(from = Task.class)),
      @OpenApiResponse(status = "404", description = "Task not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Task task = taskDAO.findById(id);
    if (task != null) {
      ctx.json(task);
    } else {
      ctx.status(404).json("Task not found");
    }
  }

  @OpenApi(path = "/tasks/{id}", methods = HttpMethod.PUT, operationId = "updateTask", summary = "Update task by ID", description = "Updates a task by its ID.", tags = "Tasks", pathParams = @OpenApiParam(name = "id", description = "Task ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated task details", content = @OpenApiContent(from = Task.class)), responses = {
      @OpenApiResponse(status = "200", description = "Task updated successfully", content = @OpenApiContent(from = Task.class)),
      @OpenApiResponse(status = "404", description = "Task not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void update(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Task task = ctx.bodyAsClass(Task.class);

    task.setId(id);
    Task updatedTask = taskDAO.update(task);
    if (updatedTask != null) {
      ctx.json(updatedTask);
    } else {
      ctx.status(404).json("Task not found");
    }
  }

  @OpenApi(path = "/tasks/{id}", methods = HttpMethod.DELETE, operationId = "deleteTask", summary = "Delete task by ID", description = "Deletes a task by its ID.", tags = "Tasks", pathParams = @OpenApiParam(name = "id", description = "Task ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Task deleted successfully"),
      @OpenApiResponse(status = "404", description = "Task not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    if (taskDAO.delete(id)) {
      ctx.status(204);
    } else {
      ctx.status(404).json("Task not found");
    }
  }
}
