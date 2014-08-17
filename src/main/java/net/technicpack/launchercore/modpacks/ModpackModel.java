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

package net.technicpack.launchercore.modpacks;

import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.install.Version;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModpackModel {
    private InstalledPack installedPack;
    private PackInfo packInfo;
    private IInstalledPackRepository installedPackRepository;
    private LauncherDirectories directories;

    private String buildName;
    private boolean isPlatform;

    private File installedDirectory;

    public ModpackModel(InstalledPack installedPack, PackInfo info, IInstalledPackRepository installedPackRepository, LauncherDirectories directories) {
        this();

        this.installedPack = installedPack;
        this.packInfo = info;
        this.installedPackRepository = installedPackRepository;
        this.directories = directories;
    }

    protected ModpackModel() {
        buildName = InstalledPack.RECOMMENDED;
        isPlatform = true;
    }

    public InstalledPack getInstalledPack() { return installedPack; }
    public PackInfo getPackInfo() { return packInfo; }

    public void setInstalledPack(InstalledPack pack, IInstalledPackRepository packRepo) {
        installedPack = pack;
        installedPackRepository = packRepo;
    }

    public void setPackInfo(PackInfo packInfo) {
        this.packInfo = packInfo;
    }

    public String getName() {
        if (packInfo != null) {
            return packInfo.getName();
        } else if (installedPack != null) {
            return installedPack.getName();
        } else
            return null;
    }

    public String getDisplayName() {
        if (packInfo != null) {
            return packInfo.getDisplayName();
        } else if (installedPack != null) {
            return installedPack.getName();
        } else
            return "";
    }

    public String getBuild() {
        if (installedPack != null) {
            return installedPack.getBuild();
        } else
            return buildName;
    }

    public Resource getIcon() {
        if (packInfo == null)
            return null;
        return packInfo.getIcon();
    }

    public Resource getLogo() {
        if (packInfo == null)
            return null;
        return packInfo.getLogo();
    }

    public Resource getBackground() {
        if (packInfo == null)
            return null;

        return packInfo.getBackground();
    }

    public ArrayList<FeedItem> getFeed() {
        if (packInfo == null)
            return new ArrayList<FeedItem>();

        return packInfo.getFeed();
    }

    public boolean isLocalOnly() {
        return (packInfo == null);
    }

    public boolean isPlatform() {
        if (installedPack != null) {
            return installedPack.isPlatform();
        } else {
            return isPlatform;
        }
    }

    public Version getInstalledVersion() {
        Version version = null;
        File versionFile = new File(getBinDir(), "version");
        if (versionFile.exists()) {
            return Version.load(versionFile);
        } else {
            return null;
        }
    }

    public boolean hasRecommendedUpdate() {
        if (installedPack == null || packInfo == null)
            return false;

        Version installedVersion = getInstalledVersion();

        if (installedVersion == null)
            return false;

        String installedBuild = installedVersion.getVersion();

        List<String> allBuilds = packInfo.getBuilds();

        if (!allBuilds.contains(installedBuild))
            return true;

        for(String build : allBuilds) {
            if (build.equalsIgnoreCase(packInfo.getRecommended())) {
                return false;
            } else if (build.equalsIgnoreCase(installedBuild)) {
                return true;
            }
        }

        return false;
    }

    public void setIsPlatform(boolean isPlatform) {
        if (installedPack == null) {
            this.isPlatform = isPlatform;
        }
    }

    public String getDescription() {
        if (packInfo == null)
            return "";

        return packInfo.getDescription();
    }

    public Integer getLikes() {
        if (packInfo == null)
            return null;

        return packInfo.getLikes();
    }

    public Integer getRuns() {
        if (packInfo == null)
            return null;

        return packInfo.getRuns();
    }

    public Integer getDownloads() {
        if (packInfo == null)
            return null;

        return packInfo.getDownloads();
    }

    public File getInstalledDirectory() {
        if (installedPack == null)
            return null;

        if (installedDirectory == null) {
            String rawDir = installedPack.getDirectory();

            if (rawDir != null && rawDir.startsWith(InstalledPack.LAUNCHER_DIR)) {
                rawDir = new File(directories.getLauncherDirectory(), rawDir.substring(InstalledPack.LAUNCHER_DIR.length())).getAbsolutePath();
            }
            if (rawDir != null && rawDir.startsWith(InstalledPack.MODPACKS_DIR)) {
                rawDir = new File(directories.getModpacksDirectory(), rawDir.substring(InstalledPack.MODPACKS_DIR.length())).getAbsolutePath();
            }

            setInstalledDirectory(new File(rawDir));
        }

        return installedDirectory;
    }

    public File getBinDir() {
        File installedDir = getInstalledDirectory();

        if (installedDir == null)
            return null;

        return new File(installedDir, "bin");
    }

    public File getModsDir() {
        File installedDir = getInstalledDirectory();

        if (installedDir == null)
            return null;

        return new File(installedDir, "mods");
    }

    public File getCoremodsDir() {
        File installedDir = getInstalledDirectory();

        if (installedDir == null)
            return null;

        return new File(installedDir, "coremods");
    }

    public File getCacheDir() {
        File installedDir = getInstalledDirectory();

        if (installedDir == null)
            return null;

        return new File(installedDir, "cache");
    }

    public File getConfigDir() {
        File installedDir = getInstalledDirectory();

        if (installedDir == null)
            return null;

        return new File(installedDir, "config");
    }

    public File getResourcesDir() {
        File installedDir = getInstalledDirectory();

        if (installedDir == null)
            return null;

        return new File(installedDir, "resources");
    }

    public File getSavesDir() {
        File installedDir = getInstalledDirectory();

        if (installedDir == null)
            return null;

        return new File(installedDir, "saves");
    }

    public void initDirectories() {
        getBinDir().mkdirs();
        getModsDir().mkdirs();
        getCoremodsDir().mkdirs();
        getConfigDir().mkdirs();
        getCacheDir().mkdirs();
        getResourcesDir().mkdirs();
        getSavesDir().mkdirs();
    }

    public void setInstalledDirectory(File targetDirectory) {
        if (installedDirectory != null && installedDirectory.exists()) {
            try {
                FileUtils.copyDirectory(installedDirectory, targetDirectory);
                FileUtils.cleanDirectory(installedDirectory);
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
        }

        installedDirectory = targetDirectory;
        String path = installedDirectory.getAbsolutePath();

        if (path.equals(directories.getModpacksDirectory().getAbsolutePath())) {
            installedPack.setDirectory(InstalledPack.MODPACKS_DIR);
        } else if (path.equals(directories.getLauncherDirectory().getAbsolutePath())) {
            installedPack.setDirectory(InstalledPack.LAUNCHER_DIR);
        } else if (path.startsWith(directories.getModpacksDirectory().getAbsolutePath())) {
            installedPack.setDirectory(InstalledPack.MODPACKS_DIR + path.substring(directories.getModpacksDirectory().getAbsolutePath().length() + 1));
        } else if (path.startsWith(directories.getLauncherDirectory().getAbsolutePath())) {
            installedPack.setDirectory(InstalledPack.LAUNCHER_DIR + path.substring(directories.getLauncherDirectory().getAbsolutePath().length() + 1));
        } else
            installedPack.setDirectory(path);

        save();
    }

    public void save() {
        if (installedPack == null) {
            installedPack = new InstalledPack(getName(), isPlatform(), getBuild());
        }

        installedPackRepository.put(installedPack);
        installedPackRepository.save();
    }

    public boolean isSelected() {
        int packCount = installedPackRepository.getPackNames().size();

        if (packCount == 0)
            return false;

        int selectedIndex = installedPackRepository.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= packCount) {
            selectedIndex = 0;
        }

        String selectedName = installedPackRepository.getPackNames().get(selectedIndex);

        return (selectedName != null && selectedName.equalsIgnoreCase(getName()));
    }

    public void select() {
        String name = getName();
        List<String> names = installedPackRepository.getPackNames();

        for(int i = 0; i < names.size(); i++) {
            String packName = names.get(i);

            if (name.equalsIgnoreCase(packName)) {
                installedPackRepository.setSelectedIndex(i);
                installedPackRepository.save();
                break;
            }
        }
    }
}
