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

package net.technicpack.minecraftcore.launch;

import net.technicpack.autoupdate.IBuildNumber;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.launch.GameProcess;
import net.technicpack.launchercore.launch.ProcessExitListener;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.RunData;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.minecraftcore.mojang.version.MojangVersion;
import net.technicpack.minecraftcore.mojang.version.io.CompleteVersion;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinecraftLauncher {

    private static final String[] BAD_ENV_VARS = new String[] {
            "JAVA_ARGS", "CLASSPATH", "CONFIGPATH", "JAVA_HOME", "JRE_HOME",
            "_JAVA_OPTIONS", "JAVA_OPTIONS", "JAVA_TOOL_OPTIONS"
    };

    private final LauncherDirectories directories;
    private final IPlatformApi platformApi;
    private final UserModel<MojangUser> userModel;
    private final JavaVersionRepository javaVersions;
    private final IBuildNumber buildNumber;

    public MinecraftLauncher(final IPlatformApi platformApi, final LauncherDirectories directories, final UserModel userModel, final JavaVersionRepository javaVersions, IBuildNumber buildNumber) {
        this.directories = directories;
        this.platformApi = platformApi;
        this.userModel = userModel;
        this.javaVersions = javaVersions;
        this.buildNumber = buildNumber;
    }

    public JavaVersionRepository getJavaVersions() {
        return javaVersions;
    }

    public GameProcess launch(ModpackModel pack, int memory, LaunchOptions options, CompleteVersion version) throws IOException {
        return launch(pack, memory, options, null, version);
    }

    public GameProcess launch(ModpackModel pack, long memory, LaunchOptions options, ProcessExitListener exitListener, MojangVersion version) throws IOException {
        List<String> commands = buildCommands(pack, memory, version, options);
        StringBuilder full = new StringBuilder();
        boolean first = true;

        for (String part : commands) {
            if (!first) full.append(" ");
            full.append(part);
            first = false;
        }
        Utils.getLogger().info("Running " + full.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands).directory(pack.getInstalledDirectory()).redirectErrorStream(true);
        Map<String, String> envVars = processBuilder.environment();
        for (String badVar : BAD_ENV_VARS) envVars.remove(badVar);
        Process process = processBuilder.start();
        GameProcess mcProcess = new GameProcess(commands, process);
        if (exitListener != null) mcProcess.setExitListener(exitListener);

        platformApi.incrementPackRuns(pack.getName());
        if (!Utils.sendTracking("runModpack", pack.getName(), pack.getInstalledVersion().getVersion(), options.getOptions().getClientId())) {
            Utils.getLogger().info("Failed to record event");
        }

        return mcProcess;
    }

    private List<String> buildCommands(ModpackModel pack, long memory, MojangVersion version, LaunchOptions options) {
        LaunchCommandCollector commands = new LaunchCommandCollector();
        commands.addRaw(javaVersions.getSelectedPath());

        OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
        String nativesDir = new File(pack.getBinDir(), "natives").getAbsolutePath();
        String cpString = buildClassPath(pack, version);

        // build arg parameter map
        Map<String, String> params = new HashMap<String, String>();
        MojangUser mojangUser = userModel.getCurrentUser();
        File gameDirectory = pack.getInstalledDirectory();
        ILaunchOptions launchOpts = options.getOptions();

        params.put("auth_username", mojangUser.getUsername());
        params.put("auth_session", mojangUser.getSessionId());
        params.put("auth_access_token", mojangUser.getAccessToken());

        params.put("auth_player_name", mojangUser.getDisplayName());
        params.put("auth_uuid", mojangUser.getProfile().getId());

        params.put("profile_name", mojangUser.getDisplayName());
        params.put("version_name", version.getId());
        params.put("version_type", version.getType().getName());

        params.put("game_directory", gameDirectory.getAbsolutePath());
        params.put("natives_directory", nativesDir);
        params.put("classpath", cpString);

        params.put("resolution_width", Integer.toString(launchOpts.getCustomWidth()));
        params.put("resolution_height", Integer.toString(launchOpts.getCustomHeight()));

        StringSubstitutor paramDereferencer = new StringSubstitutor(params);

        String targetAssets = directories.getAssetsDirectory().getAbsolutePath();

        String assetsKey = version.getAssetsKey();

        if (assetsKey == null || assetsKey.isEmpty()) {
            assetsKey = "legacy";
        }

        if (version.getAreAssetsVirtual()) {
            targetAssets += File.separator + "virtual" + File.separator + assetsKey;
        }

        params.put("game_assets", targetAssets);
        params.put("assets_root", targetAssets);
        params.put("assets_index_name", assetsKey);
        params.put("user_type", mojangUser.getProfile().isLegacy() ? "legacy" : "mojang");
        params.put("user_properties", mojangUser.getUserPropertiesAsJson());

        params.put("launcher_name", "technic");
        params.put("launcher_version", "4." + buildNumber.getBuildNumber());

        // build jvm args
        String launchJavaVersion = javaVersions.getSelectedVersion().getVersionNumber();
        ArgumentList jvmArgs = version.getJavaArguments();

        if (jvmArgs != null) {
            for (String arg : jvmArgs.resolve(options.getOptions(), paramDereferencer)) {
                commands.add(arg);
            }
        }

        int permSize = 128;
        if (memory >= (1024 * 6)) {
            permSize = 512;
        } else if (memory >= 2048) {
            permSize = 256;
        }

        // So in 1.8 permgen autoscales- only problem, it doesn't do it based on RAM allocation like we do, instead
        // It has a SEPARATE heap for permgen that is by default unbounded by anything.  Result: instead of 2GB
        // with 256m set aside for permgen, you have a whole 2GB PLUS however much permgen uses.
        commands.addRaw("-Xms" + memory + "m");
        commands.addRaw("-Xmx" + memory + "m");

        if (!RunData.isJavaVersionAtLeast(launchJavaVersion, "1.8"))
            commands.add("-XX:MaxPermSize=" + permSize + "m");

        if (memory >= 4096) {
            if (RunData.isJavaVersionAtLeast(launchJavaVersion, "1.7")) {
                commands.add("-XX:+UseG1GC");
                commands.add("-XX:MaxGCPauseMillis=4");
            } else {
                commands.add("-XX:+UseConcMarkSweepGC");
            }
        }

        commands.addUnique("-Djava.library.path=" + nativesDir);
        // Tell forge 1.5 to download from our mirror instead
        commands.addUnique("-Dfml.core.libraries.mirror=http://mirror.technicpack.net/Technic/lib/fml/%s");
        commands.addUnique("-Dminecraft.applet.TargetDirectory=" + pack.getInstalledDirectory().getAbsolutePath());
        commands.addUnique("-Duser.language=en");

        if (!options.getOptions().shouldUseStencilBuffer())
            commands.add("-Dforge.forceNoStencil=true");

        if (operatingSystem.equals(OperatingSystem.OSX)) {
            commands.add("-Xdock:icon=" + options.getIconPath());
            commands.add("-Xdock:name=" + pack.getDisplayName());
        } else if (operatingSystem.equals(OperatingSystem.WINDOWS)) {
            // I have no idea if this helps technic or not.
            commands.addUnique("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
        }

        // build game args
        commands.addUnique("-cp", cpString);
        commands.addRaw(version.getMainClass());
        for (String arg : version.getMinecraftArguments().resolve(launchOpts, paramDereferencer)) {
            commands.add(arg);
        }
        options.appendToCommands(commands);

        //TODO: Add all the other less important commands
        return commands.collect();
    }

    private String buildClassPath(ModpackModel pack, MojangVersion version) {
        StringBuilder result = new StringBuilder();
        String separator = System.getProperty("path.separator");

        // Add the modpack.jar to the classpath, if it exists and minecraftforge is not a library already
        File modpack = new File(pack.getBinDir(), "modpack.jar");
        if (modpack.exists()) {
            if (result.length() > 1) {
                result.append(separator);
            }
            result.append(modpack.getAbsolutePath());
        }

        // if MC < 1.6, we inject LegacyWrapper
        // HACK
        boolean isLegacy = MojangUtils.isLegacyVersion(version.getId());

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

            if (isLegacy && library.getName().startsWith("net.minecraft:launchwrapper")) {
                continue;
            }

            File file = new File(directories.getCacheDirectory(), library.getArtifactPath().replace("${arch}", System.getProperty("sun.arch.data.model")));
            if (!file.isFile() || !file.exists()) {
                throw new RuntimeException("Library " + library.getName() + " not found.");
            }

            if (result.length() > 1) {
                result.append(separator);
            }
            result.append(file.getAbsolutePath());
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
