package model;

import static org.junit.jupiter.api.Assertions.*;

import controller.HistoryRecorder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Observer pattern: Game is the subject; the board screen and the HistoryRecorder observe it. */
class ObserverTest {

    /** A test observer that counts how many times it was told. */
    private static final class Counter implements GameObserver {
        int calls;
        @Override public void gameChanged(Game game) { calls++; }
    }

    @Test @DisplayName("Observer: every registered observer is told after a roll and after a move")
    void allObserversAreToldAfterEachAction() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        Counter a = new Counter(), b = new Counter();
        f.game.addObserver(a);
        f.game.addObserver(b);
        f.roll(6);
        assertEquals(1, a.calls);
        assertEquals(1, b.calls);
        f.game.move(0);
        assertEquals(2, a.calls);
        assertEquals(2, b.calls);
    }

    @Test @DisplayName("Observer: a removed observer is not told any more; adding twice counts once")
    void removedObserverIsNotTold() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        Counter a = new Counter();
        f.game.addObserver(a);
        f.game.addObserver(a);
        f.roll(3);
        assertEquals(1, a.calls);
        f.game.removeObserver(a);
        f.roll(4);
        assertEquals(1, a.calls);
    }

    @Test @DisplayName("Observer: the observer is told after a question is answered")
    void observerIsToldAfterAnswer() {
        Fixture f = new Fixture(Difficulty.MEDIUM, PlayerColor.RED, PlayerColor.GREEN);
        f.game.setQuestionBank(Fixture.questionBank());
        Piece piece = f.piece(0, 0);
        int q = f.find(piece, Board.SquareType.QUESTION_EASY, 1);
        if (q < 0) q = f.find(piece, Board.SquareType.QUESTION_MEDIUM, 1);
        if (q < 0) q = f.find(piece, Board.SquareType.QUESTION_HARD, 1);
        assertTrue(q > 1, "the medium board has a question square");
        f.put(0, 0, q - 1);
        f.roll(1, 2);                 // 1 x (2 - 1) = 1 step onto the question square
        f.game.move(0);
        assertNotNull(f.game.getPendingQuestion());
        Counter a = new Counter();
        f.game.addObserver(a);
        f.game.answerQuestion(0);
        assertEquals(1, a.calls);
    }

    /** Three pieces of RED are home, the fourth needs exactly 2 more steps. */
    private static void almostDone(Fixture f) {
        for (int n = 0; n < 3; n++) f.put(0, n, f.piece(0, n).getFinishProgress());
        f.put(0, 3, f.piece(0, 3).getFinishProgress() - 2);
    }

    @Test @DisplayName("FR-32 + Observer: the HistoryRecorder saves the game once, only when it ends")
    void recorderSavesOnceWhenTheGameEnds() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        List<Game> saved = new ArrayList<>();
        HistoryRecorder recorder = new HistoryRecorder(saved::add);
        f.game.addObserver(recorder);
        almostDone(f);
        f.roll(2);                    // the game has changed, but it is not over yet
        assertTrue(saved.isEmpty());
        assertFalse(recorder.isSaved());
        f.game.move(3);               // the last piece reaches FINISH: the game ends
        assertTrue(f.game.isGameOver());
        assertEquals(List.of(f.game), saved);
        recorder.gameChanged(f.game); // told again: still saved only once
        assertEquals(1, saved.size());
        assertNull(recorder.getError());
    }

    @Test @DisplayName("3.6.1 + Observer: a failed save is reported, and the game is still over")
    void recorderReportsAFailedSave() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        HistoryRecorder recorder = new HistoryRecorder(g -> { throw new IOException("disk full"); });
        f.game.addObserver(recorder);
        almostDone(f);
        f.roll(2);
        f.game.move(3);
        assertTrue(recorder.isSaved());
        assertEquals("disk full", recorder.getError().getMessage());
        assertTrue(f.game.isGameOver());
    }
}
