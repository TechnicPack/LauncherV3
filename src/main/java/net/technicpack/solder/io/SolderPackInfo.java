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

package net.technicpack.solder.io;

import com.google.gson.annotations.SerializedName;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;
import net.technicpack.solder.ISolderPackApi;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused"})
public class SolderPackInfo extends RestObject implements PackInfo {

    private String name;
    @SerializedName("display_name")
    private String displayName;
    private String recommended;
    private String latest;
    private List<String> builds;
    private transient ISolderPackApi solder;
    private transient boolean isLocal = false;

    private SolderPackInfo() {
        // Empty constructor for GSON
    }

    public ISolderPackApi getSolder() {
        return solder;
    }

    public void setSolder(ISolderPackApi solder) {
        this.solder = solder;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getWebSite() {
        return null;
    }

    @Override
    public Resource getIcon() {
        return null;
    }

    @Override
    public Resource getBackground() {
        return null;
    }

    @Override
    public Resource getLogo() {
        return null;
    }

    @Override
    public String getRecommended() {
        return recommended;
    }

    @Override
    public String getLatest() {
        return latest;
    }

    @Override
    public List<String> getBuilds() {
        return builds;
    }

    @Override
    public String getDiscordId() { return null; }

    @Override
    public ArrayList<FeedItem> getFeed() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Integer getLikes() {
        return null;
    }

    @Override
    public Integer getInstalls() {
        return null;
    }

    @Override
    public Integer getRuns() {
        return null;
    }

    @Override
    public boolean isServerPack() { return false; }

    @Override
    public boolean isOfficial() { return false; }

    @Override
    public Modpack getModpack(String build) throws BuildInaccessibleException {
        return solder.getPackBuild(build);
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isLocal() {
        // If this Solder modpack has no builds available, then there's effectively no modpack available, so we
        // consider the modpack as local-only/not enough information available for installation or launch.
        if (builds.isEmpty()) {
            return true;
        }

        return isLocal;
    }

    public void setLocal() { isLocal = true; }

    @Override
    public boolean hasSolder() {
        return true;
    }

    @Override
    public String toString() {
        return "SolderPackInfo{" +
                "name='" + name + '\'' +
                ", display_name='" + displayName + '\'' +
                ", recommended='" + recommended + '\'' +
                ", latest='" + latest + '\'' +
                ", builds=" + builds +
                ", solder=" + solder +
                '}';
    }
}
