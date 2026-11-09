package controller;

import java.io.IOException;
import java.util.Random;
import model.Board;
import model.Game;
import model.GameConfig;
import model.GameRecord;
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

    /** FR-32: saves the finished game to the history. */
    public GameRecord saveResult() throws IOException { return SysData.get().history().append(game); }
}
