package ch.heigvd.bdr.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Stores all the data related to a user
 */
public class User {
  private int id;
  private String firstname;
  private String lastname;
  private String email;
  private UserRole role;
  @JsonIgnore
  private String password;

  public User(){}

  public User(int id, String firstname, String lastname, String email, UserRole role) {
    this.id = id;
    this.firstname = firstname;
    this.lastname = lastname;
    this.email = email;
    this.role = role;
  }

  // Getters and setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }
}
