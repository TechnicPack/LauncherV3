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

package net.technicpack.platform.io;

import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.RestObject;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;

import java.util.ArrayList;
import java.util.List;

public class PlatformPackInfo extends RestObject implements PackInfo {
    private String name;
    private String displayName;
    private String url;
    private String platformUrl;
    private Resource icon;
    private Resource logo;
    private Resource background;
    private String minecraft;
    private String forge;
    private String version;
    private String solder;
    private String description;
    private Integer ratings;
    private Integer runs;
    private Integer downloads;
    private boolean forceDir;
    private boolean isServer;
    private ArrayList<FeedItem> feed = new ArrayList<FeedItem>();

    private transient boolean isLocal = false;

    public PlatformPackInfo() {

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
        return platformUrl;
    }

    @Override
    public Resource getIcon() {
        return icon;
    }

    @Override
    public Resource getBackground() {
        return background;
    }

    @Override
    public Resource getLogo() {
        return logo;
    }

    @Override
    public String getRecommended() {
        return version;
    }

    @Override
    public String getLatest() {
        return version;
    }

    @Override
    public boolean shouldForceDirectory() {
        return forceDir;
    }

    @Override
    public ArrayList<FeedItem> getFeed() {
        return feed;
    }

    @Override
    public List<String> getBuilds() {
        List<String> builds = new ArrayList<String>();
        builds.add(version);
        return builds;
    }

    public String getGameVersion() {
        return minecraft;
    }

    public String getForge() {
        return forge;
    }

    public String getSolder() {
        return solder;
    }

    public boolean hasSolder() {
        return solder != null && !solder.equals("");
    }

    public String getDescription() {
        if (description == null)
            return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc facilisis congue dignissim. Aliquam posuere eros vel eros luctus molestie. Duis non massa vel orci sagittis semper. Pellentesque lorem diam, viverra in bibendum in, tincidunt in neque. Curabitur consectetur aliquam sem eget laoreet. Quisque eget turpis a velit semper dictum at ut neque. Nulla placerat odio eget neque commodo posuere. Nam porta lacus elit, a rutrum enim mollis vel.";

        return description;
    }

    public Integer getLikes() {
        return ratings;
    }

    public Integer getRuns() {
        return runs;
    }

    public Integer getDownloads() {
        return downloads;
    }

    public boolean isServerPack() { return isServer; }

    @Override
    public Modpack getModpack(String build) throws BuildInaccessibleException {
        return new Modpack(this);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    public String getUrl() {
        return url;
    }

    public void setLocal() { isLocal = true; }
    @Override
    public boolean isLocal() { return isLocal; }

    @Override
    public String toString() {
        return "PlatformPackInfo{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", url='" + url + '\'' +
                ", icon=" + icon +
                ", logo=" + logo +
                ", background=" + background +
                ", gameVersion='" + minecraft + '\'' +
                ", forge='" + forge + '\'' +
                ", version='" + version + '\'' +
                ", solder='" + solder + '\'' +
                ", forceDir=" + forceDir +
                '}';
    }
}
