package ch.heigvd.bdr.models;

public class SubtaskInfo {
    private Task task;
    private boolean isRequired;

    public SubtaskInfo(Task task, boolean isRequired) {
        this.task = task;
        this.isRequired = isRequired;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean isRequired) {
        this.isRequired = isRequired;
    }
}