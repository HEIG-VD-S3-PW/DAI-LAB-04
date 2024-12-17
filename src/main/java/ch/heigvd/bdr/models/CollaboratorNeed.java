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
}