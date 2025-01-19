package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.misc.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamDAO implements GenericDAO<Team, Integer> {

    /**
     * Insert a new team
     *
     * @param team: team to insert
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return inserted team
     */
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

    /**
     * Find a team by its id
     *
     * @param id: id to use for research
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return found team
     */
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

    /**
     * Find all the teams
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return List of all the teams
     */
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

    /**
     * Update a specific team
     *
     * @param team: team to update with new values
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return updated team
     */
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

    /**
     * Delete a specific team
     *
     * @param id: id of the task to delete
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the deletion
     */
    @Override
    public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
        String query = "DELETE FROM \"Team\" WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }


    /**
     * Get the manager of a team
     *
     * @param teamId: team to use for research
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return Manager found
     */
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

    /**
     * Add a manager to a team
     *
     * @param userId: user to set as manager
     * @param teamId: team to update
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the update
     */
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

    /**
     * Remove a manager for a specific team
     *
     * @param teamId: team to edit
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the update
     */
    public boolean removeManager(int teamId) throws ClassNotFoundException, SQLException, IOException {
        String query = "UPDATE \"Team\" SET managerId = NULL WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, teamId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Get all the members of a team
     *
     * @param id: id of the team to use for the research
     * @throws Exception
     * @return List of the users
     */
    public List<User> getTeamMembers(int id) throws Exception {
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
