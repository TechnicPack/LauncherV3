package net.technicpack.launcher.ui.controls;

import net.technicpack.launcher.lang.ResourceLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

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

public class FeedItem extends JComponent {
    private ResourceLoader resources;
    private String text;
    private URL url;
    private String author;
    private BufferedImage authorAvatar;

    private static BufferedImage background;

    public FeedItem(ResourceLoader loader, String text, URL url, String author, BufferedImage avatar) {
        if (background == null) {
            background = loader.getImage("news/FeedItem.png");
        }

        this.resources = loader;
        this.text = text;
        this.url = url;
        this.author = author;
        this.authorAvatar = avatar;
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
        g.drawImage(background, 0, 0, null);
        g.drawImage(authorAvatar, 0, background.getHeight()-10, null);
    }
}
