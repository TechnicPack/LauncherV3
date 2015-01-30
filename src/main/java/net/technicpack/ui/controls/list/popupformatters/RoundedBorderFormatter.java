/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.controls.list.popupformatters;

import net.technicpack.ui.controls.list.popupformatters.IPopupFormatter;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;

public class RoundedBorderFormatter implements IPopupFormatter {
    private Border border;

    public RoundedBorderFormatter(Border border) {
        this.border = border;
    }

    @Override
    public void formatPopup(BasicComboPopup popup) {
        popup.setBorder(new LineBorder(Color.white, 1));
        popup.setBorder(border);
    }
}
