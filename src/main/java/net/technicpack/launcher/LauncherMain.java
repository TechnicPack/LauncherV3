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

package net.technicpack.launcher;

import com.beust.jcommander.JCommander;
import net.technicpack.autoupdate.Relauncher;
import net.technicpack.autoupdate.http.HttpUpdateStream;
import net.technicpack.launcher.io.*;
import net.technicpack.launcher.settings.migration.IMigrator;
import net.technicpack.launcher.settings.migration.InitialV3Migrator;
import net.technicpack.launcher.ui.InstallerFrame;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserStore;
import net.technicpack.launchercore.image.face.CrafatarFaceImageStore;
import net.technicpack.launchercore.logging.BuildLogFormatter;
import net.technicpack.launchercore.logging.RotatingFileHandler;
import net.technicpack.launchercore.modpacks.PackLoader;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.platform.cache.ModpackCachePlatformApi;
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
import net.technicpack.launchercore.image.face.MinotarFaceImageStore;
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
import net.technicpack.launchercore.modpacks.sources.IPackSource;
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
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LauncherMain {

    public static ConsoleFrame consoleFrame;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        StartupParameters params = new StartupParameters(args);
        try {
            new JCommander(params, args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Relauncher launcher = new Relauncher(new HttpUpdateStream("http://www.technicpack.net/api/launcher/version/"));
        TechnicSettings settings = null;

        try {
            settings = SettingsFactory.buildSettingsObject(launcher.getRunningPath(LauncherMain.class), params.isMover());
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        if (settings == null) {
            ResourceLoader installerResources = new ResourceLoader("net","technicpack","launcher","resources");
            installerResources.setLocale(ResourceLoader.DEFAULT_LOCALE);
            InstallerFrame dialog = new InstallerFrame(installerResources, params);
            dialog.setVisible(true);
            return;
        }

        LauncherDirectories directories = new TechnicLauncherDirectories(settings.getTechnicRoot());
        ResourceLoader resources = new ResourceLoader("net","technicpack","launcher","resources");
        resources.setLocale(settings.getLanguageCode());

        setupLogging(directories, resources);

        boolean needsReboot = false;

        if (System.getProperty("awt.useSystemAAFontSettings") == null || !System.getProperty("awt.useSystemAAFontSettings").equals("lcd"))
            needsReboot = true;
        else if (!Boolean.parseBoolean(System.getProperty("java.net.preferIPv4Stack")))
            needsReboot = true;

        if (params.isLauncher())
            startLauncher(settings, params, directories, resources);
        else if (params.isMover())
            startMover(params, launcher);
        else if (needsReboot && StringUtils.isNumeric(resources.getLauncherBuild())) {
            // ^^^^^
            //The debugger can't really relaunch so double check the build number to make sure we're operating in a valid environment
            launcher.launch(null, LauncherMain.class, params.getArgs());
            return;
        } else {
            updateAndRelaunch(params, directories, resources, settings, launcher);
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

    private static void startLauncher(TechnicSettings settings, StartupParameters startupParameters, LauncherDirectories directories, ResourceLoader resources) {
        UIManager.put( "ComboBox.disabledBackground", LauncherFrame.COLOR_FORMELEMENT_INTERNAL );
        UIManager.put( "ComboBox.disabledForeground", LauncherFrame.COLOR_GREY_TEXT );
        System.setProperty("xr.load.xml-reader",  "org.ccil.cowan.tagsoup.Parser");

        final SplashScreen splash = new SplashScreen(resources.getImage("launch_splash.png"), 0);
        splash.pack();
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

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

        ISolderApi solder = new HttpSolderApi(settings.getClientId(), userModel);
        HttpPlatformApi httpPlatform = new HttpPlatformApi("http://www.technicpack.net/", mirrorStore);

        IPlatformApi platform = new ModpackCachePlatformApi(httpPlatform, 60 * 60);

        IInstalledPackRepository packStore = TechnicInstalledPackStore.load(new File(directories.getLauncherDirectory(), "installedPacks"));
        IAuthoritativePackSource packInfoRepository = new PlatformPackInfoRepository(platform, solder);

        ArrayList<IMigrator> migrators = new ArrayList<IMigrator>(1);
        migrators.add(new InitialV3Migrator(platform));
        SettingsFactory.migrateSettings(settings, packStore, directories, users, migrators);

        PackLoader packList = new PackLoader(directories, packStore, packInfoRepository);
        ModpackSelector selector = new ModpackSelector(resources, packList, new SolderPackSource("http://solder.technicpack.net/api/", solder, true), solder, platform, iconRepo);
        selector.setBorder(BorderFactory.createEmptyBorder());
        userModel.addAuthListener(selector);

        resources.registerResource(selector);

        DiscoverInfoPanel discoverInfoPanel = new DiscoverInfoPanel(resources, startupParameters.getDiscoverUrl(), platform, splash);

        MinecraftLauncher launcher = new MinecraftLauncher(platform, directories, userModel, settings.getClientId());
        ModpackInstaller modpackInstaller = new ModpackInstaller(platform, settings.getClientId());
        Installer installer = new Installer(startupParameters, mirrorStore, directories, modpackInstaller, launcher, settings, iconMapper);

        LauncherFrame frame = new LauncherFrame(resources, skinRepo, userModel, settings, selector, iconRepo, logoRepo, backgroundRepo, installer, avatarRepo, platform, directories, packStore, startupParameters, discoverInfoPanel);
        userModel.addAuthListener(frame);

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

    private static void startMover(StartupParameters params, Relauncher relauncher) {
        try {
            relauncher.replacePackage(LauncherMain.class, params.getMoveTarget());
        } catch (UnsupportedEncodingException ex) {
            Utils.getLogger().log(Level.SEVERE, "Error attempting to copy downloaded package: ", ex);
            return;
        }

        String[] args = relauncher.buildLauncherArgs(relauncher.buildLauncherArgs(params.getArgs()));
        relauncher.launch(params.getMoveTarget(), LauncherMain.class, args);
    }

    private static void updateAndRelaunch(StartupParameters params, LauncherDirectories directories, ResourceLoader resources, TechnicSettings settings, Relauncher relauncher) {
        String launcherBuild = resources.getLauncherBuild();
        int build = -1;

        try {
            build = Integer.parseInt(launcherBuild);
        } catch (NumberFormatException ex) {
            //This is probably a debug build or something, build number is invalid
        }

        if (build < 1) {
            //We're in debug mode do not relaunch
            startLauncher(settings, params, directories, resources);
            return;
        }

        //In order to allow the old launcher to update & maintain backward compatibility we're keeping the old
        //stream pages live, and appending a "4" to the new streams.  So "stable" in the settings file means we
        //pull from "stable4"
        String url = relauncher.getUpdateUrl(settings.getBuildStream()+"4", build, LauncherMain.class);

        String[] args;
        if (url == null) {
            startLauncher(settings, params, directories, resources);
            return;
        }

        SplashScreen screen = new SplashScreen(resources.getImage("launch_splash.png"), 30);
        screen.getProgressBar().setForeground(Color.white);
        screen.getProgressBar().setBackground(LauncherFrame.COLOR_GREEN);
        screen.getProgressBar().setBackFill(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        screen.getProgressBar().setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        screen.pack();
        screen.setLocationRelativeTo(null);
        screen.setVisible(true);

        String tempPath = relauncher.downloadUpdate(url, directories, resources.getString("updater.downloading"), screen.getProgressBar());

        if (tempPath == null) {
            Utils.getLogger().severe("The launcher update failed to download.");
            screen.dispose();
            startLauncher(settings, params, directories, resources);
            return;
        }

        try {
            args = relauncher.buildMoverArgs(LauncherMain.class, params.getArgs());
        } catch (UnsupportedEncodingException ex) {
            Utils.getLogger().log(Level.SEVERE, "Error attempting to launch mover mode: ", ex);
            return;
        }
        relauncher.launch(tempPath, LauncherMain.class, args);
    }
}
