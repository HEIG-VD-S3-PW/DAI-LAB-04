package ch.heigvd.bdr;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.*;

public class DatabaseUtil {

  // Method to establish and return a database connection
  public static Connection getConnection() throws Exception {
    // Build the connection URL with search_path set to 'public'
    String url = String.format(
        "jdbc:postgresql://db:%s/%s?options=-c%%20search_path=public",
        System.getenv("DB_PORT"),
        System.getenv("DB_NAME"));

    // Load credentials
    Properties props = new Properties();
    String dbUser = System.getenv("DB_USER");
    String dbPassword = new String(Files.readAllBytes(Paths.get("/run/secrets/db_password"))).trim();
    String dbSsl = System.getenv("DB_SSL");

    props.setProperty("user", dbUser);
    props.setProperty("password", dbPassword);
    props.setProperty("ssl", dbSsl);

    // Register the PostgreSQL driver
    Class.forName("org.postgresql.Driver");

    // Return the database connection
    return DriverManager.getConnection(url, props);
  }

  // Method to execute a prepared SELECT statement
  public static void executePreparedQuery(String query, Object... parameters) {
    try (Connection connection = getConnection();
        PreparedStatement pstmt = connection.prepareStatement(query)) {

      // Set parameters for the prepared statement
      for (int i = 0; i < parameters.length; i++) {
        pstmt.setObject(i + 1, parameters[i]);
      }

      // Execute the query
      try (ResultSet rs = pstmt.executeQuery()) {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Print all rows from the result set
        while (rs.next()) {
          for (int i = 1; i <= columnCount; i++) {
            System.out.print(metaData.getColumnName(i) + ": " + rs.getObject(i) + "  ");
          }
          System.out.println();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Method to execute a prepared INSERT/UPDATE/DELETE statement
  public static int executePreparedUpdate(String query, Object... parameters) {
    try (Connection connection = getConnection();
        PreparedStatement pstmt = connection.prepareStatement(query)) {

      // Set parameters for the prepared statement
      for (int i = 0; i < parameters.length; i++) {
        pstmt.setObject(i + 1, parameters[i]);
      }

      // Execute the update and return the affected rows count
      return pstmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }
}
