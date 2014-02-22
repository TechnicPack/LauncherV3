package net.technicpack.launcher.ui.components;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.AAJLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

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

public class ModpackBanner extends JPanel {
    private ResourceLoader resources;

    public ModpackBanner(ResourceLoader resources) {
        this.resources = resources;

        initComponents();
    }

    private void initComponents() {
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.add(Box.createRigidArea(new Dimension(20, 10)));

        JLabel modpackIcon = new JLabel();
        modpackIcon.setIcon(resources.getIcon("icon.png"));
        this.add(modpackIcon);

        JPanel modpackNamePanel = new JPanel();
        modpackNamePanel.setOpaque(false);
        modpackNamePanel.setLayout(new BoxLayout(modpackNamePanel, BoxLayout.PAGE_AXIS));
        this.add(modpackNamePanel);

        JLabel modpackName = new AAJLabel("MODPACK");
        modpackName.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        modpackName.setFont(resources.getFont("Raleway-Light.ttf", 26));
        modpackName.setHorizontalTextPosition(SwingConstants.LEFT);
        modpackName.setAlignmentX(LEFT_ALIGNMENT);
        modpackName.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        modpackNamePanel.add(modpackName);

        JPanel modpackTags = new JPanel();
        modpackTags.setLayout(new BoxLayout(modpackTags,BoxLayout.LINE_AXIS));
        modpackTags.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        modpackTags.setOpaque(false);
        modpackTags.setAlignmentX(LEFT_ALIGNMENT);

        modpackTags.add(Box.createRigidArea(new Dimension(2,0)));

        JLabel tagOfficial = new JLabel();
        tagOfficial.setIcon(resources.getIcon("modpack/tag_official.png"));
        modpackTags.add(tagOfficial);

        modpackTags.add(Box.createRigidArea(new Dimension(2,0)));

        JLabel tagSolder = new JLabel();
        tagSolder.setIcon(resources.getIcon("modpack/tag_solder.png"));
        modpackTags.add(tagSolder);

        modpackTags.add(Box.createRigidArea(new Dimension(2,0)));

        JLabel tagOffline = new JLabel();
        tagOffline.setIcon(resources.getIcon("modpack/tag_offline.png"));
        modpackTags.add(tagOffline);

        modpackNamePanel.add(modpackTags);

        this.add(Box.createHorizontalGlue());

        JPanel packDoodads = new JPanel();
        packDoodads.setOpaque(false);
        packDoodads.setLayout(new BoxLayout(packDoodads, BoxLayout.PAGE_AXIS));

        JPanel versionPanel = new JPanel();
        versionPanel.setOpaque(false);
        versionPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        versionPanel.setAlignmentX(RIGHT_ALIGNMENT);
        packDoodads.add(versionPanel);

        JLabel updateReady = new JLabel();
        updateReady.setIcon(resources.getIcon("update_available.png"));
        updateReady.setHorizontalTextPosition(SwingConstants.LEADING);
        updateReady.setHorizontalAlignment(SwingConstants.RIGHT);
        updateReady.setAlignmentX(RIGHT_ALIGNMENT);
        versionPanel.add(updateReady);

        JLabel boldText = new JLabel("Installed Version:");
        boldText.setFont(resources.getFont("Raleway-Light.ttf", 16, Font.BOLD));
        boldText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        boldText.setHorizontalTextPosition(SwingConstants.LEADING);
        boldText.setHorizontalAlignment(SwingConstants.RIGHT);
        boldText.setAlignmentX(RIGHT_ALIGNMENT);
        versionPanel.add(boldText);

        JLabel installedVersion = new AAJLabel("1.0.7");
        installedVersion.setFont(resources.getFont("Raleway-Light.ttf", 16));
        installedVersion.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        installedVersion.setHorizontalTextPosition(SwingConstants.LEADING);
        installedVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        installedVersion.setAlignmentX(RIGHT_ALIGNMENT);
        versionPanel.add(installedVersion);

        packDoodads.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel modpackOptions = new JLabel("Modpack Options");
        Font font = resources.getFont("Raleway-Light.ttf", 15, Font.BOLD);
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        modpackOptions.setFont(font.deriveFont(attributes));
        modpackOptions.setForeground(LauncherFrame.COLOR_BLUE);
        modpackOptions.setHorizontalTextPosition(SwingConstants.RIGHT);
        modpackOptions.setHorizontalAlignment(SwingConstants.RIGHT);
        modpackOptions.setAlignmentX(RIGHT_ALIGNMENT);
        packDoodads.add(modpackOptions);

        this.add(packDoodads);
        this.add(Box.createRigidArea(new Dimension(10, 10)));
    }
}
