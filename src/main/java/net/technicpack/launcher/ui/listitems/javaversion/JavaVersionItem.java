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

package net.technicpack.launcher.ui.listitems.javaversion;

import net.technicpack.launchercore.launch.java.IJavaVersion;
import net.technicpack.ui.lang.ResourceLoader;

public class JavaVersionItem {

    private IJavaVersion javaVersion;
    private ResourceLoader resourceLoader;

    public JavaVersionItem(IJavaVersion javaVersion, ResourceLoader resourceLoader) {
        this.javaVersion = javaVersion;
        this.resourceLoader = resourceLoader;
    }

    public String getVersionNumber() { return javaVersion.getVersionNumber(); }
    public boolean is64Bit() { return javaVersion.is64Bit(); }

    protected IJavaVersion getJavaVersion() { return javaVersion; }
    protected ResourceLoader getResourceLoader() { return resourceLoader; }

    public String toString() {
        String text = javaVersion.getVersionNumber();

        String bitness = (javaVersion.is64Bit())?resourceLoader.getString("launcheroptions.java.64bit"):resourceLoader.getString("launcheroptions.java.32bit");

        return text + " " + bitness;
    }
}
