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

package net.technicpack.ui.controls.lang;

import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.listitems.LanguageItem;

import javax.swing.*;
import java.awt.*;

public class LanguageCellRenderer  extends JLabel implements ListCellRenderer {

    private ResourceLoader resources;
    private ImageIcon globe;

    private Color background;
    private Color foreground;

    public LanguageCellRenderer(ResourceLoader resourceLoader, String langIcon, Color background, Color foreground) {
        resources = resourceLoader;

        if (langIcon != null)
            globe = resourceLoader.getIcon(langIcon);
        this.background = background;
        this.foreground = foreground;

        setForeground(foreground);
        setBackground(background);
        setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setForeground(this.foreground);
        setBackground(this.background);
        setFont(((LanguageItem)value).getLanguageResources().getFont(ResourceLoader.FONT_OPENSANS, list.getFont().getSize()));
        setText(value.toString());

        if (globe != null)
            setIcon((!isSelected && list.getSelectedValue().equals(value))?globe:null);

        return this;
    }
}
