/*
 * This file is part of Technic UI Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic UI Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic UI Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic UI Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.controls.login;

import net.technicpack.ui.controls.list.AdvancedCellRenderer;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class UserCellRenderer extends AdvancedCellRenderer implements ListCellRenderer, IImageJobListener<IUserType> {
    private Icon addUserIcon;

    private ImageRepository<IUserType> mSkinRepo;

    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;

    private HashMap<String, Icon> headMap = new HashMap<String, Icon>();

    public UserCellRenderer(ResourceLoader resources, ImageRepository<IUserType> skinRepo) {
        this.mSkinRepo = skinRepo;
        addUserIcon = resources.getIcon("add_user.png");
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof IUserType) {
            IUserType mojangUser = (IUserType) value;
            this.setText(mojangUser.getDisplayName());
            this.setIconTextGap(8);

            if (!headMap.containsKey(mojangUser.getUsername())) {
                ImageJob<IUserType> job = mSkinRepo.startImageJob(mojangUser);
                job.addJobListener(this);
                headMap.put(mojangUser.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));
            }

            Icon head = headMap.get(mojangUser.getUsername());

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
    public void jobComplete(ImageJob<IUserType> job) {
        IUserType mojangUser = job.getJobData();
        if (headMap.containsKey(mojangUser.getUsername()))
            headMap.remove(mojangUser.getUsername());

        headMap.put(mojangUser.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));

        this.invalidate();
    }
}
