package model;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Regression tests for the course-compatible classic and 2v2 CSV history. */
public final class TeamHistoryRegressionTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Game finished(boolean teams, boolean teamA) throws Exception {
        List<GameConfig.PlayerSetup> players = Arrays.asList(
                new GameConfig.PlayerSetup("Red", PlayerColor.RED),
                new GameConfig.PlayerSetup("Green", PlayerColor.GREEN),
                new GameConfig.PlayerSetup("Yellow", PlayerColor.YELLOW),
                new GameConfig.PlayerSetup("Blue", PlayerColor.BLUE));
        GameConfig config = new GameConfig(Difficulty.EASY, players,
                teams ? GameConfig.GameMode.TEAMS_2V2 : GameConfig.GameMode.CLASSIC);
        Game game = new Game(config, new Board(Difficulty.EASY), new Random(42));
        for (Game.Player player : game.getPlayers()) {
            boolean finish = teams ? GameConfig.teamOf(player.getColor()) == (teamA ? 0 : 1)
                    : player.getColor() != PlayerColor.BLUE;
            if (finish) for (Piece piece : player.getPieces()) piece.setProgress(piece.getFinishProgress());
        }
        Method update = Game.class.getDeclaredMethod("updateFinishingOrder");
        update.setAccessible(true);
        update.invoke(game);
        check(game.isGameOver(), "Test game must finish");
        return game;
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("haina-team-history-");
        try {
            Path csv = directory.resolve("history.csv");
            GameHistory history = new GameHistory(csv);
            history.append(finished(true, true));
            history.append(finished(true, false));
            history.append(finished(false, false));
            List<GameRecord> records = history.readRecords();
            check(records.size() == 3, "Expected three saved games");
            check(Files.readAllLines(csv).get(0).equals(GameHistory.HEADER), "CSV header unchanged");
            check(records.get(0).isTeamMode() && records.get(0).getWinningTeam() == 0, "Team A winner persists");
            check(records.get(1).isTeamMode() && records.get(1).getWinningTeam() == 1, "Team B winner persists");
            check(!records.get(2).isTeamMode(), "Classic match remains classic");
            for (GameRecord record : records) check(record.toCsvFields().size() == 8, "Eight CSV columns required");
            for (int i = 0; i < 2; i++) {
                GameRecord record = records.get(i);
                int winnerTeam = record.getWinningTeam();
                for (int place = 0; place < 4; place++) {
                    String name = record.getPlaces()[place];
                    int colorTeam = name.equals("Red") || name.equals("Yellow") ? 0 : 1;
                    check(colorTeam == (place < 2 ? winnerTeam : 1 - winnerTeam), "Partners grouped correctly");
                }
            }
            check(GameStatistics.from(records).easyGames() == 3, "Difficulty totals include team mode");
            List<PlayerStatistics> players = PlayerStatistics.from(records);
            check(players.stream().filter(p -> p.name().equals("Red")).findFirst().orElseThrow().wins() >= 1,
                    "Winning Team A player receives a win");
            check(players.stream().filter(p -> p.name().equals("Blue")).findFirst().orElseThrow().wins() == 1,
                    "Losing partner does not gain team win");
            System.out.println("PASS: Team history, classic history, CSV and statistics regression");
        } finally {
            Files.deleteIfExists(directory.resolve("history.csv"));
            Files.deleteIfExists(directory);
        }
    }
}
