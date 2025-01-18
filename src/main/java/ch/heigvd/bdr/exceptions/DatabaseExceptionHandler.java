package ch.heigvd.bdr.exceptions;

import io.javalin.http.Context;

import org.postgresql.util.PSQLException;

public class DatabaseExceptionHandler {
  /**
   * Translate postgreSQL expections into http methods
   * @param e: exception from postgreSQL
   * @param ctx: context to use
   */
  public static void handlePostgreSQLException(PSQLException e, Context ctx) {
    String state = e.getSQLState();
    switch (state) {
      case "23505":
        ctx.status(409).json("Conflict: A resource with the same unique field already exists.");
        break;
      case "23503":
        ctx.status(400).json("Bad Request: Referenced entity not found.");
        break;
      case "23514":
        ctx.status(400).json("Bad Request: Check constraint failed.");
        break;
      default:
        ctx.status(500).json("Database Error: " + e.getMessage());
    }
  }

  /**
   * For all server internal errors
   * @param e: exception thrown
   * @param ctx: context to use
   */
  public static void handleGenericException(Exception e, Context ctx) {
    ctx.status(500).json("Internal Server Error: " + e.getMessage());
  }
}
