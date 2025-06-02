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
import java.util.regex.Pattern;
import java.util.zip.ZipException;

public class Installer {
    protected final ModpackInstaller<MojangVersion> installer;
    protected final MinecraftLauncher launcher;
    protected final TechnicSettings settings;
    protected final PackResourceMapper packIconMapper;
    protected final StartupParameters startupParameters;
    protected final LauncherDirectories directories;
    protected final Object cancelLock = new Object();
    protected boolean isCancelledByUser = false;

    private Thread installerThread;
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
        installerThread.interrupt();
    }

    public void justInstall(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener) {
        internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, false);
    }

    public void installAndRun(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener) {
        internalInstallAndRun(resources, pack, build, doFullInstall, frame, listener, true);
    }

    protected void internalInstallAndRun(final ResourceLoader resources, final ModpackModel pack, final String build, final boolean doFullInstall, final LauncherFrame frame, final DownloadListener listener, final boolean doLaunch) {
        installerThread = new Thread(() -> {
            GameProcess gameProcess = null;

            try {
                MojangVersion version = null;

                InstallTasksQueue<MojangVersion> tasksQueue = new InstallTasksQueue<>(listener);
                MojangVersionBuilder versionBuilder = createVersionBuilder(pack, tasksQueue);

                if (build != null && !build.isEmpty()) {
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

                    boolean usingMojangJava = version.getJavaVersion() != null && settings.shouldUseMojangJava();

                    JavaVersionRepository javaVersions = launcher.getJavaVersions();
                    Memory memoryObj = Memory.getClosestAvailableMemory(Memory.getMemoryFromId(settings.getMemory()), javaVersions.getSelectedVersion().is64Bit());
                    long memory = memoryObj.getMemoryMB();
                    String versionNumber = javaVersions.getSelectedVersion().getVersion();
                    RunData data = pack.getRunData();

                    if (data != null && !data.isRunDataValid(memory, versionNumber, usingMojangJava)) {
                        FixRunDataDialog dialog = new FixRunDataDialog(frame, resources, data, javaVersions, memoryObj, !settings.shouldAutoAcceptModpackRequirements(), usingMojangJava);
                        dialog.setVisible(true);
                        if (dialog.getResult() == FixRunDataDialog.Result.ACCEPT) {
                            memoryObj = dialog.getRecommendedMemory();
                            memory = memoryObj.getMemoryMB();
                            settings.setMemory(memoryObj.getSettingsId());

                            IJavaRuntime recommendedJavaVersion = dialog.getRecommendedJavaVersion();
                            if (recommendedJavaVersion != null) {
                                javaVersions.selectVersion(recommendedJavaVersion.getVersion(), recommendedJavaVersion.is64Bit());
                                settings.setJavaVersion(recommendedJavaVersion.getVersion());
                                settings.setJavaBitness(recommendedJavaVersion.is64Bit());
                            }

                            if (dialog.shouldRemember()) {
                                settings.setAutoAcceptModpackRequirements(true);
                            }
                        } else
                            return;
                    }

                    if (!usingMojangJava && RunData.isJavaVersionAtLeast(versionNumber, "1.9")) {
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
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!doLaunch || gameProcess == null || gameProcess.getProcess() == null || !gameProcess.getProcess().isAlive()) {
                    EventQueue.invokeLater(frame::launchCompleted);
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
        installerThread.start();
    }

    public boolean isCurrentlyRunning() {
        if (installerThread != null && installerThread.isAlive())
            return true;
        return false;
    }

    public void buildTasksQueue(InstallTasksQueue queue, ResourceLoader resources, ModpackModel modpack, String build, boolean doFullInstall, MojangVersionBuilder versionBuilder) throws IOException {
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
        TaskGroup examineJava = new TaskGroup("Examining Java runtime...");
        TaskGroup downloadJava = new TaskGroup("Downloading Java runtime...");

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
        queue.addTask(examineJava);
        queue.addTask(downloadJava);
        if (OperatingSystem.getOperatingSystem() == OperatingSystem.OSX)
            queue.addTask(new RenameJnilibToDylibTask(modpack));

        // Add legacy FML libs
        HashMap<String, String> fmlLibs = new HashMap<>();

        switch (minecraft) {
            case "1.3.2":
                fmlLibs.put("argo-2.25.jar", "bb672829fde76cb163004752b86b0484bd0a7f4b");
                fmlLibs.put("guava-12.0.1.jar", "b8e78b9af7bf45900e14c6f958486b6ca682195f");
                fmlLibs.put("asm-all-4.0.jar", "98308890597acb64047f7e896638e0d98753ae82");
                break;
            case "1.4":
            case "1.4.1":
            case "1.4.2":
            case "1.4.3":
            case "1.4.4":
            case "1.4.5":
            case "1.4.6":
            case "1.4.7":
                fmlLibs.put("argo-2.25.jar", "bb672829fde76cb163004752b86b0484bd0a7f4b");
                fmlLibs.put("guava-12.0.1.jar", "b8e78b9af7bf45900e14c6f958486b6ca682195f");
                fmlLibs.put("asm-all-4.0.jar", "98308890597acb64047f7e896638e0d98753ae82");
                fmlLibs.put("bcprov-jdk15on-147.jar", "b6f5d9926b0afbde9f4dbe3db88c5247be7794bb");
                break;
            case "1.5":
                fmlLibs.put("argo-small-3.2.jar", "58912ea2858d168c50781f956fa5b59f0f7c6b51");
                fmlLibs.put("guava-14.0-rc3.jar", "931ae21fa8014c3ce686aaa621eae565fefb1a6a");
                fmlLibs.put("asm-all-4.1.jar", "054986e962b88d8660ae4566475658469595ef58");
                fmlLibs.put("bcprov-jdk15on-148.jar", "960dea7c9181ba0b17e8bab0c06a43f0a5f04e65");
                fmlLibs.put("deobfuscation_data_1.5.zip", "5f7c142d53776f16304c0bbe10542014abad6af8");
                fmlLibs.put("scala-library.jar", "458d046151ad179c85429ed7420ffb1eaf6ddf85");
                break;
            case "1.5.1":
                fmlLibs.put("argo-small-3.2.jar", "58912ea2858d168c50781f956fa5b59f0f7c6b51");
                fmlLibs.put("guava-14.0-rc3.jar", "931ae21fa8014c3ce686aaa621eae565fefb1a6a");
                fmlLibs.put("asm-all-4.1.jar", "054986e962b88d8660ae4566475658469595ef58");
                fmlLibs.put("bcprov-jdk15on-148.jar", "960dea7c9181ba0b17e8bab0c06a43f0a5f04e65");
                fmlLibs.put("deobfuscation_data_1.5.1.zip", "22e221a0d89516c1f721d6cab056a7e37471d0a6");
                fmlLibs.put("scala-library.jar", "458d046151ad179c85429ed7420ffb1eaf6ddf85");
                break;
            case "1.5.2":
                fmlLibs.put("argo-small-3.2.jar", "58912ea2858d168c50781f956fa5b59f0f7c6b51");
                fmlLibs.put("guava-14.0-rc3.jar", "931ae21fa8014c3ce686aaa621eae565fefb1a6a");
                fmlLibs.put("asm-all-4.1.jar", "054986e962b88d8660ae4566475658469595ef58");
                fmlLibs.put("bcprov-jdk15on-148.jar", "960dea7c9181ba0b17e8bab0c06a43f0a5f04e65");
                fmlLibs.put("deobfuscation_data_1.5.2.zip", "446e55cd986582c70fcf12cb27bc00114c5adfd9");
                fmlLibs.put("scala-library.jar", "458d046151ad179c85429ed7420ffb1eaf6ddf85");
                break;
        }

        if (!fmlLibs.isEmpty()) {
            File modpackFmlLibDir = new File(modpack.getInstalledDirectory(), "lib");
            File fmlLibsCache = new File(directories.getCacheDirectory(), "fmllibs");
            Files.createDirectories(fmlLibsCache.toPath());

            fmlLibs.forEach((name, sha1) -> {
                SHA1FileVerifier verifier = null;

                if (!sha1.isEmpty())
                    verifier = new SHA1FileVerifier(sha1);

                File cached = new File(fmlLibsCache, name);
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

            // Remove the runData file between updates/reinstall as well
            File runData = new File(modpack.getBinDir(), "runData");
            if (runData.exists()) {
                if (!runData.delete()) {
                    throw new CacheDeleteException(runData.getAbsolutePath());
                }
            }

            // Remove the bin/modpack.jar file
            // This prevents issues when upgrading a modpack between a version that has a modpack.jar, and
            // one that doesn't. One example of this is updating BareBonesPack from a Forge to a Fabric build.
            File modpackJar = new File(modpack.getBinDir(), "modpack.jar");
            if (modpackJar.exists()) {
                if (!modpackJar.delete()) {
                    throw new CacheDeleteException(modpackJar.getAbsolutePath());
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

        examineJava.addTask(new EnsureJavaRuntimeManifestTask(directories.getRuntimesDirectory(), modpack, launcher.getJavaVersions(), examineJava, downloadJava));

        // Check if we need to regenerate the Minecraft jar. This is necessary if:
        // - A reinstall was requested (or forced, via modpack version update)
        // - The installed version is marked as legacy
        boolean jarRegenerationRequired = doFullInstall || (installedVersion != null && installedVersion.isLegacy());

        installingMinecraft.addTask(new InstallMinecraftIfNecessaryTask(modpack, minecraft, directories.getCacheDirectory(), jarRegenerationRequired));
    }

    private MojangVersionBuilder createVersionBuilder(ModpackModel modpack, InstallTasksQueue tasksQueue) {

        ZipFileRetriever zipVersionRetriever = new ZipFileRetriever(new File(modpack.getBinDir(), "modpack.jar"));
        HttpFileRetriever fallbackVersionRetriever = new HttpFileRetriever(TechnicConstants.technicVersions, tasksQueue.getDownloadListener());

        ArrayList<MojangVersionRetriever> fallbackRetrievers = new ArrayList<>(1);
        fallbackRetrievers.add(fallbackVersionRetriever);

        File versionJson = new File(modpack.getBinDir(), "version.json");

        // This always gets the version.json from the modpack.jar (it ignores "key"), cached as bin/version.json
        FileVersionBuilder zipVersionBuilder = new FileVersionBuilder(versionJson, zipVersionRetriever, fallbackRetrievers);
        // This gets the "key" from bin/$key.json if it exists, otherwise it downloads it from our repo into that location
        FileVersionBuilder webVersionBuilder = new FileVersionBuilder(modpack.getBinDir(), null, fallbackRetrievers);

        return new ChainVersionBuilder(zipVersionBuilder, webVersionBuilder);
    }
}
