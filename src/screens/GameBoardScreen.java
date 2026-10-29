package screens;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import model.Game;
import model.Piece;
import model.Board;
import model.GameConfig;
import model.PlayerColor;
import model.QuestionBank;
import model.GameHistory;
import java.nio.file.Paths;
import java.io.File;
import java.util.Random;

/** Polished responsive board view; all rules remain in model.Game. */
public final class GameBoardScreen extends JPanel {
    private static final Color INK = new Color(245, 248, 255);
    private static final Color MUTED = new Color(166, 184, 217);
    private static final Color GOLD = new Color(255, 199, 92);
    private final Game game;
    private boolean resultShown; 
    private final Runnable onBackToMenu;
    private final BoardCanvas canvas;
    private final JLabel status;
    private final JLabel dice;
    private final JLabel playerTag;
    private final FancyButton rollButton;
    private final FancyButton[] pieceButtons = new FancyButton[4];

    public GameBoardScreen(GameConfig config, Board board, Runnable onBackToMenu) {
        this.onBackToMenu = onBackToMenu;
        game = new Game(config, board);
        try {
            File file = new File("data/questions.xml");
            if (!file.isFile()) file = new File("Haina-main/data/questions.xml");
            game.setQuestionBank(new QuestionBank(file, new Random()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Question bank could not be loaded: " + ex.getMessage(),
                    "Question bank error", JOptionPane.ERROR_MESSAGE);
        }
        setLayout(new BorderLayout(12, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 26, 17, 26));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        JLabel title = new JLabel("LUDO  /  CODE QUEST");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("SansSerif", Font.BOLD, 29));
        title.setForeground(INK);
        JLabel subtitle = new JLabel(config.getDifficulty().getLabel().toUpperCase()
                + " MODE   •   " + config.getPlayers().size() + " PLAYERS");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(heading, BorderLayout.CENTER);

        JButton exitButton = new JButton("EXIT GAME");
        exitButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        exitButton.setBackground(new Color(241, 91, 105));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        exitButton.addActionListener(e -> {
        	showExitConfirmation();
        });

        topBar.add(exitButton, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        canvas = new BoardCanvas(board, config, game);
        RoundedCard boardCard = new RoundedCard(new Color(20, 34, 67, 242), 30);
        boardCard.setLayout(new BorderLayout());
        boardCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        boardCard.add(canvas, BorderLayout.CENTER);
        add(boardCard, BorderLayout.CENTER);

        RoundedCard controls = new RoundedCard(new Color(17, 31, 62, 245), 25);
        controls.setLayout(new BorderLayout(5, 8));
        controls.setBorder(BorderFactory.createEmptyBorder(13, 16, 14, 16));
        JPanel information = new JPanel(new BorderLayout(10, 0));
        information.setOpaque(false);
        playerTag = new JLabel("", SwingConstants.LEFT);
        playerTag.setFont(new Font("SansSerif", Font.BOLD, 15));
        information.add(playerTag, BorderLayout.WEST);
        dice = new JLabel("Dice: not rolled", SwingConstants.RIGHT);
        dice.setForeground(INK);
        dice.setFont(new Font("SansSerif", Font.BOLD, 24));        information.add(dice, BorderLayout.EAST);
        controls.add(information, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        actions.setOpaque(false);
        rollButton = new FancyButton("ROLL DICE", GOLD, new Color(35, 39, 53), true);
        rollButton.addActionListener(e -> { game.roll(); refresh(); });
        actions.add(rollButton);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            pieceButtons[i] = new FancyButton("PIECE " + (i + 1), new Color(52, 74, 115), INK, false);
            pieceButtons[i].addActionListener(e -> { game.move(index); resolveSpecials(); refresh(); });
            actions.add(pieceButtons[i]);
        }
        controls.add(actions, BorderLayout.CENTER);
        status = new JLabel("", SwingConstants.CENTER);
        status.setForeground(MUTED);
        status.setFont(new Font("SansSerif", Font.PLAIN, 13));
        controls.add(status, BorderLayout.SOUTH);
        add(controls, BorderLayout.SOUTH);
        refresh();
    }

    /** Answers are given by the player who just moved, before the turn advances. */
    private void resolveSpecials() {
        int guard = 0;
        while ((game.getPendingQuestion() != null || game.isAwaitingSurpriseTarget()) && guard++ < 40) {
            if (game.isAwaitingSurpriseTarget()) {
                java.util.List<int[]> targets = new java.util.ArrayList<>();
                java.util.List<String> labels = new java.util.ArrayList<>();
                for (int p = 0; p < game.getPlayers().size(); p++)
                    for (int i = 0; i < 4; i++)
                        if (game.canSelectSurpriseTarget(p, i)) {
                            targets.add(new int[]{p, i});
                            labels.add(game.getPlayers().get(p).getName() + " – Piece " + (i+1));
                        }
                if (targets.isEmpty()) break;
                Object selection = JOptionPane.showInputDialog(this,
                    "Surprise! Select an opponent piece to move backward:", "Choose an opponent",
                    JOptionPane.QUESTION_MESSAGE, null, labels.toArray(), labels.get(0));
                if (selection == null) {
                    JOptionPane.showMessageDialog(this, "Select a piece to continue.");
                    continue;
                }
                int[] chosen = targets.get(labels.indexOf(selection.toString()));
                int die = game.selectSurpriseTarget(chosen[0], chosen[1]);
                JOptionPane.showMessageDialog(this, "Opponent Step Die: " + die
                    + "\nMoves backward " + ((die+1)/2) + " step(s), subject to blocks.");
                refresh();
                continue;
            }

            QuestionBank.Question q = game.getPendingQuestion();
            String[] choices = q.getAnswers();
            int selection = JOptionPane.showOptionDialog(this,
                    q.getText(), "Question – " + q.getLevel().toUpperCase(),
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, choices, choices[0]);
            if (selection < 0) {
                // Do not leave an unanswerable pending state; reopen the same question.
                JOptionPane.showMessageDialog(this, "Please select an answer to continue.");
                continue;
            }
            game.answerQuestion(selection);
            refresh();
        }
    }

    private void refresh() {
        StringBuilder result = new StringBuilder("STEP  ").append(game.getStepDie());
        if (game.getExtraDie() > 0) result.append("   •   BOOST  ").append(game.getExtraDie());
        result.append("   •   MOVES  ").append(game.getSteps());
        String[] diceFaces = {"", "⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};

        int step = game.getStepDie();

        dice.setText(step == 0
            ? "DICE / READY"
            : diceFaces[step] + "  STEP " + step + "  •  MOVES " + game.getSteps());
        Game.Player current = game.getCurrentPlayer();
        playerTag.setText("●  " + current.getName().toUpperCase() + "'S TURN");
        playerTag.setForeground(BoardCanvas.playerColor(current.getColor()));
        status.setText(game.getMessage());
        rollButton.setEnabled(!game.isGameOver() && !game.isAwaitingPiece() && game.getPendingQuestion() == null
                && !game.isAwaitingSurpriseTarget());
        for (int i = 0; i < 4; i++)
            pieceButtons[i].setEnabled(game.isAwaitingPiece() && game.canMove(i));
        canvas.repaint();
        if (game.isGameOver() && !resultShown) {
            resultShown = true;
            SwingUtilities.invokeLater(this::showFinalResults);
        }
    }

    private void showFinalResults() {
        StringBuilder results = new StringBuilder("FINAL RANKINGS\n\n");
        int rank = 1;
        for (Game.Player player : game.getFinishingOrder())
            results.append(rank++).append(". ").append(player.getName()).append(" (")
                   .append(player.getColor()).append(")\n");
        long sec = game.getDurationSeconds();
        results.append("\nDuration: ").append(String.format("%02d:%02d:%02d", sec/3600, sec/60%60, sec%60));
        try {
            new GameHistory(Paths.get("data", "history.csv")).append(game);
            results.append("\n\nResult saved to data/history.csv");
        } catch (java.io.IOException ex) {
            results.append("\n\nUnable to save result: ").append(ex.getMessage());
        }
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.setContentPane(new EndGameScreen(results.toString(), onBackToMenu));
        frame.revalidate();
        frame.repaint();    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, new Color(14, 25, 55),
                0, Math.max(1, getHeight()), new Color(5, 10, 25)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    private void showExitConfirmation() {
    	JDialog dialog = new JDialog(
    			SwingUtilities.getWindowAncestor(this),
    			"Exit Game",
    			Dialog.ModalityType.APPLICATION_MODAL
    			);
    	
    	dialog.setUndecorated(true);
    	dialog.setSize(440, 260);
    	dialog.setLocationRelativeTo(this);

    	Color navy = new Color(17, 31, 62);
    	Color text = new Color(245, 248, 255);
    	Color muted = new Color(166, 184, 217);

    	JPanel panel = new JPanel(new BorderLayout(15, 18));
    	panel.setBackground(navy);
    	panel.setBorder(BorderFactory.createCompoundBorder(
    			BorderFactory.createLineBorder(new Color(85, 112, 165), 2),
    			BorderFactory.createEmptyBorder(28, 30, 25, 30)
    			));

    	JLabel title = new JLabel("EXIT CURRENT GAME?", SwingConstants.CENTER);
    	title.setFont(new Font("SansSerif", Font.BOLD, 23));
    	title.setForeground(new Color(241, 91, 105));
    	panel.add(title, BorderLayout.NORTH);

    	JLabel message = new JLabel(
    			"<html><div style='text-align:center;'>"
    					+ "Your progress will be lost.<br>"
    					+ "This unfinished game will not be saved."
    					+ "</div></html>",
    					SwingConstants.CENTER
    			);
    	message.setFont(new Font("SansSerif", Font.PLAIN, 16));
    	message.setForeground(muted);
    	panel.add(message, BorderLayout.CENTER);

    	JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
    	buttons.setOpaque(false);

    	FancyButton cancel = new FancyButton("KEEP PLAYING",
    			new Color(77, 198, 149), navy, true);

    	FancyButton exit = new FancyButton("EXIT GAME",
    			new Color(241, 91, 105), text, true);

    	cancel.setPreferredSize(new Dimension(160, 45));
    	exit.setPreferredSize(new Dimension(160, 45));

    	cancel.addActionListener(e -> dialog.dispose());

    	exit.addActionListener(e -> {
    		dialog.dispose();
    			onBackToMenu.run();
    	});

    	buttons.add(cancel);
    	buttons.add(exit);
    	panel.add(buttons, BorderLayout.SOUTH);

    	dialog.setContentPane(panel);
    	dialog.setVisible(true);
    }

    private static final class RoundedCard extends JPanel {
        private final Color color;
        private final int arc;
        RoundedCard(Color color, int arc) {
            this.color = color; this.arc = arc; setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillRoundRect(4, 5, Math.max(0, getWidth()-8), Math.max(0, getHeight()-8), arc, arc);
            g2.setColor(color);
            g2.fillRoundRect(1, 1, Math.max(0, getWidth()-5), Math.max(0, getHeight()-7), arc, arc);
            g2.setColor(new Color(123, 158, 217, 72));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(1, 1, Math.max(0, getWidth()-6), Math.max(0, getHeight()-8), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class FancyButton extends JButton {
        private final Color background;
        private final Color foreground;
        private boolean hovering;
        FancyButton(String text, Color background, Color foreground, boolean primary) {
            super(text);
            this.background=background; this.foreground=foreground;
            setFont(new Font("SansSerif", Font.BOLD, primary ? 13 : 12));
            setPreferredSize(new Dimension(primary ? 140 : 107, 39));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovering=true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovering=false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = isEnabled() ? (hovering ? background.brighter() : background)
                    : new Color(37, 49, 75);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
            g2.setColor(isEnabled() ? foreground : new Color(115, 132, 160));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()-fm.getHeight())/2+fm.getAscent());
            g2.dispose();
        }
    }

    private static final class BoardCanvas extends JPanel {
        private final Board board;
        private final GameConfig config;
        private final Game game;
        private static final Color EMPTY = new Color(244, 247, 252);
        private static final Color OUTLINE = new Color(143, 162, 185);
        private static final int[][] PATH = makePath();

        BoardCanvas(Board board, GameConfig config, Game game) {
            this.game = game;
            this.board = board;
            this.config = config;
            setOpaque(false);
            setPreferredSize(new Dimension(620, 520));
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
            int cell = Math.max(10, Math.min(39, Math.min((getWidth() - 100) / 19, (getHeight() - 25) / 19)));
            int size = 15 * cell;
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            // Draw the actual outer tracks used by the movement model.
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
                    if (piece.isOnCommonPath() && piece.getRing() > 0) {
                        double inset = piece.getRing() == 1 ? 1.0 : 1.8;
                        double ox = left - cell * inset;
                        double oy = top - cell * inset;
                        double side = 15 * cell + 2 * cell * inset;
                        double[] pos = outerPoint(piece.getBoardIndex(),
                                board.getRings().get(piece.getRing()).size(), ox, oy, side);
                        int diameter = Math.max(8, cell / 2);
                        g.setColor(playerColor(piece.getColor()));
                        g.fillOval((int)pos[0] - diameter / 2, (int)pos[1] - diameter / 2, diameter, diameter);
                        g.setColor(new Color(12, 20, 39));
                        g.drawOval((int)pos[0] - diameter / 2, (int)pos[1] - diameter / 2, diameter, diameter);
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
            g.fillRoundRect(px + 2, py + 2, 6 * cell - 4, 6 * cell - 4, 19, 19);
            g.setColor(new Color(255, 255, 255, 220));
            g.fillRoundRect(px + cell, py + cell, 4 * cell, 4 * cell, 16, 16);
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

        /** Ring indices are clockwise from the top-left corner. */
        private static double[] outerPoint(int index, int count, double x, double y, double size) {
            int perSide = count / 4;
            int side = index / perSide;
            int offset = index % perSide;
            double step = size / perSide;
            switch (side) {
                case 0: return new double[]{x + (offset + .5) * step, y + step / 2};
                case 1: return new double[]{x + size - step / 2, y + (offset + .5) * step};
                case 2: return new double[]{x + size - (offset + .5) * step, y + size - step / 2};
                default: return new double[]{x + step / 2, y + size - (offset + .5) * step};
            }
        }

        private void drawOuterTrack(Graphics2D g, List<Board.Square> ring,
                                    double x, double y, double size, double unusedDiameter) {
            int n = ring.size();
            int cellSize = Math.max(6, (int)Math.round(size / (n / 4)) - 2);
            for (int i = 0; i < n; i++) {
                double[] point = outerPoint(i, n, x, y, size);
                Board.Square square = ring.get(i);
                int cx = (int)Math.round(point[0]), cy = (int)Math.round(point[1]);
                g.setColor(squareColor(square));
                g.fillRoundRect(cx - cellSize/2, cy - cellSize/2, cellSize, cellSize, 4, 4);
                g.setColor(OUTLINE);
                g.drawRoundRect(cx - cellSize/2, cy - cellSize/2, cellSize, cellSize, 4, 4);
                String label = symbol(square);
                if (!label.isEmpty()) {
                    g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, cellSize / 2)));
                    g.setColor(new Color(18, 28, 48));
                    drawCentered(g, label, cx, cy + cellSize/5);
                }
            }
        }

        private void drawCell(Graphics2D g, int x, int y, int cell, Color color, String symbol) {
            g.setColor(color);
            g.fillRoundRect(x + 1, y + 1, cell - 2, cell - 2, 5, 5);
            g.setColor(OUTLINE);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x + 1, y + 1, cell - 2, cell - 2, 5, 5);
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
        private static Color playerColor(PlayerColor color) {
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
