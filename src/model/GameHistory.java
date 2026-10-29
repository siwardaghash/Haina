package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Local CSV history using the exact header required by the course. */
public final class GameHistory {
    public static final String HEADER = "gameID,difficulty,timestamp(DD/MM/YYYY; HH:MM:SS),1st_place_name,2nd_place_name,3rd_place_name,4rd_place_name,game_duration(HH:MM:SS)";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy; HH:mm:ss");
    private final Path file;

    public GameHistory(Path file) { this.file = file; }

    public synchronized void append(Game game) throws IOException {
        if (!game.isGameOver()) throw new IllegalStateException("Only completed games may be saved");
        List<String> rows = readRows();
        long maxId = 0;
        for (int i = 1; i < rows.size(); i++) {
            String[] values = rows.get(i).split(",", 2);
            try { maxId = Math.max(maxId, Long.parseLong(values[0])); }
            catch (NumberFormatException ignored) { /* Ignore malformed external rows. */ }
        }
        List<String> fields = new ArrayList<>();
        fields.add(String.valueOf(maxId + 1));
        fields.add(game.getDifficulty().name().toLowerCase(java.util.Locale.ROOT));
        fields.add(LocalDateTime.ofInstant(Instant.ofEpochMilli(game.getFinishedAtMillis()), ZoneId.systemDefault()).format(TIME));
        for (Game.Player player : game.getFinishingOrder()) fields.add(player.getName());
        while (fields.size() < 7) fields.add("");
        long sec = game.getDurationSeconds();
        fields.add(String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60));
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) line.append(',');
            line.append(escape(fields.get(i)));
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        String prefix = Files.exists(file) && Files.size(file) > 0 ? "" : HEADER + "\n";
        Files.write(file, (prefix + line + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public List<String> readRows() throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        List<String> rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (!rows.isEmpty() && !HEADER.equals(rows.get(0)))
            throw new IOException("History CSV header does not match the course template: " + file);
        return rows;
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }
}
