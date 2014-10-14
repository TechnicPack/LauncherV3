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
import java.awt.*;

public class FindMoreWidget extends SelectorWidget {
    public FindMoreWidget(ResourceLoader resources) {
        super(resources);

        setBorder(BorderFactory.createEmptyBorder(4, 20, 4, 8));
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBackground(LauncherFrame.COLOR_CENTRAL_BACK);
        setLayout(new GridBagLayout());

        JLabel icon = new JLabel();
        icon.setIcon(new ImageIcon(ImageUtils.scaleWithAspectWidth(resources.getImage("icon.png"), 32)));
        add(icon, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,14),0,0));

        JLabel text = new JLabel(resources.getString("launcher.packselector.more"));
        text.setFont(getResources().getFont(ResourceLoader.FONT_OPENSANS, 14));
        text.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        text.setMaximumSize(new Dimension(210, text.getPreferredSize().height));
        text.setOpaque(false);
        add(text, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
    }
}
