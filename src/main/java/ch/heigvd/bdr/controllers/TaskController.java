package ch.heigvd.bdr.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ch.heigvd.bdr.dao.TaskDAO;
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

public class TaskController implements ResourceControllerInterface {
    // Manages cache for all tasks
    private final ConcurrentHashMap<Integer, LocalDateTime> taskCache = new ConcurrentHashMap<>();
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Show all tasks
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/tasks", methods = HttpMethod.GET, operationId = "getAllTasks", summary = "Get all tasks for a given user", description = "Returns a list of all tasks.", tags = "Tasks", headers = {
            @OpenApiParam(name = "X-User-ID", required = true, type = UUID.class, example = "1"),
    }, responses = {
            @OpenApiResponse(status = "200", description = "List of tasks", content = @OpenApiContent(from = Task[].class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "RFC 1123 formatted timestamp of last modification")
            }),
            @OpenApiResponse(status = "304", description = "Resource not modified since If-Modified-Since timestamp"),
            @OpenApiResponse(status = "400", description = "Invalid format / missing required argument"),
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
            if (taskCache.containsKey(id)) {
                // Check if the list has been modified since the client's last fetch
                if (UtilsController.isModifiedSince(taskCache.get(id), lastKnownModification)) {
                    ctx.status(304).json(Map.of("message", "Not modified"));
                    return;
                }
            }
        }

        List<Task> tasks = taskDAO.getTasksByUserID(user.getId());

        LocalDateTime now = LocalDateTime.now();
        taskCache.put(id, now);

        ctx.header("Last-Modified", now.toString());

        ctx.json(tasks);
    }

    /**
     * Create a new task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/tasks", methods = HttpMethod.POST, operationId = "createTask", summary = "Create a new task", description = "Creates a new task.", tags = "Tasks", requestBody = @OpenApiRequestBody(description = "Task details", content = @OpenApiContent(from = Task.class)), responses = {
            @OpenApiResponse(status = "201", description = "Task created successfully", content = @OpenApiContent(from = Task.class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted creation timestamp")
            }),
            @OpenApiResponse(status = "400", description = "Bad request, missing required arguments"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        try {
            Task task = ctx.bodyAsClass(Task.class);
            taskCache.put(task.getId(), LocalDateTime.now());
            ctx.header("Last-Modified", LocalDateTime.now().toString());
            ctx.status(201).json(taskDAO.create(task));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("message", "Invalid request data."));
        }
    }

    /**
     * Show a specific task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/tasks/{id}", methods = HttpMethod.GET, operationId = "getTaskById", summary = "Get task by ID", description = """
            Fetches a task by its ID. Supports conditional retrieval using If-Modified-Since header.
            The timestamp comparison ignores nanoseconds for cache validation.
            Returns 304 Not Modified if the resource hasn't changed since the specified timestamp.
            """, tags = "Tasks", headers = {
            @OpenApiParam(name = "If-Modified-Since", required = false, description = "RFC 1123 formatted timestamp. Returns 304 if resource unchanged since this time.")
    }, pathParams = @OpenApiParam(name = "id", description = "Task ID", required = true, type = UUID.class), responses = {
            @OpenApiResponse(status = "200", description = "Task found", content = @OpenApiContent(from = Task.class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted timestamp of last modification")
            }),
            @OpenApiResponse(status = "304", description = "Task not modified since If-Modified-Since timestamp"),
            @OpenApiResponse(status = "400", description = "Invalid If-Modified-Since header format"),
            @OpenApiResponse(status = "404", description = "Task not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));

        UtilsController.checkModif(ctx, taskCache, id);

        Task task = taskDAO.findById(id);

        if (task != null) {
            UtilsController.sendResponse(ctx, taskCache, task.getId());
            ctx.json(task);
        } else {
            ctx.status(404).json(Map.of("message", "Task not found"));
        }
    }

    /**
     * Update a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/tasks/{id}", methods = HttpMethod.PUT, operationId = "updateTask", summary = "Update task by ID", description = "Updates a task by its ID and updates its Last-Modified timestamp in the cache.", tags = "Tasks", pathParams = @OpenApiParam(name = "id", description = "Task ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated task details", content = @OpenApiContent(from = Task.class)), responses = {
            @OpenApiResponse(status = "200", description = "Task updated successfully", content = @OpenApiContent(from = Task.class), headers = {
                    @OpenApiParam(name = "Last-Modified", description = "ISO-8601 formatted update timestamp")
            }),
            @OpenApiResponse(status = "400", description = "Bad request"),
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
            taskCache.put(id, LocalDateTime.now());
            ctx.header("Last-Modified", LocalDateTime.now().toString());
            ctx.json(updatedTask);
        } else {
            ctx.status(404).json(Map.of("message", "Task not found"));
        }
    }

    /**
     * Delete a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/tasks/{id}", methods = HttpMethod.DELETE, operationId = "deleteTask", summary = "Delete task by ID", description = "Deletes a task by its ID and removes its entry from the cache.", tags = "Tasks", pathParams = @OpenApiParam(name = "id", description = "Task ID", required = true, type = UUID.class), responses = {
            @OpenApiResponse(status = "204", description = "Task deleted successfully"),
            @OpenApiResponse(status = "404", description = "Task not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));
        if (taskDAO.delete(id)) {
            taskCache.remove(id);
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("message", "Task not found"));
        }
    }

    /**
     * Get all subtasks of a given task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

    /**
     * Add a subtask to a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

    /**
     * Update a subtask
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

        boolean required;
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

    /**
     * Remove a subtask of a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

    /**
     * Add material needs to a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

        MaterialNeed materialNeed = ctx.bodyAsClass(MaterialNeed.class);
        boolean success = taskDAO.addMaterialNeed(taskId, materialNeed);

        if (success) {
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("message", "Task not found"));
        }
    }

    /**
     * Add collaborator needs to a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

        CollaboratorNeed collaboratorNeed = ctx.bodyAsClass(CollaboratorNeed.class);
        boolean success = taskDAO.addCollaboratorNeed(taskId, collaboratorNeed);

        if (success) {
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("message", "Task not found"));
        }
    }

    /**
     * Update the material needs of a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/tasks/{id}/materialNeeds/{type}", methods = HttpMethod.PATCH, operationId = "updateMaterialNeed", summary = "Update the quantity of a material need for a task", description = "Update the quantity of a material need for a task given by its id, only for the \"Material\" type", tags = "Tasks", pathParams = {
            @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

    }, responses = {
            @OpenApiResponse(status = "204", description = "Material need updated successfully"),
            @OpenApiResponse(status = "400", description = "Invalid request data"),
            @OpenApiResponse(status = "404", description = "Task not found"),
            @OpenApiResponse(status = "500", description = "Internal server error")
    })
    public void updateMaterialNeed(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int taskId = Integer.parseInt(ctx.pathParam("id"));

        Task task = taskDAO.findById(taskId);
        if (task == null) {
            ctx.status(404).json(Map.of("message", "Task not found"));
            return;
        }

        MaterialNeed materialNeed = ctx.bodyAsClass(MaterialNeed.class);

        boolean success = taskDAO.updateMaterialNeed(taskId, materialNeed);
        if (success) {
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("message", "Task not found"));
        }
    }

    /**
     * Update the collaborator needs of a task
     *
     * @param ctx
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/tasks/{id}/collaboratorNeeds/{type}", methods = HttpMethod.PATCH, operationId = "updateCollaboratorNeed", summary = "Update the quantity of a collaborator need for a task", description = "Update the quantity of a collaborator need for a task given by its id, only for the \"UserRole\" type", tags = "Tasks", pathParams = {
            @OpenApiParam(name = "id", description = "The unique identifier of the task", required = true, type = Integer.class),

    }, responses = {
            @OpenApiResponse(status = "204", description = "Collaborator need updated successfully"),
            @OpenApiResponse(status = "400", description = "Invalid request data"),
            @OpenApiResponse(status = "404", description = "Task not found"),
            @OpenApiResponse(status = "500", description = "Internal server error")
    })
    public void updateCollaboratorNeed(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int taskId = Integer.parseInt(ctx.pathParam("id"));

        Task task = taskDAO.findById(taskId);
        if (task == null) {
            ctx.status(404).json(Map.of("message", "Task not found"));
            return;
        }

        CollaboratorNeed collaboratorNeed = ctx.bodyAsClass(CollaboratorNeed.class);

        boolean success = taskDAO.updateCollaboratorNeed(taskId, collaboratorNeed);
        if (success) {
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("message", "Task not found"));
        }
    }

    /**
     * Get all the material needs of a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

    /**
     * Get all the collaborator needs of a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

    /**
     * Delete a specific material need for a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

    /**
     * Delete a specific collaborator need for a task
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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
