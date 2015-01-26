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

package net.technicpack.launcher.ui.controls.feeds;

import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.components.news.AuthorshipWidget;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.platform.io.FeedItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Date;

public class FeedItemView extends JButton {
    private FeedItem feedItem;

    public FeedItemView(ResourceLoader loader, FeedItem feedItem, ImageJob<AuthorshipInfo> avatar) {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setContentAreaFilled(false);
        this.setFocusable(false);
        this.setBackground(LauncherFrame.COLOR_FEEDITEM_BACK);
        this.setForeground(LauncherFrame.COLOR_HEADER_TEXT);
        this.setFont(loader.getFont(ResourceLoader.FONT_OPENSANS, 12));

        this.feedItem = feedItem;

        add(Box.createVerticalGlue(), new GridBagConstraints(0,0,3,1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        AuthorshipWidget authorship = new AuthorshipWidget(loader, feedItem.getAuthorship(), avatar);
        add(authorship, new GridBagConstraints(0,1,1,1,0.0,0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
        authorship.setBounds(0, 0, getWidth(), getHeight());

        add(Box.createHorizontalGlue(), new GridBagConstraints(1,1,1,1,1.0,0.0,GridBagConstraints.CENTER,GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public String getUrl() {
        return feedItem.getUrl();
    }

    private Dimension getCalcSize() {
        return new Dimension(250, 132);
    }

    @Override
    public Dimension getPreferredSize() {
        return getCalcSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getCalcSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getCalcSize();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
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

        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, 250, 94, 15, 15);

        Shape oldClip = g2d.getClip();
        g2d.clipRect(3, 2, 245, 90);
        g2d.setFont(getFont());
        g2d.setColor(getForeground());

        drawTextUgly(feedItem.getContent(), g2d, 92);
        g2d.setClip(oldClip);
    }

    private void drawTextUgly(String text, Graphics2D g2, int maxY)
    {
        // Ugly code to wrap text
        String textToDraw = text;
        String[] arr = textToDraw.split(" ");
        int nIndex = 0;
        int startX = 4;
        int startY = 3;
        int lineSize = (int)g2.getFontMetrics().getHeight();
        int elipsisSize = (int)g2.getFontMetrics().stringWidth("...");

        while ( nIndex < arr.length )
        {
            int nextStartY = startY + lineSize;

            if (nextStartY > maxY)
                break;

            int nextEndY = nextStartY + lineSize;

            String line = arr[nIndex++];
            int lineWidth = g2.getFontMetrics().stringWidth(line);

            while ( ( nIndex < arr.length ) && (lineWidth < 243) )
            {
                line = line + " " + arr[nIndex];
                nIndex++;

                if (nIndex == arr.length)
                    break;

                lineWidth = g2.getFontMetrics().stringWidth(line+" "+arr[nIndex]);
                if (nextEndY >= maxY)
                    lineWidth += elipsisSize;
            }

            if (nextEndY >= maxY && nIndex < arr.length)
                line += "...";

            g2.drawString(line, startX, startY + g2.getFontMetrics().getAscent());
            startY = nextStartY;
        }
    }
}
