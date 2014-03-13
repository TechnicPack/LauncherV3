/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.technicpack.launcher.ui.controls;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TiledBackground extends JPanel {
    private BufferedImage image;
    private int width;
    private int height;
    private boolean filterImage;

    public TiledBackground(BufferedImage image) {
        setImage(image);
    }

    public boolean getFilterImage() { return filterImage; }
    public void setFilterImage(boolean filterImage) { this.filterImage = filterImage; }

    public void setImage(BufferedImage image) {
        this.image = image;

        if (image != null) {
            width = image.getWidth();
            height = image.getHeight();
        } else {
            width = 0;
            height = 0;
        }
    }

    public void paintComponent(Graphics g) {
        int destWidth = getWidth();
        int destHeight = getHeight();

        if (image == null) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, destWidth, destHeight);
            return;
        }

        int startY = 0;

        while (startY < destHeight) {
            int startX = 0;
            int nextStartY = startY + height;

            while (startX < destWidth) {
                int nextStartX = startX + width;

                //draw
                g.drawImage(image, startX, startY, nextStartX, nextStartY, 0, 0, width, height, null);

                startX = nextStartX;
            }

            startY = nextStartY;
        }

        if (filterImage) {
            g.setColor(getBackground());
            g.fillRect(0, 0, destWidth, destHeight);
        }
    }
}
