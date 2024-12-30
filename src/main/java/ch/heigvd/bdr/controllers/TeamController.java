package ch.heigvd.bdr.controllers;

import ch.heigvd.bdr.dao.UserDAO;
import ch.heigvd.bdr.dao.UserTeamDAO;
import ch.heigvd.bdr.models.User;
import ch.heigvd.bdr.models.UserTeam;
import io.javalin.http.Context;
import ch.heigvd.bdr.dao.TeamDAO;
import ch.heigvd.bdr.models.Team;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TeamController implements ResourceControllerInterface {
  private final TeamDAO teamDAO = new TeamDAO();
  private final UserTeamDAO userTeamDAO = new UserTeamDAO();
  private final UserDAO userDAO = new UserDAO();

  @OpenApi(path = "/teams", methods = HttpMethod.GET, operationId = "getAllTeams", summary = "Get all teams", description = "Returns a list of all teams.", tags = "Teams", responses = {
      @OpenApiResponse(status = "200", description = "List of all teams", content = @OpenApiContent(from = Team.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    // ctx.json(teamDAO.findAll());

    int userId = Integer.parseInt(Objects.requireNonNull(ctx.header("X-User-ID")));
    if (userId == 0) {
      ctx.status(400).json(Map.of("message", "Missing X-User-ID header"));
      return;
    }

    User user = userDAO.findById(userId);
    if (user == null) {
      ctx.status(404).json("User not found");
      return;
    }

    List<Team> teams = userDAO.getTeams(user.getId());
    ctx.json(teams);

  }

  @OpenApi(path = "/teams", methods = HttpMethod.POST, operationId = "createTeam", summary = "Create a new team", description = "Creates a new team.", tags = "Teams", requestBody = @OpenApiRequestBody(description = "Team details", content = @OpenApiContent(from = Team.class)), responses = {
      @OpenApiResponse(status = "201", description = "Team created successfully", content = @OpenApiContent(from = Team.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void create(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    Team team = ctx.bodyAsClass(Team.class);
    ctx.status(201).json(teamDAO.create(team));
  }

  @OpenApi(path = "/teams/{id}", methods = HttpMethod.GET, operationId = "getTeamById", summary = "Get team by ID", description = "Fetches a team by its ID.", tags = "Teams", pathParams = @OpenApiParam(name = "id", description = "Team ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Team found", content = @OpenApiContent(from = Team.class)),
      @OpenApiResponse(status = "404", description = "Team not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void show(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Team team = teamDAO.findById(id);
    if (team != null) {
      ctx.json(team);
    } else {
      ctx.status(404).json("Team not found");
    }
  }

  @OpenApi(path = "/teams/{id}", methods = HttpMethod.PUT, operationId = "updateTeam", summary = "Update team by ID", description = "Updates a team by its ID.", tags = "Teams", pathParams = @OpenApiParam(name = "id", description = "Team ID", required = true, type = UUID.class), requestBody = @OpenApiRequestBody(description = "Updated team details", content = @OpenApiContent(from = Team.class)), responses = {
      @OpenApiResponse(status = "200", description = "Team updated successfully", content = @OpenApiContent(from = Team.class)),
      @OpenApiResponse(status = "404", description = "Team not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void update(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    Team team = ctx.bodyAsClass(Team.class);
    team.setId(id);
    Team updatedTeam = teamDAO.update(team);
    if (updatedTeam != null) {
      ctx.json(updatedTeam);
    } else {
      ctx.status(404).json("Team not found");
    }
  }

  @OpenApi(path = "/teams/{id}", methods = HttpMethod.DELETE, operationId = "deleteTeam", summary = "Delete team by ID", description = "Deletes a team by its ID.", tags = "Teams", pathParams = @OpenApiParam(name = "id", description = "Team ID", required = true, type = UUID.class), responses = {
      @OpenApiResponse(status = "200", description = "Team deleted successfully"),
      @OpenApiResponse(status = "404", description = "Team not found"),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void delete(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int id = Integer.parseInt(ctx.pathParam("id"));
    if (teamDAO.delete(id)) {
      ctx.status(204);
    } else {
      ctx.status(404).json("Team not found");
    }
  }

  @OpenApi(path = "/teams/{id}/join", methods = HttpMethod.POST, operationId = "joinTeam", summary = "Join a team", description = "Allows a user to join a team.", tags = "Teams", pathParams = @OpenApiParam(name = "id", description = "Team ID", required = true, type = Integer.class), responses = {
          @OpenApiResponse(status = "200", description = "User joined the team successfully"),
          @OpenApiResponse(status = "400", description = "User is already a member of the team"),
          @OpenApiResponse(status = "404", description = "Team not found"),
          @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  public void join(Context ctx) throws ClassNotFoundException, SQLException, IOException {

    ctx.headerMap().forEach((key, value) -> System.out.println(key + " : " + value));

    int teamId = Integer.parseInt(ctx.pathParam("id"));
    int userId = Integer.parseInt(Objects.requireNonNull(ctx.header("X-User-ID")));

    // Vérifier si l'utilisateur est déjà membre de l'équipe
    if (userTeamDAO.isUserInTeam(userId, teamId)) {
      ctx.status(400).json(Map.of("message", "User is already a member of the team"));
      return;
    }

    // Ajouter l'utilisateur à l'équipe
    UserTeam userTeam = new UserTeam(userId, teamId);
    userTeamDAO.create(userTeam);

    ctx.status(200).json(Map.of("message", "User joined the team successfully"));
  }

  @OpenApi(path = "/teams/{id}/leave", methods = HttpMethod.POST, operationId = "leaveTeam", summary = "Leave a team", description = "Allows a user to leave a team.", tags = "Teams", pathParams = @OpenApiParam(name = "id", description = "Team ID", required = true, type = Integer.class), responses = {
          @OpenApiResponse(status = "200", description = "User left the team successfully"),
          @OpenApiResponse(status = "400", description = "User is not a member of the team"),
          @OpenApiResponse(status = "404", description = "Team not found"),
          @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  public void leave(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    int teamId = Integer.parseInt(ctx.pathParam("id"));
    int userId = Integer.parseInt(Objects.requireNonNull(ctx.header("X-User-ID")));

    // Vérifier si l'utilisateur est membre de l'équipe
    if (!userTeamDAO.isUserInTeam(userId, teamId)) {
      ctx.status(400).json(Map.of("message", "User is not a member of the team"));
      return;
    }

    // Retirer l'utilisateur de l'équipe
    userTeamDAO.deleteByUserAndTeam(userId, teamId);

    ctx.status(200).json(Map.of("message", "User left the team successfully"));
  }

  @OpenApi(
          path = "/teams/{id}/users",
          methods = HttpMethod.GET,
          operationId = "getTeamMembers",
          summary = "Get users of a team",
          description = "Returns a list of users belonging to the specified team.",
          tags = "Teams",
          pathParams = @OpenApiParam(name = "id", description = "Team ID", required = true, type = Integer.class),
          responses = {
                  @OpenApiResponse(status = "200", description = "List of users in the team", content = @OpenApiContent(from = User.class)),
                  @OpenApiResponse(status = "404", description = "Team not found"),
                  @OpenApiResponse(status = "500", description = "Internal Server Error")
          }
  )
  public void getTeamMembers(Context ctx) throws Exception {
    int teamId = Integer.parseInt(ctx.pathParam("id"));

    // Vérifier si l'équipe existe
    Team team = teamDAO.findById(teamId);
    if (team == null) {
      ctx.status(404).json(Map.of("message", "Team not found"));
      return;
    }
    // Récupérer les utilisateurs de l'équipe
    List<User> users = userTeamDAO.getTeamMembers(teamId);
    ctx.json(users);
  }
}


