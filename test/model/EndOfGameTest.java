package model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Finishing and the end of the game: SRS FR-29, FR-30, FR-32. */
class EndOfGameTest {

    /** Three pieces of player p are home, the fourth needs exactly 2 more steps. */
    private static void almostDone(Fixture f, int p) {
        for (int n = 0; n < 3; n++) f.put(p, n, f.piece(p, n).getFinishProgress());
        f.put(p, 3, f.piece(p, 3).getFinishProgress() - 2);
    }

    @Test @DisplayName("FR-29: a player with all four pieces home finishes and gets a place; the game goes on")
    void playerFinishesAndTheGameGoesOn() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.BLUE);
        almostDone(f, 0);
        f.roll(2);
        f.game.move(3);
        assertEquals(1, f.game.getFinishingOrder().size());
        assertSame(f.player(0), f.game.getFinishingOrder().get(0));
        assertFalse(f.game.isGameOver());
    }

    @Test @DisplayName("FR-30: the game ends when all but one have finished; the last player is placed last")
    void gameEndsWhenAllButOneFinished() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        almostDone(f, 0);
        f.roll(2);
        f.game.move(3);
        assertTrue(f.game.isGameOver());
        assertEquals(2, f.game.getFinishingOrder().size());
        assertSame(f.player(1), f.game.getFinishingOrder().get(1));
        assertThrows(IllegalStateException.class, f.game::roll);
    }

    @Test @DisplayName("FR-32: the history record has the course schema fields")
    void historyRecordHasTheCourseFields() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        almostDone(f, 0);
        f.roll(2);
        f.game.move(3);
        GameRecord record = GameRecord.of(f.game, 7);
        assertEquals(7, record.getGameId());
        assertEquals("easy", record.getDifficulty());
        assertEquals("Red", record.getPlaces()[0]);
        assertEquals("Green", record.getPlaces()[1]);
        assertTrue(record.getDuration().matches("\\d\\d:\\d\\d:\\d\\d"));
        assertEquals(8, record.toCsvFields().size());
    }
}
