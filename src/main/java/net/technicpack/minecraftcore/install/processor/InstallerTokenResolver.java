package net.technicpack.minecraftcore.install.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import net.technicpack.utilslib.ZipUtils;

/** The installer's one-pass token grammar; substitutions are never interpreted a second time. */
public final class InstallerTokenResolver {
  private InstallerTokenResolver() {}

  public static String expand(String value, Map<String, String> data) throws IOException {
    StringBuilder result = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '\\') {
        if (++index == value.length()) throw new IOException("Trailing installer token escape");
        result.append(value.charAt(index));
      } else if (current == '\'') {
        index = collect(value, index + 1, '\'', result);
      } else if (current == '{') {
        StringBuilder key = new StringBuilder();
        index = collect(value, index + 1, '}', key);
        String replacement = data.get(key.toString());
        if (replacement == null) throw new IOException("Missing client installer data: " + key);
        result.append(replacement);
      } else {
        result.append(current);
      }
    }
    return result.toString();
  }

  private static int collect(String value, int start, char delimiter, StringBuilder result)
      throws IOException {
    for (int index = start; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '\\') {
        if (++index == value.length()) throw new IOException("Trailing installer token escape");
        result.append(value.charAt(index));
      } else if (current == delimiter) {
        return index;
      } else {
        result.append(current);
      }
    }
    throw new IOException("Unclosed installer " + (delimiter == '}' ? "token" : "literal"));
  }

  public static String resolveArgument(String value, Map<String, String> data, Path libraries)
      throws IOException {
    return isCoordinate(value) ? coordinatePath(value, libraries).toString() : expand(value, data);
  }

  private static boolean isCoordinate(String value) {
    return value.startsWith("[") && value.endsWith("]");
  }

  private static Path coordinatePath(String value, Path libraries) throws IOException {
    try {
      return MavenCoordinate.resolve(
          libraries, MavenCoordinate.parse(value.substring(1, value.length() - 1)).getPath());
    } catch (IllegalArgumentException invalid) {
      throw new IOException("Invalid installer coordinate: " + value, invalid);
    }
  }

  /** The caller owns workDirectory, creates it under cache, and removes it in finally. */
  public static Map<String, String> resolveData(
      ModernInstallerProfile profile,
      Path launcherRoot,
      Path installer,
      Path vanillaJar,
      Path workDirectory)
      throws IOException, InterruptedException {
    Path root = launcherRoot.toAbsolutePath().normalize();
    Path libraries = root.resolve("libraries");
    InstallerArtifactStore.checkedPath(root.resolve("cache"), workDirectory);
    Map<String, String> data = new LinkedHashMap<>();
    data.put("SIDE", "client");
    data.put("MINECRAFT_JAR", vanillaJar.toAbsolutePath().normalize().toString());
    data.put("MINECRAFT_VERSION", profile.getMinecraft());
    data.put("ROOT", root.toString());
    data.put("INSTALLER", installer.toAbsolutePath().normalize().toString());
    data.put("LIBRARY_DIR", libraries.toString());
    Set<Path> extracted = new HashSet<>();
    for (Map.Entry<String, ModernInstallerProfile.DataValue> entry : profile.getData().entrySet()) {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
      String value = entry.getValue().getClient();
      if (value == null || data.containsKey(entry.getKey())) continue;
      String resolved;
      if (isCoordinate(value)) {
        resolved = coordinatePath(value, libraries).toString();
      } else if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
        resolved = value.substring(1, value.length() - 1);
      } else {
        String member = value.startsWith("/") ? value.substring(1) : value;
        Path target;
        try {
          target = MavenCoordinate.resolve(workDirectory, member);
        } catch (IllegalArgumentException invalid) {
          throw new IOException("Unsafe installer resource " + value, invalid);
        }
        InstallerArtifactStore.checkedPath(workDirectory, target);
        if (extracted.add(target)) ZipUtils.extractEntryTo(installer, member, target);
        resolved = target.toString();
      }
      data.put(entry.getKey(), resolved);
    }
    return Collections.unmodifiableMap(data);
  }

  /**
   * Resolves only declared output targets; arbitrary command arguments never become delete targets.
   */
  public static Map<Path, String> resolveOutputs(
      ModernInstallerProfile.Processor processor,
      Map<String, String> data,
      Path launcherRoot,
      Path libraries)
      throws IOException {
    Path root = launcherRoot.toAbsolutePath().normalize();
    Map<Path, String> outputs = new LinkedHashMap<>();
    for (Map.Entry<String, String> output : processor.getOutputs().entrySet()) {
      String key = output.getKey();
      Path target;
      try {
        target = isCoordinate(key) ? coordinatePath(key, libraries) : Paths.get(expand(key, data));
      } catch (IllegalArgumentException invalid) {
        throw new IOException("Invalid processor output path: " + key, invalid);
      }
      target = InstallerArtifactStore.checkedPath(root, target);
      String sha1 = expand(output.getValue(), data);
      if (!sha1.matches("[0-9a-fA-F]{40}"))
        throw new IOException("Invalid processor output SHA-1: " + key);
      String previous = outputs.put(target, sha1.toLowerCase(Locale.ROOT));
      if (previous != null && !previous.equalsIgnoreCase(sha1)) {
        throw new IOException("Conflicting output SHA-1 values at " + target);
      }
    }
    return Collections.unmodifiableMap(outputs);
  }
}
