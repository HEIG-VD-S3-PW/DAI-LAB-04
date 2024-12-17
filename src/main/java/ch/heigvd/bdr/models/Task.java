package ch.heigvd.bdr.models;

import java.sql.Timestamp;

public class Task {
  private int id;
  private Timestamp startsAt;
  private short progress;
  private String note;
  private String tag;
  private TaskPriority priority;
  private TaskDeadline deadline;
  private boolean isRequired;
  private int requiredTaskId;
  private int resultId;

  public Task(int id, Timestamp startsAt, short progress, TaskPriority priority,
      TaskDeadline deadline, String note, String tag,
      boolean isRequired, int requiredTaskId, int resultId) {
    this.id = id;
    this.startsAt = startsAt;
    this.progress = progress;
    this.priority = priority;
    this.deadline = deadline;
    this.note = note;
    this.tag = tag;
    this.isRequired = isRequired;
    this.requiredTaskId = requiredTaskId;
    this.resultId = resultId;
  }

  // Getters and setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Timestamp getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(Timestamp startsAt) {
    this.startsAt = startsAt;
  }

  public short getProgress() {
    return progress;
  }

  public void setProgress(short progress) {
    this.progress = progress;
  }

  public TaskPriority getPriority() {
    return priority;
  }

  public void setPriority(TaskPriority priority) {
    this.priority = priority;
  }

  public TaskDeadline getDeadline() {
    return deadline;
  }

  public void setDeadline(TaskDeadline deadline) {
    this.deadline = deadline;
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

  public boolean getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(boolean isRequired) {
    this.isRequired = isRequired;
  }

  public int getRequiredTaskId() {
    return requiredTaskId;
  }

  public void setRequiredTaskId(int requiredTaskId) {
    this.requiredTaskId = requiredTaskId;
  }

  public int getResultId() {
    return resultId;
  }

  public void setResultId(int resultId) {
    this.resultId = resultId;
  }
}
