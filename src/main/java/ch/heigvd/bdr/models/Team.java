package ch.heigvd.bdr.models;

import java.util.ArrayList;

public class Team {
  private int id;
  private String name;
  private int managerId;
  private ArrayList<User> users = new ArrayList<User>();

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

  // TODO : Ask others if the user who has the id must be a manager
  public void setManagerId(int managerId) {this.managerId = managerId;}

  // So the list isn't editable
  public ArrayList<User> getUsers() {return new ArrayList<>(users);}

  public void addUser(User user) {this.users.add(user);}
}
