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

package net.technicpack.autoupdate.io;

import net.technicpack.rest.RestObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused"})
public class StreamVersion extends RestObject {
    private int build;
    private StreamUrls url;
    private List<LauncherResource> resources = new ArrayList<>();

    public int getBuild() {
        return build;
    }

    public String getExeUrl() {
        return url.getExeUrl();
    }

    public String getJarUrl() {
        return url.getJarUrl();
    }

    public List<LauncherResource> getResources() { return resources; }
}
