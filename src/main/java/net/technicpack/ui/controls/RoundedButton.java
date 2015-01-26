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
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.LinkedList;

public class RoundedButton extends JButton implements MouseListener {

    private Color hoverBackground;
    private Color clickBackground;

    private Color hoverForeground;
    private Color clickForeground;
    private Icon hoverIcon = null;

    private boolean isHovered = false;
    private boolean isClicked = false;
    private boolean shouldShowBackground = false;

    private Collection<ActionListener> actionListeners = new LinkedList<ActionListener>();

    public RoundedButton(String text)
    {
        super(text);
        addMouseListener(this);
    }

    public Icon getHoverIcon() { return hoverIcon; }
    public Color getHoverBackground() { return (hoverBackground != null) ? hoverBackground : this.getBackground(); }
    public Color getClickBackground() { return (clickBackground != null) ? clickBackground : this.getBackground(); }
    public Color getHoverForeground() { return (hoverForeground != null) ? hoverForeground : this.getForeground(); }
    public Color getClickForeground() { return (clickForeground != null) ? clickForeground : this.getForeground(); }

    public void setHoverIcon(Icon icon) { hoverIcon = icon; }
    public void setHoverBackground(Color color) { hoverBackground = color; }
    public void setClickBackground(Color color) { clickBackground = color; }
    public void setHoverForeground(Color color) { hoverForeground = color; }
    public void setClickForeground(Color color) { clickForeground = color; }

    public boolean shouldShowBackground() { return shouldShowBackground; }
    public void setShouldShowBackground(boolean shouldShowBackground) { this.shouldShowBackground = shouldShowBackground; }

    @Override
    public void paintComponent(Graphics g) {}

    @Override
    public void paint(Graphics g) {
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

        if (shouldShowBackground()) {
            if (isClicked) g2d.setColor(getClickBackground());
            else if (isHovered) g2d.setColor(getHoverBackground());
            else g2d.setColor(getBackground());

            g2d.fillRoundRect(1, 1, getWidth()-3, getHeight()-2, getHeight(), getHeight());
        }

        if (isClicked) g2d.setColor(getClickForeground());
        else if (isHovered) g2d.setColor(getHoverForeground());
        else g2d.setColor(getForeground());

        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, getHeight(), getHeight());
        g2d.setStroke(oldStroke);

        g2d.setFont(getFont());

        int width = g2d.getFontMetrics().stringWidth(getText());

        if (getIcon() != null)
            width += getIcon().getIconWidth() + getIconTextGap();

        int height = ((getHeight() - g2d.getFontMetrics().getHeight()) / 2) + g2d.getFontMetrics().getAscent() - 1;

        int textX = (getWidth() - width) / 2;

        Icon currentIcon = getIcon();

        if (isHovered && getHoverIcon() != null)
            currentIcon = getHoverIcon();

        if (currentIcon != null)
            textX += (getIconTextGap() + currentIcon.getIconWidth());
        g.drawString(getText(), textX, height);

        if (currentIcon != null) {
            currentIcon.paintIcon(this, g2d, (getWidth() - width) / 2,(getHeight() - currentIcon.getIconHeight())/2);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        isClicked = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        isClicked = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        isHovered = true;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        isHovered = false;
    }
}
