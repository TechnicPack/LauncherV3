package net.technicpack.launcher.ui.components;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.StatBox;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;

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

public class ModpackDataDisplay extends JPanel {
    private ResourceLoader resources;

    public ModpackDataDisplay(ResourceLoader resources) {
        this.resources = resources;

        initComponents();
    }

    private void initComponents() {
        BorderLayout packFeatureLayout = new BorderLayout();
        packFeatureLayout.setHgap(10);
        this.setLayout(packFeatureLayout);
        this.setOpaque(false);

        JPanel imagePanel = new JPanel();
        imagePanel.setOpaque(false);
        imagePanel.setAlignmentX(RIGHT_ALIGNMENT);
        imagePanel.setAlignmentY(TOP_ALIGNMENT);
        this.add(imagePanel, BorderLayout.LINE_START);

        JLabel packImage = new JLabel();
        packImage.setIcon(resources.getIcon("modpack/HexxitFeature.jpg"));
        packImage.setAlignmentX(RIGHT_ALIGNMENT);
        packImage.setPreferredSize(new Dimension(370, 220));
        imagePanel.add(packImage);

        JPanel packInfoPanel = new JPanel();
        packInfoPanel.setLayout(new BoxLayout(packInfoPanel, BoxLayout.PAGE_AXIS));
        packInfoPanel.setOpaque(false);
        packInfoPanel.setAlignmentY(TOP_ALIGNMENT);
        packInfoPanel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
        this.add(packInfoPanel, BorderLayout.CENTER);

        JPanel statsBoxes = new JPanel();
        statsBoxes.setOpaque(false);
        statsBoxes.setLayout(new BoxLayout(statsBoxes, BoxLayout.LINE_AXIS));
        statsBoxes.setAlignmentX(LEFT_ALIGNMENT);
        packInfoPanel.add(statsBoxes);

        StatBox ratings = new StatBox(resources, "RATINGS", 1799);
        ratings.setBackground(LauncherFrame.COLOR_GREEN);
        ratings.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        statsBoxes.add(ratings);

        statsBoxes.add(Box.createRigidArea(new Dimension(5, 0)));

        StatBox downloads = new StatBox(resources, "DOWNLOADS", 80429);
        downloads.setBackground(LauncherFrame.COLOR_BLUE);
        downloads.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        statsBoxes.add(downloads);

        statsBoxes.add(Box.createRigidArea(new Dimension(5, 0)));

        StatBox runs = new StatBox(resources, "RUNS", 172319);
        runs.setBackground(LauncherFrame.COLOR_BLUE);
        runs.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        statsBoxes.add(runs);

        packInfoPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel title = new JLabel("About Modpack");
        title.setFont(resources.getFont("Raleway-ExtraLight.ttf", 24, Font.BOLD));
        title.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setHorizontalTextPosition(SwingConstants.LEFT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        packInfoPanel.add(title);

        packInfoPanel.add(Box.createRigidArea(new Dimension(0,10)));

        JTextArea description = new JTextArea("Gear up and set forth on a campaign worthy of legend, for Hexxit has been unearthed! Dark dungeons, towering spires, weathered ruins and musty tomes lay before you. Lay claim to riches or create your own artifacts, tame beasts and carve out your own story in endless wonder. Alone or with friends, adventure awaits in Hexxit.");
        description.setFont(resources.getFont("OpenSans-Bold.ttf", 13));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setEditable(false);
        description.setHighlighter(null);
        description.setAlignmentX(LEFT_ALIGNMENT);
        description.setForeground(LauncherFrame.COLOR_WHITE_TEXT);

        packInfoPanel.add(description);
    }
}
