package net.technicpack.minecraftcore.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExtractedFilesManifestTest {

  @TempDir Path tempDir;

  // -------------------------------------------------------------------------
  // Load shape handling
  // -------------------------------------------------------------------------

  @Test
  void loadFromMissingFileReturnsEmpty() {
    File binDir = tempDir.toFile();
    ExtractedFilesManifest manifest = ExtractedFilesManifest.load(binDir);
    assertTrue(manifest.getFiles().isEmpty());
  }

  @Test
  void loadFromArrayShapeReadsAllPaths() throws Exception {
    Files.write(
        tempDir.resolve("extractedFiles.json"),
        "[\"a.txt\", \"sub/b.txt\", \"sub/c.txt\"]".getBytes(StandardCharsets.UTF_8));

    ExtractedFilesManifest manifest = ExtractedFilesManifest.load(tempDir.toFile());

    assertEquals(
        new LinkedHashSet<>(Arrays.asList("a.txt", "sub/b.txt", "sub/c.txt")),
        manifest.getFiles());
  }

  @Test
  void loadFromLegacyMapShapeKeepsOnlyKeys() throws Exception {
    Files.write(
        tempDir.resolve("extractedFiles.json"),
        "{\"a.txt\":\"hash1\",\"sub/b.txt\":\"hash2\"}".getBytes(StandardCharsets.UTF_8));

    ExtractedFilesManifest manifest = ExtractedFilesManifest.load(tempDir.toFile());

    assertEquals(
        new LinkedHashSet<>(Arrays.asList("a.txt", "sub/b.txt")), manifest.getFiles());
  }

  @Test
  void loadFromMalformedJsonReturnsEmpty() throws Exception {
    Files.write(
        tempDir.resolve("extractedFiles.json"),
        "{not valid json".getBytes(StandardCharsets.UTF_8));

    ExtractedFilesManifest manifest = ExtractedFilesManifest.load(tempDir.toFile());

    assertTrue(manifest.getFiles().isEmpty());
  }

  // -------------------------------------------------------------------------
  // Round-trip + persistence
  // -------------------------------------------------------------------------

  @Test
  void saveThenLoadPreservesInsertionOrder() throws Exception {
    ExtractedFilesManifest original = new ExtractedFilesManifest();
    original.add("first.txt");
    original.add("second.txt");
    original.add("nested/third.txt");

    original.save(tempDir.toFile());
    ExtractedFilesManifest reloaded = ExtractedFilesManifest.load(tempDir.toFile());

    assertEquals(
        Arrays.asList("first.txt", "second.txt", "nested/third.txt"),
        new java.util.ArrayList<>(reloaded.getFiles()));
  }

  @Test
  void saveOverwritesExistingManifest() throws Exception {
    ExtractedFilesManifest first = new ExtractedFilesManifest();
    first.add("old.txt");
    first.save(tempDir.toFile());

    ExtractedFilesManifest second = new ExtractedFilesManifest();
    second.add("new.txt");
    second.save(tempDir.toFile());

    ExtractedFilesManifest reloaded = ExtractedFilesManifest.load(tempDir.toFile());
    assertEquals(Collections.singleton("new.txt"), reloaded.getFiles());
  }

  @Test
  void saveDoesNotLeaveTempFileBehind() throws Exception {
    ExtractedFilesManifest manifest = new ExtractedFilesManifest();
    manifest.add("only.txt");
    manifest.save(tempDir.toFile());

    assertFalse(
        Files.exists(tempDir.resolve("extractedFiles.json.tmp")),
        "atomic-rename should have consumed the .tmp file");
    assertTrue(Files.exists(tempDir.resolve("extractedFiles.json")));
  }

  // -------------------------------------------------------------------------
  // Orphan diff
  // -------------------------------------------------------------------------

  @Test
  void findOrphansReturnsPathsInOldButNotInNew() {
    ExtractedFilesManifest oldManifest = new ExtractedFilesManifest();
    oldManifest.add("a.txt");
    oldManifest.add("b.txt");
    oldManifest.add("c.txt");

    ExtractedFilesManifest newManifest = new ExtractedFilesManifest();
    newManifest.add("b.txt");

    Set<String> orphans = ExtractedFilesManifest.findOrphans(oldManifest, newManifest);

    assertEquals(new LinkedHashSet<>(Arrays.asList("a.txt", "c.txt")), orphans);
  }

  @Test
  void findOrphansReturnsEmptyWhenNothingRemoved() {
    ExtractedFilesManifest oldManifest = new ExtractedFilesManifest();
    oldManifest.add("a.txt");

    ExtractedFilesManifest newManifest = new ExtractedFilesManifest();
    newManifest.add("a.txt");
    newManifest.add("b.txt");

    Set<String> orphans = ExtractedFilesManifest.findOrphans(oldManifest, newManifest);

    assertTrue(orphans.isEmpty());
  }

  // -------------------------------------------------------------------------
  // Deletion
  // -------------------------------------------------------------------------

  @Test
  void deleteOrphansRemovesExistingFilesAndReturnsCount() throws Exception {
    Path modpackDir = tempDir.resolve("modpack");
    Files.createDirectories(modpackDir.resolve("sub"));
    Path fileA = modpackDir.resolve("a.txt");
    Path fileB = modpackDir.resolve("sub/b.txt");
    Files.write(fileA, "alpha".getBytes(StandardCharsets.UTF_8));
    Files.write(fileB, "beta".getBytes(StandardCharsets.UTF_8));

    Set<String> orphans = new LinkedHashSet<>(Arrays.asList("a.txt", "sub/b.txt"));
    int deleted = ExtractedFilesManifest.deleteOrphans(orphans, modpackDir.toFile());

    assertEquals(2, deleted);
    assertFalse(Files.exists(fileA));
    assertFalse(Files.exists(fileB));
  }

  @Test
  void deleteOrphansSkipsPathsThatAreNotRegularFiles() throws Exception {
    Path modpackDir = tempDir.resolve("modpack");
    Files.createDirectories(modpackDir.resolve("a-directory"));
    Path realFile = modpackDir.resolve("real.txt");
    Files.write(realFile, "x".getBytes(StandardCharsets.UTF_8));

    Set<String> orphans =
        new LinkedHashSet<>(Arrays.asList("a-directory", "missing.txt", "real.txt"));
    int deleted = ExtractedFilesManifest.deleteOrphans(orphans, modpackDir.toFile());

    assertEquals(1, deleted);
    assertFalse(Files.exists(realFile));
    assertTrue(Files.exists(modpackDir.resolve("a-directory")));
  }

  // -------------------------------------------------------------------------
  // Build
  // -------------------------------------------------------------------------

  @Test
  void buildFromExtractedFilesSkipsEntriesWithoutCorrespondingFile() throws Exception {
    Path baseDir = tempDir.resolve("base");
    Files.createDirectories(baseDir.resolve("sub"));
    Files.write(baseDir.resolve("real.txt"), new byte[] {1});
    Files.write(baseDir.resolve("sub/real.txt"), new byte[] {2});

    Set<String> candidates =
        new LinkedHashSet<>(
            Arrays.asList("real.txt", "sub/real.txt", "ghost.txt", "sub/ghost.txt"));

    ExtractedFilesManifest manifest =
        ExtractedFilesManifest.buildFromExtractedFiles(baseDir.toFile(), candidates);

    assertEquals(
        new LinkedHashSet<>(Arrays.asList("real.txt", "sub/real.txt")), manifest.getFiles());
  }
}
