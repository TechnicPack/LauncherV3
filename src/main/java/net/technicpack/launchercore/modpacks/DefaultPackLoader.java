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

import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.modpacks.sources.IModpackTagBuilder;
import net.technicpack.launchercore.modpacks.sources.IPackSource;

import java.util.Collection;
import java.util.LinkedList;

public class DefaultPackLoader implements IAuthListener<IUserType> {
    private PackLoader loader;
    private Collection<IModpackContainer> registeredContainers = new LinkedList<IModpackContainer>();
    private Collection<IPackSource> packSources;
    private IModpackTagBuilder tagBuilder;

    public DefaultPackLoader(PackLoader loader, Collection<IPackSource> packSources, IModpackTagBuilder tagBuilder) {
        this.loader = loader;
        this.packSources = packSources;
        this.tagBuilder = tagBuilder;
    }

    public void registerModpackContainer(IModpackContainer container) {
        registeredContainers.add(container);

        loader.createRepositoryLoadJob(container, packSources, tagBuilder, true);
    }

    public void removeModpackContainer(IModpackContainer container) {
        registeredContainers.remove(container);
    }

    protected void refreshAllContainers() {
        for(IModpackContainer container : registeredContainers) {
            loader.createRepositoryLoadJob(container, packSources, tagBuilder, true);
        }
    }

    @Override
    public void userChanged(IUserType user) {
        refreshAllContainers();
    }
}
