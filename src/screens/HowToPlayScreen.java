
package screens;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

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
        back.addActionListener(e -> onBack.run());
        header.add(back, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(15, 45, 35, 45));

        addCard(content, "01  GAME SETUP",
            "Choose 2–4 players, enter their names, select unique colors, " +
            "and choose Easy, Medium, or Hard difficulty.",
            new Color(77, 198, 149));

        addCard(content, "02  DICE & MOVEMENT",
            "Players take turns rolling the dice. Roll a 6 to move a piece " +
            "out of its Castle. Rolling a 6 gives an extra turn. " +
            "Move your pieces along the board toward Finish Home.",
            new Color(96, 158, 239));

        addCard(content, "03  SPECIAL SQUARES",
            "Question squares test your software engineering knowledge. " +
            "Your answer affects piece movement. Surprise squares trigger " +
            "random events. Protection squares prevent capturing.",
            new Color(247, 193, 74));

        addCard(content, "04  CAPTURE & DOUBLE BLOCK",
            "Landing on an opponent's unprotected piece captures it. " +
            "Two pieces of the same player on one square create a Double Block " +
            "that prevents opponents from passing or landing.",
            new Color(239, 92, 111));

        addCard(content, "05  HOW TO WIN",
            "Bring all four of your pieces to Finish Home. " +
            "You need the exact number of steps to finish. " +
            "The game records the players' finishing order.",
            new Color(174, 139, 245));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addCard(JPanel parent, String heading,
                         String description, Color accent) {

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(20, 25, 20, 25));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));
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
}
