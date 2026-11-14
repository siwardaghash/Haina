package model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record PlayerStatistics(String name, int gamesPlayed, int wins) {

    public double winRate() {
        return gamesPlayed == 0 ? 0.0 : 100.0 * wins / gamesPlayed;
    }

    public static List<PlayerStatistics> from(List<GameRecord> games) {
        Map<String, PlayerStatistics> byName =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (GameRecord game : games) {
            String[] places = game.getPlaces();

            for (int place = 0; place < places.length; place++) {
                String name = places[place].strip();
                if (name.isEmpty()) continue;

                int win = game.isTeamMode() ? (place < 2 ? 1 : 0) : (place == 0 ? 1 : 0);
                PlayerStatistics previous = byName.get(name);

                if (previous == null) {
                    byName.put(name, new PlayerStatistics(name, 1, win));
                } else {
                    byName.put(name, new PlayerStatistics(previous.name(),
                            previous.gamesPlayed() + 1, previous.wins() + win));
                }
            }
        }

        return List.copyOf(byName.values());
    }
}