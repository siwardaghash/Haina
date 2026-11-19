package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Test helper: a game with known players, dice and piece positions. Player 0 starts. */
final class Fixture {
    final ScriptedRandom dice = new ScriptedRandom();
    final Board board;
    final Game game;

    Fixture(Difficulty difficulty, PlayerColor... colors) { this(difficulty, GameConfig.GameMode.CLASSIC, colors); }

    Fixture(Difficulty difficulty, GameConfig.GameMode mode, PlayerColor... colors) {
        board = new Board(difficulty, new Random(7));
        List<GameConfig.PlayerSetup> players = new ArrayList<>();
        for (PlayerColor c : colors) players.add(new GameConfig.PlayerSetup(c.getLabel(), c));
        game = new Game(new GameConfig(difficulty, players, mode), board, dice);
    }

    Game.Player player(int p) { return game.getPlayers().get(p); }
    Piece piece(int p, int n) { return player(p).getPieces().get(n); }
    void put(int p, int n, int progress) { piece(p, n).setProgress(progress); }

    /** Rolls the step die only (Easy), or the step die and the extra die (Medium/Hard). */
    void roll(int step) { dice.push(step - 1); game.roll(); }
    void roll(int step, int extra) { dice.push(step - 1, extra - 1); game.roll(); }

    Board.SquareType typeAt(Piece piece, int progress) {
        return board.getRings().get(piece.ringAt(progress)).get(piece.indexAt(progress)).getType();
    }

    /** The first route position from {@code from} on the common path whose square has that type, or -1. */
    int find(Piece piece, Board.SquareType type, int from) {
        for (int p = Math.max(0, from); p < piece.getCommonLength(); p++) if (typeAt(piece, p) == type) return p;
        return -1;
    }

    /** The route position of {@code other} that is the same board square as {@code piece} at {@code progress}. */
    int sameSquare(Piece other, Piece piece, int progress) {
        for (int q = 0; q < other.getCommonLength(); q++)
            if (other.ringAt(q) == piece.ringAt(progress) && other.indexAt(q) == piece.indexAt(progress)) return q;
        return -1;
    }

    /** A route position p where p .. p+span are all regular squares. */
    int regularRun(Piece piece, int from, int span) {
        for (int p = Math.max(1, from); p + span < piece.getCommonLength(); p++) {
            boolean ok = true;
            for (int k = 0; k <= span; k++) if (typeAt(piece, p + k) != Board.SquareType.REGULAR) { ok = false; break; }
            if (ok) return p;
        }
        return -1;
    }

    /** A bank with exactly the 10 questions per level the game needs (SRS: at least 10 per level). */
    static QuestionBank questionBank() {
        List<Question> qs = new ArrayList<>();
        for (String level : new String[]{"easy", "medium", "hard"})
            for (int i = 1; i <= 10; i++)
                qs.add(new Question(level.charAt(0) + "" + i, level, level + " question " + i,
                        new String[]{"a", "b", "c", "d"}, 1));
        return new QuestionBank(qs, new Random(3));
    }
}
