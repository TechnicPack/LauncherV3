/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.rest.io;


import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.platform.io.FeedItem;

import java.util.ArrayList;
import java.util.List;

public interface PackInfo {

    public String getName();

    public String getDisplayName();

    public String getWebSite();

    public Resource getIcon();

    public Resource getBackground();

    public Resource getLogo();

    public String getRecommended();

    public String getLatest();

    public List<String> getBuilds();

    public boolean shouldForceDirectory();

    public ArrayList<FeedItem> getFeed();

    public String getDescription();

    public Integer getRuns();

    public Integer getDownloads();

    public Integer getLikes();

    public Modpack getModpack(String build) throws BuildInaccessibleException;

    public boolean isComplete();

    public boolean isLocal();

    public boolean isServerPack();
}
