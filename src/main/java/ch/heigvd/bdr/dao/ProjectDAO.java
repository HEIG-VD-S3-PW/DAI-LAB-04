package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO implements GenericDAO<Project, Integer> {

  /**
   * Used to get all the data from a database
   * @param rs: structure that stores all the data
   * @return: The goal with the result data
   * @throws SQLException
   */
  public Project mapToProject(ResultSet rs) throws ClassNotFoundException, SQLException, IOException {
    Project project = new Project();
    project.setId(rs.getInt("id"));
    project.setName(rs.getString("name"));
    project.setDescription(rs.getString("description"));

    return project;
  }

  /**
   * Create a new project
   * @param project: project to insert
   * @return: new project
   * @throws SQLException
   * @throws ClassNotFoundException
   * @throws IOException
   */
  @Override
  public Project create(Project project) throws SQLException, ClassNotFoundException, IOException {
    String query = "INSERT INTO \"Project\" (name, description) VALUES (?, ?) RETURNING id";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, project.getName());
      pstmt.setString(2, project.getDescription());
      pstmt.executeUpdate();

      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          project.setId(rs.getInt(1));
        }
      }
      return project;
    }
  }

  /**
   * Find a project using the id
   * @param id: id to search for
   * @return: found project
   * @throws SQLException
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public Project findById(Integer id) throws SQLException, IOException, ClassNotFoundException {
    String query = "SELECT * FROM \"Project\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          Project project = mapToProject(rs);
          return project;
        }
      }
      return null;
    }
  }

  /**
   * Find all projects
   * @return: list of all the projects
   * @throws SQLException
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public List<Project> findAll() throws SQLException, IOException, ClassNotFoundException {
    List<Project> projects = new ArrayList<>();
    String query = "SELECT * FROM \"Project\"";
    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        Project project = mapToProject(rs);
        projects.add(project);
      }
      return projects;
    }
  }

  /**
   * Update a project
   * @param project: project to use for update
   * @return: updated project
   * @throws SQLException
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public Project update(Project project) throws SQLException, IOException, ClassNotFoundException {
    String query = "UPDATE \"Project\" SET name = ?, description = ? WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, project.getName());
      pstmt.setString(2, project.getDescription());
      pstmt.setInt(3, project.getId());
      pstmt.executeUpdate();
      return project;
    }
  }

  /**
   * Delete a project
   * @param id: id of the project to delete
   * @return: success of the deletion
   * @throws SQLException
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public boolean delete(Integer id) throws SQLException, IOException, ClassNotFoundException {
    String query = "DELETE FROM \"Project\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  /**
   * Get all the goals related to a project
   * @param projectId: project id to use
   * @return: list of all the goals
   * @throws SQLException
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public List<Goal> getProjectGoals(int projectId) throws SQLException, IOException, ClassNotFoundException {
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
