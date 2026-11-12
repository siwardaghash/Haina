package model;

import java.util.List;

public record GameStatistics(int totalGames, int easyGames,
                             int mediumGames, int hardGames) {

    public static GameStatistics from(List<GameRecord> games) {
        int easy = 0;
        int medium = 0;
        int hard = 0;

        for (GameRecord game : games) {
            String difficulty = game.getDifficulty();

            if ("easy".equalsIgnoreCase(difficulty)) {
                easy++;
            } else if ("medium".equalsIgnoreCase(difficulty)) {
                medium++;
            } else if ("hard".equalsIgnoreCase(difficulty)) {
                hard++;
            }
        }

        return new GameStatistics(games.size(), easy, medium, hard);
    }
}