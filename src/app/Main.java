package app;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import model.GameConfig;
import model.Board;
import screens.GameBoardScreen;
import screens.GameHistoryScreen;
import screens.AdminScreen;
import screens.GameSetupScreen;
import screens.OpeningScreen;
import screens.HowToPlayScreen;


public class Main {

    private static JFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        frame = new JFrame("LUDO • Code Quest");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        showOpening();
        frame.pack();
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showOpening() {
        show(new OpeningScreen(new OpeningScreen.Listener() {
            @Override public void onStartGame()      { showSetup(); }
            @Override
            public void onHowToPlay() {
                show(new HowToPlayScreen(Main::showOpening));
            }
            @Override public void onGameHistory()    { show(new GameHistoryScreen(Main::showOpening)); }
            @Override public void onQuestionWizard() { show(new AdminScreen(Main::showOpening)); }
            @Override public void onExit()           { frame.dispose(); System.exit(0); }
        }));
    }

    private static void showSetup() {
        show(new GameSetupScreen(new GameSetupScreen.Listener() {
            @Override public void onBack() { showOpening(); }

            @Override public void onStart(GameConfig config) {
                show(new GameBoardScreen(config, new Board(config.getDifficulty())));
            }
        }));
    }

    private static void show(JComponent screen) {
        frame.setContentPane(screen);
        frame.revalidate();
        frame.repaint();
    }

    private static void comingSoon(String what) {
        JOptionPane.showMessageDialog(frame, what + " - coming in a next step!");
    }
}