package ch.heigvd.bdr.models;

public class TaskMaterialNeed {
  private int taskId;
  private Material materialNeedType;
  private int quantity;

  public TaskMaterialNeed(int taskId, Material materialNeedType, int quantity) {
    this.taskId = taskId;
    this.materialNeedType = materialNeedType;
    this.quantity = quantity;
  }

  // Getters and setters
  public int getTaskId() {
    return taskId;
  }

  public void setTaskId(int taskId) {
    this.taskId = taskId;
  }

  public Material getMaterialNeedType() {
    return materialNeedType;
  }

  public void setMaterialNeedType(Material materialNeedType) {
    this.materialNeedType = materialNeedType;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
