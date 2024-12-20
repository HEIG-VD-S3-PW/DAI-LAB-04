package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoalDAO implements GenericDAO<Goal, Integer> {
  private static final String GOAL_QUERY = "SELECT g.id AS goal_id, g.name AS goal_name, g.description AS goal_description, "
      +
      "g.note AS goal_note, g.tag AS goal_tag, g.projectId AS goal_projectId, g.teamId AS goal_teamId, " +
      "t.id AS team_id, t.name AS team_name, " +
      "p.id AS project_id, p.name AS project_name " +
      "FROM \"Goal\" g " +
      "INNER JOIN \"Team\" t ON g.teamId = t.id " +
      "INNER JOIN \"Project\" p ON g.projectId = p.id";

  private Goal mapGoal(ResultSet rs) throws SQLException {
    Goal goal = new Goal();
    goal.setId(rs.getInt("goal_id"));
    goal.setName(rs.getString("goal_name"));
    goal.setDescription(rs.getString("goal_description"));
    goal.setNote(rs.getString("goal_note"));
    goal.setTag(rs.getString("goal_tag"));
    goal.setProjectId(rs.getInt("goal_projectId"));
    goal.setTeamId(rs.getInt("goal_teamId"));

    Team team = new Team();
    team.setId(rs.getInt("team_id"));
    team.setName(rs.getString("team_name"));
    goal.setTeam(team);

    Project project = new Project();
    project.setId(rs.getInt("project_id"));
    project.setName(rs.getString("project_name"));
    goal.setProject(project);

    return goal;
  }

  @Override
  public Goal create(Goal goal) throws ClassNotFoundException, SQLException, IOException {
    String query = "INSERT INTO \"Goal\" (name, description, note, tag, projectId, teamId) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, goal.getName());
      pstmt.setString(2, goal.getDescription());
      pstmt.setString(3, goal.getNote());
      pstmt.setString(4, goal.getTag());
      pstmt.setInt(5, goal.getProjectId());
      pstmt.setInt(6, goal.getTeamId());
      pstmt.executeUpdate();

      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          goal.setId(rs.getInt(1));
        }
      }
      return goal;
    }
  }

  @Override
  public Goal findById(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = GOAL_QUERY + " WHERE g.id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapGoal(rs);
        }
      }
    }
    return null;
  }

  @Override
  public List<Goal> findAll() throws ClassNotFoundException, SQLException, IOException {
    List<Goal> goals = new ArrayList<>();
    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(GOAL_QUERY)) {

      while (rs.next()) {
        goals.add(mapGoal(rs));
      }
      return goals;
    }
  }

  @Override
  public Goal update(Goal goal) throws ClassNotFoundException, SQLException, IOException {
    System.out.println(goal.getTeamId());
    String query = "UPDATE \"Goal\" SET name = ?, description = ?, note = ?, tag = ?, projectId = ?, teamId = ? WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, goal.getName());
      pstmt.setString(2, goal.getDescription());
      pstmt.setString(3, goal.getNote());
      pstmt.setString(4, goal.getTag());
      pstmt.setInt(5, goal.getProjectId());
      pstmt.setInt(6, goal.getTeamId());
      pstmt.setInt(7, goal.getId());
      pstmt.executeUpdate();
      return goal;
    }
  }

  @Override
  public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "DELETE FROM \"Goal\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  // Relationship methods
  public List<Result> getGoalResults(int goalId) throws ClassNotFoundException, SQLException, IOException {
    List<Result> results = new ArrayList<>();
    String query = "SELECT * FROM \"Result\" WHERE goalId = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, goalId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Result result = new Result();
          result.setId(rs.getInt("id"));
          result.setCreatedAt(rs.getTimestamp("createdAt"));
          result.setEndsAt(rs.getTimestamp("endsAt"));
          result.setNote(rs.getString("note"));
          result.setTag(rs.getString("tag"));
          result.setGoalId(rs.getInt("goalId"));
          results.add(result);
        }
      }
      return results;
    }
  }
}
