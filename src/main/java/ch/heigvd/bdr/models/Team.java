package ch.heigvd.bdr.models;

/**
 * Store all the data related to a team
 */
public class Team {
  private int id;
  private String name;
  private int managerId;
  private User manager;

  public Team() {
  }

  public Team(int id, String name, Integer managerId) {
    this.id = id;
    this.name = name;
    this.managerId = managerId;
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

  public void setManagerId(int managerId) {
    this.managerId = managerId;
  }

  public User getManager() {
    return manager;
  }

  public void setManager(User manager) {
    this.manager = manager;
  }

}
