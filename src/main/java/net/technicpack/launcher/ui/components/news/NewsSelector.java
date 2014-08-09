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
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.SimpleScrollbarUI;
import net.technicpack.launcher.ui.controls.TiledBackground;
import net.technicpack.launcher.ui.controls.feeds.NewsWidget;
import sun.tools.jar.resources.jar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NewsSelector extends JPanel {
    private ResourceLoader resources;
    private NewsWidget selectedItem;

    public NewsSelector(ResourceLoader resources) {
        this.resources = resources;

        initComponents();
    }

    protected void selectNewsItem(NewsWidget widget) {
        selectedItem.setIsSelected(false);
        selectedItem = widget;
        selectedItem.setIsSelected(true);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(LauncherFrame.COLOR_SELECTOR_BACK);

        JPanel widgetHost = new JPanel();
        widgetHost.setOpaque(false);
        widgetHost.setLayout(new GridBagLayout());

        JScrollPane scrollPane = new JScrollPane(widgetHost, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new SimpleScrollbarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10,10));
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        add(scrollPane, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints(0,0,1,1,1.0,0.0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);

//        for (int i = 0; i < 12; i++) {
//            NewsWidget widget = new NewsWidget(resources);
//            widget.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    if (e.getSource() instanceof NewsWidget)
//                        selectNewsItem((NewsWidget)e.getSource());
//                }
//            });
//
//            if (i == 2)
//                selectedItem = widget;
//            widgetHost.add(widget, constraints);
//            constraints.gridy++;
//        }

        constraints.weighty = 1.0;
        widgetHost.add(Box.createGlue(), constraints);
    }
}
