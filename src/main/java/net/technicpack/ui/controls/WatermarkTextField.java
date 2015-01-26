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
import java.awt.event.FocusEvent;

public class WatermarkTextField extends JTextField {
    private String watermarkText;
    private Color watermarkColor;

    public WatermarkTextField(String watermarkText, Color watermarkColor) {
        this.watermarkColor = watermarkColor;
        this.watermarkText = watermarkText;
    }

    @Override
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getText().length() == 0 && !hasFocus()) {
            g.setColor(watermarkColor);

            double height = g.getFontMetrics().getStringBounds(watermarkText, g).getHeight();

            Graphics2D g2d = (Graphics2D)g;

            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.drawString(watermarkText, 6,g.getFontMetrics ().getAscent() + (int)((getHeight() - height)/2));
        }
    }
}
