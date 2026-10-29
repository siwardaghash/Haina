package screens;

import static screens.Theme.BLUE;
import static screens.Theme.GREEN;
import static screens.Theme.GRID;
import static screens.Theme.MUTED;
import static screens.Theme.PLAYERS;
import static screens.Theme.RED;
import static screens.Theme.TEXT;
import static screens.Theme.YELLOW;
import static screens.Theme.alpha;
import static screens.Theme.applyQuality;
import static screens.Theme.clamp01;
import static screens.Theme.easeOutBack;
import static screens.Theme.mix;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Opening screen of "LUDO // Code Quest".
 *
 * A fully custom-painted, animated Swing panel (no images, no external libraries).
 * MVC: this class is a pure View. It only reports user intent through {@link Listener};
 * the controller decides what happens next.
 */
public class OpeningScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Callbacks implemented by the controller. */
    public interface Listener {
        void onStartGame();
        void onGameHistory();
        void onQuestionWizard();   // restricted (admin) area - the controller will ask for credentials
        void onExit();
    }

    private final Font titleFont = Theme.display(150f);
    private final Font subFont   = Theme.ui(14f, Font.BOLD);
    private final Font monoFont  = Theme.mono(15f);
    private final Font smallFont = Theme.ui(11f, Font.BOLD);

    private final Random rnd = new Random();
    private final Floater[] floaters = new Floater[30];
    private final Timer ticker;
    private long lastNanos;
    private float t;                       // seconds since the screen appeared

    private final NeonButton startBtn;
    private final NeonButton exitBtn;
    private final NeonButton[] buttons;

    public OpeningScreen(Listener listener) {
        setLayout(null);                   // we position the buttons ourselves in doLayout()
        setOpaque(true);
        setBackground(Theme.BG_TOP);
        setPreferredSize(new Dimension(1200, 800));

        for (int i = 0; i < floaters.length; i++) {
            floaters[i] = new Floater(true);
        }

        startBtn = new NeonButton("Start Game", GREEN, NeonButton.Glyph.PLAY, null);
        NeonButton historyBtn = new NeonButton("Game History", BLUE, NeonButton.Glyph.HISTORY, null);
        NeonButton wizardBtn = new NeonButton("Question Wizard", YELLOW, NeonButton.Glyph.LOCK, "ADMIN");
        exitBtn = new NeonButton("Exit", RED, NeonButton.Glyph.POWER, null);
        buttons = new NeonButton[] {startBtn, historyBtn, wizardBtn, exitBtn};

        startBtn.addActionListener(e -> { if (listener != null) listener.onStartGame(); });
        historyBtn.addActionListener(e -> { if (listener != null) listener.onGameHistory(); });
        wizardBtn.addActionListener(e -> { if (listener != null) listener.onQuestionWizard(); });
        exitBtn.addActionListener(e -> { if (listener != null) listener.onExit(); });

        for (NeonButton b : buttons) {
            b.setIntro(0f);                // they slide in during the intro animation
            add(b);
        }

        // keyboard shortcuts: ENTER = start, ESC = exit
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "start");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exit");
        am.put("start", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { startBtn.doClick(); }
        });
        am.put("exit", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { exitBtn.doClick(); }
        });

        ticker = new Timer(16, e -> tick());   // ~60 FPS
    }

    // ------------------------------------------------------------------ lifecycle
    @Override
    public void addNotify() {
        super.addNotify();
        lastNanos = System.nanoTime();
        ticker.start();
    }

    @Override
    public void removeNotify() {
        ticker.stop();                     // stop animating when the screen is replaced
        super.removeNotify();
    }

    private void tick() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        t += dt;
        for (Floater f : floaters) {
            f.update(dt);
        }
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setIntro(clamp01((t - 0.75f - i * 0.12f) / 0.45f));
        }
        repaint();
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int bw = 380;
        int bh = 76;                       // visual height 56 + 10px glow padding on each side
        int x = (w - bw) / 2;
        int y = (int) (h * 0.50f);
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setBounds(x, y + i * bh, bw, bh);
        }
    }

    // ------------------------------------------------------------------ painting
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        applyQuality(g2);
        int w = getWidth();
        int h = getHeight();
        float base = h * 0.335f;           // title baseline

        Theme.paintBackdrop(g2, w, h, t);
        paintGrid(g2, w, h);
        paintFloaters(g2, w, h);
        paintEmblem(g2, w, h);
        paintDice(g2, w, base);
        paintTitle(g2, w, base);
        paintSubtitle(g2, w, base + 42);
        paintTerminal(g2, w, base + 64);
        paintVignette(g2, w, h);
        paintFooter(g2, w, h);
        g2.dispose();
    }

    private void paintGrid(Graphics2D g, int w, int h) {
        float horizon = h * 0.66f;
        Graphics2D gg = (Graphics2D) g.create();
        gg.clip(new Rectangle2D.Float(0, horizon, w, h - horizon));
        gg.setStroke(new BasicStroke(1f));

        int rows = 16;
        float scroll = (t * 0.35f) % 1f;
        for (int i = 0; i < rows; i++) {
            float p = (i + scroll) / rows;
            float y = horizon + (h - horizon) * p * p;
            gg.setColor(alpha(GRID, (int) (60 * p)));
            gg.draw(new Line2D.Float(0, y, w, y));
        }
        float vx = w / 2f;
        gg.setColor(alpha(GRID, 26));
        for (int k = -14; k <= 14; k++) {
            gg.draw(new Line2D.Float(vx + k * 14f, horizon, vx + k * (w / 9f), h));
        }
        gg.dispose();

        g.setPaint(new GradientPaint(0, horizon - 2, alpha(GRID, 70), 0, horizon + 40, alpha(GRID, 0)));
        g.fill(new Rectangle2D.Float(0, horizon - 2, w, 42));
    }

    private void paintFloaters(Graphics2D g, int w, int h) {
        for (Floater f : floaters) {
            float px = f.x * w + (float) Math.sin(t * 0.8 + f.phase) * 18f;
            float py = f.y * h;
            int a = Math.min(200, (int) (40 + f.r * 10));
            Theme.paintPawn(g, px, py, f.r, f.c, a);
        }
    }

    /** The Ludo "home" square (four triangles) with an orbit ring of player dots. */
    private void paintEmblem(Graphics2D g, int w, int h) {
        float in = easeOutBack(clamp01(t / 0.7f));
        if (in <= 0.01f) {
            return;
        }
        float cx = w / 2f;
        float cy = h * 0.12f;
        float s = 30f * in;
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * 2.0));

        float gr = s * 3.4f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), gr, new float[] {0f, 1f},
                new Color[] {new Color(255, 255, 255, (int) (30 + 30 * pulse)), new Color(255, 255, 255, 0)}));
        g.fill(new Ellipse2D.Float(cx - gr, cy - gr, gr * 2, gr * 2));

        Graphics2D e = (Graphics2D) g.create();
        e.translate(cx, cy);

        e.rotate(-t * 0.6);
        float ringR = s * 1.9f;
        e.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {4f, 9f}, 0f));
        e.setColor(alpha(TEXT, 90));
        e.draw(new Ellipse2D.Float(-ringR, -ringR, ringR * 2, ringR * 2));
        for (int i = 0; i < 4; i++) {
            double ang = i * Math.PI / 2;
            float px = (float) (Math.cos(ang) * ringR);
            float py = (float) (Math.sin(ang) * ringR);
            e.setColor(alpha(PLAYERS[i], 70));
            e.fill(new Ellipse2D.Float(px - 8, py - 8, 16, 16));
            e.setColor(PLAYERS[i]);
            e.fill(new Ellipse2D.Float(px - 4, py - 4, 8, 8));
        }

        e.rotate(t * 0.6 + t * 0.35);
        Color[] order = {GREEN, YELLOW, BLUE, RED};      // top, right, bottom, left
        float[][] tri = {{-s, -s, s, -s}, {s, -s, s, s}, {s, s, -s, s}, {-s, s, -s, -s}};
        for (int i = 0; i < 4; i++) {
            Path2D p = new Path2D.Float();
            p.moveTo(0, 0);
            p.lineTo(tri[i][0], tri[i][1]);
            p.lineTo(tri[i][2], tri[i][3]);
            p.closePath();
            e.setColor(order[i]);
            e.fill(p);
        }
        e.setStroke(new BasicStroke(2f));
        e.setColor(alpha(Color.WHITE, 210));
        e.draw(new Rectangle2D.Float(-s, -s, s * 2, s * 2));
        e.dispose();
    }

    private void paintDice(Graphics2D g, int w, float base) {
        float in = easeOutBack(clamp01((t - 0.5f) / 0.6f));
        if (in <= 0.01f) {
            return;
        }
        float y = base - 50;
        float off = 335;
        float size = 74 * in;
        float bobL = (float) Math.sin(t * 1.6) * 8;
        float bobR = (float) Math.sin(t * 1.6 + 1.7) * 8;
        paintDie(g, w / 2f - off, y + bobL, size, Math.sin(t * 0.9) * 0.35 - 0.25, dieFace(1, 1.1f), RED);
        paintDie(g, w / 2f + off, y + bobR, size, Math.sin(t * 0.8 + 2) * 0.35 + 0.25, dieFace(7, 1.35f), BLUE);
    }

    private void paintDie(Graphics2D g, float cx, float cy, float size, double angle, int face, Color accent) {
        float gr = size * 1.3f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), gr, new float[] {0f, 1f},
                new Color[] {alpha(accent, 90), alpha(accent, 0)}));
        g.fill(new Ellipse2D.Float(cx - gr, cy - gr, gr * 2, gr * 2));

        Graphics2D d = (Graphics2D) g.create();
        d.translate(cx, cy);
        d.rotate(angle);
        float half = size / 2f;

        d.setColor(new Color(0, 0, 0, 90));
        d.fill(new RoundRectangle2D.Float(-half + 5, -half + 8, size, size, size * 0.3f, size * 0.3f));

        RoundRectangle2D body = new RoundRectangle2D.Float(-half, -half, size, size, size * 0.3f, size * 0.3f);
        d.setPaint(new GradientPaint(-half, -half, Color.WHITE, half, half, new Color(0xCDD3EE)));
        d.fill(body);
        d.setStroke(new BasicStroke(2.5f));
        d.setColor(accent);
        d.draw(body);

        int[][] pips;
        switch (face) {
            case 1:  pips = new int[][] {{0, 0}}; break;
            case 2:  pips = new int[][] {{-1, -1}, {1, 1}}; break;
            case 3:  pips = new int[][] {{-1, -1}, {0, 0}, {1, 1}}; break;
            case 4:  pips = new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}; break;
            case 5:  pips = new int[][] {{-1, -1}, {1, -1}, {0, 0}, {-1, 1}, {1, 1}}; break;
            default: pips = new int[][] {{-1, -1}, {-1, 0}, {-1, 1}, {1, -1}, {1, 0}, {1, 1}}; break;
        }
        float q = size * 0.26f;
        float pr = size * 0.09f;
        d.setColor(new Color(0x1A2040));
        for (int[] p : pips) {
            d.fill(new Ellipse2D.Float(p[0] * q - pr, p[1] * q - pr, pr * 2, pr * 2));
        }
        d.dispose();
    }

    private void paintTitle(Graphics2D g, int w, float baseline) {
        String word = "LUDO";
        FontRenderContext frc = g.getFontRenderContext();
        GlyphVector gv = titleFont.createGlyphVector(frc, word);
        float tracking = 14f;
        float total = (float) gv.getLogicalBounds().getWidth() + tracking * (word.length() - 1);
        float x0 = w / 2f - total / 2f;

        for (int i = 0; i < word.length(); i++) {
            float k = clamp01((t - 0.15f - i * 0.12f) / 0.55f);
            if (k <= 0f) {
                continue;
            }
            float drop = (1f - easeOutBack(k)) * -90f;
            float bob = (float) Math.sin(t * 2.2 + i * 0.7) * 7f;
            Shape glyph = gv.getGlyphOutline(i, x0 + i * tracking, baseline + bob + drop);
            Color c = PLAYERS[i];

            g.setComposite(AlphaComposite.SrcOver.derive(k));

            Graphics2D sh = (Graphics2D) g.create();
            sh.translate(0, 8);
            sh.setColor(new Color(0, 0, 0, 110));
            sh.fill(glyph);
            sh.dispose();

            float[] widths = {24f, 14f, 7f};
            int[] alphas = {16, 30, 60};
            for (int j = 0; j < widths.length; j++) {
                g.setStroke(new BasicStroke(widths[j], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(alpha(c, alphas[j]));
                g.draw(glyph);
            }

            Rectangle2D b = glyph.getBounds2D();
            g.setPaint(new GradientPaint(0, (float) b.getMinY(), mix(c, Color.WHITE, 0.5f),
                    0, (float) b.getMaxY(), c));
            g.fill(glyph);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(alpha(Color.WHITE, 80));
            g.draw(glyph);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void paintSubtitle(Graphics2D g, int w, float y) {
        String s = "CODE QUEST  •  SOFTWARE ENGINEERING EDITION";
        float in = clamp01((t - 0.6f) / 0.6f);
        if (in <= 0f) {
            return;
        }
        g.setFont(subFont);
        FontMetrics fm = g.getFontMetrics();
        float spacing = 4.5f;
        float x = w / 2f - Theme.trackedWidth(g, s, spacing) / 2f;
        g.setComposite(AlphaComposite.SrcOver.derive(in));
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            g.setColor(i < 10 ? TEXT : MUTED);
            g.drawString(String.valueOf(ch), x, y);
            x += fm.charWidth(ch) + spacing;
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void paintTerminal(Graphics2D g, int w, float y) {
        float in = clamp01((t - 0.9f) / 0.5f);
        if (in <= 0f) {
            return;
        }
        String cmd = "> roll --dice --learn --players=4";
        g.setFont(monoFont);
        FontMetrics fm = g.getFontMetrics();
        float bw = fm.stringWidth(cmd + "_") + 44;
        float bh = 36;
        float bx = w / 2f - bw / 2f;

        g.setComposite(AlphaComposite.SrcOver.derive(in));
        RoundRectangle2D pill = new RoundRectangle2D.Float(bx, y, bw, bh, bh, bh);
        g.setColor(new Color(8, 12, 28, 190));
        g.fill(pill);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(alpha(GREEN, 90));
        g.draw(pill);

        float cycle = (t - 0.9f) % 6f;
        int n = Math.max(0, Math.min(cmd.length(), (int) (cycle * 16)));
        String shown = cmd.substring(0, n);
        float tx = bx + 22;
        float ty = y + bh / 2f + (fm.getAscent() - fm.getDescent()) / 2f;
        g.setColor(GREEN);
        g.drawString(shown, tx, ty);
        if (((int) (t * 2.4f)) % 2 == 0) {
            float cx = tx + fm.stringWidth(shown) + 2;
            g.fill(new Rectangle2D.Float(cx, ty - fm.getAscent() + 3, 9, fm.getAscent() - 2));
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void paintVignette(Graphics2D g, int w, int h) {
        float r = (float) Math.hypot(w, h) * 0.62f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), r, new float[] {0.55f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)}));
        g.fillRect(0, 0, w, h);
    }

    private void paintFooter(Graphics2D g, int w, int h) {
        g.setFont(smallFont);
        FontMetrics fm = g.getFontMetrics();
        int y = h - 24;
        g.setPaint(new GradientPaint(0, 0, alpha(TEXT, 0), w / 2f, 0, alpha(TEXT, 45), true));
        g.fill(new Rectangle2D.Float(28, y - 22, w - 56, 1));

        String left = "2–4 PLAYERS   •   3 DIFFICULTY LEVELS   •   SMART QUESTIONS";
        String right = "ENTER  start     •     ESC  exit";
        g.setColor(alpha(MUTED, 210));
        g.drawString(left, 28, y);
        g.drawString(right, w - 28 - fm.stringWidth(right), y);
    }

    private int dieFace(int seed, float period) {
        int k = (int) Math.floor(t / period);
        double v = Math.sin(k * 12.9898 + seed * 78.233) * 43758.5453;
        v -= Math.floor(v);
        return 1 + (int) (v * 6);
    }

    // ------------------------------------------------------------------ floating pawns
    private final class Floater {
        float x, y, r, speed, phase;
        Color c;

        Floater(boolean anywhere) {
            reset(anywhere);
        }

        void reset(boolean anywhere) {
            x = rnd.nextFloat();
            y = anywhere ? rnd.nextFloat() : 1.08f + rnd.nextFloat() * 0.1f;
            r = 5 + rnd.nextFloat() * 12;
            speed = 0.012f + r * 0.0028f;          // bigger = closer = faster (parallax)
            phase = rnd.nextFloat() * 6.28f;
            c = PLAYERS[rnd.nextInt(PLAYERS.length)];
        }

        void update(float dt) {
            y -= speed * dt;
            if (y < -0.08f) {
                reset(false);
            }
        }
    }
}