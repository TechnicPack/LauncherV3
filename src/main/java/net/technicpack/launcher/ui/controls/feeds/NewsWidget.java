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
import net.technicpack.launcher.ui.controls.SelectorWidget;

import javax.swing.*;

public class NewsWidget extends SelectorWidget {

    public NewsWidget(ResourceLoader resources) {
        super(resources);

        initComponents();
    }

    protected void initComponents() {
        super.initComponents();
        setBorder(BorderFactory.createEmptyBorder(8,10,8,8));

        JLabel icon = new JLabel();
        icon.setIcon(new ImageIcon(getResources().getCircleClippedImage("news/AuthorAvatar.jpg")));
        add(icon);

        add(Box.createHorizontalStrut(14));

        JLabel text = new JLabel("Something Updated");
        text.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 14));
        text.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(text);

        add(Box.createHorizontalGlue());

        JLabel date = new JLabel("3/23/14");
        date.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 14));
        date.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(date);
    }
}
