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

package net.technicpack.launcher.ui.controls;

import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;

import javax.swing.*;
import java.awt.*;

public class SelectorWidget extends JButton {

    private ResourceLoader resources;

    private boolean isSelected;

    public SelectorWidget(ResourceLoader resources) {
        this.resources = resources;
    }

    protected ResourceLoader getResources() { return resources; }

    protected void initComponents() {
        setBorder(BorderFactory.createEmptyBorder());
        setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setContentAreaFilled(false);
        setOpaque(true);
        setFocusPainted(false);
    }

    public boolean isSelected() { return isSelected; }
    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
        setBackground(isSelected?LauncherFrame.COLOR_PANEL:LauncherFrame.COLOR_SELECTOR_BACK);
    }
}