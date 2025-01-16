package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
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

public class ProjectController implements ResourceControllerInterface {
  private final ConcurrentHashMap<Integer, LocalDateTime> projectCache = new ConcurrentHashMap<>();
  private final ProjectDAO projectDAO;

  public ProjectController() {
    this.projectDAO = new ProjectDAO();
  }

  @OpenApi(path = "/projects", methods = HttpMethod.GET, operationId = "getAllProjects", summary = "Get all projects", description = "Returns a list of all projects.", tags = "Projects", responses = {
      @OpenApiResponse(status = "200", description = "List of all projects", content = @OpenApiContent(from = Project[].class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  // Retrieve all projects
  @Override
  public void all(Context ctx) throws SQLException, ClassNotFoundException, IOException {
    List<Project> projects = projectDAO.findAll();
    for(Project p : projects) {
      if(!projectCache.containsKey(p.getId())) {
        projectCache.put(p.getId(), LocalDateTime.now());
      }
    }
    ctx.json(projects);
  }

  @OpenApi(path = "/projects", methods = HttpMethod.POST, operationId = "createProject", summary = "Create a new project", description = "Creates a new project in the system.", tags = "Projects", requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Project.class)), responses = {
      @OpenApiResponse(status = "201", description = "Project created successfully", content = @OpenApiContent(from = Project.class)),
      @OpenApiResponse(status = "400", description = "Bad Request"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  // Create a new project
  @Override
  public void create(Context ctx) throws ClassNotFoundException, IOException, SQLException {
    Project project = ctx.bodyAsClass(Project.class);
    Project createdProject = projectDAO.create(project);
    projectCache.put(createdProject.getId(), LocalDateTime.now());
    ctx.header("Last-Modified", LocalDateTime.now().toString());
    ctx.status(201).json(createdProject);
  }

  @OpenApi(path = "/projects/{id}", methods = HttpMethod.GET, operationId = "getProjectById", summary = "Get project by ID", description = "Fetches a project by it's ID.", tags = "Projects", pathParams = @OpenApiParam(name = "id", description = "Project ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Project found", content = @OpenApiContent(from = Project.class)),
      @OpenApiResponse(status = "404", description = "Project not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  // Retrieve a single project by ID
  @Override
  public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));

    UtilsController.checkModif(ctx, projectCache, id);

    Project project = projectDAO.findById(id);

    if (project != null) {
      UtilsController.sendResponse(ctx, projectCache, project.getId());
      ctx.json(project);
    } else {
      throw new NotFoundResponse();
    }
  }

  @OpenApi(path = "/projects/{id}", methods = HttpMethod.PUT, operationId = "updateProject", summary = "Update project by ID", description = "Updates project information by ID.", tags = "Projects", pathParams = @OpenApiParam(name = "id", description = "Project ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated user details", content = @OpenApiContent(from = Project.class)), responses = {
      @OpenApiResponse(status = "200", description = "Project updated successfully", content = @OpenApiContent(from = Project.class)),
      @OpenApiResponse(status = "404", description = "Project not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  // Update a project
  @Override
  public void update(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Project project = ctx.bodyAsClass(Project.class);
    project.setId(id);
    projectDAO.update(project);
    // Update the cache
    projectCache.put(id, LocalDateTime.now());
    ctx.header("Last-Modified", LocalDateTime.now().toString());
    ctx.status(204);
  }

  @OpenApi(path = "/projects/{id}", methods = HttpMethod.DELETE, operationId = "deleteProject", summary = "Delete project by ID", description = "Deletes a project by it's ID.", tags = "Projects", pathParams = @OpenApiParam(name = "id", description = "Project ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Project deleted successfully"),
      @OpenApiResponse(status = "404", description = "Project not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })

  // Delete a project
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
