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
import javax.swing.SwingConstants;
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

		JTable table = new JTable(model) {
		    @Override
		    protected void paintComponent(java.awt.Graphics g) {
		        super.paintComponent(g);
		
		        if (getRowCount() == 0) {
		            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
		            g2.setColor(new Color(166, 184, 217));
		            g2.setFont(new Font("SansSerif", Font.BOLD, 19));
		
		            String text = "No completed games yet. Finish a game to see your results here.";
		            java.awt.FontMetrics fm = g2.getFontMetrics();
		
		            int x = (getWidth() - fm.stringWidth(text)) / 2;
		            int y = getHeight() / 2 + fm.getAscent() / 2;
		
		            g2.drawString(text, x, y);
		            g2.dispose();
		        }
		    }
		};
		table.setFillsViewportHeight(true);
		table.setRowHeight(30);
		table.setAutoCreateRowSorter(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        table.getTableHeader().setBackground(new Color(40, 60, 100));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(
            new Font("SansSerif", Font.BOLD, 14)
        );
        table.getTableHeader().setPreferredSize(
            new java.awt.Dimension(0, 42)
        );
        table.getTableHeader().setReorderingAllowed(false);
        table.setBackground(new Color(25, 40, 72));
        table.setForeground(new Color(235, 242, 255));

        table.setSelectionBackground(new Color(55, 95, 160));
        table.setSelectionForeground(Color.WHITE);

        table.setGridColor(new Color(55, 75, 110));
        table.setShowVerticalLines(false);

        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(table);

        scrollPane.getViewport().setBackground(new Color(25, 40, 72));

        scrollPane.setBorder(
            BorderFactory.createLineBorder(new Color(65, 90, 140), 1)
        );

        add(scrollPane, BorderLayout.CENTER);
        JButton backButton = new JButton("← Back to Menu");
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(heading, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
        backButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        backButton.setBackground(new Color(55, 85, 140));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setCursor(
            java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        );

        backButton.addActionListener(e -> back.run());
        header.add(backButton, BorderLayout.EAST);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        if (model.getRowCount() > 0) {
            footer.add(message, BorderLayout.CENTER);
        }
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
