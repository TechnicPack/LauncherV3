package net.technicpack.launcher.ui.controls;


import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;

import javax.swing.*;

/**
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

public class HeaderTab extends AAJLabel {
    private boolean isActive;

    public HeaderTab(String text, ResourceLoader resources) {
        super(text);

        setIsActive(false);

        setFont(resources.getFont("Raleway-Light.ttf", 26));
        setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        setBackground(LauncherFrame.COLOR_BLUE_DARKER);
        setBorder(BorderFactory.createEmptyBorder(20,18,20,18));
    }

    public boolean isActive() { return isActive; }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
        this.setOpaque(isActive);
    }
}
