package net.technicpack.launcher.settings.migration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.minecraftcore.install.processor.ModernInstallerLock;
import net.technicpack.utilslib.CryptoUtils;
import net.technicpack.utilslib.Memory;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LibraryCacheMigrationTest {
  private static final String SAMPLE = "test/migration/sample/1/sample-1.jar";

  @TempDir Path tempDir;

  @Test
  void movesCanonicalPayloadsAndPrunesOnlyTheirEmptiedAncestors() throws Exception {
    Fixture fixture = fixture("layout");
    Map<String, byte[]> payloads = new LinkedHashMap<>();
    payloads.put("org/example/core/1.0/core-1.0.jar", bytes("canonical jar"));
    payloads.put("org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-linux.jar", bytes("native classifier"));
    payloads.put("net/example/mappings/2/mappings-2.txt", bytes("non-jar mappings"));
    payloads.put("com/example/distribution/3/distribution-3.zip", bytes("zip payload"));
    for (Map.Entry<String, byte[]> payload : payloads.entrySet()) {
      write(fixture.cache.resolve(payload.getKey()), payload.getValue());
    }
    String retainedParentPayload = "retained/example/shared/1/shared-1.jar";
    write(fixture.cache.resolve(retainedParentPayload), bytes("payload with unrelated sibling"));
    Map<Path, byte[]> retained = new LinkedHashMap<>();
    for (String relative :
        Arrays.asList(
            "minecraft_1.16.1.jar",
            "discover.html",
            "custom/plain/renamed.jar",
            "org/example/noncanonical/1/not-the-artifact-1.jar",
            "org.example/roundtrip/1/roundtrip-1.jar",
            "fmllibs/org/example/legacy/1/legacy-1.jar",
            "processor-state/org/example/receipt/1/receipt-1.jar",
            "modern-installer-work/org/example/staged/1/staged-1.jar",
            "modern-installer-other/org/example/staged/1/staged-1.jar",
            "modern-installer.lock",
            "retained/example/shared/1/unrelated.txt")) {
      retained.put(fixture.cache.resolve(relative), bytes("keep " + relative));
    }
    retained.put(fixture.root.resolve("modpacks/pack/cache/" + SAMPLE), bytes("pack cache"));
    retained.put(fixture.root.resolve("modpacks/pack/libraries/" + SAMPLE), bytes("pack library"));
    retained.put(tempDir.resolve(".m2/repository/" + SAMPLE), bytes("separate Maven cache"));
    for (Map.Entry<Path, byte[]> sentinel : retained.entrySet()) {
      write(sentinel.getKey(), sentinel.getValue());
    }
    Path emptyDirectory = Files.createDirectories(fixture.cache.resolve("unrelated/empty"));

    migrate(fixture);

    assertVersion(fixture, "4");
    for (Map.Entry<String, byte[]> payload : payloads.entrySet()) {
      assertBytes(fixture.libraries.resolve(payload.getKey()), payload.getValue());
      assertFalse(Files.exists(fixture.cache.resolve(payload.getKey())));
      assertFalse(Files.exists(fixture.cache.resolve(payload.getKey()).getParent()));
    }
    assertFalse(Files.exists(fixture.cache.resolve("org/lwjgl")));
    assertFalse(Files.exists(fixture.cache.resolve("net")));
    assertFalse(Files.exists(fixture.cache.resolve("com")));
    assertBytes(
        fixture.libraries.resolve(retainedParentPayload), bytes("payload with unrelated sibling"));
    assertFalse(Files.exists(fixture.cache.resolve(retainedParentPayload)));
    for (Map.Entry<Path, byte[]> sentinel : retained.entrySet()) {
      assertBytes(sentinel.getKey(), sentinel.getValue());
    }
    assertTrue(Files.isDirectory(emptyDirectory));
    assertTrue(Files.isDirectory(fixture.cache));
  }

  @Test
  void deduplicatesWithoutRewritingAndCompletesDespiteDestinationConflicts() throws Exception {
    Fixture fixture = fixture("destinations");
    Path duplicate = write(fixture.cache.resolve(SAMPLE), bytes("same payload"));
    Path destination = write(fixture.libraries.resolve(SAMPLE), bytes("same payload"));
    Files.setLastModifiedTime(destination, FileTime.fromMillis(1234567890000L));
    FileTime originalDestinationTime = Files.getLastModifiedTime(destination);
    String conflicting = "test/migration/conflict/1/conflict-1.jar";
    write(fixture.cache.resolve(conflicting), bytes("cached version"));
    write(fixture.libraries.resolve(conflicting), bytes("preferred destination"));
    String directoryConflict = "test/migration/directory/1/directory-1.jar";
    write(fixture.cache.resolve(directoryConflict), bytes("cached directory conflict"));
    Path directorySentinel =
        write(fixture.libraries.resolve(directoryConflict).resolve("keep"), bytes("unrelated"));
    String independent = "test/migration/independent/1/independent-1.jar";
    write(fixture.cache.resolve(independent), bytes("independent payload"));

    migrate(fixture);

    assertVersion(fixture, "4");
    assertFalse(Files.exists(duplicate));
    assertBytes(destination, bytes("same payload"));
    assertEquals(originalDestinationTime, Files.getLastModifiedTime(destination));
    assertBytes(fixture.cache.resolve(conflicting), bytes("cached version"));
    assertBytes(fixture.libraries.resolve(conflicting), bytes("preferred destination"));
    assertBytes(fixture.cache.resolve(directoryConflict), bytes("cached directory conflict"));
    assertBytes(directorySentinel, bytes("unrelated"));
    assertBytes(fixture.libraries.resolve(independent), bytes("independent payload"));
    assertFalse(Files.exists(fixture.cache.resolve(independent)));
  }

  @Test
  void movesVerifiedSha1CompanionsButPreservesConflictingDestinationCompanions() throws Exception {
    Fixture fixture = fixture("paired-sidecars");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("paired payload"));
    byte[] matching = bytes(CryptoUtils.getSHA1(source) + "  *sample-1.jar\n");
    write(sidecar(source), matching);
    String conflict = "test/migration/sidecar-conflict/1/sidecar-conflict-1.jar";
    Path conflictSource = write(fixture.cache.resolve(conflict), bytes("duplicate payload"));
    Path conflictTarget = write(fixture.libraries.resolve(conflict), bytes("duplicate payload"));
    byte[] sourceCompanion = bytes(CryptoUtils.getSHA1(conflictSource));
    write(sidecar(conflictSource), sourceCompanion);
    byte[] destinationCompanion = bytes("existing destination metadata wins");
    write(sidecar(conflictTarget), destinationCompanion);

    migrate(fixture);

    assertVersion(fixture, "4");
    assertBytes(fixture.libraries.resolve(SAMPLE), bytes("paired payload"));
    assertBytes(sidecar(fixture.libraries.resolve(SAMPLE)), matching);
    assertFalse(Files.exists(source));
    assertFalse(Files.exists(sidecar(source)));
    assertBytes(conflictTarget, bytes("duplicate payload"));
    assertFalse(Files.exists(conflictSource));
    assertBytes(sidecar(conflictSource), sourceCompanion);
    assertBytes(sidecar(conflictTarget), destinationCompanion);
  }

  @Test
  void failedCompanionDeletionRetainsPayloadForAnIdempotentRetry() throws Exception {
    Fixture fixture = fixture("companion-retry");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("paired payload"));
    byte[] matching = bytes(CryptoUtils.getSHA1(source));
    write(sidecar(source), matching);
    Path parent = source.getParent();
    assumeTrue(Files.getFileStore(parent).supportsFileAttributeView("posix"));
    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(parent);
    try {
      Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("r-x------"));
      assumeTrue(!Files.isWritable(parent), "Current user bypasses filesystem write permissions");

      migrate(fixture);

      assertVersion(fixture, "3");
      assertBytes(source, bytes("paired payload"));
      assertBytes(sidecar(source), matching);
      assertBytes(fixture.libraries.resolve(SAMPLE), bytes("paired payload"));
      assertBytes(sidecar(fixture.libraries.resolve(SAMPLE)), matching);
    } finally {
      Files.setPosixFilePermissions(parent, permissions);
    }

    migrate(fixture);

    assertVersion(fixture, "4");
    assertFalse(Files.exists(source));
    assertFalse(Files.exists(sidecar(source)));
    assertBytes(fixture.libraries.resolve(SAMPLE), bytes("paired payload"));
    assertBytes(sidecar(fixture.libraries.resolve(SAMPLE)), matching);
  }

  @Test
  void doesNotImportInvalidOrUnpairedChecksumMetadata() throws Exception {
    Fixture fixture = fixture("invalid-sidecars");
    Path hashFixture = write(tempDir.resolve("other-bytes"), bytes("not any payload"));
    Map<String, byte[]> invalid = new LinkedHashMap<>();
    invalid.put("wrong", bytes(CryptoUtils.getSHA1(hashFixture)));
    invalid.put("malformed", bytes("not a checksum"));
    invalid.put("oversized", new byte[4097]);
    Path namedSource =
        write(fixture.cache.resolve("test/migration/named/1/named-1.jar"), bytes("named payload"));
    invalid.put("named", bytes(CryptoUtils.getSHA1(namedSource) + "  another-file.jar\n"));
    for (Map.Entry<String, byte[]> entry : invalid.entrySet()) {
      String artifact = entry.getKey();
      Path source =
          write(
              fixture.cache.resolve("test/migration/" + artifact + "/1/" + artifact + "-1.jar"),
              bytes(artifact + " payload"));
      write(sidecar(source), entry.getValue());
    }
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("metadata payload"));
    Map<String, byte[]> unsupported = new LinkedHashMap<>();
    for (String suffix : Arrays.asList(".md5", ".sha256", ".sha512", ".asc", ".SHA1", ".SHA256")) {
      unsupported.put(suffix, bytes(CryptoUtils.getSHA1(source)));
      write(source.resolveSibling(source.getFileName() + suffix), unsupported.get(suffix));
    }
    String orphan = "test/migration/orphan/1/orphan-1.jar.sha1";
    byte[] orphanBytes = bytes(CryptoUtils.getSHA1(source));
    write(fixture.cache.resolve(orphan), orphanBytes);
    String differing = "test/migration/differing/1/differing-1.jar";
    Path differingSource = write(fixture.cache.resolve(differing), bytes("source payload"));
    write(fixture.libraries.resolve(differing), bytes("different destination payload"));
    byte[] differingCompanion = bytes(CryptoUtils.getSHA1(differingSource));
    write(sidecar(differingSource), differingCompanion);

    migrate(fixture);

    assertVersion(fixture, "4");
    for (Map.Entry<String, byte[]> entry : invalid.entrySet()) {
      String artifact = entry.getKey();
      String relative = "test/migration/" + artifact + "/1/" + artifact + "-1.jar";
      assertBytes(fixture.libraries.resolve(relative), bytes(artifact + " payload"));
      assertFalse(Files.exists(fixture.cache.resolve(relative)));
      assertBytes(sidecar(fixture.cache.resolve(relative)), entry.getValue());
      assertFalse(Files.exists(sidecar(fixture.libraries.resolve(relative))));
    }
    assertBytes(fixture.libraries.resolve(SAMPLE), bytes("metadata payload"));
    assertFalse(Files.exists(source));
    for (Map.Entry<String, byte[]> entry : unsupported.entrySet()) {
      assertBytes(fixture.cache.resolve(SAMPLE + entry.getKey()), entry.getValue());
      assertFalse(Files.exists(fixture.libraries.resolve(SAMPLE + entry.getKey())));
    }
    assertBytes(fixture.cache.resolve(orphan), orphanBytes);
    assertFalse(Files.exists(fixture.libraries.resolve(orphan)));
    assertBytes(differingSource, bytes("source payload"));
    assertBytes(fixture.libraries.resolve(differing), bytes("different destination payload"));
    assertBytes(sidecar(differingSource), differingCompanion);
    assertFalse(Files.exists(sidecar(fixture.libraries.resolve(differing))));
  }

  @Test
  void persistsEarlierMigrationsAndRetriesPartialFailureOnlyUntilVersionFour() throws Exception {
    Fixture fixture = fixture("retry");
    fixture.settings.setLauncherSettingsVersion("1");
    fixture.settings.setMemory(0);
    fixture.settings.setJavaArgs(
        "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20"
            + " -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M");
    fixture.settings.save();
    String blocked = "blocked/example/library/1/library-1.jar";
    write(fixture.cache.resolve(blocked), bytes("retry me"));
    Path blocker = write(fixture.libraries.resolve("blocked"), bytes("directory blocker"));
    write(fixture.cache.resolve(SAMPLE), bytes("independent library"));

    migrate(fixture);

    assertVersion(fixture, "3");
    TechnicSettings persisted = loadSettings(fixture.root);
    assertEquals(Memory.DEFAULT_SETTINGS_ID, persisted.getMemory());
    assertTrue(persisted.isUsingDefaultJavaArgs());
    assertBytes(blocker, bytes("directory blocker"));
    assertBytes(fixture.cache.resolve(blocked), bytes("retry me"));
    assertBytes(fixture.libraries.resolve(SAMPLE), bytes("independent library"));
    assertFalse(Files.exists(fixture.cache.resolve(SAMPLE)));
    Path independentTarget = fixture.libraries.resolve(SAMPLE);
    Files.setLastModifiedTime(independentTarget, FileTime.fromMillis(1234567890000L));
    FileTime independentTime = Files.getLastModifiedTime(independentTarget);

    Files.delete(blocker);
    migrate(fixture, persisted);

    assertEquals("4", persisted.getLauncherSettingsVersion());
    assertEquals("4", loadSettings(fixture.root).getLauncherSettingsVersion());
    assertBytes(fixture.libraries.resolve(blocked), bytes("retry me"));
    assertFalse(Files.exists(fixture.cache.resolve(blocked)));
    assertEquals(independentTime, Files.getLastModifiedTime(independentTarget));
    Path lateSource = write(fixture.cache.resolve(SAMPLE), bytes("independent library"));
    String late = "test/migration/late/1/late-1.jar";
    write(fixture.cache.resolve(late), bytes("old launcher wrote this later"));

    migrate(fixture, loadSettings(fixture.root));

    assertEquals("4", loadSettings(fixture.root).getLauncherSettingsVersion());
    assertBytes(lateSource, bytes("independent library"));
    assertBytes(fixture.cache.resolve(late), bytes("old launcher wrote this later"));
    assertFalse(Files.exists(fixture.libraries.resolve(late)));
    assertEquals(independentTime, Files.getLastModifiedTime(independentTarget));
    assertBytes(fixture.libraries.resolve(blocked), bytes("retry me"));
  }

  @Test
  void deletionGuardRejectsSameSizeMutationWithRestoredModificationTime() throws Exception {
    Fixture fixture = fixture("source-mutation");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("original"));
    Path target = write(fixture.libraries.resolve(SAMPLE), bytes("original"));
    BasicFileAttributes original = attributes(source);
    String expected = CryptoUtils.getSHA256(source);
    Files.write(source, bytes("modified"));
    Files.setLastModifiedTime(source, original.lastModifiedTime());
    assertEquals(original.size(), Files.size(source));
    assertEquals(original.lastModifiedTime(), Files.getLastModifiedTime(source));

    assertFalse(
        LibraryCacheMigration.deleteVerifiedSource(
            fixture.cache, fixture.libraries, source, target, expected, original));

    assertBytes(source, bytes("modified"));
    assertBytes(target, bytes("original"));
  }

  @Test
  void deletionGuardRejectsChangedAttributesEvenWhenBytesStillMatch() throws Exception {
    Fixture fixture = fixture("source-attributes");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("original"));
    Path target = write(fixture.libraries.resolve(SAMPLE), bytes("original"));
    BasicFileAttributes original = attributes(source);
    String expected = CryptoUtils.getSHA256(source);
    Files.setLastModifiedTime(
        source, FileTime.fromMillis(original.lastModifiedTime().toMillis() - 60000));

    assertFalse(
        LibraryCacheMigration.deleteVerifiedSource(
            fixture.cache, fixture.libraries, source, target, expected, original));

    assertBytes(source, bytes("original"));
    assertBytes(target, bytes("original"));
  }

  @Test
  void deletionGuardRejectsReplacementWithIdenticalBytesAndTimestamp() throws Exception {
    Fixture fixture = fixture("source-replacement");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("original"));
    Path target = write(fixture.libraries.resolve(SAMPLE), bytes("original"));
    BasicFileAttributes original = attributes(source);
    assumeTrue(original.fileKey() != null, "Filesystem does not expose file identity");
    String expected = CryptoUtils.getSHA256(source);
    Path retainedOriginal = source.resolveSibling("retained-original");
    Files.move(source, retainedOriginal);
    write(source, bytes("original"));
    Files.setLastModifiedTime(source, original.lastModifiedTime());

    assertFalse(
        LibraryCacheMigration.deleteVerifiedSource(
            fixture.cache, fixture.libraries, source, target, expected, original));

    assertBytes(source, bytes("original"));
    assertBytes(retainedOriginal, bytes("original"));
    assertBytes(target, bytes("original"));
  }

  @Test
  void deletionGuardRequiresCurrentDestinationAndAllowsAlreadyRemovedSourceOnlyAfterVerification()
      throws Exception {
    Fixture fixture = fixture("destination-mutation");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("original"));
    Path target = write(fixture.libraries.resolve(SAMPLE), bytes("original"));
    BasicFileAttributes original = attributes(source);
    String expected = CryptoUtils.getSHA256(source);
    Files.delete(target);

    assertFalse(
        LibraryCacheMigration.deleteVerifiedSource(
            fixture.cache, fixture.libraries, source, target, expected, original));
    assertBytes(source, bytes("original"));
    write(target, bytes("modified"));
    assertFalse(
        LibraryCacheMigration.deleteVerifiedSource(
            fixture.cache, fixture.libraries, source, target, expected, original));
    assertBytes(source, bytes("original"));
    assertBytes(target, bytes("modified"));
    Files.delete(source);
    assertFalse(
        LibraryCacheMigration.deleteVerifiedSource(
            fixture.cache, fixture.libraries, source, target, expected, original));
    write(target, bytes("original"));
    assertTrue(
        LibraryCacheMigration.deleteVerifiedSource(
            fixture.cache, fixture.libraries, source, target, expected, original));
    assertBytes(target, bytes("original"));
    assertFalse(Files.exists(source));
  }

  @Test
  void neverFollowsSourceSymlinkSubtreesOrPayloadSymlinks() throws Exception {
    Fixture fixture = fixture("source-links");
    Path outside = Files.createDirectories(tempDir.resolve("source-outside"));
    Path outsidePayload =
        write(outside.resolve("example/external/1/external-1.jar"), bytes("external"));
    Path subtree = symlink(fixture.cache.resolve("linked"), outside);
    Path externalFile = write(outside.resolve("single-file"), bytes("external file"));
    Path sourceLink = fixture.cache.resolve(SAMPLE);
    Files.createDirectories(sourceLink.getParent());
    symlink(sourceLink, externalFile);

    migrate(fixture);

    assertVersion(fixture, "4");
    assertTrue(Files.isSymbolicLink(subtree));
    assertTrue(Files.isSymbolicLink(sourceLink));
    assertBytes(outsidePayload, bytes("external"));
    assertBytes(externalFile, bytes("external file"));
    assertFalse(Files.exists(fixture.libraries.resolve("linked")));
    assertFalse(Files.exists(fixture.libraries.resolve(SAMPLE)));
  }

  @Test
  void escapingDestinationParentPreservesExternalBytesAndAllowsIndependentTransfers()
      throws Exception {
    Fixture fixture = fixture("destination-parent");
    Path outside = Files.createDirectories(tempDir.resolve("destination-outside"));
    String blocked = "escape/example/library/1/library-1.jar";
    write(fixture.cache.resolve(blocked), bytes("cached bytes"));
    Path outsideTarget =
        write(outside.resolve("example/library/1/library-1.jar"), bytes("external"));
    Path parentLink = symlink(fixture.libraries.resolve("escape"), outside);
    write(fixture.cache.resolve(SAMPLE), bytes("independent"));

    migrate(fixture);

    assertVersion(fixture, "3");
    assertTrue(Files.isSymbolicLink(parentLink));
    assertBytes(outsideTarget, bytes("external"));
    assertBytes(fixture.cache.resolve(blocked), bytes("cached bytes"));
    assertBytes(fixture.libraries.resolve(SAMPLE), bytes("independent"));
    assertFalse(Files.exists(fixture.cache.resolve(SAMPLE)));
  }

  @Test
  void finalDestinationSymlinksRemainConflictsEvenWhenTheirBytesMatch() throws Exception {
    Fixture fixture = fixture("destination-links");
    Path outside = write(tempDir.resolve("linked-external-file"), bytes("matching payload"));
    Path inside = write(fixture.libraries.resolve("unrelated"), bytes("matching payload"));
    for (String artifact : Arrays.asList("external", "internal")) {
      String relative = "test/migration/" + artifact + "/1/" + artifact + "-1.jar";
      write(fixture.cache.resolve(relative), bytes("matching payload"));
      Path target = fixture.libraries.resolve(relative);
      Files.createDirectories(target.getParent());
      symlink(target, artifact.equals("external") ? outside : inside);
    }

    migrate(fixture);

    assertVersion(fixture, "4");
    for (String artifact : Arrays.asList("external", "internal")) {
      String relative = "test/migration/" + artifact + "/1/" + artifact + "-1.jar";
      assertBytes(fixture.cache.resolve(relative), bytes("matching payload"));
      assertTrue(Files.isSymbolicLink(fixture.libraries.resolve(relative)));
    }
    assertBytes(outside, bytes("matching payload"));
    assertBytes(inside, bytes("matching payload"));
  }

  @Test
  void rejectsEqualAndNestedRepositoryRootsBeforeMovingAnything() throws Exception {
    for (String arrangement :
        Arrays.asList("equal", "destination-inside-source", "source-inside-destination")) {
      Fixture fixture = fixture(arrangement);
      Path alias;
      if (arrangement.equals("source-inside-destination")) {
        Files.delete(fixture.cache);
        Path nested = Files.createDirectories(fixture.libraries.resolve("nested"));
        alias = symlink(fixture.cache, nested);
      } else {
        Files.delete(fixture.libraries);
        Path target =
            arrangement.equals("equal")
                ? fixture.cache
                : Files.createDirectories(fixture.cache.resolve("nested"));
        alias = symlink(fixture.libraries, target);
      }
      Path source = write(fixture.cache.resolve(SAMPLE), bytes("must remain"));
      Path unrelated = write(fixture.root.resolve("unrelated"), bytes("root sentinel"));

      migrate(fixture);

      assertVersion(fixture, "3");
      assertTrue(Files.isSymbolicLink(alias));
      assertBytes(source, bytes("must remain"));
      assertBytes(unrelated, bytes("root sentinel"));
      if (!arrangement.equals("equal")) {
        assertFalse(Files.exists(fixture.libraries.resolve(SAMPLE)));
      }
    }
  }

  @Test
  void rejectsRepositoryRootAliasesEscapingTheConfiguredRoot() throws Exception {
    for (String rootName : Arrays.asList("cache", "libraries")) {
      Fixture fixture = fixture("escaping-root-" + rootName);
      Path outside = Files.createDirectories(tempDir.resolve("outside-root-" + rootName));
      Path alias = fixture.root.resolve(rootName);
      Files.delete(alias);
      symlink(alias, outside);
      Path source = write(fixture.cache.resolve(SAMPLE), bytes("cached library"));
      Path externalSentinel = write(outside.resolve("unrelated"), bytes("external sentinel"));

      migrate(fixture);

      assertVersion(fixture, "3");
      assertTrue(Files.isSymbolicLink(alias));
      assertBytes(source, bytes("cached library"));
      assertBytes(externalSentinel, bytes("external sentinel"));
      assertFalse(Files.exists(fixture.libraries.resolve(SAMPLE)));
    }
  }

  @Test
  void acceptsDisjointRepositoryAliasesContainedInTheConfiguredRoot() throws Exception {
    Fixture fixture = fixture("contained-roots");
    Path realCache = Files.createDirectories(fixture.root.resolve("storage/old"));
    Path realLibraries = Files.createDirectories(fixture.root.resolve("storage/new"));
    Files.delete(fixture.cache);
    Files.delete(fixture.libraries);
    symlink(fixture.cache, realCache);
    symlink(fixture.libraries, realLibraries);
    write(realCache.resolve(SAMPLE), bytes("safe aliases"));

    migrate(fixture);

    assertVersion(fixture, "4");
    assertTrue(Files.isSymbolicLink(fixture.cache));
    assertTrue(Files.isSymbolicLink(fixture.libraries));
    assertBytes(realLibraries.resolve(SAMPLE), bytes("safe aliases"));
    assertFalse(Files.exists(realCache.resolve(SAMPLE)));
  }

  @Test
  void acceptsSymlinkedConfiguredRootWithSafeDisjointRepositories() throws Exception {
    Path realRoot = Files.createDirectories(tempDir.resolve("real-root"));
    Path rootAlias = symlink(tempDir.resolve("configured-root"), realRoot);
    Fixture fixture = new Fixture(rootAlias);
    write(fixture.cache.resolve(SAMPLE), bytes("configured alias"));

    migrate(fixture);

    assertVersion(fixture, "4");
    assertTrue(Files.isSymbolicLink(rootAlias));
    assertBytes(realRoot.resolve("libraries").resolve(SAMPLE), bytes("configured alias"));
    assertFalse(Files.exists(realRoot.resolve("cache").resolve(SAMPLE)));
  }

  @Test
  void repositoryRootFilesArePreservedAndKeepTheMigrationRetryable() throws Exception {
    for (String rootName : Arrays.asList("cache", "libraries")) {
      Fixture fixture = fixture("root-file-" + rootName);
      Path blocker = fixture.root.resolve(rootName);
      Files.delete(blocker);
      write(blocker, bytes("not a directory"));
      if (rootName.equals("libraries")) {
        write(fixture.cache.resolve(SAMPLE), bytes("cached library"));
      }

      migrate(fixture);

      assertVersion(fixture, "3");
      assertBytes(blocker, bytes("not a directory"));
      if (rootName.equals("libraries")) {
        assertBytes(fixture.cache.resolve(SAMPLE), bytes("cached library"));
      } else {
        assertFalse(Files.exists(fixture.libraries.resolve(SAMPLE)));
      }
    }
  }

  @Test
  void danglingRepositoryRootLinksAreNotRepairedOrTreatedAsAbsent() throws Exception {
    for (String rootName : Arrays.asList("cache", "libraries")) {
      Fixture fixture = fixture("dangling-root-" + rootName);
      Path alias = fixture.root.resolve(rootName);
      Files.delete(alias);
      Path missing = fixture.root.resolve("missing-" + rootName);
      symlink(alias, missing);
      if (rootName.equals("libraries")) {
        write(fixture.cache.resolve(SAMPLE), bytes("cached library"));
      }

      migrate(fixture);

      assertVersion(fixture, "3");
      assertTrue(Files.isSymbolicLink(alias));
      assertEquals(missing, Files.readSymbolicLink(alias));
      assertFalse(Files.exists(missing, LinkOption.NOFOLLOW_LINKS));
      if (rootName.equals("libraries")) {
        assertBytes(fixture.cache.resolve(SAMPLE), bytes("cached library"));
      }
    }
  }

  @Test
  void waitsForSharedWriterLockBeforeCreatingDestinationOrChangingSource() throws Exception {
    Fixture fixture = fixture("lock-wait");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("locked payload"));
    Files.delete(fixture.libraries);
    CountDownLatch attempting = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> waiter;
      try (ModernInstallerLock ignored = ModernInstallerLock.acquire(fixture.cache, () -> false)) {
        waiter =
            executor.submit(
                () -> {
                  attempting.countDown();
                  return new LibraryCacheMigration()
                      .migrate(fixture.settings, null, fixture.fileSystem, null);
                });
        assertTrue(attempting.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> waiter.get(250, TimeUnit.MILLISECONDS));
        assertBytes(source, bytes("locked payload"));
        assertFalse(Files.exists(fixture.libraries));
      }
      assertTrue(waiter.get(5, TimeUnit.SECONDS));
      assertBytes(fixture.libraries.resolve(SAMPLE), bytes("locked payload"));
      assertFalse(Files.exists(source));
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void interruptedLockWaitReturnsIncompletePreservesFlagAndCanBeRetried() throws Exception {
    Fixture fixture = fixture("lock-interruption");
    Path source = write(fixture.cache.resolve(SAMPLE), bytes("interrupted payload"));
    Files.delete(fixture.libraries);
    CountDownLatch attempting = new CountDownLatch(1);
    AtomicReference<Thread> waitingThread = new AtomicReference<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      try (ModernInstallerLock ignored = ModernInstallerLock.acquire(fixture.cache, () -> false)) {
        Future<boolean[]> waiter =
            executor.submit(
                () -> {
                  waitingThread.set(Thread.currentThread());
                  attempting.countDown();
                  try {
                    boolean complete =
                        new LibraryCacheMigration()
                            .migrate(fixture.settings, null, fixture.fileSystem, null);
                    return new boolean[] {complete, Thread.currentThread().isInterrupted()};
                  } finally {
                    Thread.interrupted();
                  }
                });
        assertTrue(attempting.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> waiter.get(250, TimeUnit.MILLISECONDS));
        waitingThread.get().interrupt();
        boolean[] result = waiter.get(5, TimeUnit.SECONDS);
        assertFalse(result[0]);
        assertTrue(result[1]);
        assertBytes(source, bytes("interrupted payload"));
        assertFalse(Files.exists(fixture.libraries));
      }
      Future<Boolean> retry =
          executor.submit(
              () ->
                  new LibraryCacheMigration()
                      .migrate(fixture.settings, null, fixture.fileSystem, null));
      assertTrue(retry.get(5, TimeUnit.SECONDS));
      assertBytes(fixture.libraries.resolve(SAMPLE), bytes("interrupted payload"));
      assertFalse(Files.exists(source));
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
      Thread.interrupted();
    }
  }

  @Test
  void settingsJsonWithoutVersionStillMigratesFromThree() throws Exception {
    Fixture fixture = fixture("missing-version");
    write(fixture.root.resolve("settings.json"), bytes("{\"memory\":6}"));
    TechnicSettings loaded = loadSettings(fixture.root);
    assertEquals("3", loaded.getLauncherSettingsVersion());
    write(fixture.cache.resolve(SAMPLE), bytes("legacy settings payload"));

    migrate(fixture, loaded);

    assertEquals("4", loaded.getLauncherSettingsVersion());
    assertEquals("4", loadSettings(fixture.root).getLauncherSettingsVersion());
    assertBytes(fixture.libraries.resolve(SAMPLE), bytes("legacy settings payload"));
    assertFalse(Files.exists(fixture.cache.resolve(SAMPLE)));
  }

  @Test
  void emptyFreshCacheCompletesAndAbsentCacheRemainsAnAlreadyCompleteNoOp() throws Exception {
    Fixture fresh = fixture("fresh");
    assertEquals("3", fresh.settings.getLauncherSettingsVersion());

    migrate(fresh);

    assertVersion(fresh, "4");
    assertTrue(Files.isDirectory(fresh.cache));
    Fixture absent = fixture("absent-cache");
    Files.delete(absent.cache);

    migrate(absent);

    assertVersion(absent, "4");
    assertFalse(Files.exists(absent.cache, LinkOption.NOFOLLOW_LINKS));
  }

  private Fixture fixture(String name) {
    return new Fixture(tempDir.resolve(name));
  }

  private static void migrate(Fixture fixture) {
    migrate(fixture, fixture.settings);
  }

  private static void migrate(Fixture fixture, TechnicSettings settings) {
    SettingsFactory.migrateSettings(
        settings,
        null,
        fixture.fileSystem,
        null,
        Arrays.asList(
            new ResetJvmArgsIfDefaultString(),
            new DefaultMemorySentinelMigration(),
            new LibraryCacheMigration()));
  }

  private static void assertVersion(Fixture fixture, String version) throws IOException {
    assertEquals(version, fixture.settings.getLauncherSettingsVersion());
    assertEquals(version, loadSettings(fixture.root).getLauncherSettingsVersion());
  }

  private static TechnicSettings loadSettings(Path root) throws IOException {
    Path path = root.resolve("settings.json");
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      TechnicSettings settings = Utils.getGson().fromJson(reader, TechnicSettings.class);
      settings.setFilePath(path.toFile());
      return settings;
    }
  }

  private static Path write(Path path, byte[] content) throws IOException {
    Files.createDirectories(path.getParent());
    return Files.write(path, content);
  }

  private static void assertBytes(Path path, byte[] expected) throws IOException {
    assertArrayEquals(expected, Files.readAllBytes(path), path.toString());
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static Path sidecar(Path payload) {
    return payload.resolveSibling(payload.getFileName() + ".sha1");
  }

  private static BasicFileAttributes attributes(Path path) throws IOException {
    return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
  }

  private static Path symlink(Path link, Path target) throws IOException {
    try {
      return Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | FileSystemException unavailable) {
      assumeTrue(false, "Symbolic links unavailable: " + unavailable);
      throw new AssertionError("unreachable", unavailable);
    }
  }

  private static final class Fixture {
    private final Path root;
    private final Path cache;
    private final Path libraries;
    private final LauncherFileSystem fileSystem;
    private final TechnicSettings settings;

    private Fixture(Path root) {
      this.root = root;
      fileSystem = new LauncherFileSystem(root);
      cache = root.resolve("cache");
      libraries = root.resolve("libraries");
      settings = new TechnicSettings();
      settings.setFilePath(root.resolve("settings.json").toFile());
      settings.save();
    }
  }
}
