package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Turn management and basic 52-square movement, with capture and Double Block rules. */
public final class Game {
    public static final class Player {
        private final GameConfig.PlayerSetup setup;
        private final List<Piece> pieces = new ArrayList<>();
        private Player(GameConfig.PlayerSetup setup) {
            this.setup = setup;
            for (int i = 1; i <= 4; i++) pieces.add(new Piece(setup.getColor(), i));
        }
        public String getName() { return setup.getName(); }
        public PlayerColor getColor() { return setup.getColor(); }
        public List<Piece> getPieces() { return Collections.unmodifiableList(pieces); }
    }

    private final Board board;
    private final Difficulty difficulty;
    private final Random random;
    private final List<Player> players = new ArrayList<>();
    private int current;
    private int consecutiveSixes;
    private int stepDie;
    private int extraDie;
    private int steps;
    private boolean awaitingPiece;
    private boolean rollAvailable;
    private String message = "Roll the dice to begin.";

    public Game(GameConfig config, Board board) { this(config, board, new Random()); }
    public Game(GameConfig config, Board board, Random random) {
        if (config == null || board == null || random == null || board.getDifficulty() != config.getDifficulty())
            throw new IllegalArgumentException("Configuration, matching board, and random are required");
        this.board = board;
        this.difficulty = config.getDifficulty();
        this.random = random;
        for (GameConfig.PlayerSetup p : config.getPlayers()) players.add(new Player(p));
        Collections.shuffle(players, random);
    }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public Player getCurrentPlayer() { return players.get(current); }
    public int getStepDie() { return stepDie; }
    public int getExtraDie() { return extraDie; }
    public int getSteps() { return steps; }
    public boolean isAwaitingPiece() { return awaitingPiece; }
    public String getMessage() { return message; }

    public void roll() {
        if (awaitingPiece) throw new IllegalStateException("Choose a piece first");
        rollAvailable = true;
        stepDie = random.nextInt(6) + 1;
        extraDie = difficulty == Difficulty.EASY ? 0 : random.nextInt(difficulty == Difficulty.MEDIUM ? 3 : 6) + 1;
        steps = difficulty == Difficulty.EASY ? stepDie : stepDie * (extraDie - 1);
        if (stepDie == 6) consecutiveSixes++;
        else consecutiveSixes = 0;
        if (consecutiveSixes == 3) {
            message = getCurrentPlayer().getName() + " rolled a third 6: turn cancelled.";
            advanceTurn();
            return;
        }
        awaitingPiece = hasAnyLegalMove();
        if (!awaitingPiece) {
            rollAvailable = false;
            String name = getCurrentPlayer().getName();
            if (stepDie == 6) message = name + ": no legal move, but a 6 grants another roll.";
            else {
                advanceTurn();
                message = name + ": no legal move. Next player: " + getCurrentPlayer().getName();
            }
        } else message = getCurrentPlayer().getName() + ": select a movable piece.";
    }

    private int requestedMovement(Piece piece) {
        return piece.isOnHomePath() ? stepDie : steps;
    }

    /** Number of steps possible without crossing/landing on an opposing Double Block. */
    private int reachableMovement(Piece piece) {
        if (piece.isFinished()) return -1;
        if (piece.isInCastle()) {
            if (stepDie != 6) return -1;
            int start = (piece.getColor().ordinal() * 13) % 52;
            return hasOpposingBlock(piece.getColor(), start) ? -1 : 0;
        }
        int requested = requestedMovement(piece);
        if (requested <= 0 || piece.getProgress() + requested > Piece.FINISH_PROGRESS) return -1;
        if (!piece.isOnCommonPath()) return requested;
        for (int distance = 1; distance <= requested; distance++) {
            int progress = piece.getProgress() + distance;
            if (progress >= Piece.HOME_PATH_START) break;
            int square = (piece.getColor().ordinal() * 13 + progress) % 52;
            if (hasOpposingBlock(piece.getColor(), square)) return distance - 1;
        }
        return requested;
    }

    private boolean hasOpposingBlock(PlayerColor movingColor, int boardIndex) {
        for (Player player : players) {
            if (player.getColor() == movingColor) continue;
            int occupied = 0;
            for (Piece piece : player.pieces)
                if (piece.isOnCommonPath() && piece.getBoardIndex() == boardIndex) occupied++;
            if (occupied >= 2) return true;
        }
        return false;
    }

    private boolean hasFullMove() {
        for (Piece piece : getCurrentPlayer().pieces) {
            int reachable = reachableMovement(piece);
            if (reachable >= 0 && (piece.isInCastle() || reachable == requestedMovement(piece))) return true;
        }
        return false;
    }

    /** A truncated move is allowed only if no piece can complete a full move. */
    public boolean canMove(int index) {
        if (!rollAvailable || index < 0 || index >= 4) return false;
        Piece piece = getCurrentPlayer().pieces.get(index);
        int reachable = reachableMovement(piece);
        if (reachable < 0) return false;
        if (piece.isInCastle() || reachable == requestedMovement(piece)) return true;
        return reachable > 0 && !hasFullMove();
    }

    private boolean hasAnyLegalMove() {
        for (int i = 0; i < 4; i++) if (canMove(i)) return true;
        return false;
    }

    private boolean isProtected(int boardIndex) {
        Board.SquareType type = board.getRings().get(0).get(boardIndex).getType();
        return type == Board.SquareType.START || type == Board.SquareType.PROTECTION
            || type == Board.SquareType.SURPRISE;
    }

    private int captureOnLanding(Piece movedPiece) {
        if (!movedPiece.isOnCommonPath() || isProtected(movedPiece.getBoardIndex())) return 0;
        int square = movedPiece.getBoardIndex();
        int captured = 0;
        for (Player player : players) {
            if (player.getColor() == movedPiece.getColor()) continue;
            for (Piece opponent : player.pieces) {
                if (opponent.isOnCommonPath() && opponent.getBoardIndex() == square) {
                    opponent.setProgress(-1);
                    captured++;
                }
            }
        }
        return captured;
    }

    public void move(int index) {
        if (!awaitingPiece || !canMove(index)) throw new IllegalArgumentException("Illegal piece selection");
        Player player = getCurrentPlayer();
        Piece piece = player.pieces.get(index);
        int reachable = reachableMovement(piece);
        boolean shortened = !piece.isInCastle() && reachable < requestedMovement(piece);
        if (piece.isInCastle()) piece.setProgress(0);
        else piece.setProgress(piece.getProgress() + reachable);
        int captured = captureOnLanding(piece);
        message = player.getName() + " moved piece " + piece.getNumber() + ".";
        if (shortened) message += " Stopped before an opposing Double Block.";
        if (captured > 0) message += " Captured opponent piece!";
        if (piece.isFinished()) message += " Reached FINISH HOME!";
        else if (piece.isOnHomePath()) message += " On the home path.";
        awaitingPiece = false;
        rollAvailable = false;
        if (stepDie != 6) advanceTurn();
        else message += " Rolled 6: roll again.";
    }

    private void advanceTurn() {
        current = (current + 1) % players.size();
        consecutiveSixes = 0;
        awaitingPiece = false;
        rollAvailable = false;
    }
}
