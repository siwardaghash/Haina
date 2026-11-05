
package view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Game rules for players, as specified in the SRS (FR-6 to FR-30). */
public class HowToPlayScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BG = new Color(15, 25, 52);
    private static final Color CARD = new Color(24, 39, 73);
    private static final Color TEXT = new Color(240, 245, 255);
    private static final Color MUTED = new Color(175, 190, 215);

    public HowToPlayScreen(Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(30, 45, 15, 45));

        JLabel title = new JLabel("HOW TO PLAY");
        title.setFont(new Font("SansSerif", Font.BOLD, 38));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.WEST);

        JButton back = new JButton("← Back to Menu");
        back.setFont(new Font("SansSerif", Font.BOLD, 15));
        back.setBackground(new Color(55, 85, 140));
        back.setForeground(Color.WHITE);
        back.setFocusPainted(false);
        back.setBorder(new EmptyBorder(10, 18, 10, 18));
        Theme.solidButton(back);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> onBack.run());
        header.add(back, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        JPanel content = new WidthTrackingPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(15, 45, 35, 45));

        addCard(content, "01  GAME SETUP",
            "Choose 2–4 players, enter their names, select unique colors, " +
            "and choose Easy, Medium, or Hard difficulty.",
            new Color(77, 198, 149));

        addCard(content, "02  YOUR TURN",
            "• Click the dice to roll. Then click one of your glowing pieces on the board to move it.\n" +
            "• You move one piece per roll and cannot split the steps. If a move is possible, you must make it.\n" +
            "• A 6 on the Step die brings a piece out of the castle onto your start square. " +
            "Instead, you may use the roll to move a piece that is already on the board.\n" +
            "• No piece on the board and you rolled 1–5? The turn passes to the next player.\n" +
            "• A 6 on the Step die gives you another roll. A third 6 in a row cancels the roll and the turn passes.",
            new Color(96, 158, 239));

        addCard(content, "03  THE DICE",
            "• Easy: Step die (1–6). You move the number you rolled.\n" +
            "• Medium: Step die + BOOST die (1–3).   • Hard: Step die + ULTRA die (1–6).\n" +
            "• Steps = Step × (BOOST or ULTRA − 1). So 1 means ×0 (no move), 2 means ×1, 3 means ×2 ... 6 means ×5.\n" +
            "   Example: Step 4 and BOOST 3 → 4 × 2 = 8 steps.   Step 5 and BOOST 1 → 0 steps.\n" +
            "• When leaving the castle with a 6, the second die is not used.\n" +
            "• A piece on the home path moves by the Step die only.",
            new Color(255, 199, 92));

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
            new Color(83, 204, 146));

        addCard(content, "05  QUESTION SQUARES  ( ? )",
            "• Landing on a question square shows a question. Green = easy, yellow = medium, red = hard.\n" +
            "• Correct answer: move forward (easy 1, medium 2, hard 3 steps). " +
            "Wrong answer: move back the same number, and the correct answer is shown.",
            new Color(247, 193, 74));

        addCard(content, "06  SURPRISE SQUARES  ( ! )",
            "Landing on a surprise square draws one event:\n" +
            "• 10%  The piece goes straight to Finish Home.\n" +
            "• 10%  Choose an opponent's piece; a die is rolled and it moves back half the result (rounded up).\n" +
            "• 10%  The piece goes back to the castle.\n" +
            "• 30%  The piece moves back 3 steps.\n" +
            "• 30%  You lose your next turn (if this was the extra roll after a 6, that extra turn is cancelled).\n" +
            "• 10%  Roll again right away with the Step die and the ULTRA die.",
            new Color(248, 165, 227));

        addCard(content, "07  CAPTURE, PROTECTION & DOUBLE BLOCK",
            "• Landing on a single opponent piece on an unprotected square captures it: it goes back to its castle.\n" +
            "• Protected squares: protection (★), start (#), surprise (!) and home squares. " +
            "No captures there, and pieces may share the square.\n" +
            "• Two of your pieces on one square form a Double Block. Opponents cannot pass or land on it; " +
            "they stop on the square before it.\n" +
            "• Capturing or reaching Finish Home does not give an extra turn.",
            new Color(239, 92, 111));

        addCard(content, "08  HOW TO WIN",
            "Bring all four of your pieces to Finish Home. " +
            "The game ends when all players but one have finished; the last player is placed last. " +
            "The finishing order is shown and saved in the game history.",
            new Color(174, 139, 245));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        // Wrapped text knows its height only once its width is known, so lay out again on resize.
        scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(content::revalidate);
            }
        });
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addCard(JPanel parent, String heading,
                         String description, Color accent) {

        // Each card is as tall as its wrapped text, so longer rules are never cut off.
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(20, 25, 20, 25));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(new Font("SansSerif", Font.BOLD, 21));
        headingLabel.setForeground(accent);
        card.add(headingLabel, BorderLayout.NORTH);

        JTextArea body = new JTextArea(description);
        body.setFont(new Font("SansSerif", Font.PLAIN, 16));
        body.setForeground(MUTED);
        body.setBackground(CARD);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFocusable(false);
        card.add(body, BorderLayout.CENTER);

        parent.add(card);
        parent.add(Box.createVerticalStrut(14));
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
