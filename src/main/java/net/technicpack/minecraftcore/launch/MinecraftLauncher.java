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

import io.sentry.Sentry;
import net.technicpack.autoupdate.IBuildNumber;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.InstallException;
import net.technicpack.launchercore.launch.GameProcess;
import net.technicpack.launchercore.launch.ProcessExitListener;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.modpacks.RunData;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.ProcessUtils;
import net.technicpack.utilslib.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;

public class MinecraftLauncher {

    private final LauncherFileSystem fileSystem;
    private final IPlatformApi platformApi;
    private final UserModel userModel;
    private final JavaVersionRepository javaVersions;
    private final IBuildNumber buildNumber;

    public MinecraftLauncher(final IPlatformApi platformApi, final LauncherFileSystem fileSystem,
            final UserModel userModel, final JavaVersionRepository javaVersions, IBuildNumber buildNumber) {
        this.fileSystem = fileSystem;
        this.platformApi = platformApi;
        this.userModel = userModel;
        this.javaVersions = javaVersions;
        this.buildNumber = buildNumber;
    }

    public JavaVersionRepository getJavaVersions() {
        return javaVersions;
    }

    public GameProcess launch(ModpackModel pack, long memory, LaunchOptions options, ProcessExitListener exitListener,
            IMinecraftVersionInfo version) throws IOException, InstallException {
        List<String> commands = buildCommands(pack, memory, version, options);

        // This will never be null
        final String userAccessToken = userModel.getCurrentUser().getAccessToken();

        String commandStr = String.join(" ", commands);

        // Censor the user access token from the logs
        // A value of "0" means offline mode, and shouldn't be censored
        if (!userAccessToken.equals("0")) {
            commandStr = commandStr.replace(userAccessToken, "USER_ACCESS_TOKEN");
        }

        Utils.getLogger().info("Running " + commandStr);

        ProcessBuilder processBuilder = ProcessUtils.createProcessBuilder(commands)
                .directory(pack.getInstalledDirectory())
                .redirectErrorStream(true);

        Process process = processBuilder.start();
        GameProcess mcProcess = new GameProcess(process, userAccessToken);
        if (exitListener != null) mcProcess.setExitListener(exitListener);

        platformApi.incrementPackRuns(pack.getName());
        if (!Utils.sendTracking("runModpack", pack.getName(), pack.getInstalledVersion().getVersion(), options.getOptions().getClientId())) {
            Utils.getLogger().info("Failed to record event");
        }

        return mcProcess;
    }

    private List<String> buildCommands(ModpackModel pack, long memory, IMinecraftVersionInfo version, LaunchOptions options) throws InstallException {
        LaunchCommandCollector commands = new LaunchCommandCollector();

        // Wrapper command (optirun, etc)
        String wrapperCommand = options.getOptions().getWrapperCommand();
        if (StringUtils.isNotEmpty(wrapperCommand)) {
            commands.addRaw(wrapperCommand);
        }

        IJavaRuntime javaRuntime = version.getJavaRuntime();

        commands.addRaw(javaRuntime.getExecutableFile().toString());

        OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
        String nativesDir = new File(pack.getBinDir(), "natives").getAbsolutePath();
        ILaunchOptions launchOpts = options.getOptions();
        String cpString = buildClassPath(pack, version, launchOpts);

        Utils.getLogger().log(Level.FINE, String.format("Classpath:%n%n%s", cpString.replace(File.pathSeparatorChar, '\n')));

        // build arg parameter map
        StringSubstitutor paramDereferencer = createParamDereferencer(pack, version, nativesDir, cpString, launchOpts);

        // Prepend custom JVM arguments
        String customJvmArgs = options.getOptions().getJavaArgs();
        if (StringUtils.isNotEmpty(customJvmArgs)) {
            customJvmArgs = customJvmArgs.replaceAll("[\\r\\n]", " ");
            for (String customJvmArg : customJvmArgs.split(" +")) {
                commands.addRaw(customJvmArg);
            }
        }

        // build jvm args
        String launchJavaVersion = javaRuntime.getVersion();

        // Ignore JVM args for Forge 1.13+, ForgeWrapper handles those
        // FIXME: HACK: This likely breaks some things as it will also skip vanilla JVM args
        if (!MojangUtils.hasModernMinecraftForge(version) && !MojangUtils.hasNeoForge(version)) {
            ArgumentList jvmArgs = version.getJavaArguments();

            if (jvmArgs != null) {
                for (String arg : jvmArgs.resolve(options.getOptions(), javaRuntime, paramDereferencer)) {
                    commands.add(arg);
                }
            }
        }

        commands.addRaw("-Xms" + memory + "m");
        commands.addRaw("-Xmx" + memory + "m");

        if (!RunData.isJavaVersionAtLeast(launchJavaVersion, "1.8")) {
            int permSize = 128;
            if (memory >= (1024 * 6)) {
                permSize = 512;
            } else if (memory >= 2048) {
                permSize = 256;
            }

            commands.add("-XX:MaxPermSize=" + permSize + "m");
        }

        commands.addUnique("-Djava.library.path=" + nativesDir);

        // This is required because we strip META-INF from the minecraft.jar
        commands.addUnique("-Dfml.ignoreInvalidMinecraftCertificates=true");
        commands.addUnique("-Dfml.ignorePatchDiscrepancies=true");

        // This is for ForgeWrapper >= 1.4.2
        if (MojangUtils.requiresForgeWrapper(version)) {
            commands.addUnique("-Dforgewrapper.librariesDir=" + fileSystem.getCacheDirectory().getAbsolutePath());

            // The Forge installer jar is really the modpack.jar
            File modpackJar = new File(pack.getBinDir(), "modpack.jar");
            commands.addUnique("-Dforgewrapper.installer=" + modpackJar.getAbsolutePath());

            // We feed ForgeWrapper the unmodified Minecraft jar here
            String mcVersion = MojangUtils.getMinecraftVersion(version);
            File minecraftJar = new File(fileSystem.getCacheDirectory(), "minecraft_" + mcVersion + ".jar");
            commands.addUnique("-Dforgewrapper.minecraft=" + minecraftJar.getAbsolutePath());
        }

        commands.addUnique("-Dminecraft.applet.TargetDirectory=" + pack.getInstalledDirectory().getAbsolutePath());
        commands.addUnique("-Duser.language=en");

        if (!options.getOptions().shouldUseStencilBuffer())
            commands.add("-Dforge.forceNoStencil=true");

        if (operatingSystem.equals(OperatingSystem.OSX)) {
            commands.add("-Xdock:icon=" + options.getIconPath());
            commands.add("-Xdock:name=" + pack.getDisplayName());

            // Add -XstartOnFirstThread for Mac on LWJGL 3
            boolean hasLwjgl3 = version.getLibrariesForCurrentOS(launchOpts, javaRuntime).stream().anyMatch(library -> library.getName().startsWith("org.lwjgl:lwjgl:"));
            if (hasLwjgl3) {
                commands.addUnique("-XstartOnFirstThread");
            }
        } else if (operatingSystem.equals(OperatingSystem.WINDOWS)) {
            commands.addUnique("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
        }

        // build game args
        commands.addUnique("-cp", cpString);
        commands.addRaw(version.getMainClass());
        List<String> mcArgs = version.getMinecraftArguments().resolve(launchOpts, javaRuntime, paramDereferencer);

        // We manually iterate over the arguments so we can add them in "--name value" pairs, and exclude duplicates
        // this way. For example: --username Foobar
        ListIterator<String> mcArgsIterator = mcArgs.listIterator();
        String current, next;
        while (mcArgsIterator.hasNext()) {
            current = mcArgsIterator.next();

            if (mcArgsIterator.hasNext()) {
                // We don't consume it so we can later add it to the commands if it turns out to not be a value
                // This allows us to keep the pair detection working correctly, and if we do end up using the value,
                // we just consume it from the iterator later on
                next = mcArgs.get(mcArgsIterator.nextIndex());
            } else {
                next = null;
            }

            // For 1.2.1 through 1.5.2, not all of it is a named argument (--argument):
            // ${auth_player_name} ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}
            if (current.startsWith("--")) {
                // This is an --argument, now we check if it has a value or not
                if (next != null && !next.startsWith("--")) {
                    // This --argument has a value, so we add that --argument value pair

                    // Special case for --tweakClass to allow multiple values of it. This allows Forge and
                    // LiteLoader to coexist
                    if (current.equals("--tweakClass")) {
                        commands.add(current, next);
                    } else {
                        commands.addUnique(current, next);
                    }

                    // Consume the next element in the iterator
                    mcArgsIterator.next();
                } else {
                    // This --argument doesn't have a value, so we just add the argument itself as a unique parameter
                    commands.addUnique(current);
                }
            } else {
                // Doesn't start with --, so this is not a named argument, and since we have no way of knowing if this
                // is a duplicate or not, we just add it as a regular argument (no duplicate detection)
                commands.add(current);
            }
        }

        options.appendToCommands(commands);

        //TODO: Add all the other less important commands
        return commands.collect();
    }

    private StringSubstitutor createParamDereferencer(ModpackModel pack, IMinecraftVersionInfo version, String nativesDir, String cpString, ILaunchOptions launchOpts) {
        Map<String, String> params = new HashMap<>();
        IUserType user = userModel.getCurrentUser();
        File gameDirectory = pack.getInstalledDirectory();

        params.put("auth_username", user.getUsername());
        params.put("auth_session", user.getSessionId());
        params.put("auth_access_token", user.getAccessToken());

        params.put("auth_player_name", user.getDisplayName());
        params.put("auth_uuid", user.getId());

        params.put("profile_name", user.getDisplayName());
        params.put("version_name", version.getId());
        params.put("version_type", version.getType().getName());

        params.put("game_directory", gameDirectory.getAbsolutePath());
        params.put("natives_directory", nativesDir);
        params.put("classpath", cpString);

        params.put("resolution_width", Integer.toString(launchOpts.getCustomWidth()));
        params.put("resolution_height", Integer.toString(launchOpts.getCustomHeight()));

        String targetAssets = fileSystem.getAssetsDirectory().getAbsolutePath();

        String assetsKey = version.getAssetsKey();

        if (assetsKey == null || assetsKey.isEmpty()) {
            assetsKey = "legacy";
        }

        if (version.getAreAssetsVirtual()) {
            targetAssets += File.separator + "virtual" + File.separator + assetsKey;
        } else if (version.getAssetsMapToResources()) {
            targetAssets = pack.getResourcesDir().getAbsolutePath();
        }

        params.put("game_assets", targetAssets);
        params.put("assets_root", targetAssets);
        params.put("assets_index_name", assetsKey);
        params.put("user_type", user.getMCUserType());
        params.put("user_properties", user.getUserProperties());

        params.put("launcher_name", "technic");
        params.put("launcher_version", "4." + buildNumber.getBuildNumber());

        return new StringSubstitutor(params);
    }

    private String buildClassPath(ModpackModel pack, IMinecraftVersionInfo version, ILaunchOptions launchOptions) throws InstallException {
        StringBuilder sb = new StringBuilder(10_000);
        final char separator = File.pathSeparatorChar;

        IJavaRuntime runtime = version.getJavaRuntime();

        Sentry.addBreadcrumb(String.format("Runtime is %s", runtime));

        final String bitness = runtime.getBitness();

        // Add all the libraries to the classpath
        for (Library library : version.getLibrariesForCurrentOS(launchOptions, runtime)) {
            if (library.getNatives() != null) {
                continue;
            }

            File file = new File(fileSystem.getCacheDirectory(), library.getArtifactPath().replace("${arch}", bitness));
            if (!file.isFile() || !file.exists()) {
                throw new InstallException("Library " + library.getName() + " not found.");
            }

            sb.append(file.getAbsolutePath()).append(separator);
        }

        // Add the modpack.jar to the classpath if it exists and the modpack isn't
        // running modern Minecraft Forge or NeoForge
        final boolean hasModernMinecraftForge = MojangUtils.hasModernMinecraftForge(version);
        final boolean hasNeoForge = MojangUtils.hasNeoForge(version);

        if (!hasModernMinecraftForge && !hasNeoForge) {
            File modpack = new File(pack.getBinDir(), "modpack.jar");
            if (modpack.exists()) {
                sb.append(modpack.getAbsolutePath()).append(separator);
            }
        }

        // Add the minecraft jar to the classpath
        File minecraft;
        if (hasModernMinecraftForge || hasNeoForge) {
            minecraft = new File(fileSystem.getCacheDirectory(), "minecraft_" + version.getParentVersion() + ".jar");
        } else {
            minecraft = new File(pack.getBinDir(), "minecraft.jar");
        }
        if (!minecraft.exists()) {
            throw new RuntimeException("Minecraft not installed for this pack: " + pack);
        }
        sb.append(minecraft.getAbsolutePath());

        return sb.toString();
    }

}
