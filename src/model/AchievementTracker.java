package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bonus: watches the move log of one game and decides which trophies the players earn.
 * The game calls {@link #onEvent} after every log entry; each trophy is earned at most once per player.
 */
public final class AchievementTracker {

    /** One earned trophy: who, which one, and when (seconds into the game). */
    public static final class Unlock {
        private final PlayerColor color;
        private final String player;
        private final Achievement achievement;
        private final long secondsIntoGame;

        Unlock(PlayerColor color, String player, Achievement achievement, long secondsIntoGame) {
            this.color = color;
            this.player = player;
            this.achievement = achievement;
            this.secondsIntoGame = secondsIntoGame;
        }

        public PlayerColor getColor() { return color; }
        public String getPlayer() { return player; }
        public Achievement getAchievement() { return achievement; }
        public long getSecondsIntoGame() { return secondsIntoGame; }
    }

    private final List<Unlock> unlocks = new ArrayList<>();
    private final Map<PlayerColor, Set<Achievement>> earned = new HashMap<>();
    private final Map<PlayerColor, Integer> captures = new HashMap<>();
    private final Map<PlayerColor, Integer> timesCaptured = new HashMap<>();
    private final Map<PlayerColor, Integer> correctStreak = new HashMap<>();
    private final Set<PlayerColor> answeredWrong = new HashSet<>();
    /** Players who had 0 pieces home while an opponent already had 2 (for Comeback Kid). */
    private final Set<PlayerColor> wasBehind = new HashSet<>();

    /** All trophies earned in this game, in the order they were earned. */
    public List<Unlock> getUnlocks() { return Collections.unmodifiableList(unlocks); }

    public boolean has(PlayerColor color, Achievement achievement) {
        return earned.containsKey(color) && earned.get(color).contains(achievement);
    }

    /** Checks one new log entry against the rules and returns the trophies it unlocked. */
    public List<Unlock> onEvent(LogEntry e, List<Game.Player> players, Board board) {
        List<Unlock> found = new ArrayList<>();
        PlayerColor actor = e.getColor();
        switch (e.getKind()) {
            case CAPTURE:
                if (add(captures, actor) >= 3) unlock(found, players, actor, Achievement.BUG_SQUASHER, e);
                if (e.getOther() != null && add(timesCaptured, e.getOther()) >= 3)
                    unlock(found, players, e.getOther(), Achievement.MERGE_CONFLICT, e);
                break;
            case CORRECT:
                if (add(correctStreak, actor) >= 3) unlock(found, players, actor, Achievement.STACK_OVERFLOW, e);
                if (e.getText().contains("hard question")) unlock(found, players, actor, Achievement.SENIOR_DEV, e);
                break;
            case WRONG:
                correctStreak.put(actor, 0);
                answeredWrong.add(actor);
                break;
            case TURN_LOST:
                if (e.getText().startsWith("rolled a third 6")) unlock(found, players, actor, Achievement.INFINITE_LOOP, e);
                break;
            case SURPRISE:
                if (e.getText().contains("jumps to FINISH")) unlock(found, players, actor, Achievement.LUCKY_COMMIT, e);
                break;
            case WIN:
                if (e.getText().contains("(#1)")) {
                    if (wasBehind.contains(actor)) unlock(found, players, actor, Achievement.COMEBACK_KID, e);
                    if (!answeredWrong.contains(actor) && timesCaptured.getOrDefault(actor, 0) == 0)
                        unlock(found, players, actor, Achievement.FLAWLESS_BUILD, e);
                }
                break;
            default:
                break;
        }
        checkBoard(found, players, board, e);
        return found;
    }

    /** Trophies that depend on where the pieces are, checked after every event. */
    private void checkBoard(List<Unlock> found, List<Game.Player> players, Board board, LogEntry e) {
        boolean helloTaken = anyoneHas(Achievement.HELLO_WORLD);
        boolean deployedTaken = anyoneHas(Achievement.DEPLOYED);
        int mostHome = 0;
        for (Game.Player p : players) mostHome = Math.max(mostHome, finished(p));
        for (Game.Player p : orderedFrom(players, e.getColor())) {
            List<Piece> pieces = p.getPieces();
            if (!helloTaken && pieces.stream().anyMatch(x -> !x.isInCastle())) {
                unlock(found, players, p.getColor(), Achievement.HELLO_WORLD, e);
                helloTaken = true;
            }
            if (!deployedTaken && pieces.stream().anyMatch(Piece::isFinished)) {
                unlock(found, players, p.getColor(), Achievement.DEPLOYED, e);
                deployedTaken = true;
            }
            if (hasDoubleBlock(pieces, board)) unlock(found, players, p.getColor(), Achievement.FIREWALL, e);
            if (finished(p) == 0 && mostHome >= 2) wasBehind.add(p.getColor());
        }
    }

    /** Two of the pieces on the same common-path square; the coloured start squares do not count. */
    private static boolean hasDoubleBlock(List<Piece> pieces, Board board) {
        for (int i = 0; i < pieces.size(); i++)
            for (int j = i + 1; j < pieces.size(); j++) {
                Piece a = pieces.get(i);
                Piece b = pieces.get(j);
                if (a.isOnCommonPath() && b.isOnCommonPath()
                        && a.getRing() == b.getRing() && a.getBoardIndex() == b.getBoardIndex()
                        && board.getRings().get(a.getRing()).get(a.getBoardIndex()).getType() != Board.SquareType.START)
                    return true;
            }
        return false;
    }

    private static int finished(Game.Player p) {
        int n = 0;
        for (Piece piece : p.getPieces()) if (piece.isFinished()) n++;
        return n;
    }

    /** The player who acted first, so a tie goes to the one who just moved. */
    private static List<Game.Player> orderedFrom(List<Game.Player> players, PlayerColor first) {
        List<Game.Player> ordered = new ArrayList<>();
        for (Game.Player p : players) if (p.getColor() == first) ordered.add(p);
        for (Game.Player p : players) if (p.getColor() != first) ordered.add(p);
        return ordered;
    }

    private boolean anyoneHas(Achievement achievement) {
        for (Set<Achievement> set : earned.values()) if (set.contains(achievement)) return true;
        return false;
    }

    private static int add(Map<PlayerColor, Integer> counts, PlayerColor color) {
        int n = counts.getOrDefault(color, 0) + 1;
        counts.put(color, n);
        return n;
    }

    private void unlock(List<Unlock> found, List<Game.Player> players, PlayerColor color, Achievement a, LogEntry e) {
        Set<Achievement> set = earned.computeIfAbsent(color, c -> EnumSet.noneOf(Achievement.class));
        if (!set.add(a)) return;                    // already earned in this game
        String name = color.getLabel();
        for (Game.Player p : players) if (p.getColor() == color) name = p.getName();
        Unlock unlock = new Unlock(color, name, a, e.getSecondsIntoGame());
        unlocks.add(unlock);
        found.add(unlock);
    }
}