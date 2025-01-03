package ch.heigvd.bdr.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import ch.heigvd.bdr.dao.TaskDAO;
import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.models.*;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import com.google.gson.reflect.TypeToken;

public class TaskController implements ResourceControllerInterface {
  private final TaskDAO taskDAO = new TaskDAO();
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/tasks", methods = HttpMethod.GET, operationId = "getAllTasks", summary = "Get all tasks", description = "Returns a list of all tasks.", tags = "Tasks", headers = {
      @OpenApiParam(name = "X-User-ID", required = true, type = UUID.class, example = "1"),
  }, responses = {
      @OpenApiResponse(status = "200", description = "List of tasks", content = @OpenApiContent(from = Task.class)),
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

    List<Task> tasks = taskDAO.getTasksByUserID(user.getId());
    ctx.json(tasks);

  }

  @OpenApi(path = "/tasks", methods = HttpMethod.POST, operationId = "createTask", summary = "Create a new task", description = "Creates a new task.", tags = "Tasks", requestBody = @OpenApiRequestBody(description = "Task details", content = @OpenApiContent(from = Task.class)), responses = {
      @OpenApiResponse(status = "201", description = "Task created successfully", content = @OpenApiContent(from = Task.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    try {
      Task task = ctx.bodyAsClass(Task.class);
      ctx.status(201).json(taskDAO.create(task));
    } catch (Exception e) {
      ctx.status(400).json(Map.of("message", "Invalid request data."));
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
      ctx.status(404).json(Map.of("message", "Task not found"));
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
      ctx.status(404).json(Map.of("message", "Task not found"));
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
      ctx.status(404).json(Map.of("message", "Task not found"));
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
      ctx.status(404).json(Map.of("message", "Task not found"));
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
      ctx.status(400).json(Map.of("message", "Invalid request data."));
      return;
    }

    int subtaskId = (Integer) requestBody.get("subtaskId");
    boolean required = requestBody.containsKey("required") ? (Boolean) requestBody.get("required") : false;

    // Find the parent task by ID
    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Parent task not found."));
      return;
    }

    // Find the subtask by ID
    Task subtask = taskDAO.findById(subtaskId);
    if (subtask == null) {
      ctx.status(404).json(Map.of("message", "Subtask not found."));
      return;
    }

    // Add the relationship between the task and the subtask, including the required
    // flag
    boolean relationshipAdded = taskDAO.addSubtaskRelationship(task, subtask, required);
    if (relationshipAdded) {
      ctx.status(200).json(Map.of("message", "Subtask relationship added successfully."));
    } else {
      ctx.status(500).json(Map.of("message", "Internal server error."));
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
      ctx.status(400).json(Map.of("message", "Invalid JSON format"));
      return;
    }

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Parent task not found."));
      return;
    }

    Task subtask = taskDAO.findById(subtaskId);
    if (subtask == null) {
      ctx.status(404).json(Map.of("message", "Subtask not found."));
      return;
    }

    boolean updated = taskDAO.updateSubtaskRequiredProperty(task, subtask, required);
    if (updated) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Subtask relationship not found."));
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
      ctx.status(404).json(Map.of("message", "Parent task not found."));
      return;
    }

    Task subtask = taskDAO.findById(subtaskId);
    if (subtask == null) {
      ctx.status(404).json(Map.of("message", "Subtask not found."));
      return;
    }

    boolean deleted = taskDAO.deleteSubtaskRelationship(task, subtask);
    if (deleted) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Subtask relationship not found."));
    }
  }

  @OpenApi(path = "/tasks/{id}/materialNeeds", methods = HttpMethod.POST, operationId = "addMaterialNeeds", summary = "Add a material need to a task", description = "Add the material resources that a task needs to be successfully completed", tags = "Tasks", pathParams = {
          @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

  }, responses = {
          @OpenApiResponse(status = "204", description = "Material need added successfully"),
          @OpenApiResponse(status = "400", description = "Invalid request data"),
          @OpenApiResponse(status = "404", description = "Task not found"),
          @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void addMaterialNeeds(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Task not found"));
      return;
    }

    Material need = MaterialNeed.fromInt(Integer.parseInt(Objects.requireNonNull(ctx.queryParam("type"))));
    int qty = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("qty")));

    boolean success = taskDAO.addMaterialNeed(task, need, qty);
    if (success) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Task not found"));
    }
  }

  @OpenApi(path = "/tasks/{id}/collaboratorNeeds", methods = HttpMethod.POST, operationId = "addCollaboratorNeeds", summary = "Add a collaborator need to a task", description = "Add the collaborator resources that a task needs to be successfully completed", tags = "Tasks", pathParams = {
          @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

  }, responses = {
          @OpenApiResponse(status = "204", description = "Human need added successfully"),
          @OpenApiResponse(status = "400", description = "Invalid request data"),
          @OpenApiResponse(status = "404", description = "Task not found"),
          @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void addCollaboratorNeeds(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Task not found"));
      return;
    }

    UserRole need = CollaboratorNeed.fromInt(Integer.parseInt(Objects.requireNonNull(ctx.queryParam("type"))));
    int qty = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("qty")));

    boolean success = taskDAO.addCollaboratorNeed(task, need, qty);
    if (success) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Task not found"));
    }
  }

  @OpenApi(path = "/tasks/{id}/materialNeeds", methods = HttpMethod.GET, operationId = "getMaterialNeeds", summary = "Get the material need of a task", description = "Get all the material needs from a task given by its id", tags = "Tasks", pathParams = {
          @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

  }, responses = {
          @OpenApiResponse(status = "204", description = "Material need found successfully"),
          @OpenApiResponse(status = "400", description = "Invalid request data"),
          @OpenApiResponse(status = "404", description = "Task not found"),
          @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void getMaterialNeeds(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Task not found"));
      return;
    }

    List<MaterialNeed> result = taskDAO.getTaskMaterialNeeds(task);
    if (!result.isEmpty()) {
      ctx.json(result);
    } else {
      ctx.status(404).json(Map.of("message", "Task not found"));
    }
  }

  @OpenApi(path = "/tasks/{id}/collaboratorNeeds", methods = HttpMethod.GET, operationId = "getCollaboratorNeeds", summary = "Get the collaborator need of a task", description = "Get all the collaborator needs from a task given by its id", tags = "Tasks", pathParams = {
          @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

  }, responses = {
          @OpenApiResponse(status = "204", description = "Collaborator need found successfully"),
          @OpenApiResponse(status = "400", description = "Invalid request data"),
          @OpenApiResponse(status = "404", description = "Task not found"),
          @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void getCollaboratorNeeds(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Task not found"));
      return;
    }

    List<CollaboratorNeed> result = taskDAO.getTaskCollaboratorNeeds(task);
    if (!result.isEmpty()) {
      ctx.json(result);
    } else {
      ctx.status(404).json(Map.of("message", "Task not found"));
    }
  }

  @OpenApi(path = "/tasks/{id}/materialNeeds/{type}/", methods = HttpMethod.DELETE, operationId = "deleteMaterialNeed", summary = "Delete a material need for a task", description = "Delete all the material needs from a task given by its id, only for the \"Material\" type", tags = "Tasks", pathParams = {
          @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

  }, responses = {
          @OpenApiResponse(status = "204", description = "MaterialNeed deleted successfully"),
          @OpenApiResponse(status = "400", description = "Invalid request data"),
          @OpenApiResponse(status = "404", description = "Task not found"),
          @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void deleteMaterialNeed(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Task not found"));
      return;
    }

    Material need = Material.valueOf(ctx.pathParam("type"));

    boolean success = taskDAO.deleteTaskMaterialNeeds(task, need);
    if (success) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Task not found"));
    }
  }

  @OpenApi(path = "/tasks/{id}/collaboratorNeeds/{type}", methods = HttpMethod.DELETE, operationId = "deleteCollaboratorNeed", summary = "Delete a collaborator need for a task", description = "Delete all the collaborator needs from a task given by its id, only for the \"UserRole\" type", tags = "Tasks", pathParams = {
          @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

  }, responses = {
          @OpenApiResponse(status = "204", description = "CollaboratorNeed deleted successfully"),
          @OpenApiResponse(status = "400", description = "Invalid request data"),
          @OpenApiResponse(status = "404", description = "Task not found"),
          @OpenApiResponse(status = "500", description = "Internal server error")
  })
  public void deleteCollaboratorNeed(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int taskId = Integer.parseInt(ctx.pathParam("id"));

    Task task = taskDAO.findById(taskId);
    if (task == null) {
      ctx.status(404).json(Map.of("message", "Task not found"));
      return;
    }

    UserRole need = UserRole.valueOf(ctx.pathParam("type"));

    boolean success = taskDAO.deleteTaskCollaboratorNeeds(task, need);
    if (success) {
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "Task not found"));
    }
  }
}
