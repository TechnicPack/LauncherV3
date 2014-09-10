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

package net.technicpack.launcher.ui.components.modpacks;

import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.modpacks.*;
import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.launchercore.modpacks.sources.NameFilterPackSource;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.packsources.SearchResultPackSource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.ui.controls.list.SimpleScrollbarUI;
import net.technicpack.launcher.ui.controls.modpacks.ModpackWidget;
import net.technicpack.launchercore.image.ImageRepository;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class ModpackSelector extends JPanel implements IModpackContainer, IAuthListener<IUserType> {
    private ResourceLoader resources;
    private PackLoader packLoader;
    private IPackSource technicSolder;
    private ImageRepository<ModpackModel> iconRepo;
    private IPlatformApi platformApi;

    private JPanel widgetList;
    private JScrollPane scrollPane;
    private ModpackInfoPanel modpackInfoPanel;
    private JTextField filterContents;

    private MemoryModpackContainer defaultPacks = new MemoryModpackContainer();
    private Map<String, ModpackWidget> allModpacks = new HashMap<String, ModpackWidget>();
    private ModpackWidget selectedWidget;
    private PackLoadJob currentLoadJob;

    private String lastFilterContents = "";

    public ModpackSelector(ResourceLoader resources, PackLoader packLoader, IPackSource techicSolder, IPlatformApi platformApi, ImageRepository<ModpackModel> iconRepo) {
        this.resources = resources;
        this.packLoader = packLoader;
        this.iconRepo = iconRepo;
        this.technicSolder = techicSolder;
        this.platformApi = platformApi;

        initComponents();
    }

    public void setInfoPanel(ModpackInfoPanel modpackInfoPanel) {
        this.modpackInfoPanel = modpackInfoPanel;
    }

    public ModpackModel getSelectedPack() {
        if (selectedWidget == null)
            return null;

        return selectedWidget.getModpack();
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

        filterContents = new JTextField();
        filterContents.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        filterContents.setBorder(BorderFactory.createEmptyBorder());
        filterContents.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                detectFilterChanges();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                detectFilterChanges();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                detectFilterChanges();
            }
        });

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

        scrollPane = new JScrollPane(widgetList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new SimpleScrollbarUI(LauncherFrame.COLOR_SCROLL_TRACK, LauncherFrame.COLOR_SCROLL_THUMB));
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
    public void replaceModpackInContainer(ModpackModel modpack) {
        if (allModpacks.containsKey(modpack.getName()))
            addModpackToContainer(modpack);
    }

    @Override
    public void addModpackToContainer(ModpackModel modpack) {
        final ModpackWidget widget = new ModpackWidget(resources, modpack, iconRepo.startImageJob(modpack));
        widget.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() instanceof ModpackWidget)
                    selectWidget((ModpackWidget)e.getSource());
            }
        });

        if (widget.getModpack().isSelected()) {
            selectedWidget = widget;
        }

        if (allModpacks.containsKey(modpack.getName()) && allModpacks.get(modpack.getName()).isSelected())
            selectedWidget = widget;

        allModpacks.put(modpack.getName(), widget);

        rebuildUI();

        if (selectedWidget != null) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (widget == selectedWidget)
                        selectWidget(widget);
                    else
                        selectedWidget.scrollRectToVisible(new Rectangle(selectedWidget.getSize()));
                }
            });
        }
    }

    @Override
    public void refreshComplete() {
        if (selectedWidget == null) {
            java.util.List<ModpackWidget> sortedPacks = new LinkedList<ModpackWidget>();
            sortedPacks.addAll(allModpacks.values());
            Collections.sort(sortedPacks, new Comparator<ModpackWidget>() {
                @Override
                public int compare(ModpackWidget o1, ModpackWidget o2) {
                    int priorityCompare = (new Integer(o2.getModpack().getPriority())).compareTo(new Integer(o1.getModpack().getPriority()));
                    if (priorityCompare != 0)
                        return priorityCompare;
                    else if (o1.getModpack().getDisplayName() == null && o2.getModpack().getDisplayName() == null)
                        return 0;
                    else if (o1.getModpack().getDisplayName() == null)
                        return -1;
                    else if (o2.getModpack().getDisplayName() == null)
                        return 1;
                    else
                        return o1.getModpack().getDisplayName().compareToIgnoreCase(o2.getModpack().getDisplayName());
                }
            });
            if (sortedPacks.size() > 0) {
                selectWidget(sortedPacks.get(0));
            }
        }
    }

    protected void selectWidget(ModpackWidget widget) {
        if (selectedWidget != null)
            selectedWidget.setIsSelected(false);
        selectedWidget = widget;
        selectedWidget.setIsSelected(true);
        selectedWidget.getModpack().select();
        selectedWidget.scrollRectToVisible(new Rectangle(selectedWidget.getSize()));

        if (modpackInfoPanel != null)
            modpackInfoPanel.setModpack(widget.getModpack());
    }

    protected void rebuildUI() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    rebuildUI();
                }
            });
            return;
        }

        GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0,0);

        java.util.List<ModpackWidget> sortedPacks = new LinkedList<ModpackWidget>();
        sortedPacks.addAll(allModpacks.values());
        Collections.sort(sortedPacks, new Comparator<ModpackWidget>() {
            @Override
            public int compare(ModpackWidget o1, ModpackWidget o2) {
                int priorityCompare = (new Integer(o2.getModpack().getPriority())).compareTo(new Integer(o1.getModpack().getPriority()));
                if (priorityCompare != 0)
                    return priorityCompare;
                else if (o1.getModpack().getDisplayName() == null && o2.getModpack().getDisplayName() == null)
                    return 0;
                else if (o1.getModpack().getDisplayName() == null)
                    return -1;
                else if (o2.getModpack().getDisplayName() == null)
                    return 1;
                else
                    return o1.getModpack().getDisplayName().compareToIgnoreCase(o2.getModpack().getDisplayName());
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
        repaint();
    }

    @Override
    public void userChanged(IUserType user) {
        if (filterContents.getText().length() > 0)
            filterContents.setText("");
        else
            detectFilterChanges();

        if (user != null) {
            ArrayList<IPackSource> sources = new ArrayList<IPackSource>(1);
            sources.add(technicSolder);
            defaultPacks.addPassthroughContainer(this);
            packLoader.createRepositoryLoadJob(defaultPacks, sources, null, true);
        }
    }

    protected void detectFilterChanges() {
        if (currentLoadJob != null) {
            currentLoadJob.cancel();
        }

        if (filterContents.getText().length() >= 3) {
            if (currentLoadJob == null || currentLoadJob.isCancelled()) {
                loadNewJob();
            } else {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        loadNewJob();
                    }
                });
            }
        } else if (lastFilterContents.length() >= 3) {
            clear();
            defaultPacks.addPassthroughContainer(this);
            for(ModpackModel modpack : defaultPacks.getModpacks()) {
                addModpackToContainer(modpack);
            }
        }

        lastFilterContents = filterContents.getText();
    }

    private void loadNewJob() {
        defaultPacks.removePassthroughContainer(this);

        ArrayList<IPackSource> sources = new ArrayList<IPackSource>(2);
        sources.add(new NameFilterPackSource(defaultPacks, filterContents.getText()));
        sources.add(new SearchResultPackSource(platformApi, filterContents.getText()));
        currentLoadJob = packLoader.createRepositoryLoadJob(this, sources, null, false);
    }
}
