package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Turn management, multi-track movement, questions, captures, blocks and surprise events. */
public final class Game {
    public static final class Player {
        private final GameConfig.PlayerSetup setup;
        private final List<Piece> pieces = new ArrayList<>();
        private boolean skipNextTurn;
        private Player(GameConfig.PlayerSetup setup, Difficulty difficulty) {
            this.setup = setup;
            for (int i = 1; i <= 4; i++) pieces.add(new Piece(setup.getColor(), i, difficulty));
        }
        public String getName() { return setup.getName(); }
        public PlayerColor getColor() { return setup.getColor(); }
        public List<Piece> getPieces() { return Collections.unmodifiableList(pieces); }
        /** Bonus: a computer player, whose turns are chosen by {@link Bot}. */
        public boolean isBot() { return setup.isBot(); }
    }

    private final Board board;
    private final Difficulty difficulty;
    private final boolean teamMode;
    private int winningTeam = -1;
    private final Random random;
    private final List<Player> players = new ArrayList<>();
    private final long startedAtMillis = System.currentTimeMillis();
    private final List<Player> finishingOrder = new ArrayList<>();
    private boolean gameOver;
    private long finishedAtMillis;
    private int current;
    private int consecutiveSixes;
    private int stepDie;
    private int extraDie;
    private int steps;
    private boolean awaitingPiece;
    private boolean rollAvailable;
    private String message = "Roll the dice to begin.";
    private QuestionBank questionBank;
    private Question pendingQuestion;
    private Piece questionPiece;
    private int questionChain;
    public enum SurpriseEvent {
        DIRECT_FINISH, OPPONENT_BACK, RETURN_CASTLE, BACK_THREE, SKIP_NEXT_TURN, BONUS_ULTRABOOST
    }
    private SurpriseEvent lastSurprise;
    private boolean awaitingSurpriseTarget;
    private boolean bonusRollReady;
    private boolean currentRollIsBonus;
    private int specialChain;
    public SurpriseEvent getLastSurprise() { return lastSurprise; }
    /** Where the last moved piece landed from the dice, before any surprise moved it again. */
    public int getLastLanding() { return lastLanding; }
    private int lastLanding = -1;
    public boolean isAwaitingSurpriseTarget() { return awaitingSurpriseTarget; }
    public boolean isBonusRollReady() { return bonusRollReady; }
    public boolean canSelectSurpriseTarget(int playerIndex, int pieceIndex) {
        if (!awaitingSurpriseTarget || playerIndex < 0 || playerIndex >= players.size() || (playerIndex == current || areTeammates(players.get(playerIndex).getColor(), getCurrentPlayer().getColor()))
                || pieceIndex < 0 || pieceIndex >= 4) return false;
        Piece candidate = players.get(playerIndex).pieces.get(pieceIndex);
        return canMoveBackward(candidate);
    }


    /** Attach the bank at game start; no question window is shown in Easy mode. */
    public void setQuestionBank(QuestionBank bank) { this.questionBank = bank; }
    public Question getPendingQuestion() { return pendingQuestion; }


    public Game(GameConfig config, Board board) { this(config, board, new Random()); }
    public Game(GameConfig config, Board board, Random random) {
        if (config == null || board == null || random == null || board.getDifficulty() != config.getDifficulty())
            throw new IllegalArgumentException("Configuration, matching board, and random are required");
        this.board = board;
        this.difficulty = config.getDifficulty();
        this.teamMode = config.isTeamMode();
        this.random = random;
        for (GameConfig.PlayerSetup p : config.getPlayers()) players.add(new Player(p, difficulty));
        Collections.shuffle(players, random);
    }
    public boolean isTeamMode() { return teamMode; }
    public int getWinningTeam() { return winningTeam; }
    public int getTeamOf(PlayerColor color) { return GameConfig.teamOf(color); }
    private boolean areTeammates(PlayerColor a, PlayerColor b) {
        return teamMode && GameConfig.teamOf(a) == GameConfig.teamOf(b);
    }
    public boolean isGameOver() { return gameOver; }
    public long getStartedAtMillis() { return startedAtMillis; }
    public long getFinishedAtMillis() { return finishedAtMillis; }
    public long getDurationSeconds() {
        long end = gameOver ? finishedAtMillis : System.currentTimeMillis();
        return Math.max(0, (end - startedAtMillis) / 1000);
    }
    public List<Player> getFinishingOrder() { return Collections.unmodifiableList(finishingOrder); }
    public Difficulty getDifficulty() { return difficulty; }
    public Board getBoard() { return board; }
    
    // Bonus: the move log. Every roll, move, capture, answer and surprise adds one entry (newest last).
    private final List<LogEntry> log = new ArrayList<>();
    public List<LogEntry> getLog() { return Collections.unmodifiableList(log); }

    // Bonus: the trophies; they are checked after every log entry.
    private final AchievementTracker achievements = new AchievementTracker();
    public AchievementTracker getAchievements() { return achievements; }

    private void log(Player player, LogEntry.Kind kind, String text) {
        log(player, kind, text, null);
    }

    /** Adds a log entry; {@code other} is the other player involved (e.g. the captured one), or null. */
    private void log(Player player, LogEntry.Kind kind, String text, PlayerColor other) {
        long seconds = (System.currentTimeMillis() - startedAtMillis) / 1000;
        LogEntry entry = new LogEntry(player.getColor(), player.getName(), kind, text, seconds, other);
        log.add(entry);
        for (AchievementTracker.Unlock u : achievements.onEvent(entry, players))
            log.add(new LogEntry(u.getColor(), u.getPlayer(), LogEntry.Kind.ACHIEVEMENT,
                    "unlocked " + u.getAchievement().getTitle(), seconds));
    }

    private Player playerOf(PlayerColor color) {
        for (Player player : players) if (player.getColor() == color) return player;
        return getCurrentPlayer();
    }

    private String rollText() {
        if (extraDie == 0) return "rolled " + stepDie;
        String die = currentRollIsBonus || difficulty == Difficulty.HARD ? "ULTRA" : "BOOST";
        return (currentRollIsBonus ? "bonus roll " : "rolled ") + stepDie + ", " + die + " " + extraDie + ": " + steps + (steps == 1 ? " step" : " steps");
    }

    private static String surpriseText(SurpriseEvent event, Piece piece) {
        switch (event) {
            case DIRECT_FINISH:    return "surprise! piece " + piece.getNumber() + " jumps to FINISH";
            case OPPONENT_BACK:    return "surprise! may send an opponent back";
            case RETURN_CASTLE:    return "surprise! piece " + piece.getNumber() + " back to the castle";
            case BACK_THREE:       return "surprise! piece " + piece.getNumber() + " moves back 3";
            case SKIP_NEXT_TURN:   return "surprise! will skip the next turn";
            case BONUS_ULTRABOOST: return "surprise! bonus UltraBoost roll";
            default:               return "surprise!";
        }
    }

    private void updateFinishingOrder() {
        if (gameOver) return;
        for (Player player : players) {
            if (!finishingOrder.contains(player)) {
                boolean allFinished = true;
                for (Piece piece : player.pieces) if (!piece.isFinished()) { allFinished = false; break; }
                if (allFinished) {
                    finishingOrder.add(player);
                    log(player, LogEntry.Kind.WIN, "brought all 4 pieces home (#" + finishingOrder.size() + ")");
                }
            }
        }
        if (teamMode) {
            for (int team = 0; team < 2; team++) {
                final int candidateTeam = team;
                boolean complete = true;
                for (Player player : players) {
                    if (GameConfig.teamOf(player.getColor()) != candidateTeam) continue;
                    for (Piece piece : player.pieces) {
                        if (!piece.isFinished()) { complete = false; break; }
                    }
                }
                if (complete) {
                    winningTeam = team;
                    gameOver = true;
                    finishedAtMillis = System.currentTimeMillis();
                    awaitingPiece = false;
                    rollAvailable = false;
                    bonusRollReady = false;
                    pendingQuestion = null;
                    awaitingSurpriseTarget = false;
                    message = "Team " + (team == 0 ? "RED + YELLOW" : "GREEN + BLUE") + " wins!";
                    return;
                }
            }
            return;
        }
        if (finishingOrder.size() == players.size() - 1) {
            for (Player player : players) if (!finishingOrder.contains(player)) finishingOrder.add(player);
            gameOver = true;
            finishedAtMillis = System.currentTimeMillis();
            awaitingPiece = false;
            rollAvailable = false;
            bonusRollReady = false;
            pendingQuestion = null;
            awaitingSurpriseTarget = false;
            message = "Game finished! View the final rankings.";
        }
    }

    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public Player getCurrentPlayer() { return players.get(current); }
    public int getStepDie() { return stepDie; }
    public int getExtraDie() { return extraDie; }
    public int getSteps() { return steps; }
    public boolean isAwaitingPiece() { return awaitingPiece; }
    public String getMessage() { return message; }

    // Observer (MVC): views register here and are told after every player action.
    private final List<GameObserver> observers = new ArrayList<>();
    public void addObserver(GameObserver observer) { if (!observers.contains(observer)) observers.add(observer); }
    public void removeObserver(GameObserver observer) { observers.remove(observer); }
    private void notifyObservers() { for (GameObserver o : new ArrayList<>(observers)) o.gameChanged(this); }

    public void roll() { doRoll(); notifyObservers(); }

    private void doRoll() {
        if (gameOver) throw new IllegalStateException("Game already finished");
        if (awaitingPiece || pendingQuestion != null || awaitingSurpriseTarget) throw new IllegalStateException("Resolve the current action first");
        currentRollIsBonus = bonusRollReady;
        bonusRollReady = false;
        rollAvailable = true;
        stepDie = random.nextInt(6) + 1;
        extraDie = currentRollIsBonus ? random.nextInt(6) + 1 : difficulty == Difficulty.EASY ? 0 : random.nextInt(difficulty == Difficulty.MEDIUM ? 3 : 6) + 1;
        steps = difficulty == Difficulty.EASY && !currentRollIsBonus ? stepDie : stepDie * (extraDie - 1);
        if (!currentRollIsBonus) {
            if (stepDie == 6) consecutiveSixes++;
            else consecutiveSixes = 0;
        }
        if (!currentRollIsBonus && consecutiveSixes == 3) {
            log(getCurrentPlayer(), LogEntry.Kind.TURN_LOST, "rolled a third 6 in a row: turn cancelled");
        	message = getCurrentPlayer().getName() + " rolled a third 6: turn cancelled.";
            advanceTurn();
            return;
        }
        awaitingPiece = hasAnyLegalMove();
        log(getCurrentPlayer(), LogEntry.Kind.ROLL, rollText());
        if (!awaitingPiece) log(getCurrentPlayer(), LogEntry.Kind.TURN_LOST,
                !currentRollIsBonus && stepDie == 6 ? "no legal move, rolls again" : "no legal move");
        if (!awaitingPiece) {
            rollAvailable = false;
            String name = getCurrentPlayer().getName();
            if (!currentRollIsBonus && stepDie == 6) message = name + ": no legal move, but a 6 grants another roll."
                    + (consecutiveSixes == 2 ? " (Second 6 in a row: a third 6 loses the turn.)" : "");
            else {
                advanceTurn();
                message = name + ": no legal move. Next player: " + getCurrentPlayer().getName();
            }
        } else message = getCurrentPlayer().getName() + ": select a movable piece.";
    }

    private int requestedMovement(Piece piece) {
        return piece.isOnHomePath() ? stepDie : steps;
    }

    /** Number of steps possible without crossing/landing on an opposing Double Block. */
    private int reachableMovement(Piece piece) {
        if (piece.isFinished()) return -1;
        if (piece.isInCastle()) {
            if (stepDie != 6) return -1;
            return hasOpposingBlock(piece.getColor(), piece.ringAt(0), piece.indexAt(0)) ? -1 : 0;
        }
        int requested = requestedMovement(piece);
        if (requested <= 0 || piece.getProgress() + requested > piece.getFinishProgress()) return -1;
        if (!piece.isOnCommonPath()) return requested;
        for (int distance = 1; distance <= requested; distance++) {
            int progress = piece.getProgress() + distance;
            if (progress >= piece.getCommonLength()) break;
            if (hasOpposingBlock(piece.getColor(), piece.ringAt(progress), piece.indexAt(progress))) return distance - 1;
        }
        return requested;
    }

    private boolean hasOpposingBlock(PlayerColor movingColor, int ring, int boardIndex) {
        for (Player player : players) {
            if (player.getColor() == movingColor || areTeammates(player.getColor(), movingColor)) continue;
            int occupied = 0;
            for (Piece piece : player.pieces)
                if (piece.isOnCommonPath() && piece.getRing() == ring && piece.getBoardIndex() == boardIndex) occupied++;
            if (occupied >= 2) return true;
        }
        return false;
    }

    private boolean hasFullMove() {
        for (Piece piece : getCurrentPlayer().pieces) {
            int reachable = reachableMovement(piece);
            if (reachable >= 0 && (piece.isInCastle() || reachable == requestedMovement(piece))) return true;
        }
        return false;
    }

    /** A truncated move is allowed only if no piece can complete a full move. */
    public boolean canMove(int index) {
        if (gameOver || !rollAvailable || index < 0 || index >= 4) return false;
        Piece piece = getCurrentPlayer().pieces.get(index);
        int reachable = reachableMovement(piece);
        if (reachable < 0) return false;
        if (piece.isInCastle() || reachable == requestedMovement(piece)) return true;
        return reachable > 0 && !hasFullMove();
    }
    
    public int getPreviewLandingProgress(int index) {
        if (!canMove(index)) return -1;

        Piece piece = getCurrentPlayer().pieces.get(index);

        // Rolling a 6 can bring a piece out of the castle.
        if (piece.isInCastle()) {
            return 0;
        }

        // Use the existing movement rules, including Double Blocks.
        int movement = reachableMovement(piece);

        return piece.getProgress() + movement;
    }

    /**
     * FR-17, for the board's messages: the route position of the opposing Double Block that
     * stops this piece with the current dice, or -1 when no block is in its way.
     */
    public int getBlockAhead(Piece piece) {
        if (piece.isFinished()) return -1;
        if (piece.isInCastle())
            return stepDie == 6 && hasOpposingBlock(piece.getColor(), piece.ringAt(0), piece.indexAt(0)) ? 0 : -1;
        int requested = requestedMovement(piece);
        if (requested <= 0 || !piece.isOnCommonPath()) return -1;
        for (int distance = 1; distance <= requested; distance++) {
            int progress = piece.getProgress() + distance;
            if (progress >= piece.getCommonLength()) return -1;
            if (hasOpposingBlock(piece.getColor(), piece.ringAt(progress), piece.indexAt(progress))) return progress;
        }
        return -1;
    }

    /** Why the current player's piece (0-3) cannot be moved with this roll, or null when it can. */
    public String explainCannotMove(int index) {
        if (gameOver || index < 0 || index >= 4 || canMove(index)) return null;
        if (!rollAvailable) return "Roll the dice first.";
        Piece piece = getCurrentPlayer().pieces.get(index);
        if (piece.isFinished()) return "This piece is already home.";
        if (piece.isInCastle())
            return stepDie != 6 ? "Only a 6 brings a piece out of the castle." : "A Double Block is on the start square.";
        int requested = requestedMovement(piece);
        if (requested <= 0) return "0 steps this roll.";
        if (piece.getProgress() + requested > piece.getFinishProgress()) return "Needs an exact roll to reach FINISH.";
        int reachable = reachableMovement(piece);
        if (reachable == 0) return "A Double Block is right ahead.";
        return "Blocked ahead: another piece can move all " + requested + " steps.";
    }

    /** Start, protection and surprise squares are safe: nobody is captured there (see isProtected). */
    public boolean isSafeSquare(int ring, int boardIndex) { return isProtected(ring, boardIndex); }

    private boolean hasAnyLegalMove() {
        for (int i = 0; i < 4; i++) if (canMove(i)) return true;
        return false;
    }

    private boolean isProtected(int ring, int boardIndex) {
        Board.SquareType type = board.getRings().get(ring).get(boardIndex).getType();
        return type == Board.SquareType.START || type == Board.SquareType.PROTECTION
            || type == Board.SquareType.SURPRISE;
    }

    private int captureOnLanding(Piece movedPiece) {
        if (!movedPiece.isOnCommonPath() || isProtected(movedPiece.getRing(), movedPiece.getBoardIndex())) return 0;
        int square = movedPiece.getBoardIndex();
        int captured = 0;
        for (Player player : players) {
            if (player.getColor() == movedPiece.getColor() || areTeammates(player.getColor(), movedPiece.getColor())) continue;
            for (Piece opponent : player.pieces) {
                if (opponent.isOnCommonPath() && opponent.getRing() == movedPiece.getRing() && opponent.getBoardIndex() == square) {
                    opponent.setProgress(-1);
                    log(playerOf(movedPiece.getColor()), LogEntry.Kind.CAPTURE,
                            "captured " + player.getName() + "'s piece " + opponent.getNumber(), player.getColor());
                    captured++;
                }
            }
        }
        return captured;
    }

    public void move(int index) { doMove(index); notifyObservers(); }

    private void doMove(int index) {
        if (gameOver || !awaitingPiece || !canMove(index)) throw new IllegalArgumentException("Illegal piece selection");
        lastSurprise = null;
        Player player = getCurrentPlayer();
        Piece piece = player.pieces.get(index);
        int reachable = reachableMovement(piece);
        boolean shortened = !piece.isInCastle() && reachable < requestedMovement(piece);
        boolean leftCastle = piece.isInCastle();
        if (leftCastle) piece.setProgress(0);
        else piece.setProgress(piece.getProgress() + reachable);
        lastLanding = piece.getProgress();
        log(player, piece.isFinished() ? LogEntry.Kind.FINISH : LogEntry.Kind.MOVE, "piece " + piece.getNumber()
                + (leftCastle ? " left the castle" : piece.isFinished() ? " reached FINISH" : " moved " + reachable + (reachable == 1 ? " step" : " steps")));
        int captured = captureOnLanding(piece);
        updateFinishingOrder();
        message = player.getName() + " moved piece " + piece.getNumber() + ".";
        if (shortened) message += " Stopped before an opposing Double Block.";
        if (captured > 0) message += " Captured opponent piece!";
        if (piece.isFinished()) message += " Reached FINISH HOME!";
        else if (piece.isOnHomePath()) message += " On the home path.";
        awaitingPiece = false;
        rollAvailable = false;
        specialChain = 0;
        if (!gameOver && !triggerSpecial(piece)) finishTurnAfterMove();
    }

    private boolean triggerSpecial(Piece piece) {
        if (!piece.isOnCommonPath() || specialChain >= 12) return false;
        Board.SquareType type = board.getRings().get(piece.getRing()).get(piece.getBoardIndex()).getType();
        if (type == Board.SquareType.SURPRISE) {
            specialChain++;
            startSurprise(piece);
            return true;
        }
        if (questionBank == null) return false;

        String level;
        switch (type) {
            case QUESTION_EASY: level = "easy"; break;
            case QUESTION_MEDIUM: level = "medium"; break;
            case QUESTION_HARD: level = "hard"; break;
            default: return false;
        }
        pendingQuestion = questionBank.next(level);
        questionPiece = piece;
        specialChain++;
        message += " Question square: " + level + ".";
        return true;
    }

    /** Answer a pending question. A correct answer moves forward; incorrect moves backward. */
    public boolean answerQuestion(int chosenIndex) {
        boolean correct = doAnswer(chosenIndex);
        notifyObservers();
        return correct;
    }

    private boolean doAnswer(int chosenIndex) {
        if (pendingQuestion == null || chosenIndex < 0 || chosenIndex > 3)
            throw new IllegalStateException("No valid answer to submit");
        lastSurprise = null;
        Question question = pendingQuestion;
        boolean correct = question.getCorrect() == chosenIndex;
        int distance = "easy".equals(question.getLevel()) ? 1 : "medium".equals(question.getLevel()) ? 2 : 3;
        int direction = correct ? 1 : -1;
        Piece moved = questionPiece;
        pendingQuestion = null;
        questionPiece = null;
        int original = moved.getProgress();
        int target = Math.max(0, original + direction * distance);
        if (target > moved.getFinishProgress()) target = original; // exact finish only
        // Backward and forward movement must stop before an opposing Double Block.
        if (moved.isOnCommonPath()) {
            int sign = target >= original ? 1 : -1;
            for (int progress = original + sign; sign > 0 ? progress <= target : progress >= target; progress += sign) {
                if (progress < 0 || progress >= moved.getCommonLength()) break;
                if (hasOpposingBlock(moved.getColor(), moved.ringAt(progress), moved.indexAt(progress))) { target = progress - sign; break; }
            }
        }
        moved.setProgress(target);
        log(getCurrentPlayer(), correct ? LogEntry.Kind.CORRECT : LogEntry.Kind.WRONG,
                (correct ? "answered " : "missed ") + ("easy".equals(question.getLevel()) ? "an " : "a ")
                + question.getLevel() + " question ("
                + (target - original > 0 ? "+" : "") + (target - original) + ")");
        int captured = captureOnLanding(moved);
        message = correct ? "Correct! Moved forward " + (target-original) + " step(s)."
                : "Incorrect. Correct answer: " + question.getAnswers()[question.getCorrect()]
                    + ". Movement: " + (target-original) + " step(s).";
        if (captured > 0) message += " Captured an opponent!";
        updateFinishingOrder();
        // FR-22: a piece that did not move does not trigger its square again.
        if (!gameOver && (target == original || !triggerSpecial(moved))) finishTurnAfterMove();
        return correct;
    }

    /** Weighted outcomes: 10%, 10%, 10%, 30%, 30%, 10%. */
    private void startSurprise(Piece piece) {
        int roll = random.nextInt(100);
        lastSurprise = roll < 10 ? SurpriseEvent.DIRECT_FINISH
                : roll < 20 ? SurpriseEvent.OPPONENT_BACK
                : roll < 30 ? SurpriseEvent.RETURN_CASTLE
                : roll < 60 ? SurpriseEvent.BACK_THREE
                : roll < 90 ? SurpriseEvent.SKIP_NEXT_TURN
                : SurpriseEvent.BONUS_ULTRABOOST;
        log(playerOf(piece.getColor()), LogEntry.Kind.SURPRISE, surpriseText(lastSurprise, piece));
        switch (lastSurprise) {
            case DIRECT_FINISH:
                piece.setProgress(piece.getFinishProgress());
                updateFinishingOrder();
                message += " Surprise: piece sent directly to FINISH!";
                finishTurnAfterMove();
                break;
            case OPPONENT_BACK:
                awaitingSurpriseTarget = false;
                for (int p = 0; p < players.size(); p++)
                    for (int i = 0; i < 4; i++)
                        if (p != current && !areTeammates(players.get(p).getColor(), getCurrentPlayer().getColor())
                                && canMoveBackward(players.get(p).pieces.get(i))) awaitingSurpriseTarget = true;
                if (!awaitingSurpriseTarget) {
                    message += " Surprise cancelled: no movable opponent piece.";
                    finishTurnAfterMove();
                } else message += " Surprise: choose an opponent's piece to move backward.";
                break;
            case RETURN_CASTLE:
                piece.setProgress(-1);
                message += " Surprise: piece returned to castle.";
                finishTurnAfterMove();
                break;
            case BACK_THREE:
                int before = piece.getProgress();
                moveBackwardWithBlock(piece, 3);
                message += " Surprise: piece moved backward up to 3 steps.";
                if (piece.getProgress() != before && triggerSpecial(piece)) break;
                finishTurnAfterMove();
                break;
            case SKIP_NEXT_TURN:
                if (stepDie == 6 && !currentRollIsBonus) {
                    message += " Surprise: extra turn after 6 cancelled.";
                    advanceTurn();
                } else {
                    getCurrentPlayer().skipNextTurn = true;
                    message += " Surprise: next turn will be skipped.";
                    finishTurnAfterMove();
                }
                break;
            case BONUS_ULTRABOOST:
                bonusRollReady = true;
                message += " Surprise: immediate Step + UltraBoost roll!";
                break;
            default: throw new IllegalStateException("Unknown surprise");
        }
    }

    /** Resolve the 10% opponent-back event after the active player selects a target. */
    public int selectSurpriseTarget(int playerIndex, int pieceIndex) {
        int die = doSelectSurpriseTarget(playerIndex, pieceIndex);
        notifyObservers();
        return die;
    }

    private int doSelectSurpriseTarget(int playerIndex, int pieceIndex) {
        if (!canSelectSurpriseTarget(playerIndex, pieceIndex))
            throw new IllegalArgumentException("Choose a movable opponent piece");
        awaitingSurpriseTarget = false;
        lastSurprise = null;
        Piece target = players.get(playerIndex).pieces.get(pieceIndex);
        int die = random.nextInt(6) + 1;
        int backward = (die + 1) / 2;
        int before = target.getProgress();
        moveBackwardWithBlock(target, backward);
        log(getCurrentPlayer(), LogEntry.Kind.SURPRISE, "sent " + players.get(playerIndex).getName()
                + "'s piece " + target.getNumber() + " back " + (before - target.getProgress()));
        message = "Surprise: opponent rolled " + die + "; piece moved backward up to " + backward + " steps.";
        if (target.getProgress() == before || !triggerSpecial(target)) finishTurnAfterMove();
        return die;
    }

    /** Reverse route traversal; home-path positions can move back onto the common route. */
    /**
     * FR-28: only a piece that can really go back at least one step may be chosen.
     * It cannot when it is in the castle, finished, on its start square (FR-21),
     * or when an opposing Double Block is right behind it (FR-17).
     */
    private boolean canMoveBackward(Piece piece) {
        if (piece.isInCastle() || piece.isFinished() || piece.getProgress() == 0) return false;
        int behind = piece.getProgress() - 1;
        return behind >= piece.getCommonLength()
                || !hasOpposingBlock(piece.getColor(), piece.ringAt(behind), piece.indexAt(behind));
    }

    private void moveBackwardWithBlock(Piece piece, int distance) {
        int original = piece.getProgress();
        int target = Math.max(0, original - distance);
        for (int progress = original - 1; progress >= target; progress--) {
            if (progress < piece.getCommonLength() && hasOpposingBlock(piece.getColor(),
                    piece.ringAt(progress), piece.indexAt(progress))) {
                target = progress + 1;
                break;
            }
        }
        piece.setProgress(target);
        captureOnLanding(piece);
    }

    private void finishTurnAfterMove() {
        updateFinishingOrder();
        if (gameOver) return;
        if (bonusRollReady) {
            message += " Bonus UltraBoost roll available!";
            return;
        }
        if (currentRollIsBonus || stepDie != 6) advanceTurn();
        else message += consecutiveSixes == 2 ? " Second 6 in a row: roll again (a third 6 loses the turn)."
                                              : " Rolled 6: roll again.";
    }

    private void advanceTurn() {
        updateFinishingOrder();
        if (gameOver) return;
        consecutiveSixes = 0;
        awaitingPiece = false;
        rollAvailable = false;
        bonusRollReady = false;
        currentRollIsBonus = false;
        for (int i = 0; i < players.size(); i++) {
            current = (current + 1) % players.size();
            if (finishingOrder.contains(players.get(current))) continue;
            if (players.get(current).skipNextTurn) {
                players.get(current).skipNextTurn = false;
                log(players.get(current), LogEntry.Kind.TURN_LOST, "skipped this turn");
                continue;
            }
            return;
        }
    }
}
