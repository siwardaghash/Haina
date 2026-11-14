package view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import model.Game;
import model.GameObserver;
import model.Piece;
import model.Board;
import model.GameConfig;
import model.PlayerColor;
import model.Question;
import controller.GameController;
import java.util.Random;

/** Polished responsive board view; all rules remain in model.Game. */
public final class GameBoardScreen extends JPanel {
	private float animationTime = 0f;

	private final Timer backgroundTimer = new Timer(60, e -> {
	    animationTime += 0.04f;
	    repaint();
	});
    private static final Color INK = new Color(245, 248, 255);
    private static final Color MUTED = new Color(166, 184, 217);
    private static final Color GOLD = new Color(255, 199, 92);
    private final GameController controller;
    private final Game game;           // read only: every action goes through the controller
    private final model.Difficulty difficulty;
    private boolean resultShown; 
    private final Runnable onBackToMenu;
    private final BoardCanvas canvas;
    private final JLabel status;
    private final DiceView dice;
    private final DiceView extraDice;
    private final JLabel extraCaption;
    private boolean animating;
    private boolean lastRollBonus;
    private final Random effects = new Random();
    private JLabel movementInfo;
    private final JLabel playerTag;
    private final PointerArrow dicePointer = new PointerArrow();
    /** Each player sees the pointing arrows while learning (first roll, first moves); then only the glow stays. */
    private static final int POINTER_MOVES = 3;
    private final java.util.Set<PlayerColor> rolledOnce = java.util.EnumSet.noneOf(PlayerColor.class);
    private final java.util.Map<PlayerColor, Integer> movesMade = new java.util.EnumMap<>(PlayerColor.class);
    /** Explains a roll that could not be used; shown until the next roll. */
    private String notice;
    private boolean captureSoundPending;
    private final Timer hintTimer;
    /** FR-9: the total game time, updated every second. */
    private final JLabel gameClock = new JLabel("", SwingConstants.CENTER);
    private final Timer clockTimer = new Timer(1000, e -> updateClock());
    
    private final Runnable onRematch;

    /** Convenience for tests: a new game controller for this board. */
    public GameBoardScreen(GameConfig config, Board board, Runnable onBackToMenu) {
        this(new GameController(config, board), onBackToMenu);
    }

    public GameBoardScreen(GameController controller, Runnable onBackToMenu) {
        this(controller, onBackToMenu, null);
    }

    public GameBoardScreen(GameController controller, Runnable onBackToMenu,
                           Runnable onRematch) {
        this.onRematch = onRematch;
        this.onBackToMenu = onBackToMenu;
        this.controller = controller;
        GameConfig config = controller.getConfig();
        Board board = controller.getBoard();
        game = controller.getGame();
        difficulty = config.getDifficulty();
        if (controller.getBankError() != null)
            Theme.neonMessage(this, "Question bank error",
                    "Question bank could not be loaded: " + controller.getBankError(), Theme.RED);
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

        // Painted by FancyButton: the Mac look and feel ignores a plain JButton's background colour.
        NeonButton exitButton = new NeonButton(
        	    "Exit Game", Theme.RED, NeonButton.Glyph.BACK, null);

        	exitButton.setChevron(false);
        	exitButton.setPreferredSize(new Dimension(184, 64));
        	exitButton.setFocusable(false);
        	
        exitButton.addActionListener(e -> {
        	showExitConfirmation();
        });

        // Music and sound toggle. Use the screen's existing custom-painted button,
        // avoiding unsupported NeonButton.Glyph values (SOUND / NONE).
        NeonButton soundButton = new NeonButton(
                Sound.isMuted() ? "Sound Off" : "Sound On",
                Theme.VIOLET, NeonButton.Glyph.SOUND, null);

        soundButton.setChevron(false);
        soundButton.setPreferredSize(new Dimension(184, 64));
        soundButton.setFocusable(false);

        soundButton.addActionListener(e -> {
            Sound.setMuted(!Sound.isMuted());
            soundButton.setText(Sound.isMuted() ? "Sound Off" : "Sound On");
            soundButton.repaint();
        });
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topButtons.setOpaque(false);
        topButtons.add(soundButton);
        topButtons.add(exitButton);
        topBar.add(topButtons, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        canvas = new BoardCanvas(board, config, game);
        RoundedCard boardCard = new RoundedCard(Theme.GLASS, 30);
        boardCard.setLayout(new BorderLayout());
        boardCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        boardCard.add(canvas, BorderLayout.CENTER);
        add(boardCard, BorderLayout.CENTER);

        RoundedCard controls = new RoundedCard(Theme.GLASS, 28);
        controls.setLayout(new BorderLayout(5, 8));
        controls.setBorder(BorderFactory.createEmptyBorder(13, 16, 14, 16));
        JPanel information = new JPanel(new BorderLayout(10, 0));
        information.setOpaque(false);
        playerTag = new JLabel("", SwingConstants.LEFT) {
        	@Override
        	protected void paintComponent(Graphics g) {
        	    Graphics2D g2 = (Graphics2D) g.create();
        	    Theme.applyQuality(g2);

        	    Color accent = getBackground();

        	    int h = Math.min(52, getHeight() - 8);
        	    int y = (getHeight() - h) / 2;

        	    // Dark glass background
        	    g2.setColor(new Color(12, 17, 38, 235));
        	    g2.fillRoundRect(1, y, getWidth() - 3, h, 22, 22);

        	    // Thin border matching the player's color
        	    g2.setColor(new Color(
        	        accent.getRed(),
        	        accent.getGreen(),
        	        accent.getBlue(),
        	        190
        	    ));
        	    g2.setStroke(new BasicStroke(1.5f));
        	    g2.drawRoundRect(1, y, getWidth() - 3, h, 22, 22);

        	    g2.dispose();
        	    super.paintComponent(g);
        	}
        };
        // Same font and border as refresh() uses, so the tag is wide enough from the start.
        playerTag.setFont(new Font("SansSerif", Font.BOLD, 22));
        playerTag.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        information.add(playerTag, BorderLayout.WEST);
        gameClock.setForeground(Theme.TEXT);
        gameClock.setFont(Theme.semibold(17f));
        gameClock.setToolTipText("Total game time");
        updateClock();
        information.add(gameClock, BorderLayout.CENTER);
        dice = new DiceView();
        dice.setForeground(new Color(24, 36, 60));
        dice.setOpaque(false);
        dice.setPreferredSize(new Dimension(66, 66));

        movementInfo = new JLabel("", SwingConstants.CENTER);
        movementInfo.setForeground(Theme.MUTED);
        movementInfo.setFont(Theme.semibold(14f));
        // Wide enough for the longest text, "6 × (ULTRA 6 − 1) = 30 STEPS".
        movementInfo.setPreferredSize(new Dimension(300, 40));
        
        extraDice = new DiceView();
        extraDice.setPreferredSize(new Dimension(66, 66));
        extraDice.setTint(new Color(255, 214, 120));
        extraCaption = caption(config.getDifficulty().hasUltraBoostDie() ? "ULTRA" : "BOOST");

        // The dice themselves are the roll control.
        MouseAdapter rollOnClick = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (canRoll()) rollAnimated(); }
        };
        dice.addMouseListener(rollOnClick);
        extraDice.addMouseListener(rollOnClick);

        JPanel tray = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        tray.setOpaque(false);
        tray.add(dicePointer);
        tray.add(withCaption(dice, caption("STEP")));
        tray.add(withCaption(extraDice, extraCaption));

        JPanel dicePanel = new JPanel(new BorderLayout(12, 0));
        dicePanel.setOpaque(false);

        dicePanel.add(tray, BorderLayout.WEST);
        dicePanel.add(movementInfo, BorderLayout.EAST);
        information.add(dicePanel, BorderLayout.EAST);
        controls.add(information, BorderLayout.NORTH);
        // Space rolls the dice, like a click on the dice (prototype feedback).
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "rollDice");
        getActionMap().put("rollDice", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (canRoll()) rollAnimated(); }
        });
        canvas.setOnPieceClicked(index -> {
            if (game.isAwaitingSurpriseTarget()) chooseSurpriseTarget(index / 4, index % 4);
            else moveAnimated(index);
        });
        canvas.setOnStuckPieceClicked(this::explainStuckPiece);

        // Only the dice and the arrow are repainted here, and only while a roll is awaited.
        hintTimer = new Timer(90, e -> {
            if (!canRoll()) return;
            dicePointer.bounce += .45;
            dice.pulse += .35;
            extraDice.pulse += .35;
            dicePointer.repaint();
            dice.repaint();
            extraDice.repaint();
        });
        status = new JLabel("", SwingConstants.CENTER);
        status.setForeground(MUTED);
        status.setFont(new Font("SansSerif", Font.PLAIN, 13));
        controls.add(status, BorderLayout.SOUTH);
        add(controls, BorderLayout.SOUTH);
        refresh();
    }

    @Override public void addNotify() {
        super.addNotify();
        game.addObserver(observer);
        hintTimer.start(); clockTimer.start(); Sound.startMusic();
        backgroundTimer.start();
    }
    @Override public void removeNotify() {
        game.removeObserver(observer);
        hintTimer.stop(); clockTimer.stop(); Sound.stopMusic();
        backgroundTimer.stop();
        super.removeNotify();
    }

    /**
     * Observer (MVC): the model tells this screen after every action. The turn, dice and message
     * panel follow it at once, unless an animation is running: then it is updated when the
     * animation ends, so the panel does not run ahead of the piece that is still walking.
     */
    private final GameObserver observer = changed -> SwingUtilities.invokeLater(() -> {
        if (!animating) refreshPanel();
    });

    private void updateClock() {
        long sec = game.getDurationSeconds();   // stops counting when the game is over
        gameClock.setText(String.format("GAME TIME  %02d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60));
    }

    private boolean canRoll() {
        return !animating && !game.isGameOver() && !game.isAwaitingPiece()
                && game.getPendingQuestion() == null && !game.isAwaitingSurpriseTarget();
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(MUTED);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        return label;
    }

    private static JPanel withCaption(JComponent die, JLabel label) {
        JPanel box = new JPanel(new BorderLayout(0, 2));
        box.setOpaque(false);
        box.add(die, BorderLayout.CENTER);
        box.add(label, BorderLayout.SOUTH);
        return box;
    }

    /** The model decides the result first; the dice only tumble before showing it. */
    private void rollAnimated() {
        if (!canRoll()) return;
        Game.Player roller = game.getCurrentPlayer();
        rolledOnce.add(roller.getColor());
        boolean allInCastle = true;
        for (Piece p : roller.getPieces()) if (!p.isInCastle() && !p.isFinished()) allInCastle = false;
        lastRollBonus = game.isBonusRollReady();
        controller.roll();
        Sound.roll();
        notice = explainUnusedRoll(roller, allInCastle);
        java.util.List<Runnable> blockMessages = new java.util.ArrayList<>();
        if (notice != null)
            for (Piece p : roller.getPieces()) {
                int block = game.getBlockAhead(p);
                if (block >= 0) blockMessages.add(() -> sayBlock(p, block, "Double Block: can't pass"));
            }
        animating = true;
        setControlsEnabled(false);
        boolean showExtra = game.getExtraDie() > 0;
        extraDice.getParent().setVisible(showExtra);
        final int[] tick = {0};
        Timer timer = new Timer(45, null);
        timer.addActionListener(e -> {
            tick[0]++;
            double wobble = (effects.nextDouble() - .5) * .9;
            dice.showFace(effects.nextInt(6) + 1, wobble);
            if (showExtra) extraDice.showFace(effects.nextInt(difficulty.hasUltraBoostDie() || lastRollBonus ? 6 : 3) + 1, -wobble);
            if (tick[0] >= 7) {
                timer.stop();
                animating = false;
                refresh();
                blockMessages.forEach(Runnable::run);
             // Automatically move when there is exactly one legal choice.
                if (game.isAwaitingPiece()) {
                    int onlyMovablePiece = -1;
                    int legalMoves = 0;

                    for (int i = 0; i < game.getCurrentPlayer().getPieces().size(); i++) {
                        if (game.canMove(i)) {
                            legalMoves++;
                            onlyMovablePiece = i;
                        }
                    }

                    if (legalMoves == 1) {
                        moveAnimated(onlyMovablePiece);
                    }
                }
            }
        });
        timer.start();
    }

    /** Null when the roll can be used; otherwise says in plain words why nothing moved. */
    private String explainUnusedRoll(Game.Player roller, boolean allInCastle) {
        if (game.isAwaitingPiece() || game.isGameOver()) return null;
        boolean turnPassed = game.getCurrentPlayer() != roller;
        String extraName = difficulty.hasUltraBoostDie() || lastRollBonus ? "ULTRA" : "BOOST";
        int step = game.getStepDie();
        if (turnPassed && step == 6) return null; // third 6: the model's message already says so
        String rolled = roller.getName() + " rolled " + step
                + (game.getExtraDie() > 0 ? " and " + game.getExtraDie() + " on the " + extraName + " die" : "") + ". ";
        String reason;
        if (allInCastle && step != 6) reason = "All pieces are in the castle, and only a 6 brings a piece out.";
        else if (game.getSteps() == 0) reason = "A 1 on the " + extraName + " die means 0 steps.";
        else reason = "No piece can move " + game.getSteps() + " step(s).";
        String next = turnPassed ? "  Turn passes to " + game.getCurrentPlayer().getName() + "."
                                 : "  A 6 gives another roll.";
        return rolled + reason + next;
    }

    /**
     * Moves in the model, then walks the piece square by square to where the dice took it.
     * If it landed on a special square, the result is explained first and then walked too.
     */
    private void moveAnimated(int index) {
        if (animating || !game.isAwaitingPiece() || !game.canMove(index)) return;
        movesMade.merge(game.getCurrentPlayer().getColor(), 1, Integer::sum);
        notice = null;
        Piece piece = game.getCurrentPlayer().getPieces().get(index);
        java.util.Map<Piece, Integer> atStart = positions();
        int before = piece.getProgress();
        int block = game.getBlockAhead(piece);
        controller.move(index);
        captureSoundPending = game.getMessage().contains("Captured");
        boolean stopped = game.getMessage().contains("Double Block");
        boolean captured = captureSoundPending;
        int landing = game.getLastLanding();
        // Where everything stands once the dice move is walked (before any surprise moved it again).
        java.util.Map<Piece, Integer> atLanding = new java.util.HashMap<>(atStart);
        atLanding.put(piece, landing);
        walk(piece, before, landing, () -> {
            announceLanding(piece, landing, captured);
            if (stopped && block >= 0) sayBlock(piece, block, "Double Block: can't pass");
            afterMove(atLanding);
        });
    }

    /**
     * Bonus: small messages right at the square where a special rule just happened:
     * a capture, a new Double Block, or a safe square where nobody can be captured.
     */
    private void announceLanding(Piece piece, int landing, boolean captured) {
    	if (landing == piece.getFinishProgress()) {
    	    canvas.say(piece, landing, "★ PIECE HOME!",
    	            BoardCanvas.playerColor(piece.getColor()));
    	    Sound.win();
    	    return;
    	}
        if (landing < 0 || landing >= piece.getCommonLength()) return;
        Color color = BoardCanvas.playerColor(piece.getColor());
        int ring = piece.ringAt(landing), index = piece.indexAt(landing);
        int own = 0;
        boolean opponentHere = false;
        for (Game.Player player : game.getPlayers())
            for (Piece other : player.getPieces())
                if (other.isOnCommonPath() && other.getRing() == ring && other.getBoardIndex() == index) {
                    if (other.getColor() == piece.getColor()) own++;
                    else opponentHere = true;
                }
        if (captured) canvas.say(piece, landing, "Captured! Back to the castle", color);
        else if (own >= 2) canvas.say(piece, landing, "Double Block formed!", color);
        else if (opponentHere && game.isSafeSquare(ring, index)) canvas.say(piece, landing, "Safe square: no capture", color);
    }

    /** The message over an opposing Double Block, at the route position {@code block} of {@code piece}. */
    private void sayBlock(Piece piece, int block, String text) {
        Color owner = BoardCanvas.playerColor(piece.getColor());
        int ring = piece.ringAt(block), index = piece.indexAt(block);
        for (Game.Player player : game.getPlayers()) {
            if (player.getColor() == piece.getColor()) continue;
            int count = 0;
            for (Piece other : player.getPieces())
                if (other.isOnCommonPath() && other.getRing() == ring && other.getBoardIndex() == index) count++;
            if (count >= 2) owner = BoardCanvas.playerColor(player.getColor());
        }
        canvas.say(piece, block, text, owner);
    }

    /** A click on one of the current player's pieces that cannot move: say why, where it happens. */
    private void explainStuckPiece(int index) {
        if (animating) return;
        Piece piece = game.getCurrentPlayer().getPieces().get(index);
        String reason = game.explainCannotMove(index);
        if (reason == null) return;
        int block = game.getBlockAhead(piece);
        if (block >= 0) sayBlock(piece, block, "Double Block: piece " + piece.getNumber() + " can't pass");
        if (block < 0 || reason.startsWith("Blocked ahead"))
            canvas.say(piece, piece.getProgress(), reason, BoardCanvas.playerColor(piece.getColor()));
        Sound.wrong();
    }

    /** After the dice move: capture sound, the surprise explanation and its walk, then questions. */
    private void afterMove(java.util.Map<Piece, Integer> atLanding) {
        if (captureSoundPending) Sound.capture();
        captureSoundPending = false;
        showSurpriseIfAny();
        animateChanges(atLanding, this::resolveSpecials);
    }

    /** Where every piece is now; compared later to walk what an action changed. */
    private java.util.Map<Piece, Integer> positions() {
        java.util.Map<Piece, Integer> map = new java.util.HashMap<>();
        for (Game.Player player : game.getPlayers())
            for (Piece piece : player.getPieces()) map.put(piece, piece.getProgress());
        return map;
    }

    /**
     * Walks, one after another, every piece that moved along the track since {@code before}
     * (forward or back). Pieces sent to their castle, or straight to Finish Home, just appear there.
     */
    private void animateChanges(java.util.Map<Piece, Integer> before, Runnable then) {
        java.util.List<Piece> moved = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Piece, Integer> e : before.entrySet()) {
            int from = e.getValue(), to = e.getKey().getProgress();
            if (from != to && from >= 0 && to >= 0 && Math.abs(to - from) <= 40) moved.add(e.getKey());
        }
        walkAll(moved, 0, before, then);
    }

    private void walkAll(java.util.List<Piece> moved, int i, java.util.Map<Piece, Integer> before, Runnable then) {
        if (i >= moved.size()) { then.run(); return; }
        Piece piece = moved.get(i);
        walk(piece, before.get(piece), piece.getProgress(), () -> walkAll(moved, i + 1, before, then));
    }

    /**
     * Shows a piece hopping from one square to the next, from {@code from} to {@code to}
     * (about 0.1 s a square, a long move at most about 2 s), then runs {@code then}.
     */
    private void walk(Piece piece, int from, int to, Runnable then) {
        if (from < 0) from = to;                       // leaving the castle: one hop onto the start square
        int steps = Math.abs(to - from);
        animating = true;
        setControlsEnabled(false);
        canvas.startHop(piece, from);
        Sound.hop();
        Timer timer = new Timer(steps == 0 ? 160 : Math.max(45, Math.min(110, 2200 / steps)), null);
        final int target = to;
        timer.addActionListener(e -> {
            if (canvas.hopToward(target)) Sound.hop();
            else {
                timer.stop();
                canvas.endHop();
                animating = false;
                then.run();
            }
        });
        timer.start();
    }

    /**
     * FR-28: tells the players what a surprise did. Called after every model action
     * (a move, an answer, a chosen opponent), because each can land on a surprise square.
     */
    private void showSurpriseIfAny() {
        Game.SurpriseEvent surprise = game.getLastSurprise();
        if (surprise == null) return;
        String text;
        switch (surprise) {
            case DIRECT_FINISH: text = "Your piece goes straight to FINISH HOME!"; break;
            case RETURN_CASTLE: text = "Your piece goes back to the castle."; break;
            case BACK_THREE: text = "Your piece moves back 3 squares (it stops before a Double Block)."; break;
            case SKIP_NEXT_TURN: text = game.getMessage().contains("extra turn after 6 cancelled")
                    ? "Your extra turn from the 6 is cancelled."
                    : "You lose your next turn."; break;
            case BONUS_ULTRABOOST: text = "Roll again right now with the Step die and the ULTRA die (1-6).\n"
                    + "Steps = Step x (ULTRA - 1). This roll does not count as a 6 in a row."; break;
            default: text = game.isAwaitingSurpriseTarget()
                    ? "Choose an opponent's piece: click one of the glowing pieces on the board.\n"
                      + "A die is rolled and that piece moves back half the result (rounded up)."
                    : "No opponent piece can move back, so this surprise is cancelled."; break;
        }
        Sound.surprise();
        GameDialogs.showSurprise(this, surprise, text);
    }

    /** The current player clicked an opponent piece for the "move an opponent back" surprise. */
    private void chooseSurpriseTarget(int playerIndex, int pieceIndex) {
        if (animating || !game.canSelectSurpriseTarget(playerIndex, pieceIndex)) return;
        java.util.Map<Piece, Integer> beforeChoice = positions();
        Game.Player owner = game.getPlayers().get(playerIndex);
        Piece target = owner.getPieces().get(pieceIndex);
        int before = target.getProgress();
        int die = controller.chooseSurpriseTarget(playerIndex, pieceIndex);
        int moved = before - target.getProgress();
        Sound.capture();
        GameDialogs.info(this, GameDialogs.SURPRISE, "DIE: " + die,
                owner.getName() + "'s piece " + (pieceIndex + 1) + " moves back " + moved + " step(s)"
                + (moved < (die + 1) / 2 ? " (stopped by its start square or a Double Block)." : "."), null);
        showSurpriseIfAny();
        animateChanges(beforeChoice, this::resolveSpecials);
    }

    private void setControlsEnabled(boolean enabled) {
        updateHints();
        canvas.repaint();
    }

    /** Arrow while the current player is still learning the action, glow afterwards. */
    private void updateHints() {
        PlayerColor current = game.getCurrentPlayer().getColor();
        boolean rollReady = canRoll();
        dice.setReady(rollReady);
        extraDice.setReady(rollReady && extraDice.getParent().isVisible());
        dicePointer.setActive(rollReady && !rolledOnce.contains(current));
        dicePointer.setColor(BoardCanvas.playerColor(current));
        canvas.setPointAtPieces(game.isAwaitingSurpriseTarget() || movesMade.getOrDefault(current, 0) < POINTER_MOVES);
    }

    /**
     * Answers are given by the player who just moved, before the turn advances. After each answer
     * the result (and any surprise it led to) is explained, then the piece walks there; a landing on
     * another question square asks again.
     */
    private void resolveSpecials() {
        if (game.getPendingQuestion() == null || game.isAwaitingSurpriseTarget()) {
            refresh();       // while a surprise target is awaited, the board waits for a click
            return;
        }
        Question q = game.getPendingQuestion();
        int selection = GameDialogs.askQuestion(this, q);
        if (selection < 0) { resolveSpecials(); return; }     // the window only closes through an answer
        java.util.Map<Piece, Integer> beforeAnswer = positions();
        Game.Player answering = game.getCurrentPlayer();
        boolean correct = controller.answer(selection);
        if (correct) Sound.correct(); else Sound.wrong();
        boolean capturedByAnswer = game.getMessage().contains("Captured");
        GameDialogs.showAnswerResult(this, correct, game.getMessage());
        java.util.Map<Piece, Integer> afterAnswer = positions();
        showSurpriseIfAny();
        animateChanges(beforeAnswer, () -> {
            for (Piece p : answering.getPieces())     // where the answer took the piece
                if (beforeAnswer.get(p).intValue() != afterAnswer.get(p).intValue() && afterAnswer.get(p) >= 0)
                    announceLanding(p, afterAnswer.get(p), capturedByAnswer);
            resolveSpecials();
        });
    }

    /** Brings the whole screen up to date: the panel, and the final results once the game is over. */
    private void refresh() {
        refreshPanel();
        if (game.isGameOver() && !resultShown) {
            resultShown = true;

            if (game.isTeamMode()) {
                status.setText(game.getMessage());
                status.setForeground(GOLD);
                status.setFont(status.getFont().deriveFont(Font.BOLD, 24f));
                Sound.stopMusic();
                Sound.win();

                Timer teamResultsDelay = new Timer(2200, e -> {
                    if (isShowing()) {
                        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
                        frame.setContentPane(new TeamEndGameScreen(game, onBackToMenu, onRematch));
                        frame.revalidate();
                        frame.repaint();
                    }
                });
                teamResultsDelay.setRepeats(false);
                teamResultsDelay.start();
                return;
            }
            Game.Player winner = game.getFinishingOrder().get(0);
            String winnerColor = winner.getColor().getLabel()
                    .toUpperCase(java.util.Locale.ROOT);

            status.setText(winnerColor + " WINS!");
            status.setForeground(BoardCanvas.playerColor(winner.getColor()));
            status.setFont(status.getFont().deriveFont(Font.BOLD, 24f));

            Sound.stopMusic();
            Sound.win();

            Timer resultsDelay = new Timer(2200, e -> {
                if (isShowing()) {
                    showFinalResults();
                }
            });
            resultsDelay.setRepeats(false);
            resultsDelay.start();
        }
    }

    /** The turn, dice, steps and message panel, and the board hints, as the model has them now. */
    private void refreshPanel() {
        // Spell out the multiplier rule: steps = Step x (Boost - 1).
        int step = game.getStepDie();
        String extraName = difficulty.hasUltraBoostDie() || lastRollBonus ? "ULTRA" : "BOOST";
        String math;
        if (step == 0) math = "Click the dice to roll";
        else if (game.getExtraDie() == 0) math = step + " STEPS";
        else math = step + " × (" + extraName + " " + game.getExtraDie() + " − 1) = " + game.getSteps() + " STEPS";
        movementInfo.setText(math);
        movementInfo.setToolTipText(game.getExtraDie() == 0 ? null
                : "<html>Steps = Step die × (" + extraName + " die − 1)<br>"
                + extraName + " 1 → ×0 (no move), 2 → ×1, 3 → ×2"
                + (extraName.equals("ULTRA") ? ", 4 → ×3, 5 → ×4, 6 → ×5" : "")
                + "<br>A 6 on the Step die brings a piece out of the castle (the other die is not used).<br>"
                + "On the home path only the Step die counts.</html>");
        dice.setValue(step == 0 ? 1 : step);
        boolean hasExtra = game.getExtraDie() > 0
                || difficulty.hasBoostDie() || difficulty.hasUltraBoostDie();
        extraDice.getParent().setVisible(hasExtra);
        extraDice.setValue(game.getExtraDie() == 0 ? 1 : game.getExtraDie());
        extraCaption.setText(difficulty.hasUltraBoostDie() || lastRollBonus ? "ULTRA" : "BOOST");
        Game.Player current = game.getCurrentPlayer();
        Color playerColor = BoardCanvas.playerColor(current.getColor());

        playerTag.setText("●  " + current.getName().toUpperCase() + "'S TURN"
                + (game.isTeamMode()
                        ? (game.getTeamOf(current.getColor()) == 0 ? "  [TEAM A]" : "  [TEAM B]")
                        : ""));
        playerTag.setOpaque(false);

        playerTag.setBackground(playerColor);
        playerTag.setForeground(Theme.TEXT);
        playerTag.setFont(Theme.semibold(18f));
        playerTag.setBorder(
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        );

        playerTag.revalidate();
        playerTag.repaint();
        boolean highlight = notice != null || game.isAwaitingPiece() || game.isAwaitingSurpriseTarget();
        status.setText(game.isAwaitingSurpriseTarget()
                ? "Surprise: " + current.getName() + ", click a glowing opponent piece to move it back."
                : notice != null ? notice
                : game.isAwaitingPiece() ? current.getName() + ": click a glowing piece on the board to move it."
                : game.getMessage());
        status.setForeground(highlight ? GOLD : MUTED);
        status.setFont(new Font("SansSerif", highlight ? Font.BOLD : Font.PLAIN, highlight ? 16 : 13));
        status.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        status.revalidate();
        updateHints();
        canvas.repaint();
    }

    private void showFinalResults() {
        StringBuilder results = new StringBuilder("FINAL RANKINGS\n\n");
        int rank = 1;
        for (Game.Player player : game.getFinishingOrder())
            results.append(rank++).append(". ").append(player.getName()).append(" (")
                   .append(player.getColor()).append(")\n");
        long sec = game.getDurationSeconds();
        results.append("\nDifficulty: ").append(game.getDifficulty().getLabel());   // FR-31
        results.append("\nDuration: ").append(String.format("%02d:%02d:%02d", sec/3600, sec/60%60, sec%60));
        try {
            controller.saveResult();
            results.append("\n\nResult saved to the game history.");
        } catch (java.io.IOException ex) {
            results.append("\n\nUnable to save result: ").append(ex.getMessage());
        }
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.setContentPane(new EndGameScreen(results.toString(), onBackToMenu, onRematch));
        frame.revalidate();
        frame.repaint();    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQuality(g2);
        Theme.paintBackdrop(g2, getWidth(), getHeight(), animationTime);
        g2.dispose();
    }
    
    private void showExitConfirmation() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Exit Game", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final boolean[] exitConfirmed = {false};

        JPanel panel = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQuality(g2);

                g2.setPaint(new GradientPaint(
                        0, 0, new Color(37, 24, 54),
                        0, getHeight(), new Color(13, 18, 42)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(Theme.RED);
                g2.setStroke(new BasicStroke(1.7f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        panel.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        panel.add(
                Theme.neonHeading("EXIT CURRENT GAME?", Theme.RED, 22f),
                BorderLayout.NORTH);

        JLabel message = new JLabel(
                "<html>Your progress will be lost.<br>" +
                "This unfinished game will not be saved.</html>");
        message.setFont(Theme.ui(17f, Font.PLAIN));
        message.setForeground(Theme.TEXT);
        message.setPreferredSize(new Dimension(520, 76));
        panel.add(message, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);

        NeonButton stay = new NeonButton(
                "Keep Playing", Theme.VIOLET,
                NeonButton.Glyph.PLAY, null);
        stay.setChevron(false);
        stay.setFocusable(false);
        stay.setPreferredSize(new Dimension(240, 72));
        stay.addActionListener(e -> dialog.dispose());

        NeonButton exit = new NeonButton(
                "Exit Game", Theme.RED,
                NeonButton.Glyph.BACK, null);
        exit.setChevron(false);
        exit.setFocusable(false);
        exit.setPreferredSize(new Dimension(220, 72));
        exit.addActionListener(e -> {
            exitConfirmed[0] = true;
            dialog.dispose();
        });

        actions.add(stay);
        actions.add(exit);
        panel.add(actions, BorderLayout.SOUTH);

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ENTER"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (exitConfirmed[0]) {
            onBackToMenu.run();
        }
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

    private static final class DiceView extends JComponent {

        private int value = 1;
        private double angle;
        private Color tint = new Color(250, 251, 255);
        private boolean ready;
        double pulse;

        DiceView() {
            setPreferredSize(new Dimension(72, 72));
        }

        /** A die that can be clicked to roll glows and shows a hand cursor. */
        void setReady(boolean ready) {
            this.ready = ready;
            setCursor(Cursor.getPredefinedCursor(ready ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            setToolTipText(ready ? "Click to roll" : null);
            repaint();
        }

        void setValue(int value) {
            this.value = Math.max(1, Math.min(6, value));
            this.angle = 0;
            repaint();
        }

        /** A face shown while rolling, slightly turned. */
        void showFace(int value, double angle) {
            this.value = Math.max(1, Math.min(6, value));
            this.angle = angle;
            repaint();
        }

        void setTint(Color tint) {
            this.tint = tint;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int size = Math.min(getWidth(), getHeight()) - 14;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            if (ready) {
                int alpha = (int) (130 + 90 * Math.sin(pulse));
                g2.setColor(new Color(255, 255, 255, Math.max(30, Math.min(230, alpha))));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(x - 4, y - 4, size + 8, size + 8, 22, 22);
            }
            g2.rotate(angle, getWidth() / 2.0, getHeight() / 2.0);

            // Soft shadow
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillRoundRect(x + 3, y + 4, size, size, 18, 18);

            // Dice body
            g2.setColor(tint);
            g2.fillRoundRect(x, y, size, size, 18, 18);

            // Subtle border
            g2.setColor(new Color(210, 220, 240));
            g2.drawRoundRect(x, y, size, size, 18, 18);

            // Dice dots
            g2.setColor(new Color(32, 45, 75));

            int left = x + size / 4;
            int center = x + size / 2;
            int right = x + size * 3 / 4;

            int top = y + size / 4;
            int middle = y + size / 2;
            int bottom = y + size * 3 / 4;

            int dotSize = Math.max(7, size / 8);

            if (value == 1 || value == 3 || value == 5) {
                drawDot(g2, center, middle, dotSize);
            }

            if (value >= 2) {
                drawDot(g2, left, top, dotSize);
                drawDot(g2, right, bottom, dotSize);
            }

            if (value >= 4) {
                drawDot(g2, right, top, dotSize);
                drawDot(g2, left, bottom, dotSize);
            }

            if (value == 6) {
                drawDot(g2, left, middle, dotSize);
                drawDot(g2, right, middle, dotSize);
            }

            g2.dispose();
        }

        private void drawDot(Graphics2D g, int x, int y, int size) {
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
    }
    private static final class FancyButton extends JButton {
    	private Color background;
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
            setFocusable(false);   // Space is for the dice, not for the focused button
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovering=true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovering=false; repaint(); }
            });
        }
        void setButtonColor(Color color) {
            this.background = color;
            repaint();
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
    /** A bouncing arrow that points right, at the dice, until the player has rolled once. */
    private static final class PointerArrow extends JComponent {
        private boolean active;
        private Color color = GOLD;
        double bounce;

        PointerArrow() { setPreferredSize(new Dimension(44, 66)); }

        void setActive(boolean active) { this.active = active; repaint(); }
        void setColor(Color color) { this.color = color; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            if (!active) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int shift = (int) (6 * Math.sin(bounce));
            int midY = getHeight() / 2 - 6, tip = getWidth() - 4 + shift;
            java.awt.geom.Path2D arrow = new java.awt.geom.Path2D.Double();
            arrow.moveTo(tip, midY);
            arrow.lineTo(tip - 16, midY - 14);
            arrow.lineTo(tip - 16, midY - 6);
            arrow.lineTo(tip - 34, midY - 6);
            arrow.lineTo(tip - 34, midY + 6);
            arrow.lineTo(tip - 16, midY + 6);
            arrow.lineTo(tip - 16, midY + 14);
            arrow.closePath();
            g2.setColor(color);
            g2.fill(arrow);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(arrow);
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

        private Piece hopPiece;
        private int hopProgress;
        private double pulse;
        /** Repaints only while a piece is waiting to be chosen, so the board is idle otherwise. */
        private final Timer pulseTimer;

        /** Pieces that can be clicked this frame: bounds and the piece index (0-3) of the current player. */
        private final List<Rectangle> hitBoxes = new ArrayList<>();
        private final List<Integer> hitIndexes = new ArrayList<>();
        private boolean pointAtPieces;
        private java.util.function.IntConsumer onPieceClicked = index -> { };
        /** The current player's pieces that cannot move with this roll; a click explains why. */
        private final List<Rectangle> stuckBoxes = new ArrayList<>();
        private final List<Integer> stuckIndexes = new ArrayList<>();
        private java.util.function.IntConsumer onStuckPieceClicked = index -> { };

        /** Bonus: a short message shown for a few seconds right above the square where something happened. */
        private static final class Bubble {
            final Piece piece; final int progress; final String text; final Color accent; final long born;
            Bubble(Piece piece, int progress, String text, Color accent) {
                this.piece = piece; this.progress = progress; this.text = text; this.accent = accent;
                born = System.currentTimeMillis();
            }
        }
        private static final long BUBBLE_MILLIS = 3200;
        private final List<Bubble> bubbles = new ArrayList<>();
        private final Timer bubbleTimer = new Timer(40, e -> {
            long now = System.currentTimeMillis();
            bubbles.removeIf(b -> now - b.born > BUBBLE_MILLIS);
            if (bubbles.isEmpty()) ((Timer) e.getSource()).stop();
            repaint();
        });

        BoardCanvas(Board board, GameConfig config, Game game) {
            this.game = game;
            this.board = board;
            this.config = config;
            // Created after game is set: Eclipse rejects a field initializer that reads game.
            pulseTimer = new Timer(90, e -> {
                if (hopPiece == null && (game.isAwaitingPiece() || game.isAwaitingSurpriseTarget())) { pulse += .35; repaint(); }
            });
            setOpaque(false);
            setPreferredSize(new Dimension(620, 520));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int index = pieceAt(e.getPoint());
                    if (index >= 0) { onPieceClicked.accept(index); return; }
                    for (int i = stuckBoxes.size() - 1; i >= 0; i--)
                        if (stuckBoxes.get(i).contains(e.getPoint())) { onStuckPieceClicked.accept(stuckIndexes.get(i)); return; }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(pieceAt(e.getPoint()) >= 0
                            ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            });
        }

        void setOnPieceClicked(java.util.function.IntConsumer listener) { onPieceClicked = listener; }
        void setOnStuckPieceClicked(java.util.function.IntConsumer listener) { onStuckPieceClicked = listener; }

        /** Shows {@code text} above the square of {@code piece} at route position {@code progress} (-1: its castle place). */
        void say(Piece piece, int progress, String text, Color accent) {
            bubbles.removeIf(b -> b.piece == piece && b.progress == progress);
            bubbles.add(new Bubble(piece, progress, text, accent));
            if (bubbles.size() > 3) bubbles.remove(0);
            bubbleTimer.start();
            repaint();
        }

        /** A current-player piece that cannot move: remember where it is, so a click on it can be explained. */
        private void addStuckBox(Game.Player player, Piece piece, int cx, int cy, int size) {
            if (hopPiece != null || !game.isAwaitingPiece() || player != game.getCurrentPlayer()
                    || game.canMove(piece.getNumber() - 1) || piece.isFinished()) return;
            int h = size, w = (int) (size * .78), pad = Math.max(4, size / 5), baseY = cy + h / 2;
            stuckBoxes.add(new Rectangle(cx - w / 2 - pad, baseY - h - pad, w + 2 * pad, h + 2 * pad));
            stuckIndexes.add(piece.getNumber() - 1);
        }
        void setPointAtPieces(boolean point) { pointAtPieces = point; }

        /** The last drawn piece is on top, so search backwards. */
        private int pieceAt(Point p) {
            for (int i = hitBoxes.size() - 1; i >= 0; i--)
                if (hitBoxes.get(i).contains(p)) return hitIndexes.get(i);
            return -1;
        }

        @Override public void addNotify() { super.addNotify(); pulseTimer.start(); }
        @Override public void removeNotify() { pulseTimer.stop(); bubbleTimer.stop(); super.removeNotify(); }

        void startHop(Piece piece, int fromProgress) { hopPiece = piece; hopProgress = fromProgress; repaint(); }
        /** Moves one square towards the target (forward or back); returns false once it is there. */
        boolean hopToward(int target) {
            if (hopPiece == null || hopProgress == target) return false;
            hopProgress += target > hopProgress ? 1 : -1;
            repaint();   // the board is not redrawn on its own while nothing waits for a click
            return true;
        }
        void endHop() { hopPiece = null; repaint(); }

        /**
         * Click code for a piece, or -1: during a move it is the piece index (0-3) of the current
         * player; while the "move an opponent back" surprise waits, it is player * 4 + piece.
         */
        private int clickIndex(Game.Player player, Piece piece) {
            if (hopPiece != null) return -1;
            if (game.isAwaitingSurpriseTarget()) {
                int p = game.getPlayers().indexOf(player);
                return game.canSelectSurpriseTarget(p, piece.getNumber() - 1) ? p * 4 + piece.getNumber() - 1 : -1;
            }
            return isMovable(player, piece) ? piece.getNumber() - 1 : -1;
        }

        private boolean isMovable(Game.Player player, Piece piece) {
            return hopPiece == null && game.isAwaitingPiece() && player == game.getCurrentPlayer()
                    && game.canMove(piece.getNumber() - 1);
        }

        /**
         * Pin-shaped piece: shadow, pointed body, white head. A piece the current player can
         * move (clickIndex 0-3) glows, is clickable, and gets an arrow while the player is learning.
         */
        private void drawPin(Graphics2D g, int cx, int cy, int size, Color color, int clickIndex, double lift) {
            int h = size, w = (int) (size * .78);
            boolean glow = clickIndex >= 0;
            if (glow) lift += Math.abs(Math.sin(pulse)) * size * .22; // movable pieces bob up and down
            int baseY = cy + h / 2 - (int) lift;
            if (glow) {
                int r = (int) (size * (.68 + .1 * Math.sin(pulse)));
                g.setColor(new Color(255, 255, 255, 90));
                g.fillOval(cx - r, cy + h / 2 - r / 2, 2 * r, r);
                g.setColor(new Color(255, 255, 255, 230));
                g.setStroke(new BasicStroke(Math.max(2.5f, size / 7f)));
                g.drawOval(cx - r, cy + h / 2 - r / 2, 2 * r, r);
                int pad = Math.max(4, size / 5);
                hitBoxes.add(new Rectangle(cx - w / 2 - pad, baseY - h - pad, w + 2 * pad, h + 2 * pad));
                hitIndexes.add(clickIndex);
                if (pointAtPieces) drawDownArrow(g, cx, (int) (baseY - h - size * .25 - Math.abs(Math.sin(pulse)) * size * .3), size, color);
            }
            g.setColor(new Color(0, 0, 0, 70));
            g.fillOval(cx - w / 2, cy + h / 2 - h / 10, w, h / 5);
            drawCharacter(g, cx, baseY, (int) (size * .9), h, color);
        }

        /**
         * Bonus: each colour's pieces are a little character, so pieces are told apart by colour
         * and by shape: red robot, green frog, yellow chick, blue cat. Drawn in code (no image files).
         * The character stands on (cx, baseY) and is w wide and h tall.
         */
        private void drawCharacter(Graphics2D g, int cx, int baseY, int w, int h, Color color) {
            Graphics2D c = (Graphics2D) g.create();
            c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color dark = color.darker().darker();
            float line = Math.max(1.1f, h / 22f);
            double top = baseY - h;
            double eyeY;
            double eyeGap = w * .2, eye = Math.max(3, w * .26);
            if (color.equals(playerColor(PlayerColor.RED))) {                 // robot
                c.setColor(dark);
                c.setStroke(new BasicStroke(line));
                c.drawLine(cx, (int) (top + h * .02), cx, (int) (top + h * .16));
                c.fillOval((int) (cx - h * .05), (int) (top - h * .02), (int) (h * .1), (int) (h * .1));
                java.awt.geom.RoundRectangle2D head = new java.awt.geom.RoundRectangle2D.Double(cx - w * .4, top + h * .14, w * .8, h * .44, h * .14, h * .14);
                java.awt.geom.RoundRectangle2D body = new java.awt.geom.RoundRectangle2D.Double(cx - w * .32, top + h * .6, w * .64, h * .4, h * .1, h * .1);
                fillOutlined(c, body, color, dark, line);
                fillOutlined(c, head, color, dark, line);
                eyeY = top + h * .36;
                c.setColor(new Color(255, 255, 255, 200));
                c.fillRoundRect((int) (cx - w * .14), (int) (top + h * .72), (int) (w * .28), (int) (h * .12), 3, 3);
            } else if (color.equals(playerColor(PlayerColor.GREEN))) {        // frog
                java.awt.geom.Ellipse2D body = new java.awt.geom.Ellipse2D.Double(cx - w * .5, top + h * .3, w, h * .7);
                fillOutlined(c, new java.awt.geom.Ellipse2D.Double(cx - eyeGap - eye * .8, top + h * .12, eye * 1.6, eye * 1.6), color, dark, line);
                fillOutlined(c, new java.awt.geom.Ellipse2D.Double(cx + eyeGap - eye * .8, top + h * .12, eye * 1.6, eye * 1.6), color, dark, line);
                fillOutlined(c, body, color, dark, line);
                eyeY = top + h * .12 + eye * .8;
                c.setColor(dark);
                c.setStroke(new BasicStroke(line));
                c.drawArc((int) (cx - w * .22), (int) (top + h * .5), (int) (w * .44), (int) (h * .22), 200, 140);
            } else if (color.equals(playerColor(PlayerColor.YELLOW))) {       // chick
                c.setColor(dark);
                c.setStroke(new BasicStroke(line));
                c.drawArc((int) (cx - w * .06), (int) (top), (int) (w * .16), (int) (h * .2), 90, 180);
                c.drawArc((int) (cx - w * .02), (int) (top + h * .02), (int) (w * .16), (int) (h * .18), 90, 180);
                java.awt.geom.Ellipse2D body = new java.awt.geom.Ellipse2D.Double(cx - w * .45, top + h * .12, w * .9, h * .88);
                fillOutlined(c, body, color, dark, line);
                eyeY = top + h * .4;
                java.awt.geom.Path2D beak = new java.awt.geom.Path2D.Double();
                beak.moveTo(cx - w * .1, top + h * .52);
                beak.lineTo(cx + w * .1, top + h * .52);
                beak.lineTo(cx, top + h * .66);
                beak.closePath();
                c.setColor(new Color(245, 140, 40));
                c.fill(beak);
            } else {                                                          // cat
                java.awt.geom.Path2D ears = new java.awt.geom.Path2D.Double();
                ears.moveTo(cx - w * .42, top + h * .38);
                ears.lineTo(cx - w * .34, top + h * .02);
                ears.lineTo(cx - w * .08, top + h * .24);
                ears.closePath();
                ears.moveTo(cx + w * .42, top + h * .38);
                ears.lineTo(cx + w * .34, top + h * .02);
                ears.lineTo(cx + w * .08, top + h * .24);
                ears.closePath();
                fillOutlined(c, ears, color, dark, line);
                java.awt.geom.Ellipse2D body = new java.awt.geom.Ellipse2D.Double(cx - w * .45, top + h * .16, w * .9, h * .84);
                fillOutlined(c, body, color, dark, line);
                eyeY = top + h * .42;
                c.setColor(dark);
                c.setStroke(new BasicStroke(Math.max(1f, line * .8f)));
                for (int side = -1; side <= 1; side += 2) {
                    c.drawLine((int) (cx + side * w * .12), (int) (top + h * .6), (int) (cx + side * w * .4), (int) (top + h * .56));
                    c.drawLine((int) (cx + side * w * .12), (int) (top + h * .64), (int) (cx + side * w * .4), (int) (top + h * .66));
                }
                c.fillOval((int) (cx - w * .05), (int) (top + h * .55), (int) (w * .1), (int) (w * .07));
            }
            // eyes: white with a dark pupil, the same on every character
            for (int side = -1; side <= 1; side += 2) {
                double ex = cx + side * eyeGap;
                c.setColor(Color.WHITE);
                c.fill(new java.awt.geom.Ellipse2D.Double(ex - eye / 2, eyeY - eye / 2, eye, eye));
                c.setColor(new Color(25, 30, 45));
                double pupil = eye * .5;
                c.fill(new java.awt.geom.Ellipse2D.Double(ex - pupil / 2 + eye * .08, eyeY - pupil / 2, pupil, pupil));
            }
            c.dispose();
        }

        private static void fillOutlined(Graphics2D g, java.awt.Shape shape, Color fill, Color outline, float line) {
            g.setColor(fill);
            g.fill(shape);
            g.setColor(outline);
            g.setStroke(new BasicStroke(line));
            g.draw(shape);
        }

        /** Arrow whose tip is at (cx, tipY), pointing down at a piece. */
        private void drawDownArrow(Graphics2D g, int cx, int tipY, int size, Color color) {
            double s = Math.max(14, size * .7);
            java.awt.geom.Path2D arrow = new java.awt.geom.Path2D.Double();
            arrow.moveTo(cx, tipY);
            arrow.lineTo(cx - s * .5, tipY - s * .5);
            arrow.lineTo(cx - s * .2, tipY - s * .5);
            arrow.lineTo(cx - s * .2, tipY - s);
            arrow.lineTo(cx + s * .2, tipY - s);
            arrow.lineTo(cx + s * .2, tipY - s * .5);
            arrow.lineTo(cx + s * .5, tipY - s * .5);
            arrow.closePath();
            g.setColor(color);
            g.fill(arrow);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.draw(arrow);
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
            hitBoxes.clear();
            hitIndexes.clear();
            stuckBoxes.clear();
            stuckIndexes.clear();
            int extra = board.getRings().size() - 1;

            int maxCell = extra == 0 ? 48 : 39;
            int gridSize = extra == 0 ? 15 : 19;

            int cell = Math.max(10, Math.min(maxCell,
                    Math.min((getWidth() - 100) / gridSize,
                             (getHeight() - 25) / gridSize)));
            int size = 15 * cell;
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            // Draw the actual outer tracks used by the movement model.
            if (extra >= 1) drawOuterTrack(g, board.getRings().get(1), left - cell * 1.0, top - cell * 1.0, size + 2.0 * cell, cell * .64);
            if (extra >= 2) drawOuterTrack(g, board.getRings().get(2), left - cell * 2.0, top - cell * 2.0, size + 4.0 * cell, cell * .56);

            // Home castles are always shown; pieces only for selected player colors.
            drawCastle(g, left, top, cell, 0, 0, PlayerColor.RED);
            drawCastle(g, left, top, cell, 9, 0, PlayerColor.GREEN);
            drawCastle(g, left, top, cell, 9, 9, PlayerColor.YELLOW);
            drawCastle(g, left, top, cell, 0, 9, PlayerColor.BLUE);

            // Central finish home.
            int c = left + 6 * cell, d = top + 6 * cell;
            g.setColor(new Color(32, 39, 65));
            g.fillRoundRect(c, d, 3 * cell, 3 * cell, 9, 9);
            drawHomePath(g, left, top, cell, PlayerColor.RED);
            drawHomePath(g, left, top, cell, PlayerColor.GREEN);
            drawHomePath(g, left, top, cell, PlayerColor.YELLOW);
            drawHomePath(g, left, top, cell, PlayerColor.BLUE);

            // The label goes on top of the home paths' last squares, on a dark pill so it stays readable.
            int finishSize = Math.max(10, cell / 3);
            g.setFont(new Font("SansSerif", Font.BOLD, finishSize));
            while (finishSize > 7 && g.getFontMetrics().stringWidth("FINISH") > 3 * cell - 10)
                g.setFont(new Font("SansSerif", Font.BOLD, --finishSize));
            FontMetrics fm = g.getFontMetrics();
            int pillW = fm.stringWidth("FINISH") + 10, pillH = fm.getHeight() + 2;
            g.setColor(new Color(32, 39, 65));
            g.fillRoundRect(c + 3 * cell / 2 - pillW / 2, d + 3 * cell / 2 - pillH / 2, pillW, pillH, pillH, pillH);
            g.setColor(Color.WHITE);
            g.drawString("FINISH", c + 3 * cell / 2 - fm.stringWidth("FINISH") / 2,
                    d + 3 * cell / 2 + fm.getAscent() / 2 - 1);

            List<Board.Square> base = board.getRings().get(0);
            for (int i = 0; i < 52; i++) {
                int[] point = PATH[i];
                Board.Square square = base.get(i);
                drawCell(g, left + point[0] * cell, top + point[1] * cell,
                    cell, squareColor(square), symbol(square));
                if (square.getType() == Board.SquareType.ARROW) {
                    // Points along the arm toward the centre, i.e. into the home path.
                    int dx = point[0] < 6 ? 1 : point[0] > 8 ? -1 : 0;
                    int dy = point[1] < 6 ? 1 : point[1] > 8 ? -1 : 0;
                    drawArrowGlyph(g, left + point[0] * cell + cell / 2, top + point[1] * cell + cell / 2, cell, dx, dy);
                }
            }
            drawPieces(g, left, top, cell);
            drawBubbles(g, left, top, cell);
            g.dispose();
        }

        private void drawPieces(Graphics2D g, int left, int top, int cell) {
            // Pieces that share a square (any colour) are drawn smaller, side by side, so none hides another.
            java.util.Map<String, java.util.List<Piece>> sharing = new java.util.HashMap<>();
            for (Game.Player player : game.getPlayers())
                for (Piece piece : player.getPieces())
                    if (piece != hopPiece && piece.getProgress() >= 0 && !piece.isFinished())
                        sharing.computeIfAbsent(squareKey(piece, piece.getProgress()), k -> new java.util.ArrayList<>()).add(piece);
            for (Game.Player player : game.getPlayers()) {
                for (Piece piece : player.getPieces()) {
                    int progress = piece == hopPiece ? hopProgress : piece.getProgress();
                    if (progress < 0) continue;
                    int click = clickIndex(player, piece);
                    double lift = piece == hopPiece ? cell * .35 : 0;
                    Color color = playerColor(piece.getColor());
                    if (progress >= piece.getFinishProgress()) {
                        double[] center = finishedPieceCenter(piece, left, top, cell);
                        drawPin(g, (int) center[0], (int) center[1],
                                Math.max(9, cell / 3), color, -1, 0);
                        continue;
                    }
                    double cx, cy;
                    int size;
                    int ring = piece.ringAt(progress);
                    if (ring > 0) {
                        double inset = ring == 1 ? 1.0 : 2.0;
                        double side = 15 * cell + 2 * cell * inset;
                        double[] pos = outerPoint(piece.indexAt(progress), board.getRings().get(ring).size(),
                                left - cell * inset, top - cell * inset, side);
                        cx = pos[0]; cy = pos[1]; size = Math.max(12, cell * 2 / 3);
                    } else {
                        int[] point = progress >= piece.getCommonLength()
                            ? homeCoordinates(piece.getColor(), progress - piece.getCommonLength())
                            : PATH[piece.indexAt(progress)];
                        cx = left + point[0] * cell + cell / 2.0;
                        cy = top + point[1] * cell + cell / 2.0;
                        size = Math.max(14, cell * 5 / 6);
                    }
                    java.util.List<Piece> here = piece == hopPiece ? null : sharing.get(squareKey(piece, progress));
                    if (here != null && here.size() > 1) {
                        int slot = here.indexOf(piece), n = here.size();
                        double dx = n == 2 ? (slot == 0 ? -.22 : .22) : (slot % 2 == 0 ? -.22 : .22);
                        double dy = n == 2 ? 0 : (slot < 2 ? -.2 : .2);
                        cx += dx * cell;
                        cy += dy * cell;
                        size = Math.max(10, (int) (size * .62));
                    }
                    drawPin(g, (int) cx, (int) cy, size, color, click, lift);
                    if (click < 0) addStuckBox(player, piece, (int) cx, (int) cy, size);
                }
            }
            drawDoubleBlocks(g, left, top, cell);
        }

        /** Identifies the square a piece stands on (track square, or a square of its home path). */
        private static String squareKey(Piece piece, int progress) {
            if (progress >= piece.getCommonLength()) return "home:" + piece.getColor() + ":" + (progress - piece.getCommonLength());
            return "track:" + piece.ringAt(progress) + ":" + piece.indexAt(progress);
        }

        /**
         * Two or more pieces of one player on a common-path square form a Double Block:
         * the square gets a frame in the player's colour and a badge with the number of pieces.
         */
        private void drawDoubleBlocks(Graphics2D g, int left, int top, int cell) {
            for (Game.Player player : game.getPlayers()) {
                java.util.Map<Integer, Integer> count = new java.util.HashMap<>();
                for (Piece piece : player.getPieces())
                    if (piece != hopPiece && piece.isOnCommonPath())
                        count.merge(piece.getRing() * 1000 + piece.getBoardIndex(), 1, Integer::sum);
                for (java.util.Map.Entry<Integer, Integer> e : count.entrySet()) {
                    if (e.getValue() < 2) continue;
                    int ring = e.getKey() / 1000, index = e.getKey() % 1000;
                    double cx, cy, size;
                    if (ring == 0) {
                        cx = left + PATH[index][0] * cell + cell / 2.0;
                        cy = top + PATH[index][1] * cell + cell / 2.0;
                        size = cell;
                    } else {
                        double inset = ring == 1 ? 1.0 : 2.0;
                        double side = 15 * cell + 2 * cell * inset;
                        int n = board.getRings().get(ring).size();
                        double[] pos = outerPoint(index, n, left - cell * inset, top - cell * inset, side);
                        cx = pos[0]; cy = pos[1]; size = side / (n / 4 + 1);
                    }
                    Color color = playerColor(player.getColor());
                    g.setStroke(new BasicStroke(Math.max(2.5f, (float) size / 9f)));
                    g.setColor(color.darker());
                    g.drawRoundRect((int) (cx - size / 2), (int) (cy - size / 2), (int) size, (int) size, 8, 8);
                    int badge = (int) Math.max(14, size * .45);
                    int bx = (int) (cx + size / 2 - badge * .7), by = (int) (cy - size / 2 - badge * .3);
                    g.setColor(Color.WHITE);
                    g.fillOval(bx, by, badge, badge);
                    g.setColor(color.darker());
                    g.setStroke(new BasicStroke(2f));
                    g.drawOval(bx, by, badge, badge);
                    g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, badge * 2 / 3)));
                    drawCentered(g, "\u00d7" + e.getValue(), bx + badge / 2, by + badge * 3 / 4);
                }
            }
        }
        private static double[] finishedPieceCenter(Piece piece, int left, int top, int cell) {
            int lastHomeIndex = piece.getFinishProgress() - piece.getCommonLength();
            int[] tile = homeCoordinates(piece.getColor(), lastHomeIndex);
            int slot = piece.getNumber() - 1;

            double x = tile[0] + 0.28 + (slot % 2) * 0.44;
            double y = tile[1] + 0.28 + (slot / 2) * 0.44;
            return new double[] {left + x * cell, top + y * cell};
        }
        /** Centre of the square a piece stands on at {@code progress}; -1 is its place in the castle. */
        private double[] squareCenter(Piece piece, int progress, int left, int top, int cell) {
            if (progress < 0) {
                PlayerColor color = piece.getColor();
                int x = color == PlayerColor.GREEN || color == PlayerColor.YELLOW ? 9 : 0;
                int y = color == PlayerColor.YELLOW || color == PlayerColor.BLUE ? 9 : 0;
                int i = piece.getNumber() - 1;
                return new double[]{left + (x + (i % 2 == 0 ? 2 : 4)) * cell, top + (y + (i / 2 == 0 ? 2 : 4)) * cell};
            }
            if (progress >= piece.getFinishProgress()) {
                return finishedPieceCenter(piece, left, top, cell);
            }
            int ring = piece.ringAt(progress);
            if (ring > 0) {
                double inset = ring == 1 ? 1.0 : 2.0;
                return outerPoint(piece.indexAt(progress), board.getRings().get(ring).size(),
                        left - cell * inset, top - cell * inset, 15 * cell + 2 * cell * inset);
            }
            int[] point = progress >= piece.getCommonLength()
                    ? homeCoordinates(piece.getColor(), progress - piece.getCommonLength())
                    : PATH[piece.indexAt(progress)];
            return new double[]{left + point[0] * cell + cell / 2.0, top + point[1] * cell + cell / 2.0};
        }

        /** Each message is a white rounded box with a pointer down to its square; it pops in and fades out. */
        private void drawBubbles(Graphics2D g, int left, int top, int cell) {
            long now = System.currentTimeMillis();
            Font font = new Font("SansSerif", Font.BOLD, Math.max(11, Math.min(15, cell / 3)));
            List<Rectangle> placed = new ArrayList<>();   // later messages move aside instead of covering earlier ones
            for (Bubble b : bubbles) {
                long age = now - b.born;
                float alpha = age < 150 ? age / 150f : age > BUBBLE_MILLIS - 450 ? Math.max(0, (BUBBLE_MILLIS - age) / 450f) : 1f;
                double rise = age < 150 ? (1 - age / 150.0) * 8 : 0;
                double[] at = squareCenter(b.piece, b.progress, left, top, cell);
                Graphics2D c = (Graphics2D) g.create();
                c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
                c.setFont(font);
                FontMetrics fm = c.getFontMetrics();
                int padX = 10, w = fm.stringWidth(b.text) + 2 * padX + 8, h = fm.getHeight() + 10, tip = 7;
                int tipX = (int) at[0], tipY = (int) (at[1] - cell * .42 + rise);
                int x = Math.max(4, Math.min(getWidth() - w - 4, tipX - w / 2));
                int y = tipY - tip - h;
                if (y < 4) {               // no room above (top row): show it below the square instead
                    tipY = (int) (at[1] + cell * .42 - rise);
                    y = tipY + tip;
                }
                boolean below = y > tipY;
                for (int tries = 0; tries < 6; tries++) {
                    Rectangle box = new Rectangle(x, y, w, h);
                    Rectangle hit = null;
                    for (Rectangle r : placed) if (r.intersects(box)) hit = r;
                    if (hit == null) break;
                    y = below ? hit.y + hit.height + 4 : hit.y - h - 4;
                    if (y < 4) { below = true; y = hit.y + hit.height + 4; }
                }
                placed.add(new Rectangle(x, y, w, h));
                java.awt.geom.Path2D pointer = new java.awt.geom.Path2D.Double();
                int px = Math.max(x + 10, Math.min(x + w - 10, tipX));
                int baseLine = below ? y + 1 : y + h - 1;
                int half = Math.abs(baseLine - tipY) > 2 * tip ? 3 : tip;   // moved aside: a narrow stem
                pointer.moveTo(px - half, baseLine);
                pointer.lineTo(px + half, baseLine);
                pointer.lineTo(px, tipY);
                pointer.closePath();
                c.setColor(new Color(0, 0, 0, 60));
                c.fillRoundRect(x + 2, y + 3, w, h, h, h);
                c.setColor(Color.WHITE);
                c.fillRoundRect(x, y, w, h, h, h);
                c.fill(pointer);
                c.setColor(b.accent);
                c.setStroke(new BasicStroke(2.2f));
                c.drawRoundRect(x, y, w, h, h, h);
                c.fillOval(x + padX - 2, y + h / 2 - 4, 8, 8);
                c.setColor(new Color(25, 30, 45));
                c.drawString(b.text, x + padX + 10, y + (h - fm.getHeight()) / 2 + fm.getAscent());
                c.dispose();
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
                g.setColor(new Color(0, 0, 0, 35));
                g.fillOval(tx - radius, ty - radius, radius * 2, radius * 2);
                if (!active) {
                    g.setColor(new Color(180, 185, 195));
                    g.fillOval(tx - radius, ty - radius, radius * 2, radius * 2);
                    continue;
                }
                int click = -1;
                for (Game.Player player : game.getPlayers())
                    if (player.getColor() == color && isMovable(player, player.getPieces().get(i))) click = i;
                drawPin(g, tx, ty - radius / 3, Math.max(14, cell * 5 / 6), playerColor(color), click, 0);
                if (click < 0)
                    for (Game.Player player : game.getPlayers())
                        if (player.getColor() == color)
                            addStuckBox(player, player.getPieces().get(i), tx, ty - radius / 3, Math.max(14, cell * 5 / 6));
            }
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, cell / 3)));
            g.setColor(Color.WHITE);
            for (Game.Player player : game.getPlayers()) {
                if (player.getColor() == color) {
                    drawCentered(g, player.getName().toUpperCase(),
                            px + 3 * cell, py + (int)(.7 * cell));
                    break;
                }
            }
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
            // perSide + 1 cells per side: each corner square belongs to one side only, so no two
            // squares are drawn on top of each other (64 -> 17 x 17 cells, 72 -> 19 x 19 cells).
            double step = size / (perSide + 1);
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
            int cellSize = Math.max(6, (int)Math.round(size / (n / 4 + 1)) - 2);
            for (int i = 0; i < n; i++) {
                double[] point = outerPoint(i, n, x, y, size);
                Board.Square square = ring.get(i);
                int cx = (int)Math.round(point[0]), cy = (int)Math.round(point[1]);
                g.setColor(squareColor(square));
                g.fillRoundRect(cx - cellSize/2, cy - cellSize/2, cellSize, cellSize, 4, 4);
                g.setColor(OUTLINE);
                g.drawRoundRect(cx - cellSize/2, cy - cellSize/2, cellSize, cellSize, 4, 4);
                if (square.getType() == Board.SquareType.ARROW) {
                    // Points from its side of the outer track toward the next inner track.
                    int side = i / (n / 4);
                    int[][] inward = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
                    drawArrowGlyph(g, cx, cy, cellSize, inward[side][0], inward[side][1]);
                }
                String label = symbol(square);
                if (!label.isEmpty()) {
                    g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, cellSize / 2)));
                    g.setColor(new Color(18, 28, 48));
                    drawCentered(g, label, cx, cy + cellSize/5);
                }
            }
        }

        /** A dark arrow centred on (cx, cy), pointing in direction (dx, dy). */
        private void drawArrowGlyph(Graphics2D g, int cx, int cy, int cell, int dx, int dy) {
            double s = cell * .32;
            java.awt.geom.Path2D arrow = new java.awt.geom.Path2D.Double();
            arrow.moveTo(s, 0);
            arrow.lineTo(0, -s * .8);
            arrow.lineTo(0, -s * .3);
            arrow.lineTo(-s, -s * .3);
            arrow.lineTo(-s, s * .3);
            arrow.lineTo(0, s * .3);
            arrow.lineTo(0, s * .8);
            arrow.closePath();
            Graphics2D a = (Graphics2D) g.create();
            a.translate(cx, cy);
            a.rotate(Math.atan2(dy, dx));
            a.setColor(new Color(24, 32, 51));
            a.fill(arrow);
            a.dispose();
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
                case QUESTION_EASY: return new Color(184, 240, 204);
                case QUESTION_MEDIUM: return new Color(255, 230, 161);
                case QUESTION_HARD: return new Color(248, 175, 192);
                case SURPRISE: return new Color(227, 136, 198);
                default: return EMPTY;
            }
        }
        private String symbol(Board.Square s) {
            switch (s.getType()) {
                case START: return "#";
                case ARROW: return ""; // drawn by drawArrowGlyph, pointing the right way
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
