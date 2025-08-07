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

import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("java:S2065")
public class MinecraftVersionInfo implements IMinecraftVersionInfo {
    private final String id;
    private final ReleaseType type;
    private final LaunchArguments arguments;
    private final List<Library> libraries;
    private final List<Rule> rules;
    private final String assets;
    private final AssetIndex assetIndex;
    private final GameDownloads downloads;
    private final String inheritsFrom;
    private final VersionJavaInfo javaVersion;
    private String mainClass;

    private transient boolean areAssetsVirtual;
    private transient boolean mapToResources;
    private transient IJavaRuntime javaRuntime;

    MinecraftVersionInfo(MinecraftVersionInfoRaw raw) {
        id = raw.id;
        type = raw.type;
        if (raw.arguments != null) {
            arguments = raw.arguments;
        } else if (raw.minecraftArguments != null && !raw.minecraftArguments.isEmpty()) {
            arguments = LaunchArguments.fromLegacyString(raw.minecraftArguments);
        } else {
            throw new IllegalArgumentException("No arguments found");
        }

        libraries = raw.libraries;
        mainClass = raw.mainClass;
        rules = raw.rules;
        assets = raw.assets;
        assetIndex = raw.assetIndex;
        downloads = raw.downloads;
        inheritsFrom = raw.inheritsFrom;
        javaVersion = raw.javaVersion;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ReleaseType getType() {
        return type;
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
    public List<Library> getLibrariesForCurrentOS(ILaunchOptions options, IJavaRuntime runtime) {
        return libraries.stream().filter(x -> x.isForCurrentOS(options, runtime)).distinct().collect(Collectors.toList());
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
    public void prependLibrary(Library library) {
        libraries.add(0, library);
    }

    @Override
    public VersionJavaInfo getMojangRuntimeInformation() {
        return javaVersion;
    }

    @Override
    public void removeLibrary(String libraryName) {
        libraries.removeIf(library -> library.getName().equals(libraryName));
    }

    @Override
    public IJavaRuntime getJavaRuntime() {
        return javaRuntime;
    }

    @Override
    public void setJavaRuntime(IJavaRuntime runtime) {
        this.javaRuntime = runtime;
    }

    @Override
    public String toString() {
        return "MinecraftVersionInfo{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", arguments=" + arguments +
                ", libraries=" + libraries +
                ", mainClass='" + mainClass + '\'' +
                ", rules=" + rules +
                ", assets='" + assets + '\'' +
                ", assetIndex=" + assetIndex +
                ", downloads=" + downloads +
                ", inheritsFrom='" + inheritsFrom + '\'' +
                ", javaVersion=" + javaVersion +
                ", areAssetsVirtual=" + areAssetsVirtual +
                ", mapToResources=" + mapToResources +
                ", javaRuntime=" + javaRuntime +
                '}';
    }
}
