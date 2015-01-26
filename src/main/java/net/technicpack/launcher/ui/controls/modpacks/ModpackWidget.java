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

package net.technicpack.launcher.ui.controls.modpacks;

import net.technicpack.ui.controls.borders.DropShadowBorder;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.SelectorWidget;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class ModpackWidget extends SelectorWidget implements IImageJobListener<ModpackModel> {
    private ModpackModel modpack;
    private ImageJob<ModpackModel> imageJob;
    private ResourceLoader resources;

    private JLabel icon;
    private JLabel displayName;
    private JLabel updateIcon;

    public ModpackWidget(ResourceLoader resources, ModpackModel modpack, ImageJob<ModpackModel> job) {
        super(resources);

        this.resources = resources;
        this.imageJob = job;
        imageJob.addJobListener(this);
        this.modpack = modpack;
        initComponents();
    }

    public ModpackModel getModpack() {
        return modpack;
    }

    protected void initComponents() {
        super.initComponents();
        setBorder(BorderFactory.createEmptyBorder(4,20,4,8));
        setLayout(new GridBagLayout());

        icon = new JLabel();
        icon.setIcon(new ImageIcon(ImageUtils.scaleWithAspectWidth(imageJob.getImage(), 32)));
        add(icon, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,14),0,0));

        displayName = new JLabel(modpack.getDisplayName());
        displayName.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 14));
        displayName.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        displayName.setMaximumSize(new Dimension(210, displayName.getPreferredSize().height));
        add(displayName, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
        updateIcon = new JLabel();
        updateIcon.setIcon(getResources().getIcon("update_available.png"));
        add(updateIcon, new GridBagConstraints(2,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,5),0,0));
        updateIcon.setVisible(modpack.hasRecommendedUpdate());
    }

    public void updateFromPack(ImageJob<ModpackModel> job) {
        displayName.setText(modpack.getDisplayName());
        updateIcon.setVisible(modpack.hasRecommendedUpdate());
        icon.setIcon(new ImageIcon(ImageUtils.scaleWithAspectWidth(job.getImage(), 32)));
        job.addJobListener(this);
    }

    @Override
    public void jobComplete(ImageJob<ModpackModel> job) {
        icon.setIcon(new ImageIcon(ImageUtils.scaleWithAspectWidth(job.getImage(), 32)));
        revalidate();
    }

    @Override
    public JToolTip createToolTip() {
        JToolTip toolTip = new JToolTip();
        toolTip.setBackground(LauncherFrame.COLOR_FOOTER);
        toolTip.setForeground(LauncherFrame.COLOR_GREY_TEXT);
        toolTip.setBorder(BorderFactory.createCompoundBorder(new LineBorder(LauncherFrame.COLOR_GREY_TEXT), BorderFactory.createEmptyBorder(5,5,5,5)));
        toolTip.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));

        return toolTip;
    }
}
