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
package net.technicpack.launcher.ui.controls.modpacks;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.SelectorWidget;

import javax.swing.*;

public class ModpackWidget extends SelectorWidget {
    public ModpackWidget(ResourceLoader resources) {
        super(resources);
    }

    protected void initComponents() {
        super.initComponents();
        setBorder(BorderFactory.createEmptyBorder(4,20,4,8));

        JLabel icon = new JLabel();
        icon.setIcon(getResources().getIcon("icon.png"));
        add(icon);

        add(Box.createHorizontalStrut(14));

        JLabel text = new JLabel("Modpack");
        text.setFont(getResources().getFont("Raleway-Light.ttf", 15));
        text.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(text);

        add(Box.createHorizontalGlue());

        JLabel updateIcon = new JLabel();
        updateIcon.setIcon(getResources().getIcon("update_available.png"));
        add(updateIcon);
    }
}
