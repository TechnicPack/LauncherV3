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

package net.technicpack.launchercore.restful.solder;

import net.technicpack.launchercore.util.Settings;

public class SolderConstants {
    public static final String TECHNIC = "http://solder.technicpack.net/api/";

    public static String getSolderPackInfoUrl(String solder, String modpack, String profileName) {
        return solder + "modpack/" + modpack + "/?cid=" + Settings.getClientId() + "&u=" + profileName;
    }

    public static String getSolderPackInfoUrlWithoutCid(String solder, String modpack) {
        return solder + "modpack/" + modpack + "/";
    }

    public static String getSolderBuildUrl(String solder, String modpack, String build, String profileName) {
        return getSolderPackInfoUrlWithoutCid(solder, modpack) + build + "/?cid=" + Settings.getClientId() + "&u=" + profileName;
    }

    public static String getFullSolderUrl(String solder, String profileName) {
        return solder + "modpack/?include=full&cid=" + Settings.getClientId() + "&u=" + profileName;
    }
}
