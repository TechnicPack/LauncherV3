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

package net.technicpack.launchercore.restful;


import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.install.user.User;

import java.util.List;

public interface PackInfo {

    public String getName();

    public String getDisplayName();

    public String getUrl();

    public Resource getIcon();

    public Resource getBackground();

    public Resource getLogo();

    public String getRecommended();

    public String getLatest();

    public List<String> getBuilds();

    public boolean shouldForceDirectory();

    public Modpack getModpack(String build, User user) throws BuildInaccessibleException;
}
