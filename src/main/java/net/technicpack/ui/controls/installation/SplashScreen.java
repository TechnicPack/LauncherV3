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

import net.technicpack.utilslib.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

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
        BufferedImage alphaImage = new BufferedImage(image.getIconWidth(), image.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = alphaImage.createGraphics();
        g.drawImage(img, 0, 0, image.getIconWidth(), image.getIconHeight(), null);
        g.dispose();

        // Draw the image
        JButton background = new JButton(new ImageIcon(alphaImage));
        background.setRolloverEnabled(true);
        background.setRolloverIcon(background.getIcon());
        background.setSelectedIcon(background.getIcon());
        background.setDisabledIcon(background.getIcon());
        background.setPressedIcon(background.getIcon());
        background.setFocusable(false);
        background.setContentAreaFilled(false);
        background.setBorderPainted(false);
        background.setOpaque(false);
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
            Utils.getLogger().warning("Your desktop environment does not support translucent windows. Technic Launcher will not look as rad for you.");
        }
    }

    public InstallationProgressDisplay getProgressDisplay() { return progressDisplay; }

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
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(true);
        footer.setBackground(Color.BLACK);
        footer.setBorder(new EmptyBorder(4, PROGRESS_HORIZONTAL_PADDING, PROGRESS_BOTTOM_PADDING, PROGRESS_HORIZONTAL_PADDING));
        footer.add(progressDisplay, BorderLayout.CENTER);
        return footer;
    }
}
