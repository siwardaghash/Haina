package model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Piece movement: SRS FR-13, FR-15, FR-18, FR-19. */
class MovementTest {

    @Test @DisplayName("FR-13: the chosen piece moves the whole roll; the others stay")
    void onePieceMovesTheWholeRoll() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        Piece a = f.piece(0, 0), b = f.piece(0, 1);
        int pa = f.regularRun(a, 2, 4), pb = f.regularRun(b, pa + 8, 4);
        f.put(0, 0, pa);
        f.put(0, 1, pb);
        f.roll(4);
        f.game.move(1);
        assertEquals(pa, a.getProgress());
        assertEquals(pb + 4, b.getProgress());
    }

    @Test @DisplayName("FR-13: a piece that cannot move cannot be chosen")
    void illegalPieceCannotBeChosen() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        f.put(0, 0, 10);
        f.roll(3);
        assertFalse(f.game.canMove(1));                    // still in the castle
        assertThrows(IllegalArgumentException.class, () -> f.game.move(1));
    }

    @Test @DisplayName("FR-15: Medium starts on the outer track and ends on the base track, changing at its own arrow")
    void routeChangesTrackAtOwnArrow() {
        Fixture f = new Fixture(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN);
        Piece red = f.piece(0, 0);
        assertEquals(1, red.ringAt(0));
        assertEquals(0, red.ringAt(red.getCommonLength() - 1));
        int change = -1;
        for (int p = 1; p < red.getCommonLength(); p++) if (red.ringAt(p) != red.ringAt(p - 1)) { change = p; break; }
        assertTrue(change > 0);
        Board.Square arrow = f.board.getRings().get(red.ringAt(change - 1)).get(red.indexAt(change - 1));
        assertEquals(Board.SquareType.ARROW, arrow.getType());
        assertEquals(PlayerColor.RED, arrow.getOwner());
    }

    @Test @DisplayName("FR-18: Finish Home needs the exact number; a roll that is too big cannot be used")
    void finishNeedsTheExactNumber() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        Piece red = f.piece(0, 0);
        f.put(0, 0, red.getFinishProgress() - 3);
        f.roll(5);
        assertFalse(f.game.canMove(0));
        assertSame(f.player(1), f.game.getCurrentPlayer());     // no other piece: the turn passes
        f.roll(1);                                               // green: nothing out, turn back to red
        f.roll(3);
        f.game.move(0);
        assertTrue(red.isFinished());
    }

    @Test @DisplayName("FR-19: on the home path only the step die counts")
    void homePathUsesTheStepDieOnly() {
        Fixture f = new Fixture(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN);
        Piece red = f.piece(0, 0);
        f.put(0, 0, red.getCommonLength() + 1);
        f.roll(2, 3);                                // multiplied result would be 4
        assertTrue(f.game.canMove(0));
        f.game.move(0);
        assertEquals(red.getCommonLength() + 3, red.getProgress());
    }
}
