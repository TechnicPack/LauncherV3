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
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.MojangVersionBuilder;
import net.technicpack.minecraftcore.mojang.version.io.CompleteVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class FileVersionBuilder implements MojangVersionBuilder {

    private File version;
    private MojangVersionRetriever retriever;
    private List<MojangVersionRetriever> fallbackRetrievers;

    public FileVersionBuilder(File version, MojangVersionRetriever retriever, List<MojangVersionRetriever> fallbackRetrievers) {
        this.version = version;
        this.retriever = retriever;
        this.fallbackRetrievers = fallbackRetrievers;
    }

    @Override
    public MojangVersion buildVersionFromKey(String key) throws InterruptedException, IOException {
        File target = version;

        if (key != null) {
            if (version.isDirectory()) {
                String targetFile = key + ".json";
                target = new File(version, targetFile);
            }

            if (retriever != null)
                retriever.retrieveVersion(target, key);

            if (fallbackRetrievers != null) {
                for (MojangVersionRetriever fallbackRetriever : fallbackRetrievers) {
                    if (target.exists())
                        break;

                    fallbackRetriever.retrieveVersion(target, key);
                }
            }
        }

        if (!target.exists())
            return null;

        String json = FileUtils.readFileToString(target, Charset.forName("UTF-8"));
        return MojangUtils.getGson().fromJson(json, CompleteVersion.class);
    }
}
