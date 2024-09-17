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

package net.technicpack.minecraftcore.mojang.version.chain;

import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.*;
import net.technicpack.minecraftcore.mojang.version.io.argument.Argument;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class VersionChain implements MojangVersion {

    private List<MojangVersion> chain;

    public VersionChain(MojangVersion rootVersion) {
        chain = new LinkedList<>();
        chain.add(rootVersion);
    }

    @Override
    public String getId() {
        return chain.get(0).getId();
    }

    @Override
    public ReleaseType getType() {
        return chain.get(0).getType();
    }

    @Override
    public void setType(ReleaseType releaseType) {
        chain.get(0).setType(releaseType);
    }

    @Override
    public Date getUpdatedTime() {
        return chain.get(0).getUpdatedTime();
    }

    @Override
    public void setUpdatedTime(Date updatedTime) {
        chain.get(0).setUpdatedTime(updatedTime);
    }

    @Override
    public Date getReleaseTime() {
        return chain.get(0).getReleaseTime();
    }

    @Override
    public void setReleaseTime(Date releaseTime) {
        chain.get(0).setReleaseTime(releaseTime);
    }

    @Override
    public ArgumentList getMinecraftArguments() {
        ArgumentList.Builder allArguments = new ArgumentList.Builder();

        for (MojangVersion version : chain) {
            if (version.getMinecraftArguments() != null) {
                for (Argument arg : version.getMinecraftArguments().getArguments()) {
                    allArguments.addArgument(arg);
                }
            }
        }

        return allArguments.build();
    }

    @Override
    public ArgumentList getJavaArguments() {
        ArgumentList.Builder allArguments = new ArgumentList.Builder();

        for (MojangVersion version : chain) {
            if (version.getJavaArguments() != null) {
                for (Argument arg : version.getJavaArguments().getArguments()) {
                    allArguments.addArgument(arg);
                }
            }
        }

        return allArguments.build();
    }

    @Override
    public List<Library> getLibraries() {
        List<Library> allLibraries = new LinkedList<>();

        for (MojangVersion version : chain) {
            if (version.getLibraries() != null)
                allLibraries.addAll(0, version.getLibraries());
        }

        return allLibraries;
    }

    @Override
    public List<Library> getLibrariesForOS() {
        List<Library> allLibraries = new LinkedList<>();

        for (int i = chain.size() - 1; i >= 0; i--) {
            MojangVersion version = chain.get(i);
            if (version.getLibrariesForOS() != null)
                allLibraries.addAll(0, version.getLibrariesForOS());
        }

        return allLibraries.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public String getMainClass() {
        for (MojangVersion version : chain) {
            if (version.getMainClass() != null)
                return version.getMainClass();
        }

        return null;
    }

    @Override
    public void setMainClass(String mainClass) {
        chain.get(0).setMainClass(mainClass);
    }

    @Override
    public int getMinimumLauncherVersion() {
        return chain.get(0).getMinimumLauncherVersion();
    }

    @Override
    public String getIncompatibilityReason() {
        for (MojangVersion version : chain) {
            if (version.getIncompatibilityReason() != null)
                return version.getIncompatibilityReason();
        }

        return null;
    }

    @Override
    public List<Rule> getRules() {
        List<Rule> allRules = new LinkedList<>();

        for (MojangVersion version : chain) {
            if (version.getRules() != null)
                allRules.addAll(0, version.getRules());
        }

        return allRules;
    }

    @Override
    public AssetIndex getAssetIndex() {
        for (MojangVersion version : chain) {
            if (version.getAssetIndex() != null)
                return version.getAssetIndex();
        }

        return null;
    }

    @Override
    public String getAssetsKey() {
        for (MojangVersion version : chain) {
            if (version.getAssetsKey() != null)
                return version.getAssetsKey();
        }

        return null;
    }

    @Override
    public GameDownloads getDownloads() {
        for (MojangVersion version : chain) {
            if (version.getDownloads() != null)
                return version.getDownloads();
        }

        return null;
    }

    @Override
    public boolean getAreAssetsVirtual() {
        return chain.get(0).getAreAssetsVirtual();
    }

    @Override
    public void setAreAssetsVirtual(boolean areAssetsVirtual) {
        chain.get(0).setAreAssetsVirtual(areAssetsVirtual);
    }

    @Override
    public boolean getAssetsMapToResources() {
        return chain.get(0).getAssetsMapToResources();
    }

    @Override
    public void setAssetsMapToResources(boolean mapToResources) {
        chain.get(0).setAssetsMapToResources(mapToResources);
    }

    @Override
    public String getParentVersion() {
        return chain.get(chain.size() - 1).getId();
    }

    @Override
    public void addLibrary(Library library) {
        chain.get(0).addLibrary(library);
    }

    @Override
    public void prependLibrary(Library library) {
        chain.get(0).prependLibrary(library);
    }

    @Override
    public JavaVersion getJavaVersion() {
        for (MojangVersion version : chain) {
            if (version.getJavaVersion() != null)
                return version.getJavaVersion();
        }

        return null;
    }

    @Override
    public void removeLibrary(String libraryName) {
        for (MojangVersion version : chain) {
            version.removeLibrary(libraryName);
        }
    }

    public void addVersionToChain(MojangVersion version) {
        chain.add(version);
    }
}
