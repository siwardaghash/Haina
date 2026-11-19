package model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Capture and Double Block: SRS FR-16, FR-17, FR-20. */
class CaptureAndBlockTest {

    /** Red piece 0 at a regular square, green piece 0 three regular squares ahead of it. */
    private static int[] redBehindGreen(Fixture f, int gap) {
        Piece red = f.piece(0, 0), green = f.piece(1, 0);
        for (int p = f.regularRun(red, 2, gap); p >= 0; p = f.regularRun(red, p + 1, gap)) {
            int q = f.sameSquare(green, red, p + gap);
            if (q > 0) { f.put(0, 0, p); f.put(1, 0, q); return new int[]{p, q}; }
        }
        throw new AssertionError("no suitable squares");
    }

    @Test @DisplayName("FR-16: landing on a single opponent on a regular square sends it to its castle")
    void captureSendsTheOpponentHome() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        redBehindGreen(f, 3);
        f.roll(3);
        f.game.move(0);
        assertTrue(f.piece(1, 0).isInCastle());
    }

    @Test @DisplayName("FR-16: no capture on a protected square")
    void noCaptureOnProtectedSquare() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        Piece red = f.piece(0, 0), green = f.piece(1, 0);
        int star = f.find(red, Board.SquareType.PROTECTION, 4);
        f.put(0, 0, star - 2);
        f.put(1, 0, f.sameSquare(green, red, star));
        f.roll(2);
        f.game.move(0);
        assertFalse(green.isInCastle());
        assertEquals(star, red.getProgress());
    }

    @Test @DisplayName("FR-20: a capture does not give another turn")
    void captureGivesNoExtraTurn() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        redBehindGreen(f, 3);
        f.roll(3);
        f.game.move(0);
        assertSame(f.player(1), f.game.getCurrentPlayer());
    }

    @Test @DisplayName("Team mode: a teammate's piece is not captured")
    void teammateIsNotCaptured() {
        Fixture f = new Fixture(Difficulty.EASY, GameConfig.GameMode.TEAMS_2V2,
                PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE);
        Piece red = f.piece(0, 0), yellow = f.piece(2, 0);
        assertEquals(f.game.getTeamOf(PlayerColor.RED), f.game.getTeamOf(PlayerColor.YELLOW));
        for (int p = f.regularRun(red, 2, 3); ; p = f.regularRun(red, p + 1, 3)) {
            int q = f.sameSquare(yellow, red, p + 3);
            if (q > 0) { f.put(0, 0, p); f.put(2, 0, q); break; }
        }
        f.roll(3);
        f.game.move(0);
        assertFalse(yellow.isInCastle());
    }

    @Test @DisplayName("FR-17: two pieces of one player form a Double Block; an opponent stops on the square before it")
    void opponentStopsBeforeADoubleBlock() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        int[] pos = redBehindGreen(f, 3);
        f.put(1, 1, pos[1]);                      // second green piece: a Double Block 3 squares ahead of red
        f.roll(5);
        assertEquals(pos[0] + 3, f.game.getBlockAhead(f.piece(0, 0)));
        assertTrue(f.game.canMove(0));            // no other red piece can make the full move
        f.game.move(0);
        assertEquals(pos[0] + 2, f.piece(0, 0).getProgress());
        assertFalse(f.piece(1, 0).isInCastle());
    }

    @Test @DisplayName("FR-17: if another piece can make the full move, the blocked piece cannot be chosen")
    void blockedPieceCannotBeChosenWhenAnotherCanMoveFully() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        int[] pos = redBehindGreen(f, 3);
        f.put(1, 1, pos[1]);
        Piece other = f.piece(0, 1);
        f.put(0, 1, f.regularRun(other, pos[0] + 12, 6));
        f.roll(5);
        assertFalse(f.game.canMove(0));
        assertTrue(f.game.canMove(1));
        assertNotNull(f.game.explainCannotMove(0));
    }
}
