package net.technicpack.minecraftcore.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import net.technicpack.utilslib.IZipPathRemapper;
import net.technicpack.utilslib.Utils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Detects and remaps Prism Launcher instance zips for use with Technic Launcher.
 *
 * <p>Prism instance zips have this structure:
 *
 * <pre>
 *   InstanceName/
 *   ├── instance.cfg          (Prism metadata)
 *   ├── mmc-pack.json         (Prism metadata)
 *   ├── patches/              (version patches)
 *   ├── libraries/            (local libraries)
 *   └── .minecraft/           (game directory)
 *       ├── mods/
 *       ├── config/
 *       └── ...
 * </pre>
 *
 * <p>This remapper:
 *
 * <ul>
 *   <li>Strips the root directory prefix (e.g., "InstanceName/")
 *   <li>Flattens .minecraft/ contents to root (e.g., ".minecraft/mods/" → "mods/")
 *   <li>Skips Prism-specific metadata files (instance.cfg, mmc-pack.json)
 * </ul>
 */
public class PrismInstanceRemapper implements IZipPathRemapper {

  private static final Set<String> SKIP_FILES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("instance.cfg", "mmc-pack.json")));

  private final String rootPrefix;
  private final long maxMemAllocMb;

  private PrismInstanceRemapper(String rootPrefix, long maxMemAllocMb) {
    this.rootPrefix = rootPrefix;
    this.maxMemAllocMb = maxMemAllocMb;
  }

  /**
   * Scan a zip file for signs of a Prism instance. Returns a remapper if instance.cfg is found,
   * null otherwise.
   */
  public static PrismInstanceRemapper detect(File zipFile) {
    try (ZipFile zip = ZipFile.builder().setFile(zipFile).get()) {
      for (ZipArchiveEntry entry : Collections.list(zip.getEntries())) {
        String name = entry.getName();
        String rootPrefix = null;

        if (name.equals("instance.cfg")) {
          rootPrefix = "";
        } else if (name.endsWith("/instance.cfg") && name.indexOf('/') == name.lastIndexOf('/')) {
          rootPrefix = name.substring(0, name.lastIndexOf('/') + 1);
        }

        if (rootPrefix != null) {
          long memory = parseMaxMemAlloc(zip, entry);
          return new PrismInstanceRemapper(rootPrefix, memory);
        }
      }
    } catch (IOException e) {
      // Not a valid zip or can't read - not a Prism instance
    }
    return null;
  }

  private static long parseMaxMemAlloc(ZipFile zip, ZipArchiveEntry instanceCfg) {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(zip.getInputStream(instanceCfg), StandardCharsets.UTF_8))) {
      Properties props = new Properties();
      props.load(reader);
      String value = props.getProperty("MaxMemAlloc");
      if (value != null && !value.isEmpty()) {
        return Long.parseLong(value.trim());
      }
    } catch (IOException | NumberFormatException e) {
      Utils.getLogger().log(Level.WARNING, "Failed to parse MaxMemAlloc from instance.cfg", e);
    }
    return 0;
  }

  /**
   * Get the MaxMemAlloc value from instance.cfg, in MB. Returns 0 if not found or not parseable.
   */
  public long getMaxMemAllocMb() {
    return maxMemAllocMb;
  }

  @Override
  public String remap(String entryName) {
    // Strip root prefix
    if (!rootPrefix.isEmpty()) {
      if (!entryName.startsWith(rootPrefix)) {
        return null; // Entry outside the root directory - skip
      }
      entryName = entryName.substring(rootPrefix.length());
    }

    // Skip empty paths (the root directory entry itself)
    if (entryName.isEmpty()) {
      return null;
    }

    // Skip Prism metadata files
    if (SKIP_FILES.contains(entryName)) {
      return null;
    }

    // Skip icon files at root
    if (entryName.endsWith(".png") && !entryName.contains("/")) {
      return null;
    }

    // Flatten .minecraft/ to root
    if (entryName.startsWith(".minecraft/")) {
      entryName = entryName.substring(".minecraft/".length());
      if (entryName.isEmpty()) {
        return null; // The .minecraft/ directory entry itself
      }
    }

    return entryName;
  }
}
