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

package net.technicpack.utilslib;

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

        if (getOperatingSystem() == WINDOWS) {
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

    public File getUserDirectoryForApp(String appName) {
        String userHome = System.getProperty("user.home", ".");

        switch (this) {
            case LINUX:
                return new File(userHome, "."+appName+"/");
            case WINDOWS:
                String applicationData = System.getenv("APPDATA");
                if (applicationData != null) {
                    return new File(applicationData, "."+appName+"/");
                } else {
                    return new File(userHome, "."+appName+"/");
                }
            case OSX:
                return new File(userHome, "Library/Application Support/" + appName);
            case UNKNOWN:
                return new File(userHome, appName + "/");
            default:
                return new File(userHome, appName + "/");
        }
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
