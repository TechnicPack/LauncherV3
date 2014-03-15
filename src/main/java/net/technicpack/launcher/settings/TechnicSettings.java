/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.settings;

import net.technicpack.launchercore.util.OperatingSystem;

import java.io.File;

public class TechnicSettings {
    private String directory;

    private transient File technicRoot;

    public File getTechnicRoot() {
        if (technicRoot == null || !technicRoot.exists())
            buildTechnicRoot();

        return technicRoot;
    }

    public boolean isPortable() {
        return (directory != null && !directory.isEmpty() && directory.equalsIgnoreCase("portable"));
    }

    protected void buildTechnicRoot() {
        if (directory == null || directory.isEmpty())
            buildDefaultTechnicDirectory();
        else if (directory.equalsIgnoreCase("portable"))
            technicRoot = new File("technic/");
        else
            technicRoot = new File(directory);

        if (!technicRoot.exists())
            technicRoot.mkdirs();
    }

    private void buildDefaultTechnicDirectory() {
        String userHome = System.getProperty("user.home", ".");

        OperatingSystem os = OperatingSystem.getOperatingSystem();
        switch (os) {
            case LINUX:
                technicRoot = new File(userHome, ".technic/");
                break;
            case WINDOWS:
                String applicationData = System.getenv("APPDATA");
                if (applicationData != null) {
                    technicRoot = new File(applicationData, ".technic/");
                } else {
                    technicRoot = new File(userHome, ".technic/");
                }
                break;
            case OSX:
                technicRoot = new File(userHome, "Library/Application Support/technic");
                break;
            case UNKNOWN:
                technicRoot = new File(userHome, "technic/");
                break;
            default:
                technicRoot = new File(userHome, "technic/");
                break;
        }
    }
}
