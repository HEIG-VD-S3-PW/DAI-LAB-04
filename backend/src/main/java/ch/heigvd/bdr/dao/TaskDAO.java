package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO implements GenericDAO<Task, Integer> {
  @Override
  public Task create(Task task) throws Exception {
    String query = "INSERT INTO \"Task\" (startsAt, progress, priority, deadline, note, tag, isRequired, requiredTaskId, resultId) "
        +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setTimestamp(1, task.getStartsAt());
      pstmt.setShort(2, task.getProgress());
      pstmt.setString(3, task.getPriority().name());
      pstmt.setString(4, task.getDeadline().name());
      pstmt.setString(5, task.getNote());
      pstmt.setString(6, task.getTag());
      pstmt.setBoolean(7, task.getIsRequired());
      pstmt.setObject(8, task.getRequiredTaskId());
      pstmt.setInt(9, task.getResultId());
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
  public Task findById(Integer id) throws Exception {
    String query = "SELECT * FROM \"Task\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          Task task = new Task();
          task.setId(rs.getInt("id"));
          task.setStartsAt(rs.getTimestamp("startsAt"));
          task.setProgress(rs.getShort("progress"));
          task.setPriority(TaskPriority.valueOf(rs.getString("priority")));
          task.setDeadline(TaskDeadline.valueOf(rs.getString("deadline")));
          task.setNote(rs.getString("note"));
          task.setTag(rs.getString("tag"));
          task.setIsRequired(rs.getBoolean("isRequired"));
          task.setRequiredTaskId(rs.getObject("requiredTaskId") != null ? rs.getInt("requiredTaskId") : null);
          task.setResultId(rs.getInt("resultId"));
          return task;
        }
      }
      return null;
    }
  }

  @Override
  public List<Task> findAll() throws Exception {
    List<Task> tasks = new ArrayList<>();
    String query = "SELECT * FROM \"Task\"";
    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setStartsAt(rs.getTimestamp("startsAt"));
        task.setProgress(rs.getShort("progress"));
        task.setPriority(TaskPriority.valueOf(rs.getString("priority")));
        task.setDeadline(TaskDeadline.valueOf(rs.getString("deadline")));
        task.setNote(rs.getString("note"));
        task.setTag(rs.getString("tag"));
        task.setIsRequired(rs.getBoolean("isRequired"));
        task.setRequiredTaskId(rs.getObject("requiredTaskId") != null ? rs.getInt("requiredTaskId") : null);
        task.setResultId(rs.getInt("resultId"));
        tasks.add(task);
      }
      return tasks;
    }
  }

  @Override
  public Task update(Task task) throws Exception {
    String query = "UPDATE \"Task\" SET startsAt = ?, progress = ?, priority = ?, deadline = ?, " +
        "note = ?, tag = ?, isRequired = ?, requiredTaskId = ?, resultId = ? WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setTimestamp(1, task.getStartsAt());
      pstmt.setShort(2, task.getProgress());
      pstmt.setString(3, task.getPriority().name());
      pstmt.setString(4, task.getDeadline().name());
      pstmt.setString(5, task.getNote());
      pstmt.setString(6, task.getTag());
      pstmt.setBoolean(7, task.getIsRequired());
      pstmt.setObject(8, task.getRequiredTaskId());
      pstmt.setInt(9, task.getResultId());
      pstmt.setInt(10, task.getId());
      pstmt.executeUpdate();
      return task;
    }
  }

  @Override
  public boolean delete(Integer id) throws Exception {
    String query = "DELETE FROM \"Task\" WHERE id = ?";
    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  // Relationship methods
  public List<TaskCollaboratorNeed> getTaskCollaboratorNeeds(int taskId) throws Exception {
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

  public List<TaskMaterialNeed> getTaskMaterialNeeds(int taskId) throws Exception {
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
}
