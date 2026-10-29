
package screens;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class EndGameScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BG = new Color(15, 25, 52);
    private static final Color CARD = new Color(24, 39, 73);
    private static final Color TEXT = new Color(240, 245, 255);
    private static final Color MUTED = new Color(175, 190, 215);

    public EndGameScreen(String results, Runnable onBack) {
        setLayout(new BorderLayout(0, 20));
        setBackground(BG);
        setBorder(new EmptyBorder(35, 50, 35, 50));

        JLabel title = new JLabel("GAME COMPLETED!", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 38));
        title.setForeground(new Color(247, 193, 74));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 15));
        center.setBackground(BG);

        JLabel heading = new JLabel("FINAL RANKINGS", SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(TEXT);
        center.add(heading, BorderLayout.NORTH);

        JTextArea resultText = new JTextArea(results);
        resultText.setFont(new Font("Monospaced", Font.BOLD, 19));
        resultText.setForeground(MUTED);
        resultText.setBackground(CARD);
        resultText.setEditable(false);
        resultText.setFocusable(false);
        resultText.setLineWrap(true);
        resultText.setWrapStyleWord(true);
        resultText.setBorder(new EmptyBorder(30, 35, 30, 35));

        JScrollPane scroll = new JScrollPane(resultText);
        scroll.setBorder(null);
        center.add(scroll, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        JButton backButton = new JButton("BACK TO MAIN MENU");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 17));
        backButton.setBackground(new Color(77, 198, 149));
        backButton.setForeground(new Color(15, 25, 52));
        backButton.setFocusPainted(false);
        backButton.setPreferredSize(new Dimension(260, 55));
        backButton.addActionListener(e -> {
            if (onBack != null) onBack.run();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.add(backButton);
        add(footer, BorderLayout.SOUTH);
    }
}
