package ch.heigvd.bdr.models;

/**
 * Used to manage the user's roles on the platform
 */
public enum UserRole {
  ADMIN(0),
  MANAGER(1),
  CONTRIBUTOR(2),
  DEVELOPER(3),
  SCRUM_MASTER(4),
  DATA_SPECIALIST(5);

  private final int value;

  UserRole(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
