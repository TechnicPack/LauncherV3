/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.technicpack.launcher.ui.components.news;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.controls.TiledBackground;
import net.technicpack.launcher.ui.controls.feeds.NewsWidget;

import javax.swing.*;
import java.awt.*;

public class NewsSelector extends TiledBackground {
    private ResourceLoader resources;

    public NewsSelector(ResourceLoader resources) {
        super(resources.getImage("background_repeat.png"));
        this.resources = resources;

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints(0,0,1,1,1.0,0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);

        for (int i = 0; i < 5; i++) {
            NewsWidget widget = new NewsWidget(resources);

            if (i == 2)
                widget.setIsSelected(true);
            add(widget, constraints);
            constraints.gridy++;
        }

        constraints.weighty = 1.0;
        add(Box.createGlue(), constraints);
    }
}
