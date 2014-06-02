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

package net.technicpack.launchercore.launch;

import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.install.user.User;
import net.technicpack.launchercore.minecraft.CompleteVersion;
import net.technicpack.launchercore.minecraft.Library;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.restful.PlatformConstants;
import net.technicpack.launchercore.util.OperatingSystem;
import net.technicpack.launchercore.util.Utils;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MinecraftLauncher {
    private final int memory;
    private final InstalledPack pack;
    private final CompleteVersion version;

    public MinecraftLauncher(int memory, InstalledPack pack, CompleteVersion version) {
        this.memory = memory;
        this.pack = pack;
        this.version = version;
    }

    public MinecraftProcess launch(User user, LaunchOptions options, MirrorStore mirrorStore) throws IOException {
        return launch(user, options, null, mirrorStore);
    }

    public MinecraftProcess launch(User user, LaunchOptions options, MinecraftExitListener exitListener, MirrorStore mirrorStore) throws IOException {
        List<String> commands = buildCommands(user, options);
        StringBuilder full = new StringBuilder();
        boolean first = true;

        for (String part : commands) {
            if (!first) full.append(" ");
            full.append(part);
            first = false;
        }
        System.out.println("Running " + full.toString());
        Utils.pingHttpURL(PlatformConstants.getRunCountUrl(pack.getName()), mirrorStore);
        if (!Utils.sendTracking("runModpack", pack.getName(), pack.getBuild())) {
            System.out.println("Failed to record event");
        }
        Process process = new ProcessBuilder(commands).directory(pack.getInstalledDirectory()).redirectErrorStream(true).start();
        MinecraftProcess mcProcess = new MinecraftProcess(commands, process);
        if (exitListener != null) mcProcess.setExitListener(exitListener);
        return mcProcess;
    }

    private List<String> buildCommands(User user, LaunchOptions options) {
        List<String> commands = new ArrayList<String>();
        commands.add(OperatingSystem.getJavaDir());

        OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();

        if (operatingSystem.equals(OperatingSystem.OSX)) {
            //TODO: -Xdock:icon=<icon path>
            commands.add("-Xdock:name=" + pack.getDisplayName());
        } else if (operatingSystem.equals(OperatingSystem.WINDOWS)) {
            // I have no idea if this helps technic or not.
            commands.add("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
        }
        commands.add("-Xmx" + memory + "m");
        int permSize = 128;
        if (memory >= 2048) {
            permSize = 256;
        }
        commands.add("-XX:MaxPermSize=" + permSize + "m");
        commands.add("-Djava.library.path=" + new File(pack.getBinDir(), "natives").getAbsolutePath());
        // Tell forge 1.5 to download from our mirror instead
        commands.add("-Dfml.core.libraries.mirror=http://mirror.technicpack.net/Technic/lib/fml/%s");
        commands.add("-Dminecraft.applet.TargetDirectory=" + pack.getInstalledDirectory().getAbsolutePath());

        String javaArguments = version.getJavaArguments();

        if (javaArguments != null && !javaArguments.isEmpty()) {
            commands.addAll(Arrays.asList(javaArguments.split(" ")));
        }

        commands.add("-cp");
        commands.add(buildClassPath());
        commands.add(version.getMainClass());
        commands.addAll(Arrays.asList(getMinecraftArguments(version, pack.getInstalledDirectory(), user)));
        options.appendToCommands(commands);

        //TODO: Add all the other less important commands
        return commands;
    }

    private String[] getMinecraftArguments(CompleteVersion version, File gameDirectory, User user) {
        Map<String, String> map = new HashMap<String, String>();
        StrSubstitutor substitutor = new StrSubstitutor(map);
        String[] split = version.getMinecraftArguments().split(" ");

        map.put("auth_username", user.getUsername());
        map.put("auth_session", user.getSessionId());
        map.put("auth_access_token", user.getAccessToken());

        map.put("auth_player_name", user.getDisplayName());
        map.put("auth_uuid", user.getProfile().getId());

        map.put("profile_name", user.getDisplayName());
        map.put("version_name", version.getId());

        map.put("game_directory", gameDirectory.getAbsolutePath());

        String targetAssets = Utils.getAssetsDirectory().getAbsolutePath();

        String assetsKey = this.version.getAssetsKey();

        if (assetsKey == null || assetsKey.isEmpty()) {
            assetsKey = "legacy";
        }

        if (this.version.getAreAssetsVirtual()) {
            targetAssets += File.separator + "virtual" + File.separator + assetsKey;
        }

        map.put("game_assets", targetAssets);
        map.put("assets_root", targetAssets);
        map.put("assets_index_name", assetsKey);
        map.put("user_type", user.getProfile().isLegacy() ? "legacy" : "mojang");
        map.put("user_properties", user.getUserPropertiesAsJson());

        for (int i = 0; i < split.length; i++) {
            split[i] = substitutor.replace(split[i]);
        }

        return split;
    }

    private String buildClassPath() {
        StringBuilder result = new StringBuilder();
        String separator = System.getProperty("path.separator");

        // Add all the libraries to the classpath.
        for (Library library : version.getLibrariesForOS()) {
            if (library.getNatives() != null) {
                continue;
            }

            // If minecraftforge is described in the libraries, skip it
            // HACK - Please let us get rid of this when we move to actually hosting forge,
            // or at least only do it if the users are sticking with modpack.jar
            if (library.getName().startsWith("net.minecraftforge:minecraftforge") ||
                    library.getName().startsWith("net.minecraftforge:forge")) {
                continue;
            }

            File file = new File(Utils.getCacheDirectory(), library.getArtifactPath().replace("${arch}", System.getProperty("sun.arch.data.model")));
            if (!file.isFile() || !file.exists()) {
                throw new RuntimeException("Library " + library.getName() + " not found.");
            }

            if (result.length() > 1) {
                result.append(separator);
            }
            result.append(file.getAbsolutePath());
        }

        // Add the modpack.jar to the classpath, if it exists and minecraftforge is not a library already
        File modpack = new File(pack.getBinDir(), "modpack.jar");
        if (modpack.exists()) {
            if (result.length() > 1) {
                result.append(separator);
            }
            result.append(modpack.getAbsolutePath());
        }

        // Add the minecraft jar to the classpath
        File minecraft = new File(pack.getBinDir(), "minecraft.jar");
        if (!minecraft.exists()) {
            throw new RuntimeException("Minecraft not installed for this pack: " + pack);
        }
        if (result.length() > 1) {
            result.append(separator);
        }
        result.append(minecraft.getAbsolutePath());

        return result.toString();
    }
}
