package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class GameConfig {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;
    public static final int NAME_MAX_LENGTH = 12;

    public static final class PlayerSetup {
        private final String name;
        private final PlayerColor color;

        public PlayerSetup(String name, PlayerColor color) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Player name is required");
            }
            if (name.trim().length() > NAME_MAX_LENGTH) {
                throw new IllegalArgumentException("Player name is too long: " + name);
            }
            if (color == null) {
                throw new IllegalArgumentException("Player colour is required");
            }
            this.name = name.trim();
            this.color = color;
        }

        public String getName()        { return name; }
        public PlayerColor getColor()  { return color; }

        @Override
        public String toString() {
            return name + " (" + color.getLabel() + ")";
        }
    }

    private final Difficulty difficulty;
    private final List<PlayerSetup> players;

    public GameConfig(Difficulty difficulty, List<PlayerSetup> players) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty is required");
        }
        if (players == null || players.size() < MIN_PLAYERS || players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("A game needs " + MIN_PLAYERS + "-" + MAX_PLAYERS + " players");
        }
        Set<String> names = new HashSet<>();
        Set<PlayerColor> colors = EnumSet.noneOf(PlayerColor.class);
        for (PlayerSetup p : players) {
            if (!names.add(p.getName().toLowerCase())) {
                throw new IllegalArgumentException("Duplicate player name: " + p.getName());
            }
            if (!colors.add(p.getColor())) {
                throw new IllegalArgumentException("Duplicate player colour: " + p.getColor());
            }
        }
        this.difficulty = difficulty;
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
    }

    public Difficulty getDifficulty()     { return difficulty; }
    public List<PlayerSetup> getPlayers() { return players; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Difficulty: ").append(difficulty.getLabel());
        for (int i = 0; i < players.size(); i++) {
            sb.append("\nPlayer ").append(i + 1).append(": ").append(players.get(i));
        }
        return sb.toString();
    }
}