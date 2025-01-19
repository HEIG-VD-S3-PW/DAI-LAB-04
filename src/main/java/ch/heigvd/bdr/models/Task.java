package ch.heigvd.bdr.models;

import java.sql.Timestamp;

/**
 * Stores all the data related a result's goal
 */
public class Task {
    private int id;
    private String title;
    private Timestamp startsAt;
    private boolean done;
    private String note;
    private String tag;
    private TaskPriority priority;
    private TaskDeadline deadline;
    private int resultId;

    public Task() {
    }

    public Task(int id, String title, Timestamp startsAt, boolean done, TaskPriority priority,
                TaskDeadline deadline, String note, String tag,
                int resultId) {
        this.id = id;
        this.title = title;
        this.startsAt = startsAt;
        this.done = done;
        this.priority = priority;
        this.deadline = deadline;
        this.note = note;
        this.tag = tag;
        this.resultId = resultId;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Timestamp getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Timestamp startsAt) {
        this.startsAt = startsAt;
    }

    public boolean getDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
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

    public int getResultId() {
        return resultId;
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
    }
}
