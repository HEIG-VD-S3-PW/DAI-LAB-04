package ch.heigvd.bdr.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import ch.heigvd.bdr.dao.TaskDAO;
import ch.heigvd.bdr.models.Task;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;

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

  @OpenApi(path = "/tasks/{id}/substasks", methods = HttpMethod.GET, operationId = "getSubstasks", summary = "Get all subtasks of a given task by ID", description = "Fetches all substasks of a given a task by its ID.", tags = "Tasks", pathParams = @OpenApiParam(name = "id", description = "Task ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Task found", content = @OpenApiContent(from = Task.class)),
      @OpenApiResponse(status = "404", description = "Task not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  public void subtasks(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Task task = taskDAO.findById(id);
    if (task == null) {
      ctx.status(404).json("Task not found");
      return;
    }

    ctx.json(taskDAO.getSubtasks(task));
  }

  @OpenApi(path = "/tasks/{id}/subtasks", methods = HttpMethod.POST, operationId = "addSubtask", summary = "Add a subtask to a task", description = "Adds a subtask to a task and optionally sets the 'required' flag.", tags = "Tasks", pathParams = {
      @OpenApiParam(name = "id", description = "The ID of the parent task", required = true, type = Integer.class)
  }, requestBody = @OpenApiRequestBody(description = "Subtask ID and optional required flag", content = @OpenApiContent(from = HashMap.class)), responses = {
      @OpenApiResponse(status = "200", description = "Subtask relationship added successfully"),
      @OpenApiResponse(status = "400", description = "Invalid request data"),
      @OpenApiResponse(status = "404", description = "Task or subtask not found"),
      @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void addSubtaskRelationship(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));

    // Get the request body as a HashMap and extract required and subtaskId
    HashMap<String, Object> requestBody = ctx.bodyAsClass(HashMap.class);

    if (!requestBody.containsKey("subtaskId")) {
      ctx.status(400).json("Subtask ID is required.");
      return;
    }

    int subtaskId = (Integer) requestBody.get("subtaskId");
    boolean required = requestBody.containsKey("required") ? (Boolean) requestBody.get("required") : false;

    // Find the parent task by ID
    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json("Parent task not found.");
      return;
    }

    // Find the subtask by ID
    Task subtask = taskDAO.findById(subtaskId);
    if (subtask == null) {
      ctx.status(404).json("Subtask not found.");
      return;
    }

    // Add the relationship between the task and the subtask, including the required
    // flag
    boolean relationshipAdded = taskDAO.addSubtaskRelationship(task, subtask, required);
    if (relationshipAdded) {
      ctx.status(200).json("Subtask relationship added successfully.");
    } else {
      ctx.status(500).json("Failed to add subtask relationship.");
    }
  }

  @OpenApi(path = "/tasks/{id}/subtasks/{subtaskId}/", methods = HttpMethod.PATCH, operationId = "patchSubtaskRequired", summary = "Update the 'required' property of a subtask relationship", description = "Updates the 'required' property of the relationship between a task and a subtask.", tags = "Tasks", pathParams = {
      @OpenApiParam(name = "id", description = "The ID of the parent task", required = true, type = Integer.class),
      @OpenApiParam(name = "subtaskId", description = "The ID of the subtask", required = true, type = Integer.class)
  }, requestBody = @OpenApiRequestBody(description = "The required property to be updated", content = @OpenApiContent(from = Boolean.class)), responses = {
      @OpenApiResponse(status = "200", description = "Subtask relationship updated successfully"),
      @OpenApiResponse(status = "400", description = "Invalid JSON format"),
      @OpenApiResponse(status = "404", description = "Task or subtask not found"),
      @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void updateSubtaskRequired(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));
    int subtaskId = Integer.parseInt(ctx.pathParam("subtaskId"));

    boolean required = false;
    HashMap<String, Boolean> r = ctx.bodyAsClass(HashMap.class);
    if (r.containsKey("required")) {
      required = r.get("required");
    } else {
      ctx.status(400).json("Invalid JSON format.");
      return;
    }

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json("Parent task not found.");
      return;
    }

    Task subtask = taskDAO.findById(subtaskId);
    if (subtask == null) {
      ctx.status(404).json("Subtask not found.");
      return;
    }

    boolean updated = taskDAO.updateSubtaskRequiredProperty(task, subtask, required);
    if (updated) {
      ctx.status(204);
    } else {
      ctx.status(404).json("Subtask relationship not found.");
    }
  }

  @OpenApi(path = "/tasks/{id}/subtasks/{subtaskId}", methods = HttpMethod.DELETE, operationId = "deleteSubtask", summary = "Delete a subtask from a specific task", description = "Deletes the relationship between a specific task and its subtask by their respective IDs. This operation removes the subtask from the parent task.", tags = "Tasks", pathParams = {
      @OpenApiParam(name = "id", description = "The unique identifier of the task to which the subtask belongs.", required = true, type = Integer.class),
      @OpenApiParam(name = "subtaskId", description = "The unique identifier of the subtask to be deleted from the task.", required = true, type = Integer.class)
  }, responses = {
      @OpenApiResponse(status = "204", description = "Subtask relationship deleted successfully, no content returned."),
      @OpenApiResponse(status = "404", description = "Either the task or subtask with the provided ID was not found."),
      @OpenApiResponse(status = "500", description = "Internal server error occurred while processing the request.")
  })
  public void deleteSubtaskRelationship(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));
    int subtaskId = Integer.parseInt(ctx.pathParam("subtaskId"));

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json("Parent task not found.");
      return;
    }

    Task subtask = taskDAO.findById(subtaskId);
    if (subtask == null) {
      ctx.status(404).json("Subtask not found.");
      return;
    }

    boolean deleted = taskDAO.deleteSubtaskRelationship(task, subtask);
    if (deleted) {
      ctx.status(204);
    } else {
      ctx.status(404).json("Subtask relationship not found.");
    }
  }

}
