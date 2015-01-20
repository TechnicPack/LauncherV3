/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
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
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a repository of all the versions of java available to launch games with.
 */
public class JavaVersionRepository {
    private Map<File, IJavaVersion> loadedVersions = new HashMap<File, IJavaVersion>();
    private Map<String, IJavaVersion> versionsByString = new HashMap<String, IJavaVersion>();
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

        if (versionsByString.containsKey(versionText))
            return;

        loadedVersions.put(path, version);
        versionsByString.put(versionText, version);
        if (selectedVersion == null)
            selectedVersion = version;
    }

    public IJavaVersion getBest64BitVersion() {
        IJavaVersion bestVersion = null;
        for(IJavaVersion version : loadedVersions.values()) {
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

    public IJavaVersion getSelectedVersion() { return selectedVersion; }
    public void selectVersion(String version) {
        if (version == null || version.isEmpty() || version.equals("default")) {
            selectedVersion = loadedVersions.get(null);
        } else if (version.equals("64bit")) {
            selectedVersion = getBest64BitVersion();
            if (selectedVersion == null)
                selectedVersion = loadedVersions.get(null);
        } else {
            selectedVersion = versionsByString.get(version);

            if ( selectedVersion == null) {
                selectedVersion = loadedVersions.get(new File(version));
            }

            if (selectedVersion == null) {
                loadedVersions.get(null);
            }
        }
    }

    public String getSelectedPath() {
        if (selectedVersion == null || selectedVersion.getJavaPath() == null)
            return OperatingSystem.getJavaDir();

        return selectedVersion.getJavaPath().getAbsolutePath();
    }
}
