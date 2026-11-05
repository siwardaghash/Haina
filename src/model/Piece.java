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
        // The route, as in the project document's board pictures (indices for red; the
        // other colours are rotated by a quarter of each ring):
        //  - outermost ring: from the corner start (#) to the colour's arrow;
        //  - every inner ring: entered on the square next to the outer arrow, which is the square
        //    right after this ring's own arrow, then a full lap ending on that arrow;
        //  - the base ring's arrow leads into the home path.
        int rings = trackSizes.length;
        this.firstIndex = new int[rings];
        this.lapLengths = new int[rings];
        for (int i = 0; i < rings; i++) {
            int size = trackSizes[i];
            boolean base = i == rings - 1;
            int arrow = base ? size - 2 : size - Board.ARROW_BEFORE_CORNER;
            if (i == 0) {                       // outermost: from the start corner to the arrow
                firstIndex[i] = 0;
                lapLengths[i] = arrow + 1;
            } else {                            // inner: one full lap, entry = square after the arrow
                firstIndex[i] = (arrow + 1) % size;
                lapLengths[i] = size;
            }
        }
        int total = 0;
        for (int length : lapLengths) total += length;
        commonLength = total;
    }
    private final int[] firstIndex;
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
            if (p < lapLengths[i]) return (color.ordinal() * (size / 4) + firstIndex[i] + p) % size;
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
