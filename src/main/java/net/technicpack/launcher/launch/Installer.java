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

import net.technicpack.launcher.io.TechnicLauncherDirectories;
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
import net.technicpack.launchercore.install.Version;
import net.technicpack.launchercore.install.tasks.*;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.launch.LaunchOptions;
import net.technicpack.launchercore.launch.MinecraftLauncher;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.launchercore.util.LaunchAction;
import net.technicpack.minecraftcore.LauncherDirectories;
import net.technicpack.minecraftcore.mojang.CompleteVersion;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.utilslib.Memory;
import sun.misc.Cache;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;

public class Installer {
    protected final ModpackInstaller installer;
    protected final MinecraftLauncher launcher;
    protected final TechnicSettings settings;
    protected final PackResourceMapper packIconMapper;
    protected final StartupParameters startupParameters;
    protected final MirrorStore mirrorStore;
    protected final LauncherDirectories directories;

    private Thread runningThread;
    private LauncherUnhider launcherUnhider;

    public Installer(StartupParameters startupParameters, MirrorStore mirrorStore, LauncherDirectories directories, ModpackInstaller installer, MinecraftLauncher launcher, TechnicSettings settings, PackResourceMapper packIconMapper) {
        this.installer = installer;
        this.launcher = launcher;
        this.settings = settings;
        this.packIconMapper = packIconMapper;
        this.startupParameters = startupParameters;
        this.mirrorStore = mirrorStore;
        this.directories = directories;
    }

    public void installAndRun(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener) {
        runningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InstallTasksQueue tasksQueue = new InstallTasksQueue(listener, mirrorStore);
                    buildTasksQueue(tasksQueue, pack, build, doFullInstall);

                    CompleteVersion version = null;
                    if (!pack.isLocalOnly()) {
                        version = installer.installPack(tasksQueue, pack, build);
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
                    JOptionPane.showMessageDialog(frame, e.getMessage(), resources.getString("launcher.installerror.unavailable"), JOptionPane.WARNING_MESSAGE);
                } catch (DownloadException e) {
                    JOptionPane.showMessageDialog(frame, resources.getString("launcher.installerror.download", pack.getDisplayName(), e.getMessage()), resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
                } catch (ZipException e) {
                    JOptionPane.showMessageDialog(frame, resources.getString("launcher.installerror.unzip", pack.getDisplayName(), e.getMessage()), resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
                } catch (CacheDeleteException e) {
                    JOptionPane.showMessageDialog(frame, resources.getString("launcher.installerror.cache", pack.getDisplayName(), e.getMessage()), resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
                } catch (BuildInaccessibleException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, e.getMessage(), resources.getString("launcher.installerror.title"), JOptionPane.WARNING_MESSAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            frame.launchCompleted();
                        }
                    });
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

    public void buildTasksQueue(InstallTasksQueue queue, ModpackModel modpack, String build, boolean doFullInstall) throws CacheDeleteException, BuildInaccessibleException {
        PackInfo packInfo = modpack.getPackInfo();
        Modpack modpackData = packInfo.getModpack(build);
        String minecraft = modpackData.getMinecraft();
        Version installedVersion = modpack.getInstalledVersion();

        if (minecraft.startsWith("1.5")) {
            queue.AddTask(new EnsureFileTask(new File(directories.getCacheDirectory(), "fml_libs15.zip"), new ValidZipFileVerifier(), new File(modpack.getInstalledDirectory(), "lib"), "http://mirror.technicpack.net/Technic/lib/fml/fml_libs15.zip"));
        } else if (minecraft.startsWith("1.4")) {
            queue.AddTask(new EnsureFileTask(new File(directories.getCacheDirectory(), "fml_libs.zip"), new ValidZipFileVerifier(), new File(modpack.getInstalledDirectory(), "lib"), "http://mirror.technicpack.net/Technic/lib/fml/fml_libs.zip"));
        }

        if (doFullInstall) {
            //If we're installing a new version of modpack, then we need to get rid of the existing version.json
            File versionFile = new File(modpack.getBinDir(), "version.json");
            if (versionFile.exists()) {
                if (!versionFile.delete()) {
                    throw new CacheDeleteException(versionFile.getAbsolutePath());
                }
            }

            queue.AddTask(new InstallModpackTask(modpack, modpackData));
        }

        queue.AddTask(new VerifyVersionFilePresentTask(modpack, minecraft));
        queue.AddTask(new HandleVersionFileTask(modpack, directories));

        if (doFullInstall || (installedVersion != null && installedVersion.isLegacy()))
            queue.AddTask(new InstallMinecraftIfNecessaryTask(modpack, minecraft, directories.getCacheDirectory()));
    }
}
