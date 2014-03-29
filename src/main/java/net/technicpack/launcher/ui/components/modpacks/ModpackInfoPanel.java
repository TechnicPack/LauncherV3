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
import net.technicpack.launcher.ui.controls.*;
import net.technicpack.launcher.ui.controls.feeds.FeedItem;
import net.technicpack.launcher.ui.controls.feeds.HorizontalGallery;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.utilslib.DesktopUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

public class ModpackInfoPanel extends JPanel {
    private ResourceLoader resources;
    private ImageRepository<ModpackModel> backgroundRepo;

    private BufferedImage defaultImage;

    private TiledBackground background;
    private HorizontalGallery feedGallery;
    private ModpackBanner banner;
    private ModpackDataDisplay dataDisplay;

    private ModpackModel modpack;

    public ModpackInfoPanel(ResourceLoader loader, ImageRepository<ModpackModel> iconRepo, ImageRepository<ModpackModel> logoRepo, ImageRepository<ModpackModel> backgroundRepo) {
        this.resources  = loader;
        this.backgroundRepo = backgroundRepo;

        initComponents(iconRepo, logoRepo);
    }

    public void setModpack(ModpackModel modpack) {
        this.modpack = modpack;
        banner.setModpack(modpack);
        dataDisplay.setModpack(modpack);
        repaint();
    }

    protected void clickLeftFeedButton() {
        feedGallery.selectPreviousComponent();
    }

    protected void clickRightFeedButton() {
        feedGallery.selectNextComponent();
    }

    protected void clickFeedItem(FeedItem item, String command) {
        if (command != null && command.equalsIgnoreCase("discuss")) {
            DesktopUtils.browseUrl(item.getUrl());
        } else
            feedGallery.selectComponent(item);
    }

    private void initComponents(ImageRepository<ModpackModel> iconRepo, ImageRepository<ModpackModel> logoRepo) {
        setLayout(new BorderLayout());

        defaultImage = resources.getImage("modpack/background.png");
        background = new TiledBackground(defaultImage);
        background.setOpaque(true);
        background.setLayout(new BorderLayout());
        background.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        background.setBackground(LauncherFrame.COLOR_CENTRAL_BACK);
        background.setFilterImage(true);
        this.add(background, BorderLayout.CENTER);

        JPanel layoutPanel = new JPanel();
        layoutPanel.setOpaque(false);
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.PAGE_AXIS));
        layoutPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        background.add(layoutPanel,BorderLayout.CENTER);

        banner = new ModpackBanner(resources, iconRepo);
        banner.setBackground(LauncherFrame.COLOR_BANNER);
        banner.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));
        layoutPanel.add(banner);

        JPanel rootFeedPanel = new JPanel();
        BorderLayout rootFeedLayout = new BorderLayout();
        rootFeedLayout.setVgap(10);
        rootFeedPanel.setLayout(rootFeedLayout);
        rootFeedPanel.setOpaque(false);
        rootFeedPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 16));
        layoutPanel.add(rootFeedPanel);

        dataDisplay = new ModpackDataDisplay(resources, logoRepo);
        rootFeedPanel.add(dataDisplay, BorderLayout.PAGE_START);

        JPanel feedBottom = new JPanel();
        feedBottom.setOpaque(false);
        feedBottom.setLayout(new GridBagLayout());
        rootFeedPanel.add(feedBottom, BorderLayout.CENTER);

        JPanel topline = new JPanel();
        topline.setOpaque(false);
        topline.setLayout(new BoxLayout(topline, BoxLayout.LINE_AXIS));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        feedBottom.add(topline, constraints);

        JLabel toplineLabel = new AAJLabel(resources.getString("launcher.packfeed.title"));
        toplineLabel.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 28));
        toplineLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        topline.add(toplineLabel);
        topline.add(Box.createHorizontalGlue());

        JButton leftButton = new JButton(resources.getIcon("status_left.png"));
        leftButton.setBorder(BorderFactory.createEmptyBorder());
        leftButton.setContentAreaFilled(false);
        leftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickLeftFeedButton();
            }
        });
        topline.add(leftButton);

        JButton rightButton = new JButton(resources.getIcon("status_right.png"));
        rightButton.setBorder(BorderFactory.createEmptyBorder());
        rightButton.setContentAreaFilled(false);
        rightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickRightFeedButton();
            }
        });
        topline.add(rightButton);

        feedGallery = new HorizontalGallery();
        feedGallery.setBackground(LauncherFrame.COLOR_FEED_BACK);
        feedGallery.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.ipady = 150;
        constraints.fill = GridBagConstraints.BOTH;
        feedBottom.add(feedGallery, constraints);

        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(resources, "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc facilisis congue dignissim. Aliquam posuere eros vel eros luctus molestie. Duis non massa vel orci sagittis semper. Pellentesque lorem diam, viverra in bibendum in, tincidunt in neque. Curabitur consectetur aliquam sem eget laoreet. Quisque eget turpis a velit semper dictum at ut neque. Nulla placerat odio eget neque commodo posuere. Nam porta lacus elit, a rutrum enim mollis vel.", "http://www.technicpack.net/", "sct", new Date(), resources.getCircleClippedImage("news/AuthorAvatar.jpg"));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clickFeedItem((FeedItem)e.getSource(), e.getActionCommand());
                }
            });
            feedGallery.add(item);
        }

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        Component vertFill = Box.createGlue();
        feedBottom.add(vertFill, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        Component horizFill = Box.createGlue();
        feedBottom.add(horizFill, constraints);

        RoundedButton playButton = new RoundedButton(resources.getString("launcher.pack.launch"));
        playButton.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 27, Font.BOLD));
        playButton.setBorder(BorderFactory.createEmptyBorder(5, 50, 10, 50));
        playButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        playButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        playButton.setContentAreaFilled(false);
        playButton.setAlignmentX(RIGHT_ALIGNMENT);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        feedBottom.add(playButton, constraints);
    }
}
