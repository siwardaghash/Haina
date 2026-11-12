
package view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.PlayerColor;
public class EndGameScreen extends JPanel {

    private static final long serialVersionUID = 1L;
	private float animationTime = 0f;
	private final Timer backgroundTimer = new Timer(30, e -> {
	    animationTime += 0.03f;
	    repaint();
	});

	/** Confetti pieces: x (0–1), start y, fall speed, size, sway phase, colour index. */
	private final float[][] confetti = createConfetti(110);
	
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
	protected void paintComponent(Graphics g) {
	    super.paintComponent(g);
	
	    Graphics2D g2 = (Graphics2D) g.create();
	    Theme.applyQuality(g2);
	
	    Theme.paintBackdrop(
	        g2, getWidth(), getHeight(), animationTime
	    ); 	    paintConfetti(g2);
	    
	
	    g2.dispose();
	}
  
    private static final Color MUTED = new Color(175, 190, 215);
    private static final Color GOLD = new Color(255, 210, 107);

    public EndGameScreen(String results, Runnable onBack) {
        this(results, onBack, null);
    }

    public EndGameScreen(String results, Runnable onBack, Runnable onRematch) {
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(20, 50, 28, 50));

        // read "1. panda (RED)", "Difficulty: Easy", "Duration: 00:12:01" from the results text
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.List<PlayerColor> colors = new java.util.ArrayList<>();
        String difficulty = "";
        String duration = "";
        boolean saved = results.contains("Result saved");
        for (String line : results.split("\\R")) {
            String entry = line.trim();
            if (entry.matches("\\d+\\. .+ \\(\\w+\\)")) {
                int open = entry.lastIndexOf(" (");
                names.add(entry.substring(entry.indexOf(". ") + 2, open));
                colors.add(PlayerColor.valueOf(entry.substring(open + 2, entry.length() - 1)));
            } else if (entry.startsWith("Difficulty:")) {
                difficulty = entry.substring(11).trim();
            } else if (entry.startsWith("Duration:")) {
                duration = entry.substring(9).trim();
            }
        }

        // the winner on a glowing stage
        String winner = names.isEmpty() ? "WINNER" : names.get(0);
        Color winnerColor = colors.isEmpty() ? GOLD : Theme.of(colors.get(0));
        add(victoryStage(winner, winnerColor), BorderLayout.NORTH);

        // one card per player in finishing order, then the game info
        JPanel ranks = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        ranks.setOpaque(false);
        for (int i = 0; i < names.size(); i++) {
            ranks.add(rankCard(i + 1, names.get(i), colors.get(i)));
        }

        JPanel info = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        info.setOpaque(false);
        info.add(infoChip("DIFFICULTY", difficulty.toUpperCase(), difficultyColor(difficulty)));
        info.add(infoChip("DURATION", duration, Theme.BLUE));
        info.add(infoChip(saved ? "\u2713" : "!", saved ? "SAVED TO HISTORY" : "NOT SAVED",
                saved ? Theme.GREEN : Theme.RED));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(Box.createVerticalGlue());
        center.add(ranks);
        center.add(Box.createVerticalStrut(18));
        center.add(info);
        center.add(Box.createVerticalGlue());
        add(center, BorderLayout.CENTER);

        // buttons
        NeonButton backButton = new NeonButton(
                "BACK TO MAIN MENU", new Color(174, 139, 245), NeonButton.Glyph.BACK, null);
        backButton.setChevron(false);
        backButton.setPreferredSize(new Dimension(340, 78));
        backButton.addActionListener(e -> {
            if (onBack != null) onBack.run();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        if (onRematch != null) {
            NeonButton rematchButton = new NeonButton(
                    "REMATCH", new Color(89, 218, 166), NeonButton.Glyph.PLAY, null);
            rematchButton.setChevron(false);
            rematchButton.setPreferredSize(new Dimension(240, 78));
            rematchButton.addActionListener(e -> onRematch.run());
            footer.add(rematchButton);
        }
        footer.add(backButton);
        add(footer, BorderLayout.SOUTH);
    }
    

    /** Random starting values for each confetti piece (same pattern every time). */
    private static float[][] createConfetti(int count) {
        java.util.Random random = new java.util.Random(7);
        float[][] pieces = new float[count][6];
        for (float[] piece : pieces) {
            piece[0] = random.nextFloat();                    // x across the screen (0–1)
            piece[1] = random.nextFloat();                    // start height (0–1)
            piece[2] = 0.05f + random.nextFloat() * 0.10f;    // fall speed
            piece[3] = 7 + random.nextFloat() * 7;            // size in pixels
            piece[4] = random.nextFloat() * 6.28f;            // sway / spin phase
            piece[5] = random.nextInt(5);                     // colour index
        }
        return pieces;
    }

    /** Falling confetti in the four player colours and gold. */
    private void paintConfetti(Graphics2D g2) {
        Color[] palette = {Theme.RED, Theme.GREEN, Theme.YELLOW, Theme.BLUE, GOLD};
        int w = getWidth();
        int h = getHeight();
        for (float[] piece : confetti) {
            float y = ((piece[1] + animationTime * piece[2]) % 1f) * (h + 40) - 20;
            float x = piece[0] * w + (float) Math.sin(animationTime * 2 + piece[4]) * 18;
            float size = piece[3];
            Graphics2D p = (Graphics2D) g2.create();
            p.translate(x, y);
            p.rotate(animationTime * 3 + piece[4]);
            p.scale(1, Math.abs(Math.sin(animationTime * 4 + piece[4])) * 0.8 + 0.2); // flipping
            p.setColor(Theme.alpha(palette[(int) piece[5]], 190));
            p.fillRect((int) (-size / 2), (int) (-size / 4), (int) size, (int) (size / 2));
            p.dispose();
        }
        
    }

    /** The big centre piece: spotlights, the winner's pawn on a glowing ring, crown and name. */
    private JComponent victoryStage(String winner, Color color) {
        JComponent stage = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                float cx = getWidth() / 2f;
                float pulse = (float) (0.5 + 0.5 * Math.sin(animationTime * 2.2));
                float bob = (float) Math.sin(animationTime * 1.6) * 5;
                float ringY = 222;

                // two spotlights shining down on the stage
                for (int side = -1; side <= 1; side += 2) {
                    java.awt.geom.Path2D beam = new java.awt.geom.Path2D.Float();
                    beam.moveTo(cx + side * 330, 0);
                    beam.lineTo(cx + side * 270, 0);
                    beam.lineTo(cx - side * 40, ringY);
                    beam.lineTo(cx + side * 110, ringY);
                    beam.closePath();
                    g2.setPaint(new GradientPaint(0, 0, Theme.alpha(Color.WHITE, 0),
                            0, ringY, Theme.alpha(color, 55 + (int) (25 * pulse))));
                    g2.fill(beam);
                }

                // glowing ring the pawn stands on
                Theme.glow(g2, cx, ringY, 150, color, 70 + (int) (40 * pulse));
                g2.setPaint(new GradientPaint(0, ringY - 18, Theme.alpha(color, 140),
                        0, ringY + 18, Theme.alpha(color, 30)));
                g2.fill(new java.awt.geom.Ellipse2D.Float(cx - 120, ringY - 18, 240, 36));
                g2.setStroke(new BasicStroke(2.5f));
                g2.setColor(Theme.alpha(color, 230));
                g2.draw(new java.awt.geom.Ellipse2D.Float(cx - 120, ringY - 18, 240, 36));
                g2.setColor(Theme.alpha(Color.WHITE, 90));
                g2.draw(new java.awt.geom.Ellipse2D.Float(cx - 84, ringY - 11, 168, 22));

                // big winner pawn, gently floating
                Theme.paintPawn(g2, cx, ringY - 72 + bob, 50, color, 255);

                // crown above the pawn
                paintCrown(g2, cx, ringY - 116 + bob, 32, pulse);

                // small label at the top
                g2.setFont(Theme.semibold(15f));
                g2.setColor(Theme.alpha(GOLD, 220));
                String label = "GAME COMPLETED";
                float lw = Theme.trackedWidth(g2, label, 6);
                Theme.drawTracked(g2, label, cx - lw / 2, 22, 6);

                // winner name with neon glow
                String name = winner.toUpperCase();
                g2.setFont(Theme.display(54f));
                FontMetrics fm = g2.getFontMetrics();
                float nx = cx - fm.stringWidth(name) / 2f;
                float ny = ringY + 82;
                g2.setColor(Theme.alpha(color, 50 + (int) (40 * pulse)));
                for (int dx = -4; dx <= 4; dx += 4) {
                    for (int dy = -4; dy <= 4; dy += 4) {
                        g2.drawString(name, nx + dx, ny + dy);
                    }
                }
                g2.setPaint(new GradientPaint(nx, 0, Color.WHITE, nx + fm.stringWidth(name), 0,
                        Theme.mix(color, Color.WHITE, 0.25f)));
                g2.drawString(name, nx, ny);

                g2.setFont(Theme.semibold(17f));
                g2.setColor(GOLD);
                String sub = "WINS THE GAME!";
                float sw = Theme.trackedWidth(g2, sub, 5);
                Theme.drawTracked(g2, sub, cx - sw / 2, ny + 34, 5);

                g2.dispose();
            }
        };
        stage.setPreferredSize(new Dimension(800, 345));
        return stage;
    }

    /** A gold crown with three points and jewels. */
    private static void paintCrown(Graphics2D g2, float cx, float baseY, float s, float pulse) {
        Theme.glow(g2, cx, baseY - s * 0.4f, s * 1.6f, GOLD, 60 + (int) (40 * pulse));
        java.awt.geom.Path2D crown = new java.awt.geom.Path2D.Float();
        crown.moveTo(cx - s, baseY);
        crown.lineTo(cx - s * 1.05f, baseY - s * 0.9f);
        crown.lineTo(cx - s * 0.5f, baseY - s * 0.45f);
        crown.lineTo(cx, baseY - s * 1.15f);
        crown.lineTo(cx + s * 0.5f, baseY - s * 0.45f);
        crown.lineTo(cx + s * 1.05f, baseY - s * 0.9f);
        crown.lineTo(cx + s, baseY);
        crown.closePath();
        g2.setPaint(new GradientPaint(0, baseY - s, new Color(255, 236, 160), 0, baseY, new Color(230, 160, 40)));
        g2.fill(crown);
        g2.setColor(new Color(255, 245, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(crown);
        float[][] jewels = {{-1.05f, -0.9f}, {0, -1.15f}, {1.05f, -0.9f}};
        for (float[] j : jewels) {
            float jr = s * 0.13f;
            g2.setColor(Color.WHITE);
            g2.fill(new java.awt.geom.Ellipse2D.Float(cx + j[0] * s - jr, baseY + j[1] * s - jr, jr * 2, jr * 2));
        }
        g2.setColor(Theme.RED);
        g2.fill(new java.awt.geom.Ellipse2D.Float(cx - s * 0.12f, baseY - s * 0.38f, s * 0.24f, s * 0.24f));
    }

    /** Glass card for one player: medal with the rank, pawn, name and colour. */
    private static JComponent rankCard(int rank, String name, PlayerColor playerColor) {
        Color color = Theme.of(playerColor);
        Color[] medals = {GOLD, new Color(200, 212, 235), new Color(222, 150, 100), MUTED};
        Color medal = medals[Math.min(rank, 4) - 1];

        JComponent card = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                java.awt.Shape body = new java.awt.geom.RoundRectangle2D.Float(2, 2, w - 4, h - 4, 22, 22);

                g2.setPaint(new GradientPaint(0, 0, Theme.alpha(color, 55), 0, h, Theme.GLASS));
                g2.fill(body);
                g2.setStroke(new BasicStroke(rank == 1 ? 2.4f : 1.6f));
                g2.setColor(Theme.alpha(rank == 1 ? GOLD : color, rank == 1 ? 240 : 170));
                g2.draw(body);

                // medal with the rank number
                float mx = 34;
                float my = h / 2f;
                g2.setColor(Theme.alpha(medal, 60));
                g2.fill(new java.awt.geom.Ellipse2D.Float(mx - 20, my - 20, 40, 40));
                g2.setColor(medal);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new java.awt.geom.Ellipse2D.Float(mx - 20, my - 20, 40, 40));
                g2.setFont(Theme.display(19f));
                FontMetrics fm = g2.getFontMetrics();
                String number = String.valueOf(rank);
                g2.drawString(number, mx - fm.stringWidth(number) / 2f, my + (fm.getAscent() - fm.getDescent()) / 2f);

                // pawn in the player's colour
                Theme.paintPawn(g2, 84, my - 6, 13, color, 255);

                // name and colour
                g2.setFont(Theme.semibold(18f));
                g2.setColor(Theme.TEXT);
                g2.drawString(name, 112, my - 2);
                g2.setFont(Theme.ui(13f, Font.PLAIN));
                g2.setColor(color);
                g2.drawString(playerColor.getLabel().toUpperCase(), 112, my + 18);

                g2.dispose();
            }
        };
        card.setPreferredSize(new Dimension(230, 84));
        return card;
    }

    /** Small pill with a muted label and a coloured value, e.g. DIFFICULTY  EASY. */
    private static JComponent infoChip(String label, String value, Color color) {
        JComponent chip = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(Theme.alpha(color, 30));
                g2.fillRoundRect(1, 1, w - 2, h - 2, h, h);
                g2.setColor(Theme.alpha(color, 160));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);

                g2.setFont(Theme.semibold(12f));
                FontMetrics fm = g2.getFontMetrics();
                float y = (h + fm.getAscent() - fm.getDescent()) / 2f;
                g2.setColor(MUTED);
                float x = Theme.drawTracked(g2, label, 18, y, 2) + 10;
                g2.setFont(Theme.semibold(15f));
                fm = g2.getFontMetrics();
                g2.setColor(color);
                g2.drawString(value, x, (h + fm.getAscent() - fm.getDescent()) / 2f);
                g2.dispose();
            }
        };
        chip.setPreferredSize(new Dimension(label.length() * 9 + value.length() * 10 + 50, 38));
        return chip;
    }

    private static Color difficultyColor(String difficulty) {
        if (difficulty.equalsIgnoreCase("medium")) return Theme.YELLOW;
        if (difficulty.equalsIgnoreCase("hard")) return Theme.RED;
        return Theme.GREEN;
    }
    
}
