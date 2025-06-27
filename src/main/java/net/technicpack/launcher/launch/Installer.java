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

package net.technicpack.launcher.launch;

import io.sentry.Sentry;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.components.FixRunDataDialog;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.exception.CacheDeleteException;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.launchercore.install.Version;
import net.technicpack.launchercore.install.tasks.*;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.launch.GameProcess;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.RunData;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.launchercore.util.LaunchAction;
import net.technicpack.minecraftcore.install.tasks.*;
import net.technicpack.minecraftcore.launch.LaunchOptions;
import net.technicpack.minecraftcore.launch.MinecraftLauncher;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.MojangVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.FileVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.MojangVersionRetriever;
import net.technicpack.minecraftcore.mojang.version.builder.retrievers.HttpFileRetriever;
import net.technicpack.minecraftcore.mojang.version.builder.retrievers.ZipFileRetriever;
import net.technicpack.minecraftcore.mojang.version.chain.ChainVersionBuilder;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.Memory;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;

import javax.swing.JOptionPane;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

public class Installer {
    protected final ModpackInstaller installer;
    protected final MinecraftLauncher launcher;
    protected final TechnicSettings settings;
    protected final PackResourceMapper packIconMapper;
    protected final StartupParameters startupParameters;
    protected final LauncherDirectories directories;
    protected final Object cancelLock = new Object();
    protected volatile boolean isCancelledByUser = false;

    private Thread installerThread;
    private volatile LauncherUnhider launcherUnhider;
    private volatile GameProcess gameProcess;

    public Installer(StartupParameters startupParameters, LauncherDirectories directories, ModpackInstaller installer,
            MinecraftLauncher launcher, TechnicSettings settings, PackResourceMapper packIconMapper) {
        this.installer = installer;
        this.launcher = launcher;
        this.settings = settings;
        this.packIconMapper = packIconMapper;
        this.startupParameters = startupParameters;
        this.directories = directories;
    }

    public void cancel() {
        Utils.getLogger().info("User pressed cancel button.");
        synchronized (cancelLock) {
            isCancelledByUser = true;
        }
        installerThread.interrupt();
    }

    public void justInstall(final ResourceLoader resources, final ModpackModel pack, final String build,
            final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener) {
        internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, false);
    }

    public void installAndRun(final ResourceLoader resources, final ModpackModel pack, final String build,
            final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener) {
        internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, true);
    }

    protected void internalInstallAndRun(final ResourceLoader resources, final ModpackModel pack, final String build,
            final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener,
            final boolean doLaunch) {
        // Ensure that the installer is not currently running
        if (isCurrentlyRunning()) {
            Utils.getLogger().warning("Installer is already running, ignoring install request.");
            return;
        }

        // Reset cancel flag
        isCancelledByUser = false;

        // Create and start the installer thread
        installerThread = new InstallerThread(listener, pack, build, resources, doFullInstall, doLaunch, frame);
        installerThread.start();
    }

    public boolean isCurrentlyRunning() {
        return installerThread != null && installerThread.isAlive();
    }

    public boolean isGameProcessRunning() {
        return gameProcess != null && gameProcess.getProcess() != null && gameProcess.getProcess().isAlive();
    }

    private class InstallerThread extends Thread {
        private final DownloadListener listener;
        private final ModpackModel pack;
        private final String build;
        private final ResourceLoader resources;
        private final boolean doFullInstall;
        private final boolean doLaunch;
        private final LauncherFrame frame;

        public InstallerThread(DownloadListener listener, ModpackModel pack, String build, ResourceLoader resources, boolean doFullInstall, boolean doLaunch, LauncherFrame frame) {
            super("InstallerThread");
            setDaemon(true);

            this.listener = listener;
            this.pack = pack;
            this.build = build;
            this.resources = resources;
            this.doFullInstall = doFullInstall;
            this.doLaunch = doLaunch;
            this.frame = frame;

            Utils.getLogger().info(String.format("Starting installer thread for %s (%s)", pack.getDisplayName(), build));
        }

        /// Interrupt is being called from a mysterious source, so unless this is a user-initiated cancel
        /// Let's print the stack trace of the interruptor.
        @Override
        public void interrupt() {
            boolean userCancelled = false;
            synchronized (cancelLock) {
                if (isCancelledByUser)
                    userCancelled = true;
            }

            if (!userCancelled) {
                // Grab stack trace for this mysterious interruption
                Exception ex = new Exception("Stack trace");
                Utils.getLogger().log(Level.WARNING, "Mysterious interruption source.", ex);
                Sentry.captureException(ex);
            }
            super.interrupt();
        }

        @Override
        public void run() {
            gameProcess = null;

            try {
                InstallTasksQueue<MojangVersion> tasksQueue = new InstallTasksQueue<>(listener);
                MojangVersionBuilder versionBuilder = createVersionBuilder(tasksQueue);
                JavaVersionRepository javaVersions = launcher.getJavaVersions();

                final boolean mojangJavaWanted = settings.shouldUseMojangJava();

                MojangVersion version;

                if (build != null && !build.isEmpty()) {
                    buildTasksQueue(tasksQueue, build, versionBuilder, javaVersions.getSelectedVersion(), mojangJavaWanted);

                    version = installer.installPack(tasksQueue, pack, build);
                } else {
                    version = versionBuilder.buildVersionFromKey(null);

                    // Set up default Java runtime
                    version.setJavaRuntime(javaVersions.getSelectedVersion());

                    pack.initDirectories();
                }

                if (doLaunch) {
                    if (version == null) {
                        throw new PackNotAvailableOfflineException(pack.getDisplayName());
                    }

                    boolean usingMojangJava = mojangJavaWanted && version.getMojangRuntimeInformation() != null;

                    Memory memoryObj = Memory.getClosestAvailableMemory(Memory.getMemoryFromId(settings.getMemory()),
                            javaVersions.getSelectedVersion().is64Bit());
                    long memory = memoryObj.getMemoryMB();
                    String versionNumber = javaVersions.getSelectedVersion().getVersion();
                    RunData data = pack.getRunData();

                    if (data != null && !data.isRunDataValid(memory, versionNumber, usingMojangJava)) {
                        FixRunDataDialog dialog = new FixRunDataDialog(frame, resources, data, javaVersions, memoryObj,
                                !settings.shouldAutoAcceptModpackRequirements(), usingMojangJava);
                        dialog.setVisible(true);
                        if (dialog.getResult() == FixRunDataDialog.Result.ACCEPT) {
                            memoryObj = dialog.getRecommendedMemory();
                            memory = memoryObj.getMemoryMB();
                            settings.setMemory(memoryObj.getSettingsId());

                            IJavaRuntime recommendedJavaVersion = dialog.getRecommendedJavaVersion();
                            if (recommendedJavaVersion != null) {
                                javaVersions.selectVersion(recommendedJavaVersion.getVersion(),
                                        recommendedJavaVersion.is64Bit());
                                settings.setJavaVersion(recommendedJavaVersion.getVersion());
                                settings.setPrefer64Bit(recommendedJavaVersion.is64Bit());
                            }

                            if (dialog.shouldRemember()) {
                                settings.setAutoAcceptModpackRequirements(true);
                            }
                        } else {
                            return;
                        }
                    }

                    if (!usingMojangJava && RunData.isJavaVersionAtLeast(versionNumber, "1.9")) {
                        int result = JOptionPane.showConfirmDialog(frame,
                                resources.getString("launcher.jverwarning", versionNumber),
                                resources.getString("launcher.jverwarning.title"), JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (result != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }

                    LaunchAction launchAction = settings.getLaunchAction();

                    if (launchAction == null || launchAction == LaunchAction.HIDE) {
                        launcherUnhider = new LauncherUnhider(settings, frame);
                    } else {
                        launcherUnhider = null;
                    }

                    LaunchOptions options = new LaunchOptions(pack.getDisplayName(),
                            packIconMapper.getImageLocation(pack).getAbsolutePath(), settings);
                    gameProcess = launcher.launch(pack, memory, options, launcherUnhider, version);

                    if (launchAction == null || launchAction == LaunchAction.HIDE) {
                        frame.setVisible(false);
                    } else if (launchAction == LaunchAction.NOTHING) {
                        EventQueue.invokeLater(frame::launchCompleted);
                    } else if (launchAction == LaunchAction.CLOSE) {
                        System.exit(0);
                    }
                }
            } catch (InterruptedException e) {
                boolean cancelledByUser = false;
                synchronized (cancelLock) {
                    if (isCancelledByUser) {
                        cancelledByUser = true;
                        isCancelledByUser = false;
                    }
                }

                // Canceled by user
                if (!cancelledByUser) {
                    if (e.getCause() != null) {
                        Utils.getLogger().info("Cancelled by exception.");
                    } else {
                        Utils.getLogger().info("Cancelled by code.");
                    }
                    e.printStackTrace();
                    Sentry.captureException(e);
                } else {
                    Utils.getLogger().info("Cancelled by user.");
                }

                Thread.currentThread().interrupt();
            } catch (PackNotAvailableOfflineException e) {
                JOptionPane.showMessageDialog(frame, e.getMessage(),
                        resources.getString("launcher.installerror.unavailable"), JOptionPane.WARNING_MESSAGE);
            } catch (DownloadException e) {
                JOptionPane.showMessageDialog(frame,
                        resources.getString("launcher.installerror.download", pack.getDisplayName(), e.getMessage()),
                        resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
            } catch (ZipException e) {
                JOptionPane.showMessageDialog(frame,
                        resources.getString("launcher.installerror.unzip", pack.getDisplayName(), e.getMessage()),
                        resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
            } catch (CacheDeleteException e) {
                JOptionPane.showMessageDialog(frame,
                        resources.getString("launcher.installerror.cache", pack.getDisplayName(), e.getMessage()),
                        resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
            } catch (BuildInaccessibleException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, e.getMessage(), resources.getString("launcher.installerror.title"),
                        JOptionPane.WARNING_MESSAGE);
            } catch (Exception e) {
                Utils.getLogger().log(Level.SEVERE, "Exception caught during modpack installation or launch.", e);
                Sentry.captureException(e);
            } finally {
                if (!doLaunch || !isGameProcessRunning()) {
                    EventQueue.invokeLater(frame::launchCompleted);
                }
            }
        }

        private void buildTasksQueue(InstallTasksQueue<MojangVersion> queue, String build,
                                    MojangVersionBuilder versionBuilder, IJavaRuntime selectedJavaRuntime, boolean mojangJavaWanted) throws IOException {
            PackInfo packInfo = pack.getPackInfo();
            Modpack modpackData = packInfo.getModpack(build);

            if (modpackData.getGameVersion() == null)
                return;

            String minecraft = modpackData.getGameVersion();
            Version installedVersion = pack.getInstalledVersion();

            TaskGroup<MojangVersion> examineModpackData = new TaskGroup<>(resources.getString("install.message.examiningmodpack"));
            TaskGroup<MojangVersion> verifyingFiles = new TaskGroup<>(resources.getString("install.message.verifyingfiles"));
            TaskGroup<MojangVersion> downloadingMods = new TaskGroup<>(resources.getString("install.message.downloadmods"));
            TaskGroup<MojangVersion> installingMods = new TaskGroup<>(resources.getString("install.message.installmods"));
            TaskGroup<MojangVersion> checkVersionFile = new TaskGroup<>(resources.getString("install.message.checkversionfile"));
            TaskGroup<MojangVersion> installVersionFile = new TaskGroup<>(resources.getString("install.message.installversionfile"));
            TaskGroup<MojangVersion> rundataTaskGroup = new TaskGroup<>(resources.getString("install.message.runData"));
            TaskGroup<MojangVersion> examineVersionFile = new TaskGroup<>(resources.getString("install.message.examiningversionfile"));
            TaskGroup<MojangVersion> grabLibs = new TaskGroup<>(resources.getString("install.message.grablibraries"));
            TaskGroup<MojangVersion> checkNonMavenLibs = new TaskGroup<>(resources.getString("install.message.nonmavenlibs"));
            TaskGroup<MojangVersion> installingLibs = new TaskGroup<>(resources.getString("install.message.installlibs"));
            TaskGroup<MojangVersion> installingMinecraft = new TaskGroup<>(resources.getString("install.message.installminecraft"));
            TaskGroup<MojangVersion> examineIndex = new TaskGroup<>(resources.getString("install.message.examiningindex"));
            TaskGroup<MojangVersion> verifyingAssets = new TaskGroup<>(resources.getString("install.message.verifyassets"));
            TaskGroup<MojangVersion> installingAssets = new TaskGroup<>(resources.getString("install.message.installassets"));
            TaskGroup<MojangVersion> fetchJavaManifest = new TaskGroup<>("Obtaining Java runtime information...");
            TaskGroup<MojangVersion> examineJava = new TaskGroup<>("Examining Java runtime...");
            TaskGroup<MojangVersion> downloadJava = new TaskGroup<>("Downloading Java runtime...");

            queue.addTask(examineModpackData);
            queue.addTask(verifyingFiles);
            queue.addTask(downloadingMods);
            queue.addTask(installingMods);
            queue.addTask(checkVersionFile);
            queue.addTask(installVersionFile);
            queue.addTask(rundataTaskGroup);
            queue.addTask(examineVersionFile);
            queue.addTask(grabLibs);
            queue.addTask(checkNonMavenLibs);
            queue.addTask(installingLibs);
            queue.addTask(installingMinecraft);
            queue.addTask(examineIndex);
            queue.addTask(verifyingAssets);
            queue.addTask(installingAssets);
            if (mojangJavaWanted) {
                queue.addTask(fetchJavaManifest);
                queue.addTask(examineJava);
                queue.addTask(downloadJava);
            }
            if (OperatingSystem.getOperatingSystem() == OperatingSystem.OSX) {
                queue.addTask(new RenameJnilibToDylibTask(pack));
            }

            if (doFullInstall) {
                examineModpackData.addTask(new CleanupAndExtractModpackTask(pack, modpackData, verifyingFiles, downloadingMods, installingMods));
                rundataTaskGroup.addTask(new WriteRundataFile(pack, modpackData));
            } else {
                rundataTaskGroup.addTask(new CheckRunDataFile(pack, modpackData, rundataTaskGroup));
            }

            verifyingFiles.addTask(new InstallFmlLibsTask(pack, directories, modpackData, verifyingFiles, installingLibs, installingLibs));

            checkVersionFile.addTask(new VerifyVersionFilePresentTask(pack, minecraft, versionBuilder));
            examineVersionFile.addTask(new HandleVersionFileTask(pack, directories, checkNonMavenLibs, grabLibs, installingLibs, installingLibs, versionBuilder, settings, selectedJavaRuntime));
            examineVersionFile.addTask(new EnsureAssetsIndexTask(directories.getAssetsDirectory(), pack, installingMinecraft, examineIndex, verifyingAssets, installingAssets, installingAssets));

            fetchJavaManifest.addTask(new EnsureJavaRuntimeManifestTask(directories.getRuntimesDirectory(), pack, fetchJavaManifest, examineJava, downloadJava));

            // Check if we need to regenerate the Minecraft jar. This is necessary if:
            // - A reinstall was requested (or forced, via modpack version update)
            // - The installed version is marked as legacy
            boolean jarRegenerationRequired = doFullInstall || (installedVersion != null && installedVersion.isLegacy());

            installingMinecraft.addTask(new InstallMinecraftIfNecessaryTask(pack, minecraft, directories.getCacheDirectory(), jarRegenerationRequired));
        }

        private MojangVersionBuilder createVersionBuilder(InstallTasksQueue<MojangVersion> tasksQueue) {
            ZipFileRetriever zipVersionRetriever = new ZipFileRetriever(new File(pack.getBinDir(), "modpack.jar"));
            HttpFileRetriever fallbackVersionRetriever = new HttpFileRetriever(TechnicConstants.VERSIONS_BASE_URL, tasksQueue.getDownloadListener());

            ArrayList<MojangVersionRetriever> fallbackRetrievers = new ArrayList<>(1);
            fallbackRetrievers.add(fallbackVersionRetriever);

            File versionJson = new File(pack.getBinDir(), "version.json");

            // This always gets the version.json from the modpack.jar (it ignores "key"), cached as bin/version.json
            FileVersionBuilder zipVersionBuilder = new FileVersionBuilder(versionJson, zipVersionRetriever, fallbackRetrievers);
            // This gets the "key" from bin/$key.json if it exists, otherwise it downloads it from our repo into that location
            FileVersionBuilder webVersionBuilder = new FileVersionBuilder(pack.getBinDir(), null, fallbackRetrievers);

            return new ChainVersionBuilder(zipVersionBuilder, webVersionBuilder);
        }
    }
}
