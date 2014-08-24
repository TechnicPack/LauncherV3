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

package net.technicpack.launcher.ui;

import net.technicpack.launchercore.modpacks.PackLoader;
import net.technicpack.ui.controls.DraggableFrame;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.TintablePanel;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.components.OptionsDialog;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.discover.DiscoverSelector;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LauncherFrame extends DraggableFrame implements IRelocalizableResource, IAuthListener<MojangUser> {

    private static final int FRAME_WIDTH = 1200;
    private static final int FRAME_HEIGHT = 720;

    private static final int SIDEKICK_WIDTH = 300;
    private static final int SIDEKICK_HEIGHT = 250;

    public static final Color COLOR_RED = new Color(229,0,0);
    public static final Color COLOR_GREEN = new Color(90, 184, 96);
    public static final Color COLOR_BLUE = new Color(16, 108, 163);
    public static final Color COLOR_BLUE_DARKER = new Color(12, 94, 145);
    public static final Color COLOR_WHITE_TEXT = new Color(208,208,208);
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

    private HeaderTab discoverTab;
    private HeaderTab modpacksTab;
    private HeaderTab newsTab;

    private CardLayout selectorLayout;
    private JPanel selectorSwap;
    private CardLayout infoLayout;
    private JPanel infoSwap;

    private UserWidget userWidget;
    private ProgressBar installProgress;
    private Component installProgressPlaceholder;
    private RoundedButton playButton;
    private ModpackSelector modpackSelector;
    private NewsSelector newsSelector;
    private TintablePanel centralPanel;
    private TintablePanel leftPanel;
    private TintablePanel footer;

    private String currentTabName;

    NewsInfoPanel newsInfoPanel;

    public LauncherFrame(ResourceLoader resources, ImageRepository<IUserType> skinRepository, UserModel userModel, TechnicSettings settings, ModpackSelector modpackSelector, ImageRepository<ModpackModel> iconRepo, ImageRepository<ModpackModel> logoRepo, ImageRepository<ModpackModel> backgroundRepo, Installer installer, ImageRepository<AuthorshipInfo> avatarRepo, IPlatformApi platformApi) {
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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

        //Handles rebuilding the frame, so use it to build the frame in the first place
        relocalize(resources);

        selectTab("modpacks");
    }

    /////////////////////////////////////////////////
    // Action responses
    /////////////////////////////////////////////////

    protected void selectTab(String tabName) {
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
        selectorLayout.show(selectorSwap, tabName);

        currentTabName = tabName;
    }

    protected void closeWindow() {
        System.exit(0);
    }

    protected void logout() {
        if (installer.isCurrentlyRunning())
            return;

        userModel.setCurrentUser(null);
    }

    protected void launchModpack() {
        if (installer.isCurrentlyRunning())
            return;

        ModpackModel pack = modpackSelector.getSelectedPack();

        if (pack == null)
            return;

        boolean forceInstall = false;
        Version installedVersion = pack.getInstalledVersion();

        //Force a full install (check cache, redownload, unzip files) if we have no current installation of this modpack
        if (installedVersion == null)
            forceInstall = true;
        else if (pack.getBuild() != null) {

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
        if (forceInstall) {
            installBuild = pack.getBuild();

            if (installBuild.equalsIgnoreCase(InstalledPack.RECOMMENDED))
                installBuild = pack.getPackInfo().getRecommended();
            else if (installBuild.equalsIgnoreCase(InstalledPack.LATEST))
                installBuild = pack.getPackInfo().getLatest();
        } else
            installBuild = installedVersion.getVersion();

        installer.installAndRun(resources, pack, installBuild, forceInstall, this, installProgress);

        installProgress.setVisible(true);
        installProgressPlaceholder.setVisible(false);
        userChanged(userModel.getCurrentUser());
        invalidate();
    }

    public void launchCompleted() {
        installProgress.setVisible(false);
        installProgressPlaceholder.setVisible(true);

        if (!installer.isCurrentlyRunning())
            userChanged(userModel.getCurrentUser());

        invalidate();
    }

    protected void openLauncherOptions() {
        leftPanel.setTintActive(true);
        centralPanel.setTintActive(true);
        footer.setTintActive(true);
        OptionsDialog dialog = new OptionsDialog(this, settings, resources);
        dialog.setVisible(true);
        leftPanel.setTintActive(false);
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
        JLabel headerLabel = new JLabel(headerIcon);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(5,8,5,0));
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
        modpacksTab.setIcon(resources.getIcon("downTriangle.png"));
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
        newsCircle.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS_BOLD, 14));
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

        ModpackInfoPanel modpackPanel = new ModpackInfoPanel(resources, iconRepo, logoRepo, backgroundRepo, avatarRepo);
        modpackSelector.setInfoPanel(modpackPanel);
        playButton = modpackPanel.getPlayButton();
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launchModpack();
            }
        });

        DiscoverInfoPanel discoverPanel = new DiscoverInfoPanel(resources);

        infoSwap = new JPanel();
        infoLayout = new CardLayout();
        infoSwap.setLayout(infoLayout);
        infoSwap.setOpaque(false);
        newsInfoPanel = new NewsInfoPanel(resources, avatarRepo);
        infoSwap.add(discoverPanel,"discover");
        infoSwap.add(newsInfoPanel, "news");
        infoSwap.add(modpackPanel, "modpacks");
        centralPanel.add(infoSwap, BorderLayout.CENTER);

        /////////////////////////////////////////////////////////////
        // LEFT SETUP
        /////////////////////////////////////////////////////////////
        leftPanel = new TintablePanel();
        leftPanel.setTintColor(COLOR_CENTRAL_BACK);
        this.add(leftPanel, BorderLayout.LINE_START);

        leftPanel.setLayout(new BorderLayout());

        selectorSwap = new JPanel();
        selectorSwap.setOpaque(false);
        this.selectorLayout = new CardLayout();
        selectorSwap.setLayout(selectorLayout);
        selectorSwap.add(new DiscoverSelector(resources), "discover");

        selectorSwap.add(modpackSelector, "modpacks");
        newsSelector = new NewsSelector(resources, newsInfoPanel, platformApi, avatarRepo, newsCircle, settings);
        selectorSwap.add(newsSelector, "news");
        leftPanel.add(selectorSwap, BorderLayout.CENTER);

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
        installProgress.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
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
    }

    @Override
    public void userChanged(MojangUser mojangUser) {
        if (mojangUser == null)
            this.setVisible(false);
        else {
            this.setVisible(true);
            userWidget.setUser(mojangUser);

            if (installer.isCurrentlyRunning()) {
                playButton.setText(resources.getString("launcher.pack.launching"));
            } else if (mojangUser.isOffline()) {
                playButton.setText(resources.getString("launcher.pack.launch.offline"));
            } else {
                playButton.setText(resources.getString("launcher.pack.launch"));
            }
        }
    }
}
