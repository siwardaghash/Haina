
package controller;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import model.GameConfig;
import view.AdminScreen;
import view.GameBoardScreen;
import view.GameHistoryScreen;
import view.GameSetupScreen;
import view.HowToPlayScreen;
import view.OpeningScreen;

/**
 * Application controller (Singleton).
 *
 * Owns the application window and controls navigation between
 * the opening screen, game setup, game board, history,
 * how-to-play screen, and question wizard.
 */
public final class AppController {

    private static final AppController INSTANCE = new AppController();

    private JFrame frame;

    private AppController() {
    }

    public static AppController get() {
        return INSTANCE;
    }

    /**
     * Opens the application window on the opening screen.
     * Must be called on the Swing Event Dispatch Thread.
     */
    public void start() {

        frame = new JFrame("LUDO • Code Quest");

        frame.setDefaultCloseOperation(
                WindowConstants.EXIT_ON_CLOSE
        );

        showOpening();

        frame.pack();

        // Preserve teammate's improvement:
        // Make sure the application window fits the available screen.
        Rectangle usable = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        frame.setSize(
                Math.min(frame.getWidth(), usable.width),
                Math.min(frame.getHeight(), usable.height)
        );

        // Maintain a minimum usable window size when possible.
        frame.setMinimumSize(new Dimension(
                Math.min(1000, usable.width),
                Math.min(700, usable.height)
        ));

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * FR-1, FR-2:
     * Displays the opening screen and its navigation options.
     */
    public void showOpening() {

        show(new OpeningScreen(new OpeningScreen.Listener() {

            @Override
            public void onStartGame() {
                showSetup();
            }

            @Override
            public void onHowToPlay() {
                show(new HowToPlayScreen(
                        AppController.this::showOpening
                ));
            }

            @Override
            public void onGameHistory() {
                show(new GameHistoryScreen(
                        new HistoryController(),
                        AppController.this::showOpening
                ));
            }

            @Override
            public void onQuestionWizard() {
                show(new AdminScreen(
                        new AdminController(),
                        AppController.this::showOpening
                ));
            }

            @Override
            public void onExit() {
                frame.dispose();
                System.exit(0);
            }
        }));
    }

    /**
     * FR-3 to FR-5:
     * Displays the game setup screen.
     */
    public void showSetup() {

        show(new GameSetupScreen(new GameSetupScreen.Listener() {

            @Override
            public void onBack() {
                showOpening();
            }

            @Override
            public void onStart(GameConfig config) {
                showGame(config);
            }
        }));
    }

    /**
     * Starts a new game using the selected configuration.
     */
    private void showGame(GameConfig config) {

        show(new GameBoardScreen(
                new GameController(config),
                this::showOpening,
                () -> showGame(config)
        ));
    }

    /**
     * Replaces the current screen inside the application window.
     */
    private void show(JComponent screen) {

        frame.setContentPane(screen);
        frame.revalidate();
        frame.repaint();
    }
}
