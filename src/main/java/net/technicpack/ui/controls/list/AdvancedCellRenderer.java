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

package net.technicpack.ui.controls.list;

import javax.swing.*;
import java.awt.*;

public class AdvancedCellRenderer extends JLabel implements ListCellRenderer {

    private Color selectedBackground;
    private Color selectedForeground;
    private Color unselectedBackground;
    private Color unselectedForeground;

    public AdvancedCellRenderer() {
        setOpaque(true);
    }

    public Color getSelectedBackgroundColor() { return selectedBackground; }
    public void setSelectedBackgroundColor(Color color) { selectedBackground = color; }

    public Color getSelectedForegroundColor() { return selectedForeground; }
    public void setSelectedForegroundColor(Color color) { selectedForeground = color; }

    public Color getUnselectedBackgroundColor() { return unselectedBackground; }
    public void setUnselectedBackgroundColor(Color color) { unselectedBackground = color; }

    public Color getUnselectedForegroundColor() { return unselectedForeground; }
    public void setUnselectedForegroundColor(Color color) { unselectedForeground = color; }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(getSelectedBackgroundColor());
            setForeground(getSelectedForegroundColor());
        } else {
            setBackground(getUnselectedBackgroundColor());
            setForeground(getUnselectedForegroundColor());
        }

        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        if (value == null)
            this.setText("");
        else
            this.setText(value.toString());

        return this;
    }
}
