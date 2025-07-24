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
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.components.FixRunDataDialog;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.*;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.launchercore.install.ModpackVersion;
import net.technicpack.launchercore.install.tasks.CheckRunDataFile;
import net.technicpack.launchercore.install.tasks.TaskGroup;
import net.technicpack.launchercore.install.tasks.WriteRundataFile;
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
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.MinecraftVersionInfoBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.FileMinecraftVersionInfoBuilder;
import net.technicpack.minecraftcore.mojang.version.builder.MinecraftVersionInfoRetriever;
import net.technicpack.minecraftcore.mojang.version.builder.retrievers.HttpMinecraftVersionInfoRetriever;
import net.technicpack.minecraftcore.mojang.version.builder.retrievers.ZipMinecraftVersionInfoRetriever;
import net.technicpack.minecraftcore.mojang.version.chain.ChainedMinecraftVersionInfoBuilder;
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
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.ZipException;

public class Installer {
    protected final ModpackInstaller installer;
    protected final MinecraftLauncher launcher;
    protected final TechnicSettings settings;
    protected final PackResourceMapper packIconMapper;
    protected final StartupParameters startupParameters;
    protected final LauncherFileSystem fileSystem;
    protected final Object cancelLock = new Object();
    protected volatile boolean isCancelledByUser = false;

    private Thread installerThread;
    private volatile LauncherUnhider launcherUnhider;
    private volatile GameProcess gameProcess;

    public Installer(StartupParameters startupParameters, LauncherFileSystem fileSystem, ModpackInstaller installer,
                     MinecraftLauncher launcher, TechnicSettings settings, PackResourceMapper packIconMapper) {
        this.installer = installer;
        this.launcher = launcher;
        this.settings = settings;
        this.packIconMapper = packIconMapper;
        this.startupParameters = startupParameters;
        this.fileSystem = fileSystem;
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
            Sentry.addBreadcrumb(String.format("Starting installer thread for %s (%s)", pack.getDisplayName(), build));
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
                InstallTasksQueue<IMinecraftVersionInfo> tasksQueue = new InstallTasksQueue<>(listener);
                MinecraftVersionInfoBuilder versionBuilder = createVersionBuilder(tasksQueue);
                JavaVersionRepository javaVersions = launcher.getJavaVersions();

                final boolean mojangJavaWanted = settings.shouldUseMojangJava();

                IMinecraftVersionInfo version;

                buildTasksQueue(tasksQueue, build, versionBuilder, javaVersions.getSelectedVersion(), mojangJavaWanted);

                version = installer.installPack(tasksQueue, pack, build);

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
                        if (dialog.getResult() == FixRunDataDialog.Result.APPLY) {
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

                    if (launchAction == LaunchAction.HIDE) {
                        launcherUnhider = new LauncherUnhider(settings, frame);
                    } else {
                        launcherUnhider = null;
                    }

                    LaunchOptions options = new LaunchOptions(pack.getDisplayName(),
                            packIconMapper.getImageLocation(pack).getAbsolutePath(), settings);
                    gameProcess = launcher.launch(pack, memory, options, launcherUnhider, version);

                    switch (launchAction) {
                        case HIDE:
                            frame.setVisible(false);
                            break;
                        case NOTHING:
                            EventQueue.invokeLater(frame::launchCompleted);
                            break;
                        case CLOSE:
                            System.exit(0);
                    }
                }
            } catch (ClosedByInterruptException | InterruptedException e) {
                if (isCancelledByUser) {
                    Utils.getLogger().info("Cancelled by user.");
                } else {
                    if (e.getCause() != null) {
                        Utils.getLogger().log(Level.INFO, "Cancelled by exception.", e);
                    } else {
                        Utils.getLogger().log(Level.INFO, "Cancelled by code.");
                    }
                    Sentry.captureException(e);
                }

                this.interrupt();
            } catch (PackNotAvailableOfflineException e) {
                showErrorDialog(resources.getString("launcher.installerror.unavailable"), e.getMessage());
            } catch (DownloadException e) {
                showErrorDialog(
                    resources.getString("launcher.installerror.download", pack.getDisplayName(), e.getMessage())
                );
            } catch (ZipException e) {
                showErrorDialog(
                    resources.getString("launcher.installerror.unzip", pack.getDisplayName(), e.getMessage())
                );
            } catch (CacheDeleteException e) {
                showErrorDialog(
                    resources.getString("launcher.installerror.cache", pack.getDisplayName(), e.getMessage())
                );
            } catch (InstallException | BuildInaccessibleException e) {
                Utils.getLogger().log(Level.SEVERE, "Exception caught during modpack installation or launch.", e);
                showErrorDialog(e.getMessage());
            } catch (Exception e) {
                Utils.getLogger().log(Level.SEVERE, "Exception caught during modpack installation or launch.", e);
                Sentry.captureException(e);
                showErrorDialog(String.format("Unknown error: %s", e.getMessage()));
            } finally {
                if (!doLaunch || !isGameProcessRunning()) {
                    EventQueue.invokeLater(frame::launchCompleted);
                }
            }
        }

        private void showErrorDialog(String title, String message) {
            EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE));
        }

        private void showErrorDialog(String message) {
            showErrorDialog(resources.getString("launcher.installerror.title"), message);
        }

        private void buildTasksQueue(InstallTasksQueue<IMinecraftVersionInfo> queue, String build,
                                     MinecraftVersionInfoBuilder versionBuilder, IJavaRuntime selectedJavaRuntime,
                                     boolean mojangJavaWanted) throws IOException, InstallException {
            PackInfo packInfo = pack.getPackInfo();

            // Abort modpack install/launch if we don't have the necessary information.
            // This can happen if a modpack is local-only and doesn't have cached information.
            if (packInfo == null) {
                throw new InstallException("No modpack information found, cannot install or launch modpack.");
            }

            Modpack modpackData = packInfo.getModpack(build);

            if (modpackData.getGameVersion() == null) {
                throw new InstallException("No game version found for modpack, cannot install or launch modpack.");
            }

            String minecraft = modpackData.getGameVersion();
            ModpackVersion installedVersion = pack.getInstalledVersion();

            TaskGroup<IMinecraftVersionInfo> examineModpackData = new TaskGroup<>(resources.getString("install.message.examiningmodpack"));
            TaskGroup<IMinecraftVersionInfo> verifyingFiles = new TaskGroup<>(resources.getString("install.message.verifyingfiles"));
            TaskGroup<IMinecraftVersionInfo> downloadingMods = new TaskGroup<>(resources.getString("install.message.downloadmods"));
            TaskGroup<IMinecraftVersionInfo> installingMods = new TaskGroup<>(resources.getString("install.message.installmods"));
            TaskGroup<IMinecraftVersionInfo> checkVersionFile = new TaskGroup<>(resources.getString("install.message.checkversionfile"));
            TaskGroup<IMinecraftVersionInfo> installVersionFile = new TaskGroup<>(resources.getString("install.message.installversionfile"));
            TaskGroup<IMinecraftVersionInfo> rundataTaskGroup = new TaskGroup<>(resources.getString("install.message.runData"));
            TaskGroup<IMinecraftVersionInfo> examineVersionFile = new TaskGroup<>(resources.getString("install.message.examiningversionfile"));
            TaskGroup<IMinecraftVersionInfo> grabLibs = new TaskGroup<>(resources.getString("install.message.grablibraries"));
            TaskGroup<IMinecraftVersionInfo> checkNonMavenLibs = new TaskGroup<>(resources.getString("install.message.nonmavenlibs"));
            TaskGroup<IMinecraftVersionInfo> installingLibs = new TaskGroup<>(resources.getString("install.message.installlibs"));
            TaskGroup<IMinecraftVersionInfo> installingMinecraft = new TaskGroup<>(resources.getString("install.message.installminecraft"));
            TaskGroup<IMinecraftVersionInfo> examineIndex = new TaskGroup<>(resources.getString("install.message.examiningindex"));
            TaskGroup<IMinecraftVersionInfo> verifyingAssets = new TaskGroup<>(resources.getString("install.message.verifyassets"));
            TaskGroup<IMinecraftVersionInfo> installingAssets = new TaskGroup<>(resources.getString("install.message.installassets"));
            TaskGroup<IMinecraftVersionInfo> fetchJavaManifest = new TaskGroup<>("Obtaining Java runtime information...");
            TaskGroup<IMinecraftVersionInfo> examineJava = new TaskGroup<>("Examining Java runtime...");
            TaskGroup<IMinecraftVersionInfo> downloadJava = new TaskGroup<>("Downloading Java runtime...");

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

            verifyingFiles.addTask(new InstallFmlLibsTask(pack, fileSystem, modpackData, verifyingFiles, installingLibs, installingLibs));

            checkVersionFile.addTask(new VerifyVersionFilePresentTask(pack, minecraft, versionBuilder));
            examineVersionFile.addTask(new HandleVersionFileTask().withPack(pack)
                                                                  .withFileSystem(fileSystem)
                                                                  .withCheckNonMavenLibsQueue(checkNonMavenLibs)
                                                                  .withCheckLibraryQueue(grabLibs)
                                                                  .withDownloadLibraryQueue(installingLibs)
                                                                  .withCopyLibraryQueue(installingLibs)
                                                                  .withVersionBuilder(versionBuilder)
                                                                  .withLaunchOptions(settings)
                                                                  .withJavaRuntime(selectedJavaRuntime));
            examineVersionFile.addTask(new EnsureAssetsIndexTask(fileSystem.getAssetsDirectory(), pack, installingMinecraft, examineIndex, verifyingAssets, installingAssets, installingAssets));

            fetchJavaManifest.addTask(new EnsureJavaRuntimeManifestTask(fileSystem.getRuntimesDirectory(), pack, fetchJavaManifest, examineJava, downloadJava));

            // Check if we need to regenerate the Minecraft jar. This is necessary if:
            // - A reinstall was requested (or forced, via modpack version update)
            // - The installed version is marked as legacy
            boolean jarRegenerationRequired = doFullInstall || (installedVersion != null && installedVersion.isLegacy());

            installingMinecraft.addTask(new InstallMinecraftIfNecessaryTask(pack, minecraft, fileSystem.getCacheDirectory(), jarRegenerationRequired));
        }

        private MinecraftVersionInfoBuilder createVersionBuilder(InstallTasksQueue<IMinecraftVersionInfo> tasksQueue) {
            ZipMinecraftVersionInfoRetriever zipVersionRetriever = new ZipMinecraftVersionInfoRetriever(new File(pack.getBinDir(), "modpack.jar"));
            HttpMinecraftVersionInfoRetriever fallbackVersionRetriever = new HttpMinecraftVersionInfoRetriever(TechnicConstants.VERSIONS_BASE_URL, tasksQueue.getDownloadListener());

            ArrayList<MinecraftVersionInfoRetriever> fallbackRetrievers = new ArrayList<>(1);
            fallbackRetrievers.add(fallbackVersionRetriever);

            File versionJson = new File(pack.getBinDir(), "version.json");

            // This always gets the version.json from the modpack.jar (it ignores "key"), cached as bin/version.json
            FileMinecraftVersionInfoBuilder zipVersionBuilder = new FileMinecraftVersionInfoBuilder(versionJson, zipVersionRetriever, fallbackRetrievers);
            // This gets the "key" from bin/$key.json if it exists, otherwise it downloads it from our repo into that location
            FileMinecraftVersionInfoBuilder webVersionBuilder = new FileMinecraftVersionInfoBuilder(pack.getBinDir(), null, fallbackRetrievers);

            return new ChainedMinecraftVersionInfoBuilder(zipVersionBuilder, webVersionBuilder);
        }
    }
}
