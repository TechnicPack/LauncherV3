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
import net.technicpack.launcher.ui.controls.RoundedButton;
import net.technicpack.launcher.ui.controls.SimpleScrollbarUI;
import net.technicpack.launcher.ui.controls.TiledBackground;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.platform.io.AuthorshipInfo;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class NewsInfoPanel extends JPanel {
    private ResourceLoader resources;
    private ImageRepository<AuthorshipInfo> avatarRepo;

    JTextArea newsText;
    JScrollPane newsScroller;
    public NewsInfoPanel(ResourceLoader resources, ImageRepository<AuthorshipInfo> avatarRepo) {

        this.resources = resources;
        this.avatarRepo = avatarRepo;

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20,20,18,16));
        setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        JLabel title = new AAJLabel("Something Updated");
        title.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        title.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 36));
        add(title, new GridBagConstraints(0,0,2,1,1.0,0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0));

        AuthorshipInfo info = new AuthorshipInfo("sct", "http://www.gravatar.com/avatar/2a29f323eddf3410fb1b531b10f7df6f?s=60&d=retro", new Date());
        AuthorshipWidget authorshipInfo = new AuthorshipWidget(resources, info, avatarRepo.startImageJob(info));
        add(authorshipInfo, new GridBagConstraints(0,1,2,1,1.0,0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,0,8,0),0,0));

        newsText = new JTextArea();
        newsText.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        newsText.setOpaque(false);
        newsText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        newsText.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        newsText.setEditable(false);
        newsText.setHighlighter(null);
        newsText.setAlignmentX(LEFT_ALIGNMENT);
        newsText.setLineWrap(true);
        newsText.setWrapStyleWord(true);
        newsText.setText("Attack of the B-Team 1.0.8 is now marked Recommended in the launcher, and users will be asked to update the next time they attempt to launch B-Team. This is a minor version update over 1.0.7a, which fixes a number of crash issues introduced in that version and also updated Open Blocks.\n\nThe build includes the following changes:\n\nDragon Mounts has been updated to version r55.fix- Dragon Mounts no longer crashes clients of users who saw a dragon in SMP without seeing one in SSP in the same Minecraft session.\nWind has been disabled in the WeatherMod config, as it was causing an incompatibility with the latest version of DubStep Gun.\nHats has been reconfigured to use Hat-Hunting Mode as teh default- in Hat-Hunting mode, each user starts with no hats, and you obtain hats by killing hat-wearing mobs. Server owners and users who do not want this change shoul dmodify the config file to change it back.\nMinions has had its mod file renamed to be entirely lower-case. This standard has been adopted to deal with an ongoing issue with Forge in which mods load in different orders based on operating system case-sensitivity, frequently causing server and client to have different entity ID's due to the way that most mods determine those IDs.\nOpen Blocks Mod has been updated to version 1.2.5.\nOpen Mods Lib has been updated to version 0.2.\nServer owners can obtain the new server file from the following link: http://mirror.technicpack.net/Technic/servers/bteam/BTeam_Server_v1.0.8.zip\n\nServer owners looking to update from 1.0.7a can simply update the config files, Open Blocks, Open Mods LIb, Dragon Mounts, and rename Minions to be lower-case.");

        newsScroller = new JScrollPane(newsText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        newsScroller.getVerticalScrollBar().setUI(new SimpleScrollbarUI());
        newsScroller.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        newsScroller.setBorder(BorderFactory.createEmptyBorder());
        newsScroller.setMaximumSize(new Dimension(32000,900));
        newsScroller.setOpaque(false);
        newsScroller.getViewport().setOpaque(false);

        JPanel newsTextPanel = new JPanel();
        newsTextPanel.setLayout(new BoxLayout(newsTextPanel, BoxLayout.PAGE_AXIS));
        newsTextPanel.setOpaque(false);
        newsTextPanel.add(newsScroller);
        newsTextPanel.add(Box.createVerticalGlue());

        add(newsTextPanel, new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 10, 0), 0, 0));

        add(Box.createGlue(), new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        RoundedButton discussButton = new RoundedButton(resources.getString("launcher.news.discuss"));
        discussButton.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 24));
        discussButton.setBorder(BorderFactory.createEmptyBorder(2,25,5,25));
        discussButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        discussButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        discussButton.setAlignmentX(RIGHT_ALIGNMENT);
        discussButton.setContentAreaFilled(false);
        add(discussButton, new GridBagConstraints(1,3,1,1,0.0,0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
    }
}
