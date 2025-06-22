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

package net.technicpack.launchercore.launch.java;

import net.technicpack.launchercore.launch.java.version.CurrentJavaRuntime;

import java.io.File;
import java.util.*;

/**
 * A repository of all the system Java runtimes available to launch Minecraft with
 */
public class JavaVersionRepository {
    public static final String VERSION_DEFAULT = "default";
    public static final String VERSION_LATEST_64BIT = "64bit";
    private final Map<File, IJavaRuntime> loadedVersions = new HashMap<>();
    private final Collection<IJavaRuntime> versionCache = new LinkedList<>();
    private IJavaRuntime selectedVersion;

    public JavaVersionRepository() {
        IJavaRuntime currentVersion = new CurrentJavaRuntime();
        selectedVersion = currentVersion;
        loadedVersions.put(null, currentVersion);
        versionCache.add(currentVersion);
    }

    public boolean addVersion(IJavaRuntime version) {
        if (loadedVersions.containsKey(version.getExecutableFile()) || versionCache.contains(version))
            return false;

        if (!version.isValid())
            return false;

        File path = version.getExecutableFile();

        loadedVersions.put(path, version);
        versionCache.add(version);

        if (selectedVersion == null)
            selectedVersion = version;

        return true;
    }

    public IJavaRuntime getBest64BitVersion() {
        return loadedVersions.values().stream()
                .filter(IJavaRuntime::is64Bit)
                .max(Comparator.comparing(IJavaRuntime::getVersion))
                .orElse(null);
    }

    public Collection<IJavaRuntime> getVersions() {
        return loadedVersions.values();
    }

    public IJavaRuntime getSelectedVersion() {
        return selectedVersion;
    }

    public void selectVersion(String version, boolean is64Bit) {
        selectedVersion = getVersion(version, is64Bit);
    }

    public void setSelectedVersion(IJavaRuntime version) {
        if (version == null) throw new IllegalArgumentException("version cannot be null");

        if (!loadedVersions.containsValue(version)) {
            throw new IllegalArgumentException("version is not loaded");
        }

        selectedVersion = version;
    }

    public IJavaRuntime getDefaultVersion() {
        return loadedVersions.get(null);
    }

    public IJavaRuntime getVersion(String version, boolean is64Bit) {
        if (version == null || version.isEmpty() || version.equals(VERSION_DEFAULT)) {
            return getDefaultVersion();
        }

        if (version.equals(VERSION_LATEST_64BIT)) {
            IJavaRuntime best64BitVersion = getBest64BitVersion();
            if (best64BitVersion == null)
                best64BitVersion = getDefaultVersion();
            return best64BitVersion;
        }

        for (IJavaRuntime checkVersion : versionCache) {
            if (version.equals(checkVersion.getVersion()) && is64Bit == checkVersion.is64Bit())
                return checkVersion;
        }

        IJavaRuntime specifiedVersion = loadedVersions.get(new File(version));

        if (specifiedVersion == null) {
            specifiedVersion = getDefaultVersion();
        }

        return specifiedVersion;
    }

}
