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

package net.technicpack.launchercore.launch.java.source;

import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.launchercore.launch.java.IVersionSource;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.source.os.MacInstalledJavaSource;
import net.technicpack.launchercore.launch.java.source.os.WinRegistryJavaSource;

/**
 * This IVersionSource is used to collect the known-installed versions of java.  The code is OS-specific,
 * so the logic here is mainly for breaking out to the OS-specific versions
 */
public class InstalledJavaSource implements IVersionSource {
    @Override
    public void enumerateVersions(JavaVersionRepository repository) {
        if (OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS)
            (new WinRegistryJavaSource()).enumerateVersions(repository);
        else if (OperatingSystem.getOperatingSystem() == OperatingSystem.OSX)
            (new MacInstalledJavaSource()).enumerateVersions(repository);
    }
}
