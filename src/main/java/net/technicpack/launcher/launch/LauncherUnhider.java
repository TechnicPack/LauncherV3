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

package net.technicpack.launcher.launch;

import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launchercore.launch.ProcessExitListener;
import net.technicpack.launchercore.util.LaunchAction;

import java.awt.*;

public class LauncherUnhider implements ProcessExitListener {

    private final TechnicSettings settings;
    private final LauncherFrame frame;
    private boolean hasExited = false;

    public LauncherUnhider(TechnicSettings settings, LauncherFrame frame) {
        this.settings = settings;
        this.frame = frame;
    }

    @Override
    public void onProcessExit() {
        LaunchAction action = settings.getLaunchAction();
        if (action == null || action == LaunchAction.HIDE) {
            frame.setVisible(true);
        }

        hasExited = true;
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.launchCompleted();
            }
        });
    }

    public boolean hasExited() {
        return hasExited;
    }
}