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

package net.technicpack.launcher.ui.controls.login;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launchercore.auth.User;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class UserCellRenderer extends JLabel implements ListCellRenderer, IImageJobListener<User> {
    private Font textFont;
    private Icon addUserIcon;

    private ImageRepository<User> mSkinRepo;

    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;

    private HashMap<String, Icon> headMap = new HashMap<String, Icon>();

    public UserCellRenderer(Font font, ResourceLoader resources, ImageRepository<User> skinRepo) {
        this.mSkinRepo = skinRepo;
        this.textFont = font;
        setOpaque(true);
        addUserIcon = resources.getIcon("add_user.png");
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
            setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        } else {
            setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
            setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        }

        this.setFont(textFont);
        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        if (value instanceof User) {
            User user = (User) value;
            this.setText(user.getDisplayName());
            this.setIconTextGap(8);

            if (!headMap.containsKey(user.getUsername())) {
                ImageJob<User> job = mSkinRepo.startImageJob(user);
                job.addJobListener(this);
                headMap.put(user.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));
            }

            Icon head = headMap.get(user.getUsername());

            if (head != null) {
                this.setIcon(head);
            }
        } else if (value == null) {
            this.setText("Add New User");
            this.setIconTextGap(8);

            if (addUserIcon != null) {
                this.setIcon(addUserIcon);
            }
        } else {
            this.setIconTextGap(0);
            this.setText(value.toString());
        }

        return this;
    }

    @Override
    public void jobComplete(ImageJob<User> job) {
        User user = job.getJobData();
        if (headMap.containsKey(user.getUsername()))
            headMap.remove(user.getUsername());

        headMap.put(user.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));

        this.invalidate();
    }
}
