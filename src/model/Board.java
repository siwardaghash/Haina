package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Initial board layout. Movement/path transitions are implemented separately. */
public final class Board {
    public enum SquareType { REGULAR, START, ARROW, PROTECTION, QUESTION_EASY, QUESTION_MEDIUM, QUESTION_HARD, SURPRISE }

    public static final class Square {
        private final int ring, index;
        private SquareType type = SquareType.REGULAR;
        private PlayerColor owner;
        private Square(int ring, int index) { this.ring = ring; this.index = index; }
        public int getRing() { return ring; }
        public int getIndex() { return index; }
        public SquareType getType() { return type; }
        public PlayerColor getOwner() { return owner; }
    }

    private final List<List<Square>> rings = new ArrayList<>();
    private final Difficulty difficulty;

    public Board(Difficulty difficulty) { this(difficulty, new Random()); }

    /** Random injection makes board generation reproducible in unit tests. */
    public Board(Difficulty difficulty, Random random) {
        if (difficulty == null || random == null) throw new IllegalArgumentException("Difficulty and random are required");
        this.difficulty = difficulty;
        int[] sizes = {52, 64, 72};
        for (int r = 0; r <= difficulty.getExtraRings(); r++) {
            List<Square> ring = new ArrayList<>();
            for (int i = 0; i < sizes[r]; i++) ring.add(new Square(r, i));
            rings.add(Collections.unmodifiableList(ring));
        }
        // Starts are on the outermost ring. Each arrow is the last square
        // before its owner's next inner track (or home path for the base ring).
        // On the base ring it is the middle square at the tip of the arm, two squares
        // before the start, as in the project document's board pictures.
        PlayerColor[] colors = {PlayerColor.RED, PlayerColor.GREEN,
                PlayerColor.YELLOW, PlayerColor.BLUE};
        for (int r = 0; r < rings.size(); r++) {
            List<Square> ring = rings.get(r);
            for (int i = 0; i < colors.length; i++) {
                int start = i * ring.size() / 4;
                int before = r == 0 ? 2 : ring.size() / 8 - 1;                Square arrow = ring.get((start + ring.size() - before) % ring.size());
                arrow.type = SquareType.ARROW;
                arrow.owner = colors[i];
                if (r == rings.size() - 1) {
                    Square startSquare = ring.get(start);
                    startSquare.type = SquareType.START;
                    startSquare.owner = colors[i];
                }
            }
        }
        List<Square> candidates = new ArrayList<>();
        for (List<Square> ring : rings) for (Square square : ring)
            if (square.type == SquareType.REGULAR) candidates.add(square);
        Collections.shuffle(candidates, random);
        int cursor = 0;
        cursor = assign(candidates, cursor, difficulty.getProtectSquares(), SquareType.PROTECTION);
        int perLevel = difficulty.getQuestionSquares() / 3;
        cursor = assign(candidates, cursor, perLevel, SquareType.QUESTION_EASY);
        cursor = assign(candidates, cursor, perLevel, SquareType.QUESTION_MEDIUM);
        cursor = assign(candidates, cursor, perLevel, SquareType.QUESTION_HARD);
        assign(candidates, cursor, difficulty.getSurpriseSquares(), SquareType.SURPRISE);
    }

    private static int assign(List<Square> candidates, int cursor, int count, SquareType type) {
        if (cursor + count > candidates.size()) throw new IllegalStateException("Too few regular squares");
        for (int i = 0; i < count; i++) candidates.get(cursor++).type = type;
        return cursor;
    }

    public Difficulty getDifficulty() { return difficulty; }
    public List<List<Square>> getRings() { return Collections.unmodifiableList(rings); }
    public long count(SquareType type) {
        return rings.stream().flatMap(List::stream).filter(s -> s.type == type).count();
    }
}
