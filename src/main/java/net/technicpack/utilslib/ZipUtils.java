/*
 * This file is part of Technic Launcher.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.utilslib;


import net.technicpack.launchercore.util.DownloadListener;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.zip.ZipException;

public class ZipUtils {
    public static boolean extractFile(File zip, File output, String fileName) throws IOException, InterruptedException {
        if (!zip.exists() || fileName == null) {
            return false;
        }

        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(zip);
            ZipArchiveEntry entry = zipFile.getEntry(fileName);
            if (entry == null) {
                Utils.getLogger().log(Level.WARNING, "File " + fileName + " not found in " + zip.getAbsolutePath());
                return false;
            }
            File outputFile = new File(output, entry.getName());

            // Zip Slip check
            if (!outputFile.toPath().normalize().startsWith(output.toPath())) {
                Utils.getLogger().log(Level.SEVERE, "Detected Zip Slip attempt on entry " + entry.getName() + " from " + zip.getAbsolutePath());
                return false;
            }

            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            unzipEntry(zipFile, zipFile.getEntry(fileName), outputFile);
            return true;
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Error extracting file " + fileName + " from " + zip.getAbsolutePath());
            return false;
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private static void unzipEntry(ZipFile zipFile, ZipArchiveEntry entry, File outputFile) throws IOException, InterruptedException {
        byte[] buffer = new byte[2048];

        try (BufferedInputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            int length;
            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        } catch (ClosedByInterruptException ex) {
            throw new InterruptedException();
        }
    }

    public static void unzipFile(File zip, File output, IZipFileFilter fileFilter, DownloadListener listener) throws IOException, InterruptedException {
        if (!zip.exists()) {
            Utils.getLogger().log(Level.SEVERE, "File to unzip does not exist: " + zip.getAbsolutePath());
            return;
        }
        if (!output.exists()) {
            output.mkdirs();
        }

        ZipFile zipFile = new ZipFile(zip);
        ArrayList<ZipArchiveEntry> entries = Collections.list(zipFile.getEntries());
        int size = entries.size();

        // Commons Compress doesn't seem to throw exception when ZIP files aren't valid, so we just check if they
        // have no entries as a means of validating it, and throw a ZipException so the launcher will show the correct
        // warning message when trying to install/update the modpack, rather than just printing it to console.
        if (size == 0) {
            Utils.getLogger().log(Level.SEVERE, "Zip file is empty: " + zip.getAbsolutePath());
            zipFile.close();
            throw new ZipException("Zip file is empty: " + zip.getAbsolutePath());
        }

        int progress = 1;
        try {
            for (ZipArchiveEntry entry : entries) {
                if (Thread.interrupted())
                    throw new InterruptedException();

                if (fileFilter == null || fileFilter.shouldExtract(entry.getName())) {
                    File outputFile = new File(output, entry.getName());

                    // Zip Slip check
                    if (!outputFile.toPath().normalize().startsWith(output.toPath())) {
                        Utils.getLogger().log(Level.SEVERE, "Detected Zip Slip attempt on entry " + entry.getName() + " from " + zip.getAbsolutePath());
                        throw new IOException("Bad zip entry");
                    }

                    if (outputFile.exists() && outputFile.isFile() && !outputFile.canWrite()) {
                        if (outputFile.delete()) {
                            Utils.getLogger().log(Level.INFO, "Deleted non-writable file " + outputFile.getAbsolutePath());
                        } else {
                            throw new IOException("Failed to delete non-writable file " + outputFile.getAbsolutePath());
                        }
                    }

                    if (outputFile.getParentFile() != null) {
                        outputFile.getParentFile().mkdirs();
                    }

                    if (!entry.isDirectory()) {
                        unzipEntry(zipFile, entry, outputFile);
                    }
                }

                if (listener != null) {
                    float totalProgress = (float) progress / (float) size;
                    listener.stateChanged("Extracting " + entry.getName() + "...", totalProgress * 100.0f);
                }
                progress++;
            }
        } finally {
            zipFile.close();
        }
    }
}
