package view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import model.Achievement;
import model.TrophyStore;

/**
 * Bonus: the Trophy Cabinet window from the Game History screen. Pick a player to see all
 * trophies: earned ones glow with how many times and since when, locked ones show how to get them.
 */
final class TrophyCabinet {
    private static final Color GOLD = TrophyArt.LEGENDARY;
    private static final Color LAVENDER = TrophyArt.COMMON;

    private TrophyCabinet() { }

    /** Opens the cabinet; {@code players} are all names from the history. */
    static void show(Component parent, List<String> players, Map<String, TrophyStore.Collection> collections) {
        // every player, the one with the most trophies first (then A-Z)
        List<String> names = new ArrayList<>(players);
        for (String name : collections.keySet()) if (!names.contains(name)) names.add(name);
        names.sort((a, b) -> {
            int byTrophies = distinct(collections, b) - distinct(collections, a);
            return byTrophies != 0 ? byTrophies : a.compareToIgnoreCase(b);
        });
        Map<String, TrophyStore.Collection> all = new LinkedHashMap<>();
        for (String name : names) all.put(name, collections.getOrDefault(name, new TrophyStore.Collection()));

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Trophy Cabinet",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        Theme.transparentCorners(dialog);

        JPanel card = new JPanel(new BorderLayout(20, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);
                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 32, 32);
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 22, 58), 0, h, new Color(12, 14, 34)));
                g2.fill(shape);
                Shape old = g2.getClip();   // keep Swing's repaint area, only narrow it to the card
                g2.clip(shape);
                Theme.glow(g2, w / 2f, 0, w * .5f, GOLD, 45);
                Theme.glow(g2, w, h, w * .35f, LAVENDER, 25);
                g2.setClip(old);
                g2.setColor(Theme.alpha(GOLD, 150));
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(shape);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(24, 30, 20, 30));
        card.add(Theme.neonHeading("TROPHY CABINET", GOLD, 32f), BorderLayout.NORTH);

        Shelf shelf = new Shelf();
        card.add(shelf, BorderLayout.CENTER);

        // left: search box and the list of players (scrolls, so it works for any number of players)
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setOpaque(false);
        list.setBackground(new Color(0, 0, 0, 0));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(PlayerRow.HEIGHT);
        list.setCellRenderer(new PlayerRow(all));
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        list.addListSelectionListener(e -> {
            String name = list.getSelectedValue();
            if (!e.getValueIsAdjusting() && name != null) shelf.show(name, all.get(name));
        });
        list.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                list.putClientProperty("hover", i >= 0 && list.getCellBounds(i, i).contains(e.getPoint()) ? i : -1);
                list.repaint();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                list.putClientProperty("hover", -1);
                list.repaint();
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(PlayerRow.HEIGHT / 2);
        Theme.neonScrollBar(scroll, LAVENDER);

        JTextField search = new JTextField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {         // placeholder text
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.applyQuality(g2);
                    g2.setFont(Theme.ui(14f, Font.PLAIN));
                    g2.setColor(Theme.MUTED);
                    Insets in = getInsets();
                    g2.drawString("Search player…", in.left, getHeight() / 2f + g2.getFontMetrics().getAscent() / 2f - 2);
                    g2.dispose();
                }
            }
        };
        Theme.styleNeonField(search, LAVENDER);
        search.setFont(Theme.ui(14f, Font.PLAIN));
        JLabel count = new JLabel();
        count.setFont(Theme.semibold(11.5f));
        count.setForeground(Theme.MUTED);
        Runnable filter = () -> {
            String keep = list.getSelectedValue();
            String text = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
            model.clear();
            for (String name : all.keySet())
                if (name.toLowerCase(java.util.Locale.ROOT).contains(text)) model.addElement(name);
            if (keep != null && model.contains(keep)) list.setSelectedValue(keep, true);
            else if (!model.isEmpty()) list.setSelectedIndex(0);
            count.setText(text.isEmpty() ? all.size() + (all.size() == 1 ? " PLAYER" : " PLAYERS")
                    : model.size() + " OF " + all.size() + " PLAYERS");
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filter.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filter.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filter.run(); }
        });

        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(250, 10));
        JPanel sideTop = new JPanel(new BorderLayout(0, 8));
        sideTop.setOpaque(false);
        sideTop.add(search, BorderLayout.NORTH);
        sideTop.add(count, BorderLayout.SOUTH);
        side.add(sideTop, BorderLayout.NORTH);
        side.add(scroll, BorderLayout.CENTER);
        card.add(side, BorderLayout.WEST);

        filter.run();                              // fills the list and selects the first player
        if (all.isEmpty()) shelf.show(null, null);

        NeonButton close = new NeonButton("CLOSE", LAVENDER, NeonButton.Glyph.BACK, null);
        close.setChevron(false);
        close.setFocusable(false);
        close.setPreferredSize(new Dimension(200, 62));
        close.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.setOpaque(false);
        bottom.add(close);
        card.add(bottom, BorderLayout.SOUTH);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // repaint while open, so the emblems shine and the hover follows the mouse
        Timer timer = new Timer(40, e -> shelf.repaint());
        timer.start();
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { timer.stop(); }
        });

        dialog.setContentPane(card);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static int distinct(Map<String, TrophyStore.Collection> collections, String name) {
        TrophyStore.Collection c = collections.get(name);
        return c == null ? 0 : c.distinct();
    }

    /** One row of the player list: place, name, trophy count and a small progress bar. */
    private static final class PlayerRow extends JComponent implements ListCellRenderer<String> {
        private static final long serialVersionUID = 1L;
        static final int HEIGHT = 56;
        private final Map<String, TrophyStore.Collection> all;
        private final List<String> order;
        private String name;
        private boolean selected;
        private boolean hover;

        PlayerRow(Map<String, TrophyStore.Collection> all) {
            this.all = all;
            this.order = new ArrayList<>(all.keySet());
            setPreferredSize(new Dimension(230, HEIGHT));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            name = value;
            selected = isSelected;
            Object h = list.getClientProperty("hover");
            hover = h instanceof Integer && (Integer) h == index;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            int w = getWidth() - 8;                // room for the scroll bar
            int h = HEIGHT - 6;
            Color c = selected ? GOLD : LAVENDER;
            g2.setColor(Theme.alpha(c, selected ? 45 : hover ? 28 : 10));
            g2.fillRoundRect(1, 1, w - 2, h, 16, 16);
            g2.setColor(Theme.alpha(c, selected ? 220 : hover ? 140 : 45));
            g2.setStroke(new BasicStroke(selected ? 1.6f : 1f));
            g2.drawRoundRect(1, 1, w - 2, h, 16, 16);

            // place in the ranking (by trophies)
            int place = order.indexOf(name) + 1;
            g2.setFont(Theme.mono(12f));
            g2.setColor(place <= 3 ? GOLD : Theme.MUTED);
            g2.drawString(String.valueOf(place), 12, 23);

            // name, cut with … when too long
            g2.setFont(Theme.semibold(14f));
            FontMetrics fm = g2.getFontMetrics();
            String shown = name;
            int maxName = w - 110;
            while (fm.stringWidth(shown) > maxName && shown.length() > 1) shown = shown.substring(0, shown.length() - 1);
            if (!shown.equals(name)) shown = shown + "…";
            g2.setColor(selected ? Theme.TEXT : Theme.alpha(Theme.TEXT, 210));
            g2.drawString(shown, 40, 23);

            // trophies x / 11 and a small bar
            TrophyStore.Collection mine = all.get(name);
            int have = mine == null ? 0 : mine.distinct();
            int total = Achievement.values().length;
            String n = have + "/" + total;
            g2.setFont(Theme.mono(11.5f));
            FontMetrics nm = g2.getFontMetrics();
            int bw = nm.stringWidth(n) + 12;
            g2.setColor(Theme.alpha(c, 45));
            g2.fillRoundRect(w - bw - 10, 9, bw, 20, 20, 20);
            g2.setColor(have > 0 ? c : Theme.MUTED);
            g2.drawString(n, w - bw - 4, 23);
            g2.setColor(new Color(36, 32, 64));
            g2.fillRoundRect(40, 35, w - 54, 5, 5, 5);
            if (have > 0) {
                g2.setPaint(new GradientPaint(40, 0, LAVENDER, w - 14, 0, GOLD));
                g2.fillRoundRect(40, 35, Math.max(5, (w - 54) * have / total), 5, 5, 5);
            }
            g2.dispose();
        }
    }

    /** The shelf: progress at the top, then a grid of all trophies. */
    private static final class Shelf extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final int COLS = 4;
        private static final int CELL_W = 206;
        private static final int CELL_H = 148;
        private static final int GAP = 14;
        private static final int TOP = 74;
        private String player;
        private TrophyStore.Collection collection;
        private Point mouse = new Point(-1, -1);
        private long shownAt;

        Shelf() {
            int rows = (Achievement.values().length + COLS - 1) / COLS;
            setPreferredSize(new Dimension(COLS * CELL_W + (COLS - 1) * GAP, TOP + rows * CELL_H + (rows - 1) * GAP));
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { mouse = e.getPoint(); }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) { mouse = new Point(-1, -1); }
            });
        }

        void show(String player, TrophyStore.Collection collection) {
            this.player = player;
            this.collection = collection;
            shownAt = System.currentTimeMillis();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            int w = getWidth();
            if (collection == null) {
                g2.setFont(Theme.display(22f));
                g2.setColor(Theme.TEXT);
                String t = "NO TROPHIES YET";
                g2.drawString(t, (w - g2.getFontMetrics().stringWidth(t)) / 2f, getHeight() / 2f - 10);
                g2.setFont(Theme.ui(15f, Font.PLAIN));
                g2.setColor(Theme.MUTED);
                String s = "Finish a game to start your collection!";
                g2.drawString(s, (w - g2.getFontMetrics().stringWidth(s)) / 2f, getHeight() / 2f + 20);
                g2.dispose();
                return;
            }
            paintProgress(g2, w);
            Achievement[] all = Achievement.values();
            long now = System.currentTimeMillis();
            for (int i = 0; i < all.length; i++) {
                int x = (i % COLS) * (CELL_W + GAP);
                int y = TOP + (i / COLS) * (CELL_H + GAP);
                float appear = Theme.easeOut(Theme.clamp01((now - shownAt - i * 35) / 260f));
                Graphics2D cell = (Graphics2D) g2.create();
                cell.translate(0, (1 - appear) * 14);
                cell.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(.01f, appear)));
                paintCell(cell, all[i], x, y, new Rectangle(x, y, CELL_W, CELL_H).contains(mouse), now);
                cell.dispose();
            }
            g2.dispose();
        }

        private void paintProgress(Graphics2D g2, int w) {
            int total = Achievement.values().length;
            int have = collection.distinct();
            float k = have / (float) total;
            g2.setFont(Theme.display(20f));
            g2.setColor(Theme.TEXT);
            g2.drawString(player.toUpperCase(), 2, 22);
            g2.setFont(Theme.semibold(13f));
            String line = have + " / " + total + " TROPHIES  ·  " + Math.round(k * 100) + "%  ·  "
                    + collection.total() + " EARNED IN TOTAL";
            g2.setColor(Theme.MUTED);
            g2.drawString(line, w - g2.getFontMetrics().stringWidth(line) - 2, 20);
            // the bar, with a gradient from lavender to gold
            int by = 36;
            int bh = 14;
            g2.setColor(new Color(36, 32, 64));
            g2.fillRoundRect(0, by, w, bh, bh, bh);
            int fw = Math.round(w * k * Theme.easeOut(Theme.clamp01((System.currentTimeMillis() - shownAt) / 700f)));
            if (fw > 0) {
                Theme.glow(g2, fw, by + bh / 2f, 26, GOLD, 70);
                g2.setPaint(new GradientPaint(0, 0, LAVENDER, Math.max(1, w), 0, GOLD));
                g2.fillRoundRect(0, by, Math.max(bh, fw), bh, bh, bh);
            }
            Achievement rarest = collection.rarest();
            if (rarest != null) {
                g2.setFont(Theme.ui(12.5f, Font.PLAIN));
                g2.setColor(Theme.MUTED);
                String r = "Rarest: ";
                g2.drawString(r, 2, by + bh + 18);
                g2.setFont(Theme.semibold(12.5f));
                g2.setColor(TrophyArt.color(rarest));
                g2.drawString(rarest.getTitle() + " (" + rarest.getTier().name() + ")",
                        2 + g2.getFontMetrics().stringWidth(r) + 4, by + bh + 18);
            }
        }

        private void paintCell(Graphics2D g2, Achievement a, int x, int y, boolean hover, long now) {
            int count = collection.count(a);
            boolean earned = count > 0;
            Color c = earned ? TrophyArt.color(a) : new Color(92, 86, 136);
            RoundRectangle2D box = new RoundRectangle2D.Float(x + 1, y + 1, CELL_W - 2, CELL_H - 2, 22, 22);
            g2.setColor(earned ? Theme.mix(new Color(24, 20, 48), c, hover ? .16f : .08f) : new Color(20, 18, 40, 220));
            g2.fill(box);
            g2.setColor(Theme.alpha(c, earned ? (hover ? 230 : 140) : (hover ? 150 : 70)));
            g2.setStroke(new BasicStroke(hover ? 1.8f : 1.2f));
            g2.draw(box);

            // tier tag in the corner
            g2.setFont(Theme.semibold(10f));
            String tier = a.getTier().name();
            float tw = Theme.trackedWidth(g2, tier, 1.5f);
            g2.setColor(Theme.alpha(c, earned ? 220 : 120));
            Theme.drawTracked(g2, tier, x + CELL_W - 12 - tw, y + 20, 1.5f);

            float shine = earned && hover ? (now % 1400) / 1000f : -1;
            TrophyArt.paintEmblem(g2, a, x + CELL_W / 2f, y + 44, hover ? 31 : 29, earned, shine <= 1 ? shine : -1);

            g2.setFont(Theme.semibold(15f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(earned ? Theme.TEXT : Theme.alpha(Theme.TEXT, 150));
            g2.drawString(a.getTitle(), x + (CELL_W - fm.stringWidth(a.getTitle())) / 2f, y + 96);

            if (earned) {
                String times = "×" + count + "  ·  since " + collection.firstDate(a);
                g2.setFont(Theme.ui(12.5f, Font.PLAIN));
                fm = g2.getFontMetrics();
                g2.setColor(Theme.mix(Theme.TEXT, c, .5f));
                g2.drawString(times, x + (CELL_W - fm.stringWidth(times)) / 2f, y + 118);
                if (hover) {
                    g2.setFont(Theme.ui(11.5f, Font.PLAIN));
                    g2.setColor(Theme.MUTED);
                    drawWrapped(g2, a.getDescription(), x + CELL_W / 2f, y + 136, CELL_W - 24, 1);
                }
            } else {
                g2.setFont(Theme.ui(12f, Font.PLAIN));
                g2.setColor(Theme.MUTED);
                drawWrapped(g2, a.getDescription(), x + CELL_W / 2f, y + 116, CELL_W - 24, 2);
            }
        }

        /** Centred text, wrapped on words, at most {@code maxLines} lines. */
        private static void drawWrapped(Graphics2D g2, String text, float cx, float y, int width, int maxLines) {
            FontMetrics fm = g2.getFontMetrics();
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                String next = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(next) > width && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else line = new StringBuilder(next);
            }
            lines.add(line.toString());
            for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
                String s = lines.get(i);
                if (i == maxLines - 1 && lines.size() > maxLines) s = s + "…";
                g2.drawString(s, cx - fm.stringWidth(s) / 2f, y + i * (fm.getHeight() - 1));
            }
        }
    }
}