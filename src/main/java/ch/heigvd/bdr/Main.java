package ch.heigvd.bdr;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.management.relation.Role;

import org.eclipse.jetty.security.UserDataConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.TextNode;

import ch.heigvd.AuthRole;
import ch.heigvd.bdr.controllers.*;
import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.exceptions.DatabaseExceptionHandler;
import ch.heigvd.bdr.misc.StringHelper;
import ch.heigvd.bdr.models.User;
import ch.heigvd.bdr.models.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.openapi.*;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.security.RouteRole;

/**
 * Starts Javalin server with OpenAPI plugin
 */
@SuppressWarnings({ "unused", "LombokGetterMayBeUsed", "LombokSetterMayBeUsed", "ProtectedMemberInFinalClass",
    "InnerClassMayBeStatic" })
public final class Main /* implements Handler */ {

  enum Rules implements RouteRole {
    ANONYMOUS,
    USER,
  }

  private static Javalin javalinBootstrap() {
    return Javalin.create(config -> {
      String deprecatedDocsPath = "/api/openapi.json"; // by default it's /openapi

      config.registerPlugin(new OpenApiPlugin(openApiConfig -> openApiConfig
          .withDocumentationPath(deprecatedDocsPath)
          .withRoles(Rules.ANONYMOUS)
          .withDefinitionConfiguration((version, openApiDefinition) -> openApiDefinition
              .withInfo(openApiInfo -> openApiInfo
                  .description("An Objective-key-result app")
                  .contact("API Support", "https://www.example.com/support", "support@example.com")
                  .license("Apache 2.0", "https://www.apache.org/licenses/", "Apache-2.0"))
              .withServer(openApiServer -> openApiServer
                  .description("A simple OKR app")
                  .url("http://localhost:{port}{basePath}/" + version + "/")
                  .variable("port", "Server's port", "7000", "7000")
                  .variable("basePath", "Base path of the server", "", "/"))
              .withDefinitionProcessor(content -> { // you can add whatever you want to this document using your
                                                    // favourite json api
                content.set("test", new TextNode("Value"));
                return content.toPrettyString();
              }))));

      config.registerPlugin(new SwaggerPlugin(swaggerConfiguration -> {
        swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
      }));

      config.registerPlugin(new ReDocPlugin(reDocConfiguration -> {
        reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
      }));

      for (JsonSchemaResource generatedJsonSchema : new JsonSchemaLoader().loadGeneratedSchemes()) {
        System.out.println(generatedJsonSchema.getName());
        System.out.println(generatedJsonSchema.getContentAsString());
      }
    });

  }

  /**
   * Runs server on localhost: 7000
   *
   * @param args args
   * @throws SQLException
   * @throws ClassNotFoundException
   * @throws IOException
   */
  public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
    var app = javalinBootstrap();

    Logger log = LoggerFactory.getLogger(Main.class);

    // bad request (json deserialisation error)
    app.exception(UnrecognizedPropertyException.class, (e, ctx) -> {
      ctx.status(400).json(e.getMessage());
    });

    app.exception(PSQLException.class, (e, ctx) -> {
      DatabaseExceptionHandler.handlePostgreSQLException(e, ctx);
    });
    app.exception(Exception.class, (e, ctx) -> {
      DatabaseExceptionHandler.handleGenericException(e, ctx);
    });

    app.beforeMatched(ctx -> {
      var userRole = getUserRole(ctx);
      var permittedRoles = ctx.routeRoles();
      if (permittedRoles.contains(AuthRole.ANY)) {
        return; // anyone can access
      }

      if (!ctx.routeRoles().contains(userRole)) {
        throw new UnauthorizedResponse();
      }

    });

    routes(app);

    String portEnv = System.getenv("JAVALIN_PORT");
    int port = 0;
    if (portEnv == null) {
      port = 7000;
    } else {
      port = Integer.valueOf(portEnv);
    }

    app.start("0.0.0.0", port);
  }

  private static AuthRole getUserRole(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    String userId = ctx.header("X-User-ID");
    if (userId == null || !StringHelper.isInteger(userId)) {
      return AuthRole.NONE;
    }

    int id = Integer.parseInt(userId);

    UserDAO userDAO = new UserDAO();
    User user = userDAO.findById(id);
    if (user == null) {
      return AuthRole.NONE;
    }

    if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER) {
      return AuthRole.SUPER;
    }

    return AuthRole.NORMAL;

  }

  private static void routes(Javalin app) {
    // routes
    app.get("/", ctx -> {
      ctx.result("hello");
    }, AuthRole.ANY);

    // Goal routes
    GoalController goalController = new GoalController();
    app.get("/goals", goalController::all, AuthRole.ANY);
    app.get("/goals/{id}", goalController::show, AuthRole.ANY);
    app.post("/goals", goalController::create, AuthRole.SUPER);
    app.put("/goals/{id}", goalController::update, AuthRole.SUPER);
    app.delete("/goals/{id}", goalController::delete, AuthRole.SUPER);

    // Result routes
    ResultController resultController = new ResultController();
    app.get("/results", resultController::all, AuthRole.ANY);
    app.get("/results/{id}", resultController::show, AuthRole.ANY);
    app.post("/results", resultController::create, AuthRole.SUPER);
    app.put("/results/{id}", resultController::update, AuthRole.SUPER);
    app.delete("/results/{id}", resultController::delete, AuthRole.SUPER);

    // Task routes
    TaskController taskController = new TaskController();
    app.get("/tasks", taskController::all, AuthRole.ANY);
    app.get("/tasks/{id}", taskController::show, AuthRole.ANY);
    app.post("/tasks", taskController::create, AuthRole.SUPER);
    app.put("/tasks/{id}", taskController::update, AuthRole.SUPER);
    app.delete("/tasks/{id}", taskController::delete, AuthRole.SUPER);
    app.get("/tasks/{id}/subtasks", taskController::subtasks, AuthRole.ANY);
    app.post("/tasks/{id}/subtasks", taskController::addSubtaskRelationship, AuthRole.SUPER);
    app.patch("/tasks/{id}/subtasks/{subtaskId}", taskController::updateSubtaskRequired, AuthRole.SUPER);
    app.delete("/tasks/{id}/subtasks/{subtaskId}", taskController::deleteSubtaskRelationship, AuthRole.SUPER);
    app.post("/tasks/{id}/materialNeeds", taskController::addMaterialNeeds, AuthRole.SUPER);
    app.post("/tasks/{id}/collaboratorNeeds", taskController::addCollaboratorNeeds, AuthRole.SUPER);
    app.get("/tasks/{id}/materialNeeds", taskController::getMaterialNeeds, AuthRole.ANY);
    app.get("/tasks/{id}/collaboratorNeeds", taskController::getCollaboratorNeeds, AuthRole.ANY);
    app.delete("/tasks/{id}/materialNeeds/{type}", taskController::deleteMaterialNeed, AuthRole.SUPER);
    app.delete("/tasks/{id}/collaboratorNeeds/{type}", taskController::deleteCollaboratorNeed, AuthRole.SUPER);
    app.put("/tasks/{id}/materialNeeds/{type}", taskController::updateMaterialNeed, AuthRole.SUPER);
    app.put("/tasks/{id}/collaboratorNeeds/{type}", taskController::updateCollaboratorNeed, AuthRole.SUPER);

    HealthController healthController = new HealthController();
    app.get("/health", healthController::checkHealth);

    app.start("0.0.0.0", 7000);

  }

}
