/*
 * This file is part of Technic Launcher.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.logging.Logger;

public class Utils {
	private static final Gson gson;
	private static final Logger logger = Logger.getLogger("net.technicpack.launcher.Main");

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
		builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
		builder.enableComplexMapKeySerialization();
		gson = builder.create();
	}

	public static Gson getGson() {
		return gson;
	}

	public static File getLauncherDirectory() {
		return Directories.instance.getLauncherDirectory();
	}

	public static File getSettingsDirectory() {
		return Directories.instance.getSettingsDirectory();
	}

	public static File getCacheDirectory() {
		return Directories.instance.getCacheDirectory();
	}

	public static File getAssetsDirectory() {
		return Directories.instance.getAssetsDirectory();
	}

	public static File getModpacksDirectory() {
		return Directories.instance.getModpacksDirectory();
	}

	public static Logger getLogger() {
		return logger;
	}

	public static boolean pingURL(String urlLoc) {
		InputStream stream = null;
		try {
			final URL url = new URL(urlLoc);
			final URLConnection conn = url.openConnection();
			conn.setConnectTimeout(10000);
			stream = conn.getInputStream();
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}
}
