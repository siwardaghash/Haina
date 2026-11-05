package model;

/**
 * Observer of a {@link Game} (MVC): the model calls {@link #gameChanged(Game)} after every
 * player action (roll, move, answer, surprise target), so a view can show the new state
 * without the model knowing anything about the view.
 */
public interface GameObserver {
    /** Called on the thread that changed the game, right after the change. */
    void gameChanged(Game game);
}
