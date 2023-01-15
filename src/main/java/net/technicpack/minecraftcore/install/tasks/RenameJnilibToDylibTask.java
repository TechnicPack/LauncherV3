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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class RenameJnilibToDylibTask implements IInstallTask {
    private ModpackModel modpack;

    public RenameJnilibToDylibTask(ModpackModel modpack) {
        this.modpack = modpack;
    }

    @Override
    public String getTaskDescription() {
        return "Fixing OSX natives";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        // Rename all *.jnilib natives to *.dylib
        // This is required due to https://bugs.openjdk.org/browse/JDK-8127215

        final File nativesDir = new File(modpack.getBinDir(), "natives");

        // Some versions don't have natives. Drop out immediately if that's the case
        if (!nativesDir.exists()) {
            return;
        }

        Iterator<File> filesIterator = FileUtils.iterateFiles(nativesDir, new String[]{"jnilib"}, true);
        while (filesIterator.hasNext()) {
            File file = filesIterator.next();

            if (!file.getName().endsWith(".jnilib")) {
                continue;
            }

            final String filename = file.getName();
            final String newFilename = filename.substring(0, filename.length() - ".jnilib".length()) + ".dylib";

            File newFile = new File(file.getParentFile(), newFilename);

            if (!file.renameTo(newFile)) {
                throw new IOException("Failed to rename " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
            }
        }
    }
}
