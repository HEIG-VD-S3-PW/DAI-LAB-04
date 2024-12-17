package ch.heigvd.bdr.models;

public class MaterialNeed {
  private Material type;

  public MaterialNeed(Material type) {
    this.type = type;
  }

  // Getters and setters
  public Material getType() {
    return type;
  }

  public void setType(Material type) {
    this.type = type;
  }
}
