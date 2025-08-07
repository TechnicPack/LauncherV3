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

import net.technicpack.autoupdate.IBuildNumber;
import net.technicpack.discord.IDiscordApi;
import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.components.ModpackOptionsDialog;
import net.technicpack.launcher.ui.components.OptionsDialog;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launcher.ui.components.news.NewsInfoPanel;
import net.technicpack.launcher.ui.components.news.NewsSelector;
import net.technicpack.launcher.ui.controls.HeaderTab;
import net.technicpack.launcher.ui.controls.UserWidget;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.install.ModpackVersion;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.source.FileJavaSource;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.ui.controls.DraggableFrame;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.SplatPane;
import net.technicpack.ui.controls.TintablePanel;
import net.technicpack.ui.controls.feeds.CountCircle;
import net.technicpack.ui.controls.installation.ProgressBar;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.DesktopUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherFrame extends DraggableFrame implements IRelocalizableResource, IAuthListener {

    public static final String TAB_DISCOVER = "discover";
    public static final String TAB_MODPACKS = "modpacks";
    public static final String TAB_NEWS = "news";

    private static final int FRAME_WIDTH = 1194;
    private static final int FRAME_HEIGHT = 718;

    private ResourceLoader resources;
    private final UserModel userModel;
    private final ImageRepository<IUserType> skinRepository;
    private final TechnicSettings settings;
    private final ImageRepository<ModpackModel> iconRepo;
    private final ImageRepository<ModpackModel> logoRepo;
    private final ImageRepository<ModpackModel> backgroundRepo;
    private final ImageRepository<AuthorshipInfo> avatarRepo;
    private final Installer installer;
    private final IPlatformApi platformApi;
    private final LauncherFileSystem fileSystem;
    private final InstalledPackStore packRepo;
    private final StartupParameters params;
    private final JavaVersionRepository javaVersions;
    private final FileJavaSource fileJavaSource;
    private final IBuildNumber buildNumber;
    private final IDiscordApi discordApi;
    private final AtomicBoolean launchCompletedRequested = new AtomicBoolean(false);

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

    public LauncherFrame(final ResourceLoader resources, final ImageRepository<IUserType> skinRepository, final UserModel userModel, final TechnicSettings settings, final ModpackSelector modpackSelector, final ImageRepository<ModpackModel> iconRepo, final ImageRepository<ModpackModel> logoRepo, final ImageRepository<ModpackModel> backgroundRepo, final Installer installer, final ImageRepository<AuthorshipInfo> avatarRepo, final IPlatformApi platformApi, final LauncherFileSystem fileSystem, final InstalledPackStore packStore, final StartupParameters params, final DiscoverInfoPanel discoverInfoPanel, final JavaVersionRepository javaVersions, final FileJavaSource fileJavaSource, final IBuildNumber buildNumber, final IDiscordApi discordApi) {
        super();
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Technic Launcher");

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
        this.fileSystem = fileSystem;
        this.packRepo = packStore;
        this.params = params;
        this.discoverInfoPanel = discoverInfoPanel;
        this.fileJavaSource = fileJavaSource;
        this.javaVersions = javaVersions;
        this.buildNumber = buildNumber;
        this.discordApi = discordApi;

        //Handles rebuilding the frame, so use it to build the frame in the first place
        relocalize(resources);

        selectTab(TAB_DISCOVER);

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

        boolean forceInstall = false;
        ModpackVersion installedVersion = pack.getInstalledVersion();

        //Force a full install (check cache, redownload, unzip files) if we have no current installation of this modpack
        if (installedVersion == null) {
            forceInstall = true;
            requiresInstall = true;
        } else if (pack.getBuild() != null && !pack.isLocalOnly()) {
            //Ask the user if they want to update to the newer version if:
            //1- the pack build is RECOMMENDED & the recommended version is diff from the installed version
            //2- the pack build is LATEST & the latest version is diff from the installed version
            //3- the pack build is neither LATEST nor RECOMMENDED & the pack build is diff from the installed version
            boolean requestInstall = shouldRequestInstall(pack, installedVersion);

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
        String installBuild = getInstallBuild(forceInstall, pack, installedVersion);

        if (requiresInstall) {
            installer.justInstall(resources, pack, installBuild, forceInstall, this, installProgress);
        } else {
            installer.installAndRun(resources, pack, installBuild, forceInstall, this, installProgress);
        }

        installProgress.stateChanged("Initializing...", 0);
        installProgress.setVisible(true);
        installProgressPlaceholder.setVisible(false);
        userChanged(userModel.getCurrentUser());
        invalidate();
    }

    private static boolean shouldRequestInstall(ModpackModel pack, ModpackVersion installedVersion) {
        final String installedBuild = installedVersion.getVersion();

        final String wantedBuild = pack.getBuild();
        final boolean wantsRecommended = wantedBuild.equalsIgnoreCase(InstalledPack.RECOMMENDED);
        final boolean wantsLatest = wantedBuild.equalsIgnoreCase(InstalledPack.LATEST);

        final PackInfo packInfo = pack.getPackInfo();
        final String recommendedBuild = packInfo.getRecommended();
        final String latestBuild = packInfo.getLatest();

        return (wantsRecommended && recommendedBuild != null && !recommendedBuild.equalsIgnoreCase(installedBuild))
                || (wantsLatest && latestBuild != null && !latestBuild.equalsIgnoreCase(installedBuild))
                || (!wantsRecommended && !wantsLatest && !wantedBuild.equalsIgnoreCase(installedBuild));
    }

    private static String getInstallBuild(boolean forceInstall, ModpackModel pack, ModpackVersion installedVersion) {
        String installBuild = null;
        if (forceInstall && !pack.isLocalOnly()) {
            installBuild = pack.getBuild();

            if (installBuild.equalsIgnoreCase(InstalledPack.RECOMMENDED))
                installBuild = pack.getPackInfo().getRecommended();
            else if (installBuild.equalsIgnoreCase(InstalledPack.LATEST))
                installBuild = pack.getPackInfo().getLatest();
        } else if (installedVersion != null)
            installBuild = installedVersion.getVersion();
        return installBuild;
    }

    /**
     * Called when the installation is complete, either successfully or not. This will hide the progress bar.
     */
    public void launchCompleted() {
        // Ensure that this method is only called once, no matter how many times it's requested
        if (!launchCompletedRequested.compareAndSet(false, true)) {
            return;
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::launchCompleted);
            return;
        }

        try {
            installProgress.setVisible(false);
            installProgressPlaceholder.setVisible(true);

            userModel.setCurrentUser(userModel.getCurrentUser());

            invalidate();
        } finally {
            launchCompletedRequested.set(false);
        }
    }

    protected void openModpackOptions(ModpackModel model) {

        if (modpackOptionsDialog == null) {
            centralPanel.setTintActive(true);
            footer.setTintActive(true);
            modpackOptionsDialog = new ModpackOptionsDialog(this, fileSystem, model, resources);
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
        OptionsDialog dialog = new OptionsDialog(this, settings, resources, params, javaVersions, fileJavaSource, buildNumber);
        dialog.setVisible(true);
        centralPanel.setTintActive(false);
        footer.setTintActive(false);
    }

    /////////////////////////////////////////////////
    // End Action responses
    /////////////////////////////////////////////////

    private void initComponents() {
        BorderLayout layout = new BorderLayout();
        getRootPane().getContentPane().setLayout(layout);

        /////////////////////////////////////////////////////////////
        //HEADER
        /////////////////////////////////////////////////////////////
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
        header.setBackground(UIConstants.COLOR_BLUE);
        header.setForeground(UIConstants.COLOR_WHITE_TEXT);
        header.setBorder(BorderFactory.createEmptyBorder(0,5,0,10));
        getRootPane().getContentPane().add(header, BorderLayout.PAGE_START);

        ImageIcon headerIcon = resources.getIcon("platform_icon_title.png");
        JButton headerLabel = new JButton(headerIcon);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(5,8,5,0));
        headerLabel.setContentAreaFilled(false);
        headerLabel.setFocusPainted(false);
        headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerLabel.addActionListener(e -> DesktopUtils.browseUrl("https://www.technicpack.net/"));
        header.add(headerLabel);

        header.add(Box.createRigidArea(new Dimension(6, 0)));

        ActionListener tabListener = e -> selectTab(e.getActionCommand());

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
        newsCircle.setBackground(UIConstants.COLOR_RED);
        newsCircle.setForeground(UIConstants.COLOR_WHITE_TEXT);
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
        minimizeButton.addActionListener(e -> minimizeWindow());
        windowGadgetPanel.add(minimizeButton);

        ImageIcon closeIcon = resources.getIcon("close.png");
        JButton closeButton = new JButton(closeIcon);
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(e -> closeWindow());
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.setFocusable(false);
        windowGadgetPanel.add(closeButton);

        rightHeaderPanel.add(windowGadgetPanel);
        rightHeaderPanel.add(Box.createVerticalGlue());

        JButton launcherOptionsLabel = new JButton(resources.getString("launcher.title.options"));
        launcherOptionsLabel.setIcon(resources.getIcon("options_cog.png"));
        launcherOptionsLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        launcherOptionsLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        launcherOptionsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        launcherOptionsLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        launcherOptionsLabel.setAlignmentX(RIGHT_ALIGNMENT);
        launcherOptionsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        launcherOptionsLabel.setBorder(BorderFactory.createEmptyBorder());
        launcherOptionsLabel.setContentAreaFilled(false);
        launcherOptionsLabel.setFocusPainted(false);
        launcherOptionsLabel.addActionListener(e -> openLauncherOptions());
        rightHeaderPanel.add(launcherOptionsLabel);

        header.add(rightHeaderPanel);

        /////////////////////////////////////////////////////////////
        // CENTRAL AREA
        /////////////////////////////////////////////////////////////
        centralPanel = new TintablePanel();
        centralPanel.setBackground(UIConstants.COLOR_CHARCOAL);
        centralPanel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        centralPanel.setTintColor(UIConstants.COLOR_CENTRAL_BACK);
        getRootPane().getContentPane().add(centralPanel, BorderLayout.CENTER);
        centralPanel.setLayout(new BorderLayout());

        modpackPanel = new ModpackInfoPanel(resources, iconRepo, logoRepo, backgroundRepo, avatarRepo, discordApi, e -> openModpackOptions((ModpackModel)e.getSource()),
                e -> refreshModpackOptions((ModpackModel)e.getSource())
        );
        modpackSelector.setInfoPanel(modpackPanel);
        modpackSelector.setLauncherFrame(this);
        playButton = modpackPanel.getPlayButton();
        playButton.addActionListener(e -> {
            if (e.getSource() instanceof ModpackModel) {
                setupPlayButtonText((ModpackModel) e.getSource(), userModel.getCurrentUser());
            } else if (installer.isCurrentlyRunning()) {
                installer.cancel();
                setupPlayButtonText(modpackSelector.getSelectedPack(), userModel.getCurrentUser());
            } else {
                launchModpack();
            }
        });

        modpackPanel.getDeleteButton().addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(LauncherFrame.this, resources.getString("modpackoptions.delete.confirmtext"), resources.getString("modpackoptions.delete.confirmtitle"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                modpackSelector.getSelectedPack().delete();
                modpackSelector.forceRefresh();
            }
        });

        infoSwap = new JPanel();
        infoLayout = new CardLayout();
        infoSwap.setLayout(infoLayout);
        infoSwap.setOpaque(false);
        newsInfoPanel = new NewsInfoPanel(resources, avatarRepo);
        infoSwap.add(discoverInfoPanel,TAB_DISCOVER);

        JPanel newsHost = new JPanel();
        infoSwap.add(newsHost, TAB_NEWS);
        JPanel modpackHost = new JPanel();
        infoSwap.add(modpackHost, TAB_MODPACKS);
        centralPanel.add(infoSwap, BorderLayout.CENTER);

        newsSelector = new NewsSelector(resources, newsInfoPanel, platformApi, avatarRepo, newsCircle, settings);
        newsHost.setLayout(new BorderLayout());
        newsHost.add(newsInfoPanel, BorderLayout.CENTER);
        newsHost.add(newsSelector, BorderLayout.WEST);

        modpackHost.setLayout(new BorderLayout());
        modpackHost.add(modpackPanel, BorderLayout.CENTER);
        modpackHost.add(modpackSelector, BorderLayout.WEST);

        footer = new TintablePanel();
        footer.setTintColor(UIConstants.COLOR_CENTRAL_BACK);
        footer.setBackground(UIConstants.COLOR_FOOTER);
        footer.setLayout(new BoxLayout(footer, BoxLayout.LINE_AXIS));
        footer.setForeground(UIConstants.COLOR_WHITE_TEXT);
        footer.setBorder(BorderFactory.createEmptyBorder(3,6,3,12));

        userWidget = new UserWidget(resources, skinRepository);
        userWidget.setMaximumSize(userWidget.getPreferredSize());
        footer.add(userWidget);

        JLabel dashText = new JLabel("| ");
        dashText.setForeground(UIConstants.COLOR_WHITE_TEXT);
        dashText.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        footer.add(dashText);

        JButton logout = new JButton(resources.getString("launcher.user.logout"));
        logout.setBorder(BorderFactory.createEmptyBorder());
        logout.setContentAreaFilled(false);
        logout.setFocusable(false);
        logout.setForeground(UIConstants.COLOR_WHITE_TEXT);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 15));
        logout.addActionListener(e -> logout());
        footer.add(logout);

        installProgress = new ProgressBar();
        installProgress.setForeground(Color.white);
        installProgress.setBackground(UIConstants.COLOR_GREEN);
        installProgress.setBorder(BorderFactory.createEmptyBorder(5, 45, 4, 45));
        installProgress.setIcon(resources.getIcon("download_icon.png"));
        installProgress.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        installProgress.setVisible(false);
        footer.add(installProgress);

        installProgressPlaceholder = Box.createHorizontalGlue();
        footer.add(installProgressPlaceholder);

        JButton buildCtrl = new JButton(resources.getIcon("akliz-logo.png"));
        buildCtrl.setBorder(BorderFactory.createEmptyBorder());
        buildCtrl.setContentAreaFilled(false);
        buildCtrl.setHorizontalTextPosition(SwingConstants.RIGHT);
        buildCtrl.setHorizontalAlignment(SwingConstants.RIGHT);
        buildCtrl.setFocusable(false);
        buildCtrl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buildCtrl.addActionListener(e -> DesktopUtils.browseUrl("https://www.akliz.net/technic"));
        footer.add(buildCtrl);

        getRootPane().getContentPane().add(footer, BorderLayout.PAGE_END);

        if (resources.hasResource("teaser.png")) {
            getRootPane().setGlassPane(new SplatPane(modpacksTab, resources.getIcon("teaser.png"), SwingConstants.SOUTH, 5, 0));
            getRootPane().getGlassPane().setVisible(true);
        }
    }

    @Override
    public void relocalize(ResourceLoader loader) {
        this.resources = loader;
        this.resources.registerResource(this);

        setIconImage(this.resources.getImage("icon.png"));

        //Wipe controls
        getRootPane().getContentPane().removeAll();
        getRootPane().getContentPane().setLayout(null);

        //Clear references to existing controls

        initComponents();
        userChanged(userModel.getCurrentUser());

        if (currentTabName != null)
            selectTab(currentTabName);

        SwingUtilities.invokeLater(() -> {
            invalidate();
            repaint();
        });
    }

    @Override
    public void userChanged(IUserType user) {
        if (user == null)
            this.setVisible(false);
        else {
            this.setVisible(true);
            userWidget.setUser(user);

            if (modpackSelector.getSelectedPack() != null)
                setupPlayButtonText(modpackSelector.getSelectedPack(), user);

            modpackSelector.forceRefresh();
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    public void setupPlayButtonText(ModpackModel modpack, IUserType user) {
        playButton.setEnabled(true);
        playButton.setForeground(UIConstants.COLOR_BUTTON_BLUE);

        final boolean isUserOffline = user != null && user.isOffline();

        if (installer.isCurrentlyRunning()) {
            playButton.setText(resources.getString("launcher.pack.cancel"));
        } else if (modpack.getInstalledVersion() != null) {
            if (isUserOffline) {
                playButton.setText(resources.getString("launcher.pack.launch.offline"));
            } else {
                playButton.setText(resources.getString("launcher.pack.launch"));
            }
            playButton.setIcon(new ImageIcon(resources.colorImage(resources.getImage("play_button.png"), UIConstants.COLOR_BUTTON_BLUE)));
            playButton.setHoverIcon(new ImageIcon(resources.colorImage(resources.getImage("play_button.png"), UIConstants.COLOR_BLUE)));
        } else {
            if (isUserOffline) {
                playButton.setEnabled(false);
                playButton.setForeground(UIConstants.COLOR_GREY_TEXT);
                playButton.setText(resources.getString("launcher.pack.cannotinstall"));
            } else {
                playButton.setText(resources.getString("launcher.pack.install"));
            }
            playButton.setIcon(new ImageIcon(resources.colorImage(resources.getImage("download_button.png"), UIConstants.COLOR_BUTTON_BLUE)));
            playButton.setHoverIcon(new ImageIcon(resources.colorImage(resources.getImage("download_button.png"), UIConstants.COLOR_BLUE)));
        }
    }
}
