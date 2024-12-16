package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;

import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.models.User;
import io.javalin.openapi.*;
import java.util.UUID;

public class UserController implements ResourceControllerInterface {
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/users", methods = HttpMethod.GET, operationId = "getAllUsers", summary = "Get all users", description = "Returns a list of all users.", tags = "Users", responses = {
      @OpenApiResponse(status = "200", description = "List of all users", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) {
    try {
      ctx.json(userDAO.findAll());
    } catch (Exception e) {
      ctx.status(500).json("Error fetching users: " + e.getMessage());
    }
  }

  @OpenApi(path = "/users", methods = HttpMethod.POST, operationId = "createUser", summary = "Create a new user", description = "Creates a new user in the system.", tags = "Users", requestBody = @OpenApiRequestBody(description = "User details", content = @OpenApiContent(from = User.class)), responses = {
      @OpenApiResponse(status = "201", description = "User created successfully", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) {
    try {
      User user = ctx.bodyAsClass(User.class);
      ctx.status(201).json(userDAO.create(user));
    } catch (Exception e) {
      ctx.status(400).json("Error creating user: " + e.getMessage());
    }
  }

  @OpenApi(path = "/users/{id}", methods = HttpMethod.GET, operationId = "getUserById", summary = "Get user by ID", description = "Fetches a user by their ID.", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "User found", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "404", description = "User not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      User user = userDAO.findById(id);
      if (user != null) {
        ctx.json(user);
      } else {
        ctx.status(404).json("User not found");
      }
    } catch (NumberFormatException e) {
      ctx.status(400).json("Invalid ID format");
    } catch (Exception e) {
      ctx.status(500).json("Unexpected error: " + e.getMessage());
    }
  }

  @OpenApi(path = "/users/{id}", methods = HttpMethod.PUT, operationId = "updateUser", summary = "Update user by ID", description = "Updates user information by ID.", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated user details", content = @OpenApiContent(from = User.class)), responses = {
      @OpenApiResponse(status = "200", description = "User updated successfully", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "404", description = "User not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void update(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      User user = ctx.bodyAsClass(User.class);
      user.setId(id);
      User updatedUser = userDAO.update(user);
      if (updatedUser != null) {
        ctx.json(updatedUser);
      } else {
        ctx.status(404).json("User not found");
      }
    } catch (NumberFormatException e) {
      ctx.status(400).json("Invalid ID format");
    } catch (Exception e) {
      ctx.status(500).json("Unexpected error: " + e.getMessage());
    }
  }

  @OpenApi(path = "/users/{id}", methods = HttpMethod.DELETE, operationId = "deleteUser", summary = "Delete user by ID", description = "Deletes a user by their ID.", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "User deleted successfully"),
      @OpenApiResponse(status = "404", description = "User not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void delete(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      if (userDAO.delete(id)) {
        ctx.status(204);
      } else {
        ctx.status(404).json("User not found");
      }
    } catch (NumberFormatException e) {
      ctx.status(400).json("Invalid ID format");
    } catch (Exception e) {
      ctx.status(500).json("Unexpected error: " + e.getMessage());
    }
  }
}
