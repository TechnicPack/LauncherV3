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

package net.technicpack.launchercore.modpacks.packinfo;

import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;

import java.util.ArrayList;
import java.util.List;

public class CombinedPackInfo implements PackInfo {
    private PackInfo solderPackInfo;
    private PackInfo platformPackInfo;

    public CombinedPackInfo(PackInfo solderPackInfo, PackInfo platformPackInfo) {
        this.solderPackInfo = solderPackInfo;
        this.platformPackInfo = platformPackInfo;
    }

    @Override
    public String getName() {
        if (platformPackInfo != null)
            return platformPackInfo.getName();
        if (solderPackInfo != null)
            return solderPackInfo.getName();

        return null;
    }

    @Override
    public String getDisplayName() {
        if (platformPackInfo != null)
            return platformPackInfo.getDisplayName();
        if (solderPackInfo != null)
            return solderPackInfo.getDisplayName();

        return null;
    }

    @Override
    public String getWebSite() {
        if (platformPackInfo != null)
            return platformPackInfo.getWebSite();
        if (solderPackInfo != null)
            return solderPackInfo.getWebSite();

        return null;
    }

    @Override
    public Resource getIcon() {
        if (platformPackInfo != null)
            return platformPackInfo.getIcon();

        return null;
    }

    @Override
    public Resource getBackground() {
        if (platformPackInfo != null)
            return platformPackInfo.getBackground();
        return null;
    }

    @Override
    public Resource getLogo() {
        if (platformPackInfo != null)
            return platformPackInfo.getLogo();

        return null;
    }

    @Override
    public String getRecommended() {
        if (solderPackInfo != null)
            return solderPackInfo.getRecommended();
        if (platformPackInfo != null)
            return platformPackInfo.getRecommended();

        return null;
    }

    @Override
    public String getLatest() {
        if (solderPackInfo != null)
            return solderPackInfo.getLatest();
        if (platformPackInfo != null)
            return platformPackInfo.getRecommended();

        return null;
    }

    @Override
    public List<String> getBuilds() {
        if (solderPackInfo != null)
            return solderPackInfo.getBuilds();
        if (platformPackInfo != null)
            return platformPackInfo.getBuilds();

        return new ArrayList<String>(0);
    }

    @Override
    public boolean shouldForceDirectory() {
        if (platformPackInfo != null)
            return platformPackInfo.shouldForceDirectory();

        return false;
    }

    @Override
    public ArrayList<FeedItem> getFeed() {
        if (platformPackInfo != null)
            return platformPackInfo.getFeed();

        return null;
    }

    @Override
    public String getDescription() {
        if (platformPackInfo != null)
            return platformPackInfo.getDescription();

        return null;
    }

    @Override
    public Integer getRuns() {
        if (platformPackInfo != null)
            return platformPackInfo.getRuns();
        return null;
    }

    @Override
    public Integer getDownloads() {
        if (platformPackInfo != null)
            return platformPackInfo.getDownloads();

        return null;
    }

    @Override
    public Integer getLikes() {
        if (platformPackInfo != null)
            return platformPackInfo.getLikes();

        return null;
    }

    @Override
    public boolean isServerPack() {
        if (platformPackInfo != null)
            return platformPackInfo.isServerPack();

        return false;
    }

    @Override
    public Modpack getModpack(String build) throws BuildInaccessibleException {
        if (solderPackInfo != null)
            return solderPackInfo.getModpack(build);
        if (platformPackInfo != null)
            return platformPackInfo.getModpack(build);

        return null;
    }

    @Override
    public boolean isComplete() {
        return (platformPackInfo != null);
    }

    @Override
    public boolean isLocal() {
        if (platformPackInfo == null || platformPackInfo.isLocal())
            return true;
        if (solderPackInfo == null || solderPackInfo.isLocal())
            return true;
        return false;
    }
}
