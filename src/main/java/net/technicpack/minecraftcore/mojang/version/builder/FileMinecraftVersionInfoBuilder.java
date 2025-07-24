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

package net.technicpack.minecraftcore.mojang.version.builder;

import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.MinecraftVersionInfoBuilder;
import net.technicpack.minecraftcore.mojang.version.io.MinecraftVersionInfo;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class FileMinecraftVersionInfoBuilder implements MinecraftVersionInfoBuilder {
    private File version;
    private MinecraftVersionInfoRetriever retriever;
    private List<MinecraftVersionInfoRetriever> fallbackRetrievers;

    public FileMinecraftVersionInfoBuilder(File version, MinecraftVersionInfoRetriever retriever, List<MinecraftVersionInfoRetriever> fallbackRetrievers) {
        this.version = version;
        this.retriever = retriever;
        this.fallbackRetrievers = fallbackRetrievers;
    }

    @Override
    public IMinecraftVersionInfo buildVersionFromKey(String key) throws InterruptedException, IOException {
        File target = version;

        if (key != null) {
            if (version.isDirectory()) {
                String targetFile = key + ".json";
                target = new File(version, targetFile);
            }

            if (retriever != null)
                retriever.retrieveVersion(target, key);

            if (fallbackRetrievers != null) {
                for (MinecraftVersionInfoRetriever fallbackRetriever : fallbackRetrievers) {
                    if (target.exists())
                        break;

                    fallbackRetriever.retrieveVersion(target, key);
                }
            }
        }

        if (!target.exists())
            return null;

        try (Reader reader = Files.newBufferedReader(target.toPath(), StandardCharsets.UTF_8)) {
            return MojangUtils.getGson().fromJson(reader, MinecraftVersionInfo.class);
        }
    }
}
