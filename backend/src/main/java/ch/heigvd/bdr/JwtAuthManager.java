package ch.heigvd.bdr;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import io.javalin.Javalin;
import io.javalin.security.RouteRole;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtAuthManager {
  private static final String SECRET_KEY = "your-256-bit-secret"; // TODO: Replace with a secure secret key
  private static final String ISSUER_NAME = "okr";
  private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);
  private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

  public enum Roles implements RouteRole {
    ANONYMOUS, USER, ADMIN
  }

  // Login request DTO
  public static class LoginRequest {
    public String username;
    public String password;
  }

  public static String generateToken(String username, List<String> roles) {
    return JWT.create()
        .withIssuer(ISSUER_NAME)
        .withSubject(username)
        .withClaim("roles", roles)
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .withJWTId(UUID.randomUUID().toString())
        .sign(ALGORITHM);
  }

  public static boolean validateToken(String token) {
    try {
      JWTVerifier verifier = JWT.require(ALGORITHM)
          .withIssuer(ISSUER_NAME)
          .build();
      verifier.verify(token);
      return true;
    } catch (JWTVerificationException e) {
      return false;
    }
  }

  public static void configureAuth(Javalin app) {
    app.before(ctx -> {
      if (needsAuth(ctx)) {
        String token = extractToken(ctx);
        if (token == null || !validateToken(token)) {
          ctx.status(HttpStatus.UNAUTHORIZED).json("Invalid or missing token");
          ctx.skipRemainingHandlers();
        }
      }
    });

    // Login endpoint to generate JWT from JSON request
    app.post("/login", ctx -> {
      try {
        // Parse JSON request body to LoginRequest
        LoginRequest loginRequest = ctx.bodyAsClass(LoginRequest.class);

        // Validate credentials
        if (validateCredentials(loginRequest.username, loginRequest.password)) {
          List<String> roles = determineUserRoles(loginRequest.username);
          String token = generateToken(loginRequest.username, roles);
          ctx.json(new TokenResponse(token));
        } else {
          ctx.status(HttpStatus.UNAUTHORIZED).json("Invalid credentials");
        }
      } catch (Exception e) {
        ctx.status(HttpStatus.BAD_REQUEST).json("Invalid request format");
      }
    });
  }

  private static boolean needsAuth(Context ctx) {
    // Define which routes require authentication
    String path = ctx.path();
    return !path.equals("/login") &&
        !path.equals("/") &&
        !path.startsWith("/public");
  }

  private static String extractToken(Context ctx) {
    String authHeader = ctx.header("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return null;
  }

  private static boolean validateCredentials(String username, String password) {
    // Implement actual credential validation
    // TODO: actually use data from db and user attack-safe methods
    return "admin".equals(username) && "password".equals(password);
  }

  private static List<String> determineUserRoles(String username) {
    // TODO: actually use data from db and user attack-safe methods
    return username.equals("admin") ? List.of(Roles.USER.name(), Roles.ADMIN.name()) : List.of(Roles.USER.name());
  }

  // Simple token response class
  static class TokenResponse {
    public String token;

    public TokenResponse(String token) {
      this.token = token;
    }
  }
}
