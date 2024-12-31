package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO implements GenericDAO<Task, Integer> {
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

  @Override
  public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "DELETE FROM \"Task\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  // Relationship methods
  public List<TaskCollaboratorNeed> getTaskCollaboratorNeeds(int taskId)
      throws ClassNotFoundException, SQLException, IOException {
    List<TaskCollaboratorNeed> collaboratorNeeds = new ArrayList<>();
    String query = "SELECT * FROM \"Task_CollaboratorNeed\" WHERE taskId = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, taskId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          TaskCollaboratorNeed need = new TaskCollaboratorNeed();
          need.setTaskId(rs.getInt("taskId"));
          need.setCollaboratorNeedType(UserRole.valueOf(rs.getString("collaboratorNeedType")));
          need.setQuantity(rs.getInt("quantity"));
          collaboratorNeeds.add(need);
        }
      }
      return collaboratorNeeds;
    }
  }

  public List<TaskMaterialNeed> getTaskMaterialNeeds(int taskId)
      throws ClassNotFoundException, SQLException, IOException {
    List<TaskMaterialNeed> materialNeeds = new ArrayList<>();
    String query = "SELECT * FROM \"Task_MaterialNeed\" WHERE taskId = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, taskId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          TaskMaterialNeed need = new TaskMaterialNeed();
          need.setTaskId(rs.getInt("taskId"));
          need.setMaterialNeedType(Material.valueOf(rs.getString("materialNeedType")));
          need.setQuantity(rs.getInt("quantity"));
          materialNeeds.add(need);
        }
      }
      return materialNeeds;
    }
  }

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
