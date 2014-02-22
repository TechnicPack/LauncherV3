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
import net.technicpack.launcher.ui.controls.AAJLabel;
import net.technicpack.launcher.ui.controls.modpacks.StatBox;

import javax.swing.*;
import java.awt.*;

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
        packInfoPanel.setLayout(new GridBagLayout());
        packInfoPanel.setOpaque(false);
        packInfoPanel.setAlignmentY(TOP_ALIGNMENT);
        packInfoPanel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
        this.add(packInfoPanel, BorderLayout.CENTER);

        StatBox ratings = new StatBox(resources, "RATINGS", 1799);
        ratings.setBackground(LauncherFrame.COLOR_GREEN);
        ratings.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        packInfoPanel.add(ratings,new GridBagConstraints(0,0,1,1,0.0,0.0,GridBagConstraints.NORTHEAST,GridBagConstraints.BOTH, new Insets(0,0,0,5), 0,0));

        StatBox downloads = new StatBox(resources, "DOWNLOADS", 80429);
        downloads.setBackground(LauncherFrame.COLOR_BLUE);
        downloads.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        packInfoPanel.add(downloads, new GridBagConstraints(1,0,1,1,0.0,0.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0,0,0,5), 0,0));

        StatBox runs = new StatBox(resources, "RUNS", 172319);
        runs.setBackground(LauncherFrame.COLOR_BLUE);
        runs.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        packInfoPanel.add(runs, new GridBagConstraints(2,0,1,1,0.0,0.0,GridBagConstraints.NORTH,GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        packInfoPanel.add(Box.createGlue(), new GridBagConstraints(3,0,1,1,1.0,0.0,GridBagConstraints.NORTH,GridBagConstraints.NONE, new Insets(0,0,0,0),0,0));

        JLabel title = new AAJLabel("About Modpack");
        title.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 24, Font.BOLD));
        title.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setHorizontalTextPosition(SwingConstants.LEFT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        packInfoPanel.add(title, new GridBagConstraints(0,1,4,1,1.0,0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(20,0,0,0),0,0));

        JTextArea description = new JTextArea("Gear up and set forth on a campaign worthy of legend, for Hexxit has been unearthed! Dark dungeons, towering spires, weathered ruins and musty tomes lay before you. Lay claim to riches or create your own artifacts, tame beasts and carve out your own story in endless wonder. Alone or with friends, adventure awaits in Hexxit.");
        description.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setEditable(false);
        description.setHighlighter(null);
        description.setAlignmentX(LEFT_ALIGNMENT);
        description.setForeground(LauncherFrame.COLOR_WHITE_TEXT);

        packInfoPanel.add(description, new GridBagConstraints(0,2,4,1,1.0,1.0,GridBagConstraints.NORTH,GridBagConstraints.BOTH, new Insets(10,0,0,0),0,0));
    }
}
