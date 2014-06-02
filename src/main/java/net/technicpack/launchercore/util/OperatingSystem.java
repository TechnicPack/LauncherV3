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

package net.technicpack.launchercore.util;

import java.io.File;
import java.util.Locale;

public enum OperatingSystem {
    LINUX("linux", new String[]{"linux", "unix"}),
    WINDOWS("windows", new String[]{"win"}),
    OSX("osx", new String[]{"mac"}),
    UNKNOWN("unknown", new String[0]);

    private static OperatingSystem operatingSystem;
    private final String name;
    private final String[] aliases;

    private OperatingSystem(String name, String[] aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public static String getJavaDir() {
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("java.home") + separator + "bin" + separator;

        if ((getOperatingSystem() == WINDOWS) &&
                (new File(path + "javaw.exe").isFile())) {
            return path + "javaw.exe";
        }

        return path + "java";
    }

    public static OperatingSystem getOperatingSystem() {
        if (OperatingSystem.operatingSystem != null) {
            return OperatingSystem.operatingSystem;
        }

        //Always specify english when tolowercase/touppercasing values for comparison against well-known values
        //Prevents an issue with turkish users
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        for (OperatingSystem operatingSystem : values()) {
            for (String alias : operatingSystem.getAliases()) {
                if (osName.contains(alias)) {
                    OperatingSystem.operatingSystem = operatingSystem;
                    return operatingSystem;
                }
            }
        }

        return UNKNOWN;
    }

    public String[] getAliases() {
        return aliases;
    }

    public String getName() {
        return name;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }
}
