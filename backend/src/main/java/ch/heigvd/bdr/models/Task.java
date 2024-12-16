package ch.heigvd.bdr.models;

import java.sql.Timestamp;

public class Task {
  private int id;
  private Timestamp startsAt;
  private Short progress;
  private TaskPriority priority;
  private TaskDeadline deadline;
  private String note;
  private String tag;
  private Boolean isRequired;
  private Integer requiredTaskId;
  private int resultId;

  public Task() {
  }

  public Task(int id, Timestamp startsAt, Short progress, TaskPriority priority,
      TaskDeadline deadline, String note, String tag,
      Boolean isRequired, Integer requiredTaskId, int resultId) {
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

  public Short getProgress() {
    return progress;
  }

  public void setProgress(Short progress) {
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

  public Boolean getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(Boolean isRequired) {
    this.isRequired = isRequired;
  }

  public Integer getRequiredTaskId() {
    return requiredTaskId;
  }

  public void setRequiredTaskId(Integer requiredTaskId) {
    this.requiredTaskId = requiredTaskId;
  }

  public int getResultId() {
    return resultId;
  }

  public void setResultId(int resultId) {
    this.resultId = resultId;
  }
}
