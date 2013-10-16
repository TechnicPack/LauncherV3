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

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModpackInstaller {
	private final DownloadListener listener;
	private final InstalledPack installedPack;
	private String build;
	private boolean finished = false;

	public ModpackInstaller(DownloadListener listener, InstalledPack installedPack, String build) {
		this.listener = listener;
		this.installedPack = installedPack;
		this.build = build;
	}

	public CompleteVersion installPack(Component component) throws IOException {
		installedPack.getInstalledDirectory();
		installedPack.initDirectories();
		PackInfo packInfo = installedPack.getInfo();
		Modpack modpack = packInfo.getModpack(build);
		String minecraft = modpack.getMinecraft();

		installOldForgeLibs(minecraft);

		Version installedVersion = getInstalledVersion();
		boolean shouldUpdate = installedVersion == null;
		if (!shouldUpdate && !build.equals(installedVersion.getVersion())) {
			int result = JOptionPane.showConfirmDialog(component, "Would you like to update this pack?", "Update Found", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

			if (result == JOptionPane.YES_OPTION) {
				shouldUpdate = true;
			} else {
				build = installedVersion.getVersion();
			}
		}

		if (shouldUpdate) {
			installModpack(modpack);
		}

		CompleteVersion version = getMinecraftVersion(minecraft);

		boolean isLegacy = installedVersion != null && installedVersion.isLegacy();
		if (shouldUpdate || isLegacy) {
			installMinecraft(version, minecraft);
		}

		Version versionFile = new Version(build, false);
		versionFile.save(installedPack.getBinDir());

		finished = true;
		return version;
	}

	private void installOldForgeLibs(String minecraft) throws IOException {
		if (minecraft.startsWith("1.5")) {
			installOldForgeLib("fml_libs15.zip");
		} else if (minecraft.startsWith("1.4")) {
			installOldForgeLib("fml_libs.zip");
		}
	}

	private void installOldForgeLib(String lib) throws IOException {
		File cache = new File(Utils.getCacheDirectory(), lib);
		if (!cache.exists()) {
			DownloadUtils.downloadFile("http://mirror.technicpack.net/Technic/lib/fml/" + lib, cache.getName(), cache.getAbsolutePath(), null, null, listener);
		}
		ZipUtils.unzipFile(cache, new File(installedPack.getInstalledDirectory(), "lib"), listener);
	}

	private void installModpack(Modpack modpack) throws IOException {
		for (Mod mod : modpack.getMods()) {
			installMod(mod);
		}
		cleanupCache(modpack.getMods());
	}

	private void installMod(Mod mod) throws IOException {
		String url = mod.getUrl();
		String md5 = mod.getMd5();
		String name = mod.getName() + "-" + mod.getVersion() + ".zip";

		File cache = new File(installedPack.getCacheDir(), name);
		if (!cache.exists() || md5.isEmpty() || !MD5Utils.checkMD5(cache, md5)) {
			DownloadUtils.downloadFile(url, cache.getName(), cache.getAbsolutePath(), null, md5, listener);
		}

		ZipUtils.unzipFile(cache, installedPack.getInstalledDirectory(), listener);
	}

	private void installMinecraft(CompleteVersion completeVersion, String version) throws IOException {
		System.out.println(completeVersion);
		installCompleteVersion(completeVersion);

		String url = MojangConstants.getVersionDownload(version);
		String md5 = DownloadUtils.getETag(url);

//		// Install the minecraft jar
		File cache = new File(Utils.getCacheDirectory(), "minecraft_" + version + ".jar");
		if (!cache.exists() || md5.isEmpty() || !MD5Utils.checkMD5(cache, md5)) {
			String output = installedPack.getCacheDir() + File.separator + "minecraft.jar";
			DownloadUtils.downloadFile(url, cache.getName(), output, cache, md5, listener);
		}
		ZipUtils.copyMinecraftJar(cache, new File(installedPack.getBinDir(), "minecraft.jar"));
	}

	private CompleteVersion getMinecraftVersion(String version) throws IOException {
		File versionFile = new File(installedPack.getBinDir(), "version.json");
		File modpackJar = new File(installedPack.getBinDir(), "modpack.jar");

		boolean extracted = ZipUtils.extractFile(modpackJar, installedPack.getBinDir(), "version.json");
		if (!extracted && !versionFile.exists()) {
			String url = MojangConstants.getVersionJson(version);
			DownloadUtils.downloadFile(url, versionFile.getName(), versionFile.getAbsolutePath(), null, null, listener);
		}

		if (!versionFile.exists()) {
			throw new IOException("Unable to find a valid version profile for minecraft " + version);
		}

		String json = FileUtils.readFileToString(versionFile, Charset.forName("UTF-8"));
		return Utils.getMojangGson().fromJson(json, CompleteVersion.class);
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
		String url = library.getDownloadUrl(path);
		String md5 = DownloadUtils.getETag(url);

		File cache = new File(Utils.getCacheDirectory(), path);
		if (cache.getParentFile() != null) {
			cache.getParentFile().mkdirs();
		}

		if (!cache.exists() || md5.isEmpty() || !MD5Utils.checkMD5(cache, md5)) {
			DownloadUtils.downloadFile(url, cache.getName(), cache.getAbsolutePath(), null, md5, listener);
		}

		if (natives != null && cache.exists()) {
			File folder = new File(installedPack.getBinDir(), "natives");
			ZipUtils.unzipFile(cache, folder, library.getExtract(), listener);
		}
	}

	private void cleanupCache(List<Mod> mods) {
		File[] files = installedPack.getCacheDir().listFiles();

		if (files == null) {
			return;
		}

		Set<String> keepFiles = new HashSet<String>(mods.size() + 1);
		for (Mod mod : mods) {
			keepFiles.add(mod.getName() + "-" + mod.getVersion() + ".zip");
		}
		keepFiles.add("minecraft.jar");

		for (File file : files) {
			String fileName = file.getName();
			if (keepFiles.contains(fileName)) {
				continue;
			}
			FileUtils.deleteQuietly(file);
		}
	}

	private Version getInstalledVersion() {
		Version version = null;
		File versionFile = new File(installedPack.getBinDir(), "version");
		if (versionFile.exists()) {
			version = Version.load(versionFile);
		}
		return version;
	}

	public boolean isFinished() {
		return finished;
	}
}
