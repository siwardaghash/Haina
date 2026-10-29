package model;

/** Tracks a piece through castle, common track, six-square home path and finish. */
public final class Piece {
    public static final int HOME_PATH_START = 51;
    public static final int FINISH_PROGRESS = 57;

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
    public boolean isOnCommonPath() { return progress >= 0 && progress < HOME_PATH_START; }
    public boolean isOnHomePath() { return progress >= HOME_PATH_START && progress < FINISH_PROGRESS; }
    public boolean isFinished() { return progress == FINISH_PROGRESS; }
    /** 0..5 for the six personal home-path cells; -1 otherwise. */
    public int getHomePathIndex() { return isOnHomePath() ? progress - HOME_PATH_START : -1; }
    public int getBoardIndex() {
        if (!isOnCommonPath()) return -1;
        return (color.ordinal() * 13 + progress) % 52;
    }
    void setProgress(int progress) {
        if (progress < -1 || progress > FINISH_PROGRESS) throw new IllegalArgumentException("Invalid progress: " + progress);
        this.progress = progress;
    }
}
