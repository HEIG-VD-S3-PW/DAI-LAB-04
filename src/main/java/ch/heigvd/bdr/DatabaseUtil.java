package ch.heigvd.bdr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.*;

public class DatabaseUtil {

  // Method to establish and return a database connection
  public static Connection getConnection() throws ClassNotFoundException, IOException, SQLException {
    String url = String.format(
        "jdbc:postgresql://db:%s/%s?options=-c%%20search_path=public",
        System.getenv("DB_PORT"),
        System.getenv("DB_NAME"));

    Properties props = new Properties();
    String dbUser = System.getenv("DB_USER");
    String dbPassword = new String(Files.readAllBytes(Paths.get("/run/secrets/db_password"))).trim();
    String dbSsl = System.getenv("DB_SSL");

    props.setProperty("user", dbUser);
    props.setProperty("password", dbPassword);
    props.setProperty("ssl", dbSsl);

    Class.forName("org.postgresql.Driver");

    return DriverManager.getConnection(url, props);
  }

  // Method to execute a prepared SELECT statement
  public static void executePreparedQuery(String query, Object... parameters)
      throws ClassNotFoundException, IOException, SQLException {
    try (Connection connection = getConnection();
        PreparedStatement pstmt = connection.prepareStatement(query)) {

      for (int i = 0; i < parameters.length; i++) {
        pstmt.setObject(i + 1, parameters[i]);
      }

      try (ResultSet rs = pstmt.executeQuery()) {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
          for (int i = 1; i <= columnCount; i++) {
            System.out.print(metaData.getColumnName(i) + ": " + rs.getObject(i) + "  ");
          }
          System.out.println();
        }
      }
    }
  }

  // Method to execute a prepared INSERT/UPDATE/DELETE statement
  public static int executePreparedUpdate(String query, Object... parameters)
      throws ClassNotFoundException, IOException, SQLException {
    try (Connection connection = getConnection();
        PreparedStatement pstmt = connection.prepareStatement(query)) {

      for (int i = 0; i < parameters.length; i++) {
        pstmt.setObject(i + 1, parameters[i]);
      }

      return pstmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }
}
