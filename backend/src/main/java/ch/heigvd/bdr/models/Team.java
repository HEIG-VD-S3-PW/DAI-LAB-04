package ch.heigvd.bdr.models;

public class Team {
  private int id;
  private String name;
  private Integer managerId;

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

  public Integer getManagerId() {
    return managerId;
  }

  public void setManagerId(Integer managerId) {
    this.managerId = managerId;
  }
}
