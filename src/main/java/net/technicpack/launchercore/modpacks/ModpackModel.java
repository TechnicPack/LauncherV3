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

package net.technicpack.launchercore.modpacks;

import com.google.gson.JsonParseException;
import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.install.ModpackVersion;
import net.technicpack.launchercore.modpacks.packinfo.CombinedPackInfo;
import net.technicpack.launchercore.modpacks.sources.IModpackTagBuilder;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ModpackModel {
    private InstalledPack installedPack;
    private PackInfo packInfo;
    private InstalledPackStore packStore;
    private LauncherFileSystem fileSystem;
    private Collection<String> tags = new ArrayList<>();

    private String buildName;
    private boolean isPlatform;

    private File installedDirectory;
    private int priority = -2;

    public ModpackModel(InstalledPack installedPack, PackInfo info, InstalledPackStore packStore, LauncherFileSystem fileSystem) {
        this();

        this.installedPack = installedPack;
        this.packInfo = info;
        this.packStore = packStore;
        this.fileSystem = fileSystem;
    }

    protected ModpackModel() {
        buildName = InstalledPack.RECOMMENDED;
        isPlatform = true;
    }

    public boolean isOfficial() {
        if (packInfo == null)
            return false;

        return packInfo.isOfficial();
    }

    public InstalledPack getInstalledPack() {
        return installedPack;
    }

    public PackInfo getPackInfo() {
        return packInfo;
    }

    public void setInstalledPack(InstalledPack pack, InstalledPackStore packStore) {
        installedPack = pack;
        this.packStore = packStore;
    }

    public void setPackInfo(PackInfo packInfo) {

        //HACK
        //I need to rework the way platform & solder data interact to produce a complete pack, but until I do so, this
        //awesome hack will combine platform & solder data where necessary
        if (packInfo instanceof SolderPackInfo && this.packInfo instanceof PlatformPackInfo) {
            this.packInfo = new CombinedPackInfo(packInfo, this.packInfo);
        } else if (packInfo instanceof PlatformPackInfo && this.packInfo instanceof SolderPackInfo) {
            this.packInfo = new CombinedPackInfo(this.packInfo, packInfo);
        } else if (packInfo instanceof SolderPackInfo && this.packInfo instanceof CombinedPackInfo) {
            this.packInfo = new CombinedPackInfo(packInfo, this.packInfo);
        } else if (packInfo instanceof PlatformPackInfo && this.packInfo instanceof CombinedPackInfo) {
            this.packInfo = new CombinedPackInfo(this.packInfo, packInfo);
        } else {
            this.packInfo = packInfo;
        }
    }

    public String getDiscordId() {
        if (packInfo != null)
            return packInfo.getDiscordId();
        else
            return null;
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

    public void setBuild(String build) {
        if (installedPack != null) {
            installedPack.setBuild(build);
            save();
        } else
            buildName = build;
    }

    public String getBuild() {
        if (installedPack != null) {
            return installedPack.getBuild();
        } else
            return buildName;
    }

    public List<String> getBuilds() {
        if (packInfo != null && packInfo.getBuilds() != null)
            return packInfo.getBuilds();

        List<String> oneBuild = new ArrayList<>(1);

        ModpackVersion version = getInstalledVersion();

        if (version != null)
            oneBuild.add(version.getVersion());
        else
            oneBuild.add(getBuild());

        return oneBuild;
    }

    public String getRecommendedBuild() {
        if (packInfo != null && packInfo.getRecommended() != null)
            return packInfo.getRecommended();
        else
            return getBuild();
    }

    public String getLatestBuild() {
        if (packInfo != null && packInfo.getLatest() != null)
            return packInfo.getLatest();
        else
            return getBuild();
    }

    public String getWebSite() {
        if (getPackInfo() == null)
            return null;

        return getPackInfo().getWebSite();
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

    public List<FeedItem> getFeed() {
        if (packInfo == null)
            return Collections.emptyList();

        return packInfo.getFeed();
    }

    public boolean isLocalOnly() {
        if (packInfo == null)
            return true;
        return packInfo.isLocal();
    }

    public ModpackVersion getInstalledVersion() {
        File versionFile = new File(getBinDir(), "version");
        if (versionFile.exists()) {
            return ModpackVersion.load(versionFile);
        } else {
            return null;
        }
    }

    public boolean hasRecommendedUpdate() {
        if (installedPack == null || packInfo == null)
            return false;

        ModpackVersion installedVersion = getInstalledVersion();

        if (installedVersion == null)
            return false;

        String installedBuild = installedVersion.getVersion();

        List<String> allBuilds = packInfo.getBuilds();

        if (allBuilds.isEmpty())
            return false;

        if (!allBuilds.contains(installedBuild))
            return true;

        for (String build : allBuilds) {
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

    public boolean isServerPack() {
        if (packInfo == null)
            return false;

        return packInfo.isServerPack();
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

    public Integer getInstalls() {
        if (packInfo == null)
            return null;

        return packInfo.getInstalls();
    }

    public RunData getRunData() {
        File runDataFile = new File(getBinDir(), "runData");

        if (!runDataFile.exists())
            return null;

        try (Reader reader = Files.newBufferedReader(runDataFile.toPath(), StandardCharsets.UTF_8)) {
            return Utils.getGson().fromJson(reader, RunData.class);
        } catch (JsonParseException | IOException e) {
            Utils.getLogger().log(Level.SEVERE, String.format("Error reading runData file %s, returning null", runDataFile.getAbsolutePath()), e);
            return null;
        }
    }

    public File getInstalledDirectory() {
        if (installedPack == null)
            return null;

        if (installedDirectory == null) {
            String rawDir = installedPack.getDirectory();

            if (rawDir == null) {
                return null;
            }

            if (rawDir.startsWith(InstalledPack.LAUNCHER_DIR)) {
                rawDir = fileSystem.getRootDirectory().resolve(rawDir.substring(InstalledPack.LAUNCHER_DIR.length())).toString();
            }
            if (rawDir.startsWith(InstalledPack.MODPACKS_DIR)) {
                rawDir = fileSystem.getModpacksDirectory().resolve(rawDir.substring(InstalledPack.MODPACKS_DIR.length())).toString();
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
            } catch (IOException e) {
                Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);
                return;
            }
        }

        installedDirectory = targetDirectory;
        Path path = installedDirectory.toPath().toAbsolutePath();

        String newInstalledPackDir;

        final Path modpacksDir = fileSystem.getModpacksDirectory();
        final Path launcherRootDir = fileSystem.getRootDirectory();

        if (path.equals(modpacksDir)) {
            newInstalledPackDir = InstalledPack.MODPACKS_DIR;
        } else if (path.equals(launcherRootDir)) {
            newInstalledPackDir = InstalledPack.LAUNCHER_DIR;
        } else if (path.startsWith(modpacksDir)) {
            newInstalledPackDir = InstalledPack.MODPACKS_DIR + modpacksDir.relativize(path);
        } else if (path.startsWith(launcherRootDir)) {
            newInstalledPackDir = InstalledPack.LAUNCHER_DIR + launcherRootDir.relativize(path);
        } else {
            newInstalledPackDir = path.toString();
        }

        // Only need to save if we've actually changed the raw directory
        if (!newInstalledPackDir.equals(installedPack.getDirectory())) {
            installedPack.setDirectory(newInstalledPackDir);
            save();
        }
    }

    public void save() {
        if (installedPack == null) {
            installedPack = new InstalledPack(getName(), getBuild());
        }

        packStore.put(installedPack);
    }

    public boolean isSelected() {
        String selectedSlug = packStore.getSelectedSlug();

        if (selectedSlug == null)
            select();

        return (selectedSlug == null || selectedSlug.equalsIgnoreCase(getName()));
    }

    public void select() {
        String storedSelection = packStore.getSelectedSlug();
        String name = getName();

        if (name == null) {
            return;
        }

        // Only save if either of the following:
        // - The stored selection is null (e.g. new launcher installs)
        // - The current modpack slug is different from the stored selection
        if (storedSelection == null || !storedSelection.equals(name)) {
            packStore.setSelectedSlug(name);
        }
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void updateTags(IModpackTagBuilder tagBuilder) {
        this.tags.clear();

        if (tagBuilder != null) {
            for (String tag : tagBuilder.getModpackTags(this)) {
                this.tags.add(tag);
            }
        }
    }

    public int getPriority() {
        return priority;
    }

    public void updatePriority(int priority) {
        if (this.priority < priority) {
            this.priority = priority;
        }

        if (this.priority == -1 && this.packInfo != null && this.packInfo.isComplete()) {
            if (this.packInfo.isOfficial())
                this.priority = 5000;
            else
                this.priority = 1000;
        }
    }

    public void resetPack() {
        if (installedPack != null && getBinDir() != null) {
            File version = new File(getBinDir(), "version");

            if (version.exists())
                version.delete();
        }
    }

    public void delete() {
        if (getInstalledDirectory() != null && getInstalledDirectory().exists()) {
            try {
                FileUtils.deleteDirectory(getInstalledDirectory());
            } catch (IOException e) {
                Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }

        Path assets = fileSystem.getPackAssetsDirectory().resolve(getName());
        if (Files.isDirectory(assets)) {
            try {
                FileUtils.deleteDirectory(assets.toFile());
            } catch (IOException e) {
                Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }

        packStore.remove(getName());
        installedPack = null;
        installedDirectory = null;
    }
}
