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
import net.technicpack.launcher.ui.controls.modpacks.ModpackTag;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.install.Version;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.logging.Level;

public class ModpackBanner extends JPanel implements IImageJobListener<ModpackModel> {
    private ResourceLoader resources;
    private ImageRepository<ModpackModel> iconRepo;
    private ModpackModel currentModpack;

    private JLabel modpackName;
    private JPanel modpackTags;
    private JLabel updateReady;
    private JLabel versionText;
    private JLabel installedVersion;
    private JLabel modpackIcon;

    public ModpackBanner(ResourceLoader resources, ImageRepository<ModpackModel> iconRepo) {
        this.resources = resources;
        this.iconRepo = iconRepo;

        initComponents();
    }

    public void setModpack(ModpackModel modpack) {
        currentModpack = modpack;
        modpackName.setText(modpack.getDisplayName());

        Version packVersion = modpack.getInstalledVersion();

        if (packVersion == null) {
            updateReady.setVisible(false);
            versionText.setVisible(false);
            installedVersion.setVisible(false);
        } else {
            updateReady.setVisible(modpack.hasRecommendedUpdate());
            versionText.setVisible(true);
            installedVersion.setVisible(true);
            installedVersion.setText(packVersion.getVersion());
        }

        ImageJob<ModpackModel> job = iconRepo.startImageJob(modpack);
        modpackIcon.setIcon(new ImageIcon(job.getImage()));

        rebuildTags(modpack);
    }

    protected void rebuildTags(ModpackModel modpack) {
        modpackTags.removeAll();

        if (!modpack.isPlatform())
            addTag("launcher.pack.tag.official", LauncherFrame.COLOR_BLUE);

        if (modpack.getPackInfo() instanceof SolderPackInfo)
            addTag("launcher.pack.tag.solder", LauncherFrame.COLOR_GREEN);

        if (modpack.isLocalOnly())
            addTag("launcher.pack.tag.offline", LauncherFrame.COLOR_RED);

        if (modpackTags.getComponentCount() == 0) {
            modpackTags.add(Box.createRigidArea(new Dimension(8,14)));
        }

        revalidate();
    }

    protected void addTag(String textString, Color lineColor) {
        modpackTags.add(Box.createRigidArea(new Dimension(5,0)));

        ModpackTag tag = new ModpackTag(resources.getString(textString));
        tag.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        tag.setUnderlineColor(lineColor);
        tag.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 10));
        modpackTags.add(tag);
    }

    private void initComponents() {
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.add(Box.createRigidArea(new Dimension(20, 10)));

        modpackIcon = new JLabel();
        modpackIcon.setIcon(resources.getIcon("icon.png"));
        this.add(modpackIcon);

        JPanel modpackNamePanel = new JPanel();
        modpackNamePanel.setOpaque(false);
        modpackNamePanel.setLayout(new BoxLayout(modpackNamePanel, BoxLayout.PAGE_AXIS));
        this.add(modpackNamePanel);

        modpackName = new AAJLabel("Modpack");
        modpackName.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        modpackName.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 26));
        modpackName.setHorizontalTextPosition(SwingConstants.LEFT);
        modpackName.setAlignmentX(LEFT_ALIGNMENT);
        modpackName.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        modpackName.setOpaque(false);
        modpackNamePanel.add(modpackName);

        modpackTags = new JPanel();
        modpackTags.setLayout(new BoxLayout(modpackTags,BoxLayout.LINE_AXIS));
        modpackTags.setBorder(BorderFactory.createEmptyBorder(0,2,2,2));
        modpackTags.setOpaque(false);
        modpackTags.setAlignmentX(LEFT_ALIGNMENT);

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

        versionPanel.add(Box.createRigidArea(new Dimension(1,20)));

        updateReady = new JLabel();
        updateReady.setIcon(resources.getIcon("update_available.png"));
        updateReady.setHorizontalTextPosition(SwingConstants.LEADING);
        updateReady.setHorizontalAlignment(SwingConstants.RIGHT);
        updateReady.setAlignmentX(RIGHT_ALIGNMENT);
        updateReady.setVisible(false);
        versionPanel.add(updateReady);

        versionText = new JLabel(resources.getString("launcher.packbanner.version"));
        versionText.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 16, Font.BOLD));
        versionText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        versionText.setHorizontalTextPosition(SwingConstants.LEADING);
        versionText.setHorizontalAlignment(SwingConstants.RIGHT);
        versionText.setAlignmentX(RIGHT_ALIGNMENT);
        versionText.setVisible(false);
        versionPanel.add(versionText);

        installedVersion = new AAJLabel("1.0.7");
        installedVersion.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 16));
        installedVersion.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        installedVersion.setHorizontalTextPosition(SwingConstants.LEADING);
        installedVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        installedVersion.setAlignmentX(RIGHT_ALIGNMENT);
        installedVersion.setVisible(false);
        versionPanel.add(installedVersion);

        packDoodads.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel modpackOptions = new JLabel(resources.getString("launcher.packbanner.options"));
        Font font = resources.getFont(ResourceLoader.FONT_RALEWAY, 15, Font.BOLD);
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

    @Override
    public void jobComplete(ImageJob<ModpackModel> job) {
        if (currentModpack == job.getJobData()) {
            modpackIcon.setIcon(new ImageIcon(job.getImage()));
        }
    }
}
