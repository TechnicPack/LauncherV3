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
package net.technicpack.launcher.ui.controls;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launchercore.auth.User;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class UserWidget extends JPanel implements IImageJobListener<User> {

    private ImageRepository<User> skinRepository;

    private JLabel userName;
    private JLabel avatar;
    private User currentUser;

    public UserWidget(ResourceLoader resources, ImageRepository<User> skinRepository) {
        this.skinRepository = skinRepository;

        initComponents(resources);
    }

    private void initComponents(ResourceLoader resources) {
        setOpaque(false);

        avatar = new JLabel();
        avatar.setIcon(resources.getIcon("news/authorHelm.png"));
        this.add(avatar);

        JLabel staticText = new JLabel("Logged in as");
        staticText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        staticText.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        this.add(staticText);

        userName = new JLabel("");
        userName.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        userName.setBackground(Color.white);
        userName.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15, Font.BOLD));
        this.add(userName);
    }

    public void setUser(User user) {
        currentUser = user;
        userName.setText(user.getDisplayName());

        ImageJob<User> job = skinRepository.startImageJob(currentUser);
        job.addJobListener(this);
        refreshFace(job.getImage());
    }

    private void refreshFace(BufferedImage image) {
        avatar.setIcon(new ImageIcon(ImageUtils.scaleWithAspectWidth(image, 30)));
    }

    @Override
    public void jobComplete(ImageJob<User> job) {
        if (job.getJobData() == currentUser)
            refreshFace(job.getImage());
    }
}
