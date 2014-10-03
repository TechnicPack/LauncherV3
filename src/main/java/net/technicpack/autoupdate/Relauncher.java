/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
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

import net.technicpack.autoupdate.io.StreamVersion;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.mirror.download.Download;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.utilslib.OperatingSystem;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import net.technicpack.utilslib.Utils;
import org.apache.commons.io.IOUtils;

public class Relauncher {
    private IUpdateStream updateStream;

    public Relauncher(IUpdateStream updateStream) {
        this.updateStream = updateStream;
    }

    public String getUpdateUrl(String stream, int currentBuild, Class mainClass) {
        try {
            StreamVersion version = updateStream.getStreamVersion(stream);

            if (version.getBuild() == currentBuild)
                return null;

            String runningPath = getRunningPath(mainClass);

            if (runningPath.endsWith(".exe"))
                return version.getExeUrl();
            else
                return version.getJarUrl();

        } catch (RestfulAPIException ex) {
            return null;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public void launch(String launchPath, Class mainClass, String[] args) {
        if (launchPath == null) {
            try {
                launchPath = getRunningPath(mainClass);
            } catch (UnsupportedEncodingException ex) {
                return;
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        ArrayList<String> commands = new ArrayList<String>();
        if (!launchPath.endsWith(".exe")) {
            if (OperatingSystem.getOperatingSystem().equals(OperatingSystem.WINDOWS))
                commands.add("javaw");
            else
                commands.add("java");
            commands.add("-Xmx256m");
            commands.add("-Djava.net.preferIPv4Stack=true");
            commands.add("-cp");
            commands.add(launchPath);
            commands.add(mainClass.getName());
        } else
            commands.add(launchPath);
        commands.addAll(Arrays.asList(args));

        String command = "";

        for(String token : commands) {
            command += token + " ";
        }

        Utils.getLogger().info("Launching command: '"+command+"'");

        processBuilder.command(commands);

        try {
            processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public String[] buildMoverArgs(Class mainClass, String[] args) throws UnsupportedEncodingException {
        List<String> outArgs = new ArrayList<String>();
        outArgs.add("-movetarget");
        outArgs.add(getRunningPath(mainClass));
        outArgs.add("-mover");
        outArgs.addAll(Arrays.asList(args));
        return outArgs.toArray(new String[outArgs.size()]);
    }

    public String[] buildLauncherArgs(String[] args) {
        List<String> outArgs = new ArrayList<String>();
        outArgs.add("-launcher");
        outArgs.addAll(Arrays.asList(args));
        return outArgs.toArray(new String[outArgs.size()]);
    }

    public String getRunningPath(Class mainClass) throws UnsupportedEncodingException {
        return URLDecoder.decode(mainClass.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
    }

    public void replacePackage(Class mainClass, String targetPath) throws UnsupportedEncodingException {
        String currentPath = getRunningPath(mainClass);
        File source = new File(currentPath);
        File dest = new File(targetPath);

        dest.delete();
        FileInputStream sourceStream = null;
        FileOutputStream destStream = null;
        try {
            sourceStream = new FileInputStream(source);
            destStream = new FileOutputStream(dest);
            IOUtils.copy(sourceStream, destStream);
        } catch (IOException ex) {
            Utils.getLogger().log(Level.SEVERE, "Error attempting to copy download package:", ex);
        } finally {
            IOUtils.closeQuietly(sourceStream);
            IOUtils.closeQuietly(destStream);
        }

        dest.setExecutable(true, true);
    }

    public String downloadUpdate(String url, LauncherDirectories directories, String progressText, DownloadListener listener) {
        File dest;
        if (url.endsWith(".exe"))
            dest = new File(directories.getLauncherDirectory(), "temp.exe");
        else
            dest = new File(directories.getLauncherDirectory(), "temp.jar");

        try {
            Utils.getLogger().info("Downloading update from "+url+" to "+dest.getAbsolutePath());
            Download download = new Download(new URL(url), progressText, dest.getPath());
            download.setListener(listener);
            download.run();

            if (download.getResult() != Download.Result.SUCCESS) {
                if (download.getException() != null)
                    download.getException().printStackTrace();
                return null;
            }
        } catch (MalformedURLException ex) {
            Utils.getLogger().log(Level.SEVERE, "Received bad url from build stream: "+url, ex);
        }

        return dest.getAbsolutePath();
    }
}
