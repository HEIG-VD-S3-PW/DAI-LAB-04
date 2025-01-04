package ch.heigvd.bdr.models;

public enum Material {
  LICENSE(0),
  SERVER(1),
  DATABASE(2);
  private final int value;

  Material(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
