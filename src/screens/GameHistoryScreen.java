package screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import model.GameHistory;

/** Read-only listing of completed games. */
public final class GameHistoryScreen extends JPanel {
    public GameHistoryScreen(Runnable back) {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(14, 25, 55));
        setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));
        JLabel heading = new JLabel("GAME HISTORY");
        heading.setFont(new Font("SansSerif", Font.BOLD, 30));
        heading.setForeground(Color.WHITE);
        add(heading, BorderLayout.NORTH);
        String[] headers = {"ID", "Difficulty", "Date / Time", "1st", "2nd", "3rd", "4th", "Duration"};
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JLabel message = new JLabel(" ");
        message.setForeground(Color.WHITE);
        try {
            List<String> rows = new GameHistory(Paths.get("data", "history.csv")).readRows();
            for (int i = 1; i < rows.size(); i++) {
                // CSV may contain quoted commas in player names.
                java.util.List<String> fields = parseCsv(rows.get(i));
                if (fields.size() == 8) model.addRow(fields.toArray());
            }
            if (model.getRowCount() == 0) message.setText("No completed games saved yet.");
        } catch (IOException e) { message.setText("Could not load history: " + e.getMessage()); }
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JButton backButton = new JButton("Back to menu");
        backButton.addActionListener(e -> back.run());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(message, BorderLayout.CENTER);
        footer.add(backButton, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private static java.util.List<String> parseCsv(String row) {
        java.util.List<String> values = new java.util.ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < row.length() && row.charAt(i + 1) == '"') { value.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { values.add(value.toString()); value.setLength(0); }
            else value.append(c);
        }
        values.add(value.toString());
        return values;
    }
}
