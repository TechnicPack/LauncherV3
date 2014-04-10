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

package net.technicpack.launcher.ui.controls.installation;

import javax.swing.*;
import java.awt.*;

public class ProgressBar extends JLabel {
    public ProgressBar() {
        super("Working...");
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        int width = getWidth()-3;
        int height = getHeight()-3;
        int x = 1;
        int y = 1;

        Insets insets = getBorder().getBorderInsets(this);

        x += insets.left;
        y += insets.top;

        width -= (insets.left + insets.right);
        height -= (insets.top + insets.bottom);

        g2d.setColor(getBackground());
        g2d.fillRoundRect(x, y, width, height, height, height);
        g2d.setColor(getForeground());
        Stroke stroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, width, height, height, height);
        g2d.setStroke(stroke);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(32000,32000);
    }
}
