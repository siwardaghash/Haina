package model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Game setup validation: SRS FR-3, FR-5. */
class GameConfigTest {
    private static GameConfig.PlayerSetup p(String name, PlayerColor c) { return new GameConfig.PlayerSetup(name, c); }

    @Test @DisplayName("FR-3: a game has 2 to 4 players")
    void twoToFourPlayers() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(Difficulty.EASY, Arrays.asList(p("A", PlayerColor.RED))));
        assertDoesNotThrow(() -> new GameConfig(Difficulty.EASY, Arrays.asList(p("A", PlayerColor.RED), p("B", PlayerColor.GREEN))));
    }

    @Test @DisplayName("FR-5: names cannot be empty or repeated, colours must be unique")
    void namesAndColoursAreUnique() {
        assertThrows(IllegalArgumentException.class, () -> p("  ", PlayerColor.RED));
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(Difficulty.EASY, Arrays.asList(p("A", PlayerColor.RED), p("a", PlayerColor.GREEN))));
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(Difficulty.EASY, Arrays.asList(p("A", PlayerColor.RED), p("B", PlayerColor.RED))));
    }

    @Test @DisplayName("Names are at most 12 characters")
    void nameLengthIsLimited() {
        assertThrows(IllegalArgumentException.class, () -> p("ThirteenChars", PlayerColor.RED));
        assertDoesNotThrow(() -> p("TwelveChars!", PlayerColor.RED));
    }

    @Test @DisplayName("Team mode needs exactly four players")
    void teamModeNeedsFourPlayers() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(Difficulty.EASY,
                Arrays.asList(p("A", PlayerColor.RED), p("B", PlayerColor.GREEN)), GameConfig.GameMode.TEAMS_2V2));
    }
}
