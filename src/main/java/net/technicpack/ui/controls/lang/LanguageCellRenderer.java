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

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.listitems.LanguageItem;

public class LanguageCellRenderer extends JLabel implements ListCellRenderer<LanguageItem> {

  private ImageIcon globe;

  private final Color defaultBackground;
  private final Color defaultForeground;

  public LanguageCellRenderer(
      ResourceLoader resourceLoader,
      String langIcon,
      Color defaultBackground,
      Color defaultForeground) {
    if (langIcon != null) globe = resourceLoader.getIcon(langIcon);
    this.defaultBackground = defaultBackground;
    this.defaultForeground = defaultForeground;

    setForeground(defaultForeground);
    setBackground(defaultBackground);
    setFont(resourceLoader.getFont(ResourceLoader.FONT_OPENSANS, 14));
    setOpaque(true);
  }

  @Override
  public Component getListCellRendererComponent(
      JList<? extends LanguageItem> list,
      LanguageItem value,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    if (index < 0) {
      setForeground(defaultForeground);
      setBackground(defaultBackground);
    } else {
      setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
      setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
    }
    setFont(
        value
            .getLanguageResources()
            .getFont(ResourceLoader.FONT_OPENSANS, list.getFont().getSize()));
    setText(value.toString());

    Object selectedValue = list.getSelectedValue();

    if (globe != null) {
      if (!isSelected && selectedValue != null && selectedValue.equals(value)) {
        setIcon(globe);
      } else {
        setIcon(null);
      }
    }

    return this;
  }
}
