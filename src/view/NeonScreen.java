package view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import javax.swing.Timer;


public abstract class NeonScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    protected float animationTime = 0f;
    private final Timer backgroundTimer;

    protected NeonScreen() {
        this(60, 0.04f);
    }

    protected NeonScreen(int frameDelay, float timeStep) {
        setOpaque(false);
        backgroundTimer = new Timer(frameDelay, e -> {
            animationTime += timeStep;
            onTick();
            repaint();
        });
    }

  
    @Override
    protected final void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQuality(g2);      
        paintBackground(g2);          
        paintExtras(g2);              
        g2.dispose();
    }

    @Override
    public final void addNotify() {
        super.addNotify();
        backgroundTimer.start();
        onShown();
    }

    @Override
    public final void removeNotify() {
        backgroundTimer.stop();
        onHidden();
        super.removeNotify();
    }

   
    protected void paintBackground(Graphics2D g2) {
        Theme.paintBackdrop(g2, getWidth(), getHeight(), animationTime);
    }

    protected void paintExtras(Graphics2D g2) {
    }

    protected void onTick() {
    }

    protected void onShown() {
    }

    protected void onHidden() {
    }
}