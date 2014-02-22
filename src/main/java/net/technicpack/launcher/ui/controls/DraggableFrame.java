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
package net.technicpack.launcher.ui.controls;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class DraggableFrame extends JFrame implements MouseListener, MouseMotionListener {
    private int dragGripX;
    private int dragGripY;

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            dragGripX = e.getX();
            dragGripY = e.getY();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            this.setLocation(e.getXOnScreen() - dragGripX, e.getYOnScreen() - dragGripY);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
