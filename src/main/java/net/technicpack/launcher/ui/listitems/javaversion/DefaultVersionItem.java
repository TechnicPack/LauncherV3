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

public class DefaultVersionItem extends JavaVersionItem {

    public DefaultVersionItem(IJavaVersion version, ResourceLoader resourceLoader) {
        super(version, resourceLoader);
    }

    @Override
    public String getVersionNumber() { return "default"; }

    @Override
    public String toString() {
        return getResourceLoader().getString("launcheroptions.java.defaultversion", super.toString());
    }
}
