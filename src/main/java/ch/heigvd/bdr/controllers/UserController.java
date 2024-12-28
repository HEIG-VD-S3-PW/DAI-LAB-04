package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.models.Goal;
import ch.heigvd.bdr.models.Team;
import ch.heigvd.bdr.models.User;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class UserController implements ResourceControllerInterface {
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/users", methods = HttpMethod.GET, operationId = "getAllUsers", summary = "Get all users", description = "Returns a list of all users.", tags = "Users", responses = {
      @OpenApiResponse(status = "200", description = "List of all users", content = @OpenApiContent(from = User[].class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    ctx.json(userDAO.findAll());
  }

  @OpenApi(path = "/users", methods = HttpMethod.POST, operationId = "createUser", summary = "Create a new user", description = "Creates a new user in the system.", tags = "Users", requestBody = @OpenApiRequestBody(description = "User details", content = @OpenApiContent(from = User.class)), responses = {
      @OpenApiResponse(status = "201", description = "User created successfully", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    User user = ctx.bodyAsClass(User.class);
    ctx.status(201).json(userDAO.create(user));
  }

  @OpenApi(path = "/users/{id}", methods = HttpMethod.GET, operationId = "getUserById", summary = "Get user by ID", description = "Fetches a user by their ID.", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "User found", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "404", description = "User not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    User user = userDAO.findById(id);
    if (user != null) {
      ctx.json(user);
    } else {
      ctx.status(404).json("User not found");
    }
  }

  @OpenApi(path = "/users/{id}", methods = HttpMethod.PUT, operationId = "updateUser", summary = "Update user by ID", description = "Updates user information by ID.", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated user details", content = @OpenApiContent(from = User.class)), responses = {
      @OpenApiResponse(status = "200", description = "User updated successfully", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "404", description = "User not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void update(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    User user = ctx.bodyAsClass(User.class);
    user.setId(id);
    User updatedUser = userDAO.update(user);
    if (updatedUser != null) {
      ctx.json(updatedUser);
    } else {
      ctx.status(404).json("User not found");
    }
  }

  @OpenApi(path = "/users/{id}", methods = HttpMethod.DELETE, operationId = "deleteUser", summary = "Delete user by ID", description = "Deletes a user by their ID.", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "User deleted successfully"),
      @OpenApiResponse(status = "404", description = "User not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    if (userDAO.delete(id)) {
      ctx.status(204);
    } else {
      ctx.status(404).json("User not found");
    }
  }

  @OpenApi(path = "/users/{id}/teams", methods = HttpMethod.GET, operationId = "getTeams", summary = "Get user teams", description = "Fetches all the teams an user belongs to", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "User found", content = @OpenApiContent(from = Team.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  public void teams(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    User user = userDAO.findById(id);
    if (user == null) {
      ctx.status(404).json("User not found");
      return;
    }

    List<Team> teams = userDAO.getTeams(user.getId());
    ctx.json(teams);
  }

  @OpenApi(path = "/users/{id}/goals", methods = HttpMethod.GET, operationId = "getGoals", summary = "Get user goals", description = "Fetches all the goals of an user", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "User found", content = @OpenApiContent(from = Team.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  public void goals(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    User user = userDAO.findById(id);
    if (user == null) {
      ctx.status(404).json("User not found");
      return;
    }

    List<Goal> goals = userDAO.getGoals(user.getId());
    ctx.json(goals);
  }
}
