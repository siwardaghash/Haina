package app;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import screens.OpeningScreen;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        JFrame frame = new JFrame("LUDO \u2022 Code Quest");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        OpeningScreen opening = new OpeningScreen(new OpeningScreen.Listener() {
            @Override public void onStartGame()      { comingSoon(frame, "Game setup screen"); }
            @Override public void onGameHistory()    { comingSoon(frame, "Game history screen"); }
            @Override public void onQuestionWizard() { comingSoon(frame, "Admin login + Question wizard"); }
            @Override public void onExit()           { frame.dispose(); System.exit(0); }
        });

        frame.setContentPane(opening);
        frame.pack();
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void comingSoon(JFrame parent, String what) {
        JOptionPane.showMessageDialog(parent, what + " - coming in the next step!");
    }
}