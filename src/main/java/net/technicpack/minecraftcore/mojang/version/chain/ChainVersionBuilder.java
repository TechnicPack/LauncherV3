/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.version.chain;

import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.MojangVersionBuilder;

import java.io.IOException;

public class ChainVersionBuilder implements MojangVersionBuilder {

    private MojangVersionBuilder primaryVersionBuilder;
    private MojangVersionBuilder chainedVersionBuilder;

    public ChainVersionBuilder(MojangVersionBuilder primaryVersionBuilder, MojangVersionBuilder chainedVersionBuilder) {
        this.primaryVersionBuilder = primaryVersionBuilder;
        this.chainedVersionBuilder = chainedVersionBuilder;
    }

    public MojangVersion buildVersionFromKey(String key) throws InterruptedException, IOException {
        MojangVersion primary = primaryVersionBuilder.buildVersionFromKey(key);

        if (primary == null)
            return null;

        VersionChain chain = new VersionChain(primary);

        MojangVersion latest = primary;

        while (latest.getParentVersion() != null) {
            latest = chainedVersionBuilder.buildVersionFromKey(latest.getParentVersion());

            if (latest == null)
                return null;

            chain.addVersionToChain(latest);
        }

        return chain;
    }
}
