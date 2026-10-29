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
        private Player(GameConfig.PlayerSetup setup, Difficulty difficulty) {
            this.setup = setup;
            for (int i = 1; i <= 4; i++) pieces.add(new Piece(setup.getColor(), i, difficulty));
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
    private QuestionBank questionBank;
    private QuestionBank.Question pendingQuestion;
    private Piece questionPiece;
    private int questionChain;

    /** Attach the bank at game start; no question window is shown in Easy mode. */
    public void setQuestionBank(QuestionBank bank) { this.questionBank = bank; }
    public QuestionBank.Question getPendingQuestion() { return pendingQuestion; }


    public Game(GameConfig config, Board board) { this(config, board, new Random()); }
    public Game(GameConfig config, Board board, Random random) {
        if (config == null || board == null || random == null || board.getDifficulty() != config.getDifficulty())
            throw new IllegalArgumentException("Configuration, matching board, and random are required");
        this.board = board;
        this.difficulty = config.getDifficulty();
        this.random = random;
        for (GameConfig.PlayerSetup p : config.getPlayers()) players.add(new Player(p, difficulty));
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
        if (awaitingPiece || pendingQuestion != null) throw new IllegalStateException("Resolve the current action first");
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
            return hasOpposingBlock(piece.getColor(), piece.ringAt(0), piece.indexAt(0)) ? -1 : 0;
        }
        int requested = requestedMovement(piece);
        if (requested <= 0 || piece.getProgress() + requested > piece.getFinishProgress()) return -1;
        if (!piece.isOnCommonPath()) return requested;
        for (int distance = 1; distance <= requested; distance++) {
            int progress = piece.getProgress() + distance;
            if (progress >= piece.getCommonLength()) break;
            if (hasOpposingBlock(piece.getColor(), piece.ringAt(progress), piece.indexAt(progress))) return distance - 1;
        }
        return requested;
    }

    private boolean hasOpposingBlock(PlayerColor movingColor, int ring, int boardIndex) {
        for (Player player : players) {
            if (player.getColor() == movingColor) continue;
            int occupied = 0;
            for (Piece piece : player.pieces)
                if (piece.isOnCommonPath() && piece.getRing() == ring && piece.getBoardIndex() == boardIndex) occupied++;
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

    private boolean isProtected(int ring, int boardIndex) {
        Board.SquareType type = board.getRings().get(ring).get(boardIndex).getType();
        return type == Board.SquareType.START || type == Board.SquareType.PROTECTION
            || type == Board.SquareType.SURPRISE;
    }

    private int captureOnLanding(Piece movedPiece) {
        if (!movedPiece.isOnCommonPath() || isProtected(movedPiece.getRing(), movedPiece.getBoardIndex())) return 0;
        int square = movedPiece.getBoardIndex();
        int captured = 0;
        for (Player player : players) {
            if (player.getColor() == movedPiece.getColor()) continue;
            for (Piece opponent : player.pieces) {
                if (opponent.isOnCommonPath() && opponent.getRing() == movedPiece.getRing() && opponent.getBoardIndex() == square) {
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
        questionChain = 0;
        if (!triggerQuestion(piece)) finishTurnAfterMove();
    }

    private boolean triggerQuestion(Piece piece) {
        if (!piece.isOnCommonPath() || questionBank == null || questionChain >= 12) return false;
        Board.SquareType type = board.getRings().get(piece.getRing()).get(piece.getBoardIndex()).getType();
        String level;
        switch (type) {
            case QUESTION_EASY: level = "easy"; break;
            case QUESTION_MEDIUM: level = "medium"; break;
            case QUESTION_HARD: level = "hard"; break;
            default: return false;
        }
        pendingQuestion = questionBank.next(level);
        questionPiece = piece;
        questionChain++;
        message += " Question square: " + level + ".";
        return true;
    }

    /** Answer a pending question. A correct answer moves forward; incorrect moves backward. */
    public boolean answerQuestion(int chosenIndex) {
        if (pendingQuestion == null || chosenIndex < 0 || chosenIndex > 3)
            throw new IllegalStateException("No valid answer to submit");
        QuestionBank.Question question = pendingQuestion;
        boolean correct = question.getCorrect() == chosenIndex;
        int distance = "easy".equals(question.getLevel()) ? 1 : "medium".equals(question.getLevel()) ? 2 : 3;
        int direction = correct ? 1 : -1;
        Piece moved = questionPiece;
        pendingQuestion = null;
        questionPiece = null;
        int original = moved.getProgress();
        int target = Math.max(0, original + direction * distance);
        if (target > moved.getFinishProgress()) target = original; // exact finish only
        // Backward and forward movement must stop before an opposing Double Block.
        if (moved.isOnCommonPath()) {
            int sign = target >= original ? 1 : -1;
            for (int progress = original + sign; sign > 0 ? progress <= target : progress >= target; progress += sign) {
                if (progress < 0 || progress >= moved.getCommonLength()) break;
                if (hasOpposingBlock(moved.getColor(), moved.ringAt(progress), moved.indexAt(progress))) { target = progress - sign; break; }
            }
        }
        moved.setProgress(target);
        int captured = captureOnLanding(moved);
        message = correct ? "Correct! Moved forward " + (target-original) + " step(s)."
                : "Incorrect. Correct answer: " + question.getAnswers()[question.getCorrect()]
                    + ". Movement: " + (target-original) + " step(s).";
        if (captured > 0) message += " Captured an opponent!";
        if (!triggerQuestion(moved)) finishTurnAfterMove();
        return correct;
    }

    private void finishTurnAfterMove() {
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
