package ch.heigvd.bdr;

import java.io.IOException;
import java.sql.SQLException;

import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.TextNode;

import ch.heigvd.bdr.misc.AuthRole;
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
import io.javalin.http.UnauthorizedResponse;
import io.javalin.openapi.*;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.security.RouteRole;

/**
 * Starts Javalin server with OpenAPI plugin
 */
@SuppressWarnings({"unused", "InnerClassMayBeStatic"})
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
                    .withRoles(AuthRole.ANY)
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

        // bad request (json deserialization error)
        app.exception(UnrecognizedPropertyException.class, (e, ctx) -> {
            ctx.status(400).json(e.getMessage());
        });

        app.exception(PSQLException.class, DatabaseExceptionHandler::handlePostgreSQLException);
        app.exception(Exception.class, DatabaseExceptionHandler::handleGenericException);

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
            port = Integer.parseInt(portEnv);
        }

        app.start("0.0.0.0", port);
    }

    /**
     * Get the user's authorizations based on his id
     *
     * @param ctx: current context to get/send data
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return The user's role
     */
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

    /**
     * Project's routes to interact with the backend
     *
     * @param app: current javalin app
     */
    private static void routes(Javalin app) {
        app.get("/", ctx -> {
            ctx.result("hello");
        }, AuthRole.ANY);

        UserController userController = new UserController();
        app.get("/users", userController::all, AuthRole.ANY);
        app.get("/users/{id}", userController::show, AuthRole.ANY);
        app.post("/users", userController::create, AuthRole.ANY);
        app.put("/users/{id}", userController::update, AuthRole.ANY);
        app.delete("/users/{id}", userController::delete, AuthRole.ANY);

        TeamController teamController = new TeamController();
        app.get("/teams", teamController::all, AuthRole.ANY);
        app.get("/teams/{id}", teamController::show, AuthRole.ANY);
        app.post("/teams", teamController::create, AuthRole.SUPER);
        app.put("/teams/{id}", teamController::update, AuthRole.SUPER);
        app.delete("/teams/{id}", teamController::delete, AuthRole.SUPER);
        app.post("/teams/{id}/join", teamController::join, AuthRole.ANY);
        app.post("/teams/{id}/leave", teamController::leave, AuthRole.ANY);
        app.get("/teams/{id}/users", teamController::getTeamMembers, AuthRole.ANY);
        app.post("/teams/{id}/manager", teamController::becomeManager, AuthRole.ANY);
        app.delete("/teams/{id}/manager", teamController::removeManager, AuthRole.ANY);

        ProjectController projectController = new ProjectController();
        app.get("/projects", projectController::all, AuthRole.ANY);
        app.get("/projects/{id}", projectController::show, AuthRole.ANY);
        app.post("/projects", projectController::create, AuthRole.SUPER);
        app.put("/projects/{id}", projectController::update, AuthRole.SUPER);
        app.delete("/projects/{id}", projectController::delete, AuthRole.SUPER);

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
        app.get("/health", healthController::checkHealth, AuthRole.ANY);
    }
}