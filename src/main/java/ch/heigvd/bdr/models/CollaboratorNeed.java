package ch.heigvd.bdr.models;

public class CollaboratorNeed {
  private UserRole type;

  public CollaboratorNeed(UserRole type) {
    this.type = type;
  }

  // Getters and setters
  public UserRole getType() {
    return type;
  }

  public void setType(UserRole type) {
    this.type = type;
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