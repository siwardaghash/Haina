package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Local CSV history using the exact header required by the course; one GameRecord per row. */
public final class GameHistory {
    public static final String HEADER = "gameID,difficulty,timestamp(DD/MM/YYYY; HH:MM:SS),1st_place_name,2nd_place_name,3rd_place_name,4rd_place_name,game_duration(HH:MM:SS)";
    private final Path file;

    public GameHistory(Path file) { this.file = file; }

    /** Saves a finished game with the next sequential gameID; the file and header are created on first use. */
    public synchronized GameRecord append(Game game) throws IOException {
        long maxId = 0;
        for (GameRecord record : readRecords()) maxId = Math.max(maxId, record.getGameId());
        GameRecord record = GameRecord.of(game, maxId + 1);
        StringBuilder line = new StringBuilder();
        List<String> fields = record.toCsvFields();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) line.append(',');
            line.append(escape(fields.get(i)));
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        String prefix = Files.exists(file) && Files.size(file) > 0 ? "" : HEADER + "\n";
        Files.write(file, (prefix + line + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return record;
    }

    /** All saved games; malformed rows are skipped, a wrong header is reported. */
    public List<GameRecord> readRecords() throws IOException {
        List<GameRecord> records = new ArrayList<>();
        if (!Files.exists(file)) return records;
        List<String> rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (!rows.isEmpty() && !HEADER.equals(rows.get(0)))
            throw new IOException("History CSV header does not match the course template: " + file);
        for (int i = 1; i < rows.size(); i++) {
            GameRecord record = GameRecord.fromCsvFields(parseCsv(rows.get(i)));
            if (record != null) records.add(record);
        }
        return records;
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    /** CSV fields may be quoted when a player name contains a comma. */
    static List<String> parseCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; }
                else if (c == '"') quoted = false;
                else current.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { fields.add(current.toString()); current.setLength(0); }
            else current.append(c);
        }
        fields.add(current.toString());
        return fields;
    }
}
