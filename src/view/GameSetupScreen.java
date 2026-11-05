package view;

import static view.Theme.BLUE;
import static view.Theme.GLASS;
import static view.Theme.GREEN;
import static view.Theme.MUTED;
import static view.Theme.PLAYERS;
import static view.Theme.RED;
import static view.Theme.TEXT;
import static view.Theme.VIOLET;
import static view.Theme.YELLOW;
import static view.Theme.alpha;
import static view.Theme.applyQuality;
import static view.Theme.approach;
import static view.Theme.clamp01;
import static view.Theme.easeOut;
import static view.Theme.mix;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import model.Difficulty;
import model.GameConfig;
import model.PlayerColor;


public class GameSetupScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    public interface Listener {
        void onBack();
        void onStart(GameConfig config);
    }

    private static final int GAP = 16;
    private static final Color[] DIFFICULTY_ACCENTS = {GREEN, YELLOW, RED};

    private final Listener listener;
    private final Timer ticker;
    private long lastNanos;
    private float t;

    private int playerCount = GameConfig.MIN_PLAYERS;
    private Difficulty difficulty = Difficulty.EASY;
    private boolean triedToStart;

    private final CountTile[] countTiles = new CountTile[GameConfig.MAX_PLAYERS - GameConfig.MIN_PLAYERS + 1];
    private final DifficultyCard[] difficultyCards = new DifficultyCard[Difficulty.values().length];
    private final PlayerCard[] playerCards = new PlayerCard[GameConfig.MAX_PLAYERS];
    private final NeonButton backBtn;
    private final NeonButton startBtn;

    private String status = "";
    private Color statusColor = MUTED;
    private float statusPulse;

    private int x0;
    private int cw;
    private int top;
    private int rightX;

    public GameSetupScreen(Listener listener) {
        this.listener = listener;
        setLayout(null);
        setOpaque(true);
        setBackground(Theme.BG_TOP);
        setPreferredSize(new Dimension(1200, 800));

        for (int i = 0; i < countTiles.length; i++) {
            countTiles[i] = new CountTile(GameConfig.MIN_PLAYERS + i, i);
            add(countTiles[i]);
        }
        Difficulty[] levels = Difficulty.values();
        for (int i = 0; i < levels.length; i++) {
            difficultyCards[i] = new DifficultyCard(levels[i], i);
            add(difficultyCards[i]);
        }
        PlayerColor[] colors = PlayerColor.values();
        for (int i = 0; i < playerCards.length; i++) {
            playerCards[i] = new PlayerCard(i, colors[i]);
            add(playerCards[i]);
        }

        backBtn = new NeonButton("Back", VIOLET, NeonButton.Glyph.BACK, null);
        backBtn.setChevron(false);
        startBtn = new NeonButton("Let's Roll", GREEN, NeonButton.Glyph.DICE, null);
        backBtn.setIntro(0f);
        startBtn.setIntro(0f);
        backBtn.addActionListener(e -> { if (listener != null) listener.onBack(); });
        startBtn.addActionListener(e -> attemptStart());
        add(backBtn);
        add(startBtn);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "start");
        getActionMap().put("back", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { backBtn.doClick(); }
        });
        getActionMap().put("start", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { attemptStart(); }
        });

        setPlayerCount(GameConfig.MIN_PLAYERS);
        ticker = new Timer(16, e -> tick());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        lastNanos = System.nanoTime();
        ticker.start();
        SwingUtilities.invokeLater(() -> playerCards[0].field.requestFocusInWindow());
    }

    @Override
    public void removeNotify() {
        ticker.stop();
        super.removeNotify();
    }

    private void tick() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        t += dt;
        for (CountTile c : countTiles) {
            c.update(dt);
        }
        for (DifficultyCard c : difficultyCards) {
            c.update(dt);
        }
        for (PlayerCard c : playerCards) {
            c.update(dt);
        }
        backBtn.setIntro(clamp01((t - 0.55f) / 0.4f));
        startBtn.setIntro(clamp01((t - 0.65f) / 0.4f));
        statusPulse = Math.max(0f, statusPulse - dt * 1.6f);
        repaint();
    }

    private float intro(float delay) {
        return easeOut(clamp01((t - delay) / 0.45f));
    }

    private void setPlayerCount(int n) {
        int old = playerCount;
        playerCount = n;
        for (PlayerCard c : playerCards) {
            c.syncActive();
        }
        refreshStatus();
        if (n > old) {
            playerCards[old].field.requestFocusInWindow();
        }
    }

    private void setDifficulty(Difficulty d) {
        difficulty = d;
        refreshStatus();
    }

    private void chooseColor(int index, PlayerColor pc) {
        PlayerCard me = playerCards[index];
        if (me.color == pc) {
            return;
        }
        for (PlayerCard other : playerCards) {
            if (other != me && other.color == pc) {
                other.color = me.color;
                other.pop = 1f;
                break;
            }
        }
        me.color = pc;
        me.pop = 1f;
    }

    private int ownerOf(PlayerColor pc) {
        for (PlayerCard c : playerCards) {
            if (c.color == pc) {
                return c.index;
            }
        }
        return -1;
    }

    private static final class Problem {
        final int index;
        final String message;

        Problem(int index, String message) {
            this.index = index;
            this.message = message;
        }
    }

    private String nameOf(int i) {
        return playerCards[i].field.getText().trim();
    }

    private Problem findProblem() {
        for (int i = 0; i < playerCount; i++) {
            if (nameOf(i).isEmpty()) {
                return new Problem(i, "Player " + (i + 1) + " still needs a nickname");
            }
        }
        for (int i = 0; i < playerCount; i++) {
            for (int j = 0; j < i; j++) {
                if (nameOf(i).equalsIgnoreCase(nameOf(j))) {
                    return new Problem(i, "\"" + nameOf(i) + "\" is already taken - nicknames must be unique");
                }
            }
        }
        return null;
    }

    private void refreshStatus() {
        Problem p = findProblem();
        if (p == null) {
            status = playerCount + " players  •  " + difficulty.getLabel() + "  •  press ENTER to roll";
            statusColor = GREEN;
        } else if (triedToStart) {
            status = p.message;
            statusColor = RED;
        } else {
            status = "Give every player a nickname to continue";
            statusColor = MUTED;
        }
    }

    private void attemptStart() {
        triedToStart = true;
        Problem p = findProblem();
        refreshStatus();
        if (p != null) {
            statusPulse = 1f;
            playerCards[p.index].shake();
            playerCards[p.index].field.requestFocusInWindow();
            return;
        }
        List<GameConfig.PlayerSetup> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(new GameConfig.PlayerSetup(nameOf(i), playerCards[i].color));
        }
        if (listener != null) {
            listener.onStart(new GameConfig(difficulty, players));
        }
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        cw = Math.min(1120, w - 80);
        x0 = (w - cw) / 2;
        top = Math.max(8, (h - 640) / 2);

        int leftW = (int) (cw * 0.33f);
        int tileW = (leftW - 2 * GAP) / 3;
        rightX = x0 + leftW + 36;
        int cardW = (x0 + cw - rightX - 2 * GAP) / 3;
        int rowA = top + 124;
        for (int i = 0; i < countTiles.length; i++) {
            countTiles[i].setBounds(x0 + i * (tileW + GAP), rowA, tileW, 146);
        }
        for (int i = 0; i < difficultyCards.length; i++) {
            difficultyCards[i].setBounds(rightX + i * (cardW + GAP), rowA, cardW, 146);
        }

        int rowB = top + 322;
        int pw = (cw - 3 * GAP) / 4;
        for (int i = 0; i < playerCards.length; i++) {
            playerCards[i].setBase(x0 + i * (pw + GAP), rowB, pw, 212);
        }

        int by = top + 556;
        backBtn.setBounds(x0 - NeonButton.PAD, by, 200, 76);
        startBtn.setBounds(x0 + cw - 280 + NeonButton.PAD, by, 280, 76);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        applyQuality(g2);
        int w = getWidth();
        int h = getHeight();

        Theme.paintBackdrop(g2, w, h, t);
        paintDotGrid(g2, w, h);

        g2.setComposite(AlphaComposite.SrcOver.derive(intro(0f)));
        paintHeader(g2);
        paintSection(g2, "01", "HOW MANY PLAYERS", x0, top + 112);
        paintSection(g2, "02", "DIFFICULTY", rightX, top + 112);
        paintSection(g2, "03", "WHO'S PLAYING", x0, top + 310);

        g2.setFont(Theme.ui(12f, Font.PLAIN));
        g2.setColor(MUTED);
        String hint = "Click a colour to take it (you swap with its owner)  •  max " + GameConfig.NAME_MAX_LENGTH + " characters";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hint, x0 + cw - fm.stringWidth(hint), top + 310);

        paintStatus(g2);
        g2.dispose();
    }

    private void paintDotGrid(Graphics2D g, int w, int h) {
        g.setColor(new Color(255, 255, 255, 13));
        int step = 32;
        int ox = (w % step) / 2;
        for (int y = step / 2; y < h; y += step) {
            for (int x = ox; x < w; x += step) {
                g.fillRect(x, y, 2, 2);
            }
        }
    }

    private void paintHeader(Graphics2D g) {
        g.setFont(Theme.mono(13f));
        g.setColor(GREEN);
        String crumb = "> new_game --setup";
        int n = Math.min(crumb.length(), (int) (t * 30));
        g.drawString(crumb.substring(0, n), x0, top + 24);

        g.setFont(Theme.display(44f));
        FontMetrics fm = g.getFontMetrics();
        String title = "GAME SETUP";
        g.setPaint(new GradientPaint(x0, 0, TEXT, x0 + fm.stringWidth(title), 0, VIOLET));
        g.drawString(title, x0, top + 72);

        String[] steps = {"SETUP", "BOARD", "PLAY"};
        g.setFont(Theme.ui(11f, Font.BOLD));
        FontMetrics sm = g.getFontMetrics();
        float pillH = 26;
        float x = x0 + cw;
        float y = top + 44;
        for (int i = steps.length - 1; i >= 0; i--) {
            String label = (i + 1) + "  " + steps[i];
            float pw = sm.stringWidth(label) + 24;
            x -= pw;
            RoundRectangle2D pill = new RoundRectangle2D.Float(x, y, pw, pillH, pillH, pillH);
            boolean current = i == 0;
            if (current) {
                g.setPaint(new GradientPaint(x, 0, VIOLET, x + pw, 0, BLUE));
                g.fill(pill);
                g.setColor(Color.WHITE);
            } else {
                g.setColor(new Color(255, 255, 255, 10));
                g.fill(pill);
                g.setStroke(new BasicStroke(1.2f));
                g.setColor(alpha(MUTED, 120));
                g.draw(pill);
                g.setColor(MUTED);
            }
            g.drawString(label, x + 12, y + pillH / 2f + (sm.getAscent() - sm.getDescent()) / 2f);
            if (i > 0) {
                x -= 22;
                g.setColor(alpha(MUTED, 120));
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {2f, 4f}, 0f));
                g.draw(new Line2D.Float(x + 4, y + pillH / 2f, x + 18, y + pillH / 2f));
            }
        }

        // divider
        g.setPaint(new GradientPaint(x0, 0, alpha(VIOLET, 150), x0 + cw, 0, alpha(VIOLET, 0)));
        g.fill(new Rectangle2D.Float(x0, top + 88, cw, 1.2f));
    }

    private void paintSection(Graphics2D g, String num, String label, float x, float y) {
        g.setFont(Theme.mono(12f));
        g.setColor(VIOLET);
        g.drawString(num, x, y);
        g.setFont(Theme.ui(12f, Font.BOLD));
        g.setColor(TEXT);
        Theme.drawTracked(g, label, x + 28, y, 2.2f);
    }

    private void paintStatus(Graphics2D g) {
        if (status.isEmpty()) {
            return;
        }
        g.setFont(Theme.semibold(14f));
        FontMetrics fm = g.getFontMetrics();
        float cy = top + 556 + 38;
        float shake = (float) Math.sin(statusPulse * 40) * 6f * statusPulse;
        float tw = fm.stringWidth(status);
        float cx = x0 + cw / 2f - tw / 2f + shake + 20;

        // little status dot in front of the text
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * 4));
        g.setColor(alpha(statusColor, (int) (60 + 60 * pulse)));
        g.fill(new Ellipse2D.Float(cx - 22, cy - 7, 14, 14));
        g.setColor(statusColor);
        g.fill(new Ellipse2D.Float(cx - 18, cy - 3, 6, 6));

        g.setColor(mix(statusColor, Color.WHITE, statusColor == MUTED ? 0f : 0.25f));
        g.drawString(status, cx, cy + (fm.getAscent() - fm.getDescent()) / 2f);
    }

    // ================================================================== shared card frame
    private static void paintCardFrame(Graphics2D g, RoundRectangle2D r, Color acc, float sel, float hover) {
        // outer glow when selected
        for (int i = 0; i < 4; i++) {
            int a = (int) (sel * (26 - i * 6));
            if (a > 0) {
                g.setStroke(new BasicStroke(i * 4 + 2));
                g.setColor(alpha(acc, a));
                g.draw(r);
            }
        }
        g.setColor(GLASS);
        g.fill(r);
        Rectangle2D b = r.getBounds2D();
        g.setPaint(new GradientPaint(0, (float) b.getMinY(), alpha(acc, (int) (sel * 95 + hover * 20)),
                0, (float) b.getMaxY(), alpha(acc, (int) (sel * 15))));
        g.fill(r);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(alpha(mix(MUTED, acc, Math.max(sel, hover)), (int) (70 + 185 * Math.max(sel, hover * 0.7f))));
        g.draw(r);
    }

    private static void paintCheck(Graphics2D g, float cx, float cy, float r, Color acc) {
        if (r < 1) {
            return;
        }
        g.setColor(acc);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g.setColor(new Color(0x0A0E1F));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D p = new Path2D.Float();
        p.moveTo(cx - r * 0.45f, cy);
        p.lineTo(cx - r * 0.1f, cy + r * 0.38f);
        p.lineTo(cx + r * 0.48f, cy - r * 0.35f);
        g.draw(p);
    }

    // ================================================================== 01: player count tile
    private final class CountTile extends JComponent {
        private static final long serialVersionUID = 1L;
        private final int count;
        private final float delay;
        private boolean hovered;
        private float hover;
        private float sel;

        CountTile(int count, int order) {
            this.count = count;
            this.delay = 0.12f + order * 0.06f;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; }
                @Override public void mouseExited(MouseEvent e) { hovered = false; }
                @Override public void mousePressed(MouseEvent e) { setPlayerCount(CountTile.this.count); }
            });
        }

        void update(float dt) {
            hover = approach(hover, hovered ? 1f : 0f, dt, 12f);
            sel = approach(sel, playerCount == count ? 1f : 0f, dt, 12f);
        }

        @Override
        protected void paintComponent(Graphics g) {
            float in = intro(delay);
            if (in <= 0.001f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            applyQuality(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setComposite(AlphaComposite.SrcOver.derive(in));
            g2.translate(0, (1f - in) * 24f - hover * 3f + 3f);

            RoundRectangle2D r = new RoundRectangle2D.Float(3, 3, w - 6, h - 10, 22, 22);
            paintCardFrame(g2, r, VIOLET, sel, hover);

            // big number
            g2.setFont(Theme.display(54f));
            FontMetrics fm = g2.getFontMetrics();
            String s = String.valueOf(count);
            g2.setColor(mix(MUTED, Color.WHITE, Math.max(sel, hover)));
            g2.drawString(s, w / 2f - fm.stringWidth(s) / 2f, h * 0.50f);

            // row of pawns in the colours that will play
            float spacing = 17f;
            float sx = w / 2f - (count - 1) * spacing / 2f;
            for (int k = 0; k < count; k++) {
                float bob = (float) Math.sin(t * 3 + k * 0.8) * 2f * sel;
                Theme.paintPawn(g2, sx + k * spacing, h * 0.64f + bob, 5.5f, PLAYERS[k], (int) (110 + 145 * Math.max(sel, hover)));
            }

            g2.setFont(Theme.ui(10f, Font.BOLD));
            g2.setColor(alpha(MUTED, 220));
            String lbl = "PLAYERS";
            Theme.drawTracked(g2, lbl, w / 2f - Theme.trackedWidth(g2, lbl, 2f) / 2f, h - 22, 2f);

            paintCheck(g2, w - 20, 20, 9 * sel, VIOLET);
            g2.dispose();
        }
    }

    // ================================================================== 02: difficulty card
    private final class DifficultyCard extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Difficulty level;
        private final Color acc;
        private final float delay;
        private boolean hovered;
        private float hover;
        private float sel;

        DifficultyCard(Difficulty level, int order) {
            this.level = level;
            this.acc = DIFFICULTY_ACCENTS[order % DIFFICULTY_ACCENTS.length];
            this.delay = 0.25f + order * 0.07f;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; }
                @Override public void mouseExited(MouseEvent e) { hovered = false; }
                @Override public void mousePressed(MouseEvent e) { setDifficulty(level); }
            });
        }

        void update(float dt) {
            hover = approach(hover, hovered ? 1f : 0f, dt, 12f);
            sel = approach(sel, difficulty == level ? 1f : 0f, dt, 12f);
        }

        @Override
        protected void paintComponent(Graphics g) {
            float in = intro(delay);
            if (in <= 0.001f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            applyQuality(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setComposite(AlphaComposite.SrcOver.derive(in));
            g2.translate(0, (1f - in) * 24f - hover * 3f + 3f);

            RoundRectangle2D r = new RoundRectangle2D.Float(3, 3, w - 6, h - 10, 22, 22);
            paintCardFrame(g2, r, acc, sel, hover);

            paintBoardIcon(g2, 18, 18, 56);

            float tx = 18 + 56 + 14;
            g2.setFont(Theme.ui(19f, Font.BOLD));
            g2.setColor(mix(TEXT, Color.WHITE, sel));
            g2.drawString(level.getLabel(), tx, 40);

            String dice = level.hasUltraBoostDie() ? "+ UltraBoost die" : level.hasBoostDie() ? "+ Boost die" : "Regular die";
            String track = level.getExtraRings() == 0 ? "52-square track"
                    : level.getExtraRings() == 1 ? "+64 outer ring" : "+64 +72 rings";
            // shrink the detail text if the card is narrow (small windows)
            g2.setFont(Theme.ui(12f, Font.PLAIN));
            FontMetrics dm = g2.getFontMetrics();
            float widest = Math.max(dm.stringWidth(dice), dm.stringWidth(track));
            float room = w - tx - 12;
            if (widest > room) {
                g2.setFont(Theme.ui(Math.max(9f, 12f * room / widest), Font.PLAIN));
            }
            g2.setColor(mix(MUTED, TEXT, sel * 0.6f));
            g2.drawString(dice, tx, 60);
            g2.drawString(track, tx, 77);

            // special-square chips
            float chipY = h - 50;
            float chipH = 26;
            float chipGap = 6;
            float chipW = (w - 32 - 2 * chipGap) / 3f;
            int[] counts = {level.getProtectSquares(), level.getQuestionSquares(), level.getSurpriseSquares()};
            for (int i = 0; i < 3; i++) {
                float cx = 16 + i * (chipW + chipGap);
                RoundRectangle2D chip = new RoundRectangle2D.Float(cx, chipY, chipW, chipH, chipH, chipH);
                g2.setColor(new Color(255, 255, 255, counts[i] == 0 ? 6 : 14));
                g2.fill(chip);
                float ic = cx + 14;
                float icy = chipY + chipH / 2f;
                int a = counts[i] == 0 ? 80 : 255;
                if (i == 0) {
                    paintStar(g2, ic, icy, 6.5f, alpha(YELLOW, a));
                } else {
                    paintBadgeIcon(g2, ic, icy, 7f, i == 1 ? "?" : "!", alpha(i == 1 ? BLUE : RED, a));
                }
                g2.setFont(Theme.ui(12f, Font.BOLD));
                g2.setColor(alpha(TEXT, counts[i] == 0 ? 90 : 230));
                g2.drawString(String.valueOf(counts[i]), ic + 12, icy + 4.5f);
            }

            paintCheck(g2, w - 20, 20, 9 * sel, acc);
            g2.dispose();
        }

        /** Mini board: harder levels get extra rings around the cross. */
        private void paintBoardIcon(Graphics2D g, float x, float y, float s) {
            int rings = level.getExtraRings();
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {3f, 3f}, t * 6));
            for (int k = 0; k < rings; k++) {
                float in = k * 7;
                g.setColor(alpha(acc, 120 + (int) (100 * sel)));
                g.draw(new RoundRectangle2D.Float(x + in, y + in, s - 2 * in, s - 2 * in, 12, 12));
            }
            float b = rings * 7 + 3;
            float bx = x + b;
            float by = y + b;
            float bs = s - 2 * b;
            float arm = bs * 0.34f;
            float home = (bs - arm) / 2f - 1.5f;
            // four coloured homes in the corners
            float[][] corners = {{bx, by}, {bx + bs - home, by}, {bx + bs - home, by + bs - home}, {bx, by + bs - home}};
            for (int i = 0; i < 4; i++) {
                g.setColor(alpha(PLAYERS[i], 200));
                g.fill(new RoundRectangle2D.Float(corners[i][0], corners[i][1], home, home, 4, 4));
            }
            // cross track
            g.setColor(new Color(255, 255, 255, 60));
            g.fill(new Rectangle2D.Float(bx, by + bs / 2f - arm / 2f, bs, arm));
            g.fill(new Rectangle2D.Float(bx + bs / 2f - arm / 2f, by, arm, bs));
            // centre
            float c = arm * 0.42f;
            float ccx = bx + bs / 2f;
            float ccy = by + bs / 2f;
            Color[] order = {GREEN, YELLOW, BLUE, RED};
            float[][] tri = {{-c, -c, c, -c}, {c, -c, c, c}, {c, c, -c, c}, {-c, c, -c, -c}};
            for (int i = 0; i < 4; i++) {
                Path2D p = new Path2D.Float();
                p.moveTo(ccx, ccy);
                p.lineTo(ccx + tri[i][0], ccy + tri[i][1]);
                p.lineTo(ccx + tri[i][2], ccy + tri[i][3]);
                p.closePath();
                g.setColor(order[i]);
                g.fill(p);
            }
        }
    }

    private static void paintStar(Graphics2D g, float cx, float cy, float r, Color c) {
        Path2D p = new Path2D.Float();
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            float rr = (i % 2 == 0) ? r : r * 0.45f;
            float px = cx + (float) Math.cos(ang) * rr;
            float py = cy + (float) Math.sin(ang) * rr;
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    private static void paintBadgeIcon(Graphics2D g, float cx, float cy, float r, String s, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g.setFont(Theme.ui(10f, Font.BOLD));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(s, cx - fm.stringWidth(s) / 2f, cy + (fm.getAscent() - fm.getDescent()) / 2f);
    }

    // ================================================================== 03: player card
    private final class PlayerCard extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final float SWATCH_R = 12f;
        private static final float SWATCH_STEP = 34f;

        final int index;
        PlayerColor color;
        final NameField field;
        float pop;                     // colour-change bounce
        float introNow;                // shared with the name field
        private float active;
        private float hover;
        private boolean hovered;
        private int hoverSwatch = -1;
        private float shakeT = -1f;
        private int baseX;
        private int baseY;

        PlayerCard(int index, PlayerColor color) {
            this.index = index;
            this.color = color;
            setLayout(null);
            field = new NameField(this);
            add(field);

            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; }

                @Override public void mouseExited(MouseEvent e) {
                    hovered = false;
                    hoverSwatch = -1;
                }

                @Override public void mouseMoved(MouseEvent e) {
                    hoverSwatch = isActive() ? swatchAt(e.getPoint()) : -1;
                    boolean hand = !isActive() || hoverSwatch >= 0;
                    setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }

                @Override public void mousePressed(MouseEvent e) {
                    if (!isActive()) {
                        setPlayerCount(PlayerCard.this.index + 1);   // click an empty slot to add it
                        return;
                    }
                    int s = swatchAt(e.getPoint());
                    if (s >= 0) {
                        chooseColor(PlayerCard.this.index, PlayerColor.values()[s]);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        boolean isActive() {
            return index < playerCount;
        }

        void syncActive() {
            field.setVisible(isActive());
        }

        void shake() {
            shakeT = 0f;
        }

        void setBase(int x, int y, int w, int h) {
            baseX = x;
            baseY = y;
            setBounds(x, y, w, h);
            field.setBounds(16, 102, w - 32, 42);
        }

        void update(float dt) {
            active = approach(active, isActive() ? 1f : 0f, dt, 10f);
            hover = approach(hover, hovered ? 1f : 0f, dt, 12f);
            pop = Math.max(0f, pop - dt * 3f);
            if (shakeT >= 0f) {
                shakeT += dt;
                float k = 1f - shakeT / 0.45f;
                int off = (int) (Math.sin(shakeT * 45) * 10 * Math.max(0f, k));
                if (shakeT > 0.45f) {
                    shakeT = -1f;
                    off = 0;
                }
                setLocation(baseX + off, baseY);
            }
        }

        private float swatchCx(int k) {
            return 22 + SWATCH_R + k * SWATCH_STEP;
        }

        private float swatchCy() {
            return getHeight() - 32;
        }

        private int swatchAt(Point p) {
            for (int k = 0; k < PlayerColor.values().length; k++) {
                if (p.distance(swatchCx(k), swatchCy()) <= SWATCH_R + 4) {
                    return k;
                }
            }
            return -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            introNow = intro(0.35f + index * 0.07f);
            if (introNow <= 0.001f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            applyQuality(g2);
            int w = getWidth();
            int h = getHeight();
            Color acc = Theme.of(color);
            float a = active;

            g2.setComposite(AlphaComposite.SrcOver.derive(introNow * (0.55f + 0.45f * a)));
            RoundRectangle2D r = new RoundRectangle2D.Float(3, 3, w - 6, h - 6, 24, 24);

            // glow + glass
            for (int i = 0; i < 4; i++) {
                int ga = (int) (a * (18 + 14 * hover - i * 5));
                if (ga > 0) {
                    g2.setStroke(new BasicStroke(i * 4 + 2));
                    g2.setColor(alpha(acc, ga));
                    g2.draw(r);
                }
            }
            g2.setColor(GLASS);
            g2.fill(r);
            Graphics2D wash = (Graphics2D) g2.create();
            wash.clip(r);
            wash.setPaint(new GradientPaint(0, 0, alpha(acc, (int) (80 * a)), 0, 100, alpha(acc, 0)));
            wash.fillRect(0, 0, w, 100);
            wash.dispose();

            if (a > 0.5f) {
                g2.setStroke(new BasicStroke(1.6f));
                g2.setColor(alpha(acc, (int) (150 + 105 * hover)));
            } else {
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {6f, 6f}, t * 10));
                g2.setColor(alpha(MUTED, (int) (90 + 100 * hover)));
            }
            g2.draw(r);

            // ---------- active content (fades with a)
            if (a > 0.01f) {
                g2.setComposite(AlphaComposite.SrcOver.derive(introNow * a));
                paintAvatar(g2, acc);

                g2.setFont(Theme.ui(11f, Font.BOLD));
                g2.setColor(MUTED);
                Theme.drawTracked(g2, "PLAYER " + (index + 1), 92, 46, 2f);
                g2.setFont(Theme.display(22f));
                g2.setColor(acc);
                g2.drawString(color.getLabel().toUpperCase(), 92, 72);

                paintSwatches(g2);
            }

            // ---------- empty slot overlay (fades with 1 - a)
            if (a < 0.99f) {
                g2.setComposite(AlphaComposite.SrcOver.derive(introNow * (1f - a)));
                float cx = w / 2f;
                float cy = h / 2f - 14;
                float rr = 24 + hover * 3;
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {4f, 5f}, 9f - (t * 8f) % 9f));
                g2.setColor(alpha(TEXT, 140));
                g2.draw(new Ellipse2D.Float(cx - rr, cy - rr, rr * 2, rr * 2));
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(cx - 8, cy, cx + 8, cy));
                g2.draw(new Line2D.Float(cx, cy - 8, cx, cy + 8));
                g2.setFont(Theme.ui(11f, Font.BOLD));
                g2.setColor(MUTED);
                String lbl = "ADD PLAYER " + (index + 1);
                Theme.drawTracked(g2, lbl, cx - Theme.trackedWidth(g2, lbl, 2f) / 2f, cy + rr + 26, 2f);
            }
            g2.dispose();
        }

        private void paintAvatar(Graphics2D g, Color acc) {
            float cx = 50;
            float cy = 56;
            float rr = 30 + pop * 4;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), rr, new float[] {0f, 1f},
                    new Color[] {alpha(acc, 110), alpha(acc, 15)}));
            g.fill(new Ellipse2D.Float(cx - rr, cy - rr, rr * 2, rr * 2));
            g.setStroke(new BasicStroke(2f));
            g.setColor(alpha(acc, 230));
            g.draw(new Ellipse2D.Float(cx - rr, cy - rr, rr * 2, rr * 2));

            // orbiting spark
            double ang = t * 2.2 + index;
            float sx = cx + (float) Math.cos(ang) * (rr + 1);
            float sy = cy + (float) Math.sin(ang) * (rr + 1);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Float(sx - 2.5f, sy - 2.5f, 5, 5));

            float bob = (float) Math.sin(t * 2.6 + index) * 2f;
            Theme.paintPawn(g, cx, cy - 6 + bob, 13f * (1f + pop * 0.25f), acc, 255);
        }

        private void paintSwatches(Graphics2D g) {
            PlayerColor[] all = PlayerColor.values();
            float cy = swatchCy();
            String hint = null;
            for (int k = 0; k < all.length; k++) {
                PlayerColor pc = all[k];
                Color c = Theme.of(pc);
                float cx = swatchCx(k);
                boolean mine = pc == color;
                int owner = ownerOf(pc);
                boolean takenByActive = !mine && owner < playerCount;
                float rr = SWATCH_R + (mine ? 1f : 0f) + (hoverSwatch == k ? 2f : 0f);

                g.setColor(alpha(c, takenByActive ? 85 : 230));
                g.fill(new Ellipse2D.Float(cx - rr, cy - rr, rr * 2, rr * 2));
                if (mine) {
                    g.setStroke(new BasicStroke(2.2f));
                    g.setColor(Color.WHITE);
                    g.draw(new Ellipse2D.Float(cx - rr - 4, cy - rr - 4, (rr + 4) * 2, (rr + 4) * 2));
                } else if (takenByActive) {
                    // show which player owns it
                    g.setFont(Theme.ui(10f, Font.BOLD));
                    FontMetrics fm = g.getFontMetrics();
                    String s = String.valueOf(owner + 1);
                    g.setColor(Color.WHITE);
                    g.drawString(s, cx - fm.stringWidth(s) / 2f, cy + (fm.getAscent() - fm.getDescent()) / 2f);
                    if (hoverSwatch == k) {
                        hint = "swap with P" + (owner + 1);
                    }
                }
            }
            if (hint != null) {
                g.setFont(Theme.ui(11f, Font.BOLD));
                g.setColor(TEXT);
                g.drawString(hint, swatchCx(all.length - 1) + SWATCH_R + 12, cy + 4);
            }
        }
    }

    // ================================================================== nickname field
    private final class NameField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final PlayerCard card;

        NameField(PlayerCard card) {
            this.card = card;
            setOpaque(false);
            setBorder(new EmptyBorder(0, 14, 0, 50));
            setFont(Theme.ui(17f, Font.BOLD));
            setForeground(TEXT);
            setCaretColor(Color.WHITE);
            setSelectionColor(alpha(VIOLET, 150));
            setSelectedTextColor(Color.WHITE);
            ((AbstractDocument) getDocument()).setDocumentFilter(new LengthFilter(GameConfig.NAME_MAX_LENGTH));
            getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refreshStatus(); }
                @Override public void removeUpdate(DocumentEvent e) { refreshStatus(); }
                @Override public void changedUpdate(DocumentEvent e) { refreshStatus(); }
            });
            addActionListener(e -> attemptStart());
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { selectAll(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color acc = Theme.of(card.color);
            boolean focus = hasFocus();
            float in = card.introNow;

            Graphics2D g2 = (Graphics2D) g.create();
            applyQuality(g2);
            g2.setComposite(AlphaComposite.SrcOver.derive(in));
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(new Color(255, 255, 255, focus ? 20 : 10));
            g2.fill(r);
            g2.setStroke(new BasicStroke(focus ? 1.8f : 1.2f));
            g2.setColor(alpha(acc, focus ? 235 : 80));
            g2.draw(r);

            // character counter on the right
            g2.setFont(Theme.ui(11f, Font.BOLD));
            FontMetrics cm = g2.getFontMetrics();
            String cnt = getText().length() + "/" + GameConfig.NAME_MAX_LENGTH;
            g2.setColor(alpha(MUTED, focus ? 220 : 130));
            g2.drawString(cnt, getWidth() - 12 - cm.stringWidth(cnt), getHeight() / 2f + 4);

            if (getText().isEmpty()) {
                g2.setFont(getFont().deriveFont(Font.PLAIN));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(alpha(MUTED, 170));
                g2.drawString("Nickname…", getInsets().left + 1,
                        getHeight() / 2f + (fm.getAscent() - fm.getDescent()) / 2f);
            }
            g2.dispose();

            // Swing draws the text + caret on top, with the same fade
            Graphics2D tg = (Graphics2D) g.create();
            applyQuality(tg);
            tg.setComposite(AlphaComposite.SrcOver.derive(in));
            super.paintComponent(tg);
            tg.dispose();
        }
    }

    /** Blocks input beyond {@code max} characters. */
    private static final class LengthFilter extends DocumentFilter {
        private final int max;

        LengthFilter(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) {
                text = "";
            }
            int room = max - (fb.getDocument().getLength() - length);
            if (room <= 0) {
                return;
            }
            if (text.length() > room) {
                text = text.substring(0, room);
            }
            super.replace(fb, offset, length, text, attrs);
        }
    }
}