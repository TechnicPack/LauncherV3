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

package net.technicpack.launcher.ui.listitems;

import net.technicpack.launchercore.util.LaunchAction;

public class OnLaunchItem {
    private String text;
    private LaunchAction launchAction;

    public OnLaunchItem(String text, LaunchAction action) {
        this.text = text;
        this.launchAction = action;
    }

    public LaunchAction getLaunchAction() { return launchAction; }
    public String toString() { return text; }
}
