/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CombinedPackInfo implements PackInfo {
    List<PackInfo> componentInfos = new LinkedList<PackInfo>();

    public CombinedPackInfo(PackInfo... infos) {
        for(PackInfo component : infos) {
            componentInfos.add(component);
        }
    }

    @Override
    public String getName() {
        for (PackInfo component : componentInfos) {
            if (component.getName() != null)
                return component.getName();
        }

        return null;
    }

    @Override
    public String getDisplayName() {
        for (PackInfo component : componentInfos) {
            if (component.getDisplayName() != null)
                return component.getDisplayName();
        }

        return null;
    }

    @Override
    public String getUrl() {
        for (PackInfo component : componentInfos) {
            if (component.getUrl() != null)
                return component.getUrl();
        }

        return null;
    }

    @Override
    public Resource getIcon() {
        for (PackInfo component : componentInfos) {
            if (component.getIcon() != null)
                return component.getIcon();
        }

        return null;
    }

    @Override
    public Resource getBackground() {
        for (PackInfo component : componentInfos) {
            if (component.getBackground() != null)
                return component.getBackground();
        }

        return null;
    }

    @Override
    public Resource getLogo() {
        for (PackInfo component : componentInfos) {
            if (component.getLogo() != null)
                return component.getLogo();
        }

        return null;
    }

    @Override
    public String getRecommended() {
        for (PackInfo component : componentInfos) {
            if (component.getRecommended() != null)
                return component.getRecommended();
        }

        return null;
    }

    @Override
    public String getLatest() {
        for (PackInfo component : componentInfos) {
            if (component.getLatest() != null)
                return component.getLatest();
        }

        return null;
    }

    @Override
    public List<String> getBuilds() {
        List<String> builds = new LinkedList<String>();

        for (PackInfo component : componentInfos) {
            List<String> subBuilds = component.getBuilds();
            if (subBuilds != null) {
                for(String build : subBuilds) {
                    if (!builds.contains(build))
                        builds.add(build);
                }
            }
        }

        return builds;
    }

    @Override
    public boolean shouldForceDirectory() {
        for (PackInfo component : componentInfos) {
            if (component.shouldForceDirectory())
                return true;
        }

        return false;
    }

    @Override
    public ArrayList<FeedItem> getFeed() {
        for (PackInfo component : componentInfos) {
            if (component.getFeed() != null)
                return component.getFeed();
        }

        return null;
    }

    @Override
    public String getDescription() {
        for (PackInfo component : componentInfos) {
            if (component.getDescription() != null)
                return component.getDescription();
        }

        return null;
    }

    @Override
    public Integer getRuns() {
        for (PackInfo component : componentInfos) {
            if (component.getRuns() != null)
                return component.getRuns();
        }

        return null;
    }

    @Override
    public Integer getDownloads() {
        for (PackInfo component : componentInfos) {
            if (component.getDownloads() != null)
                return component.getDownloads();
        }

        return null;
    }

    @Override
    public Integer getLikes() {
        for (PackInfo component : componentInfos) {
            if (component.getLikes() != null)
                return component.getLikes();
        }

        return null;
    }

    @Override
    public Modpack getModpack(String build) throws BuildInaccessibleException {
        for (PackInfo component : componentInfos) {
            Modpack modpack = component.getModpack(build);

            if (modpack != null)
                return modpack;
        }

        return null;
    }

    @Override
    public boolean isComplete() {
        for (PackInfo component : componentInfos) {
            if (component.isComplete())
                return true;
        }

        return false;
    }
}
