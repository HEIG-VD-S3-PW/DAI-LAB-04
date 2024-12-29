package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.User;
import ch.heigvd.bdr.models.UserRole;
import ch.heigvd.bdr.models.UserTeam;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserTeamDAO {

    public void create(UserTeam userTeam) throws SQLException, IOException, ClassNotFoundException {
        String query = "INSERT INTO \"User_Team\" (userId, teamId) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userTeam.getUserId());
            pstmt.setInt(2, userTeam.getTeamId());
            pstmt.executeUpdate();
        }
    }

    public boolean isUserInTeam(int userId, int teamId) throws SQLException, IOException, ClassNotFoundException {
        String query = "SELECT 1 FROM \"User_Team\" WHERE userId = ? AND teamId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, teamId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void deleteByUserAndTeam(int userId, int teamId) throws SQLException, IOException, ClassNotFoundException {
        String query = "DELETE FROM \"User_Team\" WHERE userId = ? AND teamId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, teamId);
            pstmt.executeUpdate();
        }
    }

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
        }
        return members;
    }
}