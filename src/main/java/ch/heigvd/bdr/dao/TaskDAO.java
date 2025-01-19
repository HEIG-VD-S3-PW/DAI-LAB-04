package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.misc.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO implements GenericDAO<Task, Integer> {

    /**
     * Used to get all the data from a database
     *
     * @param rs: structure that stores all the data
     * @throws SQLException
     * @return The goal with the result data
     */
    public Task mapToTask(ResultSet rs) throws ClassNotFoundException, SQLException, IOException {
        Task task = new Task();

        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setStartsAt(rs.getTimestamp("startsAt"));
        task.setDone(rs.getBoolean("done"));
        task.setPriority(TaskPriority.valueOf(rs.getString("priority")));
        task.setDeadline(TaskDeadline.valueOf(rs.getString("deadline")));
        task.setNote(rs.getString("note"));
        task.setTag(rs.getString("tag"));
        task.setResultId(rs.getInt("resultId"));

        return task;
    }

    /**
     * Create a new task
     *
     * @param task: task to insert
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return inserted task
     */
    @Override
    public Task create(Task task) throws ClassNotFoundException, SQLException, IOException {
        String query = "INSERT INTO \"Task\" (title, startsAt, done, priority, deadline, note, tag, resultId) "
                +
                "VALUES (?, ?, ?, ?::\"TaskPriority\", ?::\"TaskDeadline\", ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setTimestamp(2, task.getStartsAt());
            pstmt.setBoolean(3, task.getDone());
            pstmt.setString(4, task.getPriority().name());
            pstmt.setString(5, task.getDeadline().name());
            pstmt.setString(6, task.getNote());
            pstmt.setString(7, task.getTag());
            pstmt.setInt(8, task.getResultId());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setId(rs.getInt(1));
                }
            }
            return task;
        }
    }

    /**
     * Find a task by its id
     *
     * @param id: id to use for the research
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return found task
     */
    @Override
    public Task findById(Integer id) throws ClassNotFoundException, SQLException, IOException {
        String query = "SELECT * FROM \"Task\" WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Task task = mapToTask(rs);
                    return task;
                }
            }
            return null;
        }
    }

    /**
     * Find all the tasks
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return List of the tasks
     */
    @Override
    public List<Task> findAll() throws ClassNotFoundException, SQLException, IOException {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT * FROM \"Task\"";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Task task = mapToTask(rs);
                tasks.add(task);
            }
            return tasks;
        }
    }

    /**
     * Update a task
     *
     * @param task: task to update with new values
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return: updated task
     */
    @Override
    public Task update(Task task) throws ClassNotFoundException, SQLException, IOException {
        String query = "UPDATE \"Task\" SET title = ?, startsAt = ?, done = ?, priority = ?::\"TaskPriority\", deadline = ?::\"TaskDeadline\", " +
                "note = ?, tag = ?, resultId = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setTimestamp(2, task.getStartsAt());
            pstmt.setBoolean(3, task.getDone());
            pstmt.setString(4, task.getPriority().name());
            pstmt.setString(5, task.getDeadline().name());
            pstmt.setString(6, task.getNote());
            pstmt.setString(7, task.getTag());
            pstmt.setInt(8, task.getResultId());
            pstmt.setInt(9, task.getId());
            pstmt.executeUpdate();
            return task;
        }
    }

    /**
     * Delete a task
     *
     * @param id: id of the task to delete
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return: success of the deletion
     */
    @Override
    public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
        String query = "DELETE FROM \"Task\" WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Add material needs to a task
     *
     * @param taskId:       task to edit
     * @param materialNeed: material need to add
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the insertion
     */
    public boolean addMaterialNeed(int taskId, MaterialNeed materialNeed) throws ClassNotFoundException, SQLException, IOException {
        if (materialNeed.getQuantity() < 0) {
            return false;
        }

        String query = "INSERT INTO \"Task_MaterialNeed\" (taskId, materialNeedType, quantity) VALUES  (?, ?::\"Material\", ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, taskId);
            pstmt.setString(2, materialNeed.getType().toString());
            pstmt.setInt(3, materialNeed.getQuantity());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Add collaborator needs to a task
     *
     * @param taskId:           task to edit
     * @param collaboratorNeed: collaborator need to add
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the insertion
     */
    public boolean addCollaboratorNeed(int taskId, CollaboratorNeed collaboratorNeed)
            throws ClassNotFoundException, SQLException, IOException {

        if (collaboratorNeed.getQuantity() < 0) {
            return false;
        }

        String query = "INSERT INTO \"Task_CollaboratorNeed\" (taskId, collaboratorNeedType, quantity) VALUES  (?, ?::\"UserRole\", ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, taskId);
            pstmt.setString(2, collaboratorNeed.getType().toString());
            pstmt.setInt(3, collaboratorNeed.getQuantity());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Update the collaborator needs of a task
     *
     * @param taskId:           task to edit
     * @param collaboratorNeed: collaborator needs to edit
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the update
     */
    public boolean updateCollaboratorNeed(int taskId, CollaboratorNeed collaboratorNeed)
            throws ClassNotFoundException, SQLException, IOException {

        if (collaboratorNeed.getQuantity() < 0) {
            return false;
        }

        String query = "UPDATE \"Task_CollaboratorNeed\" SET quantity = ? WHERE taskId = ? AND collaboratorNeedType = ?::\"UserRole\"";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, collaboratorNeed.getQuantity());
            pstmt.setInt(2, taskId);
            pstmt.setString(3, collaboratorNeed.getType().toString());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Update the material needs of a task
     *
     * @param taskId:       task to edit
     * @param materialNeed: material needs to edit
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the update
     */
    public boolean updateMaterialNeed(int taskId, MaterialNeed materialNeed)
            throws ClassNotFoundException, SQLException, IOException {

        if (materialNeed.getQuantity() < 0) {
            return false;
        }

        String query = "UPDATE \"Task_MaterialNeed\" SET quantity = ? WHERE taskId = ? AND materialNeedType = ?::\"Material\"";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, materialNeed.getQuantity());
            pstmt.setInt(2, taskId);
            pstmt.setString(3, materialNeed.getType().toString());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Get all the material needs of a task
     *
     * @param task: task to use for research
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return List of all the material needs
     */
    public List<MaterialNeed> getTaskMaterialNeeds(Task task)
            throws ClassNotFoundException, SQLException, IOException {
        List<MaterialNeed> materialNeeds = new ArrayList<>();
        String query = "SELECT * FROM \"Task_MaterialNeed\" WHERE taskId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, task.getId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MaterialNeed need = new MaterialNeed(Material.valueOf(rs.getString("materialNeedType")), rs.getInt("quantity"));
                    materialNeeds.add(need);
                }
            }
            return materialNeeds;
        }
    }

    /**
     * Get all the collaborator needs of a task
     *
     * @param task: task to use for research
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return: List of all the collaborator needs
     */
    public List<CollaboratorNeed> getTaskCollaboratorNeeds(Task task)
            throws ClassNotFoundException, SQLException, IOException {
        List<CollaboratorNeed> collaboratorNeeds = new ArrayList<>();
        String query = "SELECT * FROM \"Task_CollaboratorNeed\" WHERE taskId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, task.getId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CollaboratorNeed need = new CollaboratorNeed(UserRole.valueOf(rs.getString("collaboratorNeedType")), rs.getInt("quantity"));
                    collaboratorNeeds.add(need);
                }
            }
            return collaboratorNeeds;
        }
    }

    /**
     * Delete a certain material need of a task
     *
     * @param task: task to edit
     * @param need: need to remove
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return: success of the deletion
     */
    public boolean deleteTaskMaterialNeeds(Task task, Material need)
            throws ClassNotFoundException, SQLException, IOException {
        String query = "DELETE FROM \"Task_MaterialNeed\" WHERE taskId = ? AND materialNeedType = ?::\"Material\"";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, task.getId());
            pstmt.setString(2, need.name());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Delete a certain collaborator need of a task
     *
     * @param task: task to edit
     * @param need: need to remove
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the deletion
     */
    public boolean deleteTaskCollaboratorNeeds(Task task, UserRole need)
            throws ClassNotFoundException, SQLException, IOException {
        String query = "DELETE FROM \"Task_CollaboratorNeed\" WHERE taskId = ? AND collaboratorNeedType = ?::\"UserRole\"";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, task.getId());
            pstmt.setString(2, need.name());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Get all the subtasks of a task
     *
     * @param t: task to use for research
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return list of all the subtasks
     */
    public List<SubtaskInfo> getSubtasks(Task t)
            throws ClassNotFoundException, SQLException, IOException {
        List<SubtaskInfo> subtasks = new ArrayList<>();
        String query = "SELECT t.*, ts.required " +
                "FROM \"Task\" t " +
                "INNER JOIN \"Task_Subtask\" ts ON ts.subtaskid = t.id " +
                "WHERE ts.taskid = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, t.getId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapToTask(rs);

                    boolean isRequired = rs.getBoolean("required");
                    SubtaskInfo taskWithRequiredInfo = new SubtaskInfo(task, isRequired);
                    subtasks.add(taskWithRequiredInfo);
                }
            }
            return subtasks;
        }
    }

    /**
     * Create a relationship between a task and subtask
     *
     * @param task:     main task
     * @param subtask:  sub task
     * @param required: if the subtasks needs the subtask for its success
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return result of the insertion
     */
    public boolean addSubtaskRelationship(Task task, Task subtask, boolean required)
            throws ClassNotFoundException, SQLException, IOException {
        String query = "INSERT INTO \"Task_Subtask\" (taskId, subtaskId, required) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, task.getId());
            pstmt.setInt(2, subtask.getId());
            pstmt.setBoolean(3, required);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Update the relationship between a task and a subtask
     *
     * @param task:     main task
     * @param subtask:  sub task
     * @param required: if the subtasks needs the subtask for its success
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return result of the update
     */
    public boolean updateSubtaskRequiredProperty(Task task, Task subtask, boolean required)
            throws ClassNotFoundException, SQLException, IOException {
        String query = "UPDATE \"Task_Subtask\" SET required = ? WHERE taskId = ? AND subtaskId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setBoolean(1, required);
            pstmt.setInt(2, task.getId());
            pstmt.setInt(3, subtask.getId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Delete the relationship between a task and a subtask
     *
     * @param task:    main task
     * @param subtask: sub task
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return success of the deletion
     */
    public boolean deleteSubtaskRelationship(Task task, Task subtask)
            throws ClassNotFoundException, SQLException, IOException {
        String query = "DELETE FROM \"Task_Subtask\" WHERE taskId = ? AND subtaskId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, task.getId());
            pstmt.setInt(2, subtask.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Find all the tasks related to a specific user
     *
     * @param userId: user id to use for the research
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     * @return List of all the tasks
     */
    public List<Task> getTasksByUserID(int userId) throws ClassNotFoundException, SQLException, IOException {
        List<Task> tasks = new ArrayList<>();
        String query = """
                SELECT t.*
                FROM "User_Team" ut
                INNER JOIN "Team" tm ON tm.id = ut.teamid
                INNER JOIN "Goal" g ON g.teamid = tm.id
                INNER JOIN "Result" r ON r.goalid = g.id
                INNER JOIN "Task" t ON t.resultid = r.id
                WHERE ut.userid = ?
                """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task();
                    task.setId(rs.getInt("id"));
                    task.setTitle(rs.getString("title"));
                    task.setStartsAt(rs.getTimestamp("startsAt"));
                    task.setDone(rs.getBoolean("done"));
                    task.setPriority(TaskPriority.valueOf(rs.getString("priority")));
                    task.setDeadline(TaskDeadline.valueOf(rs.getString("deadline")));
                    task.setNote(rs.getString("note"));
                    task.setTag(rs.getString("tag"));
                    task.setResultId(rs.getInt("resultId"));
                    tasks.add(task);
                }
            }
            return tasks;
        }
    }

}
