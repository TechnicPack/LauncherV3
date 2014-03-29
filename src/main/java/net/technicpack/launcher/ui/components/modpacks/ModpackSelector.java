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
package net.technicpack.launcher.ui.components.modpacks;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.SimpleScrollbarUI;
import net.technicpack.launcher.ui.controls.modpacks.ModpackWidget;
import net.technicpack.launchercore.modpacks.AvailablePackList;
import net.technicpack.launchercore.modpacks.IModpackContainer;
import net.technicpack.launchercore.modpacks.ModpackModel;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ModpackSelector extends JPanel implements IModpackContainer {
    private ResourceLoader resources;
    private AvailablePackList packList;

    private JPanel widgetList;

    private Map<String, ModpackWidget> allModpacks = new HashMap<String, ModpackWidget>();
    private ModpackWidget selectedWidget;

    public ModpackSelector(ResourceLoader resources, AvailablePackList packList) {
        this.resources = resources;
        this.packList = packList;

        initComponents();

        packList.addRegisteredContainer(this);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(LauncherFrame.COLOR_SELECTOR_BACK);

        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(9,8,8,8));
        header.setBackground(LauncherFrame.COLOR_PANEL);
        add(header, BorderLayout.PAGE_START);

        JLabel filterLabel = new JLabel(resources.getString("launcher.packselector.filter"));
        filterLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS,16));
        filterLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        header.add(filterLabel, constraints);

        JTextField filterContents = new JTextField();
        filterContents.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        filterContents.setBorder(BorderFactory.createEmptyBorder());

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(0, 8, 0, 0);
        constraints.fill = GridBagConstraints.BOTH;
        header.add(filterContents, constraints);

        widgetList = new JPanel();
        widgetList.setOpaque(false);
        widgetList.setLayout(new GridBagLayout());

        JScrollPane scrollPane = new JScrollPane(widgetList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new SimpleScrollbarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        add(scrollPane, BorderLayout.CENTER);

        constraints = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0);
        widgetList.add(Box.createGlue(), constraints);
    }

    @Override
    public void clear() {
        allModpacks.clear();
        rebuildUI();
    }

    @Override
    public void addOrReplace(ModpackModel modpack) {
        allModpacks.put(modpack.getName(), new ModpackWidget(resources, modpack));
        rebuildUI();
    }

    protected void rebuildUI() {
        GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0,0);

        java.util.List<ModpackWidget> sortedPacks = new LinkedList<ModpackWidget>();
        sortedPacks.addAll(allModpacks.values());
        Collections.sort(sortedPacks, new Comparator<ModpackWidget>() {
            @Override
            public int compare(ModpackWidget o1, ModpackWidget o2) {
                int platformCompare = Boolean.valueOf(o1.getModpack().isPlatform()).compareTo(Boolean.valueOf(o2.getModpack().isPlatform()));

                if (platformCompare != 0)
                    return platformCompare;
                else if (o1.getModpack().getDisplayName() == null && o2.getModpack().getDisplayName() == null)
                    return 0;
                else if (o1.getModpack().getDisplayName() == null)
                    return -1;
                else if (o2.getModpack().getDisplayName() == null)
                    return 1;
                else
                    return o1.getModpack().getDisplayName().compareTo(o2.getModpack().getDisplayName());
            }
        });

        widgetList.removeAll();

        for(ModpackWidget sortedPack : sortedPacks) {
            widgetList.add(sortedPack, constraints);
            constraints.gridy++;
        }

        constraints.weighty = 1.0;
        widgetList.add(Box.createGlue(), constraints);
        revalidate();
    }
}
