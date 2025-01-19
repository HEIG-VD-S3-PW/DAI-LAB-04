package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.misc.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoalDAO implements GenericDAO<Goal, Integer> {
    private static final String GOAL_QUERY = """
            SELECT g.id AS goal_id, g.name AS goal_name, g.description AS goal_description,
            g.note AS goal_note, g.tag AS goal_tag, g.projectId AS goal_projectId, g.teamId AS goal_teamId,
            t.id AS team_id, t.name AS team_name,
            p.id AS project_id, p.name AS project_name
            FROM "Goal" g
            INNER JOIN "Team" t ON g.teamId = t.id 
            INNER JOIN "Project" p ON g.projectId = p.id
            """;

    /**
     * Used to get all the data from a database
     *
     * @param rs: structure that stores all the data
     * @throws SQLException
     * @return: The goal with the result data
     */
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

    /**
     * Used to insert a new goal
     *
     * @param goal: goal to insert
     * @return inserted goal
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
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

    /**
     * Find a goal by its id
     *
     * @param id: id to search for
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return goal found
     */
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

    /**
     * Get all goals
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return list of all the goals
     */
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

    /**
     * Update a goal
     *
     * @param goal: goal to use for update
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return updated goal
     */
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

    /**
     * Delete a goal
     *
     * @param id: id of the goal to delete
     * @return success of the deletion
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
        String query = "DELETE FROM \"Goal\" WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Find goals related to an user's id
     *
     * @param userId: user id to use for the research
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public List<Goal> getGoalsByUserID(int userId) throws ClassNotFoundException, SQLException, IOException {
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
                    goal.setProjectId(rs.getInt("projectId"));
                    goal.setTeamId(rs.getInt("teamId"));
                    goals.add(goal);
                }
            }
            return goals;
        }
    }
}
