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

package net.technicpack.minecraftcore.install.tasks;

import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.ListenerTask;
import net.technicpack.launchercore.install.verifiers.IFileVerifier;
import net.technicpack.launchercore.install.verifiers.SHA1FileVerifier;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.GameDownloads;
import net.technicpack.utilslib.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InstallMinecraftIfNecessaryTask extends ListenerTask<IMinecraftVersionInfo> {

	private ModpackModel pack;
	private String minecraftVersion;
	private Path cacheDirectory;
	private boolean forceRegeneration;

	public InstallMinecraftIfNecessaryTask(ModpackModel pack, String minecraftVersion, Path cacheDirectory, boolean forceRegeneration) {
		this.pack = pack;
		this.minecraftVersion = minecraftVersion;
		this.cacheDirectory = cacheDirectory;
		this.forceRegeneration = forceRegeneration;
	}

    @Override
	public String getTaskDescription() {
		return "Installing Minecraft";
	}

	@Override
	public void runTask(InstallTasksQueue<IMinecraftVersionInfo> queue) throws IOException, InterruptedException {
		super.runTask(queue);

		IMinecraftVersionInfo version = queue.getMetadata();

		GameDownloads dls = version.getDownloads();

		if (dls == null) {
			throw new RuntimeException("Using legacy Minecraft download! Version id = " + version.getId() + "; parent = " + version.getParentVersion());
		}

		String url = dls.forClient().getUrl();
		IFileVerifier verifier = new SHA1FileVerifier(dls.forClient().getSha1());

        Path originalJar = cacheDirectory.resolve(String.format("minecraft_%s.jar", minecraftVersion));

		boolean regenerate = forceRegeneration;

		if (!Files.isRegularFile(originalJar) || !verifier.isFileValid(originalJar)) {
			String output = this.pack.getCacheDir() + File.separator + "minecraft.jar";
			Utils.downloadFile(url, originalJar.getFileName().toString(), output, originalJar.toFile(), verifier, this);
			regenerate = true;
		}

		File targetJar = new File(this.pack.getBinDir(), "minecraft.jar");

		if (!targetJar.exists() || regenerate) {
			copyMinecraftJar(originalJar, targetJar.toPath(), this);
		}
	}


    private static void copyMinecraftJar(Path jar, Path output, DownloadListener listener) throws IOException {
        String[] security = {"MOJANG_C.DSA", "MOJANG_C.SF", "CODESIGN.RSA", "CODESIGN.SF"};
        Pattern securityPattern = Pattern.compile(Arrays.stream(security).map(Pattern::quote).collect(Collectors.joining("|")));
        listener.stateChanged("Processing Minecraft jar", 0);
        try (JarFile jarFile = new JarFile(jar.toFile());
             OutputStream out = Files.newOutputStream(output);
             JarOutputStream jos = new JarOutputStream(out)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            final int totalEntries = jarFile.size();
            int x = 0;
            JarEntry entry;
            byte[] buffer = new byte[8192];
            int bytesRead;

            while (entries.hasMoreElements()) {
                listener.stateChanged("Processing Minecraft jar", (float) ++x / totalEntries * 100);
                entry = entries.nextElement();
                if (securityPattern.matcher(entry.getName()).find()) {
                    continue;
                }
                try (InputStream is = jarFile.getInputStream(entry)) {
                    // create a new entry to avoid ZipException: invalid entry compressed size
                    jos.putNextEntry(new JarEntry(entry.getName()));

                    while ((bytesRead = is.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                }
                jos.flush();
                jos.closeEntry();
            }
        }
    }
}
