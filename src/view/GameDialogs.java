package view;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import model.Game;
import model.Question;

/** Question, answer-result and surprise windows, drawn in the neon Code Quest style. */
final class GameDialogs {
    static final Color EASY = new Color(91, 218, 145);
    static final Color MEDIUM = new Color(255, 205, 92);
    static final Color HARD = new Color(255, 106, 128);
    static final Color SURPRISE = new Color(255, 122, 214);
    private static final Color LAVENDER = new Color(198, 166, 255);
    private static final int CARD_WIDTH = 560;

    private GameDialogs() { }

    static Color levelColor(String level) {
        return "easy".equals(level) ? EASY : "medium".equals(level) ? MEDIUM : HARD;
    }

    /** Shows the question and waits until one of the four answers is chosen (FR-24: no time limit). */
    static int askQuestion(Component parent, Question q) {
        Color accent = levelColor(q.getLevel());
        int steps = "easy".equals(q.getLevel()) ? 1 : "medium".equals(q.getLevel()) ? 2 : 3;
        JDialog dialog = dialog(parent);
        final int[] chosen = {-1};

        NeonCard card = new NeonCard(accent);
        card.setBorder(BorderFactory.createEmptyBorder(26, 30, 20, 30));

        // header: level badge, title and what is at stake, the question id on the right
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.add(new LevelBadge(accent, steps), BorderLayout.WEST);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = Theme.neonHeading(q.getLevel().toUpperCase() + " QUESTION", accent, 26f);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        JPanel stakes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        stakes.setOpaque(false);
        stakes.setAlignmentX(Component.LEFT_ALIGNMENT);
        stakes.add(new StakePill(true, steps));
        stakes.add(Box.createHorizontalStrut(8));
        stakes.add(new StakePill(false, steps));
        titles.add(stakes);
        header.add(titles, BorderLayout.CENTER);
        JLabel id = new JLabel(q.getId());
        id.setFont(Theme.mono(13f));
        id.setForeground(Theme.MUTED);
        id.setVerticalAlignment(SwingConstants.TOP);
        header.add(id, BorderLayout.EAST);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(18));

        // the question itself
        card.add(text(q.getText(), Theme.semibold(20f), Theme.TEXT, CARD_WIDTH));
        card.add(Box.createVerticalStrut(16));

        // four answer cards
        String[] answers = q.getAnswers();
        for (int i = 0; i < answers.length; i++) {
            final int index = i;
            AnswerCard answer = new AnswerCard((char) ('A' + i), answers[i], accent, () -> {
                chosen[0] = index;
                dialog.dispose();
            });
            card.add(answer);
            card.add(Box.createVerticalStrut(10));
            // A-D and 1-4 choose an answer from the keyboard
            for (int key : new int[]{KeyEvent.VK_A + i, KeyEvent.VK_1 + i, KeyEvent.VK_NUMPAD1 + i}) {
                dialog.getRootPane().registerKeyboardAction(e -> answer.choose(),
                        KeyStroke.getKeyStroke(key, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            }
        }

        JLabel hint = new JLabel("Click an answer, or press A–D / 1–4", SwingConstants.CENTER);
        hint.setFont(Theme.ui(12.5f, Font.PLAIN));
        hint.setForeground(Theme.MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setMaximumSize(new Dimension(CARD_WIDTH, 24));
        hint.setPreferredSize(new Dimension(CARD_WIDTH, 24));
        card.add(hint);

        show(dialog, card, parent);
        return chosen[0];
    }

    /** After an answer: a green or red card with the model's message (the correct answer when wrong, FR-26). */
    static void showAnswerResult(Component parent, boolean correct, String message) {
        Color accent = correct ? EASY : HARD;
        result(parent, accent, correct ? "CORRECT!" : "WRONG ANSWER", message,
                new ResultIcon(correct ? ResultIcon.CHECK : ResultIcon.CROSS, null, 0, accent));
    }

    static void showSurprise(Component parent, Game.SurpriseEvent event, String text) {
        result(parent, SURPRISE, "SURPRISE!", text, new ResultIcon(ResultIcon.SURPRISE, event, 0, SURPRISE));
    }

    /** A message card; a heading like "DIE: 4" shows that die face as its picture. */
    static void info(Component parent, Color accent, String heading, String text, Game.SurpriseEvent icon) {
        ResultIcon picture;
        if (icon != null) picture = new ResultIcon(ResultIcon.SURPRISE, icon, 0, accent);
        else if (heading.startsWith("DIE: ")) {
            int face = 0;
            try { face = Integer.parseInt(heading.substring(5).trim()); } catch (NumberFormatException ignored) { }
            picture = new ResultIcon(ResultIcon.DIE, null, face, accent);
        } else picture = new ResultIcon(ResultIcon.SPARK, null, 0, accent);
        result(parent, accent, heading, text, picture);
    }

    /** Centred card: glowing picture, heading, message and one CONTINUE button (Enter, Space or Esc). */
    private static void result(Component parent, Color accent, String heading, String text, ResultIcon picture) {
        JDialog dialog = dialog(parent);
        NeonCard card = new NeonCard(accent);
        card.setBorder(BorderFactory.createEmptyBorder(26, 34, 24, 34));

        picture.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(picture);
        card.add(Box.createVerticalStrut(6));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titleRow.setOpaque(false);
        titleRow.add(Theme.neonHeading(heading, accent, 28f));
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(10));

        JLabel message = new JLabel("<html><div style='text-align:center;width:400px'>"
                + escape(text).replace("\n", "<br>") + "</div></html>", SwingConstants.CENTER);
        message.setFont(Theme.ui(16f, Font.PLAIN));
        message.setForeground(Theme.TEXT);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(message);
        card.add(Box.createVerticalStrut(20));

        NeonButton ok = new NeonButton("CONTINUE", accent, NeonButton.Glyph.PLAY, null);
        ok.setChevron(false);
        ok.setFocusable(false);
        ok.setPreferredSize(new Dimension(230, 64));
        ok.setMaximumSize(new Dimension(230, 64));
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.addActionListener(e -> dialog.dispose());
        card.add(ok);
        for (String key : new String[]{"ENTER", "SPACE", "ESCAPE"}) {
            dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                    KeyStroke.getKeyStroke(key), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        show(dialog, card, parent);
    }

    // ------------------------------------------------------------------ building blocks
    private static JDialog dialog(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);  // the player must choose
        Theme.transparentCorners(dialog);
        return dialog;
    }

    /** Packs and centres the window, then fades it in when the computer supports it. */
    private static void show(JDialog dialog, JPanel card, Component parent) {
        dialog.setContentPane(card);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        boolean fade = dialog.getGraphicsConfiguration().getDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        if (fade) {
            dialog.setOpacity(0f);
            long start = System.currentTimeMillis();
            Timer timer = new Timer(15, null);
            timer.addActionListener(e -> {
                float t = Math.min(1f, (System.currentTimeMillis() - start) / 180f);
                try { dialog.setOpacity(Theme.easeOut(t)); } catch (RuntimeException ignored) { t = 1f; }
                if (t >= 1f) timer.stop();
            });
            timer.start();
        }
        dialog.setVisible(true);
    }

    private static JComponent text(String text, Font font, Color color, int width) {
        JTextArea area = new JTextArea(text);
        area.setFont(font);
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setSize(new Dimension(width, Short.MAX_VALUE));   // lets the wrapped height be measured
        area.setPreferredSize(new Dimension(width, area.getPreferredSize().height));
        area.setMaximumSize(area.getPreferredSize());
        return area;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** The dark glass card with a soft glow and a neon line at the top, in the accent colour. */
    private static final class NeonCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color accent;

        NeonCard(Color accent) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 30, 30);
            g2.setPaint(new GradientPaint(0, 0, new Color(30, 22, 58), 0, h, new Color(12, 14, 34)));
            g2.fill(shape);
            g2.setClip(shape);
            Theme.glow(g2, w / 2f, 0, w * .55f, accent, 60);
            Theme.glow(g2, w, h, w * .35f, LAVENDER, 22);
            g2.setClip(null);
            // neon line at the top, fading out to the sides
            g2.setPaint(new LinearGradientPaint(w * .15f, 0, w * .85f, 0, new float[]{0f, .5f, 1f},
                    new Color[]{Theme.alpha(accent, 0), accent, Theme.alpha(accent, 0)}));
            g2.fill(new RoundRectangle2D.Float(w * .15f, 1, w * .7f, 3, 3, 3));
            g2.setColor(Theme.alpha(accent, 150));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(shape);
            g2.dispose();
        }
    }

    /** Glowing circle with a question mark and 1-3 pips for the level. */
    private static final class LevelBadge extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Color accent;
        private final int level;

        LevelBadge(Color accent, int level) {
            this.accent = accent;
            this.level = level;
            setPreferredSize(new Dimension(64, 64));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            float cx = 32;
            float cy = 28;
            Theme.glow(g2, cx, cy, 34, accent, 90);
            g2.setColor(Theme.alpha(accent, 45));
            g2.fill(new Ellipse2D.Float(cx - 24, cy - 24, 48, 48));
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Float(cx - 24, cy - 24, 48, 48));
            g2.setFont(Theme.display(26f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("?", cx - fm.stringWidth("?") / 2f, cy + fm.getAscent() / 2f - 3);
            for (int i = 0; i < 3; i++) {             // level pips: 1 easy, 2 medium, 3 hard
                g2.setColor(i < level ? accent : Theme.alpha(accent, 50));
                g2.fill(new Ellipse2D.Float(cx - 15 + i * 11, cy + 29, 8, 8));
            }
            g2.dispose();
        }
    }

    /** Small pill: "+3 if correct" in green or "−3 if wrong" in red. */
    private static final class StakePill extends JComponent {
        private static final long serialVersionUID = 1L;
        private final boolean good;
        private final String text;

        StakePill(boolean good, int steps) {
            this.good = good;
            this.text = good ? "+" + steps + " if correct" : "−" + steps + " if wrong";
            setFont(Theme.semibold(12.5f));
            setPreferredSize(new Dimension(getFontMetrics(getFont()).stringWidth(text) + 44, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            Color c = good ? EASY : HARD;
            int h = 24;
            int y = (getHeight() - h) / 2 + 2;
            g2.setColor(Theme.alpha(c, 30));
            g2.fillRoundRect(0, y, getWidth() - 1, h, h, h);
            g2.setColor(Theme.alpha(c, 150));
            g2.drawRoundRect(0, y, getWidth() - 1, h, h, h);
            // arrow up (forward) or down (back)
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float ax = 15;
            float ay = y + h / 2f;
            float s = good ? 1 : -1;
            g2.draw(new Line2D.Float(ax, ay - 5 * s, ax, ay + 5 * s));
            g2.draw(new Line2D.Float(ax - 4, ay - 1 * s, ax, ay - 5 * s));
            g2.draw(new Line2D.Float(ax + 4, ay - 1 * s, ax, ay - 5 * s));
            g2.setFont(getFont());
            g2.drawString(text, 28, y + h / 2f + g2.getFontMetrics().getAscent() / 2f - 2);
            g2.dispose();
        }
    }

    /** One answer: a glass row with a letter badge; it lights up in the level colour on hover. */
    private static final class AnswerCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private final char letter;
        private final Color accent;
        private final Runnable onChoose;
        private boolean hover;
        private boolean pressed;

        AnswerCard(char letter, String answer, Color accent, Runnable onChoose) {
            super(new BorderLayout());
            this.letter = letter;
            this.accent = accent;
            this.onChoose = onChoose;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JComponent label = text(answer, Theme.ui(16.5f, Font.PLAIN), Theme.TEXT, CARD_WIDTH - 80);
            add(label, BorderLayout.CENTER);
            Dimension size = new Dimension(CARD_WIDTH, Math.max(56, label.getPreferredSize().height + 28));
            int top = (size.height - label.getPreferredSize().height) / 2;   // text centred next to the letter
            setBorder(BorderFactory.createEmptyBorder(top, 62, size.height - top - label.getPreferredSize().height, 18));
            setPreferredSize(size);
            setMaximumSize(size);

            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean inside = contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), AnswerCard.this));
                    pressed = false;
                    repaint();
                    if (inside) choose();
                }
            };
            addMouseListener(mouse);
            label.addMouseListener(mouse);     // the text area would otherwise swallow the clicks
        }

        void choose() {
            onChoose.run();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            int w = getWidth();
            int h = getHeight();
            float lift = pressed ? 0 : hover ? -1 : 0;
            g2.translate(0, lift);
            RoundRectangle2D box = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 18, 18);
            g2.setColor(hover ? Theme.mix(new Color(26, 22, 52), accent, .16f) : new Color(26, 22, 52, 230));
            g2.fill(box);
            g2.setColor(Theme.alpha(hover ? accent : LAVENDER, hover ? 220 : 60));
            g2.setStroke(new BasicStroke(hover ? 1.8f : 1.2f));
            g2.draw(box);

            // letter badge
            float cx = 32;
            float cy = h / 2f;
            if (hover) Theme.glow(g2, cx, cy, 26, accent, 80);
            g2.setColor(hover ? accent : Theme.alpha(accent, 35));
            g2.fill(new Ellipse2D.Float(cx - 16, cy - 16, 32, 32));
            g2.setColor(Theme.alpha(accent, hover ? 255 : 170));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Float(cx - 16, cy - 16, 32, 32));
            g2.setFont(Theme.display(16f));
            FontMetrics fm = g2.getFontMetrics();
            String s = String.valueOf(letter);
            g2.setColor(hover ? new Color(20, 14, 30) : accent);
            g2.drawString(s, cx - fm.stringWidth(s) / 2f, cy + fm.getAscent() / 2f - 2);
            g2.dispose();
        }
    }

    /** The big glowing picture at the top of a result card. */
    private static final class ResultIcon extends JComponent {
        private static final long serialVersionUID = 1L;
        static final int CHECK = 0;
        static final int CROSS = 1;
        static final int SURPRISE = 2;
        static final int DIE = 3;
        static final int SPARK = 4;
        private final int kind;
        private final Game.SurpriseEvent event;
        private final int face;
        private final Color accent;

        ResultIcon(int kind, Game.SurpriseEvent event, int face, Color accent) {
            this.kind = kind;
            this.event = event;
            this.face = face;
            this.accent = accent;
            setPreferredSize(new Dimension(110, 96));
            setMaximumSize(new Dimension(110, 96));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQuality(g2);
            float cx = getWidth() / 2f;
            float cy = 46;
            Theme.glow(g2, cx, cy, 58, accent, 95);
            g2.setColor(Theme.alpha(accent, 40));
            g2.fill(new Ellipse2D.Float(cx - 38, cy - 38, 76, 76));
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(new Ellipse2D.Float(cx - 38, cy - 38, 76, 76));
            g2.translate(cx - 32, cy - 32);        // the pictures below use a 64 x 64 box
            g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case CHECK:
                    Path2D tick = new Path2D.Float();
                    tick.moveTo(18, 33);
                    tick.lineTo(28, 43);
                    tick.lineTo(47, 22);
                    g2.draw(tick);
                    break;
                case CROSS:
                    g2.draw(new Line2D.Float(21, 21, 43, 43));
                    g2.draw(new Line2D.Float(43, 21, 21, 43));
                    break;
                case DIE:
                    paintDie(g2, face);
                    break;
                case SURPRISE:
                    paintSurprise(g2, event);
                    break;
                default:                           // a four-pointed spark
                    Path2D spark = new Path2D.Float();
                    spark.moveTo(32, 14);
                    spark.quadTo(35, 29, 50, 32);
                    spark.quadTo(35, 35, 32, 50);
                    spark.quadTo(29, 35, 14, 32);
                    spark.quadTo(29, 29, 32, 14);
                    g2.fill(spark);
                    break;
            }
            g2.dispose();
        }

        private void paintDie(Graphics2D g2, int n) {
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Float(16, 16, 32, 32, 10, 10));
            int[][] spots;
            switch (n) {
                case 1: spots = new int[][]{{32, 32}}; break;
                case 2: spots = new int[][]{{24, 24}, {40, 40}}; break;
                case 3: spots = new int[][]{{24, 24}, {32, 32}, {40, 40}}; break;
                case 4: spots = new int[][]{{24, 24}, {40, 24}, {24, 40}, {40, 40}}; break;
                case 5: spots = new int[][]{{24, 24}, {40, 24}, {32, 32}, {24, 40}, {40, 40}}; break;
                default: spots = new int[][]{{24, 23}, {40, 23}, {24, 32}, {40, 32}, {24, 41}, {40, 41}}; break;
            }
            for (int[] s : spots) g2.fill(new Ellipse2D.Float(s[0] - 3.2f, s[1] - 3.2f, 6.4f, 6.4f));
        }

        private void paintSurprise(Graphics2D g2, Game.SurpriseEvent e) {
            switch (e) {
                case DIRECT_FINISH:    // flag
                    g2.draw(new Line2D.Float(23, 15, 23, 49));
                    Path2D flag = new Path2D.Float();
                    flag.moveTo(23, 16);
                    flag.lineTo(46, 23);
                    flag.lineTo(23, 31);
                    flag.closePath();
                    g2.fill(flag);
                    break;
                case RETURN_CASTLE:    // tower
                    g2.fill(new Rectangle(19, 27, 26, 22));
                    for (int x = 19; x < 45; x += 9) g2.fill(new Rectangle(x, 19, 6, 9));
                    g2.setColor(new Color(30, 22, 58));
                    g2.fill(new RoundRectangle2D.Float(28, 38, 8, 11, 6, 6));
                    break;
                case BACK_THREE:       // arrow back with -3
                    g2.draw(new Line2D.Float(45, 29, 20, 29));
                    g2.draw(new Line2D.Float(20, 29, 29, 20));
                    g2.draw(new Line2D.Float(20, 29, 29, 38));
                    g2.setFont(Theme.display(15f));
                    g2.drawString("−3", 31, 52);
                    break;
                case SKIP_NEXT_TURN:   // pause
                    g2.fill(new RoundRectangle2D.Float(22, 19, 7, 26, 4, 4));
                    g2.fill(new RoundRectangle2D.Float(35, 19, 7, 26, 4, 4));
                    break;
                case BONUS_ULTRABOOST: // die with five spots
                    paintDie(g2, 5);
                    break;
                default:               // opponent back: an arrow onto a piece
                    g2.draw(new Line2D.Float(45, 18, 25, 38));
                    g2.draw(new Line2D.Float(25, 38, 25, 27));
                    g2.draw(new Line2D.Float(25, 38, 36, 38));
                    g2.fill(new Ellipse2D.Float(15, 41, 11, 11));
                    break;
            }
        }
    }
}