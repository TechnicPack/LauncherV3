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

package net.technicpack.launcher;

import com.beust.jcommander.JCommander;
import net.technicpack.autoupdate.Relauncher;
import net.technicpack.autoupdate.http.HttpUpdateStream;
import net.technicpack.launcher.autoupdate.TechnicRelauncher;
import net.technicpack.launcher.io.*;
import net.technicpack.launcher.settings.migration.IMigrator;
import net.technicpack.launcher.settings.migration.InitialV3Migrator;
import net.technicpack.launcher.ui.InstallerFrame;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserStore;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.image.face.CrafatarFaceImageStore;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.source.FileJavaSource;
import net.technicpack.launchercore.launch.java.source.InstalledJavaSource;
import net.technicpack.launchercore.logging.BuildLogFormatter;
import net.technicpack.launchercore.logging.RotatingFileHandler;
import net.technicpack.launchercore.modpacks.PackLoader;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.platform.IPlatformSearchApi;
import net.technicpack.platform.cache.ModpackCachePlatformApi;
import net.technicpack.platform.http.HttpPlatformSearchApi;
import net.technicpack.solder.cache.CachedSolderApi;
import net.technicpack.ui.components.Console;
import net.technicpack.ui.components.ConsoleFrame;
import net.technicpack.ui.components.ConsoleHandler;
import net.technicpack.ui.components.LoggerOutputStream;
import net.technicpack.ui.controls.installation.SplashScreen;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.LoginFrame;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.mojang.auth.AuthenticationService;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.image.face.WebAvatarImageStore;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.minecraftcore.launch.MinecraftLauncher;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.resources.PackImageStore;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.modpacks.resources.resourcetype.BackgroundResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IModpackResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IconResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.LogoResourceType;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.mirror.secure.rest.JsonWebSecureMirror;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.PlatformPackInfoRepository;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.SolderPackSource;
import net.technicpack.solder.http.HttpSolderApi;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LauncherMain {

    public static ConsoleFrame consoleFrame;

    public static Locale[] supportedLanguages = new Locale[] {
            Locale.ENGLISH,
            new Locale("pt","BR"),
            new Locale("pt","PT"),
            new Locale("cs"),
            Locale.GERMAN,
            Locale.FRENCH,
            Locale.ITALIAN,
            new Locale("hu"),
            Locale.CHINA,
            Locale.TAIWAN
    };

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        StartupParameters params = new StartupParameters(args);
        try {
            new JCommander(params, args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        TechnicSettings settings = null;

        try {
            settings = SettingsFactory.buildSettingsObject(Relauncher.getRunningPath(LauncherMain.class), params.isMover());
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        if (settings == null) {
            ResourceLoader installerResources = new ResourceLoader(null, "net","technicpack","launcher","resources");
            installerResources.setSupportedLanguages(supportedLanguages);
            installerResources.setLocale(ResourceLoader.DEFAULT_LOCALE);
            InstallerFrame dialog = new InstallerFrame(installerResources, params);
            dialog.setVisible(true);
            return;
        }

        LauncherDirectories directories = new TechnicLauncherDirectories(settings.getTechnicRoot());
        ResourceLoader resources = new ResourceLoader(directories, "net","technicpack","launcher","resources");
        resources.setSupportedLanguages(supportedLanguages);
        resources.setLocale(settings.getLanguageCode());

        setupLogging(directories, resources);

        String launcherBuild = resources.getLauncherBuild();
        int build = -1;

        try {
            build = Integer.parseInt(launcherBuild);
        } catch (NumberFormatException ex) {
            //This is probably a debug build or something, build number is invalid
        }

        Relauncher launcher = new TechnicRelauncher(new HttpUpdateStream("http://api.technicpack.net/launcher/"), settings.getBuildStream()+"4", build, directories, resources, params);

        try {
            if (launcher.runAutoUpdater())
                startLauncher(settings, params, directories, resources);
        } catch (InterruptedException e) {
            //Canceled by user
        } catch (DownloadException e) {
            //JOptionPane.showMessageDialog(null, resources.getString("launcher.updateerror.download", pack.getDisplayName(), e.getMessage()), resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setupLogging(LauncherDirectories directories, ResourceLoader resources) {
        System.out.println("Setting up logging");
        final Logger logger = Utils.getLogger();
        File logDirectory = new File(directories.getLauncherDirectory(), "logs");
        if (!logDirectory.exists()) {
            logDirectory.mkdir();
        }
        File logs = new File(logDirectory, "techniclauncher_%D.log");
        RotatingFileHandler fileHandler = new RotatingFileHandler(logs.getPath());

        fileHandler.setFormatter(new BuildLogFormatter(resources.getLauncherBuild()));

        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);

        LauncherMain.consoleFrame = new ConsoleFrame(2500, resources.getImage("icon.png"));
        Console console = new Console(LauncherMain.consoleFrame, resources.getLauncherBuild());

        logger.addHandler(new ConsoleHandler(console));

        System.setOut(new PrintStream(new LoggerOutputStream(console, Level.INFO, logger), true));
        System.setErr(new PrintStream(new LoggerOutputStream(console, Level.SEVERE, logger), true));

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "Unhandled Exception in " + t, e);

//                if (errorDialog == null) {
//                    LauncherFrame frame = null;
//
//                    try {
//                        frame = Launcher.getFrame();
//                    } catch (Exception ex) {
//                        //This can happen if we have a very early crash- before Launcher initializes
//                    }
//
//                    errorDialog = new ErrorDialog(frame, e);
//                    errorDialog.setVisible(true);
//                }
            }
        });
    }

    private static void startLauncher(final TechnicSettings settings, StartupParameters startupParameters, LauncherDirectories directories, ResourceLoader resources) {
        UIManager.put( "ComboBox.disabledBackground", LauncherFrame.COLOR_FORMELEMENT_INTERNAL );
        UIManager.put( "ComboBox.disabledForeground", LauncherFrame.COLOR_GREY_TEXT );
        System.setProperty("xr.load.xml-reader",  "org.ccil.cowan.tagsoup.Parser");

        final SplashScreen splash = new SplashScreen(resources.getImage("launch_splash.png"), 0);
        Color bg = LauncherFrame.COLOR_FORMELEMENT_INTERNAL;
        splash.getContentPane().setBackground(new Color (bg.getRed(),bg.getGreen(),bg.getBlue(),255));
        splash.pack();
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        JavaVersionRepository javaVersions = new JavaVersionRepository();
        (new InstalledJavaSource()).enumerateVersions(javaVersions);
        FileJavaSource javaVersionFile = FileJavaSource.load(new File(settings.getTechnicRoot(), "javaVersions.json"));
        javaVersionFile.enumerateVersions(javaVersions);
        javaVersions.selectVersion(settings.getJavaVersion(), settings.getJavaBitness());

        IUserStore<MojangUser> users = TechnicUserStore.load(new File(directories.getLauncherDirectory(),"users.json"));
        UserModel userModel = new UserModel(users, new AuthenticationService());

        MirrorStore mirrorStore = new MirrorStore(userModel);
        mirrorStore.addSecureMirror("mirror.technicpack.net", new JsonWebSecureMirror("http://mirror.technicpack.net/", "mirror.technicpack.net"));

        IModpackResourceType iconType = new IconResourceType();
        IModpackResourceType logoType = new LogoResourceType();
        IModpackResourceType backgroundType = new BackgroundResourceType();

        PackResourceMapper iconMapper = new PackResourceMapper(directories, resources.getImage("icon.png"), iconType);
        ImageRepository<ModpackModel> iconRepo = new ImageRepository<ModpackModel>(iconMapper, new PackImageStore(iconType, mirrorStore, userModel));
        ImageRepository<ModpackModel> logoRepo = new ImageRepository<ModpackModel>(new PackResourceMapper(directories, resources.getImage("modpack/ModImageFiller.png"), logoType), new PackImageStore(logoType, mirrorStore, userModel));
        ImageRepository<ModpackModel> backgroundRepo = new ImageRepository<ModpackModel>(new PackResourceMapper(directories, null, backgroundType), new PackImageStore(backgroundType, mirrorStore, userModel));

        ImageRepository<IUserType> skinRepo = new ImageRepository<IUserType>(new TechnicFaceMapper(directories, resources), new CrafatarFaceImageStore("http://crafatar.com/", mirrorStore));

        ImageRepository<AuthorshipInfo> avatarRepo = new ImageRepository<AuthorshipInfo>(new TechnicAvatarMapper(directories, resources), new WebAvatarImageStore(mirrorStore));

        HttpSolderApi httpSolder = new HttpSolderApi(settings.getClientId(), userModel);
        ISolderApi solder = new CachedSolderApi(directories, httpSolder, 60 * 60);
        HttpPlatformApi httpPlatform = new HttpPlatformApi("http://api.technicpack.net/", mirrorStore);

        IPlatformApi platform = new ModpackCachePlatformApi(httpPlatform, 60 * 60, directories);
        IPlatformSearchApi platformSearch = new HttpPlatformSearchApi("http://api.technicpack.net/");

        IInstalledPackRepository packStore = TechnicInstalledPackStore.load(new File(directories.getLauncherDirectory(), "installedPacks"));
        IAuthoritativePackSource packInfoRepository = new PlatformPackInfoRepository(platform, solder);

        ArrayList<IMigrator> migrators = new ArrayList<IMigrator>(1);
        migrators.add(new InitialV3Migrator(platform));
        SettingsFactory.migrateSettings(settings, packStore, directories, users, migrators);

        PackLoader packList = new PackLoader(directories, packStore, packInfoRepository);
        ModpackSelector selector = new ModpackSelector(resources, packList, new SolderPackSource("http://solder.technicpack.net/api/", solder, true), solder, platform, platformSearch, iconRepo);
        selector.setBorder(BorderFactory.createEmptyBorder());
        userModel.addAuthListener(selector);

        resources.registerResource(selector);

        DiscoverInfoPanel discoverInfoPanel = new DiscoverInfoPanel(resources, startupParameters.getDiscoverUrl(), platform, directories, selector);

        MinecraftLauncher launcher = new MinecraftLauncher(platform, directories, userModel, settings.getClientId(), javaVersions);
        ModpackInstaller modpackInstaller = new ModpackInstaller(platform, settings.getClientId());
        Installer installer = new Installer(startupParameters, mirrorStore, directories, modpackInstaller, launcher, settings, iconMapper);

        final LauncherFrame frame = new LauncherFrame(resources, skinRepo, userModel, settings, selector, iconRepo, logoRepo, backgroundRepo, installer, avatarRepo, platform, directories, packStore, startupParameters, discoverInfoPanel, javaVersions, javaVersionFile);
        userModel.addAuthListener(frame);

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                splash.dispose();
                if (settings.getLaunchToModpacks())
                    frame.selectTab("modpacks");
            }
        };

        discoverInfoPanel.setLoadListener(listener);

        LoginFrame login = new LoginFrame(resources, settings, userModel, skinRepo);
        userModel.addAuthListener(login);
        userModel.addAuthListener(new IAuthListener() {
            @Override
            public void userChanged(Object user) {
                if (user == null)
                    splash.dispose();
            }
        });

        userModel.initAuth();
    }
}
