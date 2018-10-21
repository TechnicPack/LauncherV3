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

    public String getId();

    public ReleaseType getType();

    public void setType(ReleaseType releaseType);

    public Date getUpdatedTime();

    public void setUpdatedTime(Date updatedTime);

    public Date getReleaseTime();

    public void setReleaseTime(Date releaseTime);

    public ArgumentList getMinecraftArguments();

    public ArgumentList getJavaArguments();

    public List<Library> getLibraries();

    public List<Library> getLibrariesForOS();

    public String getMainClass();

    public void setMainClass(String mainClass);

    public int getMinimumLauncherVersion();

    public String getIncompatibilityReason();

    public List<Rule> getRules();

    String getAssetsKey();

    AssetIndex getAssetIndex();

    GameDownloads getDownloads();

    public String getJarKey();

    public String getParentVersion();

    public boolean getAreAssetsVirtual();

    public void setAreAssetsVirtual(boolean areAssetsVirtual);

    public void addLibrary(Library library);
}
