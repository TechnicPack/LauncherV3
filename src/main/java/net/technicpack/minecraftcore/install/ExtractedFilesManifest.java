package net.technicpack.minecraftcore.install;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.technicpack.utilslib.Utils;

/**
 * Tracks files extracted from modpack zips so orphaned files (removed in a modpack update) can be
 * cleaned up.
 *
 * <p>Stored as bin/extractedFiles.json with path → SHA-256 hash entries.
 */
public class ExtractedFilesManifest {

  private static final String MANIFEST_FILENAME = "extractedFiles.json";
  private static final Gson GSON = new Gson();
  private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

  private final Map<String, String> files;

  public ExtractedFilesManifest() {
    this.files = new LinkedHashMap<>();
  }

  private ExtractedFilesManifest(Map<String, String> files) {
    this.files = new LinkedHashMap<>(files);
  }

  public Map<String, String> getFiles() {
    return files;
  }

  public void put(String relativePath, String sha256) {
    files.put(relativePath, sha256);
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
      Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
      if (map != null) {
        return new ExtractedFilesManifest(map);
      }
    } catch (IOException | JsonIOException e) {
      Utils.getLogger()
          .log(Level.WARNING, "Failed to load extracted files manifest, starting fresh", e);
    }
    return new ExtractedFilesManifest();
  }

  /** Save the manifest to bin/extractedFiles.json. */
  public void save(File binDir) throws IOException {
    binDir.mkdirs();
    File manifestFile = new File(binDir, MANIFEST_FILENAME);
    try (Writer writer = Files.newBufferedWriter(manifestFile.toPath(), StandardCharsets.UTF_8)) {
      GSON.toJson(files, MAP_TYPE, writer);
    }
  }

  /**
   * Find orphaned files: paths that exist in the old manifest but not in this (new) manifest.
   * Returns relative paths.
   */
  public static Set<String> findOrphans(
      ExtractedFilesManifest oldManifest, ExtractedFilesManifest newManifest) {
    return oldManifest.files.keySet().stream()
        .filter(path -> !newManifest.files.containsKey(path))
        .collect(Collectors.toSet());
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
   * Build a manifest from a set of extracted file paths. Only hashes the specified files. Paths in
   * the set should be relative to baseDir.
   */
  public static ExtractedFilesManifest buildFromExtractedFiles(
      File baseDir, Set<String> extractedPaths) {
    ExtractedFilesManifest manifest = new ExtractedFilesManifest();
    Path basePath = baseDir.toPath();

    for (String relativePath : extractedPaths) {
      Path filePath = basePath.resolve(relativePath);
      if (Files.isRegularFile(filePath)) {
        try {
          String hash = sha256(filePath);
          manifest.put(relativePath, hash);
        } catch (IOException e) {
          Utils.getLogger().log(Level.WARNING, "Failed to hash file: " + filePath, e);
        }
      }
    }

    return manifest;
  }

  /** Compute SHA-256 hash of a file. */
  public static String sha256(Path file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream is = Files.newInputStream(file)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
          digest.update(buffer, 0, read);
        }
      }
      byte[] hashBytes = digest.digest();
      StringBuilder sb = new StringBuilder(hashBytes.length * 2);
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("SHA-256 not available", e);
    }
  }
}
