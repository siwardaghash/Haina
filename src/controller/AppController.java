package controller;

import java.awt.Dimension;
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
 * Application controller (Singleton): owns the window and decides which screen is shown,
 * following the system flow of the project document (opening, setup, game, history, wizard).
 */
public final class AppController {
    private static final AppController INSTANCE = new AppController();
    private JFrame frame;

    private AppController() { }

    public static AppController get() { return INSTANCE; }

    /** Opens the window on the opening screen. Call on the Swing event thread. */
    public void start() {
        frame = new JFrame("LUDO • Code Quest");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        showOpening();
        frame.pack();
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** FR-1, FR-2: the opening screen and its options. */
    public void showOpening() {
        show(new OpeningScreen(new OpeningScreen.Listener() {
            @Override public void onStartGame()      { showSetup(); }
            @Override public void onHowToPlay()      { show(new HowToPlayScreen(AppController.this::showOpening)); }
            @Override public void onGameHistory()    { show(new GameHistoryScreen(new HistoryController(), AppController.this::showOpening)); }
            @Override public void onQuestionWizard() { show(new AdminScreen(new AdminController(), AppController.this::showOpening)); }
            @Override public void onExit()           { frame.dispose(); System.exit(0); }
        }));
    }

    /** FR-3 to FR-5: game setup, then a new game with its own controller. */
    public void showSetup() {
        show(new GameSetupScreen(new GameSetupScreen.Listener() {
            @Override public void onBack() { showOpening(); }
            @Override public void onStart(GameConfig config) {
            	showGame(config);
            }
        }));
    }

    private void showGame(GameConfig config) {
        show(new GameBoardScreen(new GameController(config), this::showOpening));
    }
    private void show(JComponent screen) {
        frame.setContentPane(screen);
        frame.revalidate();
        frame.repaint();
    }
}
