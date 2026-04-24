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
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.technicpack.utilslib.Utils;

public class SplashScreen extends JFrame {
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

    // Redraw the image to fix the alpha channel
    BufferedImage alphaImage =
        new BufferedImage(image.getIconWidth(), image.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = alphaImage.createGraphics();
    g.drawImage(img, 0, 0, image.getIconWidth(), image.getIconHeight(), null);
    g.dispose();

    // Draw the image. JLabel rather than JButton because JButton fires full-component
    // repaints on mouse enter/exit (rollover) and press/release, which on a translucent
    // JFrame show as a visible flicker as the buffer gets redrawn. JLabel installs no
    // mouse listeners and stays visually stable under any cursor interaction.
    // Explicit sizing ensures pack() gets exactly the icon dimensions.
    JLabel background = new JLabel(new ImageIcon(alphaImage));
    Dimension iconSize = new Dimension(image.getIconWidth(), image.getIconHeight());
    background.setMinimumSize(iconSize);
    background.setPreferredSize(iconSize);
    container.add(background, BorderLayout.CENTER);

    if (barHeight > 0) {
      progressDisplay = new InstallationProgressDisplay();
      progressDisplay.configureForSplash(computeProgressDisplayWidth(image.getIconWidth()));
      container.add(createProgressFooter(progressDisplay), BorderLayout.SOUTH);
    }

    // Finalize
    this.getRootPane().setOpaque(false);
    try {
      // Try to set a transparent background, but it isn't always supported
      this.setBackground(new Color(0, 0, 0, 0));
    } catch (UnsupportedOperationException e) {
      this.setBackground(new Color(0, 0, 0));
    } catch (IllegalArgumentException e) {
      Utils.getLogger()
          .warning(
              "Your desktop environment does not support translucent windows. Technic Launcher will not look as rad for you.");
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

  static int computeProgressDisplayWidth(int imageWidth) {
    return Math.max(160, imageWidth - (PROGRESS_HORIZONTAL_PADDING * 2));
  }

  static JPanel createProgressFooter(InstallationProgressDisplay progressDisplay) {
    // Semi-transparent dark backdrop: reads as a soft ribbon that gives white progress text a
    // reliable contrast surface without the hard black-slab look of a fully-opaque footer. The
    // RGB matches the launcher palette's central-back colour (25, 30, 34) but it's hard-coded
    // here so this UI-controls module doesn't take a reverse dependency on the launcher layer.
    JPanel footer =
        new JPanel(new BorderLayout()) {
          @Override
          protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
              // AlphaComposite.SRC (not SRC_OVER) overwrites destination pixels instead of
              // blending with them, which is required on a translucent JFrame. SRC_OVER
              // compounds with stale buffer alpha across repaints and causes visible flicker.
              g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.75f));
              g2d.setColor(new Color(25, 30, 34));
              g2d.fillRect(0, 0, getWidth(), getHeight());
            } finally {
              g2d.dispose();
            }
            super.paintComponent(g);
          }
        };
    footer.setOpaque(false);
    footer.setBorder(
        new EmptyBorder(
            4, PROGRESS_HORIZONTAL_PADDING, PROGRESS_BOTTOM_PADDING, PROGRESS_HORIZONTAL_PADDING));
    footer.add(progressDisplay, BorderLayout.CENTER);
    return footer;
  }
}
