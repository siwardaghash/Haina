package controller;

import java.util.Random;
import model.Board;
import model.Bot;
import model.Game;
import model.GameConfig;
import model.QuestionBank;
import model.SysData;

/**
 * Controller of one game (MVC): the board screen sends every player action here,
 * and this class applies it to the model. The screen only reads the model to draw it.
 */
public final class GameController {
    private final GameConfig config;
    private final Board board;
    private final Game game;
    private final String bankError;
    private final Bot bot = new Bot(new Random());
    /** Observer of the game: saves the result to the history as soon as the game ends. */
    private final HistoryRecorder recorder = new HistoryRecorder(finished -> {
        SysData.get().history().append(finished);
        SysData.get().trophies().append(finished);   // bonus: the trophies earned in it
    });

    /** Builds the board for the chosen level and starts a game (FR-6, FR-7). */
    public GameController(GameConfig config) { this(config, new Board(config.getDifficulty())); }

    /** Starts a game on a given board and loads the question bank for it (FR-39: changes apply from the next game). */
    public GameController(GameConfig config, Board board) {
        this.config = config;
        this.board = board;
        game = new Game(config, board);
        String error = null;
        try {
            game.setQuestionBank(new QuestionBank(SysData.get(), new Random()));
        } catch (Exception ex) {
            error = ex.getMessage();   // 3.6.1: the game still runs, the screen reports the problem
        }
        bankError = error;
        game.addObserver(recorder);
    }

    public GameConfig getConfig() { return config; }
    public Board getBoard() { return board; }

    /** The model, for reading its state (board, pieces, dice, current player, messages). */
    public Game getGame() { return game; }

    /** Why the question bank could not be loaded, or null when it loaded. */
    public String getBankError() { return bankError; }

    /** FR-10: the current player rolls the dice of the level. */
    public void roll() { game.roll(); }

    /** FR-13: moves the chosen piece (0-3) of the current player. */
    public void move(int pieceIndex) { game.move(pieceIndex); }

    /** FR-25: answers the pending question; returns whether the answer was correct. */
    public boolean answer(int answerIndex) { return game.answerQuestion(answerIndex); }

    /** FR-28: the opponent piece chosen for the "move an opponent back" surprise; returns the die. */
    public int chooseSurpriseTarget(int playerIndex, int pieceIndex) {
        return game.selectSurpriseTarget(playerIndex, pieceIndex);
    }

    /** Bonus: true when the player whose turn it is is a computer player. */
    public boolean isBotTurn() { return !game.isGameOver() && game.getCurrentPlayer().isBot(); }

    /** Bonus: the piece the bot moves after its roll (-1 when none can move). */
    public int botChoosePiece() { return bot.choosePiece(game); }

    /** Bonus: the answer the bot gives to the pending question. */
    public int botChooseAnswer() { return bot.chooseAnswer(game.getPendingQuestion()); }

    /** Bonus: the opponent piece the bot sends back, as {player, piece}, or null. */
    public int[] botChooseSurpriseTarget() { return bot.chooseSurpriseTarget(game); }

    /**
     * FR-32: the finished game was already saved by the {@link HistoryRecorder} observer.
     * Returns null when it was saved, otherwise why it failed (or that the game is not over).
     */
    public String getSaveError() {
        if (!recorder.isSaved()) return "the game is not over yet";
        return recorder.getError() == null ? null : recorder.getError().getMessage();
    }
}
