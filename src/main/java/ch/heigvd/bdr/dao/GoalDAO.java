package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoalDAO implements GenericDAO<Goal, Integer> {
  @Override
  public Goal create(Goal goal) throws Exception {
    String query = "INSERT INTO \"Goal\" (name, description, note, tag, projectId, teamId) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, goal.getName());
      pstmt.setString(2, goal.getDescription());
      pstmt.setString(3, goal.getNote());
      pstmt.setString(4, goal.getTag());
      pstmt.setInt(5, goal.getProjectId());
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
  public Goal findById(Integer id) throws Exception {
    String query = "SELECT * FROM \"Goal\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          Goal goal = new Goal();
          goal.setId(rs.getInt("id"));
          goal.setName(rs.getString("name"));
          goal.setDescription(rs.getString("description"));
          goal.setNote(rs.getString("note"));
          goal.setTag(rs.getString("tag"));
          goal.setProjectId(rs.getInt("projectId"));
          return goal;
        }
      }
      return null;
    }
  }

  @Override
  public List<Goal> findAll() throws Exception {
    List<Goal> goals = new ArrayList<>();
    String query = "SELECT * FROM \"Goal\"";
    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        Goal goal = new Goal();
        goal.setId(rs.getInt("id"));
        goal.setName(rs.getString("name"));
        goal.setDescription(rs.getString("description"));
        goal.setNote(rs.getString("note"));
        goal.setTag(rs.getString("tag"));
        goal.setProjectId(rs.getInt("projectId"));
        goals.add(goal);
      }
      return goals;
    }
  }

  @Override
  public Goal update(Goal goal) throws Exception {
    String query = "UPDATE \"Goal\" SET name = ?, description = ?, note = ?, tag = ?, projectId = ?, teamId = ? WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, goal.getName());
      pstmt.setString(2, goal.getDescription());
      pstmt.setString(3, goal.getNote());
      pstmt.setString(4, goal.getTag());
      pstmt.setInt(5, goal.getProjectId());
      pstmt.setInt(6, goal.getId());
      pstmt.executeUpdate();
      return goal;
    }
  }

  @Override
  public boolean delete(Integer id) throws Exception {
    String query = "DELETE FROM \"Goal\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  // Relationship methods
  public List<Result> getGoalResults(int goalId) throws Exception {
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
