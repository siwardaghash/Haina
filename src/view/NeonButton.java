package view;

import static view.Theme.TEXT;
import static view.Theme.alpha;
import static view.Theme.applyQuality;
import static view.Theme.easeOut;
import static view.Theme.mix;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.Timer;

/**
 * Glass / neon button used on every screen: icon bubble, label, optional badge,
 * hover fill that sweeps in from the left, and an optional slide-in intro.
 *
 * The component is 10px bigger than the visible pill on every side (room for the glow),
 * so when you call setBounds, add 10px of padding around where you want the pill.
 */
public class NeonButton extends JButton {

    private static final long serialVersionUID = 1L;

    public enum Glyph { PLAY, HISTORY, LOCK, POWER, BACK, DICE }

    public static final int PAD = 10;

    private final Color accent;
    private final Glyph glyph;
    private final String badge;
    private boolean chevron = true;
    private float hover;
    private float target;
    private float intro = 1f;
    private final Timer anim;

    public NeonButton(String text, Color accent, Glyph glyph, String badge) {
        super(text);
        this.accent = accent;
        this.glyph = glyph;
        this.badge = badge;
        setFont(Theme.semibold(19f));
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

    /** Show or hide the "&gt;" arrow on the right (hide it for Back buttons). */
    public void setChevron(boolean chevron) {
        this.chevron = chevron;
        repaint();
    }

    /** 0 = hidden, 1 = fully visible. Used by screens for staggered entrance animations. */
    public void setIntro(float v) {
        if (v != intro) {
            intro = v;
            repaint();
        }
    }

    private void setTarget(float v) {
        target = v;
        if (!anim.isRunning()) {
            anim.start();
        }
    }

    /** Only the visible pill is clickable, not the transparent glow padding. */
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
        float hv = isEnabled() ? hover : 0f;
        Graphics2D g2 = (Graphics2D) g.create();
        applyQuality(g2);
        int w = getWidth();
        int h = getHeight();

        g2.setComposite(AlphaComposite.SrcOver.derive(in * (isEnabled() ? 1f : 0.4f)));
        g2.translate((1f - in) * -50f, 0);
        if (getModel().isArmed() && getModel().isPressed()) {
            g2.translate(w / 2.0, h / 2.0);
            g2.scale(0.97, 0.97);
            g2.translate(-w / 2.0, -h / 2.0);
        }

        RoundRectangle2D r = new RoundRectangle2D.Float(PAD, PAD, w - 2 * PAD, h - 2 * PAD, 22, 22);

        // outer glow
        for (int i = 0; i < PAD; i += 2) {
            int a = (int) (hv * (24 - i * 2));
            if (a > 0) {
                g2.setStroke(new BasicStroke(i * 2 + 2));
                g2.setColor(alpha(accent, a));
                g2.draw(r);
            }
        }

        // glass body
        g2.setPaint(new GradientPaint(0, PAD, new Color(255, 255, 255, 22), 0, h - PAD, new Color(255, 255, 255, 6)));
        g2.fill(r);
        g2.setColor(Theme.GLASS);
        g2.fill(r);

        // accent fill sweeping in from the left on hover
        if (hv > 0.001f) {
            Graphics2D f = (Graphics2D) g2.create();
            f.clip(r);
            f.setPaint(new GradientPaint(PAD, 0, alpha(accent, (int) (170 * hv)),
                    w - PAD, 0, alpha(accent, (int) (40 * hv))));
            f.fill(new Rectangle2D.Float(PAD, PAD, (w - 2 * PAD) * easeOut(hv), h - 2 * PAD));
            f.dispose();
        }

        // border
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(alpha(accent, (int) (110 + 145 * hv)));
        g2.draw(r);

        // icon bubble
        float ic = h - 2 * PAD - 16;
        float ix = PAD + 10;
        float iy = PAD + 8;
        g2.setColor(alpha(accent, (int) (70 + 185 * hv)));
        g2.fill(new Ellipse2D.Float(ix, iy, ic, ic));
        paintGlyph(g2, ix + ic / 2f, iy + ic / 2f, ic * 0.42f);

        // label
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        float tx = ix + ic + 16;
        float ty = h / 2f + (fm.getAscent() - fm.getDescent()) / 2f;
        g2.setColor(mix(getForeground(), Color.WHITE, hv));
        g2.drawString(getText(), tx, ty);

        // badge (e.g. ADMIN)
        if (badge != null) {
            Font bf = getFont().deriveFont(11f);
            g2.setFont(bf);
            FontMetrics bm = g2.getFontMetrics();
            float bw = bm.stringWidth(badge) + 18;
            float bh = 22;
            float bx = w - PAD - (chevron ? 44 : 16) - bw;
            float by = h / 2f - bh / 2f;
            RoundRectangle2D pill = new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh);
            g2.setColor(alpha(accent, 45));
            g2.fill(pill);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(alpha(accent, 200));
            g2.draw(pill);
            g2.setColor(hv > 0.5f ? Color.WHITE : accent);
            g2.drawString(badge, bx + 9, by + bh / 2f + (bm.getAscent() - bm.getDescent()) / 2f);
        }

        // chevron
        if (chevron) {
            float cx = w - PAD - 30 + hv * 6;
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(alpha(Color.WHITE, (int) (110 + 145 * hv)));
            Path2D chev = new Path2D.Float();
            chev.moveTo(cx, h / 2f - 7);
            chev.lineTo(cx + 7, h / 2f);
            chev.lineTo(cx, h / 2f + 7);
            g2.draw(chev);
        }
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
                g.setColor(accent);
                g.fill(new Ellipse2D.Float(cx - s * 0.12f, cy + s * 0.02f, s * 0.24f, s * 0.24f));
                break;
            }
            case BACK: {
                g.draw(new Line2D.Float(cx + s * 0.55f, cy, cx - s * 0.5f, cy));
                Path2D p = new Path2D.Float();
                p.moveTo(cx - s * 0.05f, cy - s * 0.5f);
                p.lineTo(cx - s * 0.55f, cy);
                p.lineTo(cx - s * 0.05f, cy + s * 0.5f);
                g.draw(p);
                break;
            }
            case DICE: {
                g.draw(new RoundRectangle2D.Float(cx - s * 0.62f, cy - s * 0.62f, s * 1.24f, s * 1.24f, s * 0.45f, s * 0.45f));
                float pr = s * 0.14f;
                g.fill(new Ellipse2D.Float(cx - s * 0.27f - pr, cy - s * 0.27f - pr, pr * 2, pr * 2));
                g.fill(new Ellipse2D.Float(cx - pr, cy - pr, pr * 2, pr * 2));
                g.fill(new Ellipse2D.Float(cx + s * 0.27f - pr, cy + s * 0.27f - pr, pr * 2, pr * 2));
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