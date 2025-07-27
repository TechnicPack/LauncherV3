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

import net.technicpack.discord.IDiscordApi;
import net.technicpack.discord.io.Server;
import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.controls.list.SimpleScrollbarUI;
import net.technicpack.ui.controls.feeds.StatBox;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class ModpackDataDisplay extends JPanel implements IImageJobListener<ModpackModel> {
    private final ResourceLoader resources;
    private final ImageRepository<ModpackModel> logoRepo;
    private final IDiscordApi discordApi;

    private JPanel statBoxes;

    private JLabel titleLabel;
    private JTextPane description;
    private JButton packImage;

    private StatBox ratings;
    private StatBox runs;
    private StatBox installs;

    private JPanel discordPanel;
    private JButton discordLabel;
    private JButton countLabel;
    private List<JButton> discordButtons = new ArrayList<>(3);

    private String packSiteUrl;

    private transient ModpackModel currentModpack;

    public ModpackDataDisplay(ResourceLoader resources, ImageRepository<ModpackModel> logoRepo, IDiscordApi api) {
        this.resources = resources;
        this.logoRepo = logoRepo;
        this.discordApi = api;

        initComponents();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, 225);
    }

    public void setModpack(ModpackModel modpack) {
        this.currentModpack = modpack;
        this.packSiteUrl = modpack.getWebSite();

        if (this.packSiteUrl == null)
            this.packSiteUrl = "https://www.technicpack.net/";

        titleLabel.setText(resources.getString("launcher.packstats.title", modpack.getDisplayName()));
        description.setText(modpack.getDescription());

        boolean wasVisible = ratings.isVisible();
        ratings.setVisible(!modpack.isOfficial());
        statBoxes.setVisible(!modpack.isOfficial());

        if (wasVisible == modpack.isOfficial()) {
            if (wasVisible)
                statBoxes.remove(ratings);
            else
                statBoxes.add(ratings, 0);
        }
        ratings.setValue(modpack.getLikes());
        installs.setValue(modpack.getInstalls());
        runs.setValue(modpack.getRuns());

        ImageJob<ModpackModel> job = logoRepo.startImageJob(modpack);
        job.addJobListener(this);
        packImage.setIcon(new ImageIcon(ImageUtils.scaleImage(job.getImage(), 370, 220)));

        SwingUtilities.invokeLater(() -> {
            description.scrollRectToVisible(new Rectangle(new Dimension(1, 1)));
            repaint();
        });

        discordPanel.setVisible(false);
        if (modpack.getDiscordId() != null && !modpack.getDiscordId().isEmpty())
            discordApi.retrieveServer(modpack, modpack.getDiscordId(), this::updateDiscordComponents);
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

        packImage = new JButton(resources.getIcon("modpack/ModImageFiller.png"));
        packImage.setIcon(resources.getIcon("modpack/ModImageFiller.png"));
        packImage.setAlignmentX(RIGHT_ALIGNMENT);
        packImage.setPreferredSize(new Dimension(370, 220));
        packImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        packImage.setBorder(BorderFactory.createEmptyBorder());
        packImage.setContentAreaFilled(false);
        packImage.setFocusPainted(false);
        packImage.addActionListener(e -> DesktopUtils.browseUrl(packSiteUrl));
        imagePanel.add(packImage);

        JPanel packInfoPanel = new JPanel();
        packInfoPanel.setLayout(new GridBagLayout());
        packInfoPanel.setOpaque(false);
        packInfoPanel.setAlignmentY(TOP_ALIGNMENT);
        packInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.add(packInfoPanel, BorderLayout.CENTER);

        statBoxes = new JPanel();
        statBoxes.setLayout(new GridLayout(1, 3, 5, 0));
        statBoxes.setOpaque(false);
        statBoxes.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        ActionListener listener = e -> DesktopUtils.browseUrl(packSiteUrl);

        ratings = new StatBox(resources, resources.getString("launcher.packstats.ratings"), null);
        ratings.setBackground(UIConstants.COLOR_LIKES_BACK);
        ratings.setForeground(UIConstants.COLOR_WHITE_TEXT);
        ratings.addActionListener(listener);
        statBoxes.add(ratings);

        installs = new StatBox(resources, resources.getString("launcher.packstats.installs"), null);
        installs.setBackground(UIConstants.COLOR_FEED_ITEM_BACK);
        installs.setForeground(UIConstants.COLOR_WHITE_TEXT);
        installs.addActionListener(listener);
        statBoxes.add(installs);

        runs = new StatBox(resources, resources.getString("launcher.packstats.runs"), null);
        runs.setBackground(UIConstants.COLOR_FEED_ITEM_BACK);
        runs.setForeground(UIConstants.COLOR_WHITE_TEXT);
        runs.addActionListener(listener);
        statBoxes.add(runs);

        packInfoPanel.add(statBoxes, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        packInfoPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(2, 2, 1, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        discordPanel = new JPanel();
        discordPanel.setOpaque(false);
        discordPanel.setLayout(new GridBagLayout());

        JButton discordImage = new JButton(resources.getIcon("discord.png"));
        discordImage.setContentAreaFilled(false);
        discordImage.setFocusPainted(false);
        discordImage.setBorder(BorderFactory.createEmptyBorder());
        discordImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        discordPanel.add(discordImage, new GridBagConstraints(0, 0, 1, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 0, 0, 3), 0, 0));
        discordButtons.add(discordImage);

        discordLabel = new JButton(resources.getString("launcher.discord.join"));
        discordLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        discordLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 20));
        discordLabel.setContentAreaFilled(false);
        discordLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        discordLabel.setFocusPainted(false);
        discordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        discordPanel.add(discordLabel, new GridBagConstraints(1, 0, 1, 1, 1, 0.5, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        discordButtons.add(discordLabel);

        countLabel = new JButton(resources.getString("launcher.discord.count", Integer.toString(0)));
        countLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        countLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        countLabel.setContentAreaFilled(false);
        countLabel.setBorder(BorderFactory.createEmptyBorder());
        countLabel.setFocusPainted(false);
        countLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        discordPanel.add(countLabel, new GridBagConstraints(1, 1, 1, 1, 1, 0.5, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        discordButtons.add(countLabel);

        packInfoPanel.add(discordPanel, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 4), 0, 0));
        discordPanel.setVisible(false);

        titleLabel = new JLabel(resources.getString("launcher.packstats.title", "Modpack"));
        titleLabel.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 24, Font.BOLD));
        titleLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        titleLabel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DesktopUtils.browseUrl(packSiteUrl+"/about");
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        packInfoPanel.add(titleLabel, new GridBagConstraints(0,0,4,1,1.0,0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0));

        description = new JTextPane();
        description.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        description.setOpaque(false);
        description.setEditable(false);
        description.setHighlighter(null);
        description.setAlignmentX(LEFT_ALIGNMENT);
        description.setForeground(UIConstants.COLOR_WHITE_TEXT);
        description.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MutableAttributeSet attributes = new SimpleAttributeSet(description.getParagraphAttributes());
        StyleConstants.setLineSpacing(attributes, StyleConstants.getLineSpacing(attributes)*1.3f);
        description.setParagraphAttributes(attributes, true);

        description.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DesktopUtils.browseUrl(packSiteUrl+"/about");
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        description.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JScrollPane scrollPane = new JScrollPane(description, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new SimpleScrollbarUI(UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10,10));

        JPanel scrollHostPanel = new JPanel();
        scrollHostPanel.setBackground(UIConstants.COLOR_FEED_BACK);
        scrollHostPanel.setLayout(new BorderLayout());
        scrollHostPanel.add(scrollPane, BorderLayout.CENTER);

        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> ModpackDataDisplay.this.repaint());

        packInfoPanel.add(scrollHostPanel, new GridBagConstraints(0,1,4,1,1.0,1.0,GridBagConstraints.NORTH,GridBagConstraints.BOTH, new Insets(5,0,0,0),0,0));
    }

    @Override
    public void jobComplete(ImageJob<ModpackModel> job) {
        if (job.getJobData() == currentModpack) {
            packImage.setIcon(new ImageIcon(ImageUtils.scaleImage(job.getImage(), 370, 220)));
        }
    }

    private void updateDiscordComponents(ModpackModel pack, Server server) {
        // Ensure that we're on the Event Dispatch Thread before updating the UI components.
        // The current modpack check should also run in the EDT because the current modpack might change while
        // other UI events are being processed.
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateDiscordComponents(pack, server));
            return;
        }

        // If the pack has changed since we requested the Discord server information, ignore the response
        if (this.currentModpack != pack) {
            return;
        }

        // If the server is null or has no invite link, hide the Discord panel
        if (server == null || server.getInviteLink() == null || server.getInviteLink().isEmpty()) {
            this.discordPanel.setVisible(false);
            return;
        }

        this.countLabel.setText(resources.getString("launcher.discord.count", Integer.toString(server.getPresenceCount())));

        if (pack.isOfficial()) {
            this.discordLabel.setText(resources.getString("launcher.discord.official"));
        } else {
            this.discordLabel.setText(resources.getString("launcher.discord.join"));
        }

        for (JButton discordButton : discordButtons) {
            // Clean up any previous action listeners
            for (ActionListener actionListener : discordButton.getActionListeners()) {
                discordButton.removeActionListener(actionListener);
            }

            discordButton.addActionListener(e -> DesktopUtils.browseUrl(server.getInviteLink()));
        }

        this.discordPanel.setVisible(true);
    }
}
