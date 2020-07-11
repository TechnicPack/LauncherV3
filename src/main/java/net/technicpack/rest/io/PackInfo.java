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

    String getName();

    String getDisplayName();

    String getWebSite();

    Resource getIcon();

    Resource getBackground();

    Resource getLogo();

    String getRecommended();

    String getLatest();

    List<String> getBuilds();

    boolean shouldForceDirectory();

    ArrayList<FeedItem> getFeed();

    String getDescription();

    Integer getRuns();

    Integer getDownloads();

    Integer getLikes();

    Modpack getModpack(String build) throws BuildInaccessibleException;

    boolean isComplete();

    boolean isLocal();

    boolean isServerPack();

    boolean isOfficial();

    boolean hasSolder();

    String getDiscordId();
}
