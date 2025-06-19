/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.ui.controls.modpacks;

import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class ModpackTag extends JLabel {
    private static final int HORIZONTAL_PADDING = 8;
    private static final int VERTICAL_PADDING = 4;

    public ModpackTag(String text) {
        super(text);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        int textSize = getFontMetrics(getFont()).getHeight();
        int textWidth = getFontMetrics(getFont()).stringWidth(getText());
        return new Dimension(textWidth + HORIZONTAL_PADDING, textSize + VERTICAL_PADDING);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g2d.setColor(getBackground());
        g2d.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 5, 5);

        g2d.setColor(getForeground());
        g2d.setFont(getFont());

        int textY =
                1 + ((getHeight() - (VERTICAL_PADDING / 2) - g2d.getFontMetrics().getHeight()) / 2) + g2d.getFontMetrics().getAscent();

        g2d.drawString(getText(), HORIZONTAL_PADDING / 2, textY);

        g2d.dispose();
    }
}
