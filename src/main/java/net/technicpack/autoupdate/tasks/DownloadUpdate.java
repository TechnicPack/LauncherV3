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

package net.technicpack.autoupdate.tasks;

import net.technicpack.autoupdate.Relauncher;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.DownloadFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;

import java.io.IOException;
import java.util.Collection;

public class DownloadUpdate extends DownloadFileTask {
    private Relauncher relauncher;
    private Collection<IInstallTask> postUpdateActions;

    public DownloadUpdate(String url, Relauncher relauncher, Collection<IInstallTask> postUpdateActions) {
        super(url, relauncher.getTempLauncher(), null, relauncher.getUpdateText());

        this.relauncher = relauncher;
        this.postUpdateActions = postUpdateActions;
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException, InterruptedException {
        super.runTask(queue);

        if (!relauncher.isUpdateOnly() && getDestination().exists()) {
            for (IInstallTask task : postUpdateActions) {
                queue.addTask(task);
            }
        }
        relauncher.setUpdated();
    }
}
