package controller;

import java.io.IOException;
import java.util.List;
import model.GameRecord;
import model.SysData;

/** Controller of the history screen (FR-33). */
public final class HistoryController {
    /** All saved games, in the order they were played. */
    public List<GameRecord> records() throws IOException { return SysData.get().history().readRecords(); }
}
