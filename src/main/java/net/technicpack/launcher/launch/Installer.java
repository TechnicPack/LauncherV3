/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.launch;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.installation.ProgressBar;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.exception.CacheDeleteException;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.launchercore.launch.LaunchOptions;
import net.technicpack.launchercore.launch.MinecraftLauncher;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.util.LaunchAction;
import net.technicpack.minecraftcore.mojang.CompleteVersion;
import net.technicpack.utilslib.Memory;

import javax.swing.*;
import java.io.IOException;
import java.util.zip.ZipException;

public class Installer {
    protected final ModpackInstaller installer;
    protected final MinecraftLauncher launcher;
    protected final TechnicSettings settings;
    protected final PackResourceMapper packIconMapper;
    protected final StartupParameters startupParameters;

    private Thread runningThread;
    private LauncherUnhider launcherUnhider;

    public Installer(StartupParameters startupParameters, ModpackInstaller installer, MinecraftLauncher launcher, TechnicSettings settings, PackResourceMapper packIconMapper) {
        this.installer = installer;
        this.launcher = launcher;
        this.settings = settings;
        this.packIconMapper = packIconMapper;
        this.startupParameters = startupParameters;
    }

    public void installAndRun(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final ProgressBar bar) {
        runningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bar.setVisible(true);
                    CompleteVersion version = null;
                    if (!pack.isLocalOnly()) {
                        version = installer.installPack(pack, build, bar);
                    } else {
                        version = installer.prepareOfflinePack(pack);
                    }

                    int memory = Memory.getMemoryFromId(settings.getMemory()).getMemoryMB();

                    LaunchOptions options = new LaunchOptions( pack.getDisplayName(), packIconMapper.getImageLocation(pack).getAbsolutePath(), startupParameters.getWidth(), startupParameters.getHeight(), startupParameters.getFullscreen());
                    launcherUnhider = new LauncherUnhider(settings, frame);
                    launcher.launch(pack, memory, options, launcherUnhider, version);

                    LaunchAction launchAction = settings.getLaunchAction();

                    if (launchAction == null || launchAction == LaunchAction.HIDE) {
                        frame.setVisible(false);
                    } else if (launchAction == LaunchAction.CLOSE) {
                        System.exit(0);
                    }
                } catch (PackNotAvailableOfflineException e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "Cannot Start Modpack", JOptionPane.WARNING_MESSAGE);
                } catch (DownloadException e) {
                    JOptionPane.showMessageDialog(frame, "Error downloading file for the following pack: " + pack.getDisplayName() + " \n\n" + e.getMessage() + "\n\nPlease consult the modpack author.", "Error", JOptionPane.WARNING_MESSAGE);
                } catch (ZipException e) {
                    JOptionPane.showMessageDialog(frame, "Error unzipping a file for the following pack: " + pack.getDisplayName() + " \n\n" + e.getMessage() + "\n\nPlease consult the modpack author.", "Error", JOptionPane.WARNING_MESSAGE);
                } catch (CacheDeleteException e) {
                    JOptionPane.showMessageDialog(frame, "Error installing the following pack: "+pack.getDisplayName() + " \n\n" + e.getMessage() + "\n\nPlease check your system settings.", "Error", JOptionPane.WARNING_MESSAGE);
                } catch (BuildInaccessibleException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bar.setVisible(false);
                }
            }
        });
        runningThread.start();
    }

    public boolean isCurrentlyRunning() {
        if (runningThread != null && runningThread.isAlive())
            return true;
        if (launcherUnhider != null && !launcherUnhider.hasExited())
            return true;
        return false;
    }
}
