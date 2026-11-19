package model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Bonus: the computer player (SRS 2.6). */
class BotTest {

    @Test @DisplayName("The bot captures when it can")
    void botPrefersACapture() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        Piece red = f.piece(0, 0), green = f.piece(1, 0);
        int p = -1, q = -1;
        for (p = f.regularRun(red, 2, 3); ; p = f.regularRun(red, p + 1, 3)) { q = f.sameSquare(green, red, p + 3); if (q > 0) break; }
        f.put(0, 0, p);
        f.put(1, 0, q);
        f.put(0, 1, f.regularRun(f.piece(0, 1), p + 15, 3));
        f.roll(3);
        assertEquals(0, new Bot(new Random(1)).choosePiece(f.game));
    }

    @Test @DisplayName("The bot brings a piece into Finish Home when it can")
    void botPrefersFinishing() {
        Fixture f = new Fixture(Difficulty.EASY, PlayerColor.RED, PlayerColor.GREEN);
        f.put(0, 0, 10);
        f.put(0, 2, f.piece(0, 2).getFinishProgress() - 4);
        f.roll(4);
        assertEquals(2, new Bot(new Random(1)).choosePiece(f.game));
    }

    @Test @DisplayName("The bot answers: right or wrong, always one of the four answers")
    void botAnswersWithinRange() {
        Question q = new Question("e1", "easy", "?", new String[]{"a", "b", "c", "d"}, 2);
        Random alwaysRight = new Random() { private static final long serialVersionUID = 1L; @Override public double nextDouble() { return 0.0; } };
        Random alwaysWrong = new Random(5) { private static final long serialVersionUID = 1L; @Override public double nextDouble() { return 0.99; } };
        assertEquals(2, new Bot(alwaysRight).chooseAnswer(q));
        for (int i = 0; i < 50; i++) {
            int a = new Bot(alwaysWrong).chooseAnswer(q);
            assertTrue(a >= 0 && a <= 3 && a != 2);
        }
    }

    @Test @DisplayName("Bots never choose an illegal move; full bot games finish (Easy, Medium, Hard)")
    void botGamesFinishWithLegalMoves() {
        for (Difficulty d : Difficulty.values())
            for (int seed = 0; seed < 10; seed++) {
                List<GameConfig.PlayerSetup> ps = Arrays.asList(new GameConfig.PlayerSetup("A", PlayerColor.RED, true),
                        new GameConfig.PlayerSetup("B", PlayerColor.GREEN, true), new GameConfig.PlayerSetup("C", PlayerColor.BLUE, true));
                Game g = new Game(new GameConfig(d, ps), new Board(d, new Random(seed)), new Random(seed));
                g.setQuestionBank(Fixture.questionBank());
                Bot bot = new Bot(new Random(seed));
                for (int t = 0; t < 200000 && !g.isGameOver(); t++) {
                    if (g.getPendingQuestion() != null) g.answerQuestion(bot.chooseAnswer(g.getPendingQuestion()));
                    else if (g.isAwaitingSurpriseTarget()) { int[] tg = bot.chooseSurpriseTarget(g); assertNotNull(tg); g.selectSurpriseTarget(tg[0], tg[1]); }
                    else if (g.isAwaitingPiece()) { int i = bot.choosePiece(g); assertTrue(g.canMove(i)); g.move(i); }
                    else g.roll();
                }
                assertTrue(g.isGameOver(), d + " seed " + seed);
            }
    }
}
