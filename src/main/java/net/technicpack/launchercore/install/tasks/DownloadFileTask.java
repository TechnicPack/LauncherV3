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

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.utilslib.Utils;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamProvider;

import java.io.*;
import java.nio.file.Files;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedReturnValue")
public class DownloadFileTask<T> extends ListenerTask<T> {
    private static final Set<String> AVAILABLE_DECOMPRESSORS;

    static {
        SortedMap<String, CompressorStreamProvider> availableProviders =
                CompressorStreamFactory.findAvailableCompressorInputStreamProviders();

        AVAILABLE_DECOMPRESSORS = availableProviders.keySet()
                                                    .stream()
                                                    .map(String::toLowerCase)
                                                    .collect(Collectors.toSet());
    }

    private final String url;
    private final File destination;
    private final String taskDescription;
    private final IFileVerifier fileVerifier;
    private boolean executable;
    private String decompressor;

    public DownloadFileTask(String url, File destination, IFileVerifier verifier) {
        this(url, destination, verifier, destination.getName());
    }

    public DownloadFileTask(String url, File destination, IFileVerifier verifier, String taskDescription) {
        this.url = url;
        this.destination = destination;
        this.fileVerifier = verifier;
        this.taskDescription = taskDescription;
    }

    protected File getDestination() {
        return destination;
    }

    /**
     * @param decompressor See {@link CompressorStreamFactory#createCompressorInputStream(String, InputStream)}
     * @see CompressorStreamFactory#createCompressorInputStream(String, InputStream)
     */
    public void setDecompressor(String decompressor) throws DownloadException {
        if (!AVAILABLE_DECOMPRESSORS.contains(decompressor.toLowerCase())) {
            throw new DownloadException(String.format("Decompressor '%s' is not available", decompressor));
        }

        this.decompressor = decompressor;
    }

    @Override
    public String getTaskDescription() {
        return taskDescription;
    }

    @Override
    public void runTask(InstallTasksQueue<T> queue) throws IOException, InterruptedException {
        super.runTask(queue);

        final boolean needsDecompression = decompressor != null;

        IFileVerifier downloadFileVerifier;
        File tempDestination;

        if (needsDecompression) {
            downloadFileVerifier = null;
            tempDestination = new File(this.destination + ".temp");
        } else {
            downloadFileVerifier = fileVerifier;
            tempDestination = this.destination;
        }

        Utils.downloadFile(url, tempDestination.getName(), tempDestination.getAbsolutePath(), null,
                           downloadFileVerifier, this);

        if (needsDecompression) {
            decompress(tempDestination);
        }

        if (!this.destination.exists()) {
            throw new DownloadException("Failed to download " + this.destination.getName() + ".");
        }

        if (this.executable && !this.destination.setExecutable(true)) {
            throw new DownloadException("Failed to set " + this.destination.getName() + " as executable");
        }
    }

    private void decompress(File tempDestination) throws IOException {
        Utils.getLogger().fine("Decompressing " + tempDestination.getAbsolutePath() + " using " + decompressor);

        try (FileInputStream fis = new FileInputStream(tempDestination);
             CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(decompressor, fis);
             FileOutputStream fos = new FileOutputStream(this.destination)) {
            byte[] buffer = new byte[65536];
            int n;
            while ((n = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, n);
            }
        } catch (CompressorException e) {
            throw new DownloadException("Failed to decompress " + tempDestination.getName(), e);
        }

        if (this.fileVerifier.isFileValid(this.destination)) {
            try {
                Files.delete(tempDestination.toPath());
            } catch (IOException e) {
                throw new DownloadException("Failed to delete temporary file " + tempDestination.getAbsolutePath(), e);
            }
        } else {
            try {
                Files.delete(this.destination.toPath());
            } catch (IOException e) {
                throw new DownloadException("Failed to delete broken downloaded file " + this.destination.getAbsolutePath(), e);
            }
            throw new DownloadException("Failed to download " + this.destination.getAbsolutePath());
        }
    }

    public DownloadFileTask<T> withExecutable() {
        this.executable = true;
        return this;
    }
}
