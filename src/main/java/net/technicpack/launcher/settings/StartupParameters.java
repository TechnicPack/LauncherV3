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

package net.technicpack.launcher.settings;

import com.beust.jcommander.Parameter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class StartupParameters {
    private final String[] args;

    @Parameter(names = "-console", description = "Show the console window")
    private boolean console = false;

    @Parameter(names = "-launcheronly", description = "Start in launcher mode (rather than update/mover)")
    private boolean launcher = false;

    @Parameter(names = "-launcher", description = "Legacy launcher mode (indicates we should do a full update)")
    private boolean oldLauncher = false;

    @Parameter(names = "-moveronly", description = "Start in mover mode (copies recently-downloaded update to originally-run package)")
    private boolean mover = false;

    @Parameter(names = "-mover", description = "Legacy mover mode (used to detect old-updater clients trying to update)")
    private boolean oldMover = false;

    @Parameter(names = "-update", description = "Start in update mode (closes after downloading updated resources)")
    private boolean update = false;

    @Parameter(names = "-movetarget", description = "The path of the originally-run package to copy to")
    private String moveTarget = null;

    @Parameter(names = "-discover", description = "An override param for the discover URL")
    private String discover = null;

    @Parameter(names = "-blockReboot", description = "Prevent rebooting the launcher due to bad java properties")
    private boolean blockReboot = false;

    @Parameter(names = "-buildNumber", description = "Force build number to this value for debugging")
    private String buildNumber = "";

    @Parameter(names= "-skipUpdate", description = "Skip the launcher update (still updates resources)")
    private boolean skipUpdate = false;

    public StartupParameters(String[] args) {
        this.args = args;
    }

    public List<String> getArgs() { return Collections.unmodifiableList(Arrays.asList(args)); }

    public boolean isConsole() {
        return console;
    }

    public boolean isLauncher() { return launcher; }

    public boolean isLegacyLauncher() { return oldLauncher; }

    public boolean isMover() { return mover; }

    public boolean isLegacyMover() { return oldMover; }

    public boolean isUpdate() { return update; }

    public boolean isBlockReboot() { return blockReboot; }

    public String getMoveTarget() { return moveTarget; }

    public String getDiscoverUrl() { return discover; }

    public String getBuildNumber() { return buildNumber; }

    public boolean isSkipUpdate() { return skipUpdate; }
}
