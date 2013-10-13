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

package net.technicpack.launchercore.minecraft;

public class MojangConstants {
	public static final String baseURL = "https://s3.amazonaws.com/Minecraft.Download/";
	public static final String versions = baseURL + "versions/";
        public static final String assets = "https://s3.amazonaws.com/MinecraftResources/";

	public static final String versionList = versions + "versions.json";

	public static String getVersionJson(String version) {
		return versions + version + "/" + version + ".json";
	}

	public static String getVersionDownload(String version) {
		return versions + version + "/" + version + ".jar";
	}
}
