package screens;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import model.Game;
import model.Piece;
import model.Board;
import model.GameConfig;
import model.PlayerColor;

/** Board visualization. The movement engine will be added in a later step. */
public final class GameBoardScreen extends JPanel {
    private final Game game;
    private final BoardCanvas canvas;
    private final JLabel status;
    private final JLabel dice;
    private final JButton rollButton;
    private final JButton[] pieceButtons = new JButton[4];

    public GameBoardScreen(GameConfig config, Board board) {
        game = new Game(config);
        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(13, 16, 32));
        JLabel title = new JLabel("LUDO  |  " + config.getDifficulty().getLabel(), SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(10, 8, 4, 8));
        add(title, BorderLayout.NORTH);
        canvas = new BoardCanvas(board, config, game);
        add(canvas, BorderLayout.CENTER);

        JPanel controls = new JPanel(new java.awt.GridLayout(3, 1, 3, 3));
        controls.setBackground(getBackground());
        dice = new JLabel("Dice: not rolled", SwingConstants.CENTER);
        dice.setForeground(Color.WHITE);
        controls.add(dice);
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        rollButton = new JButton("ROLL DICE");
        rollButton.addActionListener(e -> { game.roll(); refresh(); });
        buttons.add(rollButton);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            pieceButtons[i] = new JButton("Piece " + (i + 1));
            pieceButtons[i].addActionListener(e -> { game.move(index); refresh(); });
            buttons.add(pieceButtons[i]);
        }
        controls.add(buttons);
        status = new JLabel("", SwingConstants.CENTER);
        status.setForeground(new Color(203, 213, 231));
        controls.add(status);
        add(controls, BorderLayout.SOUTH);
        refresh();
    }

    private void refresh() {
        StringBuilder result = new StringBuilder("Step: ").append(game.getStepDie());
        if (game.getExtraDie() > 0) result.append(" | Boost: ").append(game.getExtraDie());
        result.append(" | Movement: ").append(game.getSteps());
        dice.setText(game.getStepDie() == 0 ? "Dice: not rolled" : result.toString());
        status.setText("Current: " + game.getCurrentPlayer().getName()
            + " | " + game.getMessage() + " | Home-path movement enabled");
        rollButton.setEnabled(!game.isAwaitingPiece());
        for (int i = 0; i < 4; i++) pieceButtons[i].setEnabled(game.isAwaitingPiece() && game.canMove(i));
        canvas.repaint();
    }

    private static final class BoardCanvas extends JPanel {
        private final Board board;
        private final GameConfig config;
        private final Game game;
        private static final Color EMPTY = new Color(244, 247, 252);
        private static final Color OUTLINE = new Color(115, 132, 160);
        private static final int[][] PATH = makePath();

        BoardCanvas(Board board, GameConfig config, Game game) {
            this.game = game;
            this.board = board;
            this.config = config;
            setOpaque(false);
            setPreferredSize(new Dimension(740, 640));
        }

        /** A classic 52-square clockwise path on a 15 by 15 Ludo grid. */
        private static int[][] makePath() {
            List<int[]> p = new ArrayList<>();
            for (int x = 1; x <= 5; x++) p.add(new int[]{x, 6});
            for (int y = 5; y >= 0; y--) p.add(new int[]{6, y});
            p.add(new int[]{7, 0});
            for (int y = 0; y <= 5; y++) p.add(new int[]{8, y});
            for (int x = 9; x <= 14; x++) p.add(new int[]{x, 6});
            p.add(new int[]{14, 7});
            for (int x = 14; x >= 9; x--) p.add(new int[]{x, 8});
            for (int y = 9; y <= 14; y++) p.add(new int[]{8, y});
            p.add(new int[]{7, 14});
            for (int y = 14; y >= 9; y--) p.add(new int[]{6, y});
            for (int x = 5; x >= 0; x--) p.add(new int[]{x, 8});
            p.add(new int[]{0, 7});
            p.add(new int[]{0, 6});
            if (p.size() != 52) throw new IllegalStateException("Expected 52 squares, got " + p.size());
            return p.toArray(new int[0][]);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int extra = board.getRings().size() - 1;
            int cell = Math.max(13, Math.min(39, Math.min((getWidth() - 110) / 19, (getHeight() - 75) / 19)));
            int size = 15 * cell;
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            // Draw additional difficulty tracks as a visual preview only.
            // Their exact movement topology must be implemented with path mapping.
            if (extra >= 1) drawOuterTrack(g, board.getRings().get(1), left - cell * 1.0, top - cell * 1.0, size + 2.0 * cell, cell * .64);
            if (extra >= 2) drawOuterTrack(g, board.getRings().get(2), left - cell * 1.8, top - cell * 1.8, size + 3.6 * cell, cell * .56);

            // Home castles are always shown; pieces only for selected player colors.
            drawCastle(g, left, top, cell, 0, 0, PlayerColor.RED);
            drawCastle(g, left, top, cell, 9, 0, PlayerColor.GREEN);
            drawCastle(g, left, top, cell, 9, 9, PlayerColor.YELLOW);
            drawCastle(g, left, top, cell, 0, 9, PlayerColor.BLUE);

            // Central finish home.
            int c = left + 6 * cell, d = top + 6 * cell;
            g.setColor(new Color(32, 39, 65));
            g.fillRoundRect(c, d, 3 * cell, 3 * cell, 9, 9);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, cell / 3)));
            drawCentered(g, "FINISH", c + 3 * cell / 2, d + 3 * cell / 2 + 4);

            drawHomePath(g, left, top, cell, PlayerColor.RED);
            drawHomePath(g, left, top, cell, PlayerColor.GREEN);
            drawHomePath(g, left, top, cell, PlayerColor.YELLOW);
            drawHomePath(g, left, top, cell, PlayerColor.BLUE);

            List<Board.Square> base = board.getRings().get(0);
            for (int i = 0; i < 52; i++) {
                int[] point = PATH[i];
                Board.Square square = base.get(i);
                drawCell(g, left + point[0] * cell, top + point[1] * cell,
                    cell, squareColor(square), symbol(square));
            }
            drawPieces(g, left, top, cell);
            g.dispose();
        }

        private void drawPieces(Graphics2D g, int left, int top, int cell) {
            for (Game.Player player : game.getPlayers()) {
                for (Piece piece : player.getPieces()) {
                    if (piece.isInCastle()) continue;
                    if (piece.isFinished()) {
                        int size = Math.max(9, cell / 3);
                        int x = left + 7 * cell + (piece.getNumber() % 2) * size;
                        int y = top + 7 * cell + (piece.getNumber() / 2) * size;
                        g.setColor(playerColor(piece.getColor()));
                        g.fillOval(x - size / 2, y - size / 2, size, size);
                        g.setColor(Color.BLACK);
                        g.drawOval(x - size / 2, y - size / 2, size, size);
                        continue;
                    }
                    int[] point = piece.isOnHomePath()
                        ? homeCoordinates(piece.getColor(), piece.getHomePathIndex())
                        : PATH[piece.getBoardIndex()];
                    int offsetX = ((piece.getNumber() - 1) % 2) * Math.max(3, cell / 7);
                    int offsetY = ((piece.getNumber() - 1) / 2) * Math.max(3, cell / 7);
                    int diameter = Math.max(9, cell * 2 / 3);
                    int x = left + point[0] * cell + (cell - diameter) / 2 + offsetX;
                    int y = top + point[1] * cell + (cell - diameter) / 2 + offsetY;
                    g.setColor(playerColor(piece.getColor()));
                    g.fillOval(x, y, diameter, diameter);
                    g.setColor(Color.BLACK);
                    g.drawOval(x, y, diameter, diameter);
                    g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, diameter / 2)));
                    drawCentered(g, Integer.toString(piece.getNumber()), x + diameter / 2, y + diameter * 2 / 3);
                }
            }
        }

        private void drawCastle(Graphics2D g, int left, int top, int cell, int x, int y, PlayerColor color) {
            int px = left + x * cell, py = top + y * cell;
            g.setColor(playerColor(color));
            g.fillRoundRect(px + 2, py + 2, 6 * cell - 4, 6 * cell - 4, 14, 14);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillRoundRect(px + cell, py + cell, 4 * cell, 4 * cell, 12, 12);
            boolean active = false;
            for (GameConfig.PlayerSetup p : config.getPlayers()) {
                if (p.getColor() == color) active = true;
            }
            for (int i = 0; i < 4; i++) {
                if (active) {
                    boolean inCastle = false;
                    for (Game.Player player : game.getPlayers()) {
                        if (player.getColor() == color) inCastle = player.getPieces().get(i).isInCastle();
                    }
                    if (!inCastle) continue;
                }
                int tx = px + (i % 2 == 0 ? 2 : 4) * cell;
                int ty = py + (i / 2 == 0 ? 2 : 4) * cell;
                int radius = Math.max(5, cell / 2);
                g.setColor(active ? playerColor(color) : new Color(180, 185, 195));
                g.fillOval(tx - radius, ty - radius, radius * 2, radius * 2);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(tx - radius, ty - radius, radius * 2, radius * 2);
            }
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, cell / 3)));
            g.setColor(Color.WHITE);
            drawCentered(g, color.getLabel().toUpperCase(), px + 3 * cell, py + (int)(.7 * cell));
        }

        private void drawHomePath(Graphics2D g, int left, int top, int cell, PlayerColor color) {
            for (int i = 0; i < 6; i++) {
                int[] point = homeCoordinates(color, i);
                drawCell(g, left + point[0] * cell, top + point[1] * cell, cell, playerColor(color), "");
            }
        }

        private static int[] homeCoordinates(PlayerColor color, int index) {
            switch (color) {
                case RED: return new int[]{index + 1, 7};
                case GREEN: return new int[]{7, index + 1};
                case YELLOW: return new int[]{13 - index, 7};
                default: return new int[]{7, 13 - index};
            }
        }

        private void drawOuterTrack(Graphics2D g, List<Board.Square> ring,
                                    double x, double y, double size, double diameter) {
            int n = ring.size();
            double perimeter = size * 4;
            for (int i = 0; i < n; i++) {
                double t = perimeter * i / n;
                double px, py;
                if (t < size) {px = x + t; py = y;}
                else if (t < size * 2) {px = x + size; py = y + (t - size);}
                else if (t < size * 3) {px = x + size - (t - size * 2); py = y + size;}
                else {px = x; py = y + size - (t - size * 3);}
                Board.Square square = ring.get(i);
                g.setColor(squareColor(square));
                int r = Math.max(5, (int)diameter);
                g.fillRoundRect((int)px - r / 2, (int)py - r / 2, r, r, 3, 3);
            }
        }

        private void drawCell(Graphics2D g, int x, int y, int cell, Color color, String symbol) {
            g.setColor(color);
            g.fillRect(x, y, cell, cell);
            g.setColor(OUTLINE);
            g.setStroke(new BasicStroke(1f));
            g.drawRect(x, y, cell, cell);
            if (!symbol.isEmpty()) {
                g.setColor(new Color(24, 32, 51));
                g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, cell / 2)));
                drawCentered(g, symbol, x + cell / 2, y + cell / 2 + cell / 6);
            }
        }
        private void drawCentered(Graphics2D g, String value, int x, int y) {
            g.drawString(value, x - g.getFontMetrics().stringWidth(value) / 2, y);
        }
        private Color squareColor(Board.Square s) {
            switch (s.getType()) {
                case START: case ARROW: return playerColor(s.getOwner());
                case PROTECTION: return new Color(196, 174, 252);
                case QUESTION_EASY: return new Color(91, 218, 145);
                case QUESTION_MEDIUM: return new Color(255, 218, 95);
                case QUESTION_HARD: return new Color(245, 116, 120);
                case SURPRISE: return new Color(248, 165, 227);
                default: return EMPTY;
            }
        }
        private String symbol(Board.Square s) {
            switch (s.getType()) {
                case START: return "#";
                case ARROW: return "→";
                case PROTECTION: return "★";
                case QUESTION_EASY: case QUESTION_MEDIUM: case QUESTION_HARD: return "?";
                case SURPRISE: return "!";
                default: return "";
            }
        }
        private Color playerColor(PlayerColor color) {
            if (color == null) return EMPTY;
            switch (color) {
                case RED: return new Color(241, 91, 105);
                case GREEN: return new Color(83, 204, 146);
                case YELLOW: return new Color(245, 205, 79);
                default: return new Color(94, 159, 240);
            }
        }
    }
}
