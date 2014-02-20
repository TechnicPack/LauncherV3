package net.technicpack.launcher.ui.controls;

import net.technicpack.launcher.lang.ResourceLoader;

import javax.annotation.Resource;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
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

public class CountCircle extends JComponent {
    private int count;

    public CountCircle() {
        setVisible(false);
    }

    public int getCount() { return count; }
    public void setCount(int count) {
        if (count < 0)
            count = 0;

        this.count = count;
        setVisible(count > 0);
    }

    public String getText() {
        if (count > 9)
            return "!";
        else
            return Integer.toString(count);
    }

    @Override
    public void paint(Graphics g) {
        String text = getText();
        g.setFont(getFont());

        Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
        int width = g.getFontMetrics().stringWidth(text);

        double radius = (bounds.getWidth() > bounds.getHeight())?bounds.getWidth():bounds.getHeight();
        int size = (int)Math.ceil(radius);

        g.setColor(getBackground());
        g.fillOval(0, 0, size, size);

        g.setColor(getForeground()); 
        
        g.drawString(text, 1 + (size/2) - (int)(0.5 + bounds.getWidth()/2), (size/2) - (int)(0.5 + bounds.getY() + bounds.getHeight()/2));
    }
}
