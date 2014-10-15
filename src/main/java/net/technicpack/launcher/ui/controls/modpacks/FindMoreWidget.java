/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
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

package net.technicpack.launcher.ui.controls.modpacks;

import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.SelectorWidget;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FindMoreWidget extends SelectorWidget {
    public FindMoreWidget(ResourceLoader resources) {
        super(resources);

        initComponents();
        setIsSelected(true);

        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(5,0,5,0));
        JLabel text = new JLabel(resources.getString("launcher.packselector.more"), JLabel.CENTER);
        text.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 14));
        text.setForeground(LauncherFrame.COLOR_DIM_TEXT);
        text.setOpaque(false);
        add(text, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    }
}
