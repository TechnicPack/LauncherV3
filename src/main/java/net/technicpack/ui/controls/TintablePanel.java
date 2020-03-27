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

package net.technicpack.ui.controls;

import javax.swing.*;
import java.awt.*;

public class TintablePanel extends JPanel {
    private Color tintColor;
    private boolean tintActive;
    private JLabel overIcon = null;

    public TintablePanel() {
        tintColor = new Color(0,0,0,0);
    }

    public Color getTintColor() { return tintColor; }
    public void setTintColor(Color color) {
        this.tintColor = color;
    }

    public void setOverIcon(ImageIcon image) {
        if (overIcon != null) {
            remove(overIcon);
        }

        overIcon = new JLabel(image);
        overIcon.setVisible(false);
        add(overIcon);
        revalidate();
    }

    public boolean isTintActive() { return tintActive; }
    public void setTintActive(boolean tintActive) {
        this.tintActive = tintActive;

        if (overIcon != null) {
            overIcon.setVisible(tintActive);
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    revalidate();
                }
            });
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
    }

    @Override
    public void doLayout() {
        super.doLayout();

        if (overIcon != null) {
            int width = overIcon.getIcon().getIconWidth();
            int height = overIcon.getIcon().getIconHeight();
            overIcon.setBounds(getWidth() / 2 - width / 2, getHeight() / 2 - height / 2, width, height);
        }
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
