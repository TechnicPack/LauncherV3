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
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class SimpleTabPaneUI extends BasicTabbedPaneUI {
    @Override
    protected Insets getContentBorderInsets(int tabPlacement) {
        return new Insets(0,0,0,0);
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement,
                                  int tabIndex,
                                  int x, int y, int w, int h,
                                  boolean isSelected ) {

    }

    protected void paintTabBackground(Graphics g, int tabPlacement,
                                      int tabIndex,
                                      int x, int y, int w, int h,
                                      boolean isSelected ) {
        g.setColor(tabPane.getBackgroundAt(tabIndex));
        switch(tabPlacement) {
            case LEFT:
                g.fillRect(x+1, y, w-1, h-2);
                break;
            case RIGHT:
                g.fillRect(x, y, w-2, h-2);
                break;
            case BOTTOM:
                g.fillRect(x, y, w-2, h-1);
                break;
            case TOP:
            default:
                g.fillRect(x, y, w-2, h-2);
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2d = (Graphics2D)g;

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        super.paint(g, c);
        g.setColor(tabPane.getBackgroundAt(tabPane.getSelectedIndex()));
        g.fillRect(tabPane.getX(), 0, tabPane.getWidth(), 2);

        g.setColor(tabPane.getComponentAt(tabPane.getSelectedIndex()).getBackground());
        g.fillRect(tabPane.getX(), calculateTabAreaHeight(tabPane.getTabPlacement(), runCount, maxTabHeight) - getTabAreaInsets(0).bottom, tabPane.getWidth(), getTabAreaInsets(0).bottom);

        drawTriangle(g);
    }

    private void drawTriangle(Graphics g) {

        Polygon triangle = new Polygon();
        Rectangle tabRect = rects[tabPane.getSelectedIndex()];

        int y = tabRect.y;
        int x = tabRect.x;
        int w = tabRect.width;
        int h = tabRect.height;

        switch (tabPane.getTabPlacement()) {
            case LEFT:
            {
                int height = y + (h-2)/2;
                triangle.addPoint(x+w, height-5);
                triangle.addPoint(x+w, height+5);
                triangle.addPoint(x+w+8, height);
                break;
            }
            case RIGHT:
            {
                int height = y + (h-2)/2;
                triangle.addPoint(x, height-5);
                triangle.addPoint(x, height+5);
                triangle.addPoint(x-8, height);
                break;
            }
            case BOTTOM:
            {
                int width = x + (w-2)/2;
                triangle.addPoint(width-5, y);
                triangle.addPoint(width+5, y);
                triangle.addPoint(width, y-8);
                break;
            }
            case TOP:
            default:
            {
                int width = x + (w-2)/2;
                triangle.addPoint(width-6, y+h-2);
                triangle.addPoint(width+6, y+h-2);
                triangle.addPoint(width, y+h+6);
                break;
            }
        }
        g.setColor(tabPane.getBackgroundAt(tabPane.getSelectedIndex()));
        g.fillPolygon(triangle);
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {

    }

    @Override
    protected Insets getTabAreaInsets(int tabPlacement) {
        Insets startInsets = new Insets(2, 0, 8, 0);
        Insets endInsets = new Insets(0,0,0,0);
        rotateInsets(startInsets, endInsets, tabPlacement);
        return endInsets;
    }

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return new Insets(8, 16, 6, 16);
    }
}
