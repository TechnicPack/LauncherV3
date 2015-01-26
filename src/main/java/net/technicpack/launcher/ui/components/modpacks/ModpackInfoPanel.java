/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.ui.components.modpacks;

import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.TiledBackground;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.feeds.FeedItemView;
import net.technicpack.ui.controls.feeds.HorizontalGallery;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.utilslib.DesktopUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ModpackInfoPanel extends JPanel implements IImageJobListener<ModpackModel> {
    private ResourceLoader resources;
    private ImageRepository<ModpackModel> backgroundRepo;
    private ImageRepository<AuthorshipInfo> avatarRepo;
    private ActionListener modpackRefreshListener;

    private TiledBackground background;
    private HorizontalGallery feedGallery;
    private ModpackBanner banner;
    private ModpackDataDisplay dataDisplay;
    private RoundedButton playButton;
    private RoundedButton deleteButton;

    private ModpackModel modpack;

    public ModpackInfoPanel(ResourceLoader loader, ImageRepository<ModpackModel> iconRepo, ImageRepository<ModpackModel> logoRepo, ImageRepository<ModpackModel> backgroundRepo, ImageRepository<AuthorshipInfo> avatarRepo, ActionListener modpackOptionsListener, ActionListener modpackRefreshListener) {
        this.resources  = loader;
        this.backgroundRepo = backgroundRepo;
        this.avatarRepo = avatarRepo;
        this.modpackRefreshListener = modpackRefreshListener;

        initComponents(iconRepo, logoRepo, modpackOptionsListener);
    }

    public void setModpackIfSame(ModpackModel modpack) {
        if (modpack == this.modpack)
            setModpack(modpack);
    }

    public void setModpack(ModpackModel modpack) {
        for (ActionListener listener : playButton.getActionListeners()) {
            listener.actionPerformed(new ActionEvent(modpack, 0, ""));
        }
        modpackRefreshListener.actionPerformed(new ActionEvent(modpack, 0, ""));
        this.modpack = modpack;
        banner.setModpack(modpack);
        dataDisplay.setModpack(modpack);
        deleteButton.setVisible(modpack.getInstalledPack() != null);

        ImageJob<ModpackModel> job = backgroundRepo.startImageJob(modpack);
        job.addJobListener(this);
        background.setImage(job.getImage());

        feedGallery.removeAll();

        ArrayList<FeedItem> feed = modpack.getFeed();

        if (feed != null) {
            for (int i = 0; i < feed.size(); i++) {
                FeedItem item = feed.get(i);
                FeedItemView itemView = new FeedItemView(resources, item, avatarRepo.startImageJob(item.getAuthorship()));
                itemView.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        clickFeedItem((FeedItemView) e.getSource(), e.getActionCommand());
                    }
                });
                feedGallery.add(itemView);
            }
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
    }

    public RoundedButton getPlayButton() {
        return playButton;
    }

    public RoundedButton getDeleteButton() { return deleteButton; }

    protected void clickLeftFeedButton() {
        feedGallery.selectPreviousComponent();
    }

    protected void clickRightFeedButton() {
        feedGallery.selectNextComponent();
    }

    protected void clickFeedItem(FeedItemView item, String command) {
        Rectangle r = item.getVisibleRect();

        if (r.getSize().equals(item.getSize()))
            DesktopUtils.browseUrl(item.getUrl());
        else
            feedGallery.selectComponent(item);
    }

    private void initComponents(ImageRepository<ModpackModel> iconRepo, ImageRepository<ModpackModel> logoRepo, ActionListener modpackOptionsListener) {
        setLayout(new BorderLayout());

        background = new TiledBackground(null);
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

        banner = new ModpackBanner(resources, iconRepo, modpackOptionsListener);
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
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        feedBottom.add(topline, constraints);

        JLabel toplineLabel = new JLabel(resources.getString("launcher.packfeed.title"));
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

        JLabel label = new JLabel(resources.getString("launcher.packfeed.noupdates"));
        label.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 20));
        label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);

        feedGallery = new HorizontalGallery();
        feedGallery.setNoComponentsMessage(label);
        feedGallery.setBackground(LauncherFrame.COLOR_FEED_BACK);
        feedGallery.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.ipady = 150;
        constraints.fill = GridBagConstraints.BOTH;
        feedBottom.add(feedGallery, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        Component vertFill = Box.createVerticalStrut(12);
        feedBottom.add(vertFill, constraints);

        deleteButton = new RoundedButton(resources.getString("modpackoptions.delete.text"));
        deleteButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        deleteButton.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
        deleteButton.setBackground(LauncherFrame.COLOR_FOOTER);
        deleteButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        deleteButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        deleteButton.setAlignmentX(RIGHT_ALIGNMENT);
        deleteButton.setFocusable(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setShouldShowBackground(true);
        deleteButton.setIconTextGap(8);
        deleteButton.setHoverIcon(new ImageIcon(resources.colorImage(resources.getImage("delete_button.png"), LauncherFrame.COLOR_BLUE)));
        deleteButton.setIcon(new ImageIcon(resources.colorImage(resources.getImage("delete_button.png"), LauncherFrame.COLOR_BUTTON_BLUE)));
        feedBottom.add(deleteButton, new GridBagConstraints(0, 3, 1,1,1,0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0),0,0));

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 0.02;
        constraints.weighty = 0.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        Component horizFill = Box.createGlue();
        feedBottom.add(horizFill, constraints);

        playButton = new RoundedButton(resources.getString("launcher.pack.launch"));
        playButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        playButton.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
        playButton.setBackground(LauncherFrame.COLOR_FOOTER);
        playButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        playButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        playButton.setAlignmentX(RIGHT_ALIGNMENT);
        playButton.setFocusable(false);
        playButton.setContentAreaFilled(false);
        playButton.setShouldShowBackground(true);
        playButton.setIconTextGap(8);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        feedBottom.add(playButton, constraints);

        feedBottom.add(Box.createVerticalGlue(), new GridBagConstraints(0, 4, 2, 1, 1.0,1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
    }

    @Override
    public void jobComplete(ImageJob<ModpackModel> job) {
        if (job.getJobData() == modpack) {
            background.setImage(job.getImage());

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    repaint();
                }
            });
        }
    }
}
