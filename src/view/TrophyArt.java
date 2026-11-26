package view;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import model.Achievement;
import model.PlayerColor;

/**
 * Bonus: draws the trophy emblems. Each trophy is a glowing hexagon in its rarity colour with
 * its own icon, drawn with shapes so it looks the same on every computer.
 */
final class TrophyArt {
    static final Color COMMON = new Color(198, 166, 255);
    static final Color RARE = new Color(83, 224, 160);
    static final Color EPIC = new Color(255, 122, 214);
    static final Color LEGENDARY = new Color(255, 199, 92);
    private static final Color LOCKED_FILL = new Color(36, 32, 64);
    private static final Color LOCKED_LINE = new Color(92, 86, 136);

    private TrophyArt() { }

    static Color tierColor(Achievement.Tier tier) {
        switch (tier) {
            case RARE:      return RARE;
            case EPIC:      return EPIC;
            case LEGENDARY: return LEGENDARY;
            default:        return COMMON;
        }
    }

    static Color color(Achievement a) { return tierColor(a.getTier()); }

    /** The same player colours as the board's pieces. */
    static Color playerColor(PlayerColor color) {
        switch (color) {
            case RED:    return new Color(241, 91, 105);
            case GREEN:  return new Color(83, 204, 146);
            case YELLOW: return new Color(245, 205, 79);
            default:     return new Color(94, 159, 240);
        }
    }

    /** A hexagon with a point at the top, centred on (cx, cy). */
    static Path2D hexagon(float cx, float cy, float r) {
        Path2D hex = new Path2D.Float();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(-90 + 60 * i);
            float x = cx + (float) (Math.cos(angle) * r);
            float y = cy + (float) (Math.sin(angle) * r);
            if (i == 0) hex.moveTo(x, y);
            else hex.lineTo(x, y);
        }
        hex.closePath();
        return hex;
    }

    /**
     * Paints one emblem. {@code earned == false} draws it dark and locked. {@code shine} (0..1)
     * moves a light reflection across the emblem; use -1 for none.
     */
    static void paintEmblem(Graphics2D g, Achievement a, float cx, float cy, float r, boolean earned, float shine) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQuality(g2);
        Color c = color(a);
        Path2D outer = hexagon(cx, cy, r);
        if (earned) {
            Theme.glow(g2, cx, cy, r * 1.5f, c, a.getTier() == Achievement.Tier.LEGENDARY ? 120 : 80);
            g2.setPaint(new GradientPaint(cx, cy - r, Theme.mix(c, Color.WHITE, .18f),
                    cx, cy + r, Theme.mix(c, new Color(18, 12, 36), .62f)));
            g2.fill(outer);
            // inner hexagon line and a light top half
            g2.setColor(Theme.alpha(Color.WHITE, 70));
            g2.setStroke(new BasicStroke(Math.max(1f, r * .05f)));
            g2.draw(hexagon(cx, cy, r * .80f));
            Shape oldClip = g2.getClip();
            g2.clip(outer);
            g2.setColor(Theme.alpha(Color.WHITE, 26));
            g2.fill(new java.awt.Rectangle.Float(cx - r, cy - r, r * 2, r * .9f));
            if (shine >= 0) {                      // a light band sliding across
                float x = cx - r * 2 + shine * r * 4;
                g2.setPaint(new GradientPaint(x - r * .4f, cy - r, Theme.alpha(Color.WHITE, 0),
                        x, cy, Theme.alpha(Color.WHITE, 120), true));
                Path2D band = new Path2D.Float();
                band.moveTo(x - r * .5f, cy + r);
                band.lineTo(x, cy + r);
                band.lineTo(x + r * .9f, cy - r);
                band.lineTo(x + r * .4f, cy - r);
                band.closePath();
                g2.fill(band);
            }
            g2.setClip(oldClip);
            g2.setColor(Theme.mix(c, Color.WHITE, .35f));
            g2.setStroke(new BasicStroke(Math.max(1.2f, r * .07f)));
            g2.draw(outer);
            paintIcon(g2, a, cx, cy, r * .46f, new Color(255, 255, 255, 240), Theme.alpha(new Color(20, 10, 40), 90));
        } else {
            g2.setColor(LOCKED_FILL);
            g2.fill(outer);
            g2.setColor(LOCKED_LINE);
            g2.setStroke(new BasicStroke(Math.max(1f, r * .05f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{r * .14f, r * .1f}, 0f));
            g2.draw(hexagon(cx, cy, r * .80f));
            g2.setStroke(new BasicStroke(Math.max(1f, r * .06f)));
            g2.draw(outer);
            paintLock(g2, cx, cy, r * .36f, LOCKED_LINE);
        }
        g2.dispose();
    }

    private static void paintLock(Graphics2D g2, float cx, float cy, float k, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(k * .2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new java.awt.geom.Arc2D.Float(cx - k * .55f, cy - k * 1.05f, k * 1.1f, k * 1.1f, 0, 180, java.awt.geom.Arc2D.OPEN));
        g2.draw(new Line2D.Float(cx - k * .55f, cy - k * .5f, cx - k * .55f, cy - k * .1f));
        g2.draw(new Line2D.Float(cx + k * .55f, cy - k * .5f, cx + k * .55f, cy - k * .1f));
        g2.fill(new RoundRectangle2D.Float(cx - k * .85f, cy - k * .15f, k * 1.7f, k * 1.2f, k * .4f, k * .4f));
    }

    /** The icon of a trophy, drawn in a box of about 2k x 2k around (cx, cy). */
    static void paintIcon(Graphics2D g, Achievement a, float cx, float cy, float k, Color ink, Color shadow) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(cx, cy);
        g2.scale(k, k);
        g2.setStroke(new BasicStroke(.17f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (shadow != null) {                       // a soft shadow under the icon
            Graphics2D s = (Graphics2D) g2.create();
            s.translate(0, .08);
            s.setColor(shadow);
            s.setStroke(g2.getStroke());
            drawIcon(s, a);
            s.dispose();
        }
        g2.setColor(ink);
        drawIcon(g2, a);
        g2.dispose();
    }

    private static void drawIcon(Graphics2D g2, Achievement a) {
        switch (a) {
            case HELLO_WORLD: {                     // </>
                Path2D left = new Path2D.Float();
                left.moveTo(-.45, -.55);
                left.lineTo(-.95, 0);
                left.lineTo(-.45, .55);
                g2.draw(left);
                Path2D right = new Path2D.Float();
                right.moveTo(.45, -.55);
                right.lineTo(.95, 0);
                right.lineTo(.45, .55);
                g2.draw(right);
                g2.draw(new Line2D.Float(.18f, -.7f, -.18f, .7f));
                break;
            }
            case INFINITE_LOOP: {                   // infinity sign
                Path2D loop = new Path2D.Float();
                for (int i = 0; i <= 64; i++) {
                    double t = i * Math.PI * 2 / 64;
                    double d = 1 + Math.sin(t) * Math.sin(t);
                    float x = (float) (Math.cos(t) / d * 1.0);
                    float y = (float) (Math.sin(t) * Math.cos(t) / d * 1.25);
                    if (i == 0) loop.moveTo(x, y);
                    else loop.lineTo(x, y);
                }
                g2.draw(loop);
                break;
            }
            case MERGE_CONFLICT: {                  // two branches merging, with a spark
                Path2D branches = new Path2D.Float();
                branches.moveTo(-.55, -.55);
                branches.curveTo(-.55, -.05, 0, -.05, 0, .25);
                branches.moveTo(.55, -.55);
                branches.curveTo(.55, -.05, 0, -.05, 0, .25);
                branches.moveTo(0, .25);
                branches.lineTo(0, .55);
                g2.draw(branches);
                g2.fill(new Ellipse2D.Float(-.78f, -.85f, .46f, .46f));
                g2.fill(new Ellipse2D.Float(.32f, -.85f, .46f, .46f));
                g2.fill(new Ellipse2D.Float(-.23f, .5f, .46f, .46f));
                break;
            }
            case FIREWALL: {                        // a brick wall
                g2.draw(new RoundRectangle2D.Float(-.9f, -.65f, 1.8f, 1.3f, .2f, .2f));
                g2.draw(new Line2D.Float(-.9f, -.22f, .9f, -.22f));
                g2.draw(new Line2D.Float(-.9f, .22f, .9f, .22f));
                g2.draw(new Line2D.Float(-.3f, -.65f, -.3f, -.22f));
                g2.draw(new Line2D.Float(.3f, -.65f, .3f, -.22f));
                g2.draw(new Line2D.Float(0f, -.22f, 0f, .22f));
                g2.draw(new Line2D.Float(-.3f, .22f, -.3f, .65f));
                g2.draw(new Line2D.Float(.3f, .22f, .3f, .65f));
                break;
            }
            case SENIOR_DEV: {                      // a graduation cap
                Path2D top = new Path2D.Float();
                top.moveTo(0, -.75);
                top.lineTo(1, -.3);
                top.lineTo(0, .15);
                top.lineTo(-1, -.3);
                top.closePath();
                g2.fill(top);
                Path2D body = new Path2D.Float();
                body.moveTo(-.55, -.05);
                body.lineTo(-.55, .4);
                body.quadTo(0, .75, .55, .4);
                body.lineTo(.55, -.05);
                g2.draw(body);
                g2.draw(new Line2D.Float(.85f, -.25f, .85f, .45f));
                g2.fill(new Ellipse2D.Float(.72f, .42f, .26f, .3f));
                break;
            }
            case BUG_SQUASHER: {                    // a bug
                g2.fill(new Ellipse2D.Float(-.42f, -.25f, .84f, 1.05f));
                g2.fill(new Ellipse2D.Float(-.26f, -.62f, .52f, .45f));
                for (int i = -1; i <= 1; i++) {
                    float y = .15f + i * .3f;
                    g2.draw(new Line2D.Float(-.42f, y, -.88f, y - .12f + i * .1f));
                    g2.draw(new Line2D.Float(.42f, y, .88f, y - .12f + i * .1f));
                }
                g2.draw(new Line2D.Float(-.12f, -.6f, -.4f, -.92f));
                g2.draw(new Line2D.Float(.12f, -.6f, .4f, -.92f));
                break;
            }
            case DEPLOYED: {                        // a rocket
                Path2D rocket = new Path2D.Float();
                rocket.moveTo(0, -.95);
                rocket.curveTo(.42, -.6, .42, -.05, .3, .35);
                rocket.lineTo(-.3, .35);
                rocket.curveTo(-.42, -.05, -.42, -.6, 0, -.95);
                rocket.closePath();
                g2.fill(rocket);
                Path2D fins = new Path2D.Float();
                fins.moveTo(-.3, .05);
                fins.lineTo(-.65, .5);
                fins.lineTo(-.3, .4);
                fins.moveTo(.3, .05);
                fins.lineTo(.65, .5);
                fins.lineTo(.3, .4);
                g2.draw(fins);
                g2.draw(new Line2D.Float(0f, .55f, 0f, .9f));
                g2.draw(new Line2D.Float(-.2f, .55f, -.25f, .78f));
                g2.draw(new Line2D.Float(.2f, .55f, .25f, .78f));
                Color ink = g2.getColor();
                g2.setColor(new Color(30, 20, 50, ink.getAlpha()));
                g2.fill(new Ellipse2D.Float(-.14f, -.45f, .28f, .28f));
                g2.setColor(ink);
                break;
            }
            case STACK_OVERFLOW: {                  // a stack with an arrow out of the top
                for (int i = 0; i < 3; i++) {
                    float y = .62f - i * .36f;
                    g2.fill(new RoundRectangle2D.Float(-.75f, y - .12f, 1.5f, .24f, .2f, .2f));
                }
                g2.draw(new Line2D.Float(0f, -.25f, 0f, -.95f));
                g2.draw(new Line2D.Float(0f, -.95f, -.3f, -.65f));
                g2.draw(new Line2D.Float(0f, -.95f, .3f, -.65f));
                break;
            }
            case LUCKY_COMMIT: {                    // a four-leaf clover
                float l = .46f;
                g2.fill(new Ellipse2D.Float(-l - .02f, -l - .25f, l, l));
                g2.fill(new Ellipse2D.Float(.02f, -l - .25f, l, l));
                g2.fill(new Ellipse2D.Float(-l - .02f, -.21f, l, l));
                g2.fill(new Ellipse2D.Float(.02f, -.21f, l, l));
                Path2D stem = new Path2D.Float();
                stem.moveTo(0, .1);
                stem.quadTo(.1, .6, .4, .85);
                g2.draw(stem);
                break;
            }
            case COMEBACK_KID: {                    // a line that drops, then shoots up
                Path2D line = new Path2D.Float();
                line.moveTo(-.9, -.2);
                line.lineTo(-.45, .45);
                line.lineTo(-.05, .1);
                line.lineTo(.75, -.7);
                g2.draw(line);
                Path2D head = new Path2D.Float();
                head.moveTo(.3, -.75);
                head.lineTo(.85, -.8);
                head.lineTo(.8, -.25);
                g2.draw(head);
                break;
            }
            default: {                              // FLAWLESS_BUILD: a diamond
                Path2D gem = new Path2D.Float();
                gem.moveTo(-.85, -.3);
                gem.lineTo(-.45, -.75);
                gem.lineTo(.45, -.75);
                gem.lineTo(.85, -.3);
                gem.lineTo(0, .85);
                gem.closePath();
                g2.draw(gem);
                g2.draw(new Line2D.Float(-.85f, -.3f, .85f, -.3f));
                Path2D facets = new Path2D.Float();
                facets.moveTo(-.45, -.75);
                facets.lineTo(-.2, -.3);
                facets.lineTo(0, .85);
                facets.lineTo(.2, -.3);
                facets.lineTo(.45, -.75);
                g2.draw(facets);
                break;
            }
        }
    }
}