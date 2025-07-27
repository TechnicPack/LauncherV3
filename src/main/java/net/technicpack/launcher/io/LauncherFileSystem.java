/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.io;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LauncherFileSystem {
    private final Path rootDirectory;
    private final Path assetsDirectory;
    private final Path cacheDirectory;
    private final Path logsDirectory;
    private final Path modpacksDirectory;
    private final Path runtimesDirectory;
    private final Path launcherAssetsDirectory;
    private final Path packAssetsDirectory;

    public LauncherFileSystem(Path rootDir) {
        this.rootDirectory = rootDir.toAbsolutePath().normalize();

        assetsDirectory = rootDirectory.resolve("assets");
        cacheDirectory = rootDirectory.resolve("cache");
        logsDirectory = rootDirectory.resolve("logs");
        modpacksDirectory = rootDirectory.resolve("modpacks");
        runtimesDirectory = rootDirectory.resolve("runtimes");

        launcherAssetsDirectory = assetsDirectory.resolve("launcher");
        packAssetsDirectory = assetsDirectory.resolve("packs");

        createDirectories();
    }

    public void createDirectories() {
        createDirectory(rootDirectory);
        createDirectory(assetsDirectory);
        createDirectory(cacheDirectory);
        createDirectory(logsDirectory);
        createDirectory(modpacksDirectory);
        createDirectory(runtimesDirectory);
        createDirectory(launcherAssetsDirectory);
        createDirectory(packAssetsDirectory);
    }

    private void createDirectory(Path path) {
        ensureDirectory(path);
    }

    private Path ensureDirectory(Path path) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                return path;
            }

            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                abortWithErrorDialog(path);
            }
        }

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            abortWithErrorDialog(path);
        }

        return path;
    }

    private static void abortWithErrorDialog(Path path) {
        JOptionPane.showMessageDialog(null, String.format("Failed to create directory %s.%nThis is a critical error, the launcher will terminate now.", path), "Critical error - Technic Launcher", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    /**
     * The root directory of the launcher storage.
     * <p/>
     * On a default setup on Windows, this is "%APPDATA%\.technic"
     */
    public Path getRootDirectory() {
        return ensureDirectory(rootDirectory);
    }

    /**
     * The directory where cached files are stored.
     * <p/>
     * This includes FML libraries, Minecraft client jars, Minecraft libraries, and the discover page.
     */
    public Path getCacheDirectory() {
        return ensureDirectory(cacheDirectory);
    }

    /**
     * The directory where assets are stored
     */
    public Path getAssetsDirectory() {
        return assetsDirectory;
    }

    /**
     * The directory where modpacks are installed to
     */
    public Path getModpacksDirectory() {
        return ensureDirectory(modpacksDirectory);
    }

    /**
     * The directory for Mojang JREs
     */
    public Path getRuntimesDirectory() {
        return ensureDirectory(runtimesDirectory);
    }

    /**
     * The directory for launcher log files
     */
    public Path getLogsDirectory() {
        return ensureDirectory(logsDirectory);
    }

    /**
     * The directory for launcher assets
     */
    public Path getLauncherAssetsDirectory() {
        return ensureDirectory(launcherAssetsDirectory);
    }

    /**
     * The directory for modpack assets
     */
    public Path getPackAssetsDirectory() {
        return ensureDirectory(packAssetsDirectory);
    }
}
