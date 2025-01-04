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

  public static Material fromInt(int i) {
    for (Material material : Material.values()) {
      if (material.getValue() == i) {
        return material;
      }
    }
    throw new IllegalArgumentException("Unexpected value: " + i);
  }
}
