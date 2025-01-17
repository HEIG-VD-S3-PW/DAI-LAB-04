package ch.heigvd.bdr.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.models.User;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;

public class UserController implements ResourceControllerInterface {
  private final ConcurrentHashMap<Integer, LocalDateTime> userCache = new ConcurrentHashMap<>();
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/users", methods = HttpMethod.GET, operationId = "getAllUsers", summary = "Get all users", description = "Returns a list of all users.", tags = "Users", headers = {
      @OpenApiParam(name = "If-Modified-Since", required = false, description = "RFC 1123 formatted timestamp for conditional request")
  }, responses = {
      @OpenApiResponse(status = "200", description = "List of all users", content = @OpenApiContent(from = User[].class)),
      @OpenApiResponse(status = "304", description = "Resource not modified since If-Modified-Since timestamp"),
      @OpenApiResponse(status = "400", description = "Invalid If-Modified-Since header format"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {

    LocalDateTime lastKnownModification = UtilsController.getLastModifiedHeader(ctx);
    LocalDateTime latestModification = userCache.values().stream().max(LocalDateTime::compareTo).orElse(null);

    if (lastKnownModification != null && latestModification != null
        && !UtilsController.isModifiedSince(latestModification, lastKnownModification)) {
      ctx.status(304).json(Map.of("message", "Not modified"));
      return;
    }

    List<User> users = userDAO.findAll();
    LocalDateTime now = LocalDateTime.now();
    for (User u : users) {
      userCache.put(u.getId(), now);
    }
    ctx.header("Last-Modified", now.toString());
    ctx.json(users);
  }

  @OpenApi(path = "/users", methods = HttpMethod.POST, operationId = "createUser", summary = "Create a new user", description = "Creates a new user in the system.", tags = "Users", requestBody = @OpenApiRequestBody(description = "User details", content = @OpenApiContent(from = User.class)), responses = {
      @OpenApiResponse(status = "201", description = "User created successfully", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "400", description = "Bad Request"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    User user = ctx.bodyAsClass(User.class);
    userCache.put(user.getId(), LocalDateTime.now());
    ctx.header("Last-Modified", LocalDateTime.now().toString());
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

    UtilsController.checkModif(ctx, userCache, id);

    User user = userDAO.findById(id);

    if (user != null) {
      UtilsController.sendResponse(ctx, userCache, user.getId());
      ctx.json(user);
    } else {
      ctx.status(404).json(Map.of("message", "User not found"));
    }
  }

  @OpenApi(path = "/users/{id}", methods = HttpMethod.PUT, operationId = "updateUser", summary = "Update user by ID", description = "Updates user information by ID.", tags = "Users", pathParams = @OpenApiParam(name = "id", description = "User ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated user details", content = @OpenApiContent(from = User.class)), responses = {
      @OpenApiResponse(status = "200", description = "User updated successfully", content = @OpenApiContent(from = User.class)),
      @OpenApiResponse(status = "400", description = "Bad Request"),
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
      userCache.put(id, LocalDateTime.now());
      ctx.header("Last-Modified", LocalDateTime.now().toString());
      ctx.json(updatedUser);
    } else {
      ctx.status(404).json(Map.of("message", "User not found"));
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
      userCache.remove(id);
      ctx.status(204);
    } else {
      ctx.status(404).json(Map.of("message", "User not found"));
    }
  }

}
