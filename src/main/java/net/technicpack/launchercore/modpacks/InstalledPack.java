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

package net.technicpack.launchercore.modpacks;

public class InstalledPack {
    public static final String RECOMMENDED = "recommended";
    public static final String LATEST = "latest";
    public static final String LAUNCHER_DIR = "launcher\\";
    public static final String MODPACKS_DIR = "%MODPACKS%\\";

    private String name;
    private String build;
    private String directory;

    public InstalledPack(String name, String build, String directory) {
        this();
        this.name = name;
        this.build = build;
        this.directory = directory;
    }

    public InstalledPack(String name, String build) {
        this(name, build, MODPACKS_DIR + name);
    }

    public InstalledPack() {
        build = RECOMMENDED;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "InstalledPack{" +
                ", name='" + name + '\'' +
                ", build='" + build + '\'' +
                ", directory='" + directory + '\'' +
                '}';
    }
}
