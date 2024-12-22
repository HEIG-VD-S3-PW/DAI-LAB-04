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
import kotlin.reflect.jvm.internal.KClassImpl.Data;

/**
 * Starts Javalin server with OpenAPI plugin
 */
@SuppressWarnings({ "unused", "LombokGetterMayBeUsed", "LombokSetterMayBeUsed", "ProtectedMemberInFinalClass",
    "InnerClassMayBeStatic" })
public final class Main implements Handler {

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
                  .variable("port", "Server's port", "7070", "7070")
                  .variable("basePath", "Base path of the server", "", "/"))
              // Based on official example:
              // https://swagger.io/docs/specification/authentication/oauth2/
              .withSecurity(openApiSecurity -> openApiSecurity
                  .withBasicAuth()
                  .withBearerAuth()
                  .withApiKeyAuth("ApiKeyAuth", "X-Api-Key")
                  .withCookieAuth("CookieAuth", "JSESSIONID"))
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

    // routes
    app.get("/", ctx -> {
      ctx.result("hello");
    });

    UserController userController = new UserController();
    app.get("/users", userController::all);
    app.get("/users/{id}", userController::show);
    app.get("/users/{id}/goals", userController::goals);
    app.get("/users/{id}/teams", userController::teams);
    app.post("/users", userController::create);
    app.put("/users/{id}", userController::update);
    app.delete("/users/{id}", userController::delete);

    TeamController teamController = new TeamController();
    app.get("/teams", teamController::all);
    app.get("/teams/{id}", teamController::show);
    app.post("/teams", teamController::create);
    app.put("/teams/{id}", teamController::update);
    app.delete("/teams/{id}", teamController::delete);

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

    TaskController taskController = new TaskController();
    app.get("/tasks", taskController::all);
    app.get("/tasks/{id}", taskController::show);
    app.post("/tasks", taskController::create);
    app.put("/tasks/{id}", taskController::update);
    app.delete("/tasks/{id}", taskController::delete);

    app.get("/main/{name}", ctx -> ctx.result("Hello "));
    app.start("0.0.0.0", 7000);

  }

  @OpenApi(path = "/main/{name}", methods = HttpMethod.POST, operationId = "cli", summary = "Remote command execution", description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.", tags = {
      "Default", "Cli" }, security = {
          @OpenApiSecurity(name = "BasicAuth")
      }, headers = {
          @OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true, type = UUID.class),
          @OpenApiParam(name = "Optional"),
          @OpenApiParam(name = "X-Rick", example = "Rolled"),
          @OpenApiParam(name = "X-SomeNumber", required = true, type = Integer.class, example = "500")
      }, pathParams = {
          @OpenApiParam(name = "name", description = "Name", required = true, type = UUID.class)
      }, queryParams = {
          @OpenApiParam(name = "query", description = "Some query", required = true, type = Integer.class)
      }, requestBody = @OpenApiRequestBody(description = "Supports multiple request bodies", content = {
          @OpenApiContent(from = String.class, example = "value"), // simple type
      // @OpenApiContent(from = DtoWithFields.class, mimeType = "app/dto"), // map
      // only fields
      }), responses = {
          @OpenApiResponse(status = "200", description = "Status of the executed command", content = {
              @OpenApiContent(from = String.class, example = "Value"),
          }),
          @OpenApiResponse(status = "400", description = "Error message related to the invalid command format (0 < command length < "
              + 10 + ")", content = @OpenApiContent(from = EntityDto[].class), headers = {
                  @OpenApiParam(name = "X-Error-Message", description = "Error message", type = String.class)
              }),
          @OpenApiResponse(status = "401", description = "Error message related to the unauthorized access", content = {
              @OpenApiContent(from = EntityDto[].class, exampleObjects = {
                  @OpenApiExampleProperty(name = "error", value = "ERROR-CODE-401"),
              })
          }),
          @OpenApiResponse(status = "500") // fill description with HttpStatus message
      }, callbacks = {
          @OpenApiCallback(name = "onData", url = "{$request.query.callbackUrl}/data", method = HttpMethod.GET, requestBody = @OpenApiRequestBody(description = "Callback request body", content = @OpenApiContent(from = String.class)), responses = {
              @OpenApiResponse(status = "200", description = "Callback response", content = {
                  @OpenApiContent(from = String.class) })
          }),
      })
  @Override
  public void handle(@NotNull Context ctx) {
  }

  @OpenApi(path = "standalone", methods = HttpMethod.DELETE, versions = "v1", headers = { @OpenApiParam(name = "V1") })
  static final class EntityDto implements Serializable {

    private final int status;
    private final @NotNull String message;
    private final @NotNull String timestamp;
    private final @NotNull Foo foo;
    private final @NotNull List<Foo> foos;
    private final @NotNull Map<String, Map<String, Foo>> bars = new HashMap<>();

    public EntityDto(int status, @NotNull String message, @Nullable Foo foo, @NotNull List<Foo> foos) {
      this.status = status;
      this.message = message;
      this.foo = foo;
      this.foos = foos;
      this.timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    // should be represented as object
    public @NotNull Map<String, Map<String, Foo>> getBars() {
      return bars;
    }

    // should be represented by array
    public @NotNull List<Foo> getFoos() {
      return foos;
    }

    // should handle fallback to object type
    @SuppressWarnings("rawtypes")
    public List getUnknowns() {
      return foos;
    }

    // should be displayed as string
    @OpenApiPropertyType(definedBy = String.class)
    public @NotNull Foo getFoo() {
      return foo;
    }

    // HiddenEntity with @OpenApiPropertyType should be displayed as string
    public HiddenEntity getHiddenEntity() {
      return new HiddenEntity();
    }

    // should support primitive types
    public int getStatus() {
      return status;
    }

    // should rename
    @OpenApiName("message")
    public String getMessageValue() {
      return message;
    }

    // should be ignored
    @OpenApiIgnore
    public String getFormattedMessage() {
      return status + message;
    }

    // should contain example
    @OpenApiExample("2022-08-14T21:13:03.546Z")
    public @NotNull String getTimestamp() {
      return timestamp;
    }

    // should contain examples
    @OpenApiExample(objects = {
        @OpenApiExampleProperty(value = "2022-08-14T21:13:03.546Z"),
        @OpenApiExampleProperty(value = "2022-08-14T21:13:03.546Z")
    })
    public @NotNull String[] getTimestamps() {
      return new String[] { timestamp };
    }

    // should contain dedicated foo example
    @OpenApiExample(objects = {
        @OpenApiExampleProperty(name = "name", value = "Margot Robbie"),
        @OpenApiExampleProperty(name = "link", value = "Dedicated link")
    })
    public @NotNull Foo getExampleFoo() {
      return new Foo();
    }

    // should contain object example
    @OpenApiExample(objects = {
        @OpenApiExampleProperty(name = "name", value = "Margot Robbie"),
        @OpenApiExampleProperty(name = "link", value = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    })
    public @NotNull Object getExampleObject() {
      return new String[] { timestamp };
    }

    // should contain objects example
    @OpenApiExample(objects = {
        @OpenApiExampleProperty(name = "Barbie", objects = {
            @OpenApiExampleProperty(name = "name", value = "Margot Robbie"),
            @OpenApiExampleProperty(name = "link", value = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        }),
    })
    public @NotNull Object[] getExampleObjects() {
      return new String[] { timestamp };
    }

    // should contain example for primitive types, SwaggerUI will automatically
    // display this as an Integer
    @OpenApiExample("5050")
    @OpenApiNumberValidation(minimum = "5000", exclusiveMinimum = true, maximum = "6000", exclusiveMaximum = true, multipleOf = "50")
    @OpenApiStringValidation(minLength = "4", maxLength = "4", pattern = "^[0-9]{4}$", format = "int32")
    @OpenApiArrayValidation(minItems = "1", maxItems = "1", uniqueItems = true)
    @OpenApiObjectValidation(minProperties = "1", maxProperties = "1")
    public int getVeryImportantNumber() {
      return status + 1;
    }

    @OpenApiDescription("Some description")
    public String getDescription() {
      return "Description";
    }

    // should support @Custom from JsonSchema
    @Custom(name = "description", value = "Custom property")
    public String getCustom() {
      return "";
    }

    // static should be ignored
    public static String getStatic() {
      return "static";
    }
  }

  static final class Foo {

    @OpenApiExample("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    public String getLink() {
      return "";
    }
  }

  @OpenApiPropertyType(definedBy = String.class)
  static class HiddenEntity {

  }

  @OpenApiName("EntityWithCustomName")
  class CustomNameEntity {
  }

}
