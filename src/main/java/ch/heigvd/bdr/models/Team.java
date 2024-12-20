package ch.heigvd.bdr.models;

import java.util.ArrayList;

public class Team {
  private int id;
  private String name;
  private int managerId;

  public Team(int id, String name, Integer managerId) {
    this.id = id;
    this.name = name;
    this.managerId = managerId;
  }

  public Team(int id, String name, Integer managerId, ArrayList<User> users) {
    this(id, name, managerId);
    this.users = users;
  }

  // Getters and setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getManagerId() {
    return managerId;
  }
  
  public void setManagerId(int managerId) {this.managerId = managerId;}

}
