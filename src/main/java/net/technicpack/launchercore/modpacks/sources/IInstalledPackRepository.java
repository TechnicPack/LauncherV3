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

package net.technicpack.launchercore.modpacks.sources;

import net.technicpack.launchercore.modpacks.InstalledPack;

import java.util.List;
import java.util.Map;

public interface IInstalledPackRepository {
    Map<String, InstalledPack> getInstalledPacks();

    List<String> getPackNames();

    String getSelectedSlug();

    void setSelectedSlug(String slug);

    InstalledPack put(InstalledPack installedPack);

    InstalledPack remove(String name);

    void save();
}
