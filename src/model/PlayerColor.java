package model;

public enum PlayerColor {
    RED("Red"),
    GREEN("Green"),
    YELLOW("Yellow"),
    BLUE("Blue");

    private final String label;

    PlayerColor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}