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

import net.technicpack.autoupdate.IUpdateStream;
import net.technicpack.autoupdate.Relauncher;
import net.technicpack.autoupdate.io.LauncherResource;
import net.technicpack.autoupdate.io.StreamVersion;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.ITasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.tasks.DownloadFileTask;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA256FileVerifier;
import net.technicpack.rest.RestfulAPIException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class QueryUpdateStream implements IInstallTask<Void> {

    private String description;
    private ITasksQueue<Void> downloadTasks;
    private IUpdateStream updateStream;
    private LauncherDirectories directories;
    private Relauncher relauncher;
    private Collection<IInstallTask<Void>> postDownloadTasks;

    public QueryUpdateStream(String description, IUpdateStream stream, ITasksQueue<Void> downloadTasks, LauncherDirectories directories, Relauncher relauncher, Collection<IInstallTask<Void>> postDownloadTasks) {
        this.description = description;
        this.downloadTasks = downloadTasks;
        this.updateStream = stream;
        this.directories = directories;
        this.relauncher = relauncher;
        this.postDownloadTasks = postDownloadTasks;
    }

    @Override
    public String getTaskDescription() {
        return description;
    }

    @Override
    public float getTaskProgress() {
        return 0;
    }

    @Override
    public void runTask(InstallTasksQueue<Void> queue) throws IOException {
        try {
            StreamVersion version = updateStream.getStreamVersion(relauncher.getStreamName());

            if (version == null || version.getBuild() == 0)
                return;

            for(LauncherResource resource : version.getResources()) {
                IFileVerifier verifier;

                if (resource.getSha256() != null && !resource.getSha256().isEmpty()) {
                    verifier = new SHA256FileVerifier(resource.getSha256());
                } else if (resource.getMd5() != null && !resource.getMd5().isEmpty()) {
                    verifier = new MD5FileVerifier(resource.getMd5());
                } else {
                    verifier = null;
                }

                File targetFile = new File(new File(directories.getAssetsDirectory(), "launcher"), resource.getFilename());

                if (targetFile.exists() && verifier.isFileValid(targetFile)) {
                    continue;
                }

                DownloadFileTask<Void> downloadFileTask;

                String zstdUrl = resource.getZstdUrl();
                if (zstdUrl != null && !zstdUrl.isEmpty()) {
                    downloadFileTask = new DownloadFileTask<>(zstdUrl, targetFile, verifier, resource.getFilename());
                    downloadFileTask.setDecompressor(CompressorStreamFactory.ZSTANDARD);
                } else {
                    downloadFileTask = new DownloadFileTask<>(resource.getUrl(), targetFile, verifier, resource.getFilename());
                }

                downloadTasks.addTask(downloadFileTask);
            }

            if (relauncher.isSkipUpdate() || version.getBuild() == relauncher.getCurrentBuild()) {
                return;
            }

            String updateUrl;
            String runningPath = relauncher.getRunningPath();

            if (runningPath == null) {
                throw new DownloadException("Could not load a running path for currently-executing launcher.");
            }

            if (runningPath.endsWith(".exe"))
                updateUrl = version.getExeUrl();
            else
                updateUrl = version.getJarUrl();

            downloadTasks.addTask(new DownloadUpdate(updateUrl, relauncher, postDownloadTasks));
        } catch (RestfulAPIException ex) {
        }
    }
}
