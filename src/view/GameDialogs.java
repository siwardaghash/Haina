package view;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import model.Game;
import model.Question;

/** Question, answer-result and surprise windows drawn in the game's own style. */
final class GameDialogs {
    private static final Color NAVY = new Color(17, 31, 62);
    private static final Color INK = new Color(245, 248, 255);
    private static final Color MUTED = new Color(166, 184, 217);
    static final Color EASY = new Color(91, 218, 145);
    static final Color MEDIUM = new Color(255, 218, 95);
    static final Color HARD = new Color(245, 116, 120);
    static final Color SURPRISE = new Color(248, 165, 227);

    private GameDialogs() { }

    static Color levelColor(String level) {
        return "easy".equals(level) ? EASY : "medium".equals(level) ? MEDIUM : HARD;
    }

    /** Shows the question and waits until one of the four answers is clicked (FR-24: no time limit). */
    static int askQuestion(Component parent, Question q) {
        Color accent = levelColor(q.getLevel());
        int steps = "easy".equals(q.getLevel()) ? 1 : "medium".equals(q.getLevel()) ? 2 : 3;
        JDialog dialog = dialog(parent);
        JPanel card = card(accent);
        card.add(title(q.getLevel().toUpperCase() + " QUESTION", accent));
        card.add(small("Correct: forward " + steps + "   •   Wrong: back " + steps));
        card.add(Box.createVerticalStrut(12));
        card.add(body(q.getText(), 18, true));
        card.add(Box.createVerticalStrut(14));
        final int[] chosen = {-1};
        String[] answers = q.getAnswers();
        for (int i = 0; i < answers.length; i++) {
            final int index = i;
            JButton answer = button("<html><div style='width:390px'><b>" + (char) ('A' + i) + ".</b>&nbsp;&nbsp;"
                    + escape(answers[i]) + "</div></html>", new Color(40, 60, 104), INK);
            answer.setHorizontalAlignment(SwingConstants.LEFT);
            answer.addActionListener(e -> { chosen[0] = index; dialog.dispose(); });
            card.add(answer);
            card.add(Box.createVerticalStrut(8));
        }
        show(dialog, card, parent);
        return chosen[0];
    }

    /** After an answer: green or red card with the model's message (the correct answer when wrong, FR-26). */
    static void showAnswerResult(Component parent, boolean correct, String message) {
        Color accent = correct ? EASY : HARD;
        info(parent, accent, correct ? "CORRECT!" : "WRONG ANSWER", message, null);
    }

    static void showSurprise(Component parent, Game.SurpriseEvent event, String text) {
        info(parent, SURPRISE, "SURPRISE!", text, event);
    }

    static void info(Component parent, Color accent, String heading, String text, Game.SurpriseEvent icon) {
        JDialog dialog = dialog(parent);
        JPanel card = card(accent);
        if (icon != null) {
            SurpriseIcon picture = new SurpriseIcon(icon, accent);
            picture.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(picture);
            card.add(Box.createVerticalStrut(8));
        }
        card.add(title(heading, accent));
        card.add(Box.createVerticalStrut(10));
        card.add(body(text, 17, false));
        card.add(Box.createVerticalStrut(16));
        JButton ok = button("OK", accent, NAVY);
        ok.addActionListener(e -> dialog.dispose());
        card.add(ok);
        dialog.getRootPane().setDefaultButton(ok);
        show(dialog, card, parent);
    }

    // ------------------------------------------------------------------ building blocks
    private static JDialog dialog(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);  // the player must choose
        dialog.setBackground(new Color(0, 0, 0, 0));
        return dialog;
    }

    private static void show(JDialog dialog, JPanel card, Component parent) {
        dialog.setContentPane(card);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /** Rounded navy card with a coloured stripe on top. */
    private static JPanel card(Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                g2.setColor(NAVY);
                g2.fill(shape);
                g2.setClip(shape);
                g2.setColor(accent);
                g2.fillRect(0, 0, getWidth(), 8);
                g2.setClip(null);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(accent.darker());
                g2.draw(shape);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));
        return card;
    }

    private static JLabel title(String text, Color accent) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 26));
        label.setForeground(accent);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel small(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JComponent body(String text, int size, boolean bold) {
        JTextArea area = new JTextArea(text);
        area.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        area.setForeground(INK);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setColumns(34);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setSize(new Dimension(460, Short.MAX_VALUE));   // lets the wrapped height be measured
        area.setPreferredSize(new Dimension(460, area.getPreferredSize().height));
        area.setMaximumSize(area.getPreferredSize());
        return area;
    }

    private static JButton button(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 15));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        Theme.solidButton(button);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        return button;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** A simple drawn picture for each surprise event. */
    private static final class SurpriseIcon extends JComponent {
        private final Game.SurpriseEvent event;
        private final Color accent;

        SurpriseIcon(Game.SurpriseEvent event, Color accent) {
            this.event = event;
            this.accent = accent;
            setPreferredSize(new Dimension(64, 64));
            setMaximumSize(new Dimension(64, 64));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
            g2.fillOval(0, 0, 64, 64);
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (event) {
                case DIRECT_FINISH:   // flag
                    g2.drawLine(22, 14, 22, 50);
                    Path2D flag = new Path2D.Double();
                    flag.moveTo(22, 15); flag.lineTo(46, 22); flag.lineTo(22, 30); flag.closePath();
                    g2.fill(flag);
                    break;
                case RETURN_CASTLE:   // tower
                    g2.fillRect(18, 26, 28, 24);
                    for (int x = 18; x < 46; x += 10) g2.fillRect(x, 18, 7, 9);
                    g2.setColor(new Color(17, 31, 62));
                    g2.fillRoundRect(28, 38, 8, 12, 6, 6);
                    break;
                case BACK_THREE:      // arrow back with -3
                    g2.drawLine(44, 32, 20, 32);
                    g2.drawLine(20, 32, 29, 23);
                    g2.drawLine(20, 32, 29, 41);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                    g2.drawString("-3", 36, 52);
                    break;
                case SKIP_NEXT_TURN:  // pause
                    g2.fillRoundRect(21, 18, 8, 28, 4, 4);
                    g2.fillRoundRect(35, 18, 8, 28, 4, 4);
                    break;
                case BONUS_ULTRABOOST: // die
                    g2.drawRoundRect(17, 17, 30, 30, 9, 9);
                    for (int[] d : new int[][]{{24, 24}, {40, 24}, {32, 32}, {24, 40}, {40, 40}})
                        g2.fillOval(d[0] - 3, d[1] - 3, 6, 6);
                    break;
                default:              // opponent back: arrow down-left onto a piece
                    g2.drawLine(44, 18, 24, 38);
                    g2.drawLine(24, 38, 24, 26);
                    g2.drawLine(24, 38, 36, 38);
                    g2.fillOval(16, 42, 10, 10);
                    break;
            }
            g2.dispose();
        }
    }
}
