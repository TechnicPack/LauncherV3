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

import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.mojang.version.io.*;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;

import java.util.List;

public interface IMinecraftVersionInfo {
    String getId();

    ReleaseType getType();

    ArgumentList getMinecraftArguments();

    ArgumentList getJavaArguments();

    List<Library> getLibraries();

    List<Library> getLibrariesForCurrentOS(ILaunchOptions options, IJavaRuntime runtime);

    String getMainClass();

    void setMainClass(String mainClass);

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

    void prependLibrary(Library library);

    /**
     * Get information about the Mojang JRE
     * @return The Mojang JRE information.<br>If there isn't one associated with the version.json manifest file, null
     * will be returned.
     */
    VersionJavaInfo getMojangRuntimeInformation();

    void removeLibrary(String libraryName);

    IJavaRuntime getJavaRuntime();

    void setJavaRuntime(IJavaRuntime runtime);
}
