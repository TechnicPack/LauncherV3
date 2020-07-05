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
import net.technicpack.autoupdate.IBuildNumber;
import net.technicpack.autoupdate.Relauncher;
import net.technicpack.autoupdate.http.HttpUpdateStream;
import net.technicpack.discord.CacheDiscordApi;
import net.technicpack.discord.HttpDiscordApi;
import net.technicpack.discord.IDiscordApi;
import net.technicpack.launcher.autoupdate.CommandLineBuildNumber;
import net.technicpack.launcher.autoupdate.TechnicRelauncher;
import net.technicpack.launcher.autoupdate.VersionFileBuildNumber;
import net.technicpack.launcher.io.*;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.settings.migration.IMigrator;
import net.technicpack.launcher.settings.migration.InitialV3Migrator;
import net.technicpack.launcher.ui.InstallerFrame;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.LoginFrame;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserStore;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.image.face.MinotarFaceImageStore;
import net.technicpack.launchercore.image.face.WebAvatarImageStore;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.source.FileJavaSource;
import net.technicpack.launchercore.launch.java.source.InstalledJavaSource;
import net.technicpack.launchercore.logging.BuildLogFormatter;
import net.technicpack.launchercore.logging.RotatingFileHandler;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.PackLoader;
import net.technicpack.launchercore.modpacks.resources.PackImageStore;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.modpacks.resources.resourcetype.BackgroundResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IModpackResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IconResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.LogoResourceType;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.minecraftcore.launch.MinecraftLauncher;
import net.technicpack.minecraftcore.mojang.auth.AuthenticationService;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.IPlatformSearchApi;
import net.technicpack.platform.PlatformPackInfoRepository;
import net.technicpack.platform.cache.ModpackCachePlatformApi;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.platform.http.HttpPlatformSearchApi;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.SolderPackSource;
import net.technicpack.solder.cache.CachedSolderApi;
import net.technicpack.solder.http.HttpSolderApi;
import net.technicpack.ui.components.Console;
import net.technicpack.ui.components.ConsoleFrame;
import net.technicpack.ui.components.ConsoleHandler;
import net.technicpack.ui.components.LoggerOutputStream;
import net.technicpack.ui.controls.installation.SplashScreen;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.JavaUtils;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LauncherMain {

    public static ConsoleFrame consoleFrame;

    public static final Locale[] supportedLanguages = new Locale[] {
            Locale.ENGLISH,
            new Locale("pt","BR"),
            new Locale("pt","PT"),
            new Locale("cs"),
            Locale.GERMAN,
            Locale.FRENCH,
            Locale.ITALIAN,
            new Locale("hu"),
            new Locale("pl"),
            Locale.CHINA,
            Locale.TAIWAN,
            new Locale("nl", "NL"),
            new Locale("sk"),
    };

    private static IBuildNumber buildNumber;

    public static void main(String[] argv) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        StartupParameters params = new StartupParameters(argv);
        try {
            JCommander.newBuilder()
                    .addObject(params)
                    .build()
                    .parse(argv);
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

        if (params.getBuildNumber() != null && !params.getBuildNumber().isEmpty())
            buildNumber = new CommandLineBuildNumber(params);
        else
            buildNumber = new VersionFileBuildNumber(resources);

        TechnicConstants.setBuildNumber(buildNumber);

        setupLogging(directories, resources);

        String launcherBuild = buildNumber.getBuildNumber();
        int build = -1;

        try {
            build = Integer.parseInt((new VersionFileBuildNumber(resources)).getBuildNumber());
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

        fileHandler.setFormatter(new BuildLogFormatter(buildNumber.getBuildNumber()));

        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);

        LauncherMain.consoleFrame = new ConsoleFrame(2500, resources.getImage("icon.png"));
        Console console = new Console(LauncherMain.consoleFrame, buildNumber.getBuildNumber());

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

    private static void startLauncher(final TechnicSettings settings, StartupParameters startupParameters, final LauncherDirectories directories, ResourceLoader resources) {
        UIManager.put( "ComboBox.disabledBackground", LauncherFrame.COLOR_FORMELEMENT_INTERNAL );
        UIManager.put( "ComboBox.disabledForeground", LauncherFrame.COLOR_GREY_TEXT );
        System.setProperty("xr.load.xml-reader", "org.ccil.cowan.tagsoup.Parser");

        //Remove all log files older than a week
        new Thread(new Runnable() {
            @Override
            public void run() {
                Iterator<File> files = FileUtils.iterateFiles(new File(directories.getLauncherDirectory(), "logs"), new String[] {"log"}, false);
                while (files.hasNext()) {
                    File logFile = files.next();
                    if (logFile.exists() && (new DateTime(logFile.lastModified())).isBefore(DateTime.now().minusWeeks(1))) {
                        logFile.delete();
                    }
                }
            }
        }).start();

        // Startup debug messages
        Utils.getLogger().info("OS: " + System.getProperty("os.name").toLowerCase(Locale.ENGLISH));
        Utils.getLogger().info("Identified as "+ OperatingSystem.getOperatingSystem().getName());
        Utils.getLogger().info("Java: " + System.getProperty("java.version") + " " + JavaUtils.getJavaBitness() + "-bit (" + System.getProperty("os.arch") + ")");
        final String[] domains = {"minecraft.net", "session.minecraft.net", "textures.minecraft.net", "libraries.minecraft.net", "authserver.mojang.com", "account.mojang.com", "technicpack.net", "launcher.technicpack.net", "api.technicpack.net", "mirror.technicpack.net", "solder.technicpack.net", "files.minecraftforge.net"};
        for (String domain : domains) {
            try {
                Collection<InetAddress> inetAddresses = Arrays.asList(InetAddress.getAllByName(domain));
                String ips = inetAddresses.stream().map(InetAddress::getHostAddress).collect(Collectors.joining(", "));
                Utils.getLogger().info(domain + " resolves to [" + ips + "]");
            } catch (UnknownHostException ex) {
                Utils.getLogger().log(Level.SEVERE, "Failed to resolve " + domain + ": " + ex.toString());
            }
        }

        final SplashScreen splash = new SplashScreen(resources.getImage("launch_splash.png"), 0);
        Color bg = LauncherFrame.COLOR_FORMELEMENT_INTERNAL;
        splash.getContentPane().setBackground(new Color (bg.getRed(),bg.getGreen(),bg.getBlue(),255));
        splash.pack();
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        // Inject new root certs for compatibility with older Java versions that don't have them
        injectNewRootCerts();

        JavaVersionRepository javaVersions = new JavaVersionRepository();
        (new InstalledJavaSource()).enumerateVersions(javaVersions);
        FileJavaSource javaVersionFile = FileJavaSource.load(new File(settings.getTechnicRoot(), "javaVersions.json"));
        javaVersionFile.enumerateVersions(javaVersions);
        javaVersions.selectVersion(settings.getJavaVersion(), settings.getJavaBitness());

        IUserStore<MojangUser> users = TechnicUserStore.load(new File(directories.getLauncherDirectory(),"users.json"));
        UserModel userModel = new UserModel(users, new AuthenticationService());

        IModpackResourceType iconType = new IconResourceType();
        IModpackResourceType logoType = new LogoResourceType();
        IModpackResourceType backgroundType = new BackgroundResourceType();

        PackResourceMapper iconMapper = new PackResourceMapper(directories, resources.getImage("icon.png"), iconType);
        ImageRepository<ModpackModel> iconRepo = new ImageRepository<ModpackModel>(iconMapper, new PackImageStore(iconType));
        ImageRepository<ModpackModel> logoRepo = new ImageRepository<ModpackModel>(new PackResourceMapper(directories, resources.getImage("modpack/ModImageFiller.png"), logoType), new PackImageStore(logoType));
        ImageRepository<ModpackModel> backgroundRepo = new ImageRepository<ModpackModel>(new PackResourceMapper(directories, null, backgroundType), new PackImageStore(backgroundType));

        ImageRepository<IUserType> skinRepo = new ImageRepository<IUserType>(new TechnicFaceMapper(directories, resources), new MinotarFaceImageStore("https://minotar.net/"));

        ImageRepository<AuthorshipInfo> avatarRepo = new ImageRepository<AuthorshipInfo>(new TechnicAvatarMapper(directories, resources), new WebAvatarImageStore());

        HttpSolderApi httpSolder = new HttpSolderApi(settings.getClientId());
        ISolderApi solder = new CachedSolderApi(directories, httpSolder, 60 * 60);
        HttpPlatformApi httpPlatform = new HttpPlatformApi("http://api.technicpack.net/", buildNumber.getBuildNumber());

        IPlatformApi platform = new ModpackCachePlatformApi(httpPlatform, 60 * 60, directories);
        IPlatformSearchApi platformSearch = new HttpPlatformSearchApi("http://api.technicpack.net/", buildNumber.getBuildNumber());

        IInstalledPackRepository packStore = TechnicInstalledPackStore.load(new File(directories.getLauncherDirectory(), "installedPacks"));
        IAuthoritativePackSource packInfoRepository = new PlatformPackInfoRepository(platform, solder);

        ArrayList<IMigrator> migrators = new ArrayList<IMigrator>(1);
        migrators.add(new InitialV3Migrator(platform));
        SettingsFactory.migrateSettings(settings, packStore, directories, users, migrators);

        PackLoader packList = new PackLoader(directories, packStore, packInfoRepository);
        ModpackSelector selector = new ModpackSelector(resources, packList, new SolderPackSource("http://solder.technicpack.net/api/", solder), solder, platform, platformSearch, iconRepo);
        selector.setBorder(BorderFactory.createEmptyBorder());
        userModel.addAuthListener(selector);

        resources.registerResource(selector);

        DiscoverInfoPanel discoverInfoPanel = new DiscoverInfoPanel(resources, startupParameters.getDiscoverUrl(), platform, directories, selector);

        MinecraftLauncher launcher = new MinecraftLauncher(platform, directories, userModel, javaVersions, buildNumber);
        ModpackInstaller modpackInstaller = new ModpackInstaller(platform, settings.getClientId());
        Installer installer = new Installer(startupParameters, directories, modpackInstaller, launcher, settings, iconMapper);

        IDiscordApi discordApi = new HttpDiscordApi("https://discord.com/api/");
        discordApi = new CacheDiscordApi(discordApi, 600, 60);

        final LauncherFrame frame = new LauncherFrame(resources, skinRepo, userModel, settings, selector, iconRepo, logoRepo, backgroundRepo, installer, avatarRepo, platform, directories, packStore, startupParameters, discoverInfoPanel, javaVersions, javaVersionFile, buildNumber, discordApi);
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

        Utils.sendTracking("runLauncher", "run", buildNumber.getBuildNumber(), settings.getClientId());
    }

    private static void injectNewRootCerts() {
        // Adapted from Forge installer
        final String javaVersion = System.getProperty("java.version");
        if (javaVersion == null || !javaVersion.startsWith("1.8.0_"))
            return;

        try {
            if (Integer.parseInt(javaVersion.substring("1.8.0_".length())) >= 101) {
                Utils.getLogger().log(Level.INFO, "Don't need to inject new root certificates");
                return;
            }
        } catch (final NumberFormatException e) {
            Utils.getLogger().log(Level.WARNING, "Couldn't parse Java version, can't inject new root certs", e);
            return;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            final Path ksPath = Paths.get(System.getProperty("java.home"),"lib", "security", "cacerts");
            keyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());
            Map<String, Certificate> jdkTrustStore = Collections.list(keyStore.aliases()).stream().collect(Collectors.toMap(a -> a, (String alias) -> {
                try {
                    return keyStore.getCertificate(alias);
                } catch (KeyStoreException e) {
                    Utils.getLogger().log(Level.WARNING, "Failed to get certificate", e);
                    return null;
                }
            }));

            KeyStore leKS = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream leKSFile = LauncherMain.class.getResourceAsStream("/net/technicpack/launcher/resources/technickeystore.jks");
            leKS.load(leKSFile, "technicrootca".toCharArray());
            Map<String, Certificate> leTrustStore = Collections.list(leKS.aliases()).stream().collect(Collectors.toMap(a -> a, (String alias) -> {
                try {
                    return leKS.getCertificate(alias);
                } catch (KeyStoreException e) {
                    Utils.getLogger().log(Level.WARNING, "Failed to get certificate", e);
                    return null;
                }
            }));

            KeyStore mergedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            mergedTrustStore.load(null, new char[0]);
            for (final Map.Entry<String, Certificate> entry : jdkTrustStore.entrySet()) {
                mergedTrustStore.setCertificateEntry(entry.getKey(), entry.getValue());
            }
            for (final Map.Entry<String , Certificate> entry : leTrustStore.entrySet()) {
                mergedTrustStore.setCertificateEntry(entry.getKey(), entry.getValue());
            }

            TrustManagerFactory instance = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            instance.init(mergedTrustStore);
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(null, instance.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(tls.getSocketFactory());
            Utils.getLogger().log(Level.INFO, "Injected new root certificates");
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException e) {
            Utils.getLogger().log(Level.WARNING, "Failed to inject new root certificates. Problems might happen");
            e.printStackTrace();
        }
    }
}
