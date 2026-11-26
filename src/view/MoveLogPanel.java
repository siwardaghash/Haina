package view;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import model.LogEntry;
import model.PlayerColor;

/**
 * Bonus: the move log next to the board. It lists every roll, move, capture, answer and
 * surprise of the game, newest on top, so the players can follow what happened (also during
 * bot turns). It only reads {@link model.Game#getLog()}; the board calls {@link #update}.
 */
public final class MoveLogPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color LAVENDER = new Color(198, 166, 255);
    private static final Color GOLD = new Color(255, 199, 92);
    private static final int GAP = 6;
    /** How long a new row glows after it appears. */
    private static final long FLASH_MILLIS = 1600;

    private final List<LogEntry> entries = new ArrayList<>();
    private final List<Long> seenAt = new ArrayList<>();
    /** Number of events, in a small neon pill. */
    private final JLabel count = new JLabel("0", SwingConstants.CENTER) {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            int h = 24;
            int y = (getHeight() - h) / 2;
            g2.setColor(Theme.alpha(LAVENDER, 35));
            g2.fillRoundRect(0, y, getWidth() - 1, h, h, h);
            g2.setColor(Theme.alpha(LAVENDER, 160));
            g2.drawRoundRect(0, y, getWidth() - 1, h, h, h);
            g2.dispose();
            super.paintComponent(g);
        }
    };
    private final Rows rows = new Rows();
    private final JScrollPane scroll;
    private final JPanel header = new JPanel(new BorderLayout(8, 0));

    // Bonus: the log can be collapsed to a thin strip, so the board gets the space back.
    private static final int OPEN_WIDTH = 300;
    private static final int CLOSED_WIDTH = 58;
    /** Remembered for the next game (rematch, new game) while the program runs. */
    private static boolean startCollapsed;
    private boolean collapsed;
    private float width = OPEN_WIDTH;
    /** Events that arrived while the log was collapsed; shown as a badge on the strip. */
    private int unseen;
    private boolean stripHovered;
    private final Timer slide = new Timer(15, e -> stepSlide());

    public MoveLogPanel() {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 14, 14, 10));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        header.add(Theme.neonHeading("MOVE LOG", LAVENDER, 20f), BorderLayout.WEST);
        count.setFont(Theme.semibold(12f));
        count.setForeground(LAVENDER);
        count.setToolTipText("Events so far");
        count.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JPanel countBox = new JPanel(new GridBagLayout());   // keeps the pill at its own height
        countBox.setOpaque(false);
        countBox.add(count);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(countBox);
        right.add(collapseButton());
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        scroll = new JScrollPane(rows);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        Theme.neonScrollBar(scroll, LAVENDER);
        add(scroll, BorderLayout.CENTER);

        // The collapsed strip opens the log again when clicked anywhere.
        java.awt.event.MouseAdapter strip = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (collapsed) setCollapsed(false); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { stripHovered = true; repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { stripHovered = false; repaint(); }
        };
        addMouseListener(strip);

        // L opens and closes the log from the keyboard.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('l'), "toggleLog");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('L'), "toggleLog");
        getActionMap().put("toggleLog", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { setCollapsed(!collapsed); }
        });

        collapsed = startCollapsed;
        width = collapsed ? CLOSED_WIDTH : OPEN_WIDTH;
        header.setVisible(!collapsed);
        scroll.setVisible(!collapsed);
        setToolTipText(collapsed ? "Show the move log (L)" : null);
        setCursor(collapsed ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(Math.round(width), 400); }

    /** Opens or closes the log with a short slide. */
    public void setCollapsed(boolean collapse) {
        if (collapse == collapsed) return;
        collapsed = collapse;
        startCollapsed = collapse;
        if (!collapse) unseen = 0;
        header.setVisible(false);          // the content comes back when the slide ends
        scroll.setVisible(false);
        setToolTipText(collapse ? "Show the move log (L)" : null);
        setCursor(collapse ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        slide.start();
    }

    private void stepSlide() {
        float target = collapsed ? CLOSED_WIDTH : OPEN_WIDTH;
        width += (target - width) * .35f;
        if (Math.abs(target - width) < 1.5f) {
            width = target;
            slide.stop();
            header.setVisible(!collapsed);
            scroll.setVisible(!collapsed);
        }
        revalidate();
        if (getParent() != null) getParent().revalidate();
        repaint();
    }

    /** Small round button with a chevron, in the header: closes the log. */
    private JButton collapseButton() {
        JButton button = new JButton() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                boolean hot = getModel().isRollover();
                int d = 26;
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;
                g2.setColor(Theme.alpha(LAVENDER, hot ? 70 : 30));
                g2.fillOval(x, y, d, d);
                g2.setColor(Theme.alpha(LAVENDER, hot ? 230 : 150));
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawOval(x, y, d, d);
                chevron(g2, x + d / 2f, y + d / 2f, true, LAVENDER);
                g2.dispose();
            }
        };
        button.setPreferredSize(new Dimension(30, 30));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Hide the move log (L)");
        button.addActionListener(e -> setCollapsed(true));
        return button;
    }

    /** A chevron: pointing right (close, towards the edge) or left (open). */
    private static void chevron(Graphics2D g2, float cx, float cy, boolean right, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float s = right ? 1 : -1;
        Path2D p = new Path2D.Float();
        p.moveTo(cx - 3 * s, cy - 6);
        p.lineTo(cx + 3 * s, cy);
        p.lineTo(cx - 3 * s, cy + 6);
        g2.draw(p);
    }

    /** Shows the game's log; only entries that are new since the last call start to glow. */
    public void update(List<LogEntry> log) {
        if (log.size() == entries.size()) return;
        long now = System.currentTimeMillis();
        if (log.size() < entries.size()) { entries.clear(); seenAt.clear(); }
        if (collapsed) unseen += log.size() - entries.size();
        for (int i = entries.size(); i < log.size(); i++) {
            entries.add(log.get(i));
            seenAt.add(entries.size() == 1 && log.size() > 1 ? 0L : now);
        }
        count.setText(String.valueOf(entries.size()));
        rows.revalidate();
        rows.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));   // newest on top
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQuality(g2);
        RoundRectangle2D card = new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
        g2.setColor(Theme.GLASS);
        g2.fill(card);
        g2.setColor(Theme.alpha(LAVENDER, collapsed && stripHovered ? 170 : 70));
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(card);
        if (collapsed && width < OPEN_WIDTH / 2f) paintStrip(g2);
        g2.dispose();
    }

    /** The collapsed log: an open button, "MOVE LOG" written upwards, and a badge for new events. */
    private void paintStrip(Graphics2D g2) {
        float cx = getWidth() / 2f;
        int d = 32;
        float by = 20;
        g2.setColor(Theme.alpha(LAVENDER, stripHovered ? 75 : 35));
        g2.fill(new java.awt.geom.Ellipse2D.Float(cx - d / 2f, by, d, d));
        g2.setColor(Theme.alpha(LAVENDER, stripHovered ? 230 : 160));
        g2.setStroke(new BasicStroke(1.3f));
        g2.draw(new java.awt.geom.Ellipse2D.Float(cx - d / 2f, by, d, d));
        chevron(g2, cx, by + d / 2f, false, LAVENDER);

        if (unseen > 0) {                    // new events while the log was closed
            String n = unseen > 99 ? "99+" : String.valueOf(unseen);
            g2.setFont(Theme.semibold(11f));
            FontMetrics fm = g2.getFontMetrics();
            float bw = Math.max(20, fm.stringWidth(n) + 10);
            float bx = cx + 6;
            float bTop = by - 6;
            float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0));
            Theme.glow(g2, bx + bw / 2f, bTop + 10, 18, GOLD, (int) (60 + 60 * pulse));
            g2.setColor(GOLD);
            g2.fill(new RoundRectangle2D.Float(bx, bTop, bw, 20, 20, 20));
            g2.setColor(new Color(30, 22, 6));
            g2.drawString(n, bx + (bw - fm.stringWidth(n)) / 2f, bTop + 14.5f);
        }

        // "MOVE LOG" written from bottom to top
        Graphics2D t = (Graphics2D) g2.create();
        t.setFont(Theme.display(15f));
        String label = "MOVE LOG";
        float tw = Theme.trackedWidth(t, label, 3);
        t.translate(cx + 6, by + d + 30 + tw);
        t.rotate(-Math.PI / 2);
        t.setColor(Theme.alpha(LAVENDER, 90));
        Theme.drawTracked(t, label, 0, 0, 3);
        t.setColor(Theme.mix(Theme.TEXT, LAVENDER, .4f));
        Theme.drawTracked(t, label, 0, -1, 3);
        t.dispose();

        // a dot for each of the last turns, in the players' colours
        float y = by + d + 30 + tw + 24;
        PlayerColor last = null;
        int dots = 0;
        for (int i = entries.size() - 1; i >= 0 && dots < 6 && y < getHeight() - 24; i--) {
            PlayerColor c = entries.get(i).getColor();
            if (c == last) continue;
            last = c;
            float a = 1f - dots * .14f;
            g2.setColor(Theme.alpha(playerColor(c), (int) (255 * a)));
            g2.fill(new java.awt.geom.Ellipse2D.Float(cx - 5, y, 10, 10));
            y += 18;
            dots++;
        }
    }

    /** Colour of a row's stripe and icon. */
    private static Color kindColor(LogEntry.Kind kind) {
        switch (kind) {
            case CAPTURE:
            case WRONG:     return Theme.RED;
            case CORRECT:   return Theme.GREEN;
            case FINISH:
            case WIN:       return GOLD;
            case SURPRISE:  return new Color(255, 122, 214);
            case TURN_LOST: return new Color(255, 150, 90);
            case MOVE:      return Theme.TEXT;
            default:        return LAVENDER;
        }
    }

    /** One turn: the events in a row of the same player, in the order they happened. */
    private static final class Turn {
        final LogEntry first;
        final List<Integer> lines = new ArrayList<>();
        Turn(LogEntry first) { this.first = first; }
        int height() { return HEAD + lines.size() * LINE + 10; }
    }

    private static final int HEAD = 30;
    private static final int LINE = 23;

    /** Groups the entries into turns, newest turn first. */
    private List<Turn> turns() {
        List<Turn> turns = new ArrayList<>();
        Turn turn = null;
        for (int i = 0; i < entries.size(); i++) {
            LogEntry e = entries.get(i);
            if (turn == null || turn.first.getColor() != e.getColor()) {
                turn = new Turn(e);
                turns.add(0, turn);
            }
            turn.lines.add(i);
        }
        return turns;
    }

    /** The painted list of turns; it is as tall as all of them, so the scroll pane can scroll it. */
    private final class Rows extends JComponent implements Scrollable {
        private static final long serialVersionUID = 1L;

        Rows() { setOpaque(false); }

        @Override
        public Dimension getPreferredSize() {
            int h = 0;
            for (Turn t : turns()) h += t.height() + GAP;
            return new Dimension(260, Math.max(60, h));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            int w = getWidth() - 8;
            if (entries.isEmpty()) {
                g2.setFont(Theme.ui(14f, Font.PLAIN));
                g2.setColor(Theme.MUTED);
                g2.drawString("No moves yet. Roll the dice!", 10, 30);
                g2.dispose();
                return;
            }
            Rectangle clip = g2.getClipBounds();
            long now = System.currentTimeMillis();
            int y = 0;
            for (Turn t : turns()) {
                int h = t.height();
                if (clip == null || (y <= clip.y + clip.height && y + h >= clip.y)) paintTurn(g2, t, now, y, w);
                y += h + GAP;
            }
            g2.dispose();
        }

        private void paintTurn(Graphics2D g2, Turn t, long now, int y, int w) {
            Color player = playerColor(t.first.getColor());
            long newest = now - seenAt.get(t.lines.get(t.lines.size() - 1));
            float flash = newest < FLASH_MILLIS ? 1f - newest / (float) FLASH_MILLIS : 0f;
            int h = t.height();

            RoundRectangle2D box = new RoundRectangle2D.Float(2, y + 1, w - 2, h - 2, 18, 18);
            g2.setColor(Theme.mix(new Color(20, 26, 54), player, .07f + .12f * flash));
            g2.fill(box);
            g2.setColor(Theme.alpha(player, (int) (60 + 150 * flash)));
            g2.setStroke(new BasicStroke(1f + flash));
            g2.draw(box);
            g2.setColor(player);
            g2.fill(new RoundRectangle2D.Float(2, y + 10, 4, h - 20, 4, 4));

            // header: a pawn in the player's colour, the name and the time
            Theme.paintPawn(g2, 24, y + 17, 7, player, 255);
            g2.setFont(Theme.semibold(14f));
            g2.setColor(player);
            g2.drawString(t.first.getPlayer().toUpperCase(), 38, y + 22);
            long sec = t.first.getSecondsIntoGame();
            String time = String.format("%02d:%02d", sec / 60, sec % 60);
            g2.setFont(Theme.mono(11.5f));
            g2.setColor(Theme.MUTED);
            g2.drawString(time, w - 12 - g2.getFontMetrics().stringWidth(time), y + 22);

            // one line per event, with its icon
            g2.setFont(Theme.ui(12.5f, Font.PLAIN));
            FontMetrics fm = g2.getFontMetrics();
            int ly = y + HEAD;
            for (int index : t.lines) {
                LogEntry e = entries.get(index);
                Color accent = kindColor(e.getKind());
                long age = now - seenAt.get(index);
                if (age < FLASH_MILLIS) {          // a new line glows for a moment
                    g2.setColor(Theme.alpha(accent, (int) (60 * (1f - age / (float) FLASH_MILLIS))));
                    g2.fillRoundRect(14, ly + 1, w - 26, LINE - 2, 10, 10);
                }
                paintIcon(g2, e.getKind(), 26, ly + LINE / 2, accent);
                boolean plain = e.getKind() == LogEntry.Kind.MOVE || e.getKind() == LogEntry.Kind.ROLL;
                g2.setColor(plain ? Theme.alpha(Theme.TEXT, 210) : Theme.mix(Theme.TEXT, accent, .55f));
                g2.drawString(fit(fm, e.getText(), w - 56), 42, ly + 16);
                ly += LINE;
            }
        }

        private String fit(FontMetrics fm, String text, int width) {
            if (fm.stringWidth(text) <= width) return text;
            String dots = "…";
            int end = text.length();
            while (end > 0 && fm.stringWidth(text.substring(0, end) + dots) > width) end--;
            return text.substring(0, end) + dots;
        }

        /** Small line icons, drawn with shapes so they look the same on every computer. */
        private void paintIcon(Graphics2D g2, LogEntry.Kind kind, int cx, int cy, Color c) {
            g2 = (Graphics2D) g2.create();
            g2.translate(cx, cy);
            g2.scale(.75, .75);
            cx = 0;
            cy = 0;
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case ROLL:          // a die with three dots
                    g2.drawRoundRect(cx - 7, cy - 7, 14, 14, 5, 5);
                    g2.fillOval(cx - 5, cy - 5, 3, 3);
                    g2.fillOval(cx - 1, cy - 1, 3, 3);
                    g2.fillOval(cx + 3, cy + 3, 3, 3);
                    break;
                case MOVE:          // an arrow to the right
                    g2.draw(new Line2D.Float(cx - 7, cy, cx + 6, cy));
                    g2.draw(new Line2D.Float(cx + 1, cy - 5, cx + 6, cy));
                    g2.draw(new Line2D.Float(cx + 1, cy + 5, cx + 6, cy));
                    break;
                case CAPTURE:       // two crossed swords, as an X with guards
                    g2.draw(new Line2D.Float(cx - 7, cy - 7, cx + 7, cy + 7));
                    g2.draw(new Line2D.Float(cx + 7, cy - 7, cx - 7, cy + 7));
                    g2.draw(new Line2D.Float(cx - 7, cy + 2, cx - 2, cy + 7));
                    g2.draw(new Line2D.Float(cx + 7, cy + 2, cx + 2, cy + 7));
                    break;
                case CORRECT:       // a tick
                    g2.draw(new Line2D.Float(cx - 7, cy, cx - 2, cy + 5));
                    g2.draw(new Line2D.Float(cx - 2, cy + 5, cx + 7, cy - 6));
                    break;
                case WRONG:         // an X
                    g2.draw(new Line2D.Float(cx - 6, cy - 6, cx + 6, cy + 6));
                    g2.draw(new Line2D.Float(cx + 6, cy - 6, cx - 6, cy + 6));
                    break;
                case SURPRISE:      // an exclamation mark
                    g2.draw(new Line2D.Float(cx, cy - 7, cx, cy + 2));
                    g2.fillOval(cx - 2, cy + 5, 4, 4);
                    break;
                case TURN_LOST:     // pause bars
                    g2.draw(new Line2D.Float(cx - 3, cy - 6, cx - 3, cy + 6));
                    g2.draw(new Line2D.Float(cx + 3, cy - 6, cx + 3, cy + 6));
                    break;
                case FINISH:        // a flag
                    g2.draw(new Line2D.Float(cx - 5, cy - 8, cx - 5, cy + 8));
                    Path2D flag = new Path2D.Float();
                    flag.moveTo(cx - 5, cy - 8);
                    flag.lineTo(cx + 7, cy - 4);
                    flag.lineTo(cx - 5, cy);
                    flag.closePath();
                    g2.fill(flag);
                    break;
                default:            // WIN: a crown
                    Path2D crown = new Path2D.Float();
                    crown.moveTo(cx - 8, cy + 5);
                    crown.lineTo(cx - 8, cy - 5);
                    crown.lineTo(cx - 3, cy);
                    crown.lineTo(cx, cy - 7);
                    crown.lineTo(cx + 3, cy);
                    crown.lineTo(cx + 8, cy - 5);
                    crown.lineTo(cx + 8, cy + 5);
                    crown.closePath();
                    g2.fill(crown);
                    break;
            }
            g2.dispose();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - LINE; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** The same player colours as the board's pieces. */
    private static Color playerColor(PlayerColor color) {
        switch (color) {
            case RED:    return new Color(241, 91, 105);
            case GREEN:  return new Color(83, 204, 146);
            case YELLOW: return new Color(245, 205, 79);
            default:     return new Color(94, 159, 240);
        }
    }
}