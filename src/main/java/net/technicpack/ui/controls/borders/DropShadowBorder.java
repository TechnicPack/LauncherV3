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

package net.technicpack.ui.controls.borders;

import net.technicpack.contrib.romainguy.FastBlurFilter;
import net.technicpack.utilslib.Utils;

import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Level;

public class DropShadowBorder extends AbstractBorder {
    private Color color;
    private int thickness = 1;
    private Insets insets = null;
    RenderingHints hints;

    public DropShadowBorder(
            Color color) {
        this(color, 3);
    }

    public DropShadowBorder(
            Color color, int thickness) {
        this.thickness = thickness;
        this.color = color;
        hints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        insets = new Insets(thickness*4,thickness*4,thickness*4,thickness*4);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        return getBorderInsets(c);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        BufferedImage shadow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = shadow.createGraphics();
        g2.setRenderingHints(hints);
        Composite oldComposite = g2.getComposite();
        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f);
        g2.setComposite(composite);
        g2.setColor(new Color(0, 0, 0, 0));
        g2.fillRect(0, 0, width, height);
        g2.setComposite(oldComposite);
        g2.setColor(color);
        g2.fillRect(thickness * 4, thickness * 4, width - (thickness * 8), height - (thickness * 8));
        g2.dispose();

        FastBlurFilter blur = new FastBlurFilter(thickness);
        shadow = blur.filter(shadow, null);
        shadow = blur.filter(shadow, null);
        shadow = blur.filter(shadow, null);
        shadow = blur.filter(shadow, null);

        g.drawImage(shadow, x, y, width, height, null);
    }
}
