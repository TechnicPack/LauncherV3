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

import com.google.api.client.http.*;
import com.google.api.client.http.apache.v5.Apache5HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.technicpack.launcher.io.IUserTypeInstanceCreator;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.mojang.java.JavaRuntimesIndex;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.CompleteVersion;
import net.technicpack.minecraftcore.mojang.version.io.CompleteVersionV21;
import net.technicpack.minecraftcore.mojang.version.io.Rule;
import net.technicpack.minecraftcore.mojang.version.io.RuleAdapter;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentListAdapter;
import net.technicpack.utilslib.DateTypeAdapter;
import net.technicpack.utilslib.LowerCaseEnumTypeAdapterFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MojangUtils {
    private static final HttpTransport HTTP_TRANSPORT = new Apache5HttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();
    private static final HttpRequestFactory REQUEST_FACTORY;

    public static final String assets = "https://resources.download.minecraft.net/";

    public static final String RUNTIMES_URL = "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
    private static JavaRuntimesIndex javaRuntimesIndex;

    public static String getResourceUrl(String hash) {
        return assets + hash.substring(0, 2) + "/" + hash;
    }

    private static final Gson gson;
    private static final NavigableMap<Integer, Class<? extends MojangVersion>> versionJsonVersions;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(ArgumentList.class, new ArgumentListAdapter());
        builder.registerTypeAdapter(Rule.class, new RuleAdapter());
        builder.registerTypeAdapter(IUserType.class, new IUserTypeInstanceCreator());
        builder.enableComplexMapKeySerialization();
        builder.setPrettyPrinting();
        gson = builder.create();

        versionJsonVersions = new TreeMap<>();
        versionJsonVersions.put(0, CompleteVersion.class);
        versionJsonVersions.put(21, CompleteVersionV21.class);

        REQUEST_FACTORY = HTTP_TRANSPORT.createRequestFactory(
                request -> request.setParser(new JsonObjectParser(JSON_FACTORY))
        );
    }

    public static Gson getGson() {
        return gson;
    }

    public static void copyMinecraftJar(File minecraft, File output) throws IOException {
        String[] security = { "MOJANG_C.DSA",
                "MOJANG_C.SF",
                "CODESIGN.RSA",
                "CODESIGN.SF" };
        Pattern securityPattern = Pattern.compile(Arrays.stream(security).map(Pattern::quote).collect(Collectors.joining("|")));
        try (JarFile jarFile = new JarFile(minecraft);
             OutputStream out = Files.newOutputStream(output.toPath());
             JarOutputStream jos = new JarOutputStream(out)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (securityPattern.matcher(entry.getName()).lookingAt()) {
                    continue;
                }
                try (InputStream is = jarFile.getInputStream(entry)) {
                    //create a new entry to avoid ZipException: invalid entry compressed size
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                }
                jos.flush();
                jos.closeEntry();
            }
        }
    }

    public static MojangVersion parseVersionJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // Safeguard in the event the version.json we get is actually the one inside a vanilla Minecraft jar
        // (which isn't valid)
        if (root.has("world_version") && root.has("protocol_version")) {
            throw new IllegalArgumentException("Invalid version file, this looks like a Minecraft client jar. Are you sure you didn't place a Minecraft jar as the modpack.jar?");
        }

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

    public static boolean hasModernMinecraftForge(MojangVersion version) {
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

    public static boolean hasNeoForge(MojangVersion version) {
        Pattern p = Pattern.compile("^neoforge-(?<forge>[0-9.]+)");
        Matcher m = p.matcher(version.getId());

        return m.lookingAt();
    }

    public static String getMinecraftVersion(MojangVersion version) {
        final String id = version.getId();

        // Simplification in case it doesn't have Forge at all
        if (!id.contains("-"))
            return id;

        // Neoforge doesn't have the mc version in the id but it's always the parent
        if (hasNeoForge(version)) {
            return version.getParentVersion();
        }

        // For Forge, this will be "mc-forge"
        final String[] idParts = id.split("-");
        return idParts[0];
    }

    public static boolean requiresForgeWrapper(MojangVersion version) {
        if (hasNeoForge(version)) {
            return true;
        }
        if (!hasModernMinecraftForge(version)) {
            return false;
        }

        final String mcVersion = getMinecraftVersion(version);

        return !mcVersion.equals("1.12.2");
    }

    public static JavaRuntimesIndex getJavaRuntimesIndex() {
        if (javaRuntimesIndex != null)
            return javaRuntimesIndex;

        try {
            HttpRequest request = REQUEST_FACTORY.buildGetRequest(new GenericUrl(RUNTIMES_URL));
            HttpResponse httpResponse = request.execute();
            javaRuntimesIndex = gson.fromJson(httpResponse.parseAsString(), JavaRuntimesIndex.class);
            return javaRuntimesIndex;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
