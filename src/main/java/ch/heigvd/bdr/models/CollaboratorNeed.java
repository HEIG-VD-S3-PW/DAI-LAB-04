package ch.heigvd.bdr.models;

/**
 * Stores all the data related to the collaborator needs of a task
 */
public class CollaboratorNeed {
  private UserRole type;
  private int quantity;

    public CollaboratorNeed() {
    }

  public CollaboratorNeed(UserRole type, int quantity) {
    this.type = type;
    this.quantity = quantity;
  }

  // Getters and setters
  public UserRole getType() {
    return type;
  }

  public void setType(UserRole type) {
    this.type = type;
  }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

  public static UserRole fromInt(int i) {
    for (UserRole collaborator : UserRole.values()) {
      if (collaborator.getValue() == i) {
        return collaborator;
      }
    }
    throw new IllegalArgumentException("Unexpected value: " + i);
  }
}