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

package net.technicpack.ui.controls;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TiledBackground extends JPanel {
    private transient BufferedImage image;
    private int imageWidth;
    private int imageHeight;
    private boolean filterImage;

    public TiledBackground(BufferedImage image) {
        setImage(image);
    }

    public boolean getFilterImage() { return filterImage; }
    public void setFilterImage(boolean filterImage) { this.filterImage = filterImage; }

    public void setImage(BufferedImage image) {
        this.image = image;

        if (image != null) {
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
        } else {
            imageWidth = 0;
            imageHeight = 0;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        int destWidth = getWidth();
        int destHeight = getHeight();

        if (image == null) {
            Color staticColor = new Color(this.getBackground().getRGB());
            g.setColor(staticColor);
            g.fillRect(0, 0, destWidth, destHeight);
            return;
        }

        int startY = 0;

        while (startY < destHeight) {
            int startX = 0;
            int nextStartY = startY + imageHeight;

            while (startX < destWidth) {
                int nextStartX = startX + imageWidth;

                //draw
                g.drawImage(image, startX, startY, nextStartX, nextStartY, 0, 0, imageWidth, imageHeight, null);

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
