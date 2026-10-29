package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * First playable increment: dice, turns, castle entry and common-path movement.
 * Home paths, arrows, captures, blocks and special-square effects are not active yet.
 */
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

    private final Difficulty difficulty;
    private final Random random;
    private final List<Player> players = new ArrayList<>();
    private int current = 0;
    private int consecutiveSixes = 0;
    private int stepDie;
    private int extraDie;
    private int steps;
    private boolean awaitingPiece;
    private String message = "Roll the dice to begin.";

    public Game(GameConfig config) { this(config, new Random()); }
    public Game(GameConfig config, Random random) {
        if (config == null || random == null) throw new IllegalArgumentException("Configuration and random are required");
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
            String name = getCurrentPlayer().getName();
            if (stepDie == 6) {
                message = name + ": no legal move, but a 6 grants another roll.";
            } else {
                advanceTurn();
                message = name + ": no legal move. Next player: " + getCurrentPlayer().getName();
            }
        } else {
            message = getCurrentPlayer().getName() + ": select a movable piece.";
        }
    }

    public boolean canMove(int index) {
        if (!awaitingPiece && stepDie == 0) return false;
        if (index < 0 || index >= 4) return false;
        Piece p = getCurrentPlayer().pieces.get(index);
        if (p.isInCastle()) return stepDie == 6;
        // Only common-path movement is available in this increment.
        return steps > 0 && p.getProgress() + steps <= 50;
    }

    private boolean hasAnyLegalMove() {
        for (int i = 0; i < 4; i++) if (canMove(i)) return true;
        return false;
    }

    public void move(int index) {
        if (!awaitingPiece || !canMove(index)) throw new IllegalArgumentException("Illegal piece selection");
        Player player = getCurrentPlayer();
        Piece piece = player.pieces.get(index);
        if (piece.isInCastle()) piece.setProgress(0);
        else piece.setProgress(piece.getProgress() + steps);
        message = player.getName() + " moved piece " + piece.getNumber() + ".";
        awaitingPiece = false;
        if (stepDie != 6) advanceTurn();
        else message += " Rolled 6: roll again.";
    }

    private void advanceTurn() {
        current = (current + 1) % players.size();
        consecutiveSixes = 0;
        awaitingPiece = false;
    }
}
