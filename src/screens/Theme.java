package screens;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import model.PlayerColor;

/**
 * Shared look of every screen: palette, fonts, easing helpers and common drawing.
 * Keeping this in one place means all screens stay visually consistent.
 */
public final class Theme {

    private Theme() {
    }

    // ------------------------------------------------------------------ palette
    public static final Color RED    = new Color(0xFF4D6D);
    public static final Color GREEN  = new Color(0x2EE59D);
    public static final Color YELLOW = new Color(0xFFC93C);
    public static final Color BLUE   = new Color(0x3D8BFF);
    /** Same order as {@link PlayerColor}. */
    public static final Color[] PLAYERS = {RED, GREEN, YELLOW, BLUE};

    public static final Color VIOLET    = new Color(0x8B7BFF);
    public static final Color BG_TOP    = new Color(0x0A0E1F);
    public static final Color BG_BOTTOM = new Color(0x171D3D);
    public static final Color TEXT      = new Color(0xE8ECFF);
    public static final Color MUTED     = new Color(0x8A93B8);
    public static final Color GRID      = new Color(0x6C7BFF);
    public static final Color GLASS     = new Color(10, 14, 32, 205);

    public static Color of(PlayerColor c) {
        return PLAYERS[c.ordinal()];
    }

    // ------------------------------------------------------------------ fonts
    public static Font display(float size) {
        return pickFont(Font.SANS_SERIF, Font.BOLD, size, "Segoe UI Black", "Arial Black", "Impact");
    }

    public static Font ui(float size, int style) {
        return pickFont(Font.SANS_SERIF, style, size, "Segoe UI", "Helvetica Neue", "Arial");
    }

    public static Font semibold(float size) {
        return pickFont(Font.SANS_SERIF, Font.BOLD, size, "Segoe UI Semibold", "Segoe UI", "Helvetica Neue", "Arial");
    }

    public static Font mono(float size) {
        return pickFont(Font.MONOSPACED, Font.BOLD, size, "Consolas", "JetBrains Mono", "Menlo");
    }

    private static Set<String> families;

    /** Returns the first installed font from {@code names}, or the logical fallback font. */
    public static Font pickFont(String fallback, int style, float size, String... names) {
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

    // ------------------------------------------------------------------ math / colour helpers
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color mix(Color a, Color b, float k) {
        k = clamp01(k);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    public static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /** Smoothly moves {@code v} toward {@code target}; frame-rate independent. */
    public static float approach(float v, float target, float dt, float speed) {
        return v + (target - v) * Math.min(1f, dt * speed);
    }

    public static float easeOut(float v) {
        float k = 1f - clamp01(v);
        return 1f - k * k * k;
    }

    public static float easeOutBack(float v) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float k = v - 1f;
        return 1f + c3 * k * k * k + c1 * k * k;
    }

    public static void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ------------------------------------------------------------------ shared drawing
    public static void glow(Graphics2D g, float x, float y, float r, Color c, int a) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), r, new float[] {0f, 1f},
                new Color[] {alpha(c, a), alpha(c, 0)}));
        g.fill(new Rectangle2D.Float(x - r, y - r, r * 2, r * 2));
    }

    /** Dark gradient + breathing glow in each corner (one per player colour). */
    public static void paintBackdrop(Graphics2D g, int w, int h, float t) {
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

    /** A Ludo pawn (head + cone body + base) with a soft glow. */
    public static void paintPawn(Graphics2D g, float cx, float cy, float r, Color c, int a) {
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
        g.fill(new Ellipse2D.Float(cx - r * 0.9f, cy + r * 1.05f, r * 1.8f, r * 0.45f)); // base
        g.fill(new Ellipse2D.Float(cx - r * 0.5f, cy - r * 0.55f, r, r));               // head
        g.setColor(alpha(Color.WHITE, a / 3));
        g.fill(new Ellipse2D.Float(cx - r * 0.3f, cy - r * 0.42f, r * 0.3f, r * 0.3f));  // shine
    }

    /** Draws text with extra letter spacing; returns the x after the last letter. */
    public static float drawTracked(Graphics2D g, String s, float x, float y, float spacing) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            g.drawString(String.valueOf(ch), x, y);
            x += fm.charWidth(ch) + spacing;
        }
        return x - spacing;
    }

    public static float trackedWidth(Graphics2D g, String s, float spacing) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        float w = -spacing;
        for (int i = 0; i < s.length(); i++) {
            w += fm.charWidth(s.charAt(i)) + spacing;
        }
        return Math.max(0, w);
    }
}