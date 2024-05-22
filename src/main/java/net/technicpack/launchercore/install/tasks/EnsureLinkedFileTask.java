/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
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

package net.technicpack.launchercore.install.tasks;

import net.technicpack.launchercore.install.InstallTasksQueue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EnsureLinkedFileTask implements IInstallTask {
    private Path link;
    private Path target;

    /**
     * Ensures that a symbolic link at "link", pointing to "target" exists and is correct.
     * If it doesn't exist, it will be created.
     * @param link Where the symbolic link is
     * @param target Where the symbolic link points to
     */
    public EnsureLinkedFileTask(File link, File target) {
        this.link = link.toPath();
        this.target = target.toPath();
    }

    @Override
    public String getTaskDescription() {
        return "Linking files.";
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException {
        if (Files.isSymbolicLink(link) && Files.readSymbolicLink(link).equals(target)) {
            // link is symlink and points to the right place
            return;
        }

        Files.deleteIfExists(link);
        Files.createSymbolicLink(link, target);
    }
}
