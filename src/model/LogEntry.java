package model;

/**
 * Bonus: one line of the move log. The game adds an entry after every roll, move, capture,
 * answer and surprise, so the board can list what happened (also during bot turns).
 */
public final class LogEntry {

    /** What kind of event the entry is; the view picks the colour of the row from it. */
    public enum Kind { ROLL, MOVE, CAPTURE, FINISH, CORRECT, WRONG, SURPRISE, TURN_LOST, WIN, ACHIEVEMENT }

    private final PlayerColor color;
    private final String player;
    private final Kind kind;
    private final String text;
    private final long secondsIntoGame;
    /** The other player involved, e.g. the one whose piece was captured; null when there is none. */
    private final PlayerColor other;

    public LogEntry(PlayerColor color, String player, Kind kind, String text, long secondsIntoGame) {
        this(color, player, kind, text, secondsIntoGame, null);
    }

    public LogEntry(PlayerColor color, String player, Kind kind, String text, long secondsIntoGame, PlayerColor other) {
        this.color = color;
        this.player = player;
        this.kind = kind;
        this.text = text;
        this.secondsIntoGame = secondsIntoGame;
        this.other = other;
    }

    public PlayerColor getColor() { return color; }
    public String getPlayer() { return player; }
    public Kind getKind() { return kind; }
    public String getText() { return text; }
    public long getSecondsIntoGame() { return secondsIntoGame; }
    public PlayerColor getOther() { return other; }

    @Override
    public String toString() { return player + ": " + text; }
}