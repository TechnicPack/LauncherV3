package net.technicpack.solder.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.technicpack.launcher.io.LauncherFileSystem;
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

  private static class CountingSolderPackApi implements ISolderPackApi {
    int packInfoPulls = 0;
    int buildPulls = 0;

    @Override
    public String getMirrorUrl() {
      return "https://mirror.example.com/";
    }

    @Override
    public SolderPackInfo getPackInfoForBulk() {
      packInfoPulls++;
      return Utils.getGson().fromJson("{\"name\":\"test-pack\"}", SolderPackInfo.class);
    }

    @Override
    public SolderPackInfo getPackInfo() {
      return getPackInfoForBulk();
    }

    @Override
    public Modpack getPackBuild(String build) {
      buildPulls++;
      return Utils.getGson().fromJson("{\"name\":\"test-pack\"}", Modpack.class);
    }
  }

  @Test
  void invalidateCacheForcesNextPackInfoPull() throws RestfulAPIException {
    CountingSolderPackApi inner = new CountingSolderPackApi();
    CachedSolderPackApi api =
        new CachedSolderPackApi(
            new LauncherFileSystem(tempDir.resolve("launcher-root")),
            inner,
            CACHE_SECONDS,
            "test-pack");

    api.getPackInfo();
    api.getPackInfo();
    assertEquals(1, inner.packInfoPulls, "second call within TTL must be served from cache");

    api.invalidateCache();

    api.getPackInfo();
    assertEquals(2, inner.packInfoPulls, "call after invalidateCache must hit the inner API");
  }

  @Test
  void invalidateCacheForcesNextBuildPull() throws Exception {
    CountingSolderPackApi inner = new CountingSolderPackApi();
    CachedSolderPackApi api =
        new CachedSolderPackApi(
            new LauncherFileSystem(tempDir.resolve("launcher-root")),
            inner,
            CACHE_SECONDS,
            "test-pack");

    api.getPackBuild("1.0.1");
    api.getPackBuild("1.0.1");
    assertEquals(1, inner.buildPulls, "second build fetch within TTL must be served from cache");

    api.invalidateCache();

    api.getPackBuild("1.0.1");
    assertEquals(2, inner.buildPulls, "build fetch after invalidateCache must hit the inner API");
  }
}
