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
package net.technicpack.launcher.ui.components.modpacks;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.modpacks.ModpackWidget;
import net.technicpack.launcher.ui.controls.TiledBackground;

import javax.swing.*;
import java.awt.*;

public class ModpackSelector extends TiledBackground {
    private ResourceLoader resources;

    public ModpackSelector(ResourceLoader resources) {
        super(resources.getImage("background_repeat.png"));

        this.resources = resources;

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(9,8,8,8));
        header.setBackground(LauncherFrame.COLOR_PANEL);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        add(header, constraints);

        JLabel filterLabel = new JLabel("FILTER");
        filterLabel.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY,14));
        filterLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        header.add(filterLabel, constraints);

        JTextField filterContents = new JTextField();
        filterContents.setBorder(BorderFactory.createEmptyBorder());

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(0, 8, 0, 0);
        constraints.fill = GridBagConstraints.BOTH;
        header.add(filterContents, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        ModpackWidget modpack = null;

        for (int i = 0; i < 5; i++) {
            modpack = new ModpackWidget(resources);

            if (i == 2)
                modpack.setIsSelected(true);

            add(modpack, constraints);

            constraints.gridy++;
        }

        constraints.weighty = 1.0;
        add(Box.createGlue(), constraints);
    }
}
