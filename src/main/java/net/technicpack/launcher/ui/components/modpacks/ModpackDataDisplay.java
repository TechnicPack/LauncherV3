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
import net.technicpack.launcher.ui.controls.RoundedButton;
import net.technicpack.launcher.ui.controls.SimpleScrollbarUI;
import net.technicpack.launcher.ui.controls.modpacks.StatBox;
import net.technicpack.launchercore.modpacks.ModpackModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class ModpackDataDisplay extends JPanel {
    private ResourceLoader resources;

    private JLabel titleLabel;

    public ModpackDataDisplay(ResourceLoader resources) {
        this.resources = resources;

        initComponents();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, 225);
    }

    public void setModpack(ModpackModel modpack) {
        titleLabel.setText(resources.getString("launcher.packstats.title", modpack.getDisplayName()));

        repaint();
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
        imagePanel.setBorder(BorderFactory.createEmptyBorder());
        imagePanel.setPreferredSize(new Dimension(370, 220));
        this.add(imagePanel, BorderLayout.LINE_START);

        JLabel packImage = new JLabel();
        packImage.setIcon(resources.getIcon("modpack/ModImageFiller.png"));
        packImage.setAlignmentX(RIGHT_ALIGNMENT);
        packImage.setPreferredSize(new Dimension(370, 220));
        imagePanel.add(packImage);

        JPanel packInfoPanel = new JPanel();
        packInfoPanel.setLayout(new GridBagLayout());
        packInfoPanel.setOpaque(false);
        packInfoPanel.setAlignmentY(TOP_ALIGNMENT);
        packInfoPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        this.add(packInfoPanel, BorderLayout.CENTER);

        JPanel statBoxes = new JPanel();
        statBoxes.setLayout(new GridLayout(1,3,5,0));
        statBoxes.setOpaque(false);
        statBoxes.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));

        StatBox ratings = new StatBox(resources, resources.getString("launcher.packstats.ratings"), 1799);
        ratings.setBackground(LauncherFrame.COLOR_LIKES_BACK);
        ratings.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        statBoxes.add(ratings);

        StatBox runs = new StatBox(resources, resources.getString("launcher.packstats.runs"), 172319);
        runs.setBackground(LauncherFrame.COLOR_FEEDITEM_BACK);
        runs.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        statBoxes.add(runs);

        StatBox downloads = new StatBox(resources, resources.getString("launcher.packstats.downloads"), 80429);
        downloads.setBackground(LauncherFrame.COLOR_FEEDITEM_BACK);
        downloads.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        statBoxes.add(downloads);

        packInfoPanel.add(statBoxes, new GridBagConstraints(0,2,3,1,0.0,0.0,GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        packInfoPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(3,2,1,1,1.0,0.0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(5,8,0,0),0,0));

        titleLabel = new AAJLabel(resources.getString("launcher.packstats.title", "Modpack"));
        titleLabel.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 24, Font.BOLD));
        titleLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        packInfoPanel.add(titleLabel, new GridBagConstraints(0,0,4,1,1.0,0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0));

        JTextArea description = new JTextArea("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc facilisis congue dignissim. Aliquam posuere eros vel eros luctus molestie. Duis non massa vel orci sagittis semper. Pellentesque lorem diam, viverra in bibendum in, tincidunt in neque. Curabitur consectetur aliquam sem eget laoreet. Quisque eget turpis a velit semper dictum at ut neque. Nulla placerat odio eget neque commodo posuere. Nam porta lacus elit, a rutrum enim mollis vel.");
        description.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setEditable(false);
        description.setHighlighter(null);
        description.setAlignmentX(LEFT_ALIGNMENT);
        description.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        description.setBorder(BorderFactory.createEmptyBorder(3,5,5,3));

        JScrollPane scrollPane = new JScrollPane(description, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new SimpleScrollbarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10,10));

        JPanel scrollHostPanel = new JPanel();
        scrollHostPanel.setBackground(LauncherFrame.COLOR_FEED_BACK);
        scrollHostPanel.setLayout(new BorderLayout());
        scrollHostPanel.add(scrollPane, BorderLayout.CENTER);

        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(final AdjustmentEvent e) {
                ModpackDataDisplay.this.repaint();
            }
        });

        packInfoPanel.add(scrollHostPanel, new GridBagConstraints(0,1,4,1,1.0,1.0,GridBagConstraints.NORTH,GridBagConstraints.BOTH, new Insets(5,0,0,0),0,0));
    }
}
