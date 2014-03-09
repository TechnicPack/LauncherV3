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
package net.technicpack.launcher.ui;

import net.technicpack.launcher.lang.IRelocalizableResource;
import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.discover.DiscoverSelector;
import net.technicpack.launcher.ui.components.modpacks.ModpackInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launcher.ui.components.news.NewsInfoPanel;
import net.technicpack.launcher.ui.components.news.NewsSelector;
import net.technicpack.launcher.ui.controls.DraggableFrame;
import net.technicpack.launcher.ui.controls.feeds.CountCircle;
import net.technicpack.launcher.ui.controls.HeaderTab;
import net.technicpack.launcher.ui.controls.TiledBackground;
import net.technicpack.launcher.ui.controls.UserWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LauncherFrame extends DraggableFrame implements IRelocalizableResource {

    private static final int FRAME_WIDTH = 1200;
    private static final int FRAME_HEIGHT = 720;

    private static final int SIDEKICK_WIDTH = 300;
    private static final int SIDEKICK_HEIGHT = 250;

    public static final Color COLOR_RED = new Color(229,0,0);
    public static final Color COLOR_GREEN = new Color(45, 130, 11);
    public static final Color COLOR_BLUE = new Color(16, 108, 163);
    public static final Color COLOR_BLUE_DARKER = new Color(12, 94, 145);
    public static final Color COLOR_WHITE_TEXT = Color.white;
    public static final Color COLOR_CHARCOAL = new Color(31, 31, 31);
    public static final Color COLOR_BANNER = new Color(0, 0, 0, 160);
    public static final Color COLOR_PANEL = new Color(45, 45, 45, 160);
    public static final Color COLOR_SCROLL_TRACK = new Color(18, 18, 18);
    public static final Color COLOR_SCROLL_THUMB = new Color(53, 53, 53);

    public static final String TAB_DISCOVER = "discover";
    public static final String TAB_MODPACKS = "modpacks";
    public static final String TAB_NEWS = "news";

    private ResourceLoader resources;

    private HeaderTab discoverTab;
    private HeaderTab modpacksTab;
    private HeaderTab newsTab;

    private CardLayout selectorLayout;
    private JPanel selectorSwap;
    private CardLayout infoLayout;
    private JPanel infoSwap;

    NewsInfoPanel newsInfoPanel;

    public LauncherFrame(ResourceLoader resources) {
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setUndecorated(true);
        addMouseListener(this);
        addMouseMotionListener(this);

        //Handles rebuilding the frame, so use it to build the frame in the first place
        relocalize(resources);

        selectTab("modpacks");
    }

    /////////////////////////////////////////////////
    // Action responses
    /////////////////////////////////////////////////

    protected void selectTab(String tabName) {
        discoverTab.setIsActive(false);
        modpacksTab.setIsActive(false);
        newsTab.setIsActive(false);

        if (tabName.equalsIgnoreCase(TAB_DISCOVER))
            discoverTab.setIsActive(true);
        else if (tabName.equalsIgnoreCase(TAB_MODPACKS))
            modpacksTab.setIsActive(true);
        else if (tabName.equalsIgnoreCase(TAB_NEWS))
            newsTab.setIsActive(true);

        infoLayout.show(infoSwap, tabName);
        selectorLayout.show(selectorSwap, tabName);
    }

    protected void closeWindow() {
        this.dispose();
    }

    /////////////////////////////////////////////////
    // End Action responses
    /////////////////////////////////////////////////

    private void initComponents() {
        BorderLayout layout = new BorderLayout();
        setLayout(layout);

        /////////////////////////////////////////////////////////////
        //HEADER
        /////////////////////////////////////////////////////////////
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
        header.setBackground(COLOR_BLUE);
        header.setForeground(COLOR_WHITE_TEXT);
        header.setBorder(BorderFactory.createEmptyBorder(0,5,0,10));
        this.add(header, BorderLayout.PAGE_START);

        ImageIcon headerIcon = resources.getIcon("platform_icon_title.png");
        JLabel headerLabel = new JLabel(headerIcon);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(5,8,5,0));
        header.add(headerLabel);

        header.add(Box.createRigidArea(new Dimension(6, 0)));

        ActionListener tabListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTab(e.getActionCommand());
            }
        };

        discoverTab = new HeaderTab(resources.getString("launcher.title.discover"), resources);
        header.add(discoverTab);
        discoverTab.setActionCommand(TAB_DISCOVER);
        discoverTab.addActionListener(tabListener);

        modpacksTab = new HeaderTab(resources.getString("launcher.title.modpacks"), resources);
        modpacksTab.setIsActive(true);
        modpacksTab.setIcon(resources.getIcon("downTriangle.png"));
        modpacksTab.setHorizontalTextPosition(SwingConstants.LEADING);
        modpacksTab.addActionListener(tabListener);
        modpacksTab.setActionCommand(TAB_MODPACKS);
        header.add(modpacksTab);

        newsTab = new HeaderTab(resources.getString("launcher.title.news"), resources);
        newsTab.setLayout(null);
        newsTab.addActionListener(tabListener);
        newsTab.setActionCommand(TAB_NEWS);
        header.add(newsTab);

        CountCircle newsCircle = new CountCircle();
        newsCircle.setBackground(COLOR_RED);
        newsCircle.setForeground(COLOR_WHITE_TEXT);
        newsCircle.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS_BOLD, 14));
        newsCircle.setCount(9);
        newsTab.add(newsCircle);
        newsCircle.setBounds(10,17,25,25);

        header.add(Box.createHorizontalGlue());

        JPanel rightHeaderPanel = new JPanel();
        rightHeaderPanel.setOpaque(false);
        rightHeaderPanel.setLayout(new BoxLayout(rightHeaderPanel, BoxLayout.PAGE_AXIS));
        rightHeaderPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel windowGadgetPanel = new JPanel();
        windowGadgetPanel.setOpaque(false);
        windowGadgetPanel.setLayout(new BoxLayout(windowGadgetPanel, BoxLayout.LINE_AXIS));
        windowGadgetPanel.setAlignmentX(RIGHT_ALIGNMENT);

        ImageIcon minimizeIcon = resources.getIcon("minimize.png");
        JButton minimizeButton = new JButton(minimizeIcon);
        minimizeButton.setBorder(BorderFactory.createEmptyBorder());
        minimizeButton.setContentAreaFilled(false);
        minimizeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        windowGadgetPanel.add(minimizeButton);

        ImageIcon closeIcon = resources.getIcon("close.png");
        JButton closeButton = new JButton(closeIcon);
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        windowGadgetPanel.add(closeButton);

        rightHeaderPanel.add(windowGadgetPanel);
        rightHeaderPanel.add(Box.createVerticalGlue());

        JLabel launcherOptionsLabel = new JLabel(resources.getString("launcher.title.options"));
        launcherOptionsLabel.setIcon(resources.getIcon("options_cog.png"));
        launcherOptionsLabel.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 14));
        launcherOptionsLabel.setForeground(COLOR_WHITE_TEXT);
        launcherOptionsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        launcherOptionsLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        launcherOptionsLabel.setAlignmentX(RIGHT_ALIGNMENT);
        rightHeaderPanel.add(launcherOptionsLabel);

        header.add(rightHeaderPanel);

        /////////////////////////////////////////////////////////////
        // LEFT SETUP
        /////////////////////////////////////////////////////////////
        JPanel selectorContainer = new JPanel();
        this.add(selectorContainer, BorderLayout.LINE_START);

        selectorContainer.setLayout(new BorderLayout());

        selectorSwap = new JPanel();
        selectorSwap.setOpaque(false);
        this.selectorLayout = new CardLayout();
        selectorSwap.setLayout(selectorLayout);
        selectorSwap.add(new DiscoverSelector(resources), "discover");
        selectorSwap.add(new ModpackSelector(resources), "modpacks");
        selectorSwap.add(new NewsSelector(resources), "news");
        selectorContainer.add(selectorSwap, BorderLayout.CENTER);

        JPanel sidekick = new JPanel();
        sidekick.setForeground(COLOR_WHITE_TEXT);
        sidekick.setPreferredSize(new Dimension(SIDEKICK_WIDTH, SIDEKICK_HEIGHT));
        selectorContainer.add(sidekick, BorderLayout.PAGE_END);

        /////////////////////////////////////////////////////////////
        // CENTRAL AREA
        /////////////////////////////////////////////////////////////
        JPanel infoContainer = new JPanel();
        infoContainer.setBackground(COLOR_CHARCOAL);
        infoContainer.setForeground(COLOR_WHITE_TEXT);
        this.add(infoContainer, BorderLayout.CENTER);
        infoContainer.setLayout(new BorderLayout());

        infoSwap = new JPanel();
        infoLayout = new CardLayout();
        infoSwap.setLayout(infoLayout);
        infoSwap.setOpaque(false);
        newsInfoPanel = new NewsInfoPanel(resources);
        infoSwap.add(new DiscoverInfoPanel(resources),"discover");
        infoSwap.add(newsInfoPanel, "news");
        infoSwap.add(new ModpackInfoPanel(resources), "modpacks");
        infoContainer.add(infoSwap, BorderLayout.CENTER);

        TiledBackground footer = new TiledBackground(resources.getImage("background_repeat.png"));
        footer.setLayout(new BoxLayout(footer, BoxLayout.LINE_AXIS));
        footer.setForeground(COLOR_WHITE_TEXT);
        footer.setBorder(BorderFactory.createEmptyBorder(4,6,5,12));

        UserWidget userWidget = new UserWidget(resources);
        footer.add(userWidget);

        footer.add(Box.createHorizontalGlue());

        JLabel buildCtrl = new JLabel(resources.getString("launcher.build.text", Integer.toString(315), resources.getString("launcher.build.stable")));
        buildCtrl.setForeground(COLOR_WHITE_TEXT);
        buildCtrl.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 14));
        buildCtrl.setHorizontalTextPosition(SwingConstants.RIGHT);
        buildCtrl.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(buildCtrl);

        infoContainer.add(footer, BorderLayout.PAGE_END);
    }

    @Override
    public void relocalize(ResourceLoader loader) {
        this.resources = loader;
        this.resources.registerResource(this);

        setIconImage(this.resources.getImage("icon.png"));

        //Wipe controls
        this.getContentPane().removeAll();
        this.setLayout(null);

        //Clear references to existing controls

        initComponents();
    }
}
