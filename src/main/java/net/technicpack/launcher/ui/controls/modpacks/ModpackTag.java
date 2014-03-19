/*
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

package net.technicpack.launcher.ui.controls.modpacks;

import net.technicpack.launcher.ui.controls.AAJLabel;

import java.awt.*;

public class ModpackTag extends AAJLabel {
    private Color underlineColor;

    public ModpackTag(String text) {
        super(text);
        underlineColor = new Color(0,0,0,0);
    }

    public Color getUnderlineColor() { return underlineColor; }
    public void setUnderlineColor(Color color) { underlineColor = color; }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(underlineColor);
        g.fillRect(0, getHeight()-1, getWidth(), 1);
    }
}
