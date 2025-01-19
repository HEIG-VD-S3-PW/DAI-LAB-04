package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import io.javalin.http.NotModifiedResponse;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ch.heigvd.bdr.dao.ProjectDAO;
import ch.heigvd.bdr.models.Project;
import ch.heigvd.bdr.models.Team;

public class ProjectController implements ResourceControllerInterface {
    // Manages the cache for all the projects
    private final ConcurrentHashMap<Integer, LocalDateTime> projectCache = new ConcurrentHashMap<>();
    private final ProjectDAO projectDAO;

    public ProjectController() {
        this.projectDAO = new ProjectDAO();
    }

    /**
     * Show all projects
     *
     * @param ctx: context to use
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @OpenApi(path = "/projects", methods = HttpMethod.GET, operationId = "getAllProjects", summary = "Get all projects", description = "Returns a list of all projects.", tags = "Projects", headers = {
            @OpenApiParam(name = "If-Modified-Since", required = false, description = "RFC 1123 formatted timestamp for conditional request")
    }, responses = {
            @OpenApiResponse(status = "200", description = "List of all projects", content = @OpenApiContent(from = Project[].class)),
            @OpenApiResponse(status = "304", description = "Resource not modified since If-Modified-Since timestamp"),
            @OpenApiResponse(status = "400", description = "Invalid If-Modified-Since header format"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void all(Context ctx) throws SQLException, ClassNotFoundException, IOException {
        LocalDateTime lastKnownModification = UtilsController.getLastModifiedHeader(ctx);
        LocalDateTime latestModification = projectCache.values().stream().max(LocalDateTime::compareTo).orElse(null);

        if (lastKnownModification != null && latestModification != null
                && !UtilsController.isModifiedSince(latestModification, lastKnownModification)) {
            throw new NotModifiedResponse();
        }

        List<Project> projects = projectDAO.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (Project p : projects) {
            projectCache.putIfAbsent(p.getId(), now);
        }
        ctx.header("Last-Modified", now.toString());
        ctx.json(projects);
    }

    /**
     * Create a project
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws SQLException
     */
    @OpenApi(path = "/projects", methods = HttpMethod.POST, operationId = "createProject", summary = "Create a new project", description = "Creates a new project in the system.", tags = "Projects", requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Project.class)), responses = {
            @OpenApiResponse(status = "201", description = "Project created successfully", content = @OpenApiContent(from = Project.class)),
            @OpenApiResponse(status = "400", description = "Bad Request"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void create(Context ctx) throws ClassNotFoundException, IOException, SQLException {
        Project project = ctx.bodyAsClass(Project.class);
        Project createdProject = projectDAO.create(project);

        projectCache.put(project.getId(), LocalDateTime.now());

        ctx.header("Last-Modified", LocalDateTime.now().toString());
        ctx.status(201).json(createdProject);
    }

    /**
     * Show a specific project
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/projects/{id}", methods = HttpMethod.GET, operationId = "getProjectById", summary = "Get project by ID", description = "Fetches a project by it's ID.", tags = "Projects", pathParams = @OpenApiParam(name = "id", description = "Project ID", required = true, type = UUID.class), responses = {
            @OpenApiResponse(status = "200", description = "Project found", content = @OpenApiContent(from = Project.class)),
            @OpenApiResponse(status = "404", description = "Project not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));

        UtilsController.checkModif(ctx, projectCache, id);

        Project project = projectDAO.findById(id);

        if (project != null) {
            ctx.json(project);
        } else {
            ctx.status(404).json(Map.of("message", "Project not found"));
        }
    }

    /**
     * Update a project
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/projects/{id}", methods = HttpMethod.PUT, operationId = "updateProject", summary = "Update project by ID", description = "Updates project information by ID.", tags = "Projects", pathParams = @OpenApiParam(name = "id", description = "Project ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated user details", content = @OpenApiContent(from = Project.class)), responses = {
            @OpenApiResponse(status = "200", description = "Project updated successfully", content = @OpenApiContent(from = Project.class)),
            @OpenApiResponse(status = "400", description = "Bad Request"),
            @OpenApiResponse(status = "404", description = "Project not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void update(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Project project = ctx.bodyAsClass(Project.class);
        project.setId(id);
        Project updatedProject = projectDAO.update(project);

        if (updatedProject != null) {
            projectCache.put(id, LocalDateTime.now());
            ctx.header("Last-Modified", LocalDateTime.now().toString());
            ctx.json(updatedProject);
        } else {
            ctx.status(404).json(Map.of("message", "Project not found"));
        }
    }

    /**
     * Delete a project
     *
     * @param ctx: context to use
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @OpenApi(path = "/projects/{id}", methods = HttpMethod.DELETE, operationId = "deleteProject", summary = "Delete project by ID", description = "Deletes a project by it's ID.", tags = "Projects", pathParams = @OpenApiParam(name = "id", description = "Project ID", required = true, type = UUID.class), responses = {
            @OpenApiResponse(status = "200", description = "Project deleted successfully"),
            @OpenApiResponse(status = "404", description = "Project not found"),
            @OpenApiResponse(status = "500", description = "Internal Server Error")
    })
    @Override
    public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
        int id = Integer.parseInt(ctx.pathParam("id"));
        boolean deleted = projectDAO.delete(id);

        if (deleted) {
            projectCache.remove(id);
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("message", "Project not found"));
        }
    }
}
