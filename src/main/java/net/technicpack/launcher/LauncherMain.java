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
import com.beust.jcommander.ParameterException;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import net.technicpack.autoupdate.IBuildNumber;
import net.technicpack.autoupdate.Relauncher;
import net.technicpack.autoupdate.http.HttpUpdateStream;
import net.technicpack.discord.CachedDiscordApi;
import net.technicpack.discord.HttpDiscordApi;
import net.technicpack.discord.IDiscordApi;
import net.technicpack.launcher.autoupdate.CommandLineBuildNumber;
import net.technicpack.launcher.autoupdate.VersionFileBuildNumber;
import net.technicpack.launcher.io.*;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.settings.migration.IMigrator;
import net.technicpack.launcher.settings.migration.InitialV3Migrator;
import net.technicpack.launcher.settings.migration.ResetJvmArgsIfDefaultString;
import net.technicpack.launcher.ui.InstallerFrame;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.LoginFrame;
import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.launcher.ui.components.discover.DiscoverInfoPanel;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.launchercore.TechnicConstants;
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
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftAuthenticator;
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
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LauncherMain {

    private static final Locale[] supportedLanguages = new Locale[]{
            Locale.ENGLISH,
            new Locale("pt", "BR"),
            new Locale("pt", "PT"),
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
    private static ConsoleFrame consoleFrame;
    private static IBuildNumber buildNumber;

    public static void main(String[] argv) {
        // Initialize Sentry
        Sentry.init(options -> {
            options.setDsn("https://4741ed8316eaefd3fa537240d8800c62@o4508140473417728.ingest.us.sentry.io/4509542931431424");
        });

        // Initialize the AWT desktop properties on Linux before any invocations are done
        // https://github.com/JFormDesigner/FlatLaf/issues/405#issuecomment-960242342
        Toolkit.getDefaultToolkit().getDesktopProperty("dummy");

        runHeadlessCheck();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Utils.getLogger().log(Level.SEVERE, "Failed to set system look and feel", ex);
        }

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        StartupParameters params = new StartupParameters(argv);
        try {
            JCommander jc = JCommander.newBuilder()
                    .addObject(params)
                    .build();
            // Allow options to be case-insensitive
            jc.setCaseSensitiveOptions(false);
            // Ignore extra unknown options
            jc.setAcceptUnknownOptions(true);
            // Parse the arguments into the params object
            jc.parse(argv);
        } catch (ParameterException ex) {
            ex.printStackTrace();
        }

        TechnicSettings settings;

        settings = SettingsFactory.buildSettingsObject(Relauncher.getRunningPath(LauncherMain.class), params.isMover());

        if (settings == null) {
            showSetupWindow(params);
            return;
        }

        User sentryUser = new User();
        sentryUser.setId(settings.getClientId());
        Sentry.setUser(sentryUser);

        LauncherDirectories directories = new TechnicLauncherDirectories(settings.getTechnicRoot());
        ResourceLoader resources = new ResourceLoader(directories, "net", "technicpack", "launcher", "resources");
        resources.setSupportedLanguages(supportedLanguages);
        resources.setLocale(settings.getLanguageCode());

        // Sanity check
        checkIfRunningInsideOneDrive(directories.getLauncherDirectory());

        if (params.getBuildNumber() != null && !params.getBuildNumber().isEmpty())
            buildNumber = new CommandLineBuildNumber(params);
        else
            buildNumber = new VersionFileBuildNumber(resources);

        Sentry.configureScope(scope -> {
            scope.setTag("buildNumber", buildNumber.getBuildNumber());
            scope.setTag("updateStream", settings.getBuildStream());
        });

        TechnicConstants.setBuildNumber(buildNumber);

        setupLogging(directories, resources);

        final boolean displayConsole = settings.getShowConsole();
        if (displayConsole) {
            EventQueue.invokeLater(() -> setConsoleVisible(true));
        }

        int build = -1;

        try {
            build = Integer.parseInt(buildNumber.getBuildNumber());
        } catch (NumberFormatException ex) {
            //This is probably a debug build or something, build number is invalid
        }

        // These 2 need to happen *before* the launcher or the updater run so we have valuable debug information and so
        // we can properly use websites that use Let's Encrypt (and other current certs not supported by old Java versions)
        runStartupDebug();
        updateJavaTrustStore();

        Relauncher launcher = new Relauncher(new HttpUpdateStream("https://api.technicpack.net/launcher/"), settings.getBuildStream()+"4", build, directories, resources, params);

        try {
            if (launcher.runAutoUpdater()) {
                startLauncher(settings, params, directories, resources);
            }
        } catch (InterruptedException e) {
            // Launcher update was interrupted, show an error message and exit
            Utils.getLogger().log(Level.SEVERE, "Launcher update interrupted", e);
            showStartupError(resources, resources.getString("updater.error.interrupt"));
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (DownloadException e) {
            // There was an error downloading the launcher resources, show an error message and exit
            Utils.getLogger().log(Level.SEVERE, "Failed to download launcher resources", e);
            Sentry.captureException(e);
            showStartupError(resources, resources.getString("updater.error.download", e.getMessage()));
            System.exit(1);
        } catch (IOException e) {
            // An unknown IO error occurred, show an error message and exit
            Utils.getLogger().log(Level.SEVERE, "IOException when starting launcher", e);
            Sentry.captureException(e);
            showStartupError(resources, resources.getString("updater.error.io", e.getMessage()));
            System.exit(1);
        }
    }

    /**
     * Checks if the launcher is running in a headless environment and terminate if so.
     */
    @SuppressWarnings("java:S106")
    private static void runHeadlessCheck() {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Technic Launcher cannot run in headless mode. Please run it in a graphical environment.");
            System.exit(1);
        }
    }

    private static void showStartupError(ResourceLoader resources, String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                resources.getString("updater.error.title"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Sets the visibility of the console frame.
     *
     * @param visible true to show the console, false to hide it
     */
    public static void setConsoleVisible(boolean visible) {
        if (consoleFrame != null) {
            consoleFrame.setVisible(visible);
        }
    }

    private static void showSetupWindow(StartupParameters params) {
        ResourceLoader installerResources = new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
        installerResources.setSupportedLanguages(supportedLanguages);
        installerResources.setLocale(ResourceLoader.DEFAULT_LOCALE);
        InstallerFrame dialog = new InstallerFrame(installerResources, params);
        dialog.setVisible(true);
    }

    private static void checkIfRunningInsideOneDrive(File launcherRoot) {
        if (OperatingSystem.getOperatingSystem() != OperatingSystem.WINDOWS) {
            return;
        }

        Path launcherRootPath = launcherRoot.toPath();

        for (String varName : new String[]{"OneDrive", "OneDriveConsumer"}) {
            String varValue = System.getenv(varName);
            if (varValue == null || varValue.isEmpty()) {
                continue;
            }

            Path oneDrivePath = new File(varValue).toPath();

            if (launcherRootPath.startsWith(oneDrivePath)) {
                JOptionPane.showMessageDialog(null,
                        "Technic Launcher cannot run inside OneDrive. Please move it out of OneDrive, in the launcher settings.",
                        "Cannot run inside OneDrive", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void setupLogging(LauncherDirectories directories, ResourceLoader resources) {
        System.out.println("Setting up logging");
        final Logger logger = Utils.getLogger();
        File logDirectory = directories.getLogsDirectory();
        File logs = new File(logDirectory, "techniclauncher_%D.log");
        RotatingFileHandler fileHandler = new RotatingFileHandler(logs.getPath());

        fileHandler.setFormatter(new BuildLogFormatter(buildNumber.getBuildNumber()));

        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);

        consoleFrame = new ConsoleFrame(2500, resources.getImage("icon.png"));
        Console console = new Console(consoleFrame, buildNumber.getBuildNumber());

        logger.addHandler(new ConsoleHandler(console));

        System.setOut(new PrintStream(new LoggerOutputStream(console, Level.INFO, logger), true));
        System.setErr(new PrintStream(new LoggerOutputStream(console, Level.SEVERE, logger), true));

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            logger.log(Level.SEVERE, String.format("Unhandled exception in thread %s", t), e);
            Sentry.captureException(e);
        });
    }

    /**
     * Runs the startup debug, including OS information and DNS resolution for key hostnames.
     */
    private static void runStartupDebug() {
        Utils.getLogger().info(String.format("OS: %s", System.getProperty("os.name").toLowerCase(Locale.ROOT)));
        Utils.getLogger().info(String.format("Identified as %s", OperatingSystem.getOperatingSystem().getName()));
        Utils.getLogger().info(String.format("Java: %s %s-bit (%s)", System.getProperty("java.version"), JavaUtils.JAVA_BITNESS, JavaUtils.OS_ARCH));
        final String[] domains = {
                "minecraft.net", "session.minecraft.net", "textures.minecraft.net", "libraries.minecraft.net",
                "account.mojang.com", "www.technicpack.net", "launcher.technicpack.net", "api.technicpack.net",
                "mirror.technicpack.net", "solder.technicpack.net", "files.minecraftforge.net",
                "user.auth.xboxlive.com", "xsts.auth.xboxlive.com", "api.minecraftservices.com",
                "launchermeta.mojang.com", "piston-meta.mojang.com",
        };

        // Run DNS resolution asynchronously
        CompletableFuture<?>[] dnsFutures = Arrays.stream(domains)
                .map(domain -> CompletableFuture.runAsync(() -> {
                    try {
                        String ips = Arrays.stream(InetAddress.getAllByName(domain))
                                .map(InetAddress::getHostAddress)
                                .collect(Collectors.joining(", "));
                        Utils.getLogger().info(String.format("%s resolves to [%s]", domain, ips));
                    } catch (UnknownHostException ex) {
                        Utils.getLogger().log(Level.SEVERE, String.format("Failed to resolve %s: %s", domain, ex));
                    }
                }))
                .toArray(CompletableFuture[]::new);

        // Wait for all DNS resolution tasks to complete
        CompletableFuture.allOf(dnsFutures).join();
    }

    private static String getCertificateFingerprint(Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X:", b));
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            Utils.getLogger().log(Level.WARNING, "Failed to get certificate fingerprint", e);
            return "unknown";
        }
    }

    private static void updateJavaTrustStore() {
        final String javaVersion = System.getProperty("java.version");

        if (JavaUtils.compareVersions(javaVersion, "1.8.0_141") >= 0) {
            Utils.getLogger().info(String.format("Don't need to update Java trust store; Java version is recent enough (%s)", javaVersion));
            return;
        }

        try {
            // Load the default trust store
            KeyStore defaultTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            final Path defaultKsPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
            try (InputStream is = Files.newInputStream(defaultKsPath)) {
                defaultTrustStore.load(is, "changeit".toCharArray());
            }

            // Load our custom trust store
            KeyStore technicTrustStore = KeyStore.getInstance("JKS");
            try (InputStream is = LauncherMain.class.getResourceAsStream("/net/technicpack/launcher/resources/technicKeystore.jks")) {
                technicTrustStore.load(is, "technicrootca".toCharArray());
            }

            // Create a new, empty trust store to merge the default and custom ones
            KeyStore mergedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            mergedTrustStore.load(null, null);

            // Copy the default trust store entries
            Enumeration<String> defaultAliases = defaultTrustStore.aliases();
            while (defaultAliases.hasMoreElements()) {
                String alias = defaultAliases.nextElement();
                Certificate cert = defaultTrustStore.getCertificate(alias);
                if (cert == null) {
                    Utils.getLogger().log(Level.WARNING, String.format("Certificate for alias '%s' in default trust store is null", alias));
                    continue;
                }
                mergedTrustStore.setCertificateEntry(alias, cert);
            }

            // Copy the custom trust store entries
            Enumeration<String> technicAliases = technicTrustStore.aliases();
            while (technicAliases.hasMoreElements()) {
                String alias = technicAliases.nextElement();
                Certificate cert = technicTrustStore.getCertificate(alias);
                if (cert == null) {
                    Utils.getLogger().log(Level.WARNING, String.format("Certificate for alias '%s' in Technic trust store is null", alias));
                    continue;
                }
                if (!mergedTrustStore.containsAlias(alias)) {
                    Utils.getLogger().log(Level.FINE, String.format("Adding certificate with alias '%s', fingerprint %s", alias, getCertificateFingerprint(cert)));
                    mergedTrustStore.setCertificateEntry(alias, cert);
                }
            }

            // Initialize the SSL context with the merged trust store
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(mergedTrustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            Utils.getLogger().log(Level.INFO, "Updated Java trust store with new root certificates successfully");
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException e) {
            Utils.getLogger().log(Level.WARNING, "Failed to update Java trust store. Problems might happen with TLS connections", e);
        }
    }

    /**
     * Creates a thread that will delete all log files older than a week.
     *
     * @param directories The launcher directories to use for finding the logs directory.
     * @return A thread that will perform the cleanup.
     */
    private static Thread createCleanupLogsThread(LauncherDirectories directories) {
        Thread cleanupLogsThread = new Thread(() -> {
            Iterator<File> files = FileUtils.iterateFiles(new File(directories.getLauncherDirectory(), "logs"), new String[]{"log"}, false);
            final DateTime aWeekAgo = DateTime.now().minusWeeks(1);
            while (files.hasNext()) {
                File logFile = files.next();
                if (logFile.exists() && (new DateTime(logFile.lastModified())).isBefore(aWeekAgo)) {
                    logFile.delete();
                }
            }
        });
        cleanupLogsThread.setDaemon(true);
        return cleanupLogsThread;
    }

    private static void startLauncher(final TechnicSettings settings, StartupParameters startupParameters, final LauncherDirectories directories,
                                      ResourceLoader resources) {
        UIManager.put("ComboBox.disabledBackground", UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        UIManager.put("ComboBox.disabledForeground", UIConstants.COLOR_GREY_TEXT);
        System.setProperty("xr.load.xml-reader", "org.ccil.cowan.tagsoup.Parser");

        // Remove all log files older than a week
        Thread cleanupLogsThread = createCleanupLogsThread(directories);
        cleanupLogsThread.start();

        final SplashScreen splash = new SplashScreen(resources.getImage("launch_splash.png"), 0);
        Color bg = UIConstants.COLOR_FORM_ELEMENT_INTERNAL;
        splash.getContentPane().setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 255));
        splash.pack();
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        JavaVersionRepository javaVersions = new JavaVersionRepository();
        (new InstalledJavaSource()).enumerateVersions(javaVersions);
        FileJavaSource javaVersionFile = FileJavaSource.load(new File(settings.getTechnicRoot(), "javaVersions.json"));
        javaVersionFile.enumerateVersions(javaVersions);
        javaVersions.selectVersion(settings.getJavaVersion(), settings.getPrefer64Bit());

        TechnicUserStore users = TechnicUserStore.load(new File(directories.getLauncherDirectory(), "users.json"));
        MicrosoftAuthenticator microsoftAuthenticator = new MicrosoftAuthenticator(new File(directories.getLauncherDirectory(), "oauth"));
        UserModel userModel = new UserModel(users, microsoftAuthenticator);

        IModpackResourceType iconType = new IconResourceType();
        IModpackResourceType logoType = new LogoResourceType();
        IModpackResourceType backgroundType = new BackgroundResourceType();

        PackResourceMapper iconMapper = new PackResourceMapper(directories, resources.getImage("icon.png"), iconType);
        ImageRepository<ModpackModel> iconRepo = new ImageRepository<>(iconMapper, new PackImageStore(iconType));
        ImageRepository<ModpackModel> logoRepo = new ImageRepository<>(new PackResourceMapper(directories,
                resources.getImage("modpack/ModImageFiller.png"), logoType), new PackImageStore(logoType));
        ImageRepository<ModpackModel> backgroundRepo = new ImageRepository<>(new PackResourceMapper(directories, null, backgroundType),
                new PackImageStore(backgroundType));

        ImageRepository<IUserType> skinRepo = new ImageRepository<>(new TechnicFaceMapper(directories, resources),
                new MinotarFaceImageStore("https://minotar.net/"));

        ImageRepository<AuthorshipInfo> avatarRepo = new ImageRepository<>(new TechnicAvatarMapper(directories, resources),
                new WebAvatarImageStore());

        HttpSolderApi httpSolder = new HttpSolderApi(settings.getClientId());
        ISolderApi solder = new CachedSolderApi(directories, httpSolder, 60 * 60);
        HttpPlatformApi httpPlatform = new HttpPlatformApi("https://api.technicpack.net/", buildNumber.getBuildNumber());

        IPlatformApi platform = new ModpackCachePlatformApi(httpPlatform, 60 * 60, directories);
        IPlatformSearchApi platformSearch = new HttpPlatformSearchApi("https://api.technicpack.net/", buildNumber.getBuildNumber());

        IInstalledPackRepository packStore = TechnicInstalledPackStore.load(new File(directories.getLauncherDirectory(), "installedPacks"));
        IAuthoritativePackSource packInfoRepository = new PlatformPackInfoRepository(platform, solder);

        ArrayList<IMigrator> migrators = new ArrayList<>(1);
        migrators.add(new InitialV3Migrator(platform));
        migrators.add(new ResetJvmArgsIfDefaultString());
        SettingsFactory.migrateSettings(settings, packStore, directories, users, migrators);

        PackLoader packList = new PackLoader(directories, packStore, packInfoRepository);
        ModpackSelector selector = new ModpackSelector(resources, packList, new SolderPackSource("https://solder.technicpack.net/api/", solder),
                solder, platform, platformSearch, iconRepo);
        selector.setBorder(BorderFactory.createEmptyBorder());
        userModel.addAuthListener(selector);

        resources.registerResource(selector);

        DiscoverInfoPanel discoverInfoPanel = new DiscoverInfoPanel(resources, startupParameters.getDiscoverUrl(), platform, directories, selector);

        MinecraftLauncher launcher = new MinecraftLauncher(platform, directories, userModel, javaVersions, buildNumber);
        ModpackInstaller modpackInstaller = new ModpackInstaller(platform, settings.getClientId());
        Installer installer = new Installer(startupParameters, directories, modpackInstaller, launcher, settings, iconMapper);

        IDiscordApi discordApi = new HttpDiscordApi("https://discord.com/api");
        discordApi = new CachedDiscordApi(discordApi, 600, 60);

        final LauncherFrame frame = new LauncherFrame(resources, skinRepo, userModel, settings, selector, iconRepo, logoRepo, backgroundRepo,
                installer, avatarRepo, platform, directories, packStore, startupParameters, discoverInfoPanel, javaVersions, javaVersionFile,
                buildNumber, discordApi);
        userModel.addAuthListener(frame);

        ActionListener listener = e -> {
            splash.dispose();
            if (settings.getLaunchToModpacks()) frame.selectTab(LauncherFrame.TAB_MODPACKS);
        };

        discoverInfoPanel.setLoadListener(listener);

        LoginFrame login = new LoginFrame(resources, settings, userModel, skinRepo);
        userModel.addAuthListener(login);
        userModel.addAuthListener(user -> {
            if (user == null) splash.dispose();
        });

        userModel.startupAuth();

        Utils.sendTracking("runLauncher", "run", buildNumber.getBuildNumber(), settings.getClientId());
    }
}
