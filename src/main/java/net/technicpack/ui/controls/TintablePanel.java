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

package net.technicpack.ui.controls;

import javax.swing.*;
import java.awt.*;

public class TintablePanel extends JPanel {
    private Color tintColor;
    private boolean tintActive;

    public TintablePanel() {

    }

    public Color getTintColor() { return tintColor; }
    public void setTintColor(Color color) {
        this.tintColor = color;
    }

    public boolean isTintActive() { return tintActive; }
    public void setTintActive(boolean tintActive) {
        this.tintActive = tintActive;
        repaint();
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);

        if (tintActive) {
            graphics.setColor(getTintColor());
            graphics.fillRect(0,0,getWidth(),getHeight());
        }
    }
}
