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

import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.MojangVersionBuilder;

import java.io.IOException;
import java.util.regex.Pattern;

public class ChainVersionBuilder implements MojangVersionBuilder {
    private static final Pattern MINECRAFT_VERSION_PATTERN = Pattern.compile("^\\d++(\\.\\d++)++$");

    private MojangVersionBuilder primaryVersionBuilder;
    private MojangVersionBuilder chainedVersionBuilder;

    public ChainVersionBuilder(MojangVersionBuilder primaryVersionBuilder, MojangVersionBuilder chainedVersionBuilder) {
        this.primaryVersionBuilder = primaryVersionBuilder;
        this.chainedVersionBuilder = chainedVersionBuilder;
    }

    public IMinecraftVersionInfo buildVersionFromKey(String key) throws InterruptedException, IOException {
        IMinecraftVersionInfo primary = primaryVersionBuilder.buildVersionFromKey(key);

        if (primary == null)
            return null;

        ChainedMinecraftVersionInfo chain = new ChainedMinecraftVersionInfo(primary);

        IMinecraftVersionInfo latest = primary;

        while (latest.getParentVersion() != null) {
            latest = chainedVersionBuilder.buildVersionFromKey(latest.getParentVersion());

            if (latest == null)
                return null;

            chain.addVersionToChain(latest);
        }

        if (latest.getDownloads() == null) {
            // HACK!
            // For some reason the last version in the chain doesn't have any downloads/probably isn't a Mojang version file.
            // This happens with Attack of the B-Team, because Forge 1.6.4 has "null" set in inheritsFrom, so the
            // launcher never attempts to download the Mojang version json from our repo.
            // So, we just guess the Minecraft version and forcefully add the Mojang version file here.

            String[] parts = latest.getId().split("-");
            if (!MINECRAFT_VERSION_PATTERN.matcher(parts[0]).matches())
                throw new IOException("Latest version in version chain failed to resolve to a Minecraft version");

            chain.addVersionToChain(chainedVersionBuilder.buildVersionFromKey(parts[0]));
        }

        return chain;
    }
}
