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

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.AAJLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

public class FeedItem extends JComponent {
    private ResourceLoader resources;
    private URL url;
    private String author;
    private String text;
    private BufferedImage authorAvatar;

    private static BufferedImage background;

    public FeedItem(ResourceLoader loader, String text, URL url, String author, BufferedImage avatar) {
        this.setOpaque(false);
        this.setBackground(new Color(0,0,0,0));
        this.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        this.setFont(loader.getFont(ResourceLoader.FONT_OPENSANS, 12));

        if (background == null) {
            background = loader.getImage("news/FeedItem.png");
        }

        this.resources = loader;
        this.url = url;
        this.author = author;
        this.authorAvatar = avatar;
        this.text = text;
    }

    private Dimension getCalcSize() {
        Dimension dimensions = new Dimension(background.getWidth(), background.getHeight());

        if (authorAvatar != null) {
            int withAvatarHeight = (dimensions.height-10)+authorAvatar.getHeight();
            dimensions.height = (withAvatarHeight > dimensions.height)?withAvatarHeight:dimensions.height;
        }

        return dimensions;
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

        g2d.drawImage(background, 0, 0, null);
        g2d.drawImage(authorAvatar, 0, background.getHeight() - 10, null);

        Shape oldClip = g2d.getClip();
        g2d.clipRect(3, 2, background.getWidth() - 5, background.getHeight() - 24);
        g2d.setFont(getFont());

        drawTextUgly(text, g2d, 2+background.getHeight()-24);
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
            int lineWidth = g2.getFontMetrics().stringWidth(line+" "+arr[nIndex]);
            if (nextEndY >= maxY)
                lineWidth += elipsisSize;
            while ( ( nIndex < arr.length ) && (lineWidth < background.getWidth()-7) )
            {
                line = line + " " + arr[nIndex];
                nIndex++;

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
