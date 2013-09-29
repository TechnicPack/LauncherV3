/*
 * This file is part of Technic Launcher.
 * Copyright (C) 2013 Syndicate, LLC
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

package net.technicpack.launchercore.util;


import net.technicpack.launchercore.minecraft.ExtractRules;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
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
		if (!dir.isDirectory()) {
			return false;
		}

		return dir.list().length == 0;
	}

	public static boolean extractFile(File zip, File output, String fileName) throws IOException {
		if (!zip.exists() || fileName == null) {
			return false;
		}

		ZipFile zipFile = new ZipFile(zip);
		try {
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
		} catch (IOException e) {
			Utils.getLogger().log(Level.WARNING, "Error extracting file " + fileName + " from " + zip.getAbsolutePath());
			return false;
		} finally {
			zipFile.close();
		}
	}

	public static void unzipFile(File zip, File output, DownloadListener listener) throws IOException {
		unzipFile(zip, output, null, listener);
	}

	/**
	 * Unzips a file into the specified directory.
	 *
	 * @param zip      file to unzip
	 * @param output   directory to unzip into
	 * @param extractRules extractRules for this zip file. May be null indicating no rules.
	 * @param listener to update progress on - may be null for no progress indicator
	 */
	public static void unzipFile(File zip, File output, ExtractRules extractRules, DownloadListener listener) throws IOException {
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
				ZipEntry entry = entries.nextElement();

				if ((extractRules == null || extractRules.shouldExtract(entry.getName())) && !entry.getName().contains("../")) {
					File outputFile = new File(output, entry.getName());

					if (outputFile.getParentFile() != null) {
						outputFile.getParentFile().mkdirs();
					}

					if (!entry.isDirectory()) {
						unzipEntry(zipFile, entry, outputFile);
					}
				}

				if (listener != null) {
					long totalProgress = progress / size;
					listener.stateChanged("Extracting " + entry.getName() + "...", totalProgress);
				}
				progress++;
			}
		} finally {
			zipFile.close();
		}
	}

	private static void unzipEntry(ZipFile zipFile, ZipEntry entry, File outputFile) throws IOException {
		byte[] buffer = new byte[2048];
		BufferedInputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
		BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
		try {
			int length;
			while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
				outputStream.write(buffer, 0, length);
			}
		} finally {
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(inputStream);
		}
	}
}
