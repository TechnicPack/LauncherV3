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

import net.technicpack.launchercore.modpacks.packinfo.CombinedPackInfo;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.modpacks.ModpackTag;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.install.Version;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.Map;

public class ModpackBanner extends JPanel implements IImageJobListener<ModpackModel> {
    private ResourceLoader resources;
    private ImageRepository<ModpackModel> iconRepo;
    private ModpackModel currentModpack;
    private ActionListener modpackOptionsListener;

    private JLabel modpackName;
    private JPanel modpackTags;
    private JLabel updateReady;
    private JLabel versionText;
    private JLabel installedVersion;
    private JLabel modpackIcon;
    private JLabel modpackOptions;

    public ModpackBanner(ResourceLoader resources, ImageRepository<ModpackModel> iconRepo, ActionListener modpackOptionsListener) {
        this.resources = resources;
        this.iconRepo = iconRepo;
        this.modpackOptionsListener = modpackOptionsListener;

        initComponents();
    }

    public void setModpack(ModpackModel modpack) {
        currentModpack = modpack;
        modpackName.setText(modpack.getDisplayName());

        modpackOptions.setVisible(!modpack.isLocalOnly() || modpack.getInstalledVersion() != null);

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
        job.addJobListener(this);

        BufferedImage icon = job.getImage();
        if (icon.getWidth() > 50 || icon.getHeight() > 50)
            icon = ImageUtils.scaleImage(icon, 50, 50);

        modpackIcon.setIcon(new ImageIcon(icon));

        rebuildTags(modpack);
    }

    protected void rebuildTags(ModpackModel modpack) {
        modpackTags.removeAll();

        if (modpack.isOfficial())
            addTag("launcher.pack.tag.official", LauncherFrame.COLOR_BLUE);

        if (modpack.getPackInfo() instanceof SolderPackInfo || modpack.getPackInfo() instanceof CombinedPackInfo)
            addTag("launcher.pack.tag.solder", LauncherFrame.COLOR_GREEN);

        if (modpack.isServerPack())
            addTag("launcher.pack.tag.server", LauncherFrame.COLOR_SERVER);

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
        tag.setForeground(Color.white);
        tag.setBackground(lineColor);
        tag.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 11));
        modpackTags.add(tag);
    }

    protected void openModpackOptions() {
        if (currentModpack != null)
            modpackOptionsListener.actionPerformed(new ActionEvent(currentModpack, 0, ""));
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

        modpackName = new JLabel("Modpack");
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
        versionText.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        versionText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        versionText.setHorizontalTextPosition(SwingConstants.LEADING);
        versionText.setHorizontalAlignment(SwingConstants.RIGHT);
        versionText.setAlignmentX(RIGHT_ALIGNMENT);
        versionText.setVisible(false);
        versionPanel.add(versionText);

        installedVersion = new JLabel("1.0.7");
        installedVersion.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        installedVersion.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        installedVersion.setHorizontalTextPosition(SwingConstants.LEADING);
        installedVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        installedVersion.setAlignmentX(RIGHT_ALIGNMENT);
        installedVersion.setVisible(false);
        versionPanel.add(installedVersion);

        packDoodads.add(Box.createRigidArea(new Dimension(0, 5)));

        modpackOptions = new JLabel(resources.getString("launcher.packbanner.options"));
        modpackOptions.setIcon(new ImageIcon(resources.colorImage(resources.getImage("options_cog.png"), LauncherFrame.COLOR_BUTTON_BLUE)));
        modpackOptions.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Font font = resources.getFont(ResourceLoader.FONT_OPENSANS, 14);
        modpackOptions.setFont(font);
        modpackOptions.setForeground(LauncherFrame.COLOR_BLUE);
        modpackOptions.setHorizontalTextPosition(SwingConstants.LEADING);
        modpackOptions.setHorizontalAlignment(SwingConstants.RIGHT);
        modpackOptions.setAlignmentX(RIGHT_ALIGNMENT);

        modpackOptions.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openModpackOptions();
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

        packDoodads.add(modpackOptions);

        this.add(packDoodads);
        this.add(Box.createRigidArea(new Dimension(8, 10)));
    }

    @Override
    public void jobComplete(ImageJob<ModpackModel> job) {
        if (currentModpack == job.getJobData()) {
            BufferedImage icon = job.getImage();
            if (icon.getWidth() > 50 || icon.getHeight() > 50)
                icon = ImageUtils.scaleImage(icon, 50, 50);

            modpackIcon.setIcon(new ImageIcon(icon));
            getParent().invalidate();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getParent().repaint();
                }
            });
        }
    }
}
