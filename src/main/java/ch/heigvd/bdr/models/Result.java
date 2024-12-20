package ch.heigvd.bdr.models;

import java.sql.Timestamp;

public class Result {
  private int id;
  private Timestamp createdAt;
  private Timestamp endsAt;
  private String note;
  private String tag;
  private int goalId;

  public Result(){}

  public Result(int id, Timestamp createdAt, Timestamp endsAt, String note, String tag, int goalId) {
    this.id = id;
    this.createdAt = createdAt;
    this.endsAt = endsAt;
    this.note = note;
    this.tag = tag;
    this.goalId = goalId;
  }

  // Getters and setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }

  public Timestamp getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(Timestamp endsAt) {
    this.endsAt = endsAt;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public int getGoalId() {
    return goalId;
  }

  public void setGoalId(int goalId) {
    this.goalId = goalId;
  }
}
