package net.technicpack.solder.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.http.HttpSolderApi;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CachedSolderPackApiTest {

  private static final int CACHE_SECONDS = 3600;
  private static final String ROOT_A = "https://solder-a.example/api/";
  private static final String ROOT_B = "https://solder-b.example/api/";

  @TempDir Path tempDir;

  @ParameterizedTest
  @ValueSource(strings = {"\"java-runtime-delta\"", "null", "absent"})
  void cachedBuildRetainsOptionalRuntimeWhenSolderGoesOffline(String runtimeJson) throws Exception {
    String runtimeField = runtimeJson.equals("absent") ? "" : ",\"java_runtime\":" + runtimeJson;
    byte[] response =
        ("{\"minecraft\":\"1.20.1\",\"java\":\"17\",\"memory\":\"4096\",\"mods\":[]"
                + runtimeField
                + "}")
            .getBytes(StandardCharsets.UTF_8);
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    CachedSolderPackApi api;
    try {
      server.createContext(
          "/api/modpack/test-pack/1.0.1",
          exchange -> {
            try (exchange) {
              exchange.getResponseHeaders().set("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, response.length);
              exchange.getResponseBody().write(response);
            }
          });
      server.start();
      ISolderPackApi inner =
          new HttpSolderApi(slug -> null)
              .getSolderPack(
                  "http://127.0.0.1:" + server.getAddress().getPort() + "/api/",
                  "test-pack",
                  "https://mirror.example.com/");
      api =
          new CachedSolderPackApi(
              new LauncherFileSystem(tempDir.resolve("launcher-root")),
              inner,
              CACHE_SECONDS,
              "http://127.0.0.1:" + server.getAddress().getPort() + "/api/",
              "test-pack");
      api.getPackBuild("1.0.1");
    } finally {
      server.stop(0);
    }

    Modpack cachedBuild = api.getPackBuild("1.0.1");

    if (runtimeJson.startsWith("\"")) {
      assertEquals("java-runtime-delta", cachedBuild.getJavaRuntime());
    } else {
      assertNull(cachedBuild.getJavaRuntime());
    }
    assertEquals("1.20.1", cachedBuild.getGameVersion());
    assertEquals("17", cachedBuild.getJava());
    assertEquals("4096", cachedBuild.getMemory());
  }

  @Test
  void packInfoUsesBuildCacheAndInvalidationRevokesCachedPrivateBuilds() throws Exception {
    MutableSolderPackApi inner = new MutableSolderPackApi();
    CachedSolderPackApi api = cache(inner, ROOT_A, CACHE_SECONDS);
    SolderPackInfo live = api.getPackInfo();
    assertEquals("1.20.1", live.getModpack("private").getGameVersion());

    inner.offline = true;
    assertEquals("1.20.1", live.getModpack("private").getGameVersion());
    api.invalidateCache();

    assertThrows(BuildInaccessibleException.class, () -> live.getModpack("private"));
    assertThrows(RestfulAPIException.class, api::getPackInfo);
    assertThrows(RestfulAPIException.class, api::getPackInfoForBulk);
    assertThrows(
        RestfulAPIException.class, () -> cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk());

    inner.offline = false;
    inner.info = pack("{\"name\":\"test-pack\",\"builds\":[]}");
    assertEquals(java.util.List.of(), api.getPackInfo().getBuilds());
    inner.offline = true;
    assertEquals(
        java.util.List.of(), cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk().getBuilds());
  }

  @Test
  void offlineFallbackAndRecoveryDoNotMutatePublishedSnapshots() throws Exception {
    MutableSolderPackApi inner = new MutableSolderPackApi();
    CachedSolderPackApi api = cache(inner, ROOT_A, 0);
    SolderPackInfo live = api.getPackInfo();
    assertFalse(live.isLocal());

    inner.offline = true;
    SolderPackInfo offline = api.getPackInfo();
    assertTrue(offline.isLocal());
    assertEquals(live.getBuilds(), offline.getBuilds());
    assertNotSame(live, offline);
    assertFalse(live.isLocal());
    assertFalse(inner.info.isLocal());

    inner.offline = false;
    SolderPackInfo recovered = api.getPackInfo();
    assertFalse(recovered.isLocal());
    assertTrue(offline.isLocal());
    assertFalse(live.isLocal());
  }

  @Test
  void failedRefreshKeepsDiskFallbackForTheCacheInterval() throws Exception {
    // a2c7eafe: a failed request must not bypass the cache interval and repeatedly hit Solder.
    MutableSolderPackApi inner = new MutableSolderPackApi();
    cache(inner, ROOT_A, CACHE_SECONDS).getPackInfo();
    CachedSolderPackApi restarted = cache(inner, ROOT_A, CACHE_SECONDS);
    assertTrue(restarted.getPackInfoForBulk().isLocal());
    inner.offline = true;
    SolderPackInfo offline = restarted.getPackInfo();
    assertTrue(offline.isLocal());

    inner.offline = false;
    inner.info = pack("{\"name\":\"test-pack\",\"builds\":[\"not-yet-refreshed\"]}");
    SolderPackInfo stillCached = restarted.getPackInfo();

    assertTrue(stillCached.isLocal());
    assertEquals(java.util.List.of("private"), stillCached.getBuilds());
  }

  @Test
  void failedFirstFetchDoesNotCacheANullCatalog() throws Exception {
    // 5f268990: the cache interval only applies when there is usable metadata to return.
    MutableSolderPackApi inner = new MutableSolderPackApi();
    inner.offline = true;
    CachedSolderPackApi api = cache(inner, ROOT_A, CACHE_SECONDS);
    assertThrows(RestfulAPIException.class, api::getPackInfo);

    inner.offline = false;
    SolderPackInfo recovered = api.getPackInfo();

    assertFalse(recovered.isLocal());
    assertEquals(java.util.List.of("private"), recovered.getBuilds());
  }

  @Test
  void endpointChangeCannotReuseAnotherRootsDiskCatalog() throws Exception {
    MutableSolderPackApi sourceA = new MutableSolderPackApi();
    cache(sourceA, ROOT_A, CACHE_SECONDS).getPackInfo();
    MutableSolderPackApi sourceB = new MutableSolderPackApi();
    sourceB.offline = true;
    assertThrows(
        RestfulAPIException.class,
        () -> cache(sourceB, ROOT_B, CACHE_SECONDS).getPackInfoForBulk());

    sourceB.offline = false;
    sourceB.info = pack("{\"name\":\"test-pack\",\"builds\":[\"source-b\"]}");
    cache(sourceB, ROOT_B, CACHE_SECONDS).getPackInfo();
    sourceA.offline = true;
    sourceB.offline = true;

    SolderPackInfo fromA = cache(sourceA, ROOT_A, CACHE_SECONDS).getPackInfoForBulk();
    SolderPackInfo fromB = cache(sourceB, ROOT_B, CACHE_SECONDS).getPackInfoForBulk();
    assertEquals(java.util.List.of("private"), fromA.getBuilds());
    assertEquals(java.util.List.of("source-b"), fromB.getBuilds());
    assertTrue(fromA.isLocal());
    assertTrue(fromB.isLocal());
  }

  @Test
  void diskCatalogBuildsUseCachingPackApi() throws Exception {
    MutableSolderPackApi inner = new MutableSolderPackApi();
    cache(inner, ROOT_A, CACHE_SECONDS).getPackInfo();
    SolderPackInfo disk = cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk();
    assertTrue(disk.isLocal());
    assertEquals("1.20.1", disk.getModpack("private").getGameVersion());
    inner.offline = true;
    assertEquals("1.20.1", disk.getModpack("private").getGameVersion());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "null",
        "{}",
        "{\"name\":\"other-pack\",\"builds\":[]}",
        "{\"name\":\" \",\"builds\":[]}",
        "{\"name\":\"test-pack\"}",
        "{\"name\":\"test-pack\",\"builds\":[null]}",
        "{\"name\":\"test-pack\",\"builds\":[\" \"]}",
        "{\"name\":\"test-pack\",\"builds\":[],\"error\":\"denied\"}"
      })
  void malformedInnerAndDiskCatalogsAreRejected(String json) throws Exception {
    MutableSolderPackApi inner = new MutableSolderPackApi();
    cache(inner, ROOT_A, CACHE_SECONDS).getPackInfo();
    Path path = diskCatalog();
    JsonObject envelope = Utils.getGson().fromJson(Files.readString(path), JsonObject.class);
    envelope.add("packInfo", Utils.getGson().fromJson(json, JsonElement.class));
    Files.writeString(path, Utils.getGson().toJson(envelope));
    inner.info = pack(json);

    assertThrows(
        RestfulAPIException.class, () -> cache(inner, ROOT_A, CACHE_SECONDS).getPackInfo());
    inner.offline = true;
    assertThrows(
        RestfulAPIException.class, () -> cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk());
  }

  @Test
  void diskCatalogRequiresMatchingRootProvenance() throws Exception {
    MutableSolderPackApi inner = new MutableSolderPackApi();
    cache(inner, ROOT_A, CACHE_SECONDS).getPackInfo();
    Path path = diskCatalog();
    JsonObject envelope = Utils.getGson().fromJson(Files.readString(path), JsonObject.class);
    envelope.addProperty("solderRoot", ROOT_B);
    Files.writeString(path, Utils.getGson().toJson(envelope));
    inner.offline = true;
    assertThrows(
        RestfulAPIException.class, () -> cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk());

    envelope.remove("solderRoot");
    Files.writeString(path, Utils.getGson().toJson(envelope));
    assertThrows(
        RestfulAPIException.class, () -> cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk());
  }

  @Test
  void legacyDiskCatalogWithoutProvenanceIsIgnored() throws Exception {
    Path directory = fileSystem().getPackAssetsDirectory().resolve("test-pack");
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve("soldercache.json"),
        "{\"name\":\"test-pack\",\"builds\":[\"legacy-private\"]}");
    MutableSolderPackApi inner = new MutableSolderPackApi();
    inner.offline = true;
    assertThrows(
        RestfulAPIException.class, () -> cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk());
  }

  @Test
  void malformedRefreshFallsBackLocallyWithoutReplacingValidCatalog() throws Exception {
    MutableSolderPackApi inner = new MutableSolderPackApi();
    CachedSolderPackApi api = cache(inner, ROOT_A, 0);
    SolderPackInfo live = api.getPackInfo();
    inner.info = pack("{\"name\":\"other-pack\",\"builds\":[\"wrong-source\"]}");

    SolderPackInfo fallback = api.getPackInfo();
    assertEquals(java.util.List.of("private"), fallback.getBuilds());
    assertTrue(fallback.isLocal());
    assertFalse(live.isLocal());
    assertEquals(
        java.util.List.of("private"),
        cache(inner, ROOT_A, CACHE_SECONDS).getPackInfoForBulk().getBuilds());
  }

  private CachedSolderPackApi cache(MutableSolderPackApi inner, String root, int seconds) {
    return new CachedSolderPackApi(fileSystem(), inner, seconds, root, "test-pack");
  }

  private LauncherFileSystem fileSystem() {
    return new LauncherFileSystem(tempDir.resolve("launcher-root"));
  }

  private Path diskCatalog() throws Exception {
    try (var files = Files.list(fileSystem().getPackAssetsDirectory().resolve("test-pack"))) {
      return files
          .filter(path -> path.getFileName().toString().startsWith("soldercache-"))
          .findFirst()
          .orElseThrow();
    }
  }

  private static SolderPackInfo pack(String json) {
    return Utils.getGson().fromJson(json, SolderPackInfo.class);
  }

  private static class MutableSolderPackApi implements ISolderPackApi {
    private SolderPackInfo info = pack("{\"name\":\"test-pack\",\"builds\":[\"private\"]}");
    private boolean offline;

    @Override
    public String getMirrorUrl() {
      return "https://mirror.example.com/";
    }

    @Override
    public SolderPackInfo getPackInfoForBulk() throws RestfulAPIException {
      return getPackInfo();
    }

    @Override
    public SolderPackInfo getPackInfo() throws RestfulAPIException {
      if (offline) {
        throw new RestfulAPIException("Solder unavailable");
      }
      return info;
    }

    @Override
    public Modpack getPackBuild(String build) throws BuildInaccessibleException {
      if (offline) {
        throw new BuildInaccessibleException("test-pack", build);
      }
      return Utils.getGson().fromJson("{\"minecraft\":\"1.20.1\",\"mods\":[]}", Modpack.class);
    }
  }
}
