package model;

/** A piece's progress follows the outermost track, then inner tracks, then home. */
public final class Piece {
    private final PlayerColor color;
    private final int number;
    private final int[] trackSizes;
    private final int commonLength;
    private int progress = -1;

    public Piece(PlayerColor color, int number) { this(color, number, Difficulty.EASY); }
    public Piece(PlayerColor color, int number, Difficulty difficulty) {
        this.color = color;
        this.number = number;
        int[][] sizes = {{52}, {64,52}, {72,64,52}};
        this.trackSizes = sizes[difficulty.ordinal()].clone();
        // Squares walked on each track: outer tracks are full laps; on the base track the
        // piece stops at its arrow, two squares before its start, so it walks 51 squares.
        this.lapLengths = trackSizes.clone();
        lapLengths[lapLengths.length - 1] = trackSizes[trackSizes.length - 1] - 1;
        int total = 0;
        for (int length : lapLengths) total += length;
        commonLength = total;
    }
    private final int[] lapLengths;
    public PlayerColor getColor() { return color; }
    public int getNumber() { return number; }
    public int getProgress() { return progress; }
    public int getCommonLength() { return commonLength; }
    public int getFinishProgress() { return commonLength + 6; }
    public boolean isInCastle() { return progress == -1; }
    public boolean isOnCommonPath() { return progress >= 0 && progress < commonLength; }
    public boolean isOnHomePath() { return progress >= commonLength && progress < getFinishProgress(); }
    public boolean isFinished() { return progress == getFinishProgress(); }
    public int getHomePathIndex() { return isOnHomePath() ? progress - commonLength : -1; }
    /** Inner ring is 0, medium ring 1, hard outer ring 2. */
    public int getRing() { return ringAt(progress); }
    public int getBoardIndex() { return indexAt(progress); }
    public int ringAt(int routeProgress) {
        if (routeProgress < 0 || routeProgress >= commonLength) return -1;
        int p = routeProgress;
        for (int i=0; i<lapLengths.length; i++) {
            if (p < lapLengths[i]) return trackSizes.length - 1 - i;
            p -= lapLengths[i];
        }
        return -1;
    }
    public int indexAt(int routeProgress) {
        if (routeProgress < 0 || routeProgress >= commonLength) return -1;
        int p = routeProgress;
        for (int i = 0; i < trackSizes.length; i++) {
            int size = trackSizes[i];
            if (p < lapLengths[i]) return (color.ordinal() * (size / 4) + p) % size;
            p -= lapLengths[i];
        }
        return -1;
    }
    void setProgress(int progress) {
        if (progress < -1 || progress > getFinishProgress())
            throw new IllegalArgumentException("Invalid progress: " + progress);
        this.progress = progress;
    }
}
