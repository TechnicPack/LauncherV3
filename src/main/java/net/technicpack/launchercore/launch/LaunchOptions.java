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

package net.technicpack.launchercore.launch;

import java.util.List;

public class LaunchOptions {
    private int width;
    private int height;
    private boolean fullscreen;
    private String title;
    private String iconPath;

    public LaunchOptions(String title, String iconPath, int width, int height, boolean fullscreen) {
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        this.title = title;
        this.iconPath = iconPath;
    }

    public String getTitle() {
        return title;
    }

    public String getIconPath() {
        return iconPath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean getFullscreen() {
        return fullscreen;
    }

    public void appendToCommands(List<String> commands) {
        if (getTitle() != null) {
            commands.add("--title");
            commands.add(title);
        }

        if (getWidth() > -1) {
            commands.add("--width");
            commands.add(Integer.toString(getWidth()));
        }

        if (getHeight() > -1) {
            commands.add("--height");
            commands.add(Integer.toString(getHeight()));
        }

        if (getFullscreen()) {
            commands.add("--fullscreen");
        }

        if (getIconPath() != null) {
            commands.add("--icon");
            commands.add(getIconPath());
        }
    }
}
