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

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.utilslib.MD5Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CopyDylibJnilibTask implements IInstallTask {
    private ModpackModel modpack;

    public CopyDylibJnilibTask(ModpackModel modpack) {
        this.modpack = modpack;
    }

    @Override
    public String getTaskDescription() {
        return "Copying OSX natives";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        File dylib = new File(new File(modpack.getBinDir(), "natives"), "liblwjgl.dylib");
        File jnilib = new File(new File(modpack.getBinDir(), "natives"), "liblwjgl.jnilib");

        File betterLib = getBetterLib(dylib, jnilib);

        if (!betterLib.exists())
            return;

        File worseLib = dylib;

        if (worseLib.equals(betterLib))
            worseLib = jnilib;

        if (betterLib.exists() && worseLib.exists()) {
            String betterMd5 = MD5Utils.getMD5(betterLib);

            if (MD5Utils.checkMD5(worseLib, betterMd5))
                return;
        }
        FileUtils.copyFile(betterLib, worseLib);
    }

    private File getBetterLib(File file1, File file2) {
        boolean file1Exists = file1.exists();
        boolean file2Exists = file2.exists();
        if (!file1Exists && !file2Exists)
            return file1;

        if (file1Exists && !file2Exists)
            return file1;

        if (!file1Exists && file2Exists)
            return file2;

        if (file1.lastModified() > file2.lastModified())
            return file1;
        else
            return file2;
    }
}
