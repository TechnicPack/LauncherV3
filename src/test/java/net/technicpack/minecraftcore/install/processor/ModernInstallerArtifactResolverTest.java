package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.technicpack.launchercore.install.plan.NodeProgressReporter;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import net.technicpack.utilslib.CryptoUtils;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ModernInstallerArtifactResolverTest {
  private static final String HASH = "0123456789012345678901234567890123456789";
  @TempDir Path temp;
  private Path root;
  private Path home;
  private String previousHome;
  private ModernInstallerLock lock;
  private static final NodeProgressReporter REPORTER =
      new NodeProgressReporter() {
        public void updateNodeProgress(float percent) {}

        public void updateCurrentItem(String label, CurrentItemMode mode, Float percent) {}
      };

  @BeforeEach
  void isolateRepositoryAndHoldWriterLock() throws Exception {
    root = Files.createDirectories(temp.resolve("root"));
    home = Files.createDirectories(temp.resolve("home"));
    previousHome = System.getProperty("user.home");
    System.setProperty("user.home", home.toString());
    lock = ModernInstallerLock.acquire(root.resolve("cache"), () -> false);
  }

  @AfterEach
  void releaseWriterLockAndRestoreHome() throws Exception {
    try {
      if (lock != null) lock.close();
    } finally {
      if (previousHome == null) System.clearProperty("user.home");
      else System.setProperty("user.home", previousHome);
    }
  }

  @Test
  void generatedCoordinatesArePathsNotInferredInputs() throws Exception {
    ModernInstallerProfile profile =
        profile(
            "[]",
            "{\"PATCHED\":{\"client\":\"[net.neoforged:minecraft-client-patched:26.2.0.75]\"}}",
            "[{\"jar\":\"net.neoforged.installertools:tools:1\","
                + "\"classpath\":[\"net.neoforged.installertools:tools:1\"],\"args\":[\"{PATCHED}\"]}]");
    ModernInstallerArtifactResolver.ArtifactPlan plan =
        ModernInstallerArtifactResolver.plan(profile, Collections.emptyList(), runtime());
    assertNull(
        plan.getRequest(
            "net/neoforged/minecraft-client-patched/26.2.0.75/minecraft-client-patched-26.2.0.75.jar"));
    assertEquals(1, plan.getRequests().size());
    ModernInstallerArtifactResolver.ArtifactRequest tool = plan.getRequests().get(0);
    assertTrue(tool.isInferred());
    assertEquals(
        Collections.singletonList(
            "https://maven.neoforged.net/releases/net/neoforged/installertools/tools/1/tools-1.jar"),
        tool.getUrls());
  }

  @Test
  void zeroClientProfileDoesNotAcquireServerToolsButKeepsGameArtifacts() throws Exception {
    ModernInstallerProfile profile =
        profile(
            "[" + declaration("tool:server:1", "", HASH, 1) + "]",
            "[]",
            "[{\"sides\":[\"server\"],\"jar\":\"tool:server:1\",\"classpath\":[],\"args\":[]}]");
    ModernInstallerArtifactResolver.ArtifactPlan plan =
        ModernInstallerArtifactResolver.plan(
            profile,
            Collections.singletonList(new Library("game:client:1", "", HASH, 1)),
            runtime());
    assertFalse(plan.hasClientProcessors());
    assertNull(plan.getRequest("tool/server/1/server-1.jar"));
    assertEquals(1, plan.getGameArtifacts().size());
    assertTrue(plan.getGameArtifacts().get(0).isEmptyUrl());
  }

  @Test
  void resolvesNativeBitnessBeforeSelectingAuthoritativeMetadata() throws Exception {
    byte[] expected = archiveBytes("marker.txt", "declared native");
    byte[] unrelated = archiveBytes("marker.txt", "wrong but valid archive");
    String os = net.technicpack.utilslib.OperatingSystem.getOperatingSystem().getName();
    Library library =
        net.technicpack.minecraftcore.MojangUtils.getGson()
            .fromJson(
                "{\"name\":\"example:native:1\",\"natives\":{\""
                    + os
                    + "\":\"natives-${arch}\"},"
                    + "\"downloads\":{\"classifiers\":{\"natives-64\":{\"path\":\"selected/native.jar\","
                    + "\"sha1\":\""
                    + sha1(expected)
                    + "\",\"size\":"
                    + expected.length
                    + ",\"url\":\"\"}}}}",
                Library.class);
    Path wrong =
        write(root.resolve("libraries/example/native/1/native-1-natives-64.jar"), unrelated);
    Path installer = archive("maven/selected/native.jar", expected);
    ModernInstallerArtifactResolver.ArtifactPlan plan =
        ModernInstallerArtifactResolver.plan(
            profile("[]", "{}", "[]"), Collections.singletonList(library), runtime());

    resolver(installer).materialize(plan, REPORTER);

    assertArrayEquals(expected, Files.readAllBytes(root.resolve("libraries/selected/native.jar")));
    assertArrayEquals(unrelated, Files.readAllBytes(wrong));
  }

  @Test
  void identicalPathsRequireCompatibleHashesAndSizes() throws Exception {
    ModernInstallerProfile profile = profile("[]", "{}", "[]");
    Library first = new Library("example:tool:1", "https://first.invalid/tool.jar", HASH, 12);
    Library second = new Library("example:tool:1", "https://second.invalid/tool.jar", HASH, 12);
    ModernInstallerArtifactResolver.ArtifactPlan plan =
        ModernInstallerArtifactResolver.plan(profile, Arrays.asList(first, second), runtime());
    assertEquals(1, plan.getRequests().size());
    assertTrue(
        plan.getRequests().get(0).getUrls().indexOf("https://first.invalid/tool.jar")
            < plan.getRequests().get(0).getUrls().indexOf("https://second.invalid/tool.jar"));
    assertThrows(
        java.io.IOException.class,
        () ->
            ModernInstallerArtifactResolver.plan(
                profile,
                Arrays.asList(first, new Library("example:tool:1", "", HASH, 13)),
                runtime()));
    assertThrows(
        java.io.IOException.class,
        () ->
            ModernInstallerArtifactResolver.plan(
                profile,
                Arrays.asList(
                    first, new Library("example:tool:1", "", "f" + HASH.substring(1), 12)),
                runtime()));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void executableDeclarationCannotRedirectItsCanonicalCoordinate(boolean collidingGameAlias)
      throws Exception {
    String redirected =
        declaration("example:tool:1", "", HASH, 1)
            .replace("\"sha1\"", "\"path\":\"elsewhere/tool.jar\",\"sha1\"");
    ModernInstallerProfile profile =
        profile(
            "[" + redirected + "]",
            "{}",
            "[{\"jar\":\"example:tool:1\",\"classpath\":[],\"args\":[]}]");
    Library alias =
        net.technicpack.minecraftcore.MojangUtils.getGson()
            .fromJson(redirected.replace("example:tool:1", "example:alias:1"), Library.class);
    assertThrows(
        java.io.IOException.class,
        () ->
            ModernInstallerArtifactResolver.plan(
                profile,
                collidingGameAlias ? Collections.singletonList(alias) : Collections.emptyList(),
                runtime()));
  }

  @Test
  void localSourcesFollowPrecedenceAndHashlessArchivesStillRequireValidZip() throws Exception {
    byte[] dedicatedBytes = archiveBytes("dedicated", "dedicated");
    byte[] embeddedBytes = archiveBytes("embedded", "embedded");
    byte[] legacyBytes = archiveBytes("legacy", "legacy");
    byte[] mavenBytes = archiveBytes("maven", "maven");
    ModernInstallerArtifactResolver.ArtifactRequest request =
        request(null, Collections.emptyList(), false, false);
    Path target = root.resolve("libraries").resolve(request.getPath());
    Path legacy = root.resolve("cache").resolve(request.getPath());
    Path maven = home.resolve(".m2/repository").resolve(request.getPath());
    Path installer = archive("maven/" + request.getPath(), embeddedBytes);
    write(target, dedicatedBytes);
    write(legacy, legacyBytes);
    write(maven, mavenBytes);
    ModernInstallerArtifactResolver resolver = resolver(installer);
    ModernInstallerArtifactResolver.ArtifactPlan plan = plan(request, false);

    assertTrue(resolver.materialize(plan, REPORTER).isEmpty());
    assertArrayEquals(dedicatedBytes, Files.readAllBytes(target));
    Files.delete(target);
    resolver.materialize(plan, REPORTER);
    assertArrayEquals(embeddedBytes, Files.readAllBytes(target));
    assertArrayEquals(legacyBytes, Files.readAllBytes(legacy));
    Files.delete(target);
    Files.write(installer, archiveBytes("unrelated", "retained"));
    resolver.materialize(plan, REPORTER);
    assertArrayEquals(legacyBytes, Files.readAllBytes(target));
    assertFalse(Files.isSameFile(legacy, target));
    Files.write(target, new byte[] {1});
    assertArrayEquals(legacyBytes, Files.readAllBytes(legacy));
    Files.write(legacy, new byte[] {2});
    resolver.materialize(plan, REPORTER);
    assertArrayEquals(mavenBytes, Files.readAllBytes(target));
    assertArrayEquals(mavenBytes, Files.readAllBytes(maven));
    assertArrayEquals(new byte[] {2}, Files.readAllBytes(legacy));
  }

  @Test
  void invalidEmbeddedAndLegacyBytesFallThroughToVerifiedMavenCopy() throws Exception {
    byte[] bytes =
        "not necessarily a jar: declared non-JAR artifacts are supported"
            .getBytes(StandardCharsets.UTF_8);
    ModernInstallerArtifactResolver.ArtifactRequest request =
        request(sha1(bytes), Collections.emptyList(), false, true);
    Path target = root.resolve("libraries").resolve(request.getPath());
    Path legacy = write(root.resolve("cache").resolve(request.getPath()), new byte[] {1});
    Path maven = write(home.resolve(".m2/repository").resolve(request.getPath()), bytes);
    Path installer = archive("maven/" + request.getPath(), new byte[] {2});
    byte[] installerBefore = Files.readAllBytes(installer);

    assertTrue(resolver(installer).materialize(plan(request, false), REPORTER).isEmpty());
    assertArrayEquals(bytes, Files.readAllBytes(target));
    assertFalse(Files.isSameFile(maven, target));
    assertArrayEquals(bytes, Files.readAllBytes(maven));
    assertArrayEquals(new byte[] {1}, Files.readAllBytes(legacy));
    assertArrayEquals(installerBefore, Files.readAllBytes(installer));
  }

  @Test
  void downloadsTryOrderedCandidatesAndValidTargetsNeedNoNetwork() throws Exception {
    byte[] bytes = "verified download".getBytes(StandardCharsets.UTF_8);
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/first.jar", new byte[] {1});
      server.serve("/second.jar", bytes);
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(
              sha1(bytes),
              Arrays.asList(server.url("/first.jar"), server.url("/second.jar")),
              false,
              false);
      Path installer = archive("unrelated", new byte[] {2});
      ModernInstallerArtifactResolver resolver = resolver(installer);
      resolver.materialize(plan(request, false), REPORTER);
      assertArrayEquals(
          bytes, Files.readAllBytes(root.resolve("libraries").resolve(request.getPath())));
      assertTrue(
          server.requests.indexOf("/second.jar") > server.requests.lastIndexOf("/first.jar"));
      assertTrue(
          server.requests.stream()
              .allMatch(path -> path.equals("/first.jar") || path.equals("/second.jar")));
      server.requests.clear();
      Files.write(installer, new byte[] {3});
      resolver.materialize(plan(request, false), REPORTER);
      assertEquals(Collections.emptyList(), server.requests);
    }
  }

  @Test
  void missingMirrorFallsThroughOnceWithoutWarningBeforeVerifiedDownload() throws Exception {
    byte[] bytes = "verified fallback".getBytes(StandardCharsets.UTF_8);
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/available.jar", bytes);
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(
              sha1(bytes),
              Arrays.asList(server.url("/missing.jar"), server.url("/available.jar")),
              false,
              false);
      List<LogRecord> warnings = Collections.synchronizedList(new ArrayList<>());
      Handler handler =
          new Handler() {
            @Override
            public void publish(LogRecord record) {
              if (record.getLevel().intValue() >= Level.WARNING.intValue()
                  && record.getMessage() != null
                  && record.getMessage().contains(server.url(""))) {
                warnings.add(record);
              }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
          };
      Utils.getLogger().addHandler(handler);
      try {
        resolver(archive("unrelated", new byte[] {1})).materialize(plan(request, false), REPORTER);
      } finally {
        Utils.getLogger().removeHandler(handler);
      }

      assertArrayEquals(
          bytes, Files.readAllBytes(root.resolve("libraries").resolve(request.getPath())));
      assertEquals(Arrays.asList("/missing.jar", "/available.jar"), server.requests);
      assertTrue(warnings.isEmpty(), "A recoverable mirror miss must not emit a download warning");
    }
  }

  @Test
  void exhaustedMissingMirrorsStillFailWithAttemptedSources() throws Exception {
    try (RepositoryServer server = new RepositoryServer()) {
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(
              HASH,
              Arrays.asList(server.url("/first.jar"), server.url("/second.jar")),
              false,
              false);
      IOException failure =
          assertThrows(
              IOException.class,
              () ->
                  resolver(archive("unrelated", new byte[] {1}))
                      .materialize(plan(request, false), REPORTER));

      assertEquals(Arrays.asList("/first.jar", "/second.jar"), server.requests);
      assertTrue(failure.getMessage().contains(request.getCoordinate().toString()));
      assertTrue(failure.getMessage().contains(server.url("/first.jar")));
      assertTrue(failure.getMessage().contains(server.url("/second.jar")));
      assertTrue(
          Arrays.stream(failure.getSuppressed())
              .anyMatch(cause -> cause.getMessage().contains("HTTP 404")));
      assertFalse(Files.exists(root.resolve("libraries").resolve(request.getPath())));
    }
  }

  @Test
  void transientServerFailureStillRetriesAndVerifiesDownload() throws Exception {
    byte[] bytes = "verified after transient failure".getBytes(StandardCharsets.UTF_8);
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/retry.jar", bytes);
      server.failOnce("/retry.jar", 503);
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(sha1(bytes), Collections.singletonList(server.url("/retry.jar")), false, false);

      resolver(archive("unrelated", new byte[] {1})).materialize(plan(request, false), REPORTER);

      assertEquals(Arrays.asList("/retry.jar", "/retry.jar"), server.requests);
      assertArrayEquals(
          bytes, Files.readAllBytes(root.resolve("libraries").resolve(request.getPath())));
    }
  }

  @Test
  void failedIntegrityPreservesExistingTargetAndUnrelatedFiles() throws Exception {
    byte[] oldBytes = "old target".getBytes(StandardCharsets.UTF_8);
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/bad.jar", new byte[] {9});
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(
              sha1(new byte[] {8}),
              Collections.singletonList(server.url("/bad.jar")),
              false,
              false);
      Path target = write(root.resolve("libraries").resolve(request.getPath()), oldBytes);
      Path sentinel = write(target.getParent().resolve(".installer-other.tmp"), new byte[] {7});
      Path legacy = write(root.resolve("cache").resolve(request.getPath()), new byte[] {6});
      IOException failure =
          assertThrows(
              IOException.class,
              () ->
                  resolver(archive("maven/" + request.getPath(), new byte[] {5}))
                      .materialize(plan(request, false), REPORTER));
      assertTrue(failure.getMessage().contains(request.getCoordinate().toString()));
      assertTrue(failure.getMessage().contains(server.url("/bad.jar")));
      assertArrayEquals(oldBytes, Files.readAllBytes(target));
      assertArrayEquals(new byte[] {6}, Files.readAllBytes(legacy));
      assertArrayEquals(new byte[] {7}, Files.readAllBytes(sentinel));
      assertEquals(2, childCount(target.getParent()));
    }
  }

  @Test
  void onlyMissingDeclaredEmptyUrlsWithClientProcessorsCanDefer() throws Exception {
    ModernInstallerArtifactResolver resolver = resolver(archive("unrelated", new byte[] {1}));
    ModernInstallerArtifactResolver.ArtifactRequest empty =
        request(HASH, Collections.emptyList(), false, true);
    assertEquals(
        Collections.singletonList(empty), resolver.materialize(plan(empty, true), REPORTER));
    assertThrows(IOException.class, () -> resolver.materialize(plan(empty, false), REPORTER));
    ModernInstallerArtifactResolver.ArtifactRequest unavailable =
        request(HASH, Collections.emptyList(), false, false);
    assertThrows(IOException.class, () -> resolver.materialize(plan(unavailable, true), REPORTER));
    assertFalse(Files.exists(root.resolve("libraries").resolve(empty.getPath())));
  }

  @Test
  void zeroClientProfileInstallsExactEmbeddedDeclaredArtifact() throws Exception {
    byte[] bytes = new byte[] {1, 2, 3};
    ModernInstallerProfile profile = profile("[]", "[]", "[]");
    ModernInstallerArtifactResolver.ArtifactPlan plan =
        ModernInstallerArtifactResolver.plan(
            profile,
            Collections.singletonList(new Library("game:client:1", "", sha1(bytes), bytes.length)),
            runtime());
    ModernInstallerArtifactResolver.ArtifactRequest request = plan.getRequests().get(0);
    resolver(archive("maven/" + request.getPath(), bytes)).materialize(plan, REPORTER);
    assertArrayEquals(
        bytes, Files.readAllBytes(root.resolve("libraries").resolve(request.getPath())));
  }

  @Test
  void inferredSidecarsAreSavedAndAllowVerifiedOfflineReuseWithoutMutatingPlan() throws Exception {
    byte[] bytes = "inferred tool".getBytes(StandardCharsets.UTF_8);
    String digest = sha1(bytes);
    ModernInstallerArtifactResolver.ArtifactRequest request;
    ModernInstallerArtifactResolver resolver = resolver(archive("unrelated", new byte[] {1}));
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve(
          "/tool.jar.sha1",
          (digest.toUpperCase(java.util.Locale.ROOT) + "  *sample-1.jar\n")
              .getBytes(StandardCharsets.US_ASCII));
      server.serve("/tool.jar", bytes);
      request = request(null, Collections.singletonList(server.url("/tool.jar")), true, false);
      resolver.materialize(plan(request, true), REPORTER);
      assertEquals(Arrays.asList("/tool.jar.sha1", "/tool.jar"), server.requests);
      assertEquals(digest, resolver.verifiedSha1(request));
      assertNull(request.getSha1());
    }
    // The official endpoint is now unavailable; neither artifact nor sidecar needs another request.
    resolver.materialize(plan(request, true), REPORTER);
    assertArrayEquals(
        bytes, Files.readAllBytes(root.resolve("libraries").resolve(request.getPath())));
    assertEquals(digest, resolver.verifiedSha1(request));
  }

  @Test
  void malformedCachedSidecarRequiresOfficialDigestEvenWhenArtifactExists() throws Exception {
    byte[] bytes = "local tool".getBytes(StandardCharsets.UTF_8);
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/tool.jar.sha1", (sha1(bytes) + "\n").getBytes(StandardCharsets.US_ASCII));
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(null, Collections.singletonList(server.url("/tool.jar")), true, false);
      Path target = write(root.resolve("libraries").resolve(request.getPath()), bytes);
      Path sidecar = write(target.resolveSibling(target.getFileName() + ".sha1"), new byte[] {1});
      ModernInstallerArtifactResolver resolver = resolver(archive("unrelated", new byte[] {2}));
      assertThrows(IOException.class, () -> resolver.verifiedSha1(request));
      resolver.materialize(plan(request, true), REPORTER);
      assertEquals(Collections.singletonList("/tool.jar.sha1"), server.requests);
      assertEquals(sha1(bytes), resolver.verifiedSha1(request));
      assertEquals(
          sha1(bytes), new String(Files.readAllBytes(sidecar), StandardCharsets.US_ASCII).trim());
    }
  }

  @Test
  void missingOrMalformedOfficialSidecarNeverTrustsExistingArtifactOrDownloadsBytes()
      throws Exception {
    byte[] bytes = "untrusted tool".getBytes(StandardCharsets.UTF_8);
    try (RepositoryServer server = new RepositoryServer()) {
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(null, Collections.singletonList(server.url("/tool.jar")), true, false);
      Path target = write(root.resolve("libraries").resolve(request.getPath()), bytes);
      Path sidecar = target.resolveSibling(target.getFileName() + ".sha1");
      ModernInstallerArtifactResolver resolver = resolver(archive("unrelated", new byte[] {1}));
      assertThrows(IOException.class, () -> resolver.materialize(plan(request, true), REPORTER));
      assertFalse(Files.exists(sidecar));
      for (String invalid :
          Arrays.asList(
              sha1(bytes) + "00",
              sha1(bytes) + "  wrong.jar",
              sha1(bytes) + "\n" + sha1(bytes),
              "not a SHA-1")) {
        server.serve("/tool.jar.sha1", invalid.getBytes(StandardCharsets.US_ASCII));
        assertThrows(IOException.class, () -> resolver.materialize(plan(request, true), REPORTER));
        assertArrayEquals(bytes, Files.readAllBytes(target));
        assertFalse(Files.exists(sidecar));
      }
      assertTrue(server.requests.stream().allMatch(path -> path.equals("/tool.jar.sha1")));
      assertEquals(1, childCount(target.getParent()));
    }
  }

  @Test
  void invalidInferredDownloadDoesNotPublishFreshSidecarOrReplaceTarget() throws Exception {
    byte[] oldBytes = new byte[] {1};
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/tool.jar.sha1", sha1(new byte[] {2}).getBytes(StandardCharsets.US_ASCII));
      server.serve("/tool.jar", new byte[] {3});
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(null, Collections.singletonList(server.url("/tool.jar")), true, false);
      Path target = write(root.resolve("libraries").resolve(request.getPath()), oldBytes);
      Path sidecar = write(target.resolveSibling(target.getFileName() + ".sha1"), new byte[] {4});
      assertThrows(
          IOException.class,
          () ->
              resolver(archive("unrelated", new byte[] {5}))
                  .materialize(plan(request, true), REPORTER));
      assertArrayEquals(oldBytes, Files.readAllBytes(target));
      assertArrayEquals(new byte[] {4}, Files.readAllBytes(sidecar));
      assertEquals(2, childCount(target.getParent()));
    }
  }

  @Test
  void validInferredSidecarStillRequiresMatchingArtifactContent() throws Exception {
    byte[] bytes = new byte[] {1, 2, 3};
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/tool.jar", bytes);
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(null, Collections.singletonList(server.url("/tool.jar")), true, false);
      Path target = write(root.resolve("libraries").resolve(request.getPath()), new byte[] {4});
      write(
          target.resolveSibling(target.getFileName() + ".sha1"),
          sha1(bytes).getBytes(StandardCharsets.US_ASCII));
      resolver(archive("unrelated", new byte[] {5})).materialize(plan(request, true), REPORTER);
      assertEquals(Collections.singletonList("/tool.jar"), server.requests);
      assertArrayEquals(bytes, Files.readAllBytes(target));
    }
  }

  @Test
  void hashlessDownloadUsesEtagMd5InsteadOfAcceptingAnyZip() throws Exception {
    byte[] expected = archiveBytes("correct", "expected");
    byte[] wrong = archiveBytes("wrong", "wrong");
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/tool.jar", wrong);
      server.etag = CryptoUtils.getMD5(write(temp.resolve("expected.jar"), expected));
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(null, Collections.singletonList(server.url("/tool.jar")), false, false);
      ModernInstallerArtifactResolver resolver = resolver(archive("unrelated", new byte[] {1}));
      Path target = root.resolve("libraries").resolve(request.getPath());
      assertThrows(IOException.class, () -> resolver.materialize(plan(request, false), REPORTER));
      assertFalse(Files.exists(target));
      server.serve("/tool.jar", expected);
      resolver.materialize(plan(request, false), REPORTER);
      assertArrayEquals(expected, Files.readAllBytes(target));
    }
  }

  @Test
  void cancellationDuringDownloadCannotPublishAndCleansOnlyOwnedStages() throws Exception {
    byte[] bytes = new byte[] {1, 2, 3};
    AtomicBoolean cancelled = new AtomicBoolean();
    try (RepositoryServer server = new RepositoryServer()) {
      server.serve("/tool.jar", bytes);
      ModernInstallerArtifactResolver.ArtifactRequest request =
          request(sha1(bytes), Collections.singletonList(server.url("/tool.jar")), false, false);
      Path target = write(root.resolve("libraries").resolve(request.getPath()), new byte[] {4});
      ModernInstallerArtifactResolver resolver =
          new ModernInstallerArtifactResolver(
              root, archive("unrelated", new byte[] {5}), cancelled::get);
      NodeProgressReporter cancelAtCompletion =
          new NodeProgressReporter() {
            public void updateNodeProgress(float percent) {
              if (percent == 100.0f) cancelled.set(true);
            }

            public void updateCurrentItem(String label, CurrentItemMode mode, Float percent) {}
          };
      try {
        assertThrows(
            InterruptedException.class,
            () -> resolver.materialize(plan(request, false), cancelAtCompletion));
      } finally {
        Thread.interrupted();
      }
      assertArrayEquals(new byte[] {4}, Files.readAllBytes(target));
      assertEquals(1, childCount(target.getParent()));
    }
  }

  @Test
  void interruptionBeforeAcquisitionDoesNotTouchRepository() throws Exception {
    ModernInstallerArtifactResolver.ArtifactRequest request =
        request(HASH, Collections.emptyList(), false, true);
    ModernInstallerArtifactResolver resolver = resolver(archive("unrelated", new byte[] {1}));
    Thread.currentThread().interrupt();
    try {
      assertThrows(
          InterruptedException.class, () -> resolver.materialize(plan(request, true), REPORTER));
    } finally {
      Thread.interrupted();
    }
    assertFalse(Files.exists(root.resolve("libraries")));
  }

  @Test
  void escapingArtifactAndSidecarSymlinksFailBeforeAccessingExternalFiles() throws Exception {
    byte[] bytes = new byte[] {1, 2, 3};
    ModernInstallerArtifactResolver.ArtifactRequest request =
        request(sha1(bytes), Collections.emptyList(), false, true);
    Path target = root.resolve("libraries").resolve(request.getPath());
    Path outside = write(temp.resolve("outside.jar"), bytes);
    Files.createDirectories(target.getParent());
    symlink(target, outside);
    ModernInstallerArtifactResolver resolver =
        resolver(archive("maven/" + request.getPath(), bytes));
    assertThrows(IOException.class, () -> resolver.materialize(plan(request, false), REPORTER));
    assertTrue(Files.isSymbolicLink(target));
    assertArrayEquals(bytes, Files.readAllBytes(outside));
    Files.delete(target);
    write(target, bytes);
    Path outsideSidecar =
        write(temp.resolve("outside.sha1"), sha1(bytes).getBytes(StandardCharsets.US_ASCII));
    symlink(target.resolveSibling(target.getFileName() + ".sha1"), outsideSidecar);
    ModernInstallerArtifactResolver.ArtifactRequest inferred =
        request(null, Collections.emptyList(), true, false);
    assertThrows(IOException.class, () -> resolver.materialize(plan(inferred, true), REPORTER));
    assertThrows(IOException.class, () -> resolver.verifiedSha1(inferred));
    assertArrayEquals(
        sha1(bytes).getBytes(StandardCharsets.US_ASCII), Files.readAllBytes(outsideSidecar));
  }

  @Test
  void configuredRootSymlinkIsAllowedButEscapingLegacySymlinkIsNot() throws Exception {
    byte[] bytes = new byte[] {1, 2, 3};
    ModernInstallerArtifactResolver.ArtifactRequest request =
        request(sha1(bytes), Collections.emptyList(), false, true);
    Path rootAlias = symlink(temp.resolve("root-alias"), root);
    Path installer = archive("maven/" + request.getPath(), bytes);
    new ModernInstallerArtifactResolver(rootAlias, installer, () -> false)
        .materialize(plan(request, false), REPORTER);
    assertArrayEquals(
        bytes, Files.readAllBytes(root.resolve("libraries").resolve(request.getPath())));
    Files.delete(root.resolve("libraries").resolve(request.getPath()));
    Files.write(installer, archiveBytes("unrelated", "unrelated"));
    Path legacy = root.resolve("cache").resolve(request.getPath());
    Files.createDirectories(legacy.getParent());
    Path outside = write(temp.resolve("outside.jar"), bytes);
    symlink(legacy, outside);
    assertThrows(
        IOException.class, () -> resolver(installer).materialize(plan(request, false), REPORTER));
    assertArrayEquals(bytes, Files.readAllBytes(outside));
  }

  private ModernInstallerArtifactResolver resolver(Path installer) {
    return new ModernInstallerArtifactResolver(root, installer, () -> false);
  }

  private static ModernInstallerArtifactResolver.ArtifactRequest request(
      String sha1, List<String> urls, boolean inferred, boolean emptyUrl) {
    MavenCoordinate coordinate = MavenCoordinate.parse("test.artifacts:sample:1");
    return new ModernInstallerArtifactResolver.ArtifactRequest(
        coordinate, coordinate.getPath(), sha1, 0, urls, !inferred, inferred, emptyUrl);
  }

  private static ModernInstallerArtifactResolver.ArtifactPlan plan(
      ModernInstallerArtifactResolver.ArtifactRequest request, boolean clientProcessors) {
    Map<String, ModernInstallerArtifactResolver.ArtifactRequest> requests = new LinkedHashMap<>();
    requests.put(request.getPath(), request);
    return new ModernInstallerArtifactResolver.ArtifactPlan(requests, clientProcessors);
  }

  private Path archive(String member, byte[] content) throws IOException {
    Path path = Files.createTempFile(temp, "artifacts-", ".jar");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
      zip.putNextEntry(new ZipEntry(member));
      zip.write(content);
      zip.closeEntry();
    }
    return path;
  }

  private static byte[] archiveBytes(String member, String content) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry(member));
      zip.write(content.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return bytes.toByteArray();
  }

  private String sha1(byte[] bytes) throws IOException {
    return CryptoUtils.getSHA1(write(Files.createTempFile(temp, "digest-", ".bin"), bytes));
  }

  private static Path write(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    return Files.write(path, bytes);
  }

  private static long childCount(Path directory) throws IOException {
    try (java.util.stream.Stream<Path> children = Files.list(directory)) {
      return children.count();
    }
  }

  private static Path symlink(Path link, Path target) throws IOException {
    try {
      return Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | FileSystemException unavailable) {
      org.junit.jupiter.api.Assumptions.assumeTrue(
          false, "Symbolic links unavailable: " + unavailable);
      throw new AssertionError("unreachable", unavailable);
    }
  }

  private static final class RepositoryServer implements AutoCloseable {
    private final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    private final Map<String, byte[]> responses = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> failures = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile String etag;

    private RepositoryServer() throws IOException {
      server.createContext(
          "/",
          exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.add(path);
            byte[] response = responses.get(path);
            Integer failure = failures.remove(path);
            if (failure != null) response = null;
            try {
              if (etag != null) exchange.getResponseHeaders().add("ETag", "\"" + etag + "\"");
              exchange.sendResponseHeaders(
                  failure != null ? failure : response == null ? 404 : 200,
                  response == null ? -1 : response.length);
              if (response != null) exchange.getResponseBody().write(response);
            } finally {
              exchange.close();
            }
          });
      server.start();
    }

    private void serve(String path, byte[] bytes) {
      responses.put(path, bytes);
    }

    private void failOnce(String path, int status) {
      failures.put(path, status);
    }

    private String url(String path) {
      return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }

  private ModernInstallerProfile profile(String libraries, String data, String processors)
      throws Exception {
    Path installer = Files.createTempFile(temp, "installer-", ".jar");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(installer))) {
      zip.putNextEntry(new ZipEntry("install_profile.json"));
      zip.write(
          ("{\"spec\":1,\"profile\":\"test\",\"version\":\"loader\",\"minecraft\":\"1.20.1\","
                  + "\"json\":\"/version.json\",\"libraries\":"
                  + libraries
                  + ",\"data\":"
                  + data
                  + ",\"processors\":"
                  + processors
                  + "}")
              .getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("version.json"));
      zip.write(
          "{\"id\":\"loader\",\"inheritsFrom\":\"1.20.1\",\"mainClass\":\"example.Main\",\"arguments\":{},\"libraries\":[]}"
              .getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return ModernInstallerProfileReader.read(installer);
  }

  private static String declaration(String coordinate, String url, String sha1, long size) {
    return "{\"name\":\""
        + coordinate
        + "\",\"downloads\":{\"artifact\":{\"url\":\""
        + url
        + "\",\"sha1\":\""
        + sha1
        + "\",\"size\":"
        + size
        + "}}}";
  }

  private static IJavaRuntime runtime() {
    return new IJavaRuntime() {
      public File getExecutableFile() {
        return new File(System.getProperty("java.home"), "bin/java");
      }

      public String getVersion() {
        return System.getProperty("java.version");
      }

      public String getVendor() {
        return "test";
      }

      public String getOsArch() {
        return System.getProperty("os.arch");
      }

      public String getBitness() {
        return "64";
      }

      public boolean is64Bit() {
        return true;
      }

      public boolean isValid() {
        return true;
      }
    };
  }
}
