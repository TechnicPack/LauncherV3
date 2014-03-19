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

package net.technicpack.launcher.ui.components.news;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.AAJLabel;

import javax.swing.*;

public class AuthorshipWidget extends JPanel {
    public AuthorshipWidget(ResourceLoader resources) {
        super();

        initComponents(resources);
    }

    private void initComponents(ResourceLoader resources) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setOpaque(false);

        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(resources.getCircleClippedImage("news/AuthorAvatar.jpg")));
        add(label);

        add(Box.createHorizontalStrut(6));

        AAJLabel authorName = new AAJLabel("sct");
        authorName.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS_BOLD, 12));
        authorName.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(authorName);

        add(Box.createHorizontalStrut(6));

        AAJLabel postTime = new AAJLabel(resources.getString("launcher.news.posted", resources.getString("time.days", Integer.toString(3))));
        postTime.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        postTime.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        add(postTime);
    }
}
