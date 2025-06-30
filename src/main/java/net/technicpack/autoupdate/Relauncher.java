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

import net.technicpack.autoupdate.tasks.*;
import net.technicpack.launcher.LauncherMain;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.tasks.TaskGroup;
import net.technicpack.ui.controls.installation.SplashScreen;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.ProcessUtils;
import net.technicpack.utilslib.Utils;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Relauncher {

    private final String stream;
    private final int currentBuild;
    private final LauncherDirectories directories;
    protected ResourceLoader resources;
    protected StartupParameters parameters;
    protected IUpdateStream updateStream;
    private boolean didUpdate = false;
    private SplashScreen screen = null;

    public Relauncher(IUpdateStream updateStream, String stream, int currentBuild, LauncherDirectories directories, ResourceLoader resources, StartupParameters parameters) {
        this.stream = stream;
        this.currentBuild = currentBuild;
        this.directories = directories;
        this.resources = resources;
        this.parameters = parameters;
        this.updateStream = updateStream;
    }

    public int getCurrentBuild() { return currentBuild; }
    public String getStreamName() { return stream; }
    public void setUpdated() { didUpdate = true; }

    protected LauncherDirectories getDirectories() { return directories; }

    public String getRunningPath() {
        return getRunningPath(getMainClass());
    }

    @SuppressWarnings({"java:S106", "java:S4507"})
    public static String getRunningPath(Class<?> clazz) {
        try {
            URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
            return Paths.get(uri).toString();
        } catch (URISyntaxException ex) {
            // This should never happen, but this is here just in case it does
            System.err.println("Failed to get running path for class: " + clazz.getName());
            //noinspection CallToPrintStackTrace
            ex.printStackTrace();
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

    public InstallTasksQueue<Void> buildMoverTasks() {
        InstallTasksQueue<Void> queue = new InstallTasksQueue<>(null);

        queue.addTask(new MoveLauncherPackage(resources.getString("updater.mover"), new File(parameters.getMoveTarget()), this));
        queue.addTask(new LaunchLauncherMode(resources.getString("updater.finallaunch"), this, parameters.getMoveTarget(), parameters.isLegacyMover()));

        return queue;
    }

    public InstallTasksQueue<Void> buildUpdaterTasks() {
        screen = new SplashScreen(resources.getImage("launch_splash.png"), 30);
        Color bg = UIConstants.COLOR_FORM_ELEMENT_INTERNAL;
        screen.getContentPane().setBackground(new Color (bg.getRed(),bg.getGreen(),bg.getBlue(),255));
        screen.getProgressBar().setForeground(Color.white);
        screen.getProgressBar().setBackground(UIConstants.COLOR_GREEN);
        screen.getProgressBar().setBackFill(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
        screen.getProgressBar().setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        screen.pack();
        screen.setLocationRelativeTo(null);
        screen.setVisible(true);

        InstallTasksQueue<Void> queue = new InstallTasksQueue<>(screen.getProgressBar());

        ArrayList<IInstallTask<Void>> postDownloadTasks = new ArrayList<>();
        postDownloadTasks.add(new LaunchMoverMode(resources.getString("updater.launchmover"), getTempLauncher(), this));

        TaskGroup<Void> downloadFilesGroup = new TaskGroup<>(resources.getString("updater.downloads"));
        queue.addTask(new EnsureUpdateFolders(resources.getString("updater.folders"), getDirectories()));
        queue.addTask(new QueryUpdateStream(resources.getString("updater.query"), updateStream, downloadFilesGroup, getDirectories(), this, postDownloadTasks));
        queue.addTask(downloadFilesGroup);

        return queue;
    }

    /**
     * Returns the arguments to be used when relaunching the updater.
     * This includes the original arguments plus an additional "-blockReboot" argument.
     *
     * @return An array of strings representing the launch arguments.
     */
    public List<String> getRelaunchArgs() {
        List<String> args = parameters.getArgs();
        List<String> launchArgs = new ArrayList<>(args.size() + 1);
        launchArgs.addAll(args);
        launchArgs.add("-blockReboot");
        return launchArgs;
    }

    public void updateComplete() {
        screen.dispose();
    }

    public boolean canReboot() { return !parameters.isBlockReboot(); }

    public boolean runAutoUpdater() throws IOException, InterruptedException {
        if (isLauncherOnly())
            return true;

        boolean needsReboot = false;

        if (canReboot()) {
            if (System.getProperty("awt.useSystemAAFontSettings") == null || !System.getProperty("awt.useSystemAAFontSettings").equals("lcd"))
                needsReboot = true;
            else if (!Boolean.parseBoolean(System.getProperty("java.net.preferIPv4Stack")))
                needsReboot = true;
        }

        InstallTasksQueue<Void> updateTasksQueue;
        if (isMover()) {
            updateTasksQueue = buildMoverTasks();
        } else if (needsReboot && getCurrentBuild() > 0) {
            relaunch();
            return false;
        } else if (getCurrentBuild() < 1) {
            return true;
        } else {
            updateTasksQueue = buildUpdaterTasks();
        }

        if (updateTasksQueue == null)
            return true;

        updateTasksQueue.runAllTasks();
        updateComplete();

        return !didUpdate && !isUpdateOnly();
    }

    public void relaunch() {
        launch(null, getRelaunchArgs());
    }

    public File getTempLauncher() {
        File dest;
        String runningPath;

        runningPath = getRunningPath();

        if (runningPath.endsWith(".exe"))
            dest = new File(directories.getLauncherDirectory(), "temp.exe");
        else
            dest = new File(directories.getLauncherDirectory(), "temp.jar");
        return dest;
    }

    public void launch(String launchPath, List<String> args) {
        if (launchPath == null) {
            launchPath = getRunningPath();
        }

        ArrayList<String> commands = getCommands(launchPath);
        commands.addAll(args);

        String commandString = String.join(" ", commands);

        Utils.getLogger().info(String.format("Launching command: '%s'", commandString));

        ProcessBuilder pb = ProcessUtils.createProcessBuilder(commands, true);

        try {
            pb.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Your OS has prevented this relaunch from completing.  You may need to add an exception in your security software.", "Relaunch Failed", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private ArrayList<String> getCommands(String launchPath) {
        ArrayList<String> commands = new ArrayList<>();
        if (!launchPath.endsWith(".exe")) {
            commands.add(OperatingSystem.getJavaDir());
            commands.add("-Xmx256m");
            commands.add("-Djava.net.preferIPv4Stack=true");
            commands.add("-Dawt.useSystemAAFontSettings=lcd");
            commands.add("-Dswing.aatext=true");
            commands.add("-cp");
            commands.add(launchPath);
            commands.add(getMainClass().getName());
        } else
            commands.add(launchPath);
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
        if (!isLegacy)
            outArgs.add("-launcheronly");
        else
            outArgs.add("-launcher");
        outArgs.addAll(getRelaunchArgs());
        outArgs.remove("-moveronly");
        return outArgs;
    }
}
