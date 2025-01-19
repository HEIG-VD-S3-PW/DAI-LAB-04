package ch.heigvd.bdr.models;

/**
 * Stores all the data related a task's material needs
 */
public class MaterialNeed {
    private Material type;
    private int quantity;

    public MaterialNeed() {
    }

    public MaterialNeed(Material type, int quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    // Getters and setters
    public Material getType() {
        return type;
    }

    public void setType(Material type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
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
