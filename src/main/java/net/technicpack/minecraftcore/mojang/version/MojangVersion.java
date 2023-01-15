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

package net.technicpack.minecraftcore.mojang.version;

import net.technicpack.minecraftcore.mojang.version.io.*;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;

import java.util.Date;
import java.util.List;

public interface MojangVersion {

    String getId();

    ReleaseType getType();

    void setType(ReleaseType releaseType);

    Date getUpdatedTime();

    void setUpdatedTime(Date updatedTime);

    Date getReleaseTime();

    void setReleaseTime(Date releaseTime);

    ArgumentList getMinecraftArguments();

    ArgumentList getJavaArguments();

    List<Library> getLibraries();

    List<Library> getLibrariesForOS();

    String getMainClass();

    void setMainClass(String mainClass);

    int getMinimumLauncherVersion();

    String getIncompatibilityReason();

    List<Rule> getRules();

    String getAssetsKey();

    AssetIndex getAssetIndex();

    GameDownloads getDownloads();

    String getParentVersion();

    boolean getAreAssetsVirtual();

    void setAreAssetsVirtual(boolean areAssetsVirtual);

    boolean getAssetsMapToResources();

    void setAssetsMapToResources(boolean mapToResources);

    void addLibrary(Library library);

    JavaVersion getJavaVersion();

    void removeLibrary(String libraryName);
}
