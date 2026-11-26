package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bonus: keeps every earned trophy in data/achievements.csv (date, player, trophy), so each
 * player's collection can be shown later. history.csv is not touched.
 */
public final class TrophyStore {
    public static final String HEADER = "date,player,achievement";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** One saved trophy. */
    public static final class Record {
        private final String date;
        private final String player;
        private final Achievement achievement;

        public Record(String date, String player, Achievement achievement) {
            this.date = date;
            this.player = player;
            this.achievement = achievement;
        }

        public String getDate() { return date; }
        public String getPlayer() { return player; }
        public Achievement getAchievement() { return achievement; }
    }

    /** A player's collection: how many times each trophy was earned and the date of the first time. */
    public static final class Collection {
        private final Map<Achievement, Integer> counts = new EnumMap<>(Achievement.class);
        private final Map<Achievement, String> firstDate = new EnumMap<>(Achievement.class);

        public int count(Achievement a) { return counts.getOrDefault(a, 0); }
        public String firstDate(Achievement a) { return firstDate.get(a); }
        public int distinct() { return counts.size(); }
        public int total() {
            int n = 0;
            for (int c : counts.values()) n += c;
            return n;
        }

        /** The rarest trophy in the collection, or null when it is empty. */
        public Achievement rarest() {
            Achievement best = null;
            for (Achievement a : counts.keySet())
                if (best == null || a.getTier().ordinal() >= best.getTier().ordinal()) best = a;
            return best;
        }
    }

    private final Path file;

    public TrophyStore(Path file) { this.file = file; }

    /** Saves the trophies earned in a finished game; the file and header are created on first use. */
    public synchronized void append(Game game) throws IOException {
        List<AchievementTracker.Unlock> unlocks = game.getAchievements().getUnlocks();
        if (unlocks.isEmpty()) return;
        String today = LocalDate.now().format(DATE);
        StringBuilder lines = new StringBuilder();
        if (!Files.exists(file) || Files.size(file) == 0) lines.append(HEADER).append('\n');
        for (AchievementTracker.Unlock u : unlocks)
            lines.append(today).append(',').append(u.getPlayer().replace(",", " ")).append(',')
                 .append(u.getAchievement().name()).append('\n');
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(file, lines.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** All saved trophies; unknown or broken rows are skipped. */
    public List<Record> readAll() throws IOException {
        List<Record> records = new ArrayList<>();
        if (!Files.exists(file)) return records;
        List<String> rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < rows.size(); i++) {
            String[] f = rows.get(i).split(",", -1);
            if (f.length != 3) continue;
            try {
                records.add(new Record(f[0], f[1], Achievement.valueOf(f[2].trim())));
            } catch (IllegalArgumentException ignored) {
                // a trophy that no longer exists
            }
        }
        return records;
    }

    /** Every player's collection, by player name, in the order the players first appear. */
    public Map<String, Collection> collections() throws IOException {
        Map<String, Collection> byPlayer = new LinkedHashMap<>();
        for (Record r : readAll()) {
            Collection c = byPlayer.computeIfAbsent(r.getPlayer(), k -> new Collection());
            c.counts.merge(r.getAchievement(), 1, Integer::sum);
            c.firstDate.putIfAbsent(r.getAchievement(), r.getDate());
        }
        return byPlayer;
    }
}