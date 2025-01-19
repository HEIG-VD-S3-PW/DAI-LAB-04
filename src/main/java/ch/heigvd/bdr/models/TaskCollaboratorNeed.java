package ch.heigvd.bdr.models;

/**
 * Stores all the data to link a task with a collaborator need
 */
public class TaskCollaboratorNeed {
    private int taskId;
    private UserRole collaboratorNeedType;
    private int quantity;

    public TaskCollaboratorNeed() {
    }

    public TaskCollaboratorNeed(int taskId, UserRole collaboratorNeedType, int quantity) {
        this.taskId = taskId;
        this.collaboratorNeedType = collaboratorNeedType;
        this.quantity = quantity;
    }

    // Getters and setters
    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public UserRole getCollaboratorNeedType() {
        return collaboratorNeedType;
    }

    public void setCollaboratorNeedType(UserRole collaboratorNeedType) {
        this.collaboratorNeedType = collaboratorNeedType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
