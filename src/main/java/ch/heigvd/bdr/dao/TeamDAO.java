package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamDAO implements GenericDAO<Team, Integer> {

  @Override
  public Team create(Team team) throws ClassNotFoundException, SQLException, IOException {
    String query = "INSERT INTO \"Team\" (name) VALUES (?) RETURNING id";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, team.getName());
      pstmt.executeUpdate();

      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          team.setId(rs.getInt(1));
        }
      }
      return team;
    }
  }

  @Override
  public Team findById(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "SELECT * FROM \"Team\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          Team team = new Team();
          team.setId(rs.getInt("id"));
          team.setName(rs.getString("name"));
            team.setManagerId(rs.getInt("managerId"));
          return team;
        }
      }
      return null;
    }
  }

  @Override
  public List<Team> findAll() throws ClassNotFoundException, SQLException, IOException {
    List<Team> teams = new ArrayList<>();
    String query = "SELECT * FROM \"Team\"";
    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        Team team = new Team();
        team.setId(rs.getInt("id"));
        team.setName(rs.getString("name"));
        team.setManagerId(rs.getInt("managerId"));
        teams.add(team);
      }
      return teams;
    }
  }

  @Override
  public Team update(Team team) throws ClassNotFoundException, SQLException, IOException {
    String query = "UPDATE \"Team\" SET name = ? WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, team.getName());
      pstmt.setInt(2, team.getId());
      pstmt.executeUpdate();
      return team;
    }
  }

  @Override
  public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "DELETE FROM \"Team\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  // Relationship methods
  public List<User> getTeamMembers(int teamId) throws Exception {
    List<User> members = new ArrayList<>();
    String query = "SELECT u.* FROM \"User\" u " +
        "JOIN \"User_Team\" ut ON u.id = ut.userId " +
        "WHERE ut.teamId = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, teamId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          User user = new User();
          user.setId(rs.getInt("id"));
          user.setFirstname(rs.getString("firstname"));
          user.setLastname(rs.getString("lastname"));
          user.setEmail(rs.getString("email"));
          user.setRole(UserRole.valueOf(rs.getString("role")));
          members.add(user);
        }
      }
      return members;
    }
  }



  public User getManager(int teamId) throws ClassNotFoundException, SQLException, IOException {
    String query = "SELECT * FROM User u INNER JOIN Team t ON u.id = t.managerId WHERE t.id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, teamId);

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

  public boolean addManager(int userId, int teamId) throws ClassNotFoundException, SQLException, IOException {
    String query = "UPDATE \"Team\" SET managerId = ? WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, userId);
      pstmt.setInt(2, teamId);

      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected > 0;
    }
  }


  public boolean removeManager(int teamId) throws ClassNotFoundException, SQLException, IOException {
    String query = "UPDATE \"Team\" SET managerId = NULL WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, teamId);

      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected > 0;
    }
  }


    public List<User> getMembers(int id) throws Exception {
        List<User> members = new ArrayList<>();
        String query = "SELECT u.* FROM \"User\" u " +
                "JOIN \"User_Team\" ut ON u.id = ut.userId " +
                "WHERE ut.teamId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setFirstname(rs.getString("firstname"));
                    user.setLastname(rs.getString("lastname"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(UserRole.valueOf(rs.getString("role")));
                    members.add(user);
                }
            }
        }
        return members;
    }

}
