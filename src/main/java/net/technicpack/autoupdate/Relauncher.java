/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.autoupdate;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import net.technicpack.launcher.LauncherMain;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.launchercore.install.plan.ExecutionPlan;
import net.technicpack.launchercore.install.plan.PlanExecutor;
import net.technicpack.ui.controls.installation.SplashScreen;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.ProcessUtils;
import net.technicpack.utilslib.Utils;

public class Relauncher {
  private static final String LAUNCHER_ASSET_PROGRESS_TEMPLATE = "%s";
  private static final int WINDOWS_PACKAGE_REPLACE_ATTEMPTS = 20;
  private static final long WINDOWS_PACKAGE_RETRY_DELAY_MILLIS = 250L;

  private final String stream;
  private final int currentBuild;
  private final LauncherFileSystem fileSystem;
  protected ResourceLoader resources;
  protected StartupParameters parameters;
  protected IUpdateStream updateStream;
  private boolean didUpdate = false;
  private SplashScreen screen = null;

  public Relauncher(
      IUpdateStream updateStream,
      String stream,
      int currentBuild,
      LauncherFileSystem fileSystem,
      ResourceLoader resources,
      StartupParameters parameters) {
    this.stream = stream;
    this.currentBuild = currentBuild;
    this.fileSystem = fileSystem;
    this.resources = resources;
    this.parameters = parameters;
    this.updateStream = updateStream;
  }

  public int getCurrentBuild() {
    return currentBuild;
  }

  public String getStreamName() {
    return stream;
  }

  public void setUpdated() {
    didUpdate = true;
  }

  protected LauncherFileSystem getFileSystem() {
    return fileSystem;
  }

  public String getRunningPath() {
    return getRunningPath(getMainClass());
  }

  @SuppressWarnings({"java:S106", "java:S4507"})
  public static String getRunningPath(Class<?> clazz) {
    try {
      URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
      return Paths.get(uri).toString();
    } catch (URISyntaxException e) {
      // This should never happen, but this is here just in case it does
      System.err.println("Failed to get running path for class: " + clazz.getName());
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
      System.exit(255);
      return null; // Unreachable but required for compilation
    }
  }

  protected Class<?> getMainClass() {
    return LauncherMain.class;
  }

  public String getUpdateText() {
    return resources.getString("updater.launcherupdate");
  }

  public boolean isUpdateOnly() {
    return parameters.isUpdate();
  }

  public boolean isMover() {
    return (parameters.isMover() || parameters.isLegacyMover()) && !parameters.isLegacyLauncher();
  }

  public boolean isLauncherOnly() {
    return parameters.isLauncher();
  }

  public boolean isSkipUpdate() {
    return parameters.isSkipUpdate();
  }

  public ExecutionPlan<UpdatePlanner.UpdateContext> buildMoverPlan() {
    UpdatePlanner planner = new UpdatePlanner(fileSystem);
    return planner.planMover(
        new UpdatePlanner.MoverPlanRequest(
            Paths.get(parameters.getMoveTarget()),
            parameters.isLegacyMover(),
            resources.getString("updater.mover"),
            resources.getString("updater.finallaunch")));
  }

  public ExecutionPlan<UpdatePlanner.UpdateContext> buildUpdaterPlan() throws IOException {
    screen = new SplashScreen(resources.getImage("launch_splash.png"), 30);
    Color bg = UIConstants.COLOR_FORM_ELEMENT_INTERNAL;
    screen.getContentPane().setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 255));
    screen.getProgressDisplay().getOverallProgressBar().setForeground(Color.white);
    screen.getProgressDisplay().getOverallProgressBar().setBackground(UIConstants.COLOR_GREEN);
    screen
        .getProgressDisplay()
        .getOverallProgressBar()
        .setBackFill(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
    screen
        .getProgressDisplay()
        .getOverallProgressBar()
        .setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
    screen
        .getProgressDisplay()
        .getCurrentItemCaptionLabel()
        .setForeground(UIConstants.COLOR_DIM_TEXT);
    screen
        .getProgressDisplay()
        .getCurrentItemCaptionLabel()
        .setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 10));
    screen.getProgressDisplay().getCurrentItemLabel().setForeground(Color.white);
    screen
        .getProgressDisplay()
        .getCurrentItemLabel()
        .setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 11));
    screen
        .getProgressDisplay()
        .getCurrentItemProgressBar()
        .setTrackColor(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
    screen.getProgressDisplay().getCurrentItemProgressBar().setFillColor(UIConstants.COLOR_SERVER);
    screen
        .getProgressDisplay()
        .getCurrentItemProgressBar()
        .setOutlineColor(new Color(121, 133, 145, 180));
    screen.getProgressDisplay().overallChanged(getUpdateText(), 0.0f);
    screen.pack();
    screen.setLocationRelativeTo(null);
    screen.setVisible(true);

    UpdatePlanner planner = new UpdatePlanner(fileSystem);
    return planner.planUpdater(
        updateStream,
        new UpdatePlanner.UpdatePlanRequest(
            stream,
            currentBuild,
            isSkipUpdate(),
            isUpdateOnly(),
            getRunningPath(),
            getTempLauncher().toPath(),
            getUpdateText(),
            resources.getString("updater.launchmover"),
            LAUNCHER_ASSET_PROGRESS_TEMPLATE,
            getUpdateText()));
  }

  /**
   * Returns the arguments to be used when relaunching the updater. This includes the original
   * arguments plus an additional "-blockReboot" argument.
   *
   * @return An array of strings representing the launch arguments.
   */
  public List<String> getRelaunchArgs() {
    List<String> args = parameters.getArgs();
    List<String> launchArgs = new ArrayList<>(args.size() + 1);
    launchArgs.addAll(args);
    if (!launchArgs.contains("-blockReboot")) {
      launchArgs.add("-blockReboot");
    }
    return launchArgs;
  }

  public void updateComplete() {
    screen.dispose();
  }

  public boolean canReboot() {
    return !parameters.isBlockReboot();
  }

  public boolean runAutoUpdater() throws IOException, InterruptedException {
    if (isLauncherOnly()) return true;

    boolean needsReboot = false;

    if (canReboot()) {
      if (System.getProperty("awt.useSystemAAFontSettings") == null
          || !System.getProperty("awt.useSystemAAFontSettings").equals("lcd")) needsReboot = true;
      else if (!Boolean.parseBoolean(System.getProperty("java.net.preferIPv4Stack")))
        needsReboot = true;
    }

    ExecutionPlan<UpdatePlanner.UpdateContext> updatePlan;
    if (isMover()) {
      updatePlan = buildMoverPlan();
    } else if (needsReboot && getCurrentBuild() > 0) {
      relaunch();
      return false;
    } else if (getCurrentBuild() < 1) {
      return true;
    } else {
      updatePlan = buildUpdaterPlan();
    }

    if (updatePlan == null) return true;

    new PlanExecutor<UpdatePlanner.UpdateContext>(
            screen == null ? null : screen.getProgressDisplay())
        .execute(updatePlan, new UpdatePlanner.UpdateContext(this));
    updateComplete();

    return !didUpdate && !isUpdateOnly();
  }

  public void relaunch() {
    launch(null, getRelaunchArgs());
  }

  public File getTempLauncher() {
    String runningPath = getRunningPath();

    String extension = runningPath.endsWith(".exe") ? "exe" : "jar";

    Path destPath = fileSystem.getRootDirectory().resolve(String.format("temp.%s", extension));

    return destPath.toFile();
  }

  public void copyToMoveTarget(Path targetPath) throws IOException {
    Path currentPath = Paths.get(getRunningPath());

    Utils.getLogger()
        .info(String.format("Copying running package from %s to %s", currentPath, targetPath));

    if (currentPath.equals(targetPath)) {
      throw new IOException("Source and destination paths are the same!");
    }

    try {
      replaceLauncherPackage(currentPath, targetPath);
    } catch (IOException e) {
      Utils.getLogger().log(Level.SEVERE, "Error copying package", e);
      throw e;
    }
  }

  static void replaceLauncherPackage(Path currentPath, Path targetPath) throws IOException {
    replaceLauncherPackage(
        currentPath,
        targetPath,
        OperatingSystem.getOperatingSystem() == OperatingSystem.WINDOWS,
        Relauncher::replaceLauncherPackageOnce,
        Thread::sleep);
  }

  static void replaceLauncherPackage(
      Path currentPath,
      Path targetPath,
      boolean retryTargetLocks,
      ReplaceOperation replaceOperation,
      PauseOperation pauseOperation)
      throws IOException {
    int attempts = retryTargetLocks ? WINDOWS_PACKAGE_REPLACE_ATTEMPTS : 1;

    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        replaceOperation.execute(currentPath, targetPath);
        return;
      } catch (FileSystemException e) {
        if (!shouldRetryLauncherPackageReplace(
            e, targetPath, retryTargetLocks, attempt, attempts)) {
          throw e;
        }

        Utils.getLogger()
            .log(
                Level.WARNING,
                String.format(
                    "Failed to replace launcher package at %s on attempt %d/%d; retrying",
                    targetPath, attempt, attempts),
                e);

        waitForLauncherPackageRetry(pauseOperation, targetPath, attempt, attempts, e);
      }
    }
  }

  private static void replaceLauncherPackageOnce(Path currentPath, Path targetPath)
      throws IOException {
    Path parent = targetPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    Files.copy(currentPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

    File targetFile = targetPath.toFile();
    if (!targetFile.setExecutable(true, true)) {
      Utils.getLogger().warning("Failed to set executable flag on package");
    }
  }

  private static boolean shouldRetryLauncherPackageReplace(
      FileSystemException exception,
      Path targetPath,
      boolean retryTargetLocks,
      int attempt,
      int attempts) {
    if (!retryTargetLocks || attempt >= attempts) {
      return false;
    }

    String targetPathString = targetPath.toString();
    return targetPathString.equals(exception.getFile())
        || targetPathString.equals(exception.getOtherFile());
  }

  private static void waitForLauncherPackageRetry(
      PauseOperation pauseOperation,
      Path targetPath,
      int attempt,
      int attempts,
      FileSystemException cause)
      throws IOException {
    try {
      pauseOperation.pause(WINDOWS_PACKAGE_RETRY_DELAY_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      IOException interrupted =
          new IOException(
              String.format(
                  "Interrupted while waiting to replace launcher package at %s after attempt %d/%d",
                  targetPath, attempt, attempts),
              e);
      interrupted.addSuppressed(cause);
      throw interrupted;
    }
  }

  @FunctionalInterface
  interface ReplaceOperation {
    void execute(Path currentPath, Path targetPath) throws IOException;
  }

  @FunctionalInterface
  interface PauseOperation {
    void pause(long millis) throws InterruptedException;
  }

  public void launch(String launchPath, List<String> args) {
    if (launchPath == null) {
      launchPath = getRunningPath();
    }

    ArrayList<String> commands = getBaseCommands(launchPath);
    commands.addAll(args);

    String commandString = String.join(" ", commands);

    Utils.getLogger().info(String.format("Launching command: '%s'", commandString));

    ProcessBuilder pb = ProcessUtils.createProcessBuilder(commands, true);

    try {
      pb.start();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
          null,
          "Your OS has prevented this relaunch from completing.  You may need to add an exception in your security software.",
          "Relaunch Failed",
          JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.exit(0);
  }

  private ArrayList<String> getBaseCommands(String launchPath) {
    ArrayList<String> commands = new ArrayList<>();
    if (!launchPath.endsWith(".exe")) {
      commands.add(OperatingSystem.getJavaDir());
      commands.add("-Djava.net.preferIPv4Stack=true");
      commands.add("-Dawt.useSystemAAFontSettings=lcd");
      commands.add("-Dswing.aatext=true");
      commands.add("-cp");
      commands.add(launchPath);
      commands.add(getMainClass().getName());
    } else commands.add(launchPath);
    return commands;
  }

  public List<String> buildMoverArgs() {
    List<String> outArgs = new ArrayList<>();
    outArgs.add("-movetarget");
    outArgs.add(getRunningPath());
    outArgs.add("-moveronly");
    outArgs.addAll(getRelaunchArgs());
    return outArgs;
  }

  public List<String> buildLauncherArgs(boolean isLegacy) {
    List<String> outArgs = new ArrayList<>();
    if (!isLegacy) outArgs.add("-launcheronly");
    else outArgs.add("-launcher");

    Iterator<String> it = getRelaunchArgs().iterator();
    while (it.hasNext()) {
      String arg = it.next();
      // ignore -movetarget
      if (arg.equals("-movetarget")) {
        // change to the next item (the -movetarget value) and also ignore that
        if (it.hasNext()) {
          it.next();
        }
      } else if (!arg.equals("-moveronly")) {
        // ignore -moveronly
        outArgs.add(arg);
      }
    }

    return outArgs;
  }
}
