/*
 * This file is part of Technic UI Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic UI Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic UI Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic UI Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.controls.tabs;

import javax.swing.*;
import java.awt.*;

public class SimpleTabPane extends JTabbedPane {

    private Color selectedForeground;
    private Color selectedBackground;

    public SimpleTabPane() {
        super();

        setUI(new SimpleTabPaneUI());
    }

    public Color getSelectedForeground() { return selectedForeground; }
    public void setSelectedForeground(Color color) {
        selectedForeground = color;
    }

    public Color getSelectedBackground() { return selectedBackground; }
    public void setSelectedBackground(Color color) {
        selectedBackground = color;
    }

    @Override
    public Color getBackgroundAt(int index) {
        if (getSelectedIndex() == index)
            return getSelectedBackground();
        else
            return getBackground();
    }

    @Override
    public Color getForegroundAt(int index) {
        if (getSelectedIndex() == index)
            return getSelectedForeground();
        else
            return getForeground();
    }
}
