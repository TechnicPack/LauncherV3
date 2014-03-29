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

package net.technicpack.launcher.ui.controls.modpacks;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.SelectorWidget;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.modpacks.ModpackModel;

import javax.swing.*;

public class ModpackWidget extends SelectorWidget implements IImageJobListener<ModpackModel> {
    private ModpackModel modpack;
    private ImageJob<ModpackModel> imageJob;

    private JLabel icon;

    public ModpackWidget(ResourceLoader resources, ImageJob<ModpackModel> job) {
        super(resources);

        this.imageJob = job;
        imageJob.addJobListener(this);
        this.modpack = job.getJobData();
        initComponents();
    }

    public ModpackModel getModpack() {
        return modpack;
    }

    protected void initComponents() {
        super.initComponents();
        setBorder(BorderFactory.createEmptyBorder(4,20,4,8));

        icon = new JLabel();
        icon.setIcon(new ImageIcon(imageJob.getImage()));
        add(icon);

        add(Box.createHorizontalStrut(14));

        JLabel text = new JLabel(modpack.getDisplayName());
        text.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 14));
        text.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(text);

        add(Box.createHorizontalGlue());

        if (modpack.hasRecommendedUpdate()) {
            JLabel updateIcon = new JLabel();
            updateIcon.setIcon(getResources().getIcon("update_available.png"));
            add(updateIcon);
        }
    }

    @Override
    public void jobComplete(ImageJob<ModpackModel> job) {
        icon.setIcon(new ImageIcon(job.getImage()));
        revalidate();
    }
}
