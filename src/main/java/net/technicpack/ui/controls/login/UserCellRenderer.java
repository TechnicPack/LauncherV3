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

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.ui.controls.list.AdvancedCellRenderer;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.util.HashMap;

public class UserCellRenderer extends AdvancedCellRenderer<IUserType> implements ListCellRenderer<IUserType>, IImageJobListener<IUserType> {
    private Icon addUserIcon;

    private ImageRepository<IUserType> mSkinRepo;

    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;

    private HashMap<String, Icon> headMap = new HashMap<>();

    public UserCellRenderer(ResourceLoader resources, ImageRepository<IUserType> skinRepo) {
        this.mSkinRepo = skinRepo;
        addUserIcon = resources.getIcon("add_user.png");
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends IUserType> list, IUserType user, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, user, index, isSelected, cellHasFocus);

        if (!headMap.containsKey(user.getUsername())) {
            ImageJob<IUserType> job = mSkinRepo.startImageJob(user);
            job.addJobListener(this);
            headMap.put(user.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));
        }

        Icon head = headMap.get(user.getUsername());

        if (head != null) {
            this.setIcon(head);
        }

        return this;
    }

    @Override
    public void jobComplete(ImageJob<IUserType> job) {
        IUserType user = job.getJobData();
        headMap.remove(user.getUsername());

        headMap.put(user.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));

        this.invalidate();
    }
}
