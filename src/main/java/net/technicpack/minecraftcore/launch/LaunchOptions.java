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

package net.technicpack.minecraftcore.launch;

import java.util.List;

public class LaunchOptions {
    private String title;
    private String iconPath;
    private ILaunchOptions options;

    public LaunchOptions(String title, String iconPath, ILaunchOptions options) {
        this.options = options;
        this.title = title;
        this.iconPath = iconPath;
    }

    public String getTitle() {
        return title;
    }

    public String getIconPath() {
        return iconPath;
    }

    public ILaunchOptions getOptions() { return options; }

    public void appendToCommands(List<String> commands) {
        if (getTitle() != null) {
            commands.add("--title");
            commands.add(title);
        }

        if (options.getLaunchWindowType() == WindowType.FULLSCREEN)
            commands.add("--fullscreen");
        else if (options.getLaunchWindowType() == WindowType.CUSTOM) {
            commands.add("--width");
            commands.add(Integer.toString(options.getCustomWidth()));
            commands.add("--height");
            commands.add(Integer.toString(options.getCustomHeight()));
        }

        if (getIconPath() != null) {
            commands.add("--icon");
            commands.add(getIconPath());
        }
    }
}
