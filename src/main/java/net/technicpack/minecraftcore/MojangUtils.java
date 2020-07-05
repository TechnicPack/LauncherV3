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

package net.technicpack.minecraftcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.technicpack.minecraftcore.mojang.auth.io.UserProperties;
import net.technicpack.minecraftcore.mojang.auth.io.UserPropertiesAdapter;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.CompleteVersion;
import net.technicpack.minecraftcore.mojang.version.io.CompleteVersionV21;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.Rule;
import net.technicpack.minecraftcore.mojang.version.io.RuleAdapter;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentListAdapter;
import net.technicpack.utilslib.DateTypeAdapter;
import net.technicpack.utilslib.LowerCaseEnumTypeAdapterFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojangUtils {
    /** @deprecated Uses old S3 bucket */
    public static final String baseURL = "https://s3.amazonaws.com/Minecraft.Download/";
    public static final String assetsIndexes = baseURL + "indexes/";
    public static final String versions = baseURL + "versions/";

    public static final String assets = "https://resources.download.minecraft.net/";

    /** @deprecated Uses old S3 bucket */
    public static String getOldVersionDownload(String version) {
        return versions + version + "/" + version + ".jar";
    }

    /** @deprecated Uses old S3 bucket */
    public static String getAssetsIndex(String assetsKey) {
        return assetsIndexes + assetsKey + ".json";
    }

    public static String getResourceUrl(String hash) {
        return assets + hash.substring(0, 2) + "/" + hash;
    }

    private static final Gson gson;
    private static final Gson uglyGson;
    private static final NavigableMap<Integer, Class<? extends MojangVersion>> versionJsonVersions;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(UserProperties.class, new UserPropertiesAdapter());
        builder.registerTypeAdapter(ArgumentList.class, new ArgumentListAdapter());
        builder.registerTypeAdapter(Rule.class, new RuleAdapter());
        builder.enableComplexMapKeySerialization();
        uglyGson = builder.create();

        builder.setPrettyPrinting();
        gson = builder.create();

        versionJsonVersions = new TreeMap<Integer, Class<? extends MojangVersion>>();
        versionJsonVersions.put(0, CompleteVersion.class);
        versionJsonVersions.put(21, CompleteVersionV21.class);
    }

    public static Gson getGson() {
        return gson;
    }

    public static Gson getUglyGson() {
        return uglyGson;
    }

    public static void copyMinecraftJar(File minecraft, File output) throws IOException {
        try (ZipFile jarFile = new ZipFile(minecraft)) {
            ZipArchiveOutputStream zos = new ZipArchiveOutputStream(new FileOutputStream(output));
            Enumeration<ZipArchiveEntry> entries = jarFile.getEntries();
            byte[] copyBuffer = new byte[32768];
            int bytesRead;

            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.getName().contains("META-INF")) {
                    continue;
                }

                // Write entry
                zos.putArchiveEntry(entry);
                // Write entry data
                InputStream is = jarFile.getInputStream(entry);
                while ((bytesRead = is.read(copyBuffer)) != -1) {
                    zos.write(copyBuffer, 0, bytesRead);
                }
                is.close();
                zos.closeArchiveEntry();
            }
            zos.close();
        }
    }

    public static MojangVersion parseVersionJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Class<? extends MojangVersion> versionJsonType;
        if (root.has("minimumLauncherVersion")) {
            int minLauncherVersion = root.get("minimumLauncherVersion").getAsInt();
            Map.Entry<Integer, Class<? extends MojangVersion>> entry = versionJsonVersions.floorEntry(minLauncherVersion);
            if (entry == null) {
                throw new IllegalArgumentException("Unsupported minimumLauncherVersion: " + minLauncherVersion);
            }
            versionJsonType = entry.getValue();
        } else { // fallback: check if "arguments" key exists since only 1.13+ should have it
            versionJsonType = root.has("arguments") ? CompleteVersionV21.class : CompleteVersion.class;
        }
        return getGson().fromJson(root, versionJsonType);
    }

    public static boolean isLegacyVersion(String version) {
        final String[] versionParts = version.split("[.-]", 3);

        return Integer.parseInt(versionParts[0]) == 1 && Integer.parseInt(versionParts[1]) < 6;
    }

    public static boolean hasModernForge(MojangVersion version) {
        boolean foundForge = false;
        for (Library library : version.getLibrariesForOS()) {
            if (library.isForge()) {
                foundForge = true;
                break;
            }
        }

        if (!foundForge) {
            return false;
        }

        Pattern p = Pattern.compile("^(?<mc>[0-9.]+)-forge-(?<forge>[0-9.]+)$");
        Matcher m = p.matcher(version.getId());

        if (!m.matches()) {
            return false;
        }

        final String mcVersionString = m.group("mc");

        final ComparableVersion mcVersion = new ComparableVersion(mcVersionString);
        final ComparableVersion forgeVersion = new ComparableVersion(m.group("forge"));

        // The new Forge installer exists in:
        // Forge for MC 1.13+
        // Forge for MC 1.12.2, after the version 14.23.5.2847

        if (mcVersion.compareTo(new ComparableVersion("1.13")) >= 0)
            return true;

        if (mcVersionString.equals("1.12.2") && forgeVersion.compareTo(new ComparableVersion("14.23.5.2847")) > 0)
            return true;

        return false;
    }
}
