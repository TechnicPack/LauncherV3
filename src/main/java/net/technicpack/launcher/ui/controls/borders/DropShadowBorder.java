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

package net.technicpack.launcher.ui.controls.borders;

import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

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

        insets = new Insets(thickness,thickness,thickness,thickness);
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
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHints(hints);

        g2.setColor(color);
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        g2.fillRoundRect(0, 0, width, height, 8, 8);
        g2.setComposite(oldComposite);
    }
}
