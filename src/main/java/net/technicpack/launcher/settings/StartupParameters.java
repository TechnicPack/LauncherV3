/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
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
import com.beust.jcommander.internal.Lists;

import java.util.List;
import java.util.logging.Logger;

public final class StartupParameters {
    @SuppressWarnings("unused")
    private final String[] args;
    @Parameter
    private List<String> parameters = Lists.newArrayList();
    @Parameter(names = {"-console"}, description = "Shows the console window")
    private boolean console = false;
    @Parameter(names = {"-width"}, description = "Sets the width of the minecraft window to be fixed to this.")
    private int width = -1;
    @Parameter(names = {"-height"}, description = "Sets the height of the minecraft window to be fixed to this.")
    private int height = -1;
    @Parameter(names = {"-fullscreen"}, description = "Whether to launch minecraft in fullscreen mode.")
    private boolean fullscreen = false;

    public StartupParameters(String[] args) {
        this.args = args;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void logParameters(Logger log) {
        log.info("------------ Startup Parameters ------------");
        if (console) {
            log.info("Console frame enabled");
        }
        if (width != -1) {
            log.info("Minecraft frame width: " + width);
        }
        if (height != -1) {
            log.info("Minecraft frame height: " + height);
        }
        log.info("--------- End of Startup Parameters ---------");
    }

    public boolean isConsole() {
        return console;
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
}