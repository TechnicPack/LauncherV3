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

package net.technicpack.launcher.ui.controls.feeds;

import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.launcher.ui.controls.SelectorWidget;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.platform.io.NewsArticle;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.text.DateFormat;
import java.util.Calendar;

public class NewsWidget extends SelectorWidget implements IImageJobListener<AuthorshipInfo> {
    private NewsArticle article;
    private ImageJob<AuthorshipInfo> avatar;
    private JLabel avatarView;

    public NewsWidget(ResourceLoader resources, NewsArticle article, ImageJob<AuthorshipInfo> avatar) {
        super(resources);

        this.article = article;
        this.avatar = avatar;

        avatar.addJobListener(this);

        initComponents();

        setAvatar(avatar);
    }

    protected void initComponents() {
        super.initComponents();
        setBorder(BorderFactory.createEmptyBorder(8,10,8,15));

        avatarView = new JLabel();
        add(avatarView);

        add(Box.createHorizontalStrut(10));

        JLabel text = new JLabel(article.getTitle());
        text.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 14));
        text.setForeground(UIConstants.COLOR_WHITE_TEXT);
        text.setPreferredSize(new Dimension(200, text.getPreferredSize().height));
        add(text);

        add(Box.createHorizontalGlue());
        add(Box.createHorizontalStrut(5));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(article.getDate());

        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
        JLabel date = new JLabel(format.format(article.getDate()));
        date.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 12));
        date.setForeground(UIConstants.COLOR_DIM_TEXT);
        add(date);
    }

    public NewsArticle getArticle() { return article; }

    @Override
    public void jobComplete(ImageJob<AuthorshipInfo> job) {
        setAvatar(job);
    }

    private void setAvatar(ImageJob<AuthorshipInfo> job) {
        avatarView.setIcon(new ImageIcon(getResources().getCircleClippedImage(ImageUtils.scaleWithAspectWidth(job.getImage(), 32))));
    }
}
