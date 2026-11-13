package model;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Standalone regression checks: no external test framework required. */
public final class TeamModeRegressionTest {
    private static int assertions;

    private static void check(boolean condition, String explanation) {
        assertions++;
        if (!condition) throw new AssertionError(explanation);
    }

    private static List<GameConfig.PlayerSetup> players() {
        return Arrays.asList(
                new GameConfig.PlayerSetup("Red", PlayerColor.RED),
                new GameConfig.PlayerSetup("Green", PlayerColor.GREEN),
                new GameConfig.PlayerSetup("Yellow", PlayerColor.YELLOW),
                new GameConfig.PlayerSetup("Blue", PlayerColor.BLUE));
    }

    private static Game create(boolean teams) {
        GameConfig config = teams
                ? new GameConfig(Difficulty.EASY, players(), GameConfig.GameMode.TEAMS_2V2)
                : new GameConfig(Difficulty.EASY, players());
        return new Game(config, new Board(Difficulty.EASY), new Random(42));
    }

    private static Game.Player player(Game game, PlayerColor color) {
        for (Game.Player player : game.getPlayers()) {
            if (player.getColor() == color) return player;
        }
        throw new AssertionError("Missing player " + color);
    }

    private static Object invoke(Game game, String method, Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method m = Game.class.getDeclaredMethod(method, parameters);
        m.setAccessible(true);
        return m.invoke(game, arguments);
    }

    private static void finishPlayer(Game game, PlayerColor color) {
        for (Piece piece : player(game, color).getPieces()) {
            piece.setProgress(piece.getFinishProgress());
        }
    }

    private static void update(Game game) throws Exception {
        invoke(game, "updateFinishingOrder", new Class<?>[0]);
    }

    private static void configuration() {
        check(!new GameConfig(Difficulty.EASY, players()).isTeamMode(), "Classic is default");
        check(GameConfig.teamOf(PlayerColor.RED) == GameConfig.teamOf(PlayerColor.BLUE), "Red + Blue team");
        check(GameConfig.teamOf(PlayerColor.GREEN) == GameConfig.teamOf(PlayerColor.YELLOW), "Green + Yellow team");
        check(GameConfig.teamOf(PlayerColor.RED) != GameConfig.teamOf(PlayerColor.GREEN), "Distinct teams");
        try {
            new GameConfig(Difficulty.EASY, players().subList(0, 2), GameConfig.GameMode.TEAMS_2V2);
            throw new AssertionError("Teams accepted fewer than four players");
        } catch (IllegalArgumentException expected) {
            check(true, "Reject incomplete team mode");
        }
    }

    private static void teamVictories() throws Exception {
        Game a = create(true);
        finishPlayer(a, PlayerColor.RED);
        update(a);
        check(!a.isGameOver(), "One teammate finishing must not end the game");
        finishPlayer(a, PlayerColor.BLUE);
        update(a);
        check(a.isGameOver() && a.getWinningTeam() == 0, "Red + Blue win as a team");
        check(a.getFinishedAtMillis() > 0, "Team A finish time recorded");

        Game b = create(true);
        finishPlayer(b, PlayerColor.GREEN);
        finishPlayer(b, PlayerColor.YELLOW);
        update(b);
        check(b.isGameOver() && b.getWinningTeam() == 1, "Green + Yellow win as a team");
    }

    private static void classicStillWorks() throws Exception {
        Game classic = create(false);
        finishPlayer(classic, PlayerColor.RED);
        finishPlayer(classic, PlayerColor.GREEN);
        update(classic);
        check(!classic.isGameOver(), "Two of four finishers don't end Classic");
        finishPlayer(classic, PlayerColor.BLUE);
        update(classic);
        check(classic.isGameOver(), "Classic ends when three of four finish");
        check(classic.getFinishingOrder().size() == 4, "Classic ranks all four players");
        check(classic.getWinningTeam() == -1, "Classic has no winning team");
    }

    /** Find an unprotected shared square using each piece's actual track coordinates. */
    private static int[] sharedSquare(Game game, Piece first, Piece second) {
        for (int a = 0; a < first.getCommonLength(); a++) {
            for (int b = 0; b < second.getCommonLength(); b++) {
                if (first.ringAt(a) == second.ringAt(b)
                        && first.indexAt(a) == second.indexAt(b)
                        && !game.isSafeSquare(first.ringAt(a), first.indexAt(a))) {
                    return new int[]{a, b};
                }
            }
        }
        throw new AssertionError("No shared unprotected square found");
    }

    private static void friendlyCapture() throws Exception {
        Game teams = create(true);
        Piece red = player(teams, PlayerColor.RED).getPieces().get(0);
        Piece blue = player(teams, PlayerColor.BLUE).getPieces().get(0);
        int[] positions = sharedSquare(teams, red, blue);
        red.setProgress(positions[0]);
        blue.setProgress(positions[1]);
        int caught = (Integer) invoke(teams, "captureOnLanding", new Class<?>[]{Piece.class}, red);
        check(caught == 0 && blue.getProgress() == positions[1], "Teammate is never captured");

        Game classic = create(false);
        Piece classicRed = player(classic, PlayerColor.RED).getPieces().get(0);
        Piece classicBlue = player(classic, PlayerColor.BLUE).getPieces().get(0);
        int[] classicPositions = sharedSquare(classic, classicRed, classicBlue);
        classicRed.setProgress(classicPositions[0]);
        classicBlue.setProgress(classicPositions[1]);
        int captures = (Integer) invoke(classic, "captureOnLanding", new Class<?>[]{Piece.class}, classicRed);
        check(captures == 1 && classicBlue.isInCastle(), "Classic still captures other colors");
    }

    private static void friendlyBlock() throws Exception {
        Game teams = create(true);
        Piece blue1 = player(teams, PlayerColor.BLUE).getPieces().get(0);
        Piece blue2 = player(teams, PlayerColor.BLUE).getPieces().get(1);
        Piece red = player(teams, PlayerColor.RED).getPieces().get(0);
        int[] loc = sharedSquare(teams, red, blue1);
        blue1.setProgress(loc[1]);
        blue2.setProgress(loc[1]);
        int ring = blue1.getRing(), index = blue1.getBoardIndex();
        Class<?>[] sig = {PlayerColor.class, int.class, int.class};
        check(!(Boolean) invoke(teams, "hasOpposingBlock", sig, PlayerColor.RED, ring, index),
                "Blue double block must not block Red teammate");
        check((Boolean) invoke(teams, "hasOpposingBlock", sig, PlayerColor.GREEN, ring, index),
                "Blue double block must block Green opponent");
    }

    public static void main(String[] args) throws Exception {
        configuration();
        teamVictories();
        classicStillWorks();
        friendlyCapture();
        friendlyBlock();
        System.out.println("PASS: " + assertions + " team/classic regression assertions");
    }
}
