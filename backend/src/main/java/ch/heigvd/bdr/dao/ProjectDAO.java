package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO implements GenericDAO<Project, Integer> {
  @Override
  public Project create(Project project) throws Exception {
    String query = "INSERT INTO \"Project\" (name) VALUES (?) RETURNING id";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, project.getName());
      pstmt.executeUpdate();

      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          project.setId(rs.getInt(1));
        }
      }
      return project;
    }
  }

  @Override
  public Project findById(Integer id) throws Exception {
    String query = "SELECT * FROM \"Project\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          Project project = new Project();
          project.setId(rs.getInt("id"));
          project.setName(rs.getString("name"));
          return project;
        }
      }
      return null;
    }
  }

  @Override
  public List<Project> findAll() throws Exception {
    List<Project> projects = new ArrayList<>();
    String query = "SELECT * FROM \"Project\"";
    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        Project project = new Project();
        project.setId(rs.getInt("id"));
        project.setName(rs.getString("name"));
        projects.add(project);
      }
      return projects;
    }
  }

  @Override
  public Project update(Project project) throws Exception {
    String query = "UPDATE \"Project\" SET name = ? WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, project.getName());
      pstmt.setInt(2, project.getId());
      pstmt.executeUpdate();
      return project;
    }
  }

  @Override
  public boolean delete(Integer id) throws Exception {
    String query = "DELETE FROM \"Project\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  // Relationship methods
  public List<Goal> getProjectGoals(int projectId) throws Exception {
    List<Goal> goals = new ArrayList<>();
    String query = "SELECT * FROM \"Goal\" WHERE projectId = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, projectId);

      try (ResultSet rs = pstmt.executeQuery()) {
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
      }
      return goals;
    }
  }
}
