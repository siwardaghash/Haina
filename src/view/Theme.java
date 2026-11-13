package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import model.PlayerColor;

/**
 * Shared look of every screen: palette, fonts, easing helpers and common drawing.
 * Keeping this in one place means all screens stay visually consistent.
 */
public final class Theme {

    private Theme() {
    }

    /**
     * Makes a button paint its own background colour. The Mac look and feel ignores
     * setBackground and draws a white button, so white text becomes invisible.
     */
    public static void solidButton(javax.swing.AbstractButton button) {
        javax.swing.border.Border border = button.getBorder();
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(border == null || border instanceof javax.swing.plaf.UIResource
                ? javax.swing.BorderFactory.createEmptyBorder(10, 18, 10, 18) : border);
    }

    // ------------------------------------------------------------------ palette
    public static final Color RED    = new Color(0xFF4D6D);
    public static final Color GREEN  = new Color(0x2EE59D);
    public static final Color YELLOW = new Color(0xFFC93C);
    public static final Color BLUE   = new Color(0x3D8BFF);
    /** Same order as {@link PlayerColor}. */
    public static final Color[] PLAYERS = {RED, GREEN, YELLOW, BLUE};

    public static final Color VIOLET    = new Color(0x8B7BFF);
    public static final Color BG_TOP    = new Color(0x0A0E1F);
    public static final Color BG_BOTTOM = new Color(0x171D3D);
    public static final Color TEXT      = new Color(0xE8ECFF);
    public static final Color MUTED     = new Color(0x8A93B8);
    public static final Color GRID      = new Color(0x6C7BFF);
    public static final Color GLASS     = new Color(10, 14, 32, 205);

    public static Color of(PlayerColor c) {
        return PLAYERS[c.ordinal()];
    }

    // ------------------------------------------------------------------ fonts
    public static Font display(float size) {
        return pickFont(Font.SANS_SERIF, Font.BOLD, size, "Segoe UI Black", "Arial Black", "Impact");
    }

    public static Font ui(float size, int style) {
        return pickFont(Font.SANS_SERIF, style, size, "Segoe UI", "Helvetica Neue", "Arial");
    }

    public static Font semibold(float size) {
        return pickFont(Font.SANS_SERIF, Font.BOLD, size, "Segoe UI Semibold", "Segoe UI", "Helvetica Neue", "Arial");
    }

    public static Font mono(float size) {
        return pickFont(Font.MONOSPACED, Font.BOLD, size, "Consolas", "JetBrains Mono", "Menlo");
    }

    private static Set<String> families;

    /** Returns the first installed font from {@code names}, or the logical fallback font. */
    public static Font pickFont(String fallback, int style, float size, String... names) {
        if (families == null) {
            families = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        }
        for (String n : names) {
            if (families.contains(n)) {
                return new Font(n, style, 1).deriveFont(size);
            }
        }
        return new Font(fallback, style, 1).deriveFont(size);
    }

    // ------------------------------------------------------------------ math / colour helpers
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color mix(Color a, Color b, float k) {
        k = clamp01(k);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    public static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /** Smoothly moves {@code v} toward {@code target}; frame-rate independent. */
    public static float approach(float v, float target, float dt, float speed) {
        return v + (target - v) * Math.min(1f, dt * speed);
    }

    public static float easeOut(float v) {
        float k = 1f - clamp01(v);
        return 1f - k * k * k;
    }

    public static float easeOutBack(float v) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float k = v - 1f;
        return 1f + c3 * k * k * k + c1 * k * k;
    }

    public static void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ------------------------------------------------------------------ shared drawing
    public static void glow(Graphics2D g, float x, float y, float r, Color c, int a) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), r, new float[] {0f, 1f},
                new Color[] {alpha(c, a), alpha(c, 0)}));
        g.fill(new Rectangle2D.Float(x - r, y - r, r * 2, r * 2));
    }

    /** Dark gradient + breathing glow in each corner (one per player colour). */
    private static java.awt.image.BufferedImage backdrop;
    private static int backdropW, backdropH;
    private static double backdropScale;
    private static float backdropT = -1;

    /**
     * The animated background, kept in an image: the five large glows are expensive to draw every
     * frame (about 60 ms on a Retina screen), while their slow pulse looks the same if refreshed
     * five times a second.
     */
    public static void paintBackdrop(Graphics2D g, int w, int h, float t) {
        double scale = Math.max(1, g.getTransform().getScaleX());
        if (backdrop == null || backdropW != w || backdropH != h || backdropScale != scale || Math.abs(t - backdropT) > 0.2f) {
            int iw = (int) Math.ceil(w * scale), ih = (int) Math.ceil(h * scale);
            if (backdrop == null || backdrop.getWidth() != iw || backdrop.getHeight() != ih)
                backdrop = new java.awt.image.BufferedImage(Math.max(1, iw), Math.max(1, ih), java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = backdrop.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bg.scale(scale, scale);
            drawBackdrop(bg, w, h, t);
            bg.dispose();
            backdropW = w; backdropH = h; backdropScale = scale; backdropT = t;
        }
        g.drawImage(backdrop, 0, 0, w, h, null);
    }

    private static void drawBackdrop(Graphics2D g, int w, int h, float t) {
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g.fillRect(0, 0, w, h);
        float r = Math.max(w, h) * 0.55f;
        float[][] corners = {{0, 0}, {w, 0}, {w, h}, {0, h}};
        for (int i = 0; i < 4; i++) {
            int a = (int) (34 + 16 * Math.sin(t * 0.9 + i * 1.6));
            glow(g, corners[i][0], corners[i][1], r, PLAYERS[i], a);
        }
        glow(g, w / 2f, h * 0.30f, w * 0.42f, new Color(0x6C5BFF), 48);
    }

    /** A Ludo pawn (head + cone body + base) with a soft glow. */
    public static void paintPawn(Graphics2D g, float cx, float cy, float r, Color c, int a) {
        float gr = r * 2.6f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), gr,
                new float[] {0f, 1f}, new Color[] {alpha(c, a / 3), alpha(c, 0)}));
        g.fill(new Ellipse2D.Float(cx - gr, cy - gr, gr * 2, gr * 2));

        Path2D body = new Path2D.Float();
        body.moveTo(cx - r * 0.8f, cy + r * 1.25f);
        body.quadTo(cx, cy - r * 0.6f, cx + r * 0.8f, cy + r * 1.25f);
        body.closePath();
        g.setColor(alpha(c, a));
        g.fill(body);
        g.fill(new Ellipse2D.Float(cx - r * 0.9f, cy + r * 1.05f, r * 1.8f, r * 0.45f)); // base
        g.fill(new Ellipse2D.Float(cx - r * 0.5f, cy - r * 0.55f, r, r));               // head
        g.setColor(alpha(Color.WHITE, a / 3));
        g.fill(new Ellipse2D.Float(cx - r * 0.3f, cy - r * 0.42f, r * 0.3f, r * 0.3f));  // shine
    }

    /** Draws text with extra letter spacing; returns the x after the last letter. */
    public static float drawTracked(Graphics2D g, String s, float x, float y, float spacing) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            g.drawString(String.valueOf(ch), x, y);
            x += fm.charWidth(ch) + spacing;
        }
        return x - spacing;
    }

    public static float trackedWidth(Graphics2D g, String s, float spacing) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        float w = -spacing;
        for (int i = 0; i < s.length(); i++) {
            w += fm.charWidth(s.charAt(i)) + spacing;
        }
        return Math.max(0, w);
    }

    /** Big neon title: glowing gradient text with a fading accent bar underneath. */
    public static javax.swing.JLabel neonHeading(String text, Color accent, float size) {
        javax.swing.JLabel heading = new javax.swing.JLabel(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyQuality(g2);
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = 4;
                int y = fm.getAscent() + 4;
                int w = fm.stringWidth(getText());
                float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 600.0));

                // soft glow: the text drawn several times, slightly offset and see-through
                g2.setColor(alpha(accent, (int) (30 + 35 * pulse)));
                for (int dx = -3; dx <= 3; dx += 3) {
                    for (int dy = -3; dy <= 3; dy += 3) {
                        g2.drawString(getText(), x + dx, y + dy);
                    }
                }

                // gradient text: white fading into the accent colour
                g2.setPaint(new GradientPaint(x, 0, Color.WHITE, x + w, 0, accent));
                g2.drawString(getText(), x, y);

                // accent bar under the title, fading out to the right
                g2.setPaint(new GradientPaint(x, 0, accent, x + w * 0.6f, 0, alpha(accent, 0)));
                g2.fillRoundRect(x, y + 10, (int) (w * 0.6f), 4, 4, 4);

                g2.dispose();
            }
        };
        heading.setFont(display(size));
        heading.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 18, 8));
        return heading;
    }

    /** Slim rounded neon scrollbar without arrow buttons. */
    public static void neonScrollBar(javax.swing.JScrollPane pane, Color accent) {
        javax.swing.JScrollBar bar = pane.getVerticalScrollBar();
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected javax.swing.JButton createDecreaseButton(int orientation) {
                return hiddenButton();
            }

            @Override
            protected javax.swing.JButton createIncreaseButton(int orientation) {
                return hiddenButton();
            }

            private javax.swing.JButton hiddenButton() {
                javax.swing.JButton button = new javax.swing.JButton();
                button.setPreferredSize(new java.awt.Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintTrack(java.awt.Graphics g, javax.swing.JComponent c, java.awt.Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyQuality(g2);
                g2.setColor(alpha(accent, 25));
                g2.fillRoundRect(r.x + 3, r.y + 4, r.width - 6, r.height - 8, 6, 6);
                g2.dispose();
            }

            @Override
            protected void paintThumb(java.awt.Graphics g, javax.swing.JComponent c, java.awt.Rectangle r) {
                if (r.isEmpty()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                applyQuality(g2);
                boolean active = isThumbRollover() || isDragging;
                g2.setColor(alpha(accent, active ? 230 : 150));
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
                g2.dispose();
            }
        });
        bar.setOpaque(false);
        bar.setPreferredSize(new java.awt.Dimension(12, 0));
    }

    /**
     * Top-3 podium (2nd | 1st | 3rd) with a glowing pawn, name and caption for each place.
     * {@code pawnColors} may be null; then every pawn takes its medal colour.
     */
    public static javax.swing.JComponent podium(String[] names, String[] captions, Color[] pawnColors) {
        javax.swing.JComponent podium = new javax.swing.JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyQuality(g2);
                int h = getHeight();
                int slotW = Math.min(getWidth() / 3, 300);
                int left = (getWidth() - slotW * 3) / 2;     // keeps the podium centred

                int[] rankAtSlot = {1, 0, 2};                // left = 2nd, middle = 1st, right = 3rd
                int[] blockH = {90, 66, 48};                 // block height for 1st, 2nd, 3rd
                Color[] medal = {
                        new Color(255, 210, 107),            // gold
                        new Color(200, 212, 235),            // silver
                        new Color(222, 150, 100)};           // bronze

                for (int slot = 0; slot < 3; slot++) {
                    int rank = rankAtSlot[slot];
                    if (rank >= names.length) continue;      // fewer than 3 players
                    Color c = medal[rank];
                    Color pawn = pawnColors == null ? c : pawnColors[rank];

                    int bx = left + slot * slotW + 14;
                    int bw = slotW - 28;
                    int by = h - blockH[rank];
                    float cx = bx + bw / 2f;

                    // podium block (taller than the panel, so only the top corners are round)
                    java.awt.Shape block = new java.awt.geom.RoundRectangle2D.Float(
                            bx, by, bw, blockH[rank] + 20, 18, 18);
                    g2.setPaint(new GradientPaint(0, by, alpha(c, 110), 0, h, alpha(c, 15)));
                    g2.fill(block);
                    g2.setColor(alpha(c, 220));
                    g2.setStroke(new java.awt.BasicStroke(2f));
                    g2.draw(block);

                    // big rank number on the block
                    g2.setFont(display(32f));
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    String number = String.valueOf(rank + 1);
                    g2.setColor(alpha(Color.WHITE, 210));
                    g2.drawString(number, cx - fm.stringWidth(number) / 2f, by + 6 + fm.getAscent());

                    // caption and name just above the block
                    g2.setFont(ui(13f, Font.PLAIN));
                    fm = g2.getFontMetrics();
                    g2.setColor(MUTED);
                    g2.drawString(captions[rank], cx - fm.stringWidth(captions[rank]) / 2f, by - 8);

                    g2.setFont(semibold(17f));
                    fm = g2.getFontMetrics();
                    g2.setColor(c);
                    g2.drawString(names[rank], cx - fm.stringWidth(names[rank]) / 2f, by - 26);

                    // glowing pawn above the name
                    float r = 15f;
                    float cy = by - 48 - r * 1.5f;
                    if (rank == 0) {
                        glow(g2, cx, cy, 70f, c, 70);
                    }
                    paintPawn(g2, cx, cy, r, pawn, 255);

                    // crown on the winner
                    if (rank == 0) {
                        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                        fm = g2.getFontMetrics();
                        g2.setColor(c);
                        g2.drawString("\u265B", cx - fm.stringWidth("\u265B") / 2f, cy - r * 0.55f - 6);
                    }
                }
                g2.dispose();
            }
        };
        podium.setOpaque(false);
        podium.setPreferredSize(new java.awt.Dimension(580, 200));
        return podium;
    }
    

    /**
     * Themed popup with a neon frame, glowing title, message and one or more neon buttons.
     * Returns the index of the button that was clicked, or -1 if the popup was closed with Esc.
     */
    public static int neonDialog(java.awt.Component parent, String title, String message,
                                 Color accent, String... buttons) {
        java.awt.Window owner = parent == null ? null : javax.swing.SwingUtilities.getWindowAncestor(parent);
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        int[] choice = {-1};

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(26, 24, 60), 0, h, new Color(13, 18, 42)));
                g2.fillRect(0, 0, w, h);
                glow(g2, w / 2f, 0, w * 0.6f, accent, 55);
                g2.setColor(alpha(accent, 220));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRect(1, 1, w - 3, h - 3);
                g2.dispose();
            }
        };
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 30, 16, 30));
        panel.add(neonHeading(title.toUpperCase(), accent, 24f), java.awt.BorderLayout.NORTH);

        // the message, wrapped at about 420 pixels
        javax.swing.JTextArea text = new javax.swing.JTextArea(message);
        text.setFont(ui(16f, Font.PLAIN));
        text.setForeground(TEXT);
        text.setOpaque(false);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setSize(new java.awt.Dimension(420, Short.MAX_VALUE));
        text.setPreferredSize(new java.awt.Dimension(420, text.getPreferredSize().height));
        panel.add(text, java.awt.BorderLayout.CENTER);

        // buttons: the first is the main action in the accent colour, the rest are violet
        javax.swing.JPanel row = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 14, 0));
        row.setOpaque(false);
        for (int i = 0; i < buttons.length; i++) {
            int index = i;
            NeonButton button = new NeonButton(buttons[i],
                    i == 0 ? accent : VIOLET,
                    i == 0 ? NeonButton.Glyph.PLAY : NeonButton.Glyph.BACK, null);
            button.setChevron(false);
            button.setFocusable(false);
            button.setPreferredSize(new java.awt.Dimension(200, 68));
            button.addActionListener(e -> {
                choice[0] = index;
                dialog.dispose();
            });
            row.add(button);
        }
        panel.add(row, java.awt.BorderLayout.SOUTH);

        // Esc closes, Enter picks the first button
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                javax.swing.KeyStroke.getKeyStroke("ESCAPE"), javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().registerKeyboardAction(e -> {
                    choice[0] = 0;
                    dialog.dispose();
                },
                javax.swing.KeyStroke.getKeyStroke("ENTER"), javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return choice[0];
    }

    /** Themed message with a single OK button. */
    public static void neonMessage(java.awt.Component parent, String title, String message, Color accent) {
        neonDialog(parent, title, message, accent, "OK");
    }

    /** Themed yes/cancel question; returns true only if the first button was clicked. */
    public static boolean neonConfirm(java.awt.Component parent, String title, String message,
                                      String yesText, Color accent) {
        return neonDialog(parent, title, message, accent, yesText, "CANCEL") == 0;
    }
}