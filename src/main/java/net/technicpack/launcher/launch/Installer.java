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
import net.technicpack.launchercore.install.verifiers.MD5FileVerifier;
import net.technicpack.launchercore.install.verifiers.ValidZipFileVerifier;
import net.technicpack.launchercore.launch.java.IJavaVersion;
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

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

public class Installer {
    protected final ModpackInstaller<MojangVersion> installer;
    protected final MinecraftLauncher launcher;
    protected final TechnicSettings settings;
    protected final PackResourceMapper packIconMapper;
    protected final StartupParameters startupParameters;
    protected final LauncherDirectories directories;
    protected Object cancelLock = new Object();
    protected boolean isCancelledByUser = false;

    private Thread runningThread;
    private LauncherUnhider launcherUnhider;

    public Installer(StartupParameters startupParameters, LauncherDirectories directories, ModpackInstaller installer, MinecraftLauncher launcher, TechnicSettings settings, PackResourceMapper packIconMapper) {
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
        runningThread.interrupt();
    }

    public void justInstall(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener) {
        internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, false);
    }

    public void installAndRun(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener) {
        internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, true);
    }

    protected void internalInstallAndRun(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener, final boolean doLaunch) {
        runningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean everythingWorked = false;

                try {
                    MojangVersion version = null;

                    InstallTasksQueue<MojangVersion> tasksQueue = new InstallTasksQueue<MojangVersion>(listener);
                    MojangVersionBuilder versionBuilder = createVersionBuilder(pack, tasksQueue);

                    if (!pack.isLocalOnly() && build != null && !build.isEmpty()) {
                        buildTasksQueue(tasksQueue, resources, pack, build, doFullInstall, versionBuilder);

                        version = installer.installPack(tasksQueue, pack, build);
                    } else {
                        version = versionBuilder.buildVersionFromKey(null);

                        if (version != null)
                            pack.initDirectories();
                    }

                    if (doLaunch) {
                        if (version == null) {
                            throw new PackNotAvailableOfflineException(pack.getDisplayName());
                        }

                        JavaVersionRepository javaVersions = launcher.getJavaVersions();
                        Memory memoryObj = Memory.getClosestAvailableMemory(Memory.getMemoryFromId(settings.getMemory()), javaVersions.getSelectedVersion().is64Bit());
                        long memory = memoryObj.getMemoryMB();
                        String versionNumber = javaVersions.getSelectedVersion().getVersionNumber();
                        RunData data = pack.getRunData();

                        if (data != null && !data.isRunDataValid(memory, versionNumber)) {
                            FixRunDataDialog dialog = new FixRunDataDialog(frame, resources, data, javaVersions, memoryObj, !settings.shouldAutoAcceptModpackRequirements());
                            dialog.setVisible(true);
                            if (dialog.getResult() == FixRunDataDialog.Result.ACCEPT) {
                                memoryObj = dialog.getRecommendedMemory();
                                memory = memoryObj.getMemoryMB();
                                IJavaVersion recommendedJavaVersion = dialog.getRecommendedJavaVersion();
                                javaVersions.selectVersion(recommendedJavaVersion.getVersionNumber(), recommendedJavaVersion.is64Bit());

                                if (dialog.shouldRemember()) {
                                    settings.setAutoAcceptModpackRequirements(true);
                                }
                            } else
                                return;
                        }

                        if (RunData.isJavaVersionAtLeast(versionNumber, "1.9")) {
                            int result = JOptionPane.showConfirmDialog(frame, resources.getString("launcher.jverwarning", versionNumber), resources.getString("launcher.jverwarning.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                            if (result != JOptionPane.YES_OPTION)
                                return;
                        }

                        LaunchAction launchAction = settings.getLaunchAction();

                        if (launchAction == null || launchAction == LaunchAction.HIDE) {
                            launcherUnhider = new LauncherUnhider(settings, frame);
                        } else
                            launcherUnhider = null;

                        LaunchOptions options = new LaunchOptions(pack.getDisplayName(), packIconMapper.getImageLocation(pack).getAbsolutePath(), settings);
                        launcher.launch(pack, memory, options, launcherUnhider, version);

                        if (launchAction == null || launchAction == LaunchAction.HIDE) {
                            frame.setVisible(false);
                        } else if (launchAction == LaunchAction.NOTHING) {
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    frame.launchCompleted();
                                }
                            });
                        } else if (launchAction == LaunchAction.CLOSE) {
                            System.exit(0);
                        }
                    }

                    everythingWorked = true;
                } catch (InterruptedException e) {
                    boolean cancelledByUser = false;
                    synchronized (cancelLock) {
                        if (isCancelledByUser) {
                            cancelledByUser = true;
                            isCancelledByUser = false;
                        }
                    }

                    //Canceled by user
                    if (!cancelledByUser) {
                        if (e.getCause() != null)
                            Utils.getLogger().info("Cancelled by exception.");
                        else
                            Utils.getLogger().info("Cancelled by code.");
                        e.printStackTrace();
                    } else
                        Utils.getLogger().info("Cancelled by user.");
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
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (!everythingWorked || !doLaunch) {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                frame.launchCompleted();
                            }
                        });
                    }
                }
            }
        }) {
            ///Interrupt is being called from a mysterious source, so unless this is a user-initiated cancel
            ///Let's print the stack trace of the interruptor.
            @Override
            public void interrupt() {
                boolean userCancelled = false;
                synchronized (cancelLock) {
                    if (isCancelledByUser)
                        userCancelled = true;
                }

                if (!userCancelled) {
                    Utils.getLogger().info("Mysterious interruption source.");
                    try {
                        //I am a charlatan and a hack.
                        throw new Exception("Grabbing stack trace- this isn't necessarily an error.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                super.interrupt();
            }
        };
        runningThread.start();
    }

    public boolean isCurrentlyRunning() {
        if (runningThread != null && runningThread.isAlive())
            return true;
        if (launcherUnhider != null && !launcherUnhider.hasExited())
            return true;
        return false;
    }

    public void buildTasksQueue(InstallTasksQueue queue, ResourceLoader resources, ModpackModel modpack, String build, boolean doFullInstall, MojangVersionBuilder versionBuilder) throws CacheDeleteException, BuildInaccessibleException {
        PackInfo packInfo = modpack.getPackInfo();
        Modpack modpackData = packInfo.getModpack(build);

        if (modpackData.getGameVersion() == null)
            return;

        String minecraft = modpackData.getGameVersion();
        Version installedVersion = modpack.getInstalledVersion();

        TaskGroup examineModpackData = new TaskGroup(resources.getString("install.message.examiningmodpack"));
        TaskGroup verifyingFiles = new TaskGroup(resources.getString("install.message.verifyingfiles"));
        TaskGroup downloadingMods = new TaskGroup(resources.getString("install.message.downloadmods"));
        TaskGroup installingMods = new TaskGroup(resources.getString("install.message.installmods"));
        TaskGroup checkVersionFile = new TaskGroup(resources.getString("install.message.checkversionfile"));
        TaskGroup installVersionFile = new TaskGroup(resources.getString("install.message.installversionfile"));
        TaskGroup rundataTaskGroup = new TaskGroup(resources.getString("install.message.runData"));
        TaskGroup examineVersionFile = new TaskGroup(resources.getString("install.message.examiningversionfile"));
        TaskGroup grabLibs = new TaskGroup(resources.getString("install.message.grablibraries"));
        TaskGroup checkNonMavenLibs = new TaskGroup(resources.getString("install.message.nonmavenlibs"));
        TaskGroup installingLibs = new TaskGroup(resources.getString("install.message.installlibs"));
        TaskGroup installingMinecraft = new TaskGroup(resources.getString("install.message.installminecraft"));
        TaskGroup examineIndex = new TaskGroup(resources.getString("install.message.examiningindex"));
        TaskGroup verifyingAssets = new TaskGroup(resources.getString("install.message.verifyassets"));
        TaskGroup installingAssets = new TaskGroup(resources.getString("install.message.installassets"));

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
        if (OperatingSystem.getOperatingSystem() == OperatingSystem.OSX)
            queue.addTask(new CopyDylibJnilibTask(modpack));

        // Add FML libs
        String fmlLibsZip;
        File modpackFmlLibDir = new File(modpack.getInstalledDirectory(), "lib");
        HashMap<String, String> fmlLibs = new HashMap<>();

        switch (minecraft) {
            case "1.4":
            case "1.4.1":
            case "1.4.2":
            case "1.4.3":
            case "1.4.4":
            case "1.4.5":
            case "1.4.6":
            case "1.4.7":
                fmlLibsZip = "fml_libs.zip";
                break;
            case "1.5":
                fmlLibsZip = "fml_libs15.zip";
                fmlLibs.put("deobfuscation_data_1.5.zip", "dba6d410a91a855f3b84457c86a8132a");
                break;
            case "1.5.1":
                fmlLibsZip = "fml_libs15.zip";
                fmlLibs.put("deobfuscation_data_1.5.1.zip", "c4fc2fedba60d920e4c7f9a095b2b883");
                break;
            case "1.5.2":
                fmlLibsZip = "fml_libs15.zip";
                fmlLibs.put("deobfuscation_data_1.5.2.zip", "270d9775872cc9fa773389812cab91fe");
                break;
            default:
                fmlLibsZip = "";
        }

        if (!fmlLibsZip.isEmpty()) {
            verifyingFiles.addTask(new EnsureFileTask(new File(directories.getCacheDirectory(), fmlLibsZip), new ValidZipFileVerifier(), modpackFmlLibDir, TechnicConstants.technicFmlLibRepo + fmlLibsZip, installingLibs, installingLibs));
        }

        if (!fmlLibs.isEmpty()) {
            fmlLibs.forEach((name, md5) -> {
                MD5FileVerifier verifier = null;

                if (!md5.isEmpty())
                    verifier = new MD5FileVerifier(md5);

                File cached = new File(directories.getCacheDirectory(), name);
                File target = new File(modpackFmlLibDir, name);

                if (!target.exists() || (verifier != null && !verifier.isFileValid(target)) ) {
                    verifyingFiles.addTask(new EnsureFileTask(cached, verifier, null, TechnicConstants.technicFmlLibRepo + name, installingLibs, installingLibs));
                    installingLibs.addTask(new CopyFileTask(cached, target));
                }
            });
        }

        if (doFullInstall) {
            //If we're installing a new version of modpack, then we need to get rid of the existing version.json
            File versionFile = new File(modpack.getBinDir(), "version.json");
            if (versionFile.exists()) {
                if (!versionFile.delete()) {
                    throw new CacheDeleteException(versionFile.getAbsolutePath());
                }
            }

            // Remove bin/install_profile.json, which is used by ForgeWrapper to install Forge in Minecraft 1.13+
            // (and the latest few Forge builds in 1.12.2)
            File installProfileFile = new File(modpack.getBinDir(), "install_profile.json");
            if (installProfileFile.exists()) {
                if (!installProfileFile.delete()) {
                    throw new CacheDeleteException(installProfileFile.getAbsolutePath());
                }
            }

            // Delete all other version JSON files in the bin dir
            File[] binFiles = modpack.getBinDir().listFiles();
            if (binFiles != null) {
                final Pattern minecraftVersionPattern = Pattern.compile("^[0-9]+(\\.[0-9]+)+\\.json$");
                for (File binFile : binFiles) {
                    if (minecraftVersionPattern.matcher(binFile.getName()).matches()) {
                        if (!binFile.delete()) {
                            throw new CacheDeleteException(binFile.getAbsolutePath());
                        }
                    }
                }
            }

            examineModpackData.addTask(new CleanupAndExtractModpackTask(modpack, modpackData, verifyingFiles, downloadingMods, installingMods));
        }

        if (doFullInstall)
            rundataTaskGroup.addTask(new WriteRundataFile(modpack, modpackData));
        else
            rundataTaskGroup.addTask(new CheckRundataFile(modpack, modpackData, rundataTaskGroup));

        checkVersionFile.addTask(new VerifyVersionFilePresentTask(modpack, minecraft, versionBuilder));
        examineVersionFile.addTask(new HandleVersionFileTask(modpack, directories, checkNonMavenLibs, grabLibs, installingLibs, installingLibs, versionBuilder));
        examineVersionFile.addTask(new EnsureAssetsIndexTask(directories.getAssetsDirectory(), modpack, installingMinecraft, examineIndex, verifyingAssets, installingAssets, installingAssets));

        if (doFullInstall || (installedVersion != null && installedVersion.isLegacy()))
            installingMinecraft.addTask(new InstallMinecraftIfNecessaryTask(modpack, minecraft, directories.getCacheDirectory()));
    }

    private MojangVersionBuilder createVersionBuilder(ModpackModel modpack, InstallTasksQueue tasksQueue) {

        ZipFileRetriever zipVersionRetriever = new ZipFileRetriever(new File(modpack.getBinDir(), "modpack.jar"));
        HttpFileRetriever fallbackVersionRetriever = new HttpFileRetriever(TechnicConstants.technicVersions, tasksQueue.getDownloadListener());

        ArrayList<MojangVersionRetriever> fallbackRetrievers = new ArrayList<MojangVersionRetriever>(1);
        fallbackRetrievers.add(fallbackVersionRetriever);

        File versionJson = new File(modpack.getBinDir(), "version.json");

        // This always gets the version.json from the modpack.jar (it ignores "key"), cached as bin/version.json
        FileVersionBuilder zipVersionBuilder = new FileVersionBuilder(versionJson, zipVersionRetriever, fallbackRetrievers);
        // This gets the "key" from bin/$key.json if it exists, otherwise it downloads it from our repo into that location
        FileVersionBuilder webVersionBuilder = new FileVersionBuilder(modpack.getBinDir(), null, fallbackRetrievers);

        return new ChainVersionBuilder(zipVersionBuilder, webVersionBuilder);
    }
}
