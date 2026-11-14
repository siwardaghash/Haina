package model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** One completed game in the history file, with exactly the fields of the course schema (SRS 3.7.1). */
public final class GameRecord {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy; HH:mm:ss");

    private final long gameId;
    private final String difficulty;
    private final String timestamp;     // DD/MM/YYYY; HH:MM:SS
    private final String[] places;      // 1st..4th place names, "" when fewer than 4 players
    private final String duration;      // HH:MM:SS

    public GameRecord(long gameId, String difficulty, String timestamp, String[] places, String duration) {
        if (places.length != 4) throw new IllegalArgumentException("Four place fields are required");
        this.gameId = gameId;
        this.difficulty = difficulty;
        this.timestamp = timestamp;
        this.places = places.clone();
        this.duration = duration;
    }

    /** Creates the record of a finished game (FR-32: unfinished games are not saved). */
    public static GameRecord of(Game game, long gameId) {
        if (!game.isGameOver()) throw new IllegalStateException("Only completed games may be saved");
        String[] places = {"", "", "", ""};
        if (game.isTeamMode()) {
            int winner = game.getWinningTeam();
            if (winner < 0 || winner > 1) throw new IllegalStateException("A finished team game must have a winning team");
            // Winning partners occupy the first two fields; losing partners the last two.
            // Team mode is encoded in the difficulty field to retain the required 8-column CSV schema.
            int nextWinner = 0, nextLoser = 2;
            for (Game.Player player : game.getPlayers()) {
                if (game.getTeamOf(player.getColor()) == winner) places[nextWinner++] = player.getName();
                else places[nextLoser++] = player.getName();
            }
        } else {
            List<Game.Player> order = game.getFinishingOrder();
            for (int i = 0; i < order.size() && i < 4; i++) places[i] = order.get(i).getName();
        }
        long sec = game.getDurationSeconds();
        return new GameRecord(gameId,
                game.getDifficulty().name().toLowerCase(Locale.ROOT)
                        + (game.isTeamMode() ? " [2v2:" + (game.getWinningTeam() == 0 ? "A" : "B") + "]" : ""),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(game.getFinishedAtMillis()), ZoneId.systemDefault()).format(TIME),
                places,
                String.format(Locale.ROOT, "%02d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60));
    }

    /** Reads one CSV row (8 fields, in header order); returns null for a malformed row. */
    public static GameRecord fromCsvFields(List<String> fields) {
        if (fields.size() != 8) return null;
        try {
            return new GameRecord(Long.parseLong(fields.get(0).trim()), fields.get(1), fields.get(2),
                    fields.subList(3, 7).toArray(new String[0]), fields.get(7));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** The 8 fields in the order of the course header. */
    public List<String> toCsvFields() {
        List<String> fields = new ArrayList<>();
        fields.add(String.valueOf(gameId));
        fields.add(difficulty);
        fields.add(timestamp);
        fields.addAll(Arrays.asList(places));
        fields.add(duration);
        return fields;
    }

    /** Team records use the standard eight-column history layout with a mode suffix. */
    public boolean isTeamMode() { return difficulty.matches("(?i).* \\[2v2:[AB]\\]"); }
    public int getWinningTeam() {
        if (!isTeamMode()) return -1;
        return Character.toUpperCase(difficulty.charAt(difficulty.length() - 2)) == 'A' ? 0 : 1;
    }
    public String getBaseDifficulty() {
        return isTeamMode() ? difficulty.substring(0, difficulty.indexOf(" [2v2:")) : difficulty;
    }
    public long getGameId() { return gameId; }
    public String getDifficulty() { return difficulty; }
    public String getTimestamp() { return timestamp; }
    public String[] getPlaces() { return places.clone(); }
    public String getDuration() { return duration; }
}
