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
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.launchercore.modpacks.sources.IModpackTagBuilder;
import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.rest.io.PackInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PackLoadJob implements Runnable {
    private LauncherDirectories directories;
    private IModpackTagBuilder tagBuilder;
    private IAuthoritativePackSource authoritativeSource;
    private IInstalledPackRepository packRepository;
    private Collection<IPackSource> packSources;
    private IModpackContainer container;
    private boolean doLoadRepository;
    private Map<String, ModpackModel> processedModpacks = new HashMap<String, ModpackModel>();

    private boolean isCancelled = false;

    public PackLoadJob(LauncherDirectories directories, IInstalledPackRepository packRepository, IAuthoritativePackSource authoritativeSource, Collection<IPackSource> packSources, IModpackContainer container, IModpackTagBuilder tagBuilder, boolean doLoadRepository) {
        this.packRepository = packRepository;
        this.authoritativeSource = authoritativeSource;
        this.packSources = packSources;
        this.container = container;
        this.tagBuilder = tagBuilder;
        this.directories = directories;
        this.doLoadRepository = doLoadRepository;
        container.clear();
    }

    //Stop adding & updating packs from this job.  Used for instance in the search bar: if the user types out 3 letters
    //we want to search for what they typed, but if they keep typing we want to cancel the created job and make a new one

    //This method forces the cancel to occur on the dispatch thread, since addPack always takes place on the dispatch thread,
    //so we don't have to worry about an addPack being halfway through completion if this object is saying it's cancelled
    public void cancel() {
        if (EventQueue.isDispatchThread()) {
            isCancelled = true;
        } else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    cancel();
                }
            });
        }
    }

    public boolean isCancelled() { return isCancelled; }

    @Override
    public void run() {

        int threadCount = 0;
        if (doLoadRepository)
            threadCount++;
        if (packSources != null)
            threadCount += packSources.size();
        Collection<Thread> threads = new ArrayList<Thread>(threadCount);

        if (doLoadRepository) {
            for (final String packName : packRepository.getPackNames()) {
                final InstalledPack pack = packRepository.getInstalledPacks().get(packName);
                Thread infoLoadThread = new Thread(pack.getName() + " Info Loading Thread") {
                    @Override
                    public void run() {
                        PackInfo info = authoritativeSource.getPackInfo(pack);
                        addPackThreadSafe(pack, info);
                    }
                };

                threads.add(infoLoadThread);
                infoLoadThread.start();
            }
        }

        if (packSources != null) {
            for (final IPackSource packSource : packSources) {
                Thread packSourceThread = new Thread(packSource.getSourceName() + " Loading Thread") {
                    @Override
                    public void run() {
                        for (PackInfo info : packSource.getPublicPacks()) {
                            addPackThreadSafe(null, info);
                        }
                    }
                };

                threads.add(packSourceThread);
                packSourceThread.start();
            }
        }

        for(Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {}
        }

        refreshCompleteThreadSafe();
    }

    protected void refreshCompleteThreadSafe() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                container.refreshComplete();
            }
        });
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
        if (pack == null && packInfo == null || isCancelled)
            return;

        String name = (pack != null)?pack.getName():packInfo.getName();

        ModpackModel modpack = null;
        if (processedModpacks.containsKey(name)) {
            modpack = processedModpacks.get(name);
            if (modpack.getInstalledPack() == null && pack != null) {
                modpack.setInstalledPack(pack, packRepository);
            }

            if (packInfo != null) {
                modpack.setPackInfo(packInfo);
            }
        } else {
            modpack = new ModpackModel(pack, packInfo, packRepository, directories);

            if (packInfo == null)
                modpack.setIsPlatform(false);

            processedModpacks.put(name, modpack);
        }

        if (modpack != null && tagBuilder != null)
            modpack.updateTags(tagBuilder);

        container.addOrReplace(modpack);
    }
}
