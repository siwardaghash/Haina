package model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Dice and turn rules: SRS FR-10, FR-11, FR-12, FR-14. */
class DiceRulesTest {

    @Test @DisplayName("FR-10: on Easy the steps are the step die")
    void easyStepsAreTheStepDie() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        f.put(0, 0, 5);
        f.roll(4);
        assertEquals(4, f.game.getSteps());
        assertTrue(f.game.canMove(0));
    }

    @Test @DisplayName("FR-10: on Medium the steps are step x (Boost - 1)")
    void mediumStepsAreMultiplied() {
        Fixture f = new Fixture(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN);
        f.put(0, 0, 5);
        f.roll(4, 3);
        assertEquals(8, f.game.getSteps());
    }

    @Test @DisplayName("FR-10: a Boost of 1 means 0 steps, the piece does not move and the turn passes")
    void boostOfOneMeansNoMove() {
        Fixture f = new Fixture(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN);
        f.put(0, 0, 5);
        f.roll(5, 1);
        assertEquals(0, f.game.getSteps());
        assertFalse(f.game.isAwaitingPiece());
        assertEquals(5, f.piece(0, 0).getProgress());
        assertSame(f.player(1), f.game.getCurrentPlayer());
    }

    @Test @DisplayName("FR-11: only a 6 brings a piece out of the castle, onto the start square")
    void onlySixLeavesTheCastle() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        f.roll(6);
        assertTrue(f.game.canMove(0));
        f.game.move(0);
        assertEquals(0, f.piece(0, 0).getProgress());
        assertEquals(Board.SquareType.START, f.typeAt(f.piece(0, 0), 0));
    }

    @Test @DisplayName("FR-12: with every piece in the castle, a roll of 1-5 passes the turn")
    void noPieceOutAndNoSixPassesTheTurn() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        for (int face = 1; face <= 5; face++) {
            Game.Player before = f.game.getCurrentPlayer();
            f.roll(face);
            assertNotSame(before, f.game.getCurrentPlayer(), "roll " + face);
        }
    }

    @Test @DisplayName("FR-14: a 6 gives another roll")
    void sixGivesAnotherRoll() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        f.put(0, 0, 10);
        f.roll(6);
        f.game.move(0);
        assertSame(f.player(0), f.game.getCurrentPlayer());
    }

    @Test @DisplayName("FR-14: a 6 gives another roll even with no legal move")
    void sixGivesAnotherRollWithoutLegalMove() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        for (int n = 0; n < 4; n++) f.put(0, n, f.piece(0, n).getFinishProgress());   // nothing can move...
        f.put(0, 3, f.piece(0, 3).getCommonLength() + 4);                         // ...a 6 overshoots Finish
        f.roll(6);
        assertFalse(f.game.isAwaitingPiece());
        assertSame(f.player(0), f.game.getCurrentPlayer());
    }

    @Test @DisplayName("FR-14: a third 6 in a row cancels the roll and passes the turn")
    void thirdSixCancelsTheTurn() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        f.put(0, 0, 1);
        f.roll(6); f.game.move(0);
        f.roll(6); f.game.move(0);
        int before = f.piece(0, 0).getProgress();
        f.roll(6);
        assertSame(f.player(1), f.game.getCurrentPlayer());
        assertEquals(before, f.piece(0, 0).getProgress());
        assertTrue(f.game.getMessage().contains("third 6"));
    }

    @Test @DisplayName("FR-14 + 1-5: any other roll passes the turn after the move")
    void otherRollPassesTheTurn() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        int p = f.regularRun(f.piece(0, 0), 2, 3);
        f.put(0, 0, p);
        f.roll(3);
        f.game.move(0);
        assertSame(f.player(1), f.game.getCurrentPlayer());
    }
}
