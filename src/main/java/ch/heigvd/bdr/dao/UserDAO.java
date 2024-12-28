package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO implements GenericDAO<User, Integer> {
  @Override
  public User create(User user) throws ClassNotFoundException, SQLException, IOException {
    String query = "INSERT INTO \"User\" (firstname, lastname, email, role) VALUES (?, ?, ?, ?::\"UserRole\") RETURNING id";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, user.getFirstname());
      pstmt.setString(2, user.getLastname());
      pstmt.setString(3, user.getEmail());
      pstmt.setString(4, user.getRole().name());
      pstmt.executeUpdate();

      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          user.setId(rs.getInt(1));
        }
      }
      return user;
    }
  }

  @Override
  public User findById(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "SELECT * FROM \"User\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          User user = new User();
          user.setId(rs.getInt("id"));
          user.setFirstname(rs.getString("firstname"));
          user.setLastname(rs.getString("lastname"));
          user.setEmail(rs.getString("email"));
          user.setRole(UserRole.valueOf(rs.getString("role")));
          return user;
        }
      }
      return null;
    }
  }

  public User findByEmail(String email) throws ClassNotFoundException, SQLException, IOException {
    String query = "SELECT * FROM \"User\" WHERE email = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, email);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          User user = new User();
          user.setId(rs.getInt("id"));
          user.setFirstname(rs.getString("firstname"));
          user.setLastname(rs.getString("lastname"));
          user.setEmail(rs.getString("email"));
          user.setRole(UserRole.valueOf(rs.getString("role")));
          return user;
        }
      }
      return null;
    }
  }

  @Override
  public List<User> findAll() throws ClassNotFoundException, SQLException, IOException {
    List<User> users = new ArrayList<>();
    String query = "SELECT * FROM \"User\"";
    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirstname(rs.getString("firstname"));
        user.setLastname(rs.getString("lastname"));
        user.setEmail(rs.getString("email"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        users.add(user);
      }
      return users;
    }
  }

  @Override
  public User update(User user) throws ClassNotFoundException, SQLException, IOException {
    String query = "UPDATE \"User\" SET firstname = ?, lastname = ?, email = ?, role = ?::\"UserRole\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, user.getFirstname());
      pstmt.setString(2, user.getLastname());
      pstmt.setString(3, user.getEmail());
      pstmt.setString(4, user.getRole().name());
      pstmt.setInt(5, user.getId());
      pstmt.executeUpdate();
      return user;
    }
  }

  @Override
  public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "DELETE FROM \"User\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  // Relationship methods
  public List<Team> getTeams(int userId) throws ClassNotFoundException, SQLException, IOException {
    List<Team> teams = new ArrayList<>();
    String query = """
        SELECT t.*, manager.id as manager_id,
               manager.firstname as manager_firstname,
               manager.lastname as manager_lastname,
               manager.email as manager_email,
               manager.role as manager_role
        FROM "Team" t
        INNER JOIN \"User_Team\" ut ON t.id = ut.teamId
        LEFT JOIN \"User\" manager ON manager.id = t.managerId
        WHERE ut.userId = ?
        """;

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, userId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Team team = new Team();
          User manager = new User();
          team.setId(rs.getInt("id"));
          team.setName(rs.getString("name"));

          int managerId = rs.getInt("managerId");
          team.setManagerId(managerId);
          if (managerId != 0) {
            manager.setId(rs.getInt("manager_id"));
            manager.setFirstname(rs.getString("manager_firstname"));
            manager.setLastname(rs.getString("manager_lastname"));
            manager.setRole(UserRole.valueOf(rs.getString("manager_role")));
            team.setManager(manager);
          }
          teams.add(team);

        }
      }
      return teams;
    }
  }

  public boolean joinTeam(int userId, int teamId) throws ClassNotFoundException, SQLException, IOException {
    String query = "INSERT INTO \"User_Team\" (userId, teamId) VALUES (?, ?)";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, userId);
      pstmt.setInt(2, teamId);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return true;
        }
      }
      return false;
    }
  }

  public boolean leaveTeam(int userId, int teamId) throws ClassNotFoundException, SQLException, IOException {
    String query = "DELETE FROM \"User_Team\" WHERE userId = ? AND teamId = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, userId);
      pstmt.setInt(2, teamId);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return true;
        }
      }
      return false;
    }
  }

  public List<Goal> getGoals(int userId) throws ClassNotFoundException, SQLException, IOException {
    List<Goal> goals = new ArrayList<>();
    String query = """
          SELECT g.*
          FROM "User_Team" ut
          INNER JOIN "Team" t ON t.id = ut.teamid
          INNER JOIN "Goal" g ON g.teamid  = t.id
          WHERE ut.userid = ?;
        """;

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, userId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Goal goal = new Goal();
          goal.setId(rs.getInt("id"));
          goal.setName(rs.getString("name"));
          goal.setDescription(rs.getString("description"));
          goal.setTag(rs.getString("tag"));
          goal.setNote(rs.getString("note"));

          goals.add(goal);
        }
      }
      return goals;
    }
  }

}
