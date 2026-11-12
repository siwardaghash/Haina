package view;

import controller.HistoryController;
import model.GameRecord;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.SwingConstants;
import model.GameStatistics;
import model.PlayerStatistics;
import javax.swing.JOptionPane;


/** Read-only listing of completed games. */
public final class GameHistoryScreen extends JPanel {
	private float animationTime = 0f;
	private final javax.swing.Timer backgroundTimer =
	    new javax.swing.Timer(40, e -> {
	        animationTime += 0.04f;
	        repaint();
	    });
	@Override
	public void addNotify() {
	    super.addNotify();
	    backgroundTimer.start();
	}	
	@Override
	public void removeNotify() {
	    backgroundTimer.stop();
	    super.removeNotify();
	}	
	@Override
	protected void paintComponent(java.awt.Graphics g) {
	    super.paintComponent(g);	
	    java.awt.Graphics2D g2 =
	        (java.awt.Graphics2D) g.create();	
	    Theme.applyQuality(g2);
	    Theme.paintBackdrop(
	        g2,
	        getWidth(),
	        getHeight(),
	        animationTime
	    );
		    g2.dispose();
	}
    public GameHistoryScreen(HistoryController controller, Runnable back) {
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
            for (GameRecord record : controller.records())
                model.addRow(record.toCsvFields().toArray());
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
		table.setFillsViewportHeight(false);		table.setRowHeight(30);
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

		table.setRowHeight(52);
		table.setFont(new Font("SansSerif", Font.PLAIN, 16));
		table.setIntercellSpacing(new java.awt.Dimension(0, 2));
		
		table.getTableHeader().setBackground(new Color(55, 43, 102));
		table.getTableHeader().setForeground(new Color(230, 217, 255));
		table.getTableHeader().setFont(
		    new Font("SansSerif", Font.BOLD, 15)
		);
		table.getTableHeader().setPreferredSize(
		    new java.awt.Dimension(0, 54)
		);
		
		table.setDefaultRenderer(
		    Object.class,
		    new javax.swing.table.DefaultTableCellRenderer() {
		        @Override
		        public java.awt.Component getTableCellRendererComponent(
		                JTable t, Object value, boolean selected,
		                boolean focused, int row, int column) {
		
		            super.getTableCellRendererComponent(
		                t, value, selected, focused, row, column
		            );
		
		            setBorder(BorderFactory.createEmptyBorder(
		                8, 12, 8, 12
		            ));
		
		            if (selected) {
		                setBackground(new Color(78, 62, 137));
		                setForeground(Color.WHITE);
		            } else {
		                setBackground(row % 2 == 0
		                    ? new Color(34, 32, 70)
		                    : new Color(27, 35, 68));
		
		                setForeground(new Color(225, 231, 255));
		
		                if (t.convertColumnIndexToModel(column) == 3) {
		                    setForeground(new Color(198, 166, 255));
		                    setFont(getFont().deriveFont(Font.BOLD));
		                } else {
		                    setFont(getFont().deriveFont(Font.PLAIN));
		                }
		            }
		            return this;
		        }
		    }
		);

        JScrollPane scrollPane = new JScrollPane(table);

		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);

		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		if (model.getRowCount() == 0) {
		
		    JPanel emptyArea = new JPanel(
		        new java.awt.GridBagLayout()
		    );
		    emptyArea.setOpaque(false);
		
		    JPanel emptyCard = new JPanel(
		        new BorderLayout(0, 15)
		    ) {
		        @Override
		        protected void paintComponent(java.awt.Graphics g) {
		            java.awt.Graphics2D g2 =
		                (java.awt.Graphics2D) g.create();
		
		            Theme.applyQuality(g2);
		
		            int w = getWidth();
		            int h = getHeight();
		
		            g2.setPaint(new java.awt.GradientPaint(
		                0, 0, new Color(45, 34, 88),
		                w, h, new Color(20, 45, 75)
		            ));
		            g2.fillRoundRect(3, 3, w - 6, h - 6, 28, 28);
		
		            g2.setColor(new Color(166, 137, 255, 190));
		            g2.setStroke(new java.awt.BasicStroke(2f));
		            g2.drawRoundRect(3, 3, w - 6, h - 6, 28, 28);
		
		            g2.dispose();
		        }
		    };
		
		    emptyCard.setOpaque(false);
		    emptyCard.setBorder(
		        BorderFactory.createEmptyBorder(35, 65, 35, 65)
		    );
		
		    JLabel icon = new JLabel(
		        "\u2726", SwingConstants.CENTER
		    );
		    icon.setFont(new Font("SansSerif", Font.BOLD, 48));
		    icon.setForeground(new Color(174, 139, 245));
		
		    boolean loadError = message.getText()
		        .startsWith("Could not load history:");
		
		    JLabel title = new JLabel(
		        loadError ? "HISTORY UNAVAILABLE" : "YOUR STORY STARTS HERE",
		        SwingConstants.CENTER
		    );
		    title.setFont(new Font("SansSerif", Font.BOLD, 23));
		    title.setForeground(new Color(224, 209, 255));
		
		    JLabel subtitle = new JLabel(
		        loadError ? message.getText()
		                  : "Finish your first game to unlock your history!",
		        SwingConstants.CENTER
		    );
		    subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
		    subtitle.setForeground(new Color(181, 192, 224));
		
		    JPanel words = new JPanel(
		        new java.awt.GridLayout(2, 1, 0, 12)
		    );
		    words.setOpaque(false);
		    words.add(title);
		    words.add(subtitle);
		
		    emptyCard.add(icon, BorderLayout.NORTH);
		    emptyCard.add(words, BorderLayout.CENTER);
		
		    emptyArea.add(emptyCard);
		    add(emptyArea, BorderLayout.CENTER);
		
		} else {
		    add(scrollPane, BorderLayout.CENTER);
		}
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		
		header.add(heading, BorderLayout.WEST);
		
		NeonButton backButton = new NeonButton(
		    "Back to Menu",
		    new Color(174, 139, 245),
		    NeonButton.Glyph.BACK,
		    null
		);
		
		backButton.setChevron(false);
		backButton.setPreferredSize(
		    new java.awt.Dimension(240, 76)
		);
		backButton.addActionListener(e -> back.run());
		
		header.add(backButton, BorderLayout.EAST);
		add(header, BorderLayout.NORTH);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        if (model.getRowCount() > 0) {
            footer.add(message, BorderLayout.CENTER);
        }
        NeonButton statisticsButton = new NeonButton(
                "Scoreboard",
                new Color(174, 139, 245),
                NeonButton.Glyph.HISTORY,
                null);
        statisticsButton.setChevron(false);
        statisticsButton.setPreferredSize(new java.awt.Dimension(240, 76));
        statisticsButton.addActionListener(e -> showStatistics(controller));
        footer.add(statisticsButton, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    
        }
    
    private void showStatistics(HistoryController controller) {
        List<GameRecord> games;
        try {
            games = controller.records();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not load history: " + e.getMessage(),
                    "Game & Player Statistics", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (games.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Finish a game to see your statistics.");
            return;
        }

        GameStatistics totals = GameStatistics.from(games);
        
        DefaultTableModel statsModel = new DefaultTableModel(
                new String[]{"Player", "Games played", "Wins", "Win rate (%)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return String.class;
                if (column == 3) return Double.class;
                return Integer.class;
            }
        };

        for (PlayerStatistics player : PlayerStatistics.from(games)) {
            double rate = Math.round(player.winRate() * 10.0) / 10.0;
            statsModel.addRow(new Object[]{player.name(), player.gamesPlayed(), player.wins(), rate});
        }

        Color lavender = new Color(198, 166, 255);

        JTable statsTable = new JTable(statsModel);
        statsTable.setAutoCreateRowSorter(true);
        statsTable.setFillsViewportHeight(true);
        statsTable.setPreferredScrollableViewportSize(new java.awt.Dimension(580, 240));
        statsTable.setRowHeight(36);
        statsTable.setBackground(new Color(32, 37, 70));
        statsTable.setForeground(new Color(235, 240, 255));
        statsTable.setSelectionBackground(new Color(78, 62, 137));
        statsTable.setSelectionForeground(Color.WHITE);
        statsTable.setGridColor(new Color(54, 61, 95));
        statsTable.getTableHeader().setBackground(new Color(55, 43, 102));
        statsTable.getTableHeader().setForeground(Color.WHITE);

        JPanel cards = new JPanel(new java.awt.GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);
        cards.add(statCard("TOTAL GAMES", totals.totalGames(), lavender));
        cards.add(statCard("EASY", totals.easyGames(), new Color(89, 218, 166)));
        cards.add(statCard("MEDIUM", totals.mediumGames(), new Color(255, 210, 107)));
        cards.add(statCard("HARD", totals.hardGames(), new Color(255, 120, 150)));

        JLabel heading = new JLabel("LUDO SCOREBOARD");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(lavender);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(new Color(20, 24, 49));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(statsTable), BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, "LUDO • Code Quest",
                JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel statCard(String title, int count, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 5)) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                Theme.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                java.awt.Shape shape = new java.awt.geom.RoundRectangle2D.Float(
                        2, 2, w - 4, h - 4, 22, 22);

                g2.setPaint(new java.awt.GradientPaint(
                        0, 0, Theme.alpha(accent, 60),
                        0, h, Theme.GLASS));
                g2.fill(shape);

                java.awt.Shape oldClip = g2.getClip();
                g2.clip(shape);
                Theme.glow(g2, w / 2f, h * 0.40f, w * 0.45f, accent, 70);
                g2.setClip(oldClip);

                g2.setColor(Theme.alpha(accent, 210));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.draw(shape);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 10, 12, 10));
        JLabel value = new JLabel(String.valueOf(count), SwingConstants.CENTER);
        value.setFont(new Font("SansSerif", Font.BOLD, 28));
        value.setForeground(accent);

        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(new Color(220, 225, 250));

        card.add(value, BorderLayout.CENTER);
        card.add(label, BorderLayout.SOUTH);
        return card;
    }
}
