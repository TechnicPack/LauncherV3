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

import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.launchercore.modpacks.sources.IInstalledPackRepository;
import net.technicpack.launchercore.modpacks.sources.IModpackTagBuilder;
import net.technicpack.launchercore.modpacks.sources.IPackSource;

import java.util.Collection;

public class PackLoader {
    private IInstalledPackRepository packRepository;
    private IAuthoritativePackSource authoritativeSource;
    private LauncherDirectories directories;

    public PackLoader(LauncherDirectories directories, IInstalledPackRepository packStore, IAuthoritativePackSource packInfos) {
        this.packRepository = packStore;
        this.authoritativeSource = packInfos;
        this.directories = directories;
    }

    public PackLoadJob createRepositoryLoadJob(IModpackContainer container, Collection<IPackSource> packSources, IModpackTagBuilder tagBuilder, boolean doLoadRepository) {
        PackLoadJob job = new PackLoadJob(directories, packRepository, authoritativeSource, packSources, container, tagBuilder, doLoadRepository);
        Thread thread = new Thread(job);
        thread.start();
        return job;
    }
}
