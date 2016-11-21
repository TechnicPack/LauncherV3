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
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipUtils {

    public static boolean checkLaunchDirectory(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }

        if (dir.list().length == 0) {
            return true;
        }

        for (File file : dir.listFiles()) {
            if (file.getName().equals("settings.json")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a directory is empty
     *
     * @param dir to check
     * @return true if the directory is empty
     */
    public static boolean checkEmpty(File dir) {
        return dir.isDirectory() && dir.list().length == 0;

    }

    public static boolean extractFile(File zip, File output, String fileName) throws IOException, InterruptedException {
        if (!zip.exists() || fileName == null) {
            return false;
        }

        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(zip);
            ZipEntry entry = zipFile.getEntry(fileName);
            if (entry == null) {
                Utils.getLogger().log(Level.WARNING, "File " + fileName + " not found in " + zip.getAbsolutePath());
                return false;
            }
            File outputFile = new File(output, entry.getName());

            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            unzipEntry(zipFile, zipFile.getEntry(fileName), outputFile);
            return true;
        } catch (ZipException e) {
            e.printStackTrace();
            throw new ZipException("Error extracting file " + zip.getName());
        } catch (IOException e) {
            Utils.getLogger().log(Level.WARNING, "Error extracting file " + fileName + " from " + zip.getAbsolutePath());
            return false;
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private static void unzipEntry(ZipFile zipFile, ZipEntry entry, File outputFile) throws IOException, InterruptedException {
        byte[] buffer = new byte[2048];
        BufferedInputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            int length;
            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        } catch (ClosedByInterruptException ex) {
            throw new InterruptedException();
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
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
        int size = zipFile.size() + 1;
        int progress = 1;
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                if (Thread.interrupted())
                    throw new InterruptedException();

                ZipEntry entry = null;

                try {
                    entry = entries.nextElement();
                } catch (IllegalArgumentException ex) {
                    //We must catch & rethrow as a zip exception because some crappy code in the zip lib will
                    //throw illegal argument exceptions for malformed zips.
                    throw new ZipException("IllegalArgumentException while parsing next element.");
                }

                if (!entry.getName().contains("../") && (fileFilter == null || fileFilter.shouldExtract(entry.getName()))) {
                    File outputFile = new File(output, entry.getName());

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
