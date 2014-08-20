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

package net.technicpack.launcher;

import com.beust.jcommander.JCommander;
import net.technicpack.launcher.io.*;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.launch.Installer;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.LoginFrame;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.mojang.auth.AuthenticationService;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.image.face.MinotarFaceImageStore;
import net.technicpack.launchercore.image.face.WebAvatarImageStore;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.minecraftcore.launch.MinecraftLauncher;
import net.technicpack.launchercore.modpacks.AvailablePackList;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.resources.PackImageStore;
import net.technicpack.launchercore.modpacks.resources.PackResourceMapper;
import net.technicpack.launchercore.modpacks.resources.resourcetype.BackgroundResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IModpackResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.IconResourceType;
import net.technicpack.launchercore.modpacks.resources.resourcetype.LogoResourceType;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.launchercore.modpacks.sources.IPackInfoRepository;
import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.mirror.secure.rest.JsonWebSecureMirror;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.PlatformPackInfoRepository;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.SolderPackSource;
import net.technicpack.solder.http.HttpSolderApi;

import java.io.File;
import java.util.ArrayList;

public class LauncherMain {
    public static void main(String[] args) {
        StartupParameters params = new StartupParameters(args);
        try {
            new JCommander(params, args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        TechnicSettings settings = SettingsFactory.buildSettingsObject();
        startLauncher(settings, params);
    }

    private static void startLauncher(TechnicSettings settings, StartupParameters startupParameters) {
        LauncherDirectories directories = new TechnicLauncherDirectories(settings.getTechnicRoot());
        ResourceLoader resources = new ResourceLoader("net","technicpack","launcher","resources");
        resources.setLocale(settings.getLanguageCode());

        UserModel userModel = new UserModel(TechnicUserStore.load(new File(directories.getLauncherDirectory(),"users.json")), new AuthenticationService());

        MirrorStore mirrorStore = new MirrorStore(userModel);
        mirrorStore.addSecureMirror("mirror.technicpack.net", new JsonWebSecureMirror("http://mirror.technicpack.net/", "mirror.technicpack.net"));

        IModpackResourceType iconType = new IconResourceType();
        IModpackResourceType logoType = new LogoResourceType();
        IModpackResourceType backgroundType = new BackgroundResourceType();

        PackResourceMapper iconMapper = new PackResourceMapper(directories, resources.getImage("icon.png"), iconType);
        ImageRepository<ModpackModel> iconRepo = new ImageRepository<ModpackModel>(iconMapper, new PackImageStore(iconType, mirrorStore, userModel));
        ImageRepository<ModpackModel> logoRepo = new ImageRepository<ModpackModel>(new PackResourceMapper(directories, resources.getImage("modpack/ModImageFiller.png"), logoType), new PackImageStore(logoType, mirrorStore, userModel));
        ImageRepository<ModpackModel> backgroundRepo = new ImageRepository<ModpackModel>(new PackResourceMapper(directories, null, backgroundType), new PackImageStore(backgroundType, mirrorStore, userModel));

        ImageRepository<IUserType> skinRepo = new ImageRepository<IUserType>(new TechnicFaceMapper(directories, resources), new MinotarFaceImageStore("https://minotar.net/", mirrorStore));

        ImageRepository<AuthorshipInfo> avatarRepo = new ImageRepository<AuthorshipInfo>(new TechnicAvatarMapper(directories, resources), new WebAvatarImageStore(mirrorStore));

        ISolderApi solder = new HttpSolderApi(settings.getClientId(), userModel);
        IPlatformApi platform = new HttpPlatformApi("http://tplatform.gopagoda.com/api/", mirrorStore);

        IInstalledPackRepository packStore = TechnicInstalledPackStore.load(new File(directories.getLauncherDirectory(), "installedPacks"));
        IPackInfoRepository packInfoRepository = new PlatformPackInfoRepository(platform, solder);
        ArrayList<IPackSource> packSources = new ArrayList<IPackSource>();
        packSources.add(new SolderPackSource("http://solder.technicpack.net/api/", solder));

        AvailablePackList packList = new AvailablePackList(directories, packStore, packInfoRepository, packSources);
        userModel.addAuthListener(packList);

        MinecraftLauncher launcher = new MinecraftLauncher(platform, directories, userModel, settings.getClientId());
        ModpackInstaller modpackInstaller = new ModpackInstaller(platform, settings.getClientId());
        Installer installer = new Installer(startupParameters, mirrorStore, directories, modpackInstaller, launcher, settings, iconMapper);

        LauncherFrame frame = new LauncherFrame(resources, skinRepo, userModel, settings, packList, iconRepo, logoRepo, backgroundRepo, installer, avatarRepo, platform);
        userModel.addAuthListener(frame);

        LoginFrame login = new LoginFrame(resources, settings, userModel, skinRepo);
        userModel.addAuthListener(login);

        userModel.initAuth();
    }
}
