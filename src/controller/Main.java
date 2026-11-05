package controller;

import javax.swing.SwingUtilities;

/** Entry point of the game (the JAR's Main-Class); the application controller takes over from here. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> AppController.get().start());
    }
}
