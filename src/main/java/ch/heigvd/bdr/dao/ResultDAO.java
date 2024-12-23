package ch.heigvd.bdr.dao;

import ch.heigvd.bdr.DatabaseUtil;
import ch.heigvd.bdr.models.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultDAO implements GenericDAO<Result, Integer> {
  @Override
  public Result create(Result result) throws ClassNotFoundException, SQLException, IOException {
    String query = "INSERT INTO \"Result\" (createdAt, endsAt, note, tag, goalId) " +
        "VALUES (?, ?, ?, ?, ?) RETURNING id";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

      pstmt.setTimestamp(1, result.getCreatedAt());
      pstmt.setTimestamp(2, result.getEndsAt());
      pstmt.setString(3, result.getNote());
      pstmt.setString(4, result.getTag());
      pstmt.setInt(5, result.getGoalId());

      pstmt.executeUpdate();

      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          result.setId(rs.getInt(1));
        }
      }
      return result;
    }
  }

  public Result findById(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "SELECT * FROM \"Result\" WHERE id = ?";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, id);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          Result r = mapResultFromResultSet(rs);
          GoalDAO goalDAO = new GoalDAO();

          r.setGoal(goalDAO.findById(r.getGoalId()));
          return r;
        }
      }
      return null;
    }
  }

  @Override
  public List<Result> findAll() throws ClassNotFoundException, SQLException, IOException {
    List<Result> results = new ArrayList<>();
    String query = "SELECT * FROM \"Result\"";

    try (Connection conn = DatabaseUtil.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        Result r = mapResultFromResultSet(rs);
        GoalDAO goalDAO = new GoalDAO();

        r.setGoal(goalDAO.findById(r.getGoalId()));
        results.add(r);
      }
      return results;
    }
  }

  @Override
  public Result update(Result result) throws ClassNotFoundException, SQLException, IOException {
    String query = "UPDATE \"Result\" SET endsAt = ?, note = ?, tag = ?, goalId = ? " +
        "WHERE id = ?";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setTimestamp(1, result.getEndsAt());
      pstmt.setString(2, result.getNote());
      pstmt.setString(3, result.getTag());
      pstmt.setInt(4, result.getGoalId());
      pstmt.setInt(5, result.getId());

      pstmt.executeUpdate();
      return result;
    }
  }

  @Override
  public boolean delete(Integer id) throws ClassNotFoundException, SQLException, IOException {
    String query = "DELETE FROM \"Result\" WHERE id = ?";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    }
  }

  public List<Result> findByGoalId(Integer goalId) throws ClassNotFoundException, SQLException, IOException {
    List<Result> results = new ArrayList<>();
    String query = "SELECT * FROM \"Result\" WHERE goalId = ?";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, goalId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapResultFromResultSet(rs));
        }
      }
      return results;
    }
  }

  public List<Task> getTasks(Integer resultId) throws Exception {
    List<Task> tasks = new ArrayList<>();
    String query = "SELECT * FROM \"Task\" WHERE resultId = ?";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, resultId);

      try (ResultSet rs = pstmt.executeQuery()) {
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
          task.setRequiredTaskId(rs.getObject("requiredTaskId", Integer.class));
          task.setResultId(rs.getInt("resultId"));
          tasks.add(task);
        }
      }
      return tasks;
    }
  }

  public List<Result> findByDateRange(Timestamp startDate, Timestamp endDate) throws Exception {
    List<Result> results = new ArrayList<>();
    String query = "SELECT * FROM \"Result\" WHERE createdAt BETWEEN ? AND ?";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setTimestamp(1, startDate);
      pstmt.setTimestamp(2, endDate);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapResultFromResultSet(rs));
        }
      }
      return results;
    }
  }

  public List<Result> findByTag(String tag) throws Exception {
    List<Result> results = new ArrayList<>();
    String query = "SELECT * FROM \"Result\" WHERE tag = ?";

    try (Connection conn = DatabaseUtil.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setString(1, tag);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapResultFromResultSet(rs));
        }
      }
      return results;
    }
  }

  private Result mapResultFromResultSet(ResultSet rs) throws SQLException {
    Result result = new Result();
    result.setId(rs.getInt("id"));
    result.setCreatedAt(rs.getTimestamp("createdAt"));
    result.setEndsAt(rs.getTimestamp("endsAt"));
    result.setNote(rs.getString("note"));
    result.setTag(rs.getString("tag"));
    result.setGoalId(rs.getInt("goalId"));
    return result;
  }
}
