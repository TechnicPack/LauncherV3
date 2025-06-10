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

import net.technicpack.ui.controls.list.SimpleScrollPopup;
import net.technicpack.ui.controls.list.popupformatters.IPopupFormatter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;

public class LanguageCellUI extends BasicComboBoxUI {

    private final Color trackColor;
    private final Color thumbColor;

    private final IPopupFormatter popupFormatter;

    public LanguageCellUI(IPopupFormatter popupFormatter, Color trackColor, Color thumbColor) {
        this.popupFormatter = popupFormatter;
        this.trackColor = trackColor;
        this.thumbColor = thumbColor;
    }

    @Override protected JButton createArrowButton() {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        return button;
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup comboPopup = new SimpleScrollPopup(comboBox, trackColor, thumbColor);
        popupFormatter.formatPopup(comboPopup);
        return comboPopup;
    }
}
