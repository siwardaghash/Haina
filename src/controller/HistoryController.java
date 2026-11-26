package controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import model.GameRecord;
import model.SysData;
import model.TrophyStore;

/** Controller of the history screen (FR-33). */
public final class HistoryController {
    /** All saved games, in the order they were played. */
    public List<GameRecord> records() throws IOException { return SysData.get().history().readRecords(); }

    /** Bonus: every player's trophy collection, by player name. */
    public Map<String, TrophyStore.Collection> trophies() throws IOException { return SysData.get().trophies().collections(); }
}