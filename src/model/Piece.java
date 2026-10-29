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
        int total = 0;
        for (int size:trackSizes) total += size;
        commonLength = total;
    }
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
        for (int i=0; i<trackSizes.length; i++) {
            if (p < trackSizes[i]) return trackSizes.length - 1 - i;
            p -= trackSizes[i];
        }
        return -1;
    }
    public int indexAt(int routeProgress) {
        if (routeProgress < 0 || routeProgress >= commonLength) return -1;
        int p = routeProgress;
        for (int size : trackSizes) {
            if (p < size) return (color.ordinal() * (size / 4) + p) % size;
            p -= size;
        }
        return -1;
    }
    void setProgress(int progress) {
        if (progress < -1 || progress > getFinishProgress())
            throw new IllegalArgumentException("Invalid progress: " + progress);
        this.progress = progress;
    }
}
