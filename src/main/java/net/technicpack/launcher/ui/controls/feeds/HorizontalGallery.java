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
package net.technicpack.launcher.ui.controls.feeds;

import sun.java2d.SunGraphics2D;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class HorizontalGallery extends JPanel {
    private int pixelPosition = -8;
    private Component selectedComponent;
    private Component lastDisplayedComponent;

    public HorizontalGallery() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setOpaque(false);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0,0);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        int startX = 0 - pixelPosition;

        for (Component component : getComponents()) {
            Rectangle cBounds = component.getBounds();
            component.setBounds(startX, cBounds.y, cBounds.width, cBounds.height);
            component.invalidate();

            if (startX >= 2 && (startX + cBounds.width) < getWidth())
                lastDisplayedComponent = component;

            startX += cBounds.width + 8;
        }

        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        ((Graphics2D)g).setPaint(getBackground());
        g.fillRect(0,0,getWidth(),getHeight());
    }

    @Override
    public void paintChildren(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        Paint oldPaint = g2d.getPaint();

        super.paintChildren(g);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        if (getSelectedComponent() != getComponent(0)) {
            g2d.setPaint(new GradientPaint(0, 0, new Color(0,0,0,255), 70, 0, new Color(0,0,0,0)));
            g2d.fillRect(0,0,getWidth(),getHeight());
        }
        if (lastDisplayedComponent != getComponents()[getComponentCount()-1]) {
            g2d.setPaint(new GradientPaint(getWidth()-80, 0, new Color(0,0,0,0), getWidth()-10, 0, new Color(0,0,0,255)));
            g2d.fillRect(0,0,getWidth(),getHeight());
        }
        g2d.setPaint(oldPaint);
    }

    public Component getSelectedComponent() {
        if (getComponentCount() == 0)
            return null;
        Component first = getComponent(0);

        if (!containsComponent(selectedComponent))
            selectComponent(first);

        return selectedComponent;
    }

    public void selectNextComponent() {
        boolean getNextComponent = false;

        if (getComponents()[getComponentCount()-1] == lastDisplayedComponent)
            return;

        for (Component component : getComponents()) {
            if (component == getSelectedComponent()) {
                getNextComponent = true;
            } else if (getNextComponent) {
                selectComponent(component);
                return;
            }
        }
    }

    public void selectPreviousComponent() {
        Component previousComponent = null;

        for (Component component : getComponents()) {
            if (component == getSelectedComponent() && previousComponent != null) {
                selectComponent(previousComponent);
                return;
            }

            previousComponent = component;
        }
    }

    public void selectComponent(Component selection) {
        if (containsComponent(selection)) {
            selectedComponent = selection;

            int seekPixel = -8;
            for (Component previousComponent : getComponents()) {
                if (previousComponent == selection)
                    break;

                seekPixel += previousComponent.getWidth();
                seekPixel += 8;
            }

            if (seekPixel != -8) {
                seekPixel -= 40;
            }

            setTargetPixelPosition(seekPixel);
        }
    }

    private boolean containsComponent(Component selection) {
        boolean foundSelected = false;
        for (Component component : getComponents()) {
            if (component == selection) {
                foundSelected = true;
                break;
            }
        }

        return foundSelected;
    }

    public void setTargetPixelPosition(int targetPixelPosition) {
        this.pixelPosition = targetPixelPosition;
        revalidate();
    }
}
