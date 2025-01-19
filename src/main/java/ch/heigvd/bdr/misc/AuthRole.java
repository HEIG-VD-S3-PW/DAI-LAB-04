package ch.heigvd.bdr.misc;

import io.javalin.security.RouteRole;

/**
 * Used to check user authorizations
 */
public enum AuthRole implements RouteRole {
    NONE(-1),
    NORMAL(1), SUPER(2),
    ANY(3);

    private final int value;

    AuthRole(int v) {
        value = v;
    }

    public int getValue() {
        return this.value;
    }

}
