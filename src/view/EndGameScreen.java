
package view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class EndGameScreen extends JPanel {

    private static final long serialVersionUID = 1L;
	private float animationTime = 0f;
	private final Timer backgroundTimer = new Timer(60, e -> {
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
	protected void paintComponent(Graphics g) {
	    super.paintComponent(g);
	
	    Graphics2D g2 = (Graphics2D) g.create();
	    Theme.applyQuality(g2);
	
	    Theme.paintBackdrop(
	        g2, getWidth(), getHeight(), animationTime
	    );
	
	    g2.dispose();
	}
    private static final Color BG = new Color(15, 25, 52);
    private static final Color CARD = new Color(24, 39, 73);
    private static final Color TEXT = new Color(240, 245, 255);
    private static final Color MUTED = new Color(175, 190, 215);

    public EndGameScreen(String results, Runnable onBack) {
        this(results, onBack, null);
    }

    public EndGameScreen(String results, Runnable onBack, Runnable onRematch) {
        setLayout(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(new EmptyBorder(35, 50, 35, 50));

        JLabel title = new JLabel("GAME COMPLETED!", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 38));
        title.setForeground(new Color(247, 193, 74));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 15));
        center.setOpaque(false);

        JLabel heading = new JLabel("FINAL RANKINGS", SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(TEXT);
        center.add(heading, BorderLayout.NORTH);

        JTextArea resultText = new JTextArea(
        	    results.replaceFirst("^FINAL RANKINGS\\s*", "")
        	);
        resultText.setFont(new Font("SansSerif", Font.BOLD, 21));
        resultText.setForeground(MUTED);
        resultText.setOpaque(false);        resultText.setEditable(false);
        resultText.setFocusable(false);
        resultText.setLineWrap(true);
        resultText.setWrapStyleWord(true);
        resultText.setBorder(new EmptyBorder(24, 35, 24, 35));
		JScrollPane scroll = new JScrollPane(resultText);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		
		JPanel resultsCard = new JPanel(new BorderLayout()) {
		    @Override
		    protected void paintComponent(Graphics g) {
		        Graphics2D g2 = (Graphics2D) g.create();
		        Theme.applyQuality(g2);
		
		        int w = getWidth();
		        int h = getHeight();
		
		        // Dark purple glass gradient
		        g2.setPaint(new GradientPaint(
		            0, 0, new Color(40, 34, 85, 235),
		            w, h, new Color(15, 31, 65, 230)
		        ));
		
		        g2.fillRoundRect(
		            6, 6,
		            Math.max(0, w - 12),
		            Math.max(0, h - 12),
		            28, 28
		        );
		
		        // Neon lavender outline
		        g2.setColor(new Color(179, 149, 255, 165));
		        g2.setStroke(new BasicStroke(1.8f));
		
		        g2.drawRoundRect(
		            6, 6,
		            Math.max(0, w - 12),
		            Math.max(0, h - 12),
		            28, 28
		        );
		
		        g2.dispose();
		    }
		};
		
		resultsCard.setOpaque(false);
		resultsCard.setBorder(
		    new EmptyBorder(14, 14, 14, 14)
		);

        // Find the actual winner from the game results
        String winnerName = "WINNER";

        for (String line : results.split("\\R")) {
            String entry = line.trim();

            if (entry.startsWith("1. ")) {
                winnerName = entry.substring(3)
                    .replaceFirst("\\s*\\([^)]*\\)$", "")
                    .toUpperCase();
                break;
            }
        }

        JLabel winnerLabel = new JLabel(
            "★  " + winnerName + "  ★",
            SwingConstants.CENTER
        );

        winnerLabel.setFont(
            new Font("SansSerif", Font.BOLD, 30)
        );
        winnerLabel.setForeground(new Color(255, 213, 112));

        JPanel winnerBanner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);

                int w = getWidth();
                int h = getHeight();

                int glow = (int) (
                    140 + 60 * Math.sin(animationTime * 1.5)
                );

                g2.setPaint(new GradientPaint(
                    0, 0, new Color(83, 58, 102),
                    w, h, new Color(48, 42, 85)
                ));

                g2.fillRoundRect(
                    8, 8, w - 16, h - 16, 24, 24
                );

                g2.setColor(new Color(
                    255, 199, 92, glow
                ));

                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(
                    8, 8, w - 16, h - 16, 24, 24
                );

                g2.dispose();
            }
        };

        winnerBanner.setOpaque(false);
        winnerBanner.setBorder(
            new EmptyBorder(22, 30, 22, 30)
        );

        winnerBanner.add(winnerLabel, BorderLayout.CENTER);

        resultsCard.add(winnerBanner, BorderLayout.NORTH);
		resultsCard.add(scroll, BorderLayout.CENTER);
		center.add(resultsCard, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
		NeonButton backButton = new NeonButton(
		    "BACK TO MAIN MENU",
		    new Color(174, 139, 245),
		    NeonButton.Glyph.BACK,
		    null
		);
		
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
}
