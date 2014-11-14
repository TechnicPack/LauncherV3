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

package net.technicpack.launcher.ui.components;

import javax.swing.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public class InfoPanelReadyDetector implements HierarchyListener {
    private IInfoPanelListener listener;

    public InfoPanelReadyDetector(IInfoPanelListener listener) {
        this.listener = listener;
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags()) != 0 && e.getComponent().isShowing()) {
            JPanel panel = (JPanel)e.getComponent();
            listener.panelReady(panel);
            panel.removeHierarchyListener(this);
        }
    }
}
