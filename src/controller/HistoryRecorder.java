package controller;

import java.io.IOException;
import model.Game;
import model.GameObserver;

/**
 * Observer pattern (iteration 2): a second observer of {@link Game}, next to the board screen.
 * It watches the game and saves the result to the history (FR-32) the moment the game ends,
 * exactly once. The game does not know it exists, and no screen has to remember to save:
 * the result is saved even if the player leaves the board right after the last move.
 */
public final class HistoryRecorder implements GameObserver {

    /** Where a finished game is written; the game controller passes the real history files. */
    @FunctionalInterface
    public interface ResultStore {
        void save(Game finishedGame) throws IOException;
    }

    private final ResultStore store;
    private boolean saved;
    private IOException error;

    public HistoryRecorder(ResultStore store) { this.store = store; }

    /** Called by the game after every action; acts only on the first call after the game is over. */
    @Override
    public void gameChanged(Game game) {
        if (saved || !game.isGameOver()) return;
        saved = true;
        try {
            store.save(game);
        } catch (IOException ex) {
            error = ex;          // 3.6.1: the game goes on; the end screen reports the problem
        }
    }

    /** True once the finished game was handed to the store (even if the store failed). */
    public boolean isSaved() { return saved; }

    /** Why saving failed, or null when it worked (or the game is not over yet). */
    public IOException getError() { return error; }
}
