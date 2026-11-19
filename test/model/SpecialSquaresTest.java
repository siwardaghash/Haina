package model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Question and surprise squares: SRS FR-22, FR-23, FR-25, FR-27, FR-28. */
class SpecialSquaresTest {
    private Fixture f;
    private Piece red;

    @BeforeEach
    void setUp() {
        f = new Fixture(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN);
        f.game.setQuestionBank(Fixture.questionBank());
        red = f.piece(0, 0);
    }

    /** Puts red two squares before the first square of that type (from position 3) and returns that square's position. */
    private int twoBefore(Board.SquareType type) {
        int target = f.find(red, type, 3);
        assertTrue(target > 2, "the board has a " + type + " square on red's route");
        f.put(0, 0, target - 2);
        return target;
    }

    private Board.SquareType anyQuestionType() {
        for (Board.SquareType t : new Board.SquareType[]{Board.SquareType.QUESTION_EASY, Board.SquareType.QUESTION_MEDIUM, Board.SquareType.QUESTION_HARD})
            if (f.find(red, t, 3) > 2) return t;
        throw new AssertionError("no question square on red's route");
    }

    private static String levelOf(Board.SquareType t) {
        return t == Board.SquareType.QUESTION_EASY ? "easy" : t == Board.SquareType.QUESTION_MEDIUM ? "medium" : "hard";
    }

    private static int distanceOf(String level) { return "easy".equals(level) ? 1 : "medium".equals(level) ? 2 : 3; }

    @Test @DisplayName("FR-23: landing on a question square asks a question of the square's level")
    void questionSquareAsksAQuestionOfItsLevel() {
        Board.SquareType type = anyQuestionType();
        twoBefore(type);
        f.roll(2, 2);                               // 2 x (2 - 1) = 2 steps
        f.game.move(0);
        assertNotNull(f.game.getPendingQuestion());
        assertEquals(levelOf(type), f.game.getPendingQuestion().getLevel());
    }

    @Test @DisplayName("FR-25: a correct answer moves forward by the level (1/2/3)")
    void correctAnswerMovesForward() {
        Board.SquareType type = anyQuestionType();
        int square = twoBefore(type);
        f.roll(2, 2);
        f.game.move(0);
        Question q = f.game.getPendingQuestion();
        assertTrue(f.game.answerQuestion(q.getCorrect()));
        assertEquals(square + distanceOf(q.getLevel()), red.getProgress());
    }

    @Test @DisplayName("FR-25: a wrong answer moves back by the level")
    void wrongAnswerMovesBack() {
        Board.SquareType type = anyQuestionType();
        int square = twoBefore(type);
        f.roll(2, 2);
        f.game.move(0);
        Question q = f.game.getPendingQuestion();
        assertFalse(f.game.answerQuestion((q.getCorrect() + 1) % 4));
        assertEquals(Math.max(0, square - distanceOf(q.getLevel())), red.getProgress());
    }

    @Test @DisplayName("FR-22: a piece that did not move (0 steps) does not trigger its square again")
    void zeroStepsDoNotRetriggerTheSquare() {
        Board.SquareType type = anyQuestionType();
        int square = f.find(red, type, 3);
        f.put(0, 0, square);
        f.roll(3, 1);                               // 3 x (1 - 1) = 0 steps
        assertNull(f.game.getPendingQuestion());
        assertEquals(square, red.getProgress());
    }

    @Test @DisplayName("FR-27: a level's questions do not repeat until all of them were shown")
    void questionsDoNotRepeatBeforeAllWereShown() {
        QuestionBank bank = Fixture.questionBank();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10; i++) assertTrue(seen.add(bank.next("easy").getId()));
        assertEquals(10, seen.size());
    }

    // FR-28: the surprise event is drawn with nextInt(100): 0-9 finish, 10-19 opponent back,
    // 20-29 castle, 30-59 back 3, 60-89 lose next turn, 90-99 bonus roll.

    @Test @DisplayName("FR-28: surprise 'straight to Finish Home'")
    void surpriseDirectFinish() {
        twoBefore(Board.SquareType.SURPRISE);
        f.dice.push(1, 1, 5);                       // step 2, boost 2, event 5
        f.game.roll();
        f.game.move(0);
        assertEquals(Game.SurpriseEvent.DIRECT_FINISH, f.game.getLastSurprise());
        assertTrue(red.isFinished());
    }

    @Test @DisplayName("FR-28: surprise 'back to the castle'")
    void surpriseReturnToCastle() {
        twoBefore(Board.SquareType.SURPRISE);
        f.dice.push(1, 1, 25);
        f.game.roll();
        f.game.move(0);
        assertEquals(Game.SurpriseEvent.RETURN_CASTLE, f.game.getLastSurprise());
        assertTrue(red.isInCastle());
    }

    @Test @DisplayName("FR-28: surprise 'move back 3 squares'")
    void surpriseBackThree() {
        int square = twoBefore(Board.SquareType.SURPRISE);
        f.dice.push(1, 1, 45);
        f.game.roll();
        f.game.move(0);
        assertEquals(Game.SurpriseEvent.BACK_THREE, f.game.getLastSurprise());
        assertEquals(square - 3, red.getProgress());
    }

    @Test @DisplayName("FR-28: surprise 'lose your next turn'")
    void surpriseSkipNextTurn() {
        twoBefore(Board.SquareType.SURPRISE);
        f.dice.push(1, 1, 75);
        f.game.roll();
        f.game.move(0);
        assertEquals(Game.SurpriseEvent.SKIP_NEXT_TURN, f.game.getLastSurprise());
        assertSame(f.player(1), f.game.getCurrentPlayer());
        f.roll(1, 1);                               // green has nothing out: turn passes, red is skipped
        assertSame(f.player(1), f.game.getCurrentPlayer());
    }

    @Test @DisplayName("FR-28: surprise 'roll again with the UltraBoost die'")
    void surpriseBonusRoll() {
        twoBefore(Board.SquareType.SURPRISE);
        f.dice.push(1, 1, 95);
        f.game.roll();
        f.game.move(0);
        assertEquals(Game.SurpriseEvent.BONUS_ULTRABOOST, f.game.getLastSurprise());
        assertTrue(f.game.isBonusRollReady());
        assertSame(f.player(0), f.game.getCurrentPlayer());
    }

    @Test @DisplayName("FR-28: surprise 'move an opponent back' waits for the target, then moves it back half the die")
    void surpriseOpponentBack() {
        twoBefore(Board.SquareType.SURPRISE);
        Piece green = f.piece(1, 0);
        f.put(1, 0, f.regularRun(green, 10, 4) + 4);
        int before = green.getProgress();
        f.dice.push(1, 1, 15);
        f.game.roll();
        f.game.move(0);
        assertTrue(f.game.isAwaitingSurpriseTarget());
        assertTrue(f.game.canSelectSurpriseTarget(1, 0));
        f.dice.push(3);                             // the die shows 4: back (4 + 1) / 2 = 2 steps
        int die = f.game.selectSurpriseTarget(1, 0);
        assertEquals(4, die);
        assertEquals(before - 2, green.getProgress());
    }
}
