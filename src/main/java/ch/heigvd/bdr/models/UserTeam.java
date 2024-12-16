package ch.heigvd.bdr.models;

public class UserTeam {
  private int id;
  private int userId;
  private int teamId;

  public UserTeam() {
  }

  public UserTeam(int id, int userId, int teamId) {
    this.id = id;
    this.userId = userId;
    this.teamId = teamId;
  }

  // Getters and setters
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public int getTeamId() {
    return teamId;
  }

  public void setTeamId(int teamId) {
    this.teamId = teamId;
  }
}
