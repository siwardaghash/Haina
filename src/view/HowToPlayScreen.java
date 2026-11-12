
package view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Game rules for players, as specified in the SRS (FR-6 to FR-30). */
public class HowToPlayScreen extends JPanel {

    private static final long serialVersionUID = 1L;

	private float animationTime = 0f;
	
	private final Timer backgroundTimer = new Timer(40, e -> {
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
	        g2,
	        getWidth(),
	        getHeight(),
	        animationTime
	    );
	
	    g2.dispose();
	}

    private static final Color BG = new Color(15, 25, 52);
    private static final Color CARD = new Color(24, 39, 73);
    private static final Color MUTED = new Color(175, 190, 215);

    /** The rule cards and their colours, in order, for the quick-jump chips. */
    private final java.util.List<JPanel> cards = new java.util.ArrayList<>();
    private final java.util.List<Color> cardAccents = new java.util.ArrayList<>();

    public HowToPlayScreen(Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(30, 45, 15, 45));

        JLabel title = Theme.neonHeading("HOW TO PLAY", new Color(198, 166, 255), 38f);
        header.add(title, BorderLayout.WEST);
        
		NeonButton back = new NeonButton(
		    "Back to Menu",
		    new Color(174, 139, 245),
		    NeonButton.Glyph.BACK,
		    null
		);
		
		back.setChevron(false);
		back.setPreferredSize(new Dimension(240, 76));
		
		back.addActionListener(e -> onBack.run());
		
		header.add(back, BorderLayout.EAST);
      
        add(header, BorderLayout.NORTH);

        JPanel content = new WidthTrackingPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 45, 35, 45));

        addCard(content, "01  GAME SETUP",
            "Choose 2–4 players, enter their names, select unique colors, " +
            "and choose Easy, Medium, or Hard difficulty.",
            new Color(86, 232, 201));

        addCard(content, "02  YOUR TURN",
            "• Click the dice to roll. Then click one of your glowing pieces on the board to move it.\n" +
            "• You move one piece per roll and cannot split the steps. If a move is possible, you must make it.\n" +
            "• A 6 on the Step die brings a piece out of the castle onto your start square. " +
            "Instead, you may use the roll to move a piece that is already on the board.\n" +
            "• No piece on the board and you rolled 1–5? The turn passes to the next player.\n" +
            "• A 6 on the Step die gives you another roll. A third 6 in a row cancels the roll and the turn passes.",
            new Color(145, 197, 255));

        addCard(content, "03  THE DICE",
            "• Easy: Step die (1–6). You move the number you rolled.\n" +
            "• Medium: Step die + BOOST die (1–3).   • Hard: Step die + ULTRA die (1–6).\n" +
            "• Steps = Step × (BOOST or ULTRA − 1). So 1 means ×0 (no move), 2 means ×1, 3 means ×2 ... 6 means ×5.\n" +
            "   Example: Step 4 and BOOST 3 → 4 × 2 = 8 steps.   Step 5 and BOOST 1 → 0 steps.\n" +
            "• When leaving the castle with a 6, the second die is not used.\n" +
            "• A piece on the home path moves by the Step die only.",
            new Color(100, 170, 255));

        addCard(content, "04  THE BOARD",
            "• Pieces move clockwise from their start square (#).\n" +
            "• Easy: one 52-square track, 4 protection squares (★), no question or surprise squares.\n" +
            "• Medium: + an outer track of 64 squares; 8 ★, 6 question squares (2 of each level), 2 surprise squares.\n" +
            "• Hard: + outer tracks of 64 and 72 squares; 12 ★, 9 question squares (3 of each level), 3 surprise squares.\n" +
            "• Medium and Hard have extra outer tracks. When your piece reaches an arrow (→) of your color, " +
            "it moves to the next inner track and continues with the remaining steps. " +
            "Arrows of other colors are regular squares.\n" +
            "• You need the exact number to enter Finish Home. If the roll is too big, move another piece; " +
            "if none can move, the turn passes.",
            new Color(196, 166, 255));

        addCard(content, "05  QUESTION SQUARES  ( ? )",
            "• Landing on a question square shows a question. Green = easy, yellow = medium, red = hard.\n" +
            "• Correct answer: move forward (easy 1, medium 2, hard 3 steps). " +
            "Wrong answer: move back the same number, and the correct answer is shown.",
            new Color(156, 120, 255));

        addCard(content, "06  SURPRISE SQUARES  ( ! )",
            "Landing on a surprise square draws one event:\n" +
            "• 10%  The piece goes straight to Finish Home.\n" +
            "• 10%  Choose an opponent's piece; a die is rolled and it moves back half the result (rounded up).\n" +
            "• 10%  The piece goes back to the castle.\n" +
            "• 30%  The piece moves back 3 steps.\n" +
            "• 30%  You lose your next turn (if this was the extra roll after a 6, that extra turn is cancelled).\n" +
            "• 10%  Roll again right away with the Step die and the ULTRA die.",
            new Color(255, 145, 214));

        addCard(content, "07  CAPTURE, PROTECTION & DOUBLE BLOCK",
            "• Landing on a single opponent piece on an unprotected square captures it: it goes back to its castle.\n" +
            "• Protected squares: protection (★), start (#), surprise (!) and home squares. " +
            "No captures there, and pieces may share the square.\n" +
            "• Two of your pieces on one square form a Double Block. Opponents cannot pass or land on it; " +
            "they stop on the square before it.\n" +
            "• Capturing or reaching Finish Home does not give an extra turn.",
            new Color(255, 110, 140));

        addCard(content, "08  HOW TO WIN",
            "Bring all four of your pieces to Finish Home. " +
            "The game ends when all players but one have finished; the last player is placed last. " +
            "The finishing order is shown and saved in the game history.",
            new Color(244, 166, 200));
        // mini visuals inside the cards
        cards.get(2).add(diceExample(), BorderLayout.SOUTH);    // 03 THE DICE
        cards.get(5).add(surpriseChart(), BorderLayout.SOUTH);  // 06 SURPRISE SQUARES
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        Theme.neonScrollBar(scrollPane, new Color(174, 139, 245));
        // Wrapped text knows its height only once its width is known, so lay out again on resize.
        scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(content::revalidate);
            }
        });
        add(scrollPane, BorderLayout.CENTER);

        // quick-jump chips under the title
        String[] chipNames = {"SETUP", "TURN", "DICE", "BOARD", "QUESTIONS", "SURPRISES", "CAPTURE", "WIN"};
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.setOpaque(false);
        chips.setBorder(new EmptyBorder(14, 0, 0, 0));
        for (int i = 0; i < cards.size(); i++) {
            JPanel target = cards.get(i);
            JButton chip = chip(chipNames[i], cardAccents.get(i));
            chip.addActionListener(e -> scrollToCard(scrollPane, target));
            chips.add(chip);
        }
        header.add(chips, BorderLayout.SOUTH);

		scrollPane.addHierarchyListener(e -> {
		    if ((e.getChangeFlags()
		            & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
		            && scrollPane.isShowing()) {
		
		        SwingUtilities.invokeLater(() -> {
		            scrollPane.getViewport().setViewPosition(
		                new Point(0, 0)
		            );
		        });
		    }
		});

    }

    private void addCard(JPanel parent, String heading,
                         String description, Color accent) {

        // Each card is as tall as its wrapped text, so longer rules are never cut off.
		JPanel card = new JPanel(new BorderLayout(0, 10)) {
		
		    @Override
		    public Dimension getMaximumSize() {
		        return new Dimension(
		            Integer.MAX_VALUE,
		            getPreferredSize().height
		        );
		    }
		
		    @Override
		    protected void paintComponent(Graphics g) {
		        super.paintComponent(g);
		
		        Graphics2D g2 = (Graphics2D) g.create();
		        Theme.applyQuality(g2);
		
		        int w = getWidth();
		        int h = getHeight();
		
		        // Dark glass-like gradient
		        g2.setPaint(new GradientPaint(
		            0, 0, new Color(30, 32, 72, 235),
		            w, h, new Color(17, 36, 65, 225)
		        ));
		        g2.fillRoundRect(5, 5, w - 10, h - 10, 24, 24);
		
		     // Brighter glow when hovering
		        boolean hovered = Boolean.TRUE.equals(
		            getClientProperty("hovered")
		        );

		        g2.setColor(new Color(
		            accent.getRed(),
		            accent.getGreen(),
		            accent.getBlue(),
		            hovered ? 110 : 35
		        ));
		        g2.setStroke(new BasicStroke(7f));
		        g2.drawRoundRect(6, 6, w - 12, h - 12, 24, 24);
		
		        // Thin neon border
		        g2.setColor(new Color(
		            accent.getRed(),
		            accent.getGreen(),
		            accent.getBlue(),
		            180
		        ));
		        g2.setStroke(new BasicStroke(1.5f));
		        g2.drawRoundRect(5, 5, w - 10, h - 10, 24, 24);
		
		        // Glowing accent line on the left
		        g2.setColor(accent);
		        g2.fillRoundRect(5, 20, 4, Math.max(4, h - 40), 4, 4);
		
		        g2.dispose();
		    }
		};

		card.setOpaque(false);        card.setBorder(new EmptyBorder(20, 25, 20, 25));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(new Font("SansSerif", Font.BOLD, 21));
        headingLabel.setForeground(accent);
        // icon badge left of the heading: "01  GAME SETUP" -> badge 1
        int number = Integer.parseInt(heading.substring(0, 2));
        JPanel headRow = new JPanel(new BorderLayout(14, 0));
        headRow.setOpaque(false);
        headRow.add(iconBadge(number, accent), BorderLayout.WEST);
        headRow.add(headingLabel, BorderLayout.CENTER);
        card.add(headRow, BorderLayout.NORTH);

        JTextArea body = new JTextArea(description);
        body.setFont(new Font("SansSerif", Font.PLAIN, 16));
        body.setForeground(MUTED);
        body.setOpaque(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFocusable(false);
        card.add(body, BorderLayout.CENTER);

		java.awt.event.MouseAdapter hoverEffect =
		    new java.awt.event.MouseAdapter() {
		
		    @Override
		    public void mouseEntered(java.awt.event.MouseEvent e) {
		        card.putClientProperty("hovered", true);
		        card.repaint();
		    }
		
		    @Override
		    public void mouseExited(java.awt.event.MouseEvent e) {
		        card.putClientProperty("hovered", false);
		        card.repaint();
		    }
		};
		
		card.addMouseListener(hoverEffect);
		headingLabel.addMouseListener(hoverEffect);
		body.addMouseListener(hoverEffect);

        parent.add(card);
        cards.add(card);
        cardAccents.add(accent);
        parent.add(Box.createVerticalStrut(14));
    }
    /** Draws one die face with 1–6 pips. */
    private static void drawDie(Graphics2D g2, float x, float y, float size,
                                int value, Color face, Color pip) {
        g2.setColor(face);
        g2.fill(new java.awt.geom.RoundRectangle2D.Float(x, y, size, size, size * 0.3f, size * 0.3f));
        // pip positions on a 3 × 3 grid: 0 = left/top, 1 = middle, 2 = right/bottom
        int[][][] layouts = {
                {{1, 1}},
                {{0, 0}, {2, 2}},
                {{0, 0}, {1, 1}, {2, 2}},
                {{0, 0}, {2, 0}, {0, 2}, {2, 2}},
                {{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}},
                {{0, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {2, 2}}};
        float r = size * 0.09f;
        g2.setColor(pip);
        for (int[] p : layouts[value - 1]) {
            float px = x + size * (0.25f + 0.25f * p[0]);
            float py = y + size * (0.25f + 0.25f * p[1]);
            g2.fill(new java.awt.geom.Ellipse2D.Float(px - r, py - r, r * 2, r * 2));
        }
    }

    /** Card 03 visual: Step 4 and BOOST 3 give 4 × (3 − 1) = 8 steps. */
    private static JComponent diceExample() {
        JComponent example = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                Color dark = new Color(22, 26, 56);
                float size = 46;
                float top = 6;
                float x = 4;
                float textY = top + size / 2 + 8;

                g2.setFont(Theme.display(22f));
                FontMetrics big = g2.getFontMetrics();

                // STEP die
                drawDie(g2, x, top, size, 4, Color.WHITE, dark);
                drawLabel(g2, "STEP", x + size / 2, top + size + 18, Color.WHITE);
                x += size + 14;

                g2.setFont(Theme.display(22f));
                g2.setColor(MUTED);
                g2.drawString("\u00D7  (", x, textY);
                x += big.stringWidth("\u00D7  (") + 10;

                // BOOST die
                drawDie(g2, x, top, size, 3, Theme.YELLOW, dark);
                drawLabel(g2, "BOOST", x + size / 2, top + size + 18, Theme.YELLOW);
                x += size + 12;

                g2.setFont(Theme.display(22f));
                g2.setColor(MUTED);
                g2.drawString("\u2212 1 )  =", x, textY);
                x += big.stringWidth("\u2212 1 )  =") + 14;

                // result
                g2.setColor(new Color(100, 170, 255));
                g2.drawString("8 STEPS", x, textY);

                g2.dispose();
            }

            private void drawLabel(Graphics2D g2, String text, float centerX, float y, Color color) {
                g2.setFont(Theme.semibold(11f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Theme.alpha(color, 200));
                g2.drawString(text, centerX - fm.stringWidth(text) / 2f, y);
            }
        };
        example.setPreferredSize(new Dimension(400, 82));
        return example;
    }
    /** Card 06 visual: the chance of each surprise event as a neon bar. */
    private static JComponent surpriseChart() {
        String[] events = {"Straight to Finish Home", "Opponent piece moves back", "Back to the castle",
                "Move back 3 steps", "Lose your next turn", "Roll again (Step + ULTRA)"};
        int[] chances = {10, 10, 10, 30, 30, 10};
        Color[] colors = {Theme.GREEN, Theme.BLUE, Theme.RED,
                new Color(255, 145, 214), Theme.YELLOW, Theme.VIOLET};

        JComponent chart = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                int labelW = 230;
                int rowH = 28;
                float maxBar = Math.max(60, getWidth() - labelW - 70);

                for (int i = 0; i < events.length; i++) {
                    int y = 6 + i * rowH;

                    g2.setFont(Theme.ui(14f, Font.PLAIN));
                    g2.setColor(MUTED);
                    g2.drawString(events[i], 4, y + 15);

                    // faint full-length track, then the coloured bar (30% = full length)
                    float barW = maxBar * chances[i] / 30f;
                    g2.setColor(Theme.alpha(colors[i], 25));
                    g2.fillRoundRect(labelW, y + 4, (int) maxBar, 12, 12, 12);
                    g2.setPaint(new GradientPaint(labelW, 0, Theme.alpha(colors[i], 120),
                            labelW + barW, 0, colors[i]));
                    g2.fillRoundRect(labelW, y + 4, (int) barW, 12, 12, 12);

                    g2.setFont(Theme.semibold(14f));
                    g2.setColor(colors[i]);
                    g2.drawString(chances[i] + "%", labelW + maxBar + 12, y + 15);
                }
                g2.dispose();
            }
        };
        chart.setPreferredSize(new Dimension(400, 6 * 28 + 10));
        return chart;
    }
    /** Small neon pill button used for the quick-jump row. */
    private static JButton chip(String text, Color accent) {
        JButton chip = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                boolean hover = getModel().isRollover();
                int w = getWidth();
                int h = getHeight();
                g2.setColor(Theme.alpha(accent, hover ? 90 : 35));
                g2.fillRoundRect(1, 1, w - 2, h - 2, h, h);
                g2.setColor(Theme.alpha(accent, hover ? 255 : 170));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(hover ? Color.WHITE : accent);
                g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2f,
                        (h + fm.getAscent() - fm.getDescent()) / 2f);
                g2.dispose();
            }
        };
        chip.setFont(Theme.semibold(13f));
        chip.setBorder(new EmptyBorder(7, 16, 7, 16));
        chip.setContentAreaFilled(false);
        chip.setFocusPainted(false);
        chip.setOpaque(false);
        chip.setRolloverEnabled(true);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return chip;
    }

    /** Smoothly scrolls so that {@code card} is at the top of the view. */
    private static void scrollToCard(JScrollPane scrollPane, JComponent card) {
        JViewport viewport = scrollPane.getViewport();
        int start = viewport.getViewPosition().y;
        int max = Math.max(0, viewport.getView().getHeight() - viewport.getHeight());
        int target = Math.min(card.getY(), max);
        long began = System.currentTimeMillis();
        Timer animation = new Timer(15, e -> {
            float t = Math.min(1f, (System.currentTimeMillis() - began) / 350f);
            int y = Math.round(start + (target - start) * Theme.easeOut(t));
            viewport.setViewPosition(new Point(0, y));
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
            }
        });
        animation.start();
    }
    /** Round glowing badge with a small drawn icon for card 1–8. */
    private static JComponent iconBadge(int number, Color accent) {
        JComponent badge = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                float r = Math.min(getWidth(), getHeight()) / 2f - 5;
                float k = r * 0.5f;   // half the size of the icon

                // glow, tinted circle and neon ring
                Theme.glow(g2, cx, cy, r + 5, accent, 90);
                java.awt.geom.Ellipse2D circle =
                        new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(Theme.alpha(accent, 70));
                g2.fill(circle);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(circle);

                // the icon itself, in white
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (number) {
                    case 1: { // game setup: a pawn
                        Theme.paintPawn(g2, cx, cy - k * 0.45f, k * 1.0f, Color.WHITE, 255);
                        break;
                    }
                    case 2: { // your turn: a circular arrow
                        g2.draw(new java.awt.geom.Arc2D.Float(cx - k, cy - k, k * 2, k * 2,
                                90, 280, java.awt.geom.Arc2D.OPEN));
                        java.awt.geom.Path2D head = new java.awt.geom.Path2D.Float();
                        head.moveTo(cx + k * 0.5f, cy - k);
                        head.lineTo(cx - k * 0.1f, cy - k * 1.45f);
                        head.lineTo(cx - k * 0.1f, cy - k * 0.55f);
                        head.closePath();
                        g2.fill(head);
                        break;
                    }
                    case 3: { // the dice: a die showing five
                        g2.draw(new java.awt.geom.RoundRectangle2D.Float(
                                cx - k, cy - k, k * 2, k * 2, k * 0.6f, k * 0.6f));
                        float p = k * 0.22f;
                        float[][] pips = {{-0.5f, -0.5f}, {0.5f, -0.5f}, {0, 0}, {-0.5f, 0.5f}, {0.5f, 0.5f}};
                        for (float[] pip : pips) {
                            g2.fill(new java.awt.geom.Ellipse2D.Float(
                                    cx + pip[0] * k - p, cy + pip[1] * k - p, p * 2, p * 2));
                        }
                        break;
                    }
                    case 4: { // the board: a 3 × 3 grid with a lit centre
                        float t = k * 2 / 3f;
                        g2.fill(new java.awt.geom.Rectangle2D.Float(cx - t / 2, cy - t / 2, t, t));
                        g2.draw(new java.awt.geom.Rectangle2D.Float(cx - k, cy - k, k * 2, k * 2));
                        g2.draw(new java.awt.geom.Line2D.Float(cx - t / 2, cy - k, cx - t / 2, cy + k));
                        g2.draw(new java.awt.geom.Line2D.Float(cx + t / 2, cy - k, cx + t / 2, cy + k));
                        g2.draw(new java.awt.geom.Line2D.Float(cx - k, cy - t / 2, cx + k, cy - t / 2));
                        g2.draw(new java.awt.geom.Line2D.Float(cx - k, cy + t / 2, cx + k, cy + t / 2));
                        break;
                    }
                    case 7: { // capture & protection: a star
                        java.awt.geom.Path2D star = new java.awt.geom.Path2D.Float();
                        for (int i = 0; i < 10; i++) {
                            double angle = Math.toRadians(-90 + i * 36);
                            float rr = (i % 2 == 0) ? k * 1.1f : k * 0.45f;
                            float px = cx + (float) (Math.cos(angle) * rr);
                            float py = cy + (float) (Math.sin(angle) * rr);
                            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
                        }
                        star.closePath();
                        g2.fill(star);
                        break;
                    }
                    default: { // 5 = ?, 6 = !, 8 = crown
                        String symbol = number == 5 ? "?" : number == 6 ? "!" : "\u265B";
                        g2.setFont(new Font("SansSerif", Font.BOLD, Math.round(r * 1.1f)));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(symbol, cx - fm.stringWidth(symbol) / 2f,
                                cy + (fm.getAscent() - fm.getDescent()) / 2f);
                        break;
                    }
                }
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(52, 52));
        return badge;
    }
    /** Follows the scroll pane's width so the text wraps instead of scrolling sideways. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
