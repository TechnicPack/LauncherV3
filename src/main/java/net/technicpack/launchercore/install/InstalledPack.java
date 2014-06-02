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

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.mirror.download.Download;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.Resource;
import net.technicpack.launchercore.util.MD5Utils;
import net.technicpack.launchercore.util.ResourceUtils;
import net.technicpack.launchercore.util.Utils;
import org.apache.commons.io.FileUtils;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class InstalledPack {
    public static final String RECOMMENDED = "recommended";
    public static final String LATEST = "latest";
    public static final String LAUNCHER_DIR = "launcher\\";
    public static final String MODPACKS_DIR = "%MODPACKS%\\";

    private static BufferedImage BACKUP_LOGO;
    private static BufferedImage BACKUP_BACKGROUND;
    private static BufferedImage BACKUP_ICON;
    private transient AtomicReference<BufferedImage> logo = new AtomicReference<BufferedImage>();
    private transient AtomicReference<BufferedImage> background = new AtomicReference<BufferedImage>();
    private transient AtomicReference<BufferedImage> icon = new AtomicReference<BufferedImage>();
    private transient HashMap<AtomicReference<BufferedImage>, AtomicReference<Boolean>> downloading = new HashMap<AtomicReference<BufferedImage>, AtomicReference<Boolean>>(3);
    private transient File installedDirectory;
    private transient File binDir;
    private transient File configDir;
    private transient File savesDir;
    private transient File cacheDir;
    private transient File resourceDir;
    private transient File modsDir;
    private transient File coremodsDir;
    private transient PackInfo info;
    private transient PackRefreshListener refreshListener;
    private transient MirrorStore mirrorStore;
    private String name;
    private boolean platform;
    private String build;
    private String directory;

    private transient boolean isLocalOnly;

    public InstalledPack(MirrorStore mirrorStore, String name, boolean platform, String build, String directory) {
        this();
        this.mirrorStore = mirrorStore;
        this.name = name;
        this.platform = platform;
        this.build = build;
        this.directory = directory;
    }

    public InstalledPack(MirrorStore mirrorStore, String name, boolean platform) {
        this(mirrorStore, name, platform, RECOMMENDED, MODPACKS_DIR + name);
    }

    public InstalledPack() {
        downloading.put(logo, new AtomicReference<Boolean>(false));
        downloading.put(background, new AtomicReference<Boolean>(false));
        downloading.put(icon, new AtomicReference<Boolean>(false));
        isLocalOnly = false;
        build = RECOMMENDED;
    }

    public void setMirrorStore(MirrorStore mirrorStore) {
        this.mirrorStore = mirrorStore;
    }

    public void setRefreshListener(PackRefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    public String getDirectory() {
        String path = directory;
        if (directory != null && directory.startsWith(LAUNCHER_DIR)) {
            path = new File(Utils.getLauncherDirectory(), directory.substring(9)).getAbsolutePath();
        }
        if (directory != null && directory.startsWith(MODPACKS_DIR)) {
            path = new File(Utils.getModpacksDirectory(), directory.substring(11)).getAbsolutePath();
        }
        return path;
    }

    public void initDirectories() {
        binDir = new File(installedDirectory, "bin");
        configDir = new File(installedDirectory, "config");
        savesDir = new File(installedDirectory, "saves");
        cacheDir = new File(installedDirectory, "cache");
        resourceDir = new File(installedDirectory, "resources");
        modsDir = new File(installedDirectory, "mods");
        coremodsDir = new File(installedDirectory, "coremods");

        binDir.mkdirs();
        configDir.mkdirs();
        savesDir.mkdirs();
        cacheDir.mkdirs();
        resourceDir.mkdirs();
        modsDir.mkdirs();
        coremodsDir.mkdirs();
    }

    public String getDisplayName() {
        if (info == null) {
            return name;
        }
        return info.getDisplayName();
    }

    public boolean isPlatform() {
        return platform;
    }

    public PackInfo getInfo() {
        return info;
    }

    public void setInfo(PackInfo info) {
        this.info = info;
        this.isLocalOnly = false;
    }

    public boolean isLocalOnly() {
        return isLocalOnly;
    }

    public void setLocalOnly() {
        this.isLocalOnly = true;
    }

    public boolean hasLogo() {
        return (getLogo() != BACKUP_LOGO);
    }

    public String getBuild() {
        if (info != null) {
            if (build.equals(RECOMMENDED)) {
                return info.getRecommended();
            }
            if (build.equals(LATEST)) {
                return info.getLatest();
            }
        }

        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getRawBuild() {
        return build;
    }

    public File getInstalledDirectory() {
        if (installedDirectory == null) {
            setPackDirectory(new File(getDirectory()));
        }
        return installedDirectory;
    }

    public void setPackDirectory(File packPath) {
        if (installedDirectory != null) {
            try {
                FileUtils.copyDirectory(installedDirectory, packPath);
                FileUtils.cleanDirectory(installedDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        installedDirectory = packPath;
        String path = installedDirectory.getAbsolutePath();
        if (path.equals(Utils.getModpacksDirectory().getAbsolutePath())) {
            directory = MODPACKS_DIR;
        } else if (path.equals(Utils.getLauncherDirectory().getAbsolutePath())) {
            directory = LAUNCHER_DIR;
        } else if (path.startsWith(Utils.getModpacksDirectory().getAbsolutePath())) {
            directory = MODPACKS_DIR + path.substring(Utils.getModpacksDirectory().getAbsolutePath().length() + 1);
        } else if (path.startsWith(Utils.getLauncherDirectory().getAbsolutePath())) {
            directory = LAUNCHER_DIR + path.substring(Utils.getLauncherDirectory().getAbsolutePath().length() + 1);
        }
        initDirectories();
    }

    public File getBinDir() {
        return binDir;
    }

    public File getConfigDir() {
        return configDir;
    }

    public File getSavesDir() {
        return savesDir;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public File getResourceDir() {
        return resourceDir;
    }

    public File getModsDir() {
        return modsDir;
    }

    public File getCoremodsDir() {
        return coremodsDir;
    }

    public synchronized BufferedImage getLogo() {
        if (logo.get() != null) {
            return logo.get();
        } else {
            Resource resource = info != null ? info.getLogo() : null;
            if (loadImage(logo, "logo.png", resource)) {
                return logo.get();
            }
        }

        if (BACKUP_LOGO == null) {
            BACKUP_LOGO = loadBackup("/org/spoutcraft/launcher/resources/noLogo.png");
        }
        return BACKUP_LOGO;
    }

    private boolean loadImage(AtomicReference<BufferedImage> image, String name, Resource resource) {
        File assets = new File(Utils.getAssetsDirectory(), "packs");
        File packs = new File(assets, getName());
        packs.mkdirs();
        File resourceFile = new File(packs, name);

        String url = "";
        String md5 = "";

        if (resource != null) {
            url = resource.getUrl();
            md5 = resource.getMd5();
        }

        boolean cached = loadCachedImage(image, resourceFile, url, md5);

        if (!cached) {
            downloadImage(image, resourceFile, url, md5);
        }

        if (image.get() == null) {
            return false;
        }

        return cached;
    }

    private boolean loadCachedImage(AtomicReference<BufferedImage> image, File file, String url, String md5) {
        try {
            if (!file.exists())
                return false;

            boolean reloadImage = (url.isEmpty() || md5.isEmpty());

            if (!reloadImage) {
                String fileMd5 = MD5Utils.getMD5(file);

                if (fileMd5 != null && !fileMd5.isEmpty())
                    reloadImage = fileMd5.equalsIgnoreCase(md5);
            }

            if (reloadImage) {
                BufferedImage newImage;
                newImage = ImageIO.read(file);
                image.set(newImage);
                return true;
            }
        } catch (IIOException e) {
            Utils.getLogger().log(Level.INFO, "Failed to load image " + file.getAbsolutePath() + " from file.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void downloadImage(final AtomicReference<BufferedImage> image, final File temp, final String url, final String md5) {
        if (url.isEmpty() || downloading.get(image).get()) {
            return;
        }

        downloading.get(image).set(true);
        final String name = getName();
        final InstalledPack pack = this;
        final MirrorStore mirror = mirrorStore;
        Thread thread = new Thread(name + " Image Download Worker") {
            @Override
            public void run() {
                try {
                    if (temp.exists()) {
                        System.out.println("Pack: " + getName() + " Calculated MD5: " + MD5Utils.getMD5(temp) + " Required MD5: " + md5);
                    }
                    Download download = mirror.downloadFile(url, temp.getName(), temp.getAbsolutePath());
                    BufferedImage newImage;
                    newImage = ImageIO.read(download.getOutFile());
                    image.set(newImage);
                    downloading.get(image).set(false);
                    if (refreshListener != null) {
                        refreshListener.refreshPack(pack);
                    }
                } catch (IOException e) {
                    System.out.println("Failed to download and load image from: " + url);
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public String getName() {
        return name;
    }

    private BufferedImage loadBackup(String backup) {
        try {
            return ImageIO.read(ResourceUtils.getResourceAsStream(backup));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized BufferedImage getBackground() {
        if (background.get() != null) {
            return background.get();
        } else {
            Resource resource = info != null ? info.getBackground() : null;
            if (loadImage(background, "background.jpg", resource)) {
                return background.get();
            }
        }

        if (BACKUP_BACKGROUND == null) {
            BACKUP_BACKGROUND = loadBackup("/org/spoutcraft/launcher/resources/background.jpg");
        }
        return BACKUP_BACKGROUND;
    }

    public synchronized BufferedImage getIcon() {
        if (icon.get() != null) {
            return icon.get();
        } else {
            Resource resource = info != null ? info.getIcon() : null;
            if (loadImage(icon, "icon.png", resource)) {
                return icon.get();
            }
        }

        if (BACKUP_ICON == null) {
            BACKUP_ICON = loadBackup("/org/spoutcraft/launcher/resources/icon.png");
        }
        return BACKUP_ICON;
    }

    public String getIconPath() {
        return Utils.getAssetsDirectory() + "/packs/" + getName() + "/icon.png";
    }

    @Override
    public String toString() {
        return "InstalledPack{" +
                "info=" + info +
                ", name='" + name + '\'' +
                ", platform=" + platform +
                ", build='" + build + '\'' +
                ", directory='" + directory + '\'' +
                '}';
    }
}
