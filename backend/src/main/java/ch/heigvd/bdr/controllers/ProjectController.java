package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import java.util.List;
import ch.heigvd.bdr.dao.ProjectDAO;
import ch.heigvd.bdr.models.Project;

public class ProjectController implements ResourceControllerInterface {

  private final ProjectDAO projectDAO;

  public ProjectController() {
    this.projectDAO = new ProjectDAO();
  }

  // Create a new project
  @Override
  public void create(Context ctx) {
    try {
      Project project = ctx.bodyAsClass(Project.class);
      Project createdProject = projectDAO.create(project);
      ctx.status(201).json(createdProject);
    } catch (Exception e) {
      ctx.status(500).json("Error: " + e.getMessage());
    }
  }

  // Retrieve a single project by ID
  @Override
  public void show(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      Project project = projectDAO.findById(id);
      if (project != null) {
        ctx.json(project);
      } else {
        ctx.status(404).json("Project not found");
      }
    } catch (Exception e) {
      ctx.status(500).json("Error: " + e.getMessage());
    }
  }

  // Retrieve all projects
  @Override
  public void all(Context ctx) {
    try {
      List<Project> projects = projectDAO.findAll();
      ctx.json(projects);
    } catch (Exception e) {
      ctx.status(500).json("Error: " + e.getMessage());
    }
  }

  // Update a project
  @Override
  public void update(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      Project project = ctx.bodyAsClass(Project.class);
      project.setId(id);
      Project updatedProject = projectDAO.update(project);
      ctx.json(updatedProject);
    } catch (Exception e) {
      ctx.status(500).json("Error: " + e.getMessage());
    }
  }

  // Delete a project
  @Override
  public void delete(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      boolean deleted = projectDAO.delete(id);
      if (deleted) {
        ctx.status(204);
      } else {
        ctx.status(404).json("Project not found");
      }
    } catch (Exception e) {
      ctx.status(500).json("Error: " + e.getMessage());
    }
  }
}
