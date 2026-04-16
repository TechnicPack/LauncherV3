package net.technicpack.minecraftcore.install;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import net.technicpack.launchercore.util.AtomicJsonWriter;
import net.technicpack.utilslib.Utils;

/**
 * Tracks files extracted from modpack zips so orphaned files (removed in a modpack update) can be
 * cleaned up.
 *
 * <p>Stored as bin/extractedFiles.json with a JSON array of relative paths. For backward
 * compatibility, the loader also accepts the legacy JSON object shape ({@code {"path": "hash"}}),
 * discarding the hash values.
 */
public class ExtractedFilesManifest {

  private static final String MANIFEST_FILENAME = "extractedFiles.json";
  private static final Gson GSON = new Gson();

  private final Set<String> files;

  public ExtractedFilesManifest() {
    this.files = new LinkedHashSet<>();
  }

  private ExtractedFilesManifest(Set<String> files) {
    this.files = new LinkedHashSet<>(files);
  }

  public Set<String> getFiles() {
    return files;
  }

  public void add(String relativePath) {
    files.add(relativePath);
  }

  /** Returns true if the manifest file exists in binDir. */
  public static boolean exists(File binDir) {
    return new File(binDir, MANIFEST_FILENAME).exists();
  }

  /** Load the manifest from bin/extractedFiles.json, or return an empty manifest if not found. */
  public static ExtractedFilesManifest load(File binDir) {
    File manifestFile = new File(binDir, MANIFEST_FILENAME);
    if (!manifestFile.exists()) {
      return new ExtractedFilesManifest();
    }

    try (Reader reader = Files.newBufferedReader(manifestFile.toPath(), StandardCharsets.UTF_8)) {
      JsonElement root = GSON.fromJson(reader, JsonElement.class);
      if (root == null || root.isJsonNull()) {
        return new ExtractedFilesManifest();
      }

      Set<String> paths = new LinkedHashSet<>();
      if (root.isJsonArray()) {
        for (JsonElement element : root.getAsJsonArray()) {
          if (element != null && !element.isJsonNull()) {
            paths.add(element.getAsString());
          }
        }
      } else if (root.isJsonObject()) {
        // Legacy format: {"path": "hash", ...}. Hash values are discarded.
        for (String key : root.getAsJsonObject().keySet()) {
          paths.add(key);
        }
      }
      return new ExtractedFilesManifest(paths);
    } catch (IOException | JsonParseException e) {
      Utils.getLogger()
          .log(Level.WARNING, "Failed to load extracted files manifest, starting fresh", e);
    }
    return new ExtractedFilesManifest();
  }

  /**
   * Save the manifest to bin/extractedFiles.json atomically.
   *
   * <p>Writes to a sibling {@code .tmp} file first, then atomically renames it over the target. If
   * the JVM dies mid-write, the original manifest is left intact and the {@code .tmp} is orphaned
   * (will be overwritten on next save). This prevents the orphan-cleanup logic from reading a
   * truncated manifest and concluding that all previously-extracted files are orphaned.
   */
  public void save(File binDir) throws IOException {
    binDir.mkdirs();
    Path manifestPath = new File(binDir, MANIFEST_FILENAME).toPath();
    AtomicJsonWriter.write(manifestPath, new ArrayList<>(files), GSON);
  }

  /**
   * Find orphaned files: paths that exist in the old manifest but not in this (new) manifest.
   * Returns relative paths.
   */
  public static Set<String> findOrphans(
      ExtractedFilesManifest oldManifest, ExtractedFilesManifest newManifest) {
    Set<String> orphans = new LinkedHashSet<>(oldManifest.files);
    orphans.removeAll(newManifest.files);
    return orphans;
  }

  /** Delete orphaned files from the modpack directory. */
  public static int deleteOrphans(Set<String> orphans, File modpackDir) {
    int deleted = 0;
    for (String orphan : orphans) {
      File file = new File(modpackDir, orphan);
      if (file.exists() && file.isFile()) {
        if (file.delete()) {
          deleted++;
        } else {
          Utils.getLogger().warning("Failed to delete orphaned file: " + file.getAbsolutePath());
        }
      }
    }
    return deleted;
  }

  /**
   * Build a manifest from a set of extracted file paths. Paths in the set should be relative to
   * baseDir; entries that don't exist as regular files are skipped.
   */
  public static ExtractedFilesManifest buildFromExtractedFiles(
      File baseDir, Set<String> extractedPaths) {
    ExtractedFilesManifest manifest = new ExtractedFilesManifest();

    for (String relativePath : extractedPaths) {
      if (Files.isRegularFile(baseDir.toPath().resolve(relativePath))) {
        manifest.add(relativePath);
      }
    }

    return manifest;
  }
}
