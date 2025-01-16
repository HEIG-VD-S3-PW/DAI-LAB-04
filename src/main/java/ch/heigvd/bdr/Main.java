package ch.heigvd.bdr;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.TextNode;
import ch.heigvd.bdr.controllers.*;
import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.exceptions.DatabaseExceptionHandler;
import ch.heigvd.bdr.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
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

  private static void routes(Javalin app) {
    // routes
    app.get("/", ctx -> {
      ctx.result("hello");
    });

    UserController userController = new UserController();
    app.get("/users", userController::all);
    app.get("/users/{id}", userController::show);
    app.post("/users", userController::create);
    app.put("/users/{id}", userController::update);
    app.delete("/users/{id}", userController::delete);

    TeamController teamController = new TeamController();
    app.get("/teams", teamController::all);
    app.get("/teams/{id}", teamController::show);
    app.post("/teams", teamController::create);
    app.put("/teams/{id}", teamController::update);
    app.delete("/teams/{id}", teamController::delete);
    app.post("/teams/{id}/join", teamController::join);
    app.post("/teams/{id}/leave", teamController::leave);
    app.get("/teams/{id}/users", teamController::getTeamMembers);
    app.post("/teams/{id}/manager", teamController::becomeManager);
    app.delete("/teams/{id}/manager", teamController::removeManager);

    ProjectController projectController = new ProjectController();
    app.get("/projects", projectController::all);
    app.get("/projects/{id}", projectController::show);
    app.post("/projects", projectController::create);
    app.put("/projects/{id}", projectController::update);
    app.delete("/projects/{id}", projectController::delete);

    GoalController goalController = new GoalController();
    app.get("/goals", goalController::all);
    app.get("/goals/{id}", goalController::show);
    app.post("/goals", goalController::create);
    app.put("/goals/{id}", goalController::update);
    app.delete("/goals/{id}", goalController::delete);

    ResultController resultController = new ResultController();
    app.get("/results", resultController::all);
    app.get("/results/{id}", resultController::show);
    app.post("/results", resultController::create);
    app.put("/results/{id}", resultController::update);
    app.delete("/results/{id}", resultController::delete);

    TaskController taskController = new TaskController();
    app.get("/tasks", taskController::all);
    app.get("/tasks/{id}", taskController::show);
    app.post("/tasks", taskController::create);
    app.put("/tasks/{id}", taskController::update);
    app.delete("/tasks/{id}", taskController::delete);
    app.get("/tasks/{id}/subtasks", taskController::subtasks);
    app.post("/tasks/{id}/subtasks", taskController::addSubtaskRelationship);
    app.patch("/tasks/{id}/subtasks/{subtaskId}", taskController::updateSubtaskRequired);
    app.delete("/tasks/{id}/subtasks/{subtaskId}", taskController::deleteSubtaskRelationship);

    app.post("/tasks/{id}/materialNeeds", taskController::addMaterialNeeds);
    app.post("/tasks/{id}/collaboratorNeeds", taskController::addCollaboratorNeeds);
    app.get("/tasks/{id}/materialNeeds", taskController::getMaterialNeeds);
    app.get("/tasks/{id}/collaboratorNeeds", taskController::getCollaboratorNeeds);
    app.delete("/tasks/{id}/materialNeeds/{type}", taskController::deleteMaterialNeed);
    app.delete("/tasks/{id}/collaboratorNeeds/{type}", taskController::deleteCollaboratorNeed);
    app.put("/tasks/{id}/materialNeeds/{type}", taskController::updateMaterialNeed);
    app.put("/tasks/{id}/collaboratorNeeds/{type}", taskController::updateCollaboratorNeed);

    HealthController healthController = new HealthController();
    app.get("/health", healthController::checkHealth);

    app.start("0.0.0.0", 7000);

  }
}
