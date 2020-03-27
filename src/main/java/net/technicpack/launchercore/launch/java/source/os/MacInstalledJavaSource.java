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

package net.technicpack.launchercore.launch.java.source.os;

import net.technicpack.launchercore.launch.java.IVersionSource;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.version.FileBasedJavaVersion;
import net.technicpack.utilslib.Utils;

import java.io.File;

public class MacInstalledJavaSource implements IVersionSource {
    @Override
    public void enumerateVersions(JavaVersionRepository repository) {
        repository.addVersion(new FileBasedJavaVersion(new File(getMacJava("1.6"))));
        repository.addVersion(new FileBasedJavaVersion(new File(getMacJava("1.7"))));
        repository.addVersion(new FileBasedJavaVersion(new File(getMacJava("1.8"))));
        repository.addVersion(new FileBasedJavaVersion(new File ("/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java")));
    }

    protected String getMacJava(String versionNumber) {
        String path = Utils.getProcessOutput("/usr/libexec/java_home", "-v", versionNumber);
        return path + File.separator + "bin" + File.separator + "java";
    }
}
