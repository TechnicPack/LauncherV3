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

import net.technicpack.launcher.io.TechnicInstalledPackStore;
import net.technicpack.launcher.io.TechnicLauncherDirectories;
import net.technicpack.launcher.io.TechnicSkinMapper;
import net.technicpack.launcher.io.TechnicUserStore;
import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.LoginFrame;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.image.MinotarSkinStore;
import net.technicpack.launchercore.image.SkinRepository;
import net.technicpack.launchercore.modpacks.AvailablePackList;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.launchercore.modpacks.sources.IPackInfoRepository;
import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.minecraftcore.LauncherDirectories;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.mirror.secure.rest.JsonWebSecureMirror;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.PlatformPackInfoRepository;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.SolderPackSource;
import net.technicpack.solder.http.HttpSolderApi;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class LauncherMain {
    public static void main(String[] args) {
        TechnicSettings settings = SettingsFactory.buildSettingsObject();
        startLauncher(settings);
    }

    private static void startLauncher(TechnicSettings settings) {
        LauncherDirectories directories = new TechnicLauncherDirectories(settings.getTechnicRoot());
        ResourceLoader resources = new ResourceLoader("net","technicpack","launcher","resources");
        resources.setLocale("default");

        UserModel userModel = new UserModel(TechnicUserStore.load(new File(directories.getLauncherDirectory(),"users.json")));

        MirrorStore mirrorStore = new MirrorStore(userModel);
        mirrorStore.addSecureMirror("mirror.technicpack.net", new JsonWebSecureMirror("http://mirror.technicpack.net/", "mirror.technicpack.net"));

        SkinRepository skinRepo = new SkinRepository(new TechnicSkinMapper(directories), new MinotarSkinStore("https://minotar.net/", mirrorStore));

        ISolderApi solder = new HttpSolderApi(settings.getClientId(), userModel);
        IPlatformApi platform = new HttpPlatformApi("http://www.technicpack.net/api/", mirrorStore);

        IInstalledPackRepository packStore = TechnicInstalledPackStore.load(new File(directories.getLauncherDirectory(), "installedPacks"));
        IPackInfoRepository packInfoRepository = new PlatformPackInfoRepository(platform, solder);
        ArrayList<IPackSource> packSources = new ArrayList<IPackSource>();
        packSources.add(new SolderPackSource("http://solder.technicpack.net/api/", solder));

        AvailablePackList packList = new AvailablePackList(directories, packStore, packInfoRepository, packSources);
        userModel.addAuthListener(packList);

        LauncherFrame frame = new LauncherFrame(resources, skinRepo, userModel, settings, packList);
        userModel.addAuthListener(frame);

        LoginFrame login = new LoginFrame(resources, userModel, skinRepo);
        userModel.addAuthListener(login);

        userModel.initAuth();
    }
}
