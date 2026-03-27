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
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.logging.Level;
import java.util.zip.ZipException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.components.FixRunDataDialog;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.*;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.launchercore.install.ModpackVersion;
import net.technicpack.launchercore.install.plan.ExecutionPlan;
import net.technicpack.launchercore.install.plan.PlanExecutor;
import net.technicpack.launchercore.launch.GameProcess;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.RunData;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.progress.ExecutionProgressListener;
import net.technicpack.launchercore.progress.ExecutionProgressListeners;
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
import net.technicpack.utilslib.Utils;

public class Installer {
  protected final ModpackInstaller installer;
  protected final MinecraftLauncher launcher;
  protected final TechnicSettings settings;
  protected final PackResourceMapper packIconMapper;
  protected final StartupParameters startupParameters;
  protected final LauncherFileSystem fileSystem;
  protected volatile boolean isCancelledByUser = false;

  private Thread installerThread;
  private GameProcess gameProcess;
  private final Object gameLock = new Object();

  public Installer(
      StartupParameters startupParameters,
      LauncherFileSystem fileSystem,
      ModpackInstaller installer,
      MinecraftLauncher launcher,
      TechnicSettings settings,
      PackResourceMapper packIconMapper) {
    this.installer = installer;
    this.launcher = launcher;
    this.settings = settings;
    this.packIconMapper = packIconMapper;
    this.startupParameters = startupParameters;
    this.fileSystem = fileSystem;
  }

  public void cancel() {
    Utils.getLogger().info("User pressed cancel button.");
    isCancelledByUser = true;
    installerThread.interrupt();
  }

  public void justInstall(
      final ResourceLoader resources,
      final ModpackModel pack,
      final String build,
      final boolean doFullInstall,
      final LauncherFrame frame,
      final DownloadListener listener) {
    internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, false);
  }

  public void installAndRun(
      final ResourceLoader resources,
      final ModpackModel pack,
      final String build,
      final boolean doFullInstall,
      final LauncherFrame frame,
      final DownloadListener listener) {
    internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, true);
  }

  protected void internalInstallAndRun(
      final ResourceLoader resources,
      final ModpackModel pack,
      final String build,
      final boolean doFullInstall,
      final LauncherFrame frame,
      final DownloadListener listener,
      final boolean doLaunch) {
    // Ensure that the installer is not currently running
    if (isCurrentlyRunning()) {
      Utils.getLogger().warning("Installer is already running, ignoring install request.");
      return;
    }

    // Reset cancel flag
    isCancelledByUser = false;

    // Create and start the installer thread
    installerThread =
        new InstallerThread(listener, pack, build, resources, doFullInstall, doLaunch, frame);
    installerThread.start();
  }

  public boolean isCurrentlyRunning() {
    return installerThread != null && installerThread.isAlive();
  }

  public boolean isGameProcessRunning() {
    synchronized (gameLock) {
      return gameProcess != null
          && gameProcess.getProcess() != null
          && gameProcess.getProcess().isAlive();
    }
  }

  Modpack preparePackForInstall(ModpackModel pack, String build)
      throws InstallException, BuildInaccessibleException {
    PackInfo packInfo = pack.getPackInfo();
    if (packInfo == null) {
      throw new InstallException("No modpack information found, cannot install or launch modpack.");
    }

    Modpack modpackData = packInfo.getModpack(build);
    if (modpackData.getGameVersion() == null) {
      throw new InstallException(
          "No game version found for modpack, cannot install or launch modpack.");
    }

    installer.preparePack(pack);
    return modpackData;
  }

  private class InstallerThread extends Thread {
    private final DownloadListener listener;
    private final ModpackModel pack;
    private final String build;
    private final ResourceLoader resources;
    private final boolean doFullInstall;
    private final boolean doLaunch;
    private final LauncherFrame frame;

    public InstallerThread(
        DownloadListener listener,
        ModpackModel pack,
        String build,
        ResourceLoader resources,
        boolean doFullInstall,
        boolean doLaunch,
        LauncherFrame frame) {
      super("InstallerThread");
      setDaemon(true);

      this.listener = listener;
      this.pack = pack;
      this.build = build;
      this.resources = resources;
      this.doFullInstall = doFullInstall;
      this.doLaunch = doLaunch;
      this.frame = frame;

      Utils.getLogger()
          .info(
              String.format("Starting installer thread for %s (%s)", pack.getDisplayName(), build));
      Sentry.addBreadcrumb(
          String.format("Starting installer thread for %s (%s)", pack.getDisplayName(), build));
    }

    @Override
    public void run() {
      setGameProcess(null);

      try {
        ExecutionProgressListener progressListener = ExecutionProgressListeners.adapt(listener);
        MinecraftVersionInfoBuilder versionBuilder = createVersionBuilder(listener);
        JavaVersionRepository javaVersions = launcher.getJavaVersions();

        final boolean mojangJavaWanted = settings.shouldUseMojangJava();

        IMinecraftVersionInfo version;
        Modpack modpackData = preparePackForInstall(pack, build);

        ModpackVersion installedVersion = pack.getInstalledVersion();
        boolean jarRegenerationRequired =
            doFullInstall || (installedVersion != null && installedVersion.isLegacy());

        ImmutableInstallerPlanner planner =
            new ImmutableInstallerPlanner(
                resources,
                pack,
                modpackData,
                fileSystem,
                versionBuilder,
                settings,
                javaVersions.getSelectedVersion(),
                doFullInstall,
                mojangJavaWanted,
                jarRegenerationRequired,
                () -> isCancelledByUser);
        ImmutableInstallerPlanner.InstallExecutionContext context =
            new ImmutableInstallerPlanner.InstallExecutionContext();
        PlanExecutor<ImmutableInstallerPlanner.InstallExecutionContext> executor =
            new PlanExecutor<>(progressListener);

        executePlan(executor, planner.buildPreparationPlan(), context);
        executePlan(executor, planner.buildVersionDiscoveryPlan(), context);
        executePlan(executor, planner.buildInstallPlan(context), context);

        version = context.getResolvedVersion();
        installer.completeInstall(pack, build, installedVersion);

        if (doLaunch) {
          if (version == null) {
            throw new PackNotAvailableOfflineException(pack.getDisplayName());
          }

          boolean usingMojangJava =
              mojangJavaWanted && version.getMojangRuntimeInformation() != null;

          Memory memoryObj = getLaunchMemory(settings, javaVersions.getSelectedVersion().is64Bit());
          long memory = memoryObj.getMemoryMB();
          String versionNumber = javaVersions.getSelectedVersion().getVersion();
          RunData data = pack.getRunData();

          if (data != null && !data.isRunDataValid(memory, versionNumber, usingMojangJava)) {
            FixRunDataDialog dialog =
                new FixRunDataDialog(
                    frame,
                    resources,
                    data,
                    javaVersions,
                    memoryObj,
                    !settings.shouldAutoAcceptModpackRequirements(),
                    usingMojangJava);
            dialog.setVisible(true);
            if (dialog.getResult() == FixRunDataDialog.Result.APPLY) {
              memoryObj = dialog.getRecommendedMemory();
              memory = memoryObj.getMemoryMB();
              settings.setMemory(memoryObj.getSettingsId());

              IJavaRuntime recommendedJavaVersion = dialog.getRecommendedJavaVersion();
              if (recommendedJavaVersion != null) {
                javaVersions.selectVersion(
                    recommendedJavaVersion.getVersion(), recommendedJavaVersion.is64Bit());
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
            int result =
                JOptionPane.showConfirmDialog(
                    frame,
                    resources.getString("launcher.jverwarning", versionNumber),
                    resources.getString("launcher.jverwarning.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
              return;
            }
          }

          LaunchAction launchAction = settings.getLaunchAction();

          LauncherUnhider launcherUnhider;
          if (launchAction == LaunchAction.HIDE) {
            launcherUnhider = new LauncherUnhider(settings, frame);
          } else {
            launcherUnhider = null;
          }

          LaunchOptions options =
              new LaunchOptions(
                  pack.getDisplayName(),
                  packIconMapper.getImageLocation(pack).getAbsolutePath(),
                  settings);
          setGameProcess(launcher.launch(pack, memory, options, launcherUnhider, version));

          switch (launchAction) {
            case HIDE:
              frame.setVisible(false);
              break;
            case NOTHING:
              SwingUtilities.invokeLater(frame::launchCompleted);
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
            resources.getString(
                "launcher.installerror.download", pack.getDisplayName(), e.getMessage()));
      } catch (ZipException e) {
        showErrorDialog(
            resources.getString(
                "launcher.installerror.unzip", pack.getDisplayName(), e.getMessage()));
      } catch (CacheDeleteException e) {
        showErrorDialog(
            resources.getString(
                "launcher.installerror.cache", pack.getDisplayName(), e.getMessage()));
      } catch (InstallException | BuildInaccessibleException e) {
        Utils.getLogger()
            .log(Level.SEVERE, "Exception caught during modpack installation or launch.", e);
        showErrorDialog(e.getMessage());
      } catch (Exception e) {
        Utils.getLogger()
            .log(Level.SEVERE, "Exception caught during modpack installation or launch.", e);
        // Provide a more friendly error message if the OS blocks a process from being launched.
        // This error message always contains "CreateProcess error=5," on Windows.
        if (e instanceof IOException && isCreateProcessAccessDenied((IOException) e)) {
          showErrorDialog(
              resources.getString("process.error.accessdenied", e.getLocalizedMessage()));
        } else {
          Sentry.captureException(e);
          showErrorDialog(String.format("Unknown error: %s", e.getMessage()));
        }
      } finally {
        if (!doLaunch || !isGameProcessRunning()) {
          SwingUtilities.invokeLater(frame::launchCompleted);
        }
      }
    }

    private void setGameProcess(GameProcess gameProcess) {
      synchronized (gameLock) {
        Installer.this.gameProcess = gameProcess;
      }
    }

    private void showErrorDialog(String title, String message) {
      SwingUtilities.invokeLater(
          () -> JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE));
    }

    private void showErrorDialog(String message) {
      showErrorDialog(resources.getString("launcher.installerror.title"), message);
    }

    private void executePlan(
        PlanExecutor<ImmutableInstallerPlanner.InstallExecutionContext> executor,
        ExecutionPlan<ImmutableInstallerPlanner.InstallExecutionContext> plan,
        ImmutableInstallerPlanner.InstallExecutionContext context)
        throws IOException, InterruptedException {
      executor.execute(plan, context);
    }

    private MinecraftVersionInfoBuilder createVersionBuilder(DownloadListener listener) {
      ZipMinecraftVersionInfoRetriever zipVersionRetriever =
          new ZipMinecraftVersionInfoRetriever(new File(pack.getBinDir(), "modpack.jar"));
      HttpMinecraftVersionInfoRetriever fallbackVersionRetriever =
          new HttpMinecraftVersionInfoRetriever(TechnicConstants.VERSIONS_BASE_URL, listener);

      java.util.ArrayList<MinecraftVersionInfoRetriever> fallbackRetrievers =
          new java.util.ArrayList<>(1);
      fallbackRetrievers.add(fallbackVersionRetriever);

      File versionJson = new File(pack.getBinDir(), "version.json");

      // This always gets the version.json from the modpack.jar (it ignores "key"), cached as
      // bin/version.json
      FileMinecraftVersionInfoBuilder zipVersionBuilder =
          new FileMinecraftVersionInfoBuilder(versionJson, zipVersionRetriever, fallbackRetrievers);
      // This gets the "key" from bin/$key.json if it exists, otherwise it downloads it from our
      // repo into that location
      FileMinecraftVersionInfoBuilder webVersionBuilder =
          new FileMinecraftVersionInfoBuilder(pack.getBinDir(), null, fallbackRetrievers);

      return new ChainedMinecraftVersionInfoBuilder(zipVersionBuilder, webVersionBuilder);
    }
  }

  static boolean isCreateProcessAccessDenied(IOException exception) {
    String message = exception.getMessage();
    return message != null && message.contains("CreateProcess error=5,");
  }

  static Memory getLaunchMemory(TechnicSettings settings, boolean is64Bit) {
    return getLaunchMemory(settings, Memory.getAvailableMemory(is64Bit));
  }

  static Memory getLaunchMemory(TechnicSettings settings, long availableMemory) {
    Memory requestedMemory = Memory.getMemoryFromId(settings.getMemory());
    Memory launchMemory = Memory.getClosestAvailableMemory(requestedMemory, availableMemory);

    if (launchMemory.getMemoryMB() != requestedMemory.getMemoryMB()) {
      Utils.getLogger()
          .warning(
              String.format(
                  "Clamping launch memory from %s to %s because only %d MB is available.",
                  requestedMemory, launchMemory, availableMemory));
    }

    return launchMemory;
  }
}
