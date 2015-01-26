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

package net.technicpack.platform.packsources;

import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.platform.io.SearchResult;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchResultPackInfo implements PackInfo {
    private SearchResult result;

    public SearchResultPackInfo(SearchResult result) {
        this.result = result;
    }

    @Override
    public String getName() {
        return result.getSlug();
    }

    @Override
    public String getDisplayName() {
        return result.getDisplayName();
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
        return null;
    }

    @Override
    public String getLatest() {
        return null;
    }

    @Override
    public List<String> getBuilds() {
        return Collections.emptyList();
    }

    @Override
    public boolean shouldForceDirectory() {
        return false;
    }

    @Override
    public ArrayList<FeedItem> getFeed() {
        return new ArrayList<FeedItem>();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Integer getRuns() {
        return null;
    }

    @Override
    public Integer getDownloads() {
        return null;
    }

    @Override
    public Integer getLikes() {
        return null;
    }

    @Override
    public boolean isServerPack() { return false; }

    @Override
    public Modpack getModpack(String build) throws BuildInaccessibleException {
        throw new BuildInaccessibleException(getDisplayName(), build);
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
