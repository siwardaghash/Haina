package view;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.plaf.LayerUI;
import model.AchievementTracker;

/**
 * Bonus: the "ACHIEVEMENT UNLOCKED" banner. It is drawn on top of the board (as a JLayer
 * decoration), drops in, shows the trophy with a burst of sparks, and slides away by itself
 * after about 3 seconds, without stopping the game. Several unlocks are shown one after another.
 */
final class TrophyBanner extends LayerUI<JComponent> {
    private static final long serialVersionUID = 1L;
    private static final long IN = 420;
    private static final long HOLD = 3000;
    private static final long OUT = 380;

    private final Deque<AchievementTracker.Unlock> queue = new ArrayDeque<>();
    private int seen;
    private AchievementTracker.Unlock current;
    private long shownAt;
    private JComponent layer;
    private final float[][] sparks = new float[22][];
    private final Random random = new Random();
    private final Timer timer = new Timer(16, e -> tick());

    /** Queues the trophies that are new since the last call. */
    void update(List<AchievementTracker.Unlock> unlocks) {
        if (unlocks.size() < seen) seen = 0;               // a new game
        for (int i = seen; i < unlocks.size(); i++) queue.add(unlocks.get(i));
        seen = unlocks.size();
        if (current == null && !queue.isEmpty()) next();
    }

    private void next() {
        current = queue.poll();
        shownAt = System.currentTimeMillis();
        for (int i = 0; i < sparks.length; i++) {         // angle, speed, size
            sparks[i] = new float[]{(float) (random.nextDouble() * Math.PI * 2),
                    60 + random.nextFloat() * 90, 2 + random.nextFloat() * 3};
        }
        Sound.correct();
        timer.start();
    }

    private void tick() {
        if (current != null && System.currentTimeMillis() - shownAt > IN + HOLD + OUT) {
            current = null;
            if (!queue.isEmpty()) next();
        }
        if (current == null) timer.stop();
        if (layer != null) layer.repaint();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        layer = c;
        if (current == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQuality(g2);
        long age = System.currentTimeMillis() - shownAt;
        float in = Theme.clamp01(age / (float) IN);
        float out = Theme.clamp01((age - IN - HOLD) / (float) OUT);
        float alpha = 1f - out;
        int w = 540;
        int h = 96;
        float x = (c.getWidth() - w) / 2f;
        float y = -h - 10 + (h + 30) * Theme.easeOutBack(in) - (h + 40) * Theme.easeOut(out);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Theme.clamp01(alpha)));
        paintBanner(g2, current, x, y, w, h, age);
        g2.dispose();
    }

    private void paintBanner(Graphics2D g2, AchievementTracker.Unlock u, float x, float y, int w, int h, long age) {
        Color c = TrophyArt.color(u.getAchievement());
        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, w, h, 26, 26);
        Theme.glow(g2, x + w / 2f, y + h / 2f, w * .55f, c, 55);
        g2.setPaint(new GradientPaint(x, y, new Color(34, 24, 64, 245), x, y + h, new Color(14, 14, 34, 245)));
        g2.fill(box);
        Shape clip = g2.getClip();
        g2.clip(box);
        Theme.glow(g2, x + 58, y + h / 2f, 90, c, 70);
        g2.setClip(clip);
        g2.setColor(Theme.alpha(c, 210));
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(box);

        // emblem with a burst of sparks
        float ex = x + 56;
        float ey = y + h / 2f;
        float burst = Theme.clamp01(age / 900f);
        if (burst < 1f) {
            for (float[] s : sparks) {
                float d = 20 + s[1] * Theme.easeOut(burst);
                float sx = ex + (float) Math.cos(s[0]) * d;
                float sy = ey + (float) Math.sin(s[0]) * d;
                g2.setColor(Theme.alpha(Theme.mix(c, Color.WHITE, .4f), (int) (230 * (1f - burst))));
                g2.fill(new Ellipse2D.Float(sx - s[2] / 2, sy - s[2] / 2, s[2], s[2]));
            }
        }
        float pop = Theme.easeOutBack(Theme.clamp01((age - 120) / 380f));
        float shine = Theme.clamp01((age - 500) / 700f);
        TrophyArt.paintEmblem(g2, u.getAchievement(), ex, ey, 34 * Math.max(.01f, pop), true, shine < 1 ? shine : -1);

        // texts
        float tx = x + 108;
        g2.setFont(Theme.semibold(11.5f));
        g2.setColor(c);
        float end = Theme.drawTracked(g2, "ACHIEVEMENT UNLOCKED", tx, y + 26, 2.2f);
        g2.setColor(Theme.alpha(c, 160));
        Theme.drawTracked(g2, "  ·  " + u.getAchievement().getTier().name(), end, y + 26, 2.2f);

        g2.setFont(Theme.display(23f));
        g2.setColor(Theme.TEXT);
        g2.drawString(u.getAchievement().getTitle(), tx, y + 55);

        g2.setFont(Theme.semibold(13.5f));
        g2.setColor(TrophyArt.playerColor(u.getColor()));
        String who = u.getPlayer();
        g2.drawString(who, tx, y + 78);
        float ww = g2.getFontMetrics().stringWidth(who);
        g2.setFont(Theme.ui(13f, Font.PLAIN));
        g2.setColor(Theme.MUTED);
        g2.drawString("  ·  " + u.getAchievement().getDescription(), tx + ww, y + 78);
    }
}