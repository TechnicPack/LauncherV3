package net.technicpack.launcher.ui.controls;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;

import javax.swing.*;

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

public class ModpackWidget extends JPanel {

    private ResourceLoader resources;

    private boolean isSelected;

    public ModpackWidget(ResourceLoader resources) {
        this.resources = resources;

        initComponents();
    }

    private void initComponents() {
        setBackground(LauncherFrame.COLOR_PANEL);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4,20,4,8));
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JLabel icon = new JLabel();
        icon.setIcon(resources.getIcon("icon.png"));
        add(icon);

        add(Box.createHorizontalStrut(14));

        JLabel text = new JLabel("Modpack");
        text.setFont(resources.getFont("Raleway-Light.ttf", 15));
        text.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(text);

        add(Box.createHorizontalGlue());

        JLabel updateIcon = new JLabel();
        updateIcon.setIcon(resources.getIcon("update_available.png"));
        add(updateIcon);
    }

    public boolean isSelected() { return isSelected; }
    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
        this.setOpaque(isSelected);
    }
}
