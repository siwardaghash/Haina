package model;

import java.util.Random;

/**
 * Bonus (SRS 2.6: computer players are an allowed addition): the decisions of a computer player.
 * It only reads the game and returns a choice; the rules themselves stay in {@link Game}.
 *
 * Piece choice, best first: finish a piece, capture an opponent, enter the home path,
 * bring a piece out with a 6, move a piece out of danger, land on a safe square,
 * form a Double Block; otherwise move the piece that is farthest along. It avoids landing
 * where an opponent can capture it on the next roll.
 */
public final class Bot {
    /** Chance of a correct answer per question level (team decision): easy, medium, hard. */
    private static final double[] CORRECT_CHANCE = {0.80, 0.60, 0.45};

    private final Random random;

    public Bot(Random random) { this.random = random; }

    /** The piece (0-3) of the current player to move after a roll, or -1 when none can move. */
    public int choosePiece(Game game) {
        Game.Player me = game.getCurrentPlayer();
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < me.getPieces().size(); i++) {
            if (!game.canMove(i)) continue;
            double score = score(game, me, me.getPieces().get(i), game.getPreviewLandingProgress(i));
            if (score > bestScore) { bestScore = score; best = i; }
        }
        return best;
    }

    private double score(Game game, Game.Player me, Piece piece, int landing) {
        double score = Math.max(0, piece.getProgress()) * 0.05;         // tie-break: the piece farthest along
        if (landing >= piece.getFinishProgress()) return 1000;          // a piece reaches Finish Home
        if (piece.isInCastle()) score += 60;                           // a 6 brings a new piece out
        if (landing >= piece.getCommonLength()) {                      // the home path: nobody can capture it there
            if (piece.isOnCommonPath()) score += 70;
            return score + 5;
        }
        int ring = piece.ringAt(landing), index = piece.indexAt(landing);
        boolean safe = game.isSafeSquare(ring, index);
        if (!safe && opponentsOn(game, me, ring, index) > 0) score += 90;   // capture
        if (piece.isOnCommonPath() && !game.isSafeSquare(piece.getRing(), piece.getBoardIndex())
                && threatened(game, me, piece.getRing(), piece.getBoardIndex())) score += 25;   // leave danger
        if (safe) score += 15;
        else if (threatened(game, me, ring, index)) score -= 30;     // would be in danger after the move
        if (ownOn(game, me, ring, index) > 0) score += 10;            // forms a Double Block
        return score;
    }

    /** Opponent pieces (not teammates) standing on that square. */
    private int opponentsOn(Game game, Game.Player me, int ring, int index) {
        int n = 0;
        for (Game.Player other : game.getPlayers()) {
            if (other == me || (game.isTeamMode() && game.getTeamOf(other.getColor()) == game.getTeamOf(me.getColor()))) continue;
            for (Piece p : other.getPieces())
                if (p.isOnCommonPath() && p.getRing() == ring && p.getBoardIndex() == index) n++;
        }
        return n;
    }

    private int ownOn(Game game, Game.Player me, int ring, int index) {
        int n = 0;
        for (Piece p : me.getPieces())
            if (p.isOnCommonPath() && p.getRing() == ring && p.getBoardIndex() == index) n++;
        return n;
    }

    /** True when an opponent piece stands 1 to 6 squares behind that square on the same track. */
    private boolean threatened(Game game, Game.Player me, int ring, int index) {
        int size = game.getBoard().getRings().get(ring).size();
        for (Game.Player other : game.getPlayers()) {
            if (other == me || (game.isTeamMode() && game.getTeamOf(other.getColor()) == game.getTeamOf(me.getColor()))) continue;
            for (Piece p : other.getPieces()) {
                if (!p.isOnCommonPath() || p.getRing() != ring) continue;
                int behind = Math.floorMod(index - p.getBoardIndex(), size);
                if (behind >= 1 && behind <= 6) return true;
            }
        }
        return false;
    }

    /** The answer (0-3) the bot gives: right with a chance that depends on the level. */
    public int chooseAnswer(Question question) {
        String level = question.getLevel();
        double chance = "easy".equals(level) ? CORRECT_CHANCE[0] : "medium".equals(level) ? CORRECT_CHANCE[1] : CORRECT_CHANCE[2];
        if (random.nextDouble() < chance) return question.getCorrect();
        int wrong = random.nextInt(3);
        return wrong >= question.getCorrect() ? wrong + 1 : wrong;
    }

    /** The opponent piece to send back ("move an opponent back" surprise): the one farthest along. {player, piece} or null. */
    public int[] chooseSurpriseTarget(Game game) {
        int[] best = null;
        int bestProgress = -1;
        for (int p = 0; p < game.getPlayers().size(); p++)
            for (int i = 0; i < 4; i++)
                if (game.canSelectSurpriseTarget(p, i)) {
                    int progress = game.getPlayers().get(p).getPieces().get(i).getProgress();
                    if (progress > bestProgress) { bestProgress = progress; best = new int[]{p, i}; }
                }
        return best;
    }
}
