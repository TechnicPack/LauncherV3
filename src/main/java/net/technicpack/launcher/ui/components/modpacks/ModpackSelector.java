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

package net.technicpack.launcher.ui.components.modpacks;

import net.technicpack.contrib.romainguy.AbstractFilter;
import net.technicpack.launcher.ui.controls.modpacks.FindMoreWidget;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.modpacks.*;
import net.technicpack.launchercore.modpacks.packinfo.CombinedPackInfo;
import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.launchercore.modpacks.sources.NameFilterPackSource;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.IPlatformSearchApi;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.platform.packsources.SearchResultPackSource;
import net.technicpack.platform.packsources.SinglePlatformSource;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.ui.controls.TintablePanel;
import net.technicpack.ui.controls.WatermarkTextField;
import net.technicpack.ui.controls.borders.RoundBorder;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.ui.controls.list.SimpleScrollbarUI;
import net.technicpack.launcher.ui.controls.modpacks.ModpackWidget;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.DesktopUtils;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModpackSelector extends TintablePanel implements IModpackContainer, IAuthListener<IUserType>, IRelocalizableResource {
    private ResourceLoader resources;
    private PackLoader packLoader;
    private IPackSource technicSolder;
    private ImageRepository<ModpackModel> iconRepo;
    private final IPlatformApi platformApi;
    private final IPlatformSearchApi platformSearchApi;
    private final ISolderApi solderApi;

    private JPanel widgetList;
    private JScrollPane scrollPane;
    private ModpackInfoPanel modpackInfoPanel;
    private LauncherFrame launcherFrame;
    private JTextField filterContents;
    private FindMoreWidget findMoreWidget;

    private MemoryModpackContainer defaultPacks = new MemoryModpackContainer();
    private Map<String, ModpackWidget> allModpacks = new HashMap<String, ModpackWidget>();
    private ModpackWidget selectedWidget;
    private PackLoadJob currentLoadJob;
    private Timer currentSearchTimer;

    private Pattern slugRegex;
    private Pattern siteRegex;

    private String lastFilterContents = "";

    private String findMoreUrl;

    private static final int MAX_SEARCH_STRING = 90;

    public ModpackSelector(ResourceLoader resources, PackLoader packLoader, IPackSource techicSolder, ISolderApi solderApi, IPlatformApi platformApi, IPlatformSearchApi platformSearchApi, ImageRepository<ModpackModel> iconRepo) {
        this.resources = resources;
        this.packLoader = packLoader;
        this.iconRepo = iconRepo;
        this.technicSolder = techicSolder;
        this.platformApi = platformApi;
        this.solderApi = solderApi;
        this.platformSearchApi = platformSearchApi;

        slugRegex = Pattern.compile("^[a-zA-Z0-9-]+$");
        siteRegex = Pattern.compile("^([a-zA-Z0-9-]+)\\.\\d+$");

        findMoreWidget = new FindMoreWidget(resources);
        findMoreWidget.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DesktopUtils.browseUrl(findMoreUrl);
            }
        });

        relocalize(resources);
    }

    public void setInfoPanel(ModpackInfoPanel modpackInfoPanel) {
        this.modpackInfoPanel = modpackInfoPanel;
    }

    public void setLauncherFrame(LauncherFrame launcherFrame) {
        this.launcherFrame = launcherFrame;
    }

    public ModpackModel getSelectedPack() {
        if (selectedWidget == null)
            return null;

        return selectedWidget.getModpack();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        setMaximumSize(new Dimension(287, getMaximumSize().height));

        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(4,8,4,4));
        header.setBackground(LauncherFrame.COLOR_SELECTOR_OPTION);
        add(header, BorderLayout.PAGE_START);

        filterContents = new WatermarkTextField(resources.getString("launcher.packselector.filter.hotfix"), LauncherFrame.COLOR_BLUE_DARKER);
        filterContents.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        filterContents.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        filterContents.setForeground(LauncherFrame.COLOR_BLUE);
        filterContents.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        filterContents.setSelectedTextColor(Color.black);
        filterContents.setSelectionColor(LauncherFrame.COLOR_BUTTON_BLUE);
        filterContents.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        filterContents.setColumns(20);
        ((AbstractDocument)filterContents.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException
            {
                if(fb.getDocument().getLength() + string.length() <= MAX_SEARCH_STRING)
                {
                    fb.insertString(offset, string, attr);
                }
            }

            @Override
            public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException
            {
                fb.remove(offset, length);
            }

            @Override
            public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs)throws BadLocationException
            {
                int finalTextLength = (fb.getDocument().getLength() - length) + text.length();
                if (finalTextLength > MAX_SEARCH_STRING)
                    text = text.substring(0, text.length() - (finalTextLength-MAX_SEARCH_STRING));
                fb.replace(offset, length, text, attrs);
            }
        });
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
        header.add(filterContents, new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(3,0,3,0), 0, 12));

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

        widgetList.add(Box.createHorizontalStrut(294), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0),0,0));
        widgetList.add(Box.createGlue(), new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0));
    }

    @Override
    public void clear() {
        allModpacks.clear();
        rebuildUI();
    }

    @Override
    public void replaceModpackInContainer(ModpackModel modpack) {
        if (allModpacks.containsKey(modpack.getName()))
            addModpackInternal(modpack);
    }

    @Override
    public void addModpackToContainer(ModpackModel modpack) {
        setTintActive(true);
        addModpackInternal(modpack);
    }

    protected void addModpackInternal(ModpackModel modpack) {
        final ModpackWidget widget = new ModpackWidget(resources, modpack, iconRepo.startImageJob(modpack));

        if (modpack.hasRecommendedUpdate()) {
            widget.setToolTipText(resources.getString("launcher.packselector.updatetip"));
        }
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
        setTintActive(false);

        if (findMoreWidget.getWidgetData().equals(resources.getString("launcher.packselector.api"))) {
            if (allModpacks.size() == 0) {
                findMoreWidget.setWidgetData(resources.getString("launcher.packselector.badapi"));
                findMoreUrl = "http://www.technicpack.net/";
            } else {
                for(ModpackWidget widget : allModpacks.values()) {
                    findMoreUrl = widget.getModpack().getWebSite();
                    break;
                }
            }
        }

        if (selectedWidget == null || selectedWidget.getModpack() == null || !allModpacks.containsKey(selectedWidget.getModpack().getName())) {
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

            final ModpackWidget refreshWidget = selectedWidget;
            Thread thread = new Thread("Modpack redownload " + selectedWidget.getModpack().getDisplayName()) {
                @Override
                public void run() {
                    try {
                        PlatformPackInfo updatedInfo = platformApi.getPlatformPackInfo(refreshWidget.getModpack().getName());
                        PackInfo infoToUse = updatedInfo;

                        if (updatedInfo != null && updatedInfo.hasSolder()) {
                            try {
                                ISolderPackApi solderPack = solderApi.getSolderPack(updatedInfo.getSolder(), updatedInfo.getName(), solderApi.getMirrorUrl(updatedInfo.getSolder()));
                                infoToUse = new CombinedPackInfo(solderPack.getPackInfo(), updatedInfo);
                            } catch (RestfulAPIException ex) {
                            }
                        }

                        if (infoToUse != null)
                            refreshWidget.getModpack().setPackInfo(infoToUse);

                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (modpackInfoPanel != null)
                                    modpackInfoPanel.setModpackIfSame(refreshWidget.getModpack());

                                if (refreshWidget.getModpack().hasRecommendedUpdate()) {
                                    refreshWidget.setToolTipText(resources.getString("launcher.packselector.updatetip"));
                                } else {
                                    refreshWidget.setToolTipText(null);
                                }

                                iconRepo.refreshRetry(refreshWidget.getModpack());
                                refreshWidget.updateFromPack(iconRepo.startImageJob(refreshWidget.getModpack()));

                                EventQueue.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        revalidate();
                                        repaint();
                                    }
                                });
                            }
                        });

                    } catch (RestfulAPIException ex) {
                        ex.printStackTrace();
                        return;
                    }
                }
            };
            thread.start();
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

        if (filterContents.getText().length() >= 3) {
            widgetList.add(findMoreWidget, constraints);
        }

        widgetList.add(Box.createHorizontalStrut(294), constraints);
        constraints.gridy++;

        constraints.weighty = 1.0;
        widgetList.add(Box.createGlue(), constraints);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                revalidate();
                repaint();
            }
        });
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

    public void forceRefresh() {
        lastFilterContents = "THIS IS A TERRIBLE HACK I'M BASICALLY FORCING A REFRESH BUT WITHOUT DOING ANY WORK";
        defaultPacks.clear();
        detectFilterChanges();
        ArrayList<IPackSource> sources = new ArrayList<IPackSource>(1);
        sources.add(technicSolder);
        packLoader.createRepositoryLoadJob(defaultPacks, sources, null, true);
    }

    public void setFilter(String text) {
        filterContents.setText(text);
        detectFilterChanges();

        if (this.launcherFrame != null)
            this.launcherFrame.selectTab("modpacks");
    }

    protected void detectFilterChanges() {
        cancelJob();

        if (filterContents.getText().length() >= 3) {
            loadNewJob(filterContents.getText());
        } else if (lastFilterContents.length() >= 3) {
            clear();
            defaultPacks.addPassthroughContainer(this);
            for(ModpackModel modpack : defaultPacks.getModpacks()) {
                addModpackToContainer(modpack);
            }
            refreshComplete();
        }

        lastFilterContents = filterContents.getText();
    }

    protected boolean isEncodedSlug(String slug) {
        try {
            URLEncoder.encode(URLDecoder.decode(slug, "UTF-8"), "UTF-8").equals(slug);
            return true;
        } catch (UnsupportedEncodingException ex) {
            return false;
        } catch (RuntimeException ex) {
            //Apparently the encoder/decoder indicate bad input by THROWING RUNTIME EXCEPTIONS
            //<3 java
            return false;
        }
    }

    private void loadNewJob(final String searchText) {
        setTintActive(true);
        defaultPacks.removePassthroughContainer(this);

        currentSearchTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String localSearchTag = searchText;

                String localSearchUrl = searchText;
                if (!localSearchUrl.startsWith("http://") && !localSearchUrl.startsWith("https://"))
                    localSearchUrl = "http://" + localSearchTag;

                try {
                    URI uri = new URI(localSearchUrl);
                    String host = uri.getHost();
                    String scheme = uri.getScheme();
                    if (host != null && scheme != null && (scheme.equals("http") || scheme.equals("https")) && (host.equals("www.technicpack.net") || host.equals("technicpack.net") || host.equals("api.technicpack.net"))) {
                        String path = uri.getPath();
                        if (path.startsWith("/"))
                            path = path.substring(1);
                        if (path.endsWith("/"))
                            path = path.substring(0, path.length()-1);
                        String[] fragments = path.split("/");

                        if ((fragments.length == 2 && fragments[0].equals("modpack")) || (fragments.length == 3 && fragments[0].equals("api") && fragments[1].equals("modpack"))) {
                            String slug = fragments[fragments.length-1];

                            Matcher siteMatcher = siteRegex.matcher(slug);
                            if (siteMatcher.find()) {
                                slug = siteMatcher.group(1);
                            }

                            Matcher slugMatcher = slugRegex.matcher(slug);
                            if (slugMatcher.find()) {
                                findMoreUrl = localSearchUrl;
                                findMoreWidget.setWidgetData(resources.getString("launcher.packselector.api"));
                                ArrayList<IPackSource> source = new ArrayList<IPackSource>(1);
                                source.add(new SinglePlatformSource(platformApi, solderApi, slug));
                                currentLoadJob = packLoader.createRepositoryLoadJob(ModpackSelector.this, source, null, false);
                                return;
                            }
                        }
                    }
                } catch (URISyntaxException ex) {
                    //It wasn't a valid URI which is actually fine.
                }

                String encodedSearch = filterContents.getText();
                try {
                    encodedSearch = URLEncoder.encode(encodedSearch, "UTF-8");
                } catch (UnsupportedEncodingException ex) {}
                findMoreUrl = "http://www.technicpack.net/search/modpacks?q="+encodedSearch;
                findMoreWidget.setWidgetData(resources.getString("launcher.packselector.more"));

                ArrayList<IPackSource> sources = new ArrayList<IPackSource>(2);
                sources.add(new NameFilterPackSource(defaultPacks, localSearchTag));
                sources.add(new SearchResultPackSource(platformSearchApi, localSearchTag));
                currentLoadJob = packLoader.createRepositoryLoadJob(ModpackSelector.this, sources, null, false);
            }
        });
        currentSearchTimer.setRepeats(false);
        currentSearchTimer.start();
    }

    private void cancelJob() {
        if (currentLoadJob != null)
            currentLoadJob.cancel();

        if (currentSearchTimer != null) {
            currentSearchTimer.stop();
        }
    }

    @Override
    public void relocalize(ResourceLoader loader) {
        this.resources = loader;
        this.resources.registerResource(this);

        this.setOverIcon(resources.getIcon("loader.gif"));
        this.setTintActive(true);

        //Wipe controls
        removeAll();
        this.setLayout(null);

        initComponents();

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                invalidate();
                repaint();
            }
        });
    }
}
