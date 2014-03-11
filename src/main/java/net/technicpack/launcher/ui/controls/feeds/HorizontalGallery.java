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

import javax.swing.*;
import java.awt.*;

public class HorizontalGallery extends JPanel {
    private int pixelPosition = -8;
    private int targetPixelPosition = -8;
    private int pixelChaseSpeed = 20;
    private boolean runningPixelChase;
    private Component selectedComponent;

    public HorizontalGallery() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0,0);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        int pixel = 0;
        synchronized (this) {
            pixel = pixelPosition;
        }
        int startX = 0 - pixel;

        for (Component component : getComponents()) {
            Rectangle cBounds = component.getBounds();
            component.setBounds(startX, cBounds.y, cBounds.width, cBounds.height);
            component.invalidate();
            startX += cBounds.width + 8;
        }
    }

    @Override
    public void paintChildren(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.clipRect(0, 0, getWidth(), getHeight());

        super.paintChildren(g);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setPaint(new GradientPaint(getWidth()-80, 0, new Color(0,0,0,0), getWidth()-10, 0, new Color(0,0,0,255)));
        g2d.fillRect(0,0,getWidth(),getHeight());
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

        for (Component component : getComponents()) {
            if (component == selectedComponent) {
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
            if (component == selectedComponent && previousComponent != null) {
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

    public void runPixelChase() {
        try {
            while(!chaseTargetPixel()) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        repaint();
                    }
                });
                Thread.sleep(200);
            }
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    repaint();
                }
            });
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void setTargetPixelPosition(int targetPixelPosition) {
        synchronized (this) {
            this.targetPixelPosition = targetPixelPosition;

            if (!runningPixelChase && this.targetPixelPosition != this.pixelPosition) {
                runningPixelChase = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runPixelChase();
                    }
                }).run();
            }
        }
    }

    protected boolean chaseTargetPixel() {
        synchronized (this) {
            if (targetPixelPosition != pixelPosition) {
                if (Math.abs(targetPixelPosition-pixelPosition) < pixelChaseSpeed) {
                    pixelPosition = targetPixelPosition;
                    runningPixelChase = false;
                    return true;
                } else if (targetPixelPosition < pixelPosition)
                    pixelPosition -= pixelChaseSpeed;
                else
                    pixelPosition += pixelChaseSpeed;

                return false;
            }

            runningPixelChase = false;
            return true;
        }
    }
}
