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

package net.technicpack.ui.controls.feeds;

import javax.swing.*;
import java.awt.*;

public class HorizontalGallery extends JPanel {
    private int pixelPosition = -8;
    private Component selectedComponent;
    private Component lastDisplayedComponent;
    private Component noComponentsMessage;

    public HorizontalGallery() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setOpaque(false);
    }

    public void setNoComponentsMessage(Component noComponentsMessage) {
        this.noComponentsMessage = noComponentsMessage;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(16,16);
    }

    @Override
    public Dimension getPreferredSize() {
        return super.getPreferredSize();
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x,y,w,h);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        boolean hasNoComponents = getComponentCount() == 0 || (getComponentCount() == 1 && getComponent(0) == noComponentsMessage);

        if (noComponentsMessage != null) {
            noComponentsMessage.setVisible(hasNoComponents);
            remove(noComponentsMessage);

            if (hasNoComponents)
                add(noComponentsMessage);
        }

        if (!hasNoComponents) {
            int startX = 0 - pixelPosition;

            for (Component component : getComponents()) {
                Rectangle cBounds = component.getBounds();
                component.setBounds(startX, cBounds.y, cBounds.width, cBounds.height);
                component.invalidate();

                if (startX >= 2 && (startX + cBounds.width) < getWidth())
                    lastDisplayedComponent = component;

                startX += cBounds.width + 8;
            }
        } else if (noComponentsMessage != null) {
            Dimension messageSize = noComponentsMessage.getPreferredSize();
            Dimension size = getSize();
            noComponentsMessage.setBounds((size.width - messageSize.width)/2, (size.height-messageSize.height)/2, messageSize.width, messageSize.height);
        }

        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(getBackground());
        ((Graphics2D)g).setPaint(getBackground());
        g.fillRect(0,0,getWidth(),getHeight());
    }

    @Override
    public void paintChildren(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        Paint oldPaint = g2d.getPaint();

        super.paintChildren(g);

        boolean hasNoComponents = getComponentCount() == 0 || (getComponentCount() == 1 && getComponent(0) == noComponentsMessage);

        if (!hasNoComponents) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color background = getBackground();
            Color flatBackground = new Color(background.getRed(), background.getGreen(), background.getBlue(), 255);

            if (getComponentCount() != 0) {
                if (getSelectedComponent() != getComponent(0)) {
                    g2d.setPaint(new GradientPaint(0, 0, flatBackground, 70, 0, new Color(0, 0, 0, 0)));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                if (lastDisplayedComponent != getComponents()[getComponentCount() - 1]) {
                    g2d.setPaint(new GradientPaint(getWidth() - 80, 0, new Color(0, 0, 0, 0), getWidth() - 10, 0, flatBackground));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
            g2d.setPaint(oldPaint);
        }
    }

    public Component getSelectedComponent() {
        if (getComponentCount() == 0)
            return null;
        Component first = getComponent(0);

        if (!containsComponent(selectedComponent))
            internalSelectComponent(first);

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
                internalSelectComponent(component);
                return;
            }
        }
    }

    public void selectPreviousComponent() {
        Component previousComponent = null;

        for (Component component : getComponents()) {
            if (component == getSelectedComponent() && previousComponent != null) {
                internalSelectComponent(previousComponent);
                return;
            }

            previousComponent = component;
        }
    }

    public boolean selectComponent(Component selection) {
        if (!containsComponent(selection))
            return false;

        boolean isAfterCurrentSelection = true;
        boolean isVisible = false;

        //Seek forward until we find the component we're trying to select.
        //If it's between getSelectedComponent() and lastDisplayedComponent, then isVisible will be
        //true coming out of the loop, and we'll not move the gallery position

        //If we find the component we're selecting before hitting the visual gallery items, then we need to travel
        //backward- otherwise, we need to travel forward
        for (Component component : getComponents()) {
            if (component == getSelectedComponent())
                isVisible = true;

            if (component == selection) {
                if (!isVisible)
                    isAfterCurrentSelection = false;
                break;
            } else if (component == lastDisplayedComponent) {
                isVisible = false;
                break;
            }
        }

        if (isVisible)
            return false;

        if (isAfterCurrentSelection)
            return seekForwardToComponent(selection);
        else
            return seekBackwardToComponent(selection);
    }

    protected boolean seekForwardToComponent(Component component) {
        Component lastComponent = null;
        boolean didMoveGallery = false;
        do {
            lastComponent = getSelectedComponent();

            selectNextComponent();

            if (getSelectedComponent() != lastComponent)
                didMoveGallery = true;

        } while (getSelectedComponent() != lastComponent && getSelectedComponent() != component);

        return didMoveGallery;
    }

    protected boolean seekBackwardToComponent(Component component) {
        Component lastComponent = null;
        boolean didMoveGallery = false;
        do {
            lastComponent = getSelectedComponent();

            selectPreviousComponent();

            if (getSelectedComponent() != lastComponent)
                didMoveGallery = true;

        } while (getSelectedComponent() != lastComponent && lastDisplayedComponent != component);

        return didMoveGallery;
    }

    protected void internalSelectComponent(Component selection) {
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

            this.pixelPosition = seekPixel;
            doLayout();
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
}
