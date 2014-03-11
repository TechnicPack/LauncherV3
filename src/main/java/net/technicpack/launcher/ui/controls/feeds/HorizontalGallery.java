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
package net.technicpack.launcher.ui.controls.feeds;

import javax.swing.*;
import java.awt.*;

public class HorizontalGallery extends JPanel {
    public HorizontalGallery() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0,0);
    }

    @Override
    public void paintChildren(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.clipRect(0, 0, getWidth(), getHeight());

        super.paintChildren(g);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setPaint(new GradientPaint(getWidth()-80, 0, new Color(0,0,0,0), getWidth()-10, 0, new Color(0,0,0,255)));
        g2d.fillRect(0,0,getWidth(),getHeight());
    }
}
