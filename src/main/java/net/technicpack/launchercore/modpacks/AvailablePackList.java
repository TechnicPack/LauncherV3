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

import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.launchercore.modpacks.sources.IPackInfoRepository;
import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.minecraftcore.LauncherDirectories;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.User;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.utilslib.Utils;

import java.awt.EventQueue;
import java.util.*;
import java.util.logging.Level;

public class AvailablePackList implements IAuthListener {
	private IInstalledPackRepository packStore;
    private IPackInfoRepository packInfos;
    private Collection<IPackSource> packSources;
    private LauncherDirectories directories;

    private Map<String, ModpackModel> processedModpacks = new HashMap<String, ModpackModel>();
    private Collection<IModpackContainer> registeredContainers = new LinkedList<IModpackContainer>();

	public AvailablePackList(LauncherDirectories directories, IInstalledPackRepository packStore, IPackInfoRepository packInfos, Collection<IPackSource> packSources) {
		this.packStore = packStore;
        this.packInfos = packInfos;
        this.packSources = packSources;
        this.directories = directories;
	}

	@Override
	public void userChanged(User user) {
        if (user != null)
		    reloadAllPacks();
	}

    public void addRegisteredContainer(IModpackContainer container) {
        registeredContainers.add(container);

        container.clear();

        for(ModpackModel modpack : processedModpacks.values()) {
            container.addOrReplace(modpack);
        }
     }

    public void removeRegisteredContainer(IModpackContainer container) {
        registeredContainers.remove(container);
    }

    public void reloadAllPacks() {
        clearContainersThreadSafe();

        for (final String packName : packStore.getPackNames()) {
            final InstalledPack pack = packStore.getInstalledPacks().get(packName);
            Thread infoLoadThread = new Thread(pack.getName() + " Info Loading Thread") {
                @Override
                public void run() {
                    PackInfo info = packInfos.getPackInfo(pack);
                    addPackThreadSafe(pack, info);
                }
            };

            infoLoadThread.start();
        }

        for (final IPackSource packSource : packSources) {
            Thread packSourceThread = new Thread(packSource.getSourceName() + " Loading Thread") {
                @Override
                public void run() {
                    for(PackInfo info : packSource.getPublicPacks()) {
                        addPackThreadSafe(null, info);
                    }
                }
            };

            packSourceThread.start();
        }
    }

    protected void clearContainersThreadSafe() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                clearContainers();
            }
        });
    }

    protected void clearContainers() {
        for(IModpackContainer container : registeredContainers) {
            container.clear();
        }
    }

    protected void addPackThreadSafe(final InstalledPack pack, final PackInfo packInfo) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                addPack(pack, packInfo);
            }
        });
    }

    protected void addPack(InstalledPack pack, PackInfo packInfo) {
        if (pack == null && packInfo == null)
            return;

        String name = (pack != null)?pack.getName():packInfo.getName();

        ModpackModel modpack = null;
        if (processedModpacks.containsKey(name)) {
            modpack = processedModpacks.get(name);
            if (modpack.getInstalledPack() == null && pack != null) {
                modpack.setInstalledPack(pack, packStore);
            }

            if (packInfo != null) {
                modpack.setPackInfo(packInfo);
            }
        } else {
            modpack = new ModpackModel(pack, packInfo, packStore, directories);

            if (pack == null)
                modpack.setIsPlatform(false);

            processedModpacks.put(name, modpack);
        }

        modpack.checkImages();

        for(IModpackContainer container : registeredContainers) {
            container.addOrReplace(modpack);
        }
    }
}
