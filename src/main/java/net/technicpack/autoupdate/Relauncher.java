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

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.ProcessUtils;
import net.technicpack.utilslib.Utils;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Relauncher {

    private final String stream;
    private final int currentBuild;
    private final LauncherDirectories directories;
    private boolean didUpdate = false;

    public Relauncher(String stream, int currentBuild, LauncherDirectories directories) {
        this.stream = stream;
        this.currentBuild = currentBuild;
        this.directories = directories;
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

    protected abstract Class<?> getMainClass();
    public abstract String getUpdateText();
    public abstract boolean isUpdateOnly();
    public abstract boolean isMover();
    public abstract boolean isLauncherOnly();
    public abstract boolean isSkipUpdate();
    public abstract InstallTasksQueue<Void> buildMoverTasks();
    public abstract InstallTasksQueue<Void> buildUpdaterTasks();
    public abstract String[] getLaunchArgs();
    public abstract void updateComplete();
    public abstract boolean canReboot();

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

        InstallTasksQueue<Void> updateTasksQueue = null;
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
        launch(null, getLaunchArgs());
    }

    public File getTempLauncher() {
        File dest;
        String runningPath = null;

        runningPath = getRunningPath();

        if (runningPath.endsWith(".exe"))
            dest = new File(directories.getLauncherDirectory(), "temp.exe");
        else
            dest = new File(directories.getLauncherDirectory(), "temp.jar");
        return dest;
    }

    public void launch(String launchPath, String[] args) {
        if (launchPath == null) {
            launchPath = getRunningPath();
        }

        ArrayList<String> commands = getCommands(launchPath);
        commands.addAll(Arrays.asList(args));

        String commandString = String.join(" ", commands);

        Utils.getLogger().info(String.format("Launching command: '%s'", commandString));

        ProcessBuilder pb = ProcessUtils.createProcessBuilder(commands);

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

    public String[] buildMoverArgs() {
        List<String> outArgs = new ArrayList<>();
        outArgs.add("-movetarget");
        outArgs.add(getRunningPath());
        outArgs.add("-moveronly");
        outArgs.addAll(Arrays.asList(getLaunchArgs()));
        return outArgs.toArray(new String[outArgs.size()]);
    }

    public String[] buildLauncherArgs(boolean isLegacy) {
        List<String> outArgs = new ArrayList<>();
        if (!isLegacy)
            outArgs.add("-launcheronly");
        else
            outArgs.add("-launcher");
        outArgs.addAll(Arrays.asList(getLaunchArgs()));
        outArgs.remove("-moveronly");
        return outArgs.toArray(new String[outArgs.size()]);
    }
}
