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
package net.technicpack.launcher.ui.controls;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;

import javax.swing.*;
import java.awt.*;

public class UserWidget extends JPanel {

    private ResourceLoader resources;

    public UserWidget(ResourceLoader resources) {
        this.resources = resources;

        initComponents();
    }

    private void initComponents() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEADING));

        JLabel avatar = new JLabel();
        avatar.setIcon(resources.getIcon("avatarHead.jpg"));
        this.add(avatar);

        JLabel staticText = new JLabel("Logged in as");
        staticText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        staticText.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        this.add(staticText);

        JLabel userName = new JLabel("sct");
        userName.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        userName.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15, Font.BOLD));
        this.add(userName);

        JLabel staticText2 = new JLabel(" | ");
        staticText2.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        staticText2.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        this.add(staticText2);

        JLabel logout = new JLabel("Logout");
        logout.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        logout.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        this.add(logout);
    }
}
