package screens;

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
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;


public class OpeningScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    public interface Listener {
        void onStartGame();
        void onGameHistory();
        void onQuestionWizard();   
        void onExit();
    }

    public static final Color RED    = new Color(0xFF4D6D);
    public static final Color GREEN  = new Color(0x2EE59D);
    public static final Color YELLOW = new Color(0xFFC93C);
    public static final Color BLUE   = new Color(0x3D8BFF);
    public static final Color[] PLAYERS = {RED, GREEN, YELLOW, BLUE};

    private static final Color BG_TOP    = new Color(0x0A0E1F);
    private static final Color BG_BOTTOM = new Color(0x171D3D);
    private static final Color TEXT      = new Color(0xE8ECFF);
    private static final Color MUTED     = new Color(0x8A93B8);
    private static final Color GRID      = new Color(0x6C7BFF);

    private final Font titleFont = pickFont(Font.SANS_SERIF, Font.BOLD, 150f, "Segoe UI Black", "Arial Black", "Impact");
    private final Font subFont   = pickFont(Font.SANS_SERIF, Font.BOLD, 14f, "Segoe UI", "Helvetica Neue", "Arial");
    private final Font monoFont  = pickFont(Font.MONOSPACED, Font.BOLD, 15f, "Consolas", "JetBrains Mono", "Menlo");
    private final Font smallFont = pickFont(Font.SANS_SERIF, Font.BOLD, 11f, "Segoe UI", "Helvetica Neue", "Arial");

    private final Listener listener;
    private final Random rnd = new Random();
    private final Floater[] floaters = new Floater[30];
    private final Timer ticker;
    private long lastNanos;
    private float t;                    

    private final NeonButton startBtn;
    private final NeonButton historyBtn;
    private final NeonButton wizardBtn;
    private final NeonButton exitBtn;
    private final NeonButton[] buttons;

    public OpeningScreen(Listener listener) {
        this.listener = listener;
        setLayout(null);                   
        setOpaque(true);
        setBackground(BG_TOP);
        setPreferredSize(new Dimension(1200, 800));

        for (int i = 0; i < floaters.length; i++) {
            floaters[i] = new Floater(true);
        }

        startBtn   = new NeonButton("Start Game",      GREEN,  NeonButton.Glyph.PLAY,    null);
        historyBtn = new NeonButton("Game History",    BLUE,   NeonButton.Glyph.HISTORY, null);
        wizardBtn  = new NeonButton("Question Wizard", YELLOW, NeonButton.Glyph.LOCK,    "ADMIN");
        exitBtn    = new NeonButton("Exit",            RED,    NeonButton.Glyph.POWER,   null);
        buttons = new NeonButton[] {startBtn, historyBtn, wizardBtn, exitBtn};

        startBtn.addActionListener(e -> { if (listener != null) listener.onStartGame(); });
        historyBtn.addActionListener(e -> { if (listener != null) listener.onGameHistory(); });
        wizardBtn.addActionListener(e -> { if (listener != null) listener.onQuestionWizard(); });
        exitBtn.addActionListener(e -> { if (listener != null) listener.onExit(); });

        for (NeonButton b : buttons) {
            add(b);
        }

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

        ticker = new Timer(16, e -> tick());   
    }

    @Override
    public void addNotify() {
        super.addNotify();
        lastNanos = System.nanoTime();
        ticker.start();
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
        int bh = 76;                      
        int x = (w - bw) / 2;
        int y = (int) (h * 0.50f);
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setBounds(x, y + i * bh, bw, bh);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        applyQuality(g2);
        int w = getWidth();
        int h = getHeight();
        float base = h * 0.335f;          

        paintBackground(g2, w, h);
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

    private void paintBackground(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g.fillRect(0, 0, w, h);

        float r = Math.max(w, h) * 0.55f;
        float[][] corners = {{0, 0}, {w, 0}, {w, h}, {0, h}};
        for (int i = 0; i < 4; i++) {
            int a = (int) (34 + 16 * Math.sin(t * 0.9 + i * 1.6));
            glow(g, corners[i][0], corners[i][1], r, PLAYERS[i], a);
        }
        glow(g, w / 2f, h * 0.30f, w * 0.42f, new Color(0x6C5BFF), 48);
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
            paintPawn(g, px, py, f.r, f.c, a);
        }
    }

    private void paintPawn(Graphics2D g, float cx, float cy, float r, Color c, int a) {
        float gr = r * 2.6f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), gr,
                new float[] {0f, 1f}, new Color[] {alpha(c, a / 3), alpha(c, 0)}));
        g.fill(new Ellipse2D.Float(cx - gr, cy - gr, gr * 2, gr * 2));

        Path2D body = new Path2D.Float();
        body.moveTo(cx - r * 0.8f, cy + r * 1.25f);
        body.quadTo(cx, cy - r * 0.6f, cx + r * 0.8f, cy + r * 1.25f);
        body.closePath();
        g.setColor(alpha(c, a));
        g.fill(body);
        g.fill(new Ellipse2D.Float(cx - r * 0.9f, cy + r * 1.05f, r * 1.8f, r * 0.45f)); 
        g.fill(new Ellipse2D.Float(cx - r * 0.5f, cy - r * 0.55f, r, r));             
        g.setColor(alpha(Color.WHITE, a / 3));
        g.fill(new Ellipse2D.Float(cx - r * 0.3f, cy - r * 0.42f, r * 0.3f, r * 0.3f)); 
    }

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
        Color[] order = {GREEN, YELLOW, BLUE, RED};      
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
        String s = "CODE QUEST  \u2022  SOFTWARE ENGINEERING EDITION";
        float in = clamp01((t - 0.6f) / 0.6f);
        if (in <= 0f) {
            return;
        }
        g.setFont(subFont);
        FontMetrics fm = g.getFontMetrics();
        float spacing = 4.5f;
        float total = -spacing;
        for (char ch : s.toCharArray()) {
            total += fm.charWidth(ch) + spacing;
        }
        float x = w / 2f - total / 2f;
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

        String left = "2\u20134 PLAYERS   \u2022   3 DIFFICULTY LEVELS   \u2022   SMART QUESTIONS";
        String right = "ENTER  start     \u2022     ESC  exit";
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

    private static void glow(Graphics2D g, float x, float y, float r, Color c, int a) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), r, new float[] {0f, 1f},
                new Color[] {alpha(c, a), alpha(c, 0)}));
        g.fill(new Rectangle2D.Float(x - r, y - r, r * 2, r * 2));
    }

    static void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, float k) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    static float easeOut(float v) {
        float k = 1f - v;
        return 1f - k * k * k;
    }

    static float easeOutBack(float v) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float k = v - 1f;
        return 1f + c3 * k * k * k + c1 * k * k;
    }

    private static Set<String> families;

 
    static Font pickFont(String fallback, int style, float size, String... names) {
        if (families == null) {
            families = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        }
        for (String n : names) {
            if (families.contains(n)) {
                return new Font(n, style, 1).deriveFont(size);
            }
        }
        return new Font(fallback, style, 1).deriveFont(size);
    }


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
            speed = 0.012f + r * 0.0028f;        
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


    static final class NeonButton extends JButton {
        private static final long serialVersionUID = 1L;

        enum Glyph { PLAY, HISTORY, LOCK, POWER }

        private static final int PAD = 10;         
        private final Color accent;
        private final Glyph glyph;
        private final String badge;
        private float hover;
        private float target;
        private float intro;
        private final Timer anim;

        NeonButton(String text, Color accent, Glyph glyph, String badge) {
            super(text);
            this.accent = accent;
            this.glyph = glyph;
            this.badge = badge;
            setFont(pickFont(Font.SANS_SERIF, Font.BOLD, 19f, "Segoe UI Semibold", "Segoe UI", "Helvetica Neue", "Arial"));
            setForeground(TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            anim = new Timer(15, e -> {
                hover += (target - hover) * 0.2f;
                if (Math.abs(target - hover) < 0.005f) {
                    hover = target;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setTarget(1f); }
                @Override public void mouseExited(MouseEvent e) { if (!hasFocus()) setTarget(0f); }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { setTarget(1f); }
                @Override public void focusLost(FocusEvent e) { if (!getModel().isRollover()) setTarget(0f); }
            });
        }

        private void setTarget(float v) {
            target = v;
            if (!anim.isRunning()) {
                anim.start();
            }
        }

        void setIntro(float v) {
            if (v != intro) {
                intro = v;
                repaint();
            }
        }

        @Override
        public boolean contains(int x, int y) {
            return x >= PAD && y >= PAD && x < getWidth() - PAD && y < getHeight() - PAD;
        }

        @Override
        protected void paintComponent(Graphics g) {
            float in = easeOut(intro);
            if (in <= 0.001f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            applyQuality(g2);
            int w = getWidth();
            int h = getHeight();

            g2.setComposite(AlphaComposite.SrcOver.derive(in));
            g2.translate((1f - in) * -50f, 0);
            if (getModel().isArmed() && getModel().isPressed()) {
                g2.translate(w / 2.0, h / 2.0);
                g2.scale(0.97, 0.97);
                g2.translate(-w / 2.0, -h / 2.0);
            }

            RoundRectangle2D r = new RoundRectangle2D.Float(PAD, PAD, w - 2 * PAD, h - 2 * PAD, 22, 22);

            for (int i = 0; i < PAD; i += 2) {
                int a = (int) (hover * (24 - i * 2));
                if (a > 0) {
                    g2.setStroke(new BasicStroke(i * 2 + 2));
                    g2.setColor(alpha(accent, a));
                    g2.draw(r);
                }
            }

            g2.setPaint(new GradientPaint(0, PAD, new Color(255, 255, 255, 22), 0, h - PAD, new Color(255, 255, 255, 6)));
            g2.fill(r);
            g2.setColor(new Color(10, 14, 32, 205));
            g2.fill(r);

            if (hover > 0.001f) {
                Graphics2D f = (Graphics2D) g2.create();
                f.clip(r);
                f.setPaint(new GradientPaint(PAD, 0, alpha(accent, (int) (170 * hover)),
                        w - PAD, 0, alpha(accent, (int) (40 * hover))));
                f.fill(new Rectangle2D.Float(PAD, PAD, (w - 2 * PAD) * easeOut(hover), h - 2 * PAD));
                f.dispose();
            }

            g2.setStroke(new BasicStroke(1.6f));
            g2.setColor(alpha(accent, (int) (110 + 145 * hover)));
            g2.draw(r);

            float ic = h - 2 * PAD - 16;
            float ix = PAD + 10;
            float iy = PAD + 8;
            g2.setColor(alpha(accent, (int) (70 + 185 * hover)));
            g2.fill(new Ellipse2D.Float(ix, iy, ic, ic));
            paintGlyph(g2, ix + ic / 2f, iy + ic / 2f, ic * 0.42f);

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            float tx = ix + ic + 16;
            float ty = h / 2f + (fm.getAscent() - fm.getDescent()) / 2f;
            g2.setColor(mix(getForeground(), Color.WHITE, hover));
            g2.drawString(getText(), tx, ty);

            float chevronX = w - PAD - 30 + hover * 6;
            if (badge != null) {
                Font bf = getFont().deriveFont(11f);
                g2.setFont(bf);
                FontMetrics bm = g2.getFontMetrics();
                float bw = bm.stringWidth(badge) + 18;
                float bh = 22;
                float bx = w - PAD - 44 - bw;
                float by = h / 2f - bh / 2f;
                RoundRectangle2D pill = new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh);
                g2.setColor(alpha(accent, 45));
                g2.fill(pill);
                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(alpha(accent, 200));
                g2.draw(pill);
                g2.setColor(hover > 0.5f ? Color.WHITE : accent);
                g2.drawString(badge, bx + 9, by + bh / 2f + (bm.getAscent() - bm.getDescent()) / 2f);
            }

            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(alpha(Color.WHITE, (int) (110 + 145 * hover)));
            Path2D chev = new Path2D.Float();
            chev.moveTo(chevronX, h / 2f - 7);
            chev.lineTo(chevronX + 7, h / 2f);
            chev.lineTo(chevronX, h / 2f + 7);
            g2.draw(chev);

            g2.dispose();
        }

        private void paintGlyph(Graphics2D g, float cx, float cy, float s) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (glyph) {
                case PLAY: {
                    Path2D p = new Path2D.Float();
                    p.moveTo(cx - s * 0.35f, cy - s * 0.55f);
                    p.lineTo(cx + s * 0.6f, cy);
                    p.lineTo(cx - s * 0.35f, cy + s * 0.55f);
                    p.closePath();
                    g.fill(p);
                    break;
                }
                case HISTORY: {
                    g.draw(new Ellipse2D.Float(cx - s * 0.65f, cy - s * 0.65f, s * 1.3f, s * 1.3f));
                    g.draw(new Line2D.Float(cx, cy, cx, cy - s * 0.4f));
                    g.draw(new Line2D.Float(cx, cy, cx + s * 0.32f, cy + s * 0.14f));
                    break;
                }
                case LOCK: {
                    g.draw(new Arc2D.Float(cx - s * 0.34f, cy - s * 0.78f, s * 0.68f, s * 0.8f, 0, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Float(cx - s * 0.34f, cy - s * 0.38f, cx - s * 0.34f, cy - s * 0.25f));
                    g.draw(new Line2D.Float(cx + s * 0.34f, cy - s * 0.38f, cx + s * 0.34f, cy - s * 0.25f));
                    g.fill(new RoundRectangle2D.Float(cx - s * 0.55f, cy - s * 0.28f, s * 1.1f, s * 0.85f, 5, 5));
                    g.setColor(alpha(accent, 255));
                    g.fill(new Ellipse2D.Float(cx - s * 0.12f, cy + s * 0.02f, s * 0.24f, s * 0.24f));
                    break;
                }
                case POWER:
                default: {
                    g.draw(new Arc2D.Float(cx - s * 0.58f, cy - s * 0.5f, s * 1.16f, s * 1.16f, 120, 300, Arc2D.OPEN));
                    g.draw(new Line2D.Float(cx, cy - s * 0.68f, cx, cy - s * 0.05f));
                    break;
                }
            }
        }
    }
}