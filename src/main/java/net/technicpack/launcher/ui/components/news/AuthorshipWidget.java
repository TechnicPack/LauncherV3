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

package net.technicpack.launcher.ui.components.news;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.AAJLabel;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import java.util.Date;

public class AuthorshipWidget extends JPanel implements IImageJobListener<AuthorshipInfo> {
    private ImageJob<AuthorshipInfo> avatar;
    private AuthorshipInfo authorshipInfo;

    private JLabel avatarView;
    private ResourceLoader resources;

    public AuthorshipWidget(ResourceLoader resources, AuthorshipInfo authorshipInfo, ImageJob<AuthorshipInfo> avatar) {
        super();

        this.resources = resources;
        this.avatar = avatar;
        this.authorshipInfo = authorshipInfo;

        initComponents(resources);

        avatar.addJobListener(this);
        updateAvatar(avatar);
    }

    private void initComponents(ResourceLoader resources) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setOpaque(false);

        avatarView = new JLabel();
        add(avatarView);

        add(Box.createHorizontalStrut(6));

        AAJLabel authorName = new AAJLabel(authorshipInfo.getUser());
        authorName.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS_BOLD, 12));
        authorName.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(authorName);

        add(Box.createHorizontalStrut(6));

        AAJLabel postTime = new AAJLabel(resources.getString("launcher.news.posted",getDateText()));
        postTime.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        postTime.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        add(postTime);
    }

    private String getDateText() {
        return "3 days ago";
    }

    @Override
    public void jobComplete(ImageJob<AuthorshipInfo> job) {
        updateAvatar(job);
    }

    public void updateAvatar(ImageJob<AuthorshipInfo> job) {
        avatarView.setIcon(new ImageIcon(resources.getCircleClippedImage(ImageUtils.scaleWithAspectWidth(job.getImage(), 32))));
    }
}
