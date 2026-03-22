package net.technicpack.ui.controls.installation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.Timer;

public class InlineProgressStrip extends JComponent {
  private static final int DEFAULT_WIDTH = 136;
  private static final int DEFAULT_HEIGHT = 10;
  private static final int ANIMATION_DELAY_MS = 40;

  private final Timer animationTimer;

  private float progress;
  private boolean indeterminate;
  private int animationOffset;
  private Color fillColor = new Color(91, 192, 222);
  private Color trackColor = new Color(17, 24, 30);
  private Color outlineColor = new Color(103, 118, 132, 180);

  public InlineProgressStrip() {
    setOpaque(false);
    setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
    setMinimumSize(new Dimension(96, DEFAULT_HEIGHT));
    setMaximumSize(new Dimension(Integer.MAX_VALUE, DEFAULT_HEIGHT));

    animationTimer =
        new Timer(
            ANIMATION_DELAY_MS,
            e -> {
              animationOffset = (animationOffset + 4) % 2000;
              repaint();
            });
  }

  public float getProgress() {
    return progress;
  }

  public boolean isIndeterminate() {
    return indeterminate;
  }

  public void setFillColor(Color fillColor) {
    this.fillColor = fillColor;
    repaint();
  }

  public void setTrackColor(Color trackColor) {
    this.trackColor = trackColor;
    repaint();
  }

  public void setOutlineColor(Color outlineColor) {
    this.outlineColor = outlineColor;
    repaint();
  }

  public void setProgress(float progress) {
    this.progress = clamp(progress);
    this.indeterminate = false;
    animationTimer.stop();
    repaint();
  }

  public void setIndeterminate(boolean indeterminate) {
    this.indeterminate = indeterminate;
    if (indeterminate) {
      if (!animationTimer.isRunning()) {
        animationTimer.start();
      }
    } else {
      animationTimer.stop();
    }
    repaint();
  }

  public void reset() {
    this.progress = 0.0f;
    this.indeterminate = false;
    this.animationOffset = 0;
    animationTimer.stop();
    repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    Graphics2D g2d = (Graphics2D) graphics.create();
    try {
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int width = Math.max(0, getWidth() - 2);
      int height = Math.max(0, getHeight() - 2);
      int arc = Math.max(4, height);
      RoundRectangle2D.Float trackShape =
          new RoundRectangle2D.Float(1.0f, 1.0f, width, height, arc, arc);

      g2d.setColor(trackColor);
      g2d.fill(trackShape);

      Shape previousClip = g2d.getClip();
      g2d.clip(trackShape);

      if (indeterminate) {
        paintIndeterminateFill(g2d, width, height, arc);
      } else if (progress > 0.0f) {
        int fillWidth = Math.max(height, Math.round(width * (progress / 100.0f)));
        g2d.setColor(fillColor);
        g2d.fillRoundRect(1, 1, fillWidth, height, arc, arc);
      }

      g2d.setClip(previousClip);

      g2d.setColor(outlineColor);
      g2d.setStroke(new BasicStroke(1f));
      g2d.draw(trackShape);
    } finally {
      g2d.dispose();
    }
  }

  private void paintIndeterminateFill(Graphics2D g2d, int width, int height, int arc) {
    int segmentWidth = Math.max(18, width / 4);
    int travel = width + segmentWidth;
    int x = (animationOffset % travel) - segmentWidth;

    g2d.setColor(fillColor);
    g2d.fillRoundRect(x + 1, 1, segmentWidth, height, arc, arc);
    g2d.fillRoundRect(x - travel + 1, 1, segmentWidth, height, arc, arc);
  }

  private static float clamp(float progress) {
    if (progress < 0.0f) {
      return 0.0f;
    }
    if (progress > 100.0f) {
      return 100.0f;
    }
    return progress;
  }
}
