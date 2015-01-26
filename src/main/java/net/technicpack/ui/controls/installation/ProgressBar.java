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

package net.technicpack.ui.controls.installation;

import net.technicpack.launchercore.util.DownloadListener;

import javax.swing.*;
import java.awt.*;

public class ProgressBar extends JLabel implements DownloadListener {
    float progressPct;
    private Color backFillColor = null;

    public ProgressBar() {
        super("");
    }

    public Color getBackFill() { return backFillColor; }
    public void setBackFill(Color backFill) { backFillColor = backFill; }

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

        int width = getWidth()-3;
        int height = getHeight()-3;
        int x = 1;
        int y = 1;

        Insets insets = new Insets(0,0,0,0);

        if (getBorder() != null)
            insets = getBorder().getBorderInsets(this);

        x += insets.left;
        y += insets.top;

        width -= (insets.left + insets.right);
        height -= (insets.top + insets.bottom);

        Shape clip = g2d.getClip();

        if (getBackFill() != null) {
            g2d.setColor(getBackFill());
            g2d.clipRect(x, y, width, height);
            g2d.fillRoundRect(x, y, width, height, height, height);
        }

        g2d.setColor(getBackground());

        float clipWidth = progressPct * (float)width;
        g2d.clipRect(x, y, (int)clipWidth, height);
        g2d.fillRoundRect(x, y, width, height, height, height);
        g2d.setClip(clip);

        g2d.setColor(getForeground());
        Stroke stroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, width, height, height, height);
        g2d.setStroke(stroke);

        int iconWidth = 0;
        if (this.getIcon() != null) {
            this.getIcon().paintIcon(this, g2d, x+(height/4), y + (height/2 - this.getIcon().getIconHeight()/2));
            iconWidth = this.getIcon().getIconWidth() + this.getIconTextGap();
        }

        g2d.setFont(getFont());
        int textY = y + ((height - g2d.getFontMetrics().getHeight()) / 2) + g2d.getFontMetrics().getAscent();
        int pct = (int)(progressPct * 100);
        String pctText = Integer.toString(pct) + "%";

        int textX = (x+width) - (height/4) - g2d.getFontMetrics().stringWidth(pctText);
        g2d.drawString(pctText, textX, textY);

        g2d.clipRect(x, y, textX-x-3, height);
        g2d.drawString(getText(), x+(height/4)+iconWidth, textY);
        g2d.setClip(clip);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(32000,32000);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0,0);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0,0);
    }

    public void setProgressThreadSafe(final String progressText, final float progress) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                setProgress(progressText, progress/100.0f);
            }
        });
    }

    public void setProgress(String progressText, float progress) {
        setText(progressText);
        this.progressPct = progress;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
    }

    @Override
    public void stateChanged(String fileName, float progress) {
        setProgressThreadSafe(fileName, progress);
    }
}
