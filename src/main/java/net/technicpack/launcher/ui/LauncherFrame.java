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

package net.technicpack.launcher.ui;

import net.technicpack.launcher.LauncherMain;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.ui.components.ModpackOptionsDialog;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.source.FileJavaSource;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.RestObject;
import net.technicpack.ui.controls.DraggableFrame;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.TintablePanel;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.components.OptionsDialog;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launcher.ui.components.news.NewsInfoPanel;
import net.technicpack.launcher.ui.components.news.NewsSelector;
import net.technicpack.launcher.ui.controls.*;
import net.technicpack.ui.controls.feeds.CountCircle;
import net.technicpack.ui.controls.installation.ProgressBar;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.install.Version;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class LauncherFrame extends DraggableFrame implements IRelocalizableResource, IAuthListener<MojangUser> {

    private static final int FRAME_WIDTH = 1194;
    private static final int FRAME_HEIGHT = 718;

    private static final int SIDEKICK_WIDTH = 300;
    private static final int SIDEKICK_HEIGHT = 250;

    public static final Color COLOR_RED = new Color(229,0,0);
    public static final Color COLOR_GREEN = new Color(90, 184, 96);
    public static final Color COLOR_BLUE = new Color(16, 108, 163);
    public static final Color COLOR_BLUE_DARKER = new Color(12, 94, 145);
    public static final Color COLOR_WHITE_TEXT = new Color(208,208,208);
    public static final Color COLOR_DIM_TEXT = new Color(160,160,160);
    public static final Color COLOR_HEADER_TEXT = new Color(255,255,255);
    public static final Color COLOR_CHARCOAL = new Color(31, 31, 31);
    public static final Color COLOR_BANNER = new Color(0, 0, 0, 160);
    public static final Color COLOR_PANEL = new Color(36, 38, 39);
    public static final Color COLOR_SCROLL_TRACK = new Color(18, 18, 18);
    public static final Color COLOR_SCROLL_THUMB = new Color(53, 53, 53);
    public static final Color COLOR_SELECTOR_BACK = new Color(22,26,29);
    public static final Color COLOR_FEED_BACK = new Color(22,26,29,200);
    public static final Color COLOR_CENTRAL_BACK = new Color(25, 30, 34, 160);
    public static final Color COLOR_CENTRAL_BACK_OPAQUE = new Color(25, 30, 34);
    public static final Color COLOR_FEEDITEM_BACK = new Color(37, 44, 49);
    public static final Color COLOR_LIKES_BACK = new Color(20, 65, 97);
    public static final Color COLOR_BUTTON_BLUE = new Color(43, 128, 195);
    public static final Color COLOR_FORMELEMENT_INTERNAL = new Color(30, 39, 46);
    public static final Color COLOR_GREY_TEXT = new Color(86, 98, 110);
    public static final Color COLOR_FOOTER = new Color(27, 32, 36);
    public static final Color COLOR_SERVER = new Color(91, 192, 222);

    public static final String TAB_DISCOVER = "discover";
    public static final String TAB_MODPACKS = "modpacks";
    public static final String TAB_NEWS = "news";

    private ResourceLoader resources;
    private final UserModel<MojangUser> userModel;
    private final ImageRepository<IUserType> skinRepository;
    private final TechnicSettings settings;
    private final ImageRepository<ModpackModel> iconRepo;
    private final ImageRepository<ModpackModel> logoRepo;
    private final ImageRepository<ModpackModel> backgroundRepo;
    private final ImageRepository<AuthorshipInfo> avatarRepo;
    private final Installer installer;
    private final IPlatformApi platformApi;
    private final LauncherDirectories directories;
    private final IInstalledPackRepository packRepo;
    private final StartupParameters params;
    private final JavaVersionRepository javaVersions;
    private final FileJavaSource fileJavaSource;

    private ModpackOptionsDialog modpackOptionsDialog = null;

    private HeaderTab discoverTab;
    private HeaderTab modpacksTab;
    private HeaderTab newsTab;

    private CardLayout infoLayout;
    private JPanel infoSwap;

    private UserWidget userWidget;
    private ProgressBar installProgress;
    private Component installProgressPlaceholder;
    private RoundedButton playButton;
    private ModpackSelector modpackSelector;
    private NewsSelector newsSelector;
    private TintablePanel centralPanel;
    private TintablePanel footer;

    private String currentTabName;

    NewsInfoPanel newsInfoPanel;
    ModpackInfoPanel modpackPanel;
    DiscoverInfoPanel discoverInfoPanel;

    public LauncherFrame(final ResourceLoader resources, final ImageRepository<IUserType> skinRepository, final UserModel userModel, final TechnicSettings settings, final ModpackSelector modpackSelector, final ImageRepository<ModpackModel> iconRepo, final ImageRepository<ModpackModel> logoRepo, final ImageRepository<ModpackModel> backgroundRepo, final Installer installer, final ImageRepository<AuthorshipInfo> avatarRepo, final IPlatformApi platformApi, final LauncherDirectories directories, final IInstalledPackRepository packRepository, final StartupParameters params, final DiscoverInfoPanel discoverInfoPanel, final JavaVersionRepository javaVersions, final FileJavaSource fileJavaSource) {
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.userModel = userModel;
        this.skinRepository = skinRepository;
        this.settings = settings;
        this.modpackSelector = modpackSelector;
        this.iconRepo = iconRepo;
        this.logoRepo = logoRepo;
        this.backgroundRepo = backgroundRepo;
        this.installer = installer;
        this.avatarRepo = avatarRepo;
        this.platformApi = platformApi;
        this.directories = directories;
        this.packRepo = packRepository;
        this.params = params;
        this.discoverInfoPanel = discoverInfoPanel;
        this.fileJavaSource = fileJavaSource;
        this.javaVersions = javaVersions;

        //Handles rebuilding the frame, so use it to build the frame in the first place
        relocalize(resources);

        selectTab("discover");

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                LauncherMain.consoleFrame.setVisible(settings.getShowConsole());
            }
        });

        setLocationRelativeTo(null);
    }

    /////////////////////////////////////////////////
    // Action responses
    /////////////////////////////////////////////////

    public void selectTab(String tabName) {
        discoverTab.setIsActive(false);
        modpacksTab.setIsActive(false);
        newsTab.setIsActive(false);

        if (tabName.equalsIgnoreCase(TAB_DISCOVER))
            discoverTab.setIsActive(true);
        else if (tabName.equalsIgnoreCase(TAB_MODPACKS))
            modpacksTab.setIsActive(true);
        else if (tabName.equalsIgnoreCase(TAB_NEWS)) {
            newsTab.setIsActive(true);
            newsSelector.ping();
        }

        infoLayout.show(infoSwap, tabName);

        currentTabName = tabName;
    }

    protected void closeWindow() {
        System.exit(0);
    }

    protected void minimizeWindow() { this.setState(Frame.ICONIFIED); }

    protected void logout() {
        if (installer.isCurrentlyRunning())
            return;

        userModel.setCurrentUser(null);
    }

    protected void launchModpack() {
        ModpackModel pack = modpackSelector.getSelectedPack();
        boolean requiresInstall = false;

        if (pack == null || (pack.getInstalledPack() == null && (pack.getPackInfo() == null || !pack.getPackInfo().isComplete())))
            return;

        if (pack.getInstalledDirectory() == null) {
            requiresInstall = true;
            pack.save();
            modpackSelector.forceRefresh();
        }

        if (requiresInstall) {
            try {
                if (pack.getPackInfo().shouldForceDirectory() && FilenameUtils.directoryContains(directories.getLauncherDirectory().getCanonicalPath(), pack.getInstalledDirectory().getCanonicalPath())) {
                    JFileChooser chooser = new JFileChooser(directories.getLauncherDirectory());
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setCurrentDirectory(directories.getLauncherDirectory());
                    int result = chooser.showOpenDialog(this);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();

                        if (file.list().length > 0) {
                            JOptionPane.showMessageDialog(this, resources.getString("modpackoptions.move.errortext"), resources.getString("modpackoptions.move.errortitle"), JOptionPane.WARNING_MESSAGE);
                            return;
                        } else if (FileUtils.directoryContains(directories.getLauncherDirectory(), file)) {
                            JOptionPane.showMessageDialog(this, resources.getString("launcher.launch.requiresmove"), resources.getString("launcher.launch.requiretitle"), JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        pack.setInstalledDirectory(file);
                    }
                }
            } catch (IOException ex) {
                Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        boolean forceInstall = false;
        Version installedVersion = pack.getInstalledVersion();

        //Force a full install (check cache, redownload, unzip files) if we have no current installation of this modpack
        if (installedVersion == null) {
            forceInstall = true;
            requiresInstall = true;
        } else if (pack.getBuild() != null && !pack.isLocalOnly()) {

            //Ask the user if they want to update to the newer version if:
            //1- the pack build is RECOMMENDED & the recommended version is diff from the installed version
            //2- the pack build is LATEST & the latest version is diff from the installed version
            //3- the pack build is neither LATEST or RECOMMENDED & the pack build is diff from the installed version
            boolean requestInstall = false;
            if (pack.getBuild().equalsIgnoreCase(InstalledPack.RECOMMENDED) && pack.getPackInfo().getRecommended() != null && !pack.getPackInfo().getRecommended().equalsIgnoreCase(installedVersion.getVersion()))
                requestInstall = true;
            else if (pack.getBuild().equalsIgnoreCase(InstalledPack.LATEST) && pack.getPackInfo().getLatest() != null && !pack.getPackInfo().getLatest().equalsIgnoreCase(installedVersion.getVersion()))
                requestInstall = true;
            else if (!pack.getBuild().equalsIgnoreCase(InstalledPack.RECOMMENDED) && !pack.getBuild().equalsIgnoreCase(InstalledPack.LATEST) && !pack.getBuild().equalsIgnoreCase(installedVersion.getVersion()))
                requestInstall = true;

            //If the user says yes, update, then force a full install
            if (requestInstall) {
    			int result = JOptionPane.showConfirmDialog(this, resources.getString("launcher.install.query"), resources.getString("launcher.install.query.title"), JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

    			if (result == JOptionPane.YES_OPTION) {
    				forceInstall = true;
    			}
            }
        }

        //If we're forcing an install, then derive the installation build from the pack build
        //otherwise, just use the installed version
        String installBuild = null;
        if (forceInstall && !pack.isLocalOnly()) {
            installBuild = pack.getBuild();

            if (installBuild.equalsIgnoreCase(InstalledPack.RECOMMENDED))
                installBuild = pack.getPackInfo().getRecommended();
            else if (installBuild.equalsIgnoreCase(InstalledPack.LATEST))
                installBuild = pack.getPackInfo().getLatest();
        } else if (installedVersion != null)
            installBuild = installedVersion.getVersion();

        if (requiresInstall && installBuild != null && !installBuild.isEmpty()) {
            installer.justInstall(resources, pack, installBuild, forceInstall, this, installProgress);
        } else {
            installer.installAndRun(resources, pack, installBuild, forceInstall, this, installProgress);
        }

        installProgress.setVisible(true);
        installProgressPlaceholder.setVisible(false);
        userChanged(userModel.getCurrentUser());
        invalidate();
    }

    public void launchCompleted() {
        if (installer.isCurrentlyRunning()) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    launchCompleted();
                }
            });
            return;
        }

        installProgress.setVisible(false);
        installProgressPlaceholder.setVisible(true);


        userModel.setCurrentUser(userModel.getCurrentUser());

        invalidate();
    }

    protected void openModpackOptions(ModpackModel model) {

        if (modpackOptionsDialog == null) {
            centralPanel.setTintActive(true);
            footer.setTintActive(true);
            modpackOptionsDialog = new ModpackOptionsDialog(this, directories, model, resources);
            modpackOptionsDialog.setVisible(true);
            modpackOptionsDialog = null;
            centralPanel.setTintActive(false);
            footer.setTintActive(false);
            modpackPanel.setModpack(model);
            modpackSelector.forceRefresh();
        }
    }

    protected void refreshModpackOptions(ModpackModel model) {
        if (modpackOptionsDialog != null)
            modpackOptionsDialog.refresh(model);
    }

    protected void openLauncherOptions() {
        centralPanel.setTintActive(true);
        footer.setTintActive(true);
        OptionsDialog dialog = new OptionsDialog(this, settings, resources, params, javaVersions, fileJavaSource);
        dialog.setVisible(true);
        centralPanel.setTintActive(false);
        footer.setTintActive(false);
    }

    /////////////////////////////////////////////////
    // End Action responses
    /////////////////////////////////////////////////

    private void initComponents() {
        BorderLayout layout = new BorderLayout();
        setLayout(layout);

        /////////////////////////////////////////////////////////////
        //HEADER
        /////////////////////////////////////////////////////////////
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
        header.setBackground(COLOR_BLUE);
        header.setForeground(COLOR_WHITE_TEXT);
        header.setBorder(BorderFactory.createEmptyBorder(0,5,0,10));
        this.add(header, BorderLayout.PAGE_START);

        ImageIcon headerIcon = resources.getIcon("platform_icon_title.png");
        JButton headerLabel = new JButton(headerIcon);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(5,8,5,0));
        headerLabel.setContentAreaFilled(false);
        headerLabel.setFocusPainted(false);
        headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerLabel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DesktopUtils.browseUrl("http://www.technicpack.net/");
            }
        });
        header.add(headerLabel);

        header.add(Box.createRigidArea(new Dimension(6, 0)));

        ActionListener tabListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTab(e.getActionCommand());
            }
        };

        discoverTab = new HeaderTab(resources.getString("launcher.title.discover"), resources);
        header.add(discoverTab);
        discoverTab.setActionCommand(TAB_DISCOVER);
        discoverTab.addActionListener(tabListener);

        modpacksTab = new HeaderTab(resources.getString("launcher.title.modpacks"), resources);
        modpacksTab.setIsActive(true);
        modpacksTab.setHorizontalTextPosition(SwingConstants.LEADING);
        modpacksTab.addActionListener(tabListener);
        modpacksTab.setActionCommand(TAB_MODPACKS);
        header.add(modpacksTab);

        newsTab = new HeaderTab(resources.getString("launcher.title.news"), resources);
        newsTab.setLayout(null);
        newsTab.addActionListener(tabListener);
        newsTab.setActionCommand(TAB_NEWS);
        header.add(newsTab);

        CountCircle newsCircle = new CountCircle();
        newsCircle.setBackground(COLOR_RED);
        newsCircle.setForeground(COLOR_WHITE_TEXT);
        newsCircle.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16, Font.BOLD));
        newsTab.add(newsCircle);
        newsCircle.setBounds(10,17,25,25);

        header.add(Box.createHorizontalGlue());

        JPanel rightHeaderPanel = new JPanel();
        rightHeaderPanel.setOpaque(false);
        rightHeaderPanel.setLayout(new BoxLayout(rightHeaderPanel, BoxLayout.PAGE_AXIS));
        rightHeaderPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel windowGadgetPanel = new JPanel();
        windowGadgetPanel.setOpaque(false);
        windowGadgetPanel.setLayout(new BoxLayout(windowGadgetPanel, BoxLayout.LINE_AXIS));
        windowGadgetPanel.setAlignmentX(RIGHT_ALIGNMENT);

        ImageIcon minimizeIcon = resources.getIcon("minimize.png");
        JButton minimizeButton = new JButton(minimizeIcon);
        minimizeButton.setBorder(BorderFactory.createEmptyBorder());
        minimizeButton.setContentAreaFilled(false);
        minimizeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        minimizeButton.setFocusable(false);
        minimizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                minimizeWindow();
            }
        });
        windowGadgetPanel.add(minimizeButton);

        ImageIcon closeIcon = resources.getIcon("close.png");
        JButton closeButton = new JButton(closeIcon);
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.setFocusable(false);
        windowGadgetPanel.add(closeButton);

        rightHeaderPanel.add(windowGadgetPanel);
        rightHeaderPanel.add(Box.createVerticalGlue());

        JButton launcherOptionsLabel = new JButton(resources.getString("launcher.title.options"));
        launcherOptionsLabel.setIcon(resources.getIcon("options_cog.png"));
        launcherOptionsLabel.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 14));
        launcherOptionsLabel.setForeground(COLOR_WHITE_TEXT);
        launcherOptionsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        launcherOptionsLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        launcherOptionsLabel.setAlignmentX(RIGHT_ALIGNMENT);
        launcherOptionsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        launcherOptionsLabel.setBorder(BorderFactory.createEmptyBorder());
        launcherOptionsLabel.setContentAreaFilled(false);
        launcherOptionsLabel.setFocusPainted(false);
        launcherOptionsLabel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openLauncherOptions();
            }
        });
        rightHeaderPanel.add(launcherOptionsLabel);

        header.add(rightHeaderPanel);

        /////////////////////////////////////////////////////////////
        // CENTRAL AREA
        /////////////////////////////////////////////////////////////
        centralPanel = new TintablePanel();
        centralPanel.setBackground(COLOR_CHARCOAL);
        centralPanel.setForeground(COLOR_WHITE_TEXT);
        centralPanel.setTintColor(COLOR_CENTRAL_BACK);
        this.add(centralPanel, BorderLayout.CENTER);
        centralPanel.setLayout(new BorderLayout());

        modpackPanel = new ModpackInfoPanel(resources, iconRepo, logoRepo, backgroundRepo, avatarRepo, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openModpackOptions((ModpackModel)e.getSource());
            }
        },
        new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshModpackOptions((ModpackModel)e.getSource());
            }
        }
        );
        modpackSelector.setInfoPanel(modpackPanel);
        modpackSelector.setLauncherFrame(this);
        playButton = modpackPanel.getPlayButton();
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() instanceof ModpackModel) {
                    setupPlayButtonText((ModpackModel) e.getSource(), userModel.getCurrentUser());
                } else if (installer.isCurrentlyRunning()) {
                    installer.cancel();
                    setupPlayButtonText(modpackSelector.getSelectedPack(), userModel.getCurrentUser());
                } else {
                    launchModpack();
                }
            }
        });

        modpackPanel.getDeleteButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(LauncherFrame.this, resources.getString("modpackoptions.delete.confirmtext"), resources.getString("modpackoptions.delete.confirmtitle"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    modpackSelector.getSelectedPack().delete();
                    modpackSelector.forceRefresh();
                }
            }
        });

        infoSwap = new JPanel();
        infoLayout = new CardLayout();
        infoSwap.setLayout(infoLayout);
        infoSwap.setOpaque(false);
        newsInfoPanel = new NewsInfoPanel(resources, avatarRepo);
        infoSwap.add(discoverInfoPanel,"discover");

        JPanel newsHost = new JPanel();
        infoSwap.add(newsHost, "news");
        JPanel modpackHost = new JPanel();
        infoSwap.add(modpackHost, "modpacks");
        centralPanel.add(infoSwap, BorderLayout.CENTER);

        newsSelector = new NewsSelector(resources, newsInfoPanel, platformApi, avatarRepo, newsCircle, settings);
        newsHost.setLayout(new BorderLayout());
        newsHost.add(newsInfoPanel, BorderLayout.CENTER);
        newsHost.add(newsSelector, BorderLayout.WEST);

        modpackHost.setLayout(new BorderLayout());
        modpackHost.add(modpackPanel, BorderLayout.CENTER);
        modpackHost.add(modpackSelector, BorderLayout.WEST);

        footer = new TintablePanel();
        footer.setTintColor(COLOR_CENTRAL_BACK);
        footer.setBackground(COLOR_FOOTER);
        footer.setLayout(new BoxLayout(footer, BoxLayout.LINE_AXIS));
        footer.setForeground(COLOR_WHITE_TEXT);
        footer.setBorder(BorderFactory.createEmptyBorder(3,6,3,12));

        userWidget = new UserWidget(resources, skinRepository);
        userWidget.setMaximumSize(userWidget.getPreferredSize());
        footer.add(userWidget);

        JLabel dashText = new JLabel("| ");
        dashText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        dashText.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        footer.add(dashText);

        JButton logout = new JButton(resources.getString("launcher.user.logout"));
        logout.setBorder(BorderFactory.createEmptyBorder());
        logout.setContentAreaFilled(false);
        logout.setFocusable(false);
        logout.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
        footer.add(logout);

        installProgress = new ProgressBar();
        installProgress.setForeground(Color.white);
        installProgress.setBackground(LauncherFrame.COLOR_GREEN);
        installProgress.setBorder(BorderFactory.createEmptyBorder(5, 45, 4, 45));
        installProgress.setIcon(resources.getIcon("download_icon.png"));
        installProgress.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        installProgress.setVisible(false);
        footer.add(installProgress);

        installProgressPlaceholder = Box.createHorizontalGlue();
        footer.add(installProgressPlaceholder);

        JLabel buildCtrl = new JLabel(resources.getString("launcher.build.text", resources.getLauncherBuild(), resources.getString("launcher.build." + settings.getBuildStream())));
        buildCtrl.setForeground(COLOR_WHITE_TEXT);
        buildCtrl.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        buildCtrl.setHorizontalTextPosition(SwingConstants.RIGHT);
        buildCtrl.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(buildCtrl);

        this.add(footer, BorderLayout.PAGE_END);
    }

    @Override
    public void relocalize(ResourceLoader loader) {
        this.resources = loader;
        this.resources.registerResource(this);

        setIconImage(this.resources.getImage("icon.png"));

        //Wipe controls
        this.getContentPane().removeAll();
        this.setLayout(null);

        //Clear references to existing controls

        initComponents();
        userChanged(userModel.getCurrentUser());

        if (currentTabName != null)
            selectTab(currentTabName);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                invalidate();
                repaint();
            }
        });
    }

    @Override
    public void userChanged(MojangUser mojangUser) {
        if (mojangUser == null)
            this.setVisible(false);
        else {
            this.setVisible(true);
            userWidget.setUser(mojangUser);

            if (modpackSelector.getSelectedPack() != null)
                setupPlayButtonText(modpackSelector.getSelectedPack(), mojangUser);

            modpackSelector.forceRefresh();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    repaint();
                }
            });
        }
    }

    public void setupPlayButtonText(ModpackModel modpack, MojangUser user) {
        playButton.setEnabled(true);
        playButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);

        if (installer.isCurrentlyRunning()) {
            playButton.setText(resources.getString("launcher.pack.cancel"));
        } else if (modpack.getInstalledVersion() != null) {
            if (userModel.getCurrentUser() == null || userModel.getCurrentUser().isOffline()) {
                playButton.setText(resources.getString("launcher.pack.launch.offline"));
            } else {
                playButton.setText(resources.getString("launcher.pack.launch"));
            }
            playButton.setIcon(new ImageIcon(resources.colorImage(resources.getImage("play_button.png"), LauncherFrame.COLOR_BUTTON_BLUE)));
            playButton.setHoverIcon(new ImageIcon(resources.colorImage(resources.getImage("play_button.png"), LauncherFrame.COLOR_BLUE)));
        } else {
            if (userModel.getCurrentUser() == null || userModel.getCurrentUser().isOffline()) {
                playButton.setEnabled(false);
                playButton.setForeground(LauncherFrame.COLOR_GREY_TEXT);
                playButton.setText(resources.getString("launcher.pack.cannotinstall"));
            } else {
                playButton.setText(resources.getString("launcher.pack.install"));
            }
            playButton.setIcon(new ImageIcon(resources.colorImage(resources.getImage("download_button.png"), LauncherFrame.COLOR_BUTTON_BLUE)));
            playButton.setHoverIcon(new ImageIcon(resources.colorImage(resources.getImage("download_button.png"), LauncherFrame.COLOR_BLUE)));
        }
    }
}
