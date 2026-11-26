package view;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import model.Achievement;
import model.AchievementTracker;
import model.PlayerColor;

/**
 * Bonus: the Trophy Room on the End Game screen. Under each rank card it shows the trophies that
 * player earned in this game; the rarest trophy of the game shines. Hover an emblem to see what it is.
 */
final class TrophyRoom extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final int CARD = 230;
    private static final int GAP = 16;
    private static final int R = 19;
    private static final int STEP = 48;
    private static final int PER_ROW = 4;

    private final List<PlayerColor> colors;
    private final List<List<Achievement>> trophies = new ArrayList<>();
    private final Achievement rarest;
    private final PlayerColor rarestOwner;

    TrophyRoom(List<PlayerColor> colors, List<AchievementTracker.Unlock> unlocks) {
        this.colors = colors;
        Achievement best = null;
        PlayerColor owner = null;
        for (AchievementTracker.Unlock u : unlocks)
            if (best == null || u.getAchievement().getTier().ordinal() > best.getTier().ordinal()) {
                best = u.getAchievement();
                owner = u.getColor();
            }
        rarest = best;
        rarestOwner = owner;
        for (PlayerColor c : colors) {
            List<Achievement> mine = new ArrayList<>();
            for (AchievementTracker.Unlock u : unlocks) if (u.getColor() == c) mine.add(u.getAchievement());
            mine.sort((x, y) -> y.getTier().ordinal() - x.getTier().ordinal());   // rarest first
            trophies.add(mine);
        }
        int width = colors.size() * CARD + (colors.size() - 1) * GAP;
        setPreferredSize(new Dimension(width, 30 + STEP));
        setMaximumSize(getPreferredSize());
        setAlignmentX(CENTER_ALIGNMENT);
        setToolTipText("");                         // turns on the per-emblem tool tips below
    }

    /** Width of one column: as wide as a rank card, narrower when the window is small. */
    private float column() {
        int n = colors.size();
        return Math.min(CARD, (getWidth() - (n - 1) * GAP) / (float) n);
    }

    /** How many emblems a column shows; the rest are counted in a "+N" chip. */
    private static int shown(int count) { return count > PER_ROW ? PER_ROW - 1 : count; }

    /** Centre of slot {@code i} (an emblem or the "+N" chip) of column {@code col}. */
    private Point centre(int col, int i, int count) {
        float col0 = column();
        float total = colors.size() * col0 + (colors.size() - 1) * GAP;
        float left = (getWidth() - total) / 2f;
        int slots = Math.min(PER_ROW, count);
        float colCentre = left + col * (col0 + GAP) + col0 / 2f;
        float step = Math.min(STEP, col0 / PER_ROW);
        float x = colCentre - (slots - 1) * step / 2f + i * step;
        return new Point(Math.round(x), 30 + R + 4);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQuality(g2);
        // title in the middle, with fading lines to both sides
        g2.setFont(Theme.semibold(12f));
        String title = "TROPHY ROOM";
        float tw = Theme.trackedWidth(g2, title, 3);
        float cx = getWidth() / 2f;
        g2.setColor(TrophyArt.LEGENDARY);
        Theme.drawTracked(g2, title, cx - tw / 2f, 14, 3);
        float lineY = 10;
        float span = getPreferredSize().width / 2f - tw / 2f - 16;
        g2.setPaint(new GradientPaint(cx - tw / 2f - 12 - span, lineY, Theme.alpha(TrophyArt.LEGENDARY, 0),
                cx - tw / 2f - 12, lineY, Theme.alpha(TrophyArt.LEGENDARY, 140)));
        g2.fillRect(Math.round(cx - tw / 2f - 12 - span), Math.round(lineY), Math.round(span), 1);
        g2.setPaint(new GradientPaint(cx + tw / 2f + 12, lineY, Theme.alpha(TrophyArt.LEGENDARY, 140),
                cx + tw / 2f + 12 + span, lineY, Theme.alpha(TrophyArt.LEGENDARY, 0)));
        g2.fillRect(Math.round(cx + tw / 2f + 12), Math.round(lineY), Math.round(span), 1);

        long now = System.currentTimeMillis();
        for (int col = 0; col < colors.size(); col++) {
            List<Achievement> mine = trophies.get(col);
            if (mine.isEmpty()) {
                Point p = centre(col, 0, 1);
                g2.setFont(Theme.ui(13f, Font.PLAIN));
                g2.setColor(Theme.alpha(Theme.MUTED, 170));
                String none = "no trophies this time";
                g2.drawString(none, p.x - g2.getFontMetrics().stringWidth(none) / 2f, p.y + 5);
                continue;
            }
            int shown = shown(mine.size());
            if (shown < mine.size()) {             // "+N" for the trophies that do not fit
                Point p = centre(col, PER_ROW - 1, mine.size());
                String more = "+" + (mine.size() - shown);
                g2.setColor(Theme.alpha(TrophyArt.COMMON, 40));
                g2.fill(TrophyArt.hexagon(p.x, p.y, R));
                g2.setColor(Theme.alpha(TrophyArt.COMMON, 170));
                g2.setStroke(new BasicStroke(1.4f));
                g2.draw(TrophyArt.hexagon(p.x, p.y, R));
                g2.setFont(Theme.semibold(13f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Theme.TEXT);
                g2.drawString(more, p.x - fm.stringWidth(more) / 2f, p.y + fm.getAscent() / 2f - 2);
            }
            for (int i = 0; i < shown; i++) {
                Point p = centre(col, i, mine.size());
                Achievement a = mine.get(i);
                boolean star = a == rarest && colors.get(col) == rarestOwner;
                float shine = star ? (now % 2600) / 1600f : -1;
                if (star) {
                    float pulse = (float) (.5 + .5 * Math.sin(now / 300.0));
                    Theme.glow(g2, p.x, p.y, R * 2.2f, TrophyArt.color(a), (int) (50 + 50 * pulse));
                }
                TrophyArt.paintEmblem(g2, a, p.x, p.y, star ? R + 3 : R, true, shine <= 1 ? shine : -1);
            }
        }
        g2.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int col = 0; col < colors.size(); col++) {
            List<Achievement> mine = trophies.get(col);
            int shown = shown(mine.size());
            for (int i = 0; i < shown; i++) {
                if (centre(col, i, mine.size()).distance(e.getPoint()) <= R + 2) {
                    Achievement a = mine.get(i);
                    return "<html><b>" + a.getTitle() + "</b> &nbsp;·&nbsp; " + a.getTier().name()
                            + "<br>" + a.getDescription() + "</html>";
                }
            }
            if (shown < mine.size() && centre(col, PER_ROW - 1, mine.size()).distance(e.getPoint()) <= R + 2) {
                StringBuilder rest = new StringBuilder("<html>");
                for (int i = shown; i < mine.size(); i++)
                    rest.append(i > shown ? "<br>" : "").append(mine.get(i).getTitle());
                return rest.append("</html>").toString();
            }
        }
        return null;
    }
}