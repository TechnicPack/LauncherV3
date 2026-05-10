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

package net.technicpack.launcher.ui.components.news;

import java.awt.Font;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.swing.*;
import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.ImageUtils;

public class AuthorshipWidget extends JPanel implements IImageJobListener<AuthorshipInfo> {
  private JLabel avatarView;
  private ResourceLoader resources;
  private JLabel authorName;
  private JLabel postTime;

  public AuthorshipWidget(ResourceLoader resources) {
    super();

    this.resources = resources;

    initComponents(resources);
  }

  public AuthorshipWidget(
      ResourceLoader resources, AuthorshipInfo authorshipInfo, ImageJob<AuthorshipInfo> avatar) {
    this(resources);
    setAuthorshipInfo(authorshipInfo, avatar);
  }

  private void initComponents(ResourceLoader resources) {
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    setOpaque(false);

    avatarView = new JLabel();
    add(avatarView);

    add(Box.createHorizontalStrut(6));

    authorName = new JLabel("");
    authorName.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12, Font.BOLD));
    authorName.setForeground(UIConstants.COLOR_WHITE_TEXT);
    add(authorName);

    add(Box.createHorizontalStrut(6));

    postTime = new JLabel("");
    postTime.setForeground(UIConstants.COLOR_WHITE_TEXT);
    postTime.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
    add(postTime);

    add(Box.createHorizontalStrut(4));
  }

  private String getDateText(Date date) {
    ZonedDateTime posted = date.toInstant().atZone(ZoneId.systemDefault());
    ZonedDateTime now = ZonedDateTime.now();

    long yearsSince = ChronoUnit.YEARS.between(posted, now);
    long monthsSince = ChronoUnit.MONTHS.between(posted, now);
    long daysSince = ChronoUnit.DAYS.between(posted, now);
    long hoursSince = ChronoUnit.HOURS.between(posted, now);
    long minutesSince = ChronoUnit.MINUTES.between(posted, now);

    if (yearsSince > 1) return resources.getString("time.years", Long.toString(yearsSince));
    else if (yearsSince == 1) return resources.getString("time.year");
    else if (monthsSince > 1) return resources.getString("time.months", Long.toString(monthsSince));
    else if (monthsSince == 1) return resources.getString("time.month");
    else if (daysSince > 1) return resources.getString("time.days", Long.toString(daysSince));
    else if (daysSince == 1) return resources.getString("time.day");
    else if (hoursSince > 1) return resources.getString("time.hours", Long.toString(hoursSince));
    else if (hoursSince == 1) return resources.getString("time.hour");
    else if (minutesSince > 1)
      return resources.getString("time.minutes", Long.toString(minutesSince));
    else return resources.getString("time.minute");
  }

  @Override
  public void jobComplete(ImageJob<AuthorshipInfo> job) {
    updateAvatar(job);
  }

  public void updateAvatar(ImageJob<AuthorshipInfo> job) {
    avatarView.setIcon(
        new ImageIcon(
            resources.getCircleClippedImage(ImageUtils.scaleWithAspectWidth(job.getImage(), 32))));
  }

  public void setAuthorshipInfo(AuthorshipInfo info, ImageJob<AuthorshipInfo> avatar) {
    postTime.setText(
        resources.getString("launcher.news.posted", getDateText(info.getDate())) + " ");
    authorName.setText(info.getUser());
    avatar.addJobListener(this);
    updateAvatar(avatar);
  }
}
