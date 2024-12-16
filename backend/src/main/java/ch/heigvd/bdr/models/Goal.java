package ch.heigvd.bdr.models;

public class Goal {
  private int id;
  private String name;
  private String description;
  private String note;
  private String tag;
  private int projectId;
  private int teamId;

  public Goal() {
  }

  public Goal(int id, String name, String description, String note, String tag, int projectId, int teamId){
    this.id = id;
    this.name = name;
    this.description = description;
    this.note = note;
    this.tag = tag;
    this.projectId = projectId;
    this.teamId = teamId;
  }

  public Goal(int id, String name, String description, String note, String tag, int projectId) {
    this(id, name, description, note, tag, projectId, 0);
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public int getTeamId() {return teamId;}

  public void setTeamId(int teamId) {this.teamId = teamId;}
}
