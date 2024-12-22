package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import ch.heigvd.bdr.dao.TeamDAO;
import ch.heigvd.bdr.models.Team;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class TeamController implements ResourceControllerInterface {
  private final TeamDAO teamDAO = new TeamDAO();

  @OpenApi(path = "/teams", methods = HttpMethod.GET, operationId = "getAllTeams", summary = "Get all teams", description = "Returns a list of all teams.", tags = "Teams", responses = {
      @OpenApiResponse(status = "200", description = "List of all teams", content = @OpenApiContent(from = Team.class)),
      @OpenApiResponse(status = "500", description = "Internal Server Error")
  })
  @Override
  public void all(Context ctx) throws ClassNotFoundException, SQLException, IOException {
    ctx.json(teamDAO.findAll());
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
}
