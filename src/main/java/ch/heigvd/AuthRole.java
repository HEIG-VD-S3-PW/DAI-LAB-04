package ch.heigvd;

import io.javalin.security.RouteRole;

/**
 * Used to check user authorizations
 */
public enum AuthRole implements RouteRole {
  NONE(-1),
  NORMAL(1), SUPER(2),
  // NORMAL_READ(1), NORMAL_WRITE(2),
  // SUPER_READ(3), SUPER_WRITE(4),
  ANY(5);

  private final int value;

  AuthRole(int v) {
    value = v;
  }

  public int getValue() {
    return this.value;
  }

}
