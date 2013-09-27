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

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.minecraft.CompleteVersion;
import net.technicpack.launchercore.minecraft.Library;
import net.technicpack.launchercore.minecraft.MojangConstants;
import net.technicpack.launchercore.restful.Modpack;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.solder.Mod;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.launchercore.util.DownloadUtils;
import net.technicpack.launchercore.util.MD5Utils;
import net.technicpack.launchercore.util.OperatingSystem;
import net.technicpack.launchercore.util.Utils;
import net.technicpack.launchercore.util.ZipUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class ModpackInstaller {
	private final DownloadListener listener;
	private final InstalledPack installedPack;
	private final String build;
	private boolean finished = false;

	public ModpackInstaller(DownloadListener listener, InstalledPack installedPack, String build) {
		this.listener = listener;
		this.installedPack = installedPack;
		this.build = build;
	}

	public void installPack() throws IOException {
		installedPack.getInstalledDirectory();
		PackInfo packInfo = installedPack.getInfo();
		Modpack modpack = packInfo.getModpack(build);
		String minecraft = modpack.getMinecraft();

		installModpack(modpack);
		installMinecraft(minecraft);
		finished = true;
		System.out.println("Finished installing pack!");
	}

	public boolean isFinished() {
		return finished;
	}

	private void installMinecraft(String version) throws IOException {
		CompleteVersion completeVersion = getMinecraftVersion(version);
		System.out.println(completeVersion);
		installCompleteVersion(completeVersion);

		String url = MojangConstants.getVersionDownload(version);
		String md5 = DownloadUtils.getETag(url);

		// Install the minecraft jar
		File cache = new File(Utils.getCacheDirectory(), "minecraft_" + version + ".jar");
		if (!cache.exists() || md5.isEmpty() || !MD5Utils.checkMD5(cache, md5)) {
			String output = installedPack.getCacheDir() + File.separator + "minecraft.jar";
			DownloadUtils.downloadFile(url, output, cache, md5, listener);
		}
		FileUtils.copyFile(cache, new File(installedPack.getBinDir(), "minecraft.jar"));


	}

	private void installCompleteVersion(CompleteVersion version) throws IOException {
		for (Library library : version.getLibrariesForOS()) {
			installLibrary(library);
		}
	}

	private void installLibrary(Library library) throws IOException {
		String natives = null;
		if (library.getNatives() != null) {
			natives = library.getNatives().get(OperatingSystem.getOperatingSystem());
		}
		String path = library.getArtifactPath(natives);
		String url = library.getDownloadUrl() + path;
		String md5 = DownloadUtils.getETag(url);

		File cache = new File(Utils.getCacheDirectory(), path);
		cache.mkdirs();

		if (!cache.exists() || md5.isEmpty() || !MD5Utils.checkMD5(cache, md5)) {
			DownloadUtils.downloadFile(url, cache.getAbsolutePath(), null, md5, listener);
		}

		if (natives != null && cache.exists()) {
			extractNatives(library, cache, natives);
		}
	}

	private void extractNatives(Library library, File cache, String natives) {
		File folder = new File(installedPack.getBinDir(), "natives");

	}

	private CompleteVersion getMinecraftVersion(String version) throws IOException {
		File versionFile = new File(installedPack.getBinDir(), "version.json");
		File modpackJar = new File(installedPack.getBinDir(), "modpack.jar");

		boolean extracted = ZipUtils.extractFile(modpackJar, versionFile, "version.json");
		if (!extracted && !versionFile.exists()) {
			String url = MojangConstants.getVersionJson(version);
			DownloadUtils.downloadFile(url, versionFile.getAbsolutePath(), null, null, listener);
		}

		if (!versionFile.exists()) {
			throw new IOException("Unable to find a valid version profile for minecraft " + version);
		}

		String json = FileUtils.readFileToString(versionFile, Charset.forName("UTF-8"));
		return Utils.getGson().fromJson(json, CompleteVersion.class);
	}

	private void installModpack(Modpack modpack) throws IOException {
		for (Mod mod : modpack.getMods()) {
			installMod(mod);
		}
	}

	private void installMod(Mod mod) throws IOException {
		String url = mod.getUrl();
		String md5 = mod.getMd5();
		String name = mod.getName() + "-" + build + ".zip";

		File cache = new File(installedPack.getCacheDir(), name);
		if (!cache.exists() || md5.isEmpty() || !MD5Utils.checkMD5(cache, md5)) {
			DownloadUtils.downloadFile(url, cache.getAbsolutePath(), null, md5, listener);
		}

		ZipUtils.unzipFile(cache, installedPack.getInstalledDirectory(), listener);
	}
}
