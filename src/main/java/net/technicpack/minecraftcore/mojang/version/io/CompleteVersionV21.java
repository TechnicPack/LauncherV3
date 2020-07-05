/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.version.io;

import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"unused"})
public class CompleteVersionV21 implements MojangVersion {

	private String id;
	private Date time;
	private Date releaseTime;
	private ReleaseType type;
	private LaunchArguments arguments;
	private List<Library> libraries;
	private String mainClass;
	private int minimumLauncherVersion;
	private String incompatibilityReason;
	private List<Rule> rules;
	private String assets;
	private AssetIndex assetIndex;
	private GameDownloads downloads;
	private String inheritsFrom;
	private transient boolean areAssetsVirtual;
	private transient boolean mapToResources;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ReleaseType getType() {
		return type;
	}

	@Override
	public void setType(ReleaseType type) {
		this.type = type;
	}

	@Override
	public Date getUpdatedTime() {
		return time;
	}

	@Override
	public void setUpdatedTime(Date updatedTime) {
		this.time = updatedTime;
	}

	@Override
	public Date getReleaseTime() {
		return releaseTime;
	}

	@Override
	public void setReleaseTime(Date releaseTime) {
		this.releaseTime = releaseTime;
	}

	@Override
	public ArgumentList getMinecraftArguments() {
		return arguments.getGameArgs();
	}

	@Override
	public ArgumentList getJavaArguments() {
		return arguments.getJvmArgs();
	}

	@Override
	public List<Library> getLibraries() {
		return libraries;
	}

	@Override
	public List<Library> getLibrariesForOS() {
		List<Library> libraryList = new ArrayList<Library>(libraries.size());
		for (Library library : libraries) {
			if (library.isForCurrentOS()) {
				libraryList.add(library);
			}
		}
		return libraryList;
	}

	@Override
	public String getMainClass() {
		return mainClass;
	}

	@Override
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	@Override
	public int getMinimumLauncherVersion() {
		return minimumLauncherVersion;
	}

	@Override
	public String getIncompatibilityReason() {
		return incompatibilityReason;
	}

	@Override
	public List<Rule> getRules() {
		return rules;
	}

	@Override
	public String getAssetsKey() {
		return assets;
	}

	@Override
	public AssetIndex getAssetIndex() {
		return assetIndex;
	}

	@Override
	public GameDownloads getDownloads() {
		return downloads;
	}

	@Override
	public String getParentVersion() {
		return inheritsFrom;
	}

	@Override
	public boolean getAreAssetsVirtual() {
		return areAssetsVirtual;
	}

	@Override
	public void setAreAssetsVirtual(boolean areAssetsVirtual) {
		this.areAssetsVirtual = areAssetsVirtual;
	}

	@Override
	public boolean getAssetsMapToResources() {
		return mapToResources;
	}

	@Override
	public void setAssetsMapToResources(boolean mapToResources) {
		this.mapToResources = mapToResources;
	}

	@Override
	public void addLibrary(Library library) {
		libraries.add(library);
	}

	@Override
	public String toString() {
		return "CompleteVersionV21{" +
				"id='" + id + '\'' +
				", time=" + time +
				", releaseTime=" + releaseTime +
				", type=" + type +
				", arguments='" + arguments + '\'' +
				", libraries=" + libraries +
				", mainClass='" + mainClass + '\'' +
				", minimumLauncherVersion=" + minimumLauncherVersion +
				", incompatibilityReason='" + incompatibilityReason + '\'' +
				", rules=" + rules +
				'}';
	}

}
