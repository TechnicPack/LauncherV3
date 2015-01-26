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

import com.sun.awt.AWTUtilities;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SplashScreen extends JWindow {
    protected final ImageIcon image;
    private ProgressBar progressBar = null;

    public SplashScreen(Image img, int barHeight) {
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
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            container.add(panel, BorderLayout.SOUTH);

            panel.setLayout(new BorderLayout());

            JPanel progressPanel = new JPanel();
            progressPanel.setOpaque(false);
            progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
            panel.add(progressPanel, BorderLayout.CENTER);
            panel.add(Box.createVerticalStrut(barHeight+barHeight-5), BorderLayout.EAST);

            progressBar = new ProgressBar();
            progressPanel.add(Box.createVerticalStrut(barHeight-5));
            progressPanel.add(progressBar);

        }

        // Finalize
        this.getRootPane().setOpaque(false);
        try {
            // Not always supported...
            AWTUtilities.setWindowOpaque(this, false);
        } catch (UnsupportedOperationException e) {
            this.setBackground(new Color(0, 0, 0));
        } catch (IllegalArgumentException ex) {
            Utils.getLogger().warning("Your desktop environment does not support translucent windows.  Technic launcher will not look as rad for you.");
        }
    }

    public ProgressBar getProgressBar() { return progressBar; }
}
