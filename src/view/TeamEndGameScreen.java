package view;

import java.awt.*;
import javax.swing.*;
import model.Game;
import model.GameConfig;

/** End screen for a 2-vs-2 match; intentionally separate from the classic rankings/history. */
public final class TeamEndGameScreen extends JPanel {
    private static final long serialVersionUID = 1L;
    private float animationTime;
    private final Timer timer = new Timer(60, e -> { animationTime += .04f; repaint(); });

    public TeamEndGameScreen(Game game, Runnable back, Runnable rematch) {
        if (!game.isTeamMode() || !game.isGameOver())
            throw new IllegalArgumentException("A finished team match is required");
        setOpaque(false);
        setLayout(new BorderLayout(20, 24));
        setBorder(BorderFactory.createEmptyBorder(45, 65, 45, 65));
        Color winnerAccent = game.getWinningTeam() == 0 ? Theme.BLUE : Theme.GREEN;
        String winnerName = game.getWinningTeam() == 0 ? "TEAM A WINS!" : "TEAM B WINS!";
        JLabel title = Theme.neonHeading(winnerName, winnerAccent, 46f);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 22, 0));
        center.setOpaque(false);
        center.add(teamCard(game, 0, "TEAM A", Theme.BLUE));
        center.add(teamCard(game, 1, "TEAM B", Theme.GREEN));
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        bottom.setOpaque(false);
        if (rematch != null) {
            NeonButton again = new NeonButton("REMATCH", Theme.GREEN, NeonButton.Glyph.PLAY, null);
            again.setChevron(false);
            again.setPreferredSize(new Dimension(255, 78));
            again.addActionListener(e -> rematch.run());
            bottom.add(again);
        }
        NeonButton menu = new NeonButton("MAIN MENU", Theme.VIOLET, NeonButton.Glyph.BACK, null);
        menu.setChevron(false);
        menu.setPreferredSize(new Dimension(260, 78));
        menu.addActionListener(e -> { if (back != null) back.run(); });
        bottom.add(menu);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel teamCard(Game game, int team, String title, Color color) {
        JPanel card = new JPanel(new GridLayout(5, 1, 5, 5));
        card.setBackground(new Color(25, 30, 65));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(20, 18, 20, 18)));
        card.add(label(title + (game.getWinningTeam() == team ? "  ★ WINNERS" : ""), color, 26));
        card.add(label(team == 0 ? "RED + YELLOW" : "GREEN + BLUE", Theme.TEXT, 17));
        for (Game.Player player : game.getPlayers()) {
            if (GameConfig.teamOf(player.getColor()) != team) continue;
            int finished = 0;
            for (model.Piece piece : player.getPieces()) if (piece.isFinished()) finished++;
            card.add(label(player.getName() + " (" + player.getColor().getLabel() + ")  "
                    + finished + "/4 home", Theme.TEXT, 17));
        }
        card.add(label("Difficulty: " + game.getDifficulty().getLabel()
                + "  ·  Time: " + formatTime(game.getDurationSeconds()), Theme.MUTED, 14));
        return card;
    }

    private static JLabel label(String text, Color color, int size) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(color);
        label.setFont(new Font("SansSerif", Font.BOLD, size));
        return label;
    }

    private static String formatTime(long sec) {
        return String.format("%02d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60);
    }

    @Override public void addNotify() { super.addNotify(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.paintBackdrop(g, getWidth(), getHeight(), animationTime);
        g.dispose();
    }
}
