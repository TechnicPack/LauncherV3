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

import net.technicpack.launchercore.launch.java.version.CurrentJavaVersion;
import net.technicpack.utilslib.OperatingSystem;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Represents a repository of all the versions of java available to launch games with.
 */
public class JavaVersionRepository {
    private Map<File, IJavaVersion> loadedVersions = new HashMap<File, IJavaVersion>();
    private Collection<IJavaVersion> versionCache = new LinkedList<IJavaVersion>();
    private IJavaVersion selectedVersion;

    public JavaVersionRepository() {
        IJavaVersion version = new CurrentJavaVersion();
        selectedVersion = version;
        loadedVersions.put(null, version);
    }

    public void addVersion(IJavaVersion version) {
        if (!version.verify())
            return;

        File path = version.getJavaPath();
        String versionText = version.getVersionNumber();

        if (versionCache.contains(version))
            return;

        loadedVersions.put(path, version);
        versionCache.add(version);
        if (selectedVersion == null)
            selectedVersion = version;
    }

    public IJavaVersion getBest64BitVersion() {
        IJavaVersion bestVersion = null;
        for (IJavaVersion version : loadedVersions.values()) {
            if (version.is64Bit()) {
                if (bestVersion == null || bestVersion.getVersionNumber() == null) {
                    bestVersion = version;
                    continue;
                }

                if (version.getVersionNumber() == null)
                    continue;

                if (version.getVersionNumber().compareTo(bestVersion.getVersionNumber()) > 0)
                    bestVersion = version;
            }
        }

        return bestVersion;
    }

    public Collection<IJavaVersion> getVersions() {
        return versionCache;
    }

    public IJavaVersion getSelectedVersion() {
        return selectedVersion;
    }

    public void selectVersion(String version, boolean is64Bit) {
        selectedVersion = getVersion(version, is64Bit);
    }

    public IJavaVersion getVersion(String version, boolean is64Bit) {
        if (version == null || version.isEmpty() || version.equals("default")) {
            return loadedVersions.get(null);
        } else if (version.equals("64bit")) {
            IJavaVersion best64BitVersion = getBest64BitVersion();
            if (best64BitVersion == null)
                best64BitVersion = loadedVersions.get(null);
            return best64BitVersion;
        } else {
            for (IJavaVersion checkVersion : versionCache) {
                if (version.equals(checkVersion.getVersionNumber()) && is64Bit == checkVersion.is64Bit())
                    return checkVersion;
            }

            IJavaVersion specifiedVersion = loadedVersions.get(new File(version));

            if (specifiedVersion == null) {
                specifiedVersion = loadedVersions.get(null);
            }

            return specifiedVersion;
        }
    }

    public String getSelectedPath() {
        if (selectedVersion == null || selectedVersion.getJavaPath() == null)
            return OperatingSystem.getJavaDir();

        return selectedVersion.getJavaPath().getAbsolutePath();
    }
}
