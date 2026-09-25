/*
 * This file is part of Technic UI Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic UI Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic UI Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic UI Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.controls.installation;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SplashScreen extends JFrame {
  private static final Color SPLASH_BACKGROUND = new Color(25, 30, 34);
  private static final int PROGRESS_HORIZONTAL_PADDING = 14;
  private static final int PROGRESS_BOTTOM_PADDING = 10;
  protected final ImageIcon image;
  private InstallationProgressDisplay progressDisplay = null;

  public SplashScreen(Image img, int barHeight) {
    setUndecorated(true);
    setTitle("Technic Launcher");

    this.image = new ImageIcon(img);

    Container container = getContentPane();
    container.setLayout(new BorderLayout());
    container.setBackground(SPLASH_BACKGROUND);

    // Redraw the image to fix the alpha channel
    BufferedImage alphaImage =
        new BufferedImage(image.getIconWidth(), image.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = alphaImage.createGraphics();
    g.drawImage(img, 0, 0, image.getIconWidth(), image.getIconHeight(), null);
    g.dispose();

    // A passive label avoids mouse-triggered button repaints.
    // Reserve the image dimensions plus breathing room on each side.
    JLabel background = new JLabel(new ImageIcon(alphaImage));
    background.setBorder(new EmptyBorder(14, 14, 14, 14));
    Dimension iconSize = new Dimension(image.getIconWidth() + 28, image.getIconHeight() + 28);
    background.setMinimumSize(iconSize);
    background.setPreferredSize(iconSize);
    container.add(background, BorderLayout.CENTER);

    if (barHeight > 0) {
      progressDisplay = new InstallationProgressDisplay();
      progressDisplay.configureForSplash(computeProgressDisplayWidth(image.getIconWidth()));
      container.add(createProgressFooter(progressDisplay), BorderLayout.SOUTH);
    }

    setBackground(SPLASH_BACKGROUND);
    if (getGraphicsConfiguration()
        .getDevice()
        .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
      addComponentListener(
          new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
              setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            }
          });
    }
  }

  public InstallationProgressDisplay getProgressDisplay() {
    return progressDisplay;
  }

  public ProgressBar getProgressBar() {
    if (progressDisplay == null) {
      return null;
    }
    return progressDisplay.getOverallProgressBar();
  }

  private static int computeProgressDisplayWidth(int imageWidth) {
    return Math.max(160, imageWidth - (PROGRESS_HORIZONTAL_PADDING * 2));
  }

  static JPanel createProgressFooter(InstallationProgressDisplay progressDisplay) {
    // An opaque surface clears the previous progress frame before painting its children.
    JPanel footer = new JPanel(new BorderLayout());
    footer.setBackground(SPLASH_BACKGROUND);
    footer.setOpaque(true);
    footer.setBorder(
        new EmptyBorder(
            4, PROGRESS_HORIZONTAL_PADDING, PROGRESS_BOTTOM_PADDING, PROGRESS_HORIZONTAL_PADDING));
    footer.add(progressDisplay, BorderLayout.CENTER);
    return footer;
  }
}
