package model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Bonus: the Code Quest trophies are earned exactly when their rule is met, and only once. */
class AchievementTrackerTest {

    private final Fixture f = new Fixture(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN);
    private final AchievementTracker tracker = new AchievementTracker();

    private List<AchievementTracker.Unlock> event(PlayerColor who, LogEntry.Kind kind, String text, PlayerColor other) {
        return tracker.onEvent(new LogEntry(who, who.getLabel(), kind, text, 0, other), f.game.getPlayers(), f.board);
    }

    @Test @DisplayName("Hello, World!: the first player to get a piece out of the castle earns it (in the game log too)")
    void helloWorldGoesToTheFirstPieceOut() {
        Fixture easy = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        easy.roll(6);
        easy.game.move(0);
        assertTrue(easy.game.getAchievements().has(PlayerColor.RED, Achievement.HELLO_WORLD));
        assertFalse(easy.game.getAchievements().has(PlayerColor.GREEN, Achievement.HELLO_WORLD));
        assertTrue(easy.game.getLog().stream().anyMatch(e -> e.getKind() == LogEntry.Kind.ACHIEVEMENT
                && e.getText().equals("unlocked Hello, World!")));
    }

    @Test @DisplayName("Bug Squasher after 3 captures, Merge Conflict for the player captured 3 times")
    void threeCaptures() {
        event(PlayerColor.RED, LogEntry.Kind.CAPTURE, "captured", PlayerColor.GREEN);
        event(PlayerColor.RED, LogEntry.Kind.CAPTURE, "captured", PlayerColor.GREEN);
        assertFalse(tracker.has(PlayerColor.RED, Achievement.BUG_SQUASHER));
        event(PlayerColor.RED, LogEntry.Kind.CAPTURE, "captured", PlayerColor.GREEN);
        assertTrue(tracker.has(PlayerColor.RED, Achievement.BUG_SQUASHER));
        assertTrue(tracker.has(PlayerColor.GREEN, Achievement.MERGE_CONFLICT));
        assertFalse(tracker.has(PlayerColor.GREEN, Achievement.BUG_SQUASHER));
    }

    @Test @DisplayName("A trophy is earned only once per player and game")
    void earnedOnlyOnce() {
        for (int i = 0; i < 6; i++) event(PlayerColor.RED, LogEntry.Kind.CAPTURE, "captured", PlayerColor.GREEN);
        long count = tracker.getUnlocks().stream()
                .filter(u -> u.getAchievement() == Achievement.BUG_SQUASHER).count();
        assertEquals(1, count);
    }

    @Test @DisplayName("Stack Overflow needs 3 correct answers in a row; a wrong answer resets the streak")
    void stackOverflowStreak() {
        event(PlayerColor.RED, LogEntry.Kind.CORRECT, "answered an easy question (+1)", null);
        event(PlayerColor.RED, LogEntry.Kind.CORRECT, "answered an easy question (+1)", null);
        event(PlayerColor.RED, LogEntry.Kind.WRONG, "missed an easy question (-1)", null);
        event(PlayerColor.RED, LogEntry.Kind.CORRECT, "answered an easy question (+1)", null);
        event(PlayerColor.RED, LogEntry.Kind.CORRECT, "answered an easy question (+1)", null);
        assertFalse(tracker.has(PlayerColor.RED, Achievement.STACK_OVERFLOW));
        event(PlayerColor.RED, LogEntry.Kind.CORRECT, "answered a medium question (+2)", null);
        assertTrue(tracker.has(PlayerColor.RED, Achievement.STACK_OVERFLOW));
    }

    @Test @DisplayName("Senior Dev only for a correct hard question")
    void seniorDevOnlyForHard() {
        event(PlayerColor.RED, LogEntry.Kind.CORRECT, "answered a medium question (+2)", null);
        assertFalse(tracker.has(PlayerColor.RED, Achievement.SENIOR_DEV));
        event(PlayerColor.RED, LogEntry.Kind.CORRECT, "answered a hard question (+3)", null);
        assertTrue(tracker.has(PlayerColor.RED, Achievement.SENIOR_DEV));
    }

    @Test @DisplayName("Infinite Loop on a third 6, Lucky Commit on the straight-to-FINISH surprise")
    void eventTrophies() {
        event(PlayerColor.GREEN, LogEntry.Kind.TURN_LOST, "rolled a third 6 in a row: turn cancelled", null);
        event(PlayerColor.RED, LogEntry.Kind.SURPRISE, "surprise! piece 2 jumps to FINISH", null);
        assertTrue(tracker.has(PlayerColor.GREEN, Achievement.INFINITE_LOOP));
        assertTrue(tracker.has(PlayerColor.RED, Achievement.LUCKY_COMMIT));
    }

    @Test @DisplayName("Firewall when two of a player's pieces stand on the same common-path square")
    void firewall() {
        Piece red = f.piece(0, 0);
        int square = f.regularRun(red, 3, 0);
        f.put(0, 0, square);
        f.put(0, 1, square);
        event(PlayerColor.RED, LogEntry.Kind.MOVE, "piece 2 moved 3 steps", null);
        assertTrue(tracker.has(PlayerColor.RED, Achievement.FIREWALL));
        assertFalse(tracker.has(PlayerColor.GREEN, Achievement.FIREWALL));
    }

    @Test @DisplayName("No Firewall for two pieces on their own coloured start square")
    void noFirewallOnTheStartSquare() {
        f.put(0, 0, 0);
        f.put(0, 1, 0);
        event(PlayerColor.RED, LogEntry.Kind.MOVE, "piece 2 left the castle", null);
        assertFalse(tracker.has(PlayerColor.RED, Achievement.FIREWALL));
    }

    @Test @DisplayName("Flawless Build for a win without wrong answers or captures; not after a wrong answer")
    void flawlessBuild() {
        event(PlayerColor.RED, LogEntry.Kind.WIN, "brought all 4 pieces home (#1)", null);
        assertTrue(tracker.has(PlayerColor.RED, Achievement.FLAWLESS_BUILD));

        AchievementTracker other = new AchievementTracker();
        other.onEvent(new LogEntry(PlayerColor.GREEN, "Green", LogEntry.Kind.WRONG, "missed", 0), f.game.getPlayers(), f.board);
        other.onEvent(new LogEntry(PlayerColor.GREEN, "Green", LogEntry.Kind.WIN, "brought all 4 pieces home (#1)", 0),
                f.game.getPlayers(), f.board);
        assertFalse(other.has(PlayerColor.GREEN, Achievement.FLAWLESS_BUILD));
    }

    @Test @DisplayName("Comeback Kid: win after having 0 pieces home while an opponent had 2")
    void comebackKid() {
        Piece green = f.piece(1, 0);
        f.put(1, 0, green.getFinishProgress());
        f.put(1, 1, green.getFinishProgress());
        event(PlayerColor.GREEN, LogEntry.Kind.FINISH, "piece 2 reached FINISH", null);   // red has 0 home
        event(PlayerColor.RED, LogEntry.Kind.WIN, "brought all 4 pieces home (#1)", null);
        assertTrue(tracker.has(PlayerColor.RED, Achievement.COMEBACK_KID));
        assertFalse(tracker.has(PlayerColor.GREEN, Achievement.COMEBACK_KID));
    }
}