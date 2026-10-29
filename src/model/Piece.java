package model;

/** A piece's progress on the common path; -1 means it is still in its castle. */
public final class Piece {
    private final PlayerColor color;
    private final int number;
    private int progress = -1;

    public Piece(PlayerColor color, int number) {
        this.color = color;
        this.number = number;
    }
    public PlayerColor getColor() { return color; }
    public int getNumber() { return number; }
    public int getProgress() { return progress; }
    public boolean isInCastle() { return progress == -1; }
    public int getBoardIndex() {
        if (isInCastle()) return -1;
        return (color.ordinal() * 13 + progress) % 52;
    }
    void setProgress(int progress) { this.progress = progress; }
}
