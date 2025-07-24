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

package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.MinecraftVersionInfoBuilder;

import java.io.IOException;

public class VerifyVersionFilePresentTask implements IInstallTask<IMinecraftVersionInfo> {
	private MinecraftVersionInfoBuilder builder;
    private String minecraftVersion;
    private ModpackModel modpack;

	public VerifyVersionFilePresentTask(ModpackModel modpack, String minecraftVersion, MinecraftVersionInfoBuilder builder) {
		this.builder = builder;
        this.minecraftVersion = minecraftVersion;
        this.modpack = modpack;
	}

	@Override
	public String getTaskDescription() {
		return "Retrieving Modpack Version";
	}

	@Override
	public float getTaskProgress() {
		return 0;
	}

	@Override
	public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) throws IOException, InterruptedException {
        IMinecraftVersionInfo version = builder.buildVersionFromKey(minecraftVersion);

        if (version == null && modpack.isLocalOnly())
            throw new PackNotAvailableOfflineException(modpack.getDisplayName());
	}
}
