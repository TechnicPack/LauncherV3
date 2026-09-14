package net.technicpack.platform.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModpackCachePlatformApiTest {
  @TempDir Path tempDir;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"name\":null}",
        "{\"name\":\"   \"}",
        "{\"name\":\"other-pack\"}",
        "{\"name\":\"../foreign-pack\"}"
      })
  void invalidInnerIdentityNeverReachesMemoryOrDiskCache(String json) throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir);
    MutablePlatformApi inner = new MutablePlatformApi();
    inner.response = pack(json);
    ModpackCachePlatformApi api = new ModpackCachePlatformApi(inner, 0, fileSystem);

    assertThrows(RestfulAPIException.class, () -> api.getPlatformPackInfoForBulk("test-pack"));
    try (var files = Files.list(fileSystem.getPackAssetsDirectory())) {
      assertEquals(0, files.count());
    }
    assertFalse(Files.exists(fileSystem.getPackAssetsDirectory().resolve("../foreign-pack")));

    inner.response = pack("{\"name\":\"test-pack\",\"version\":\"recovered\"}");
    assertEquals("recovered", api.getPlatformPackInfoForBulk("test-pack").getRecommended());
    assertEquals("recovered", api.getPlatformPackInfo("test-pack").getRecommended());
    PlatformPackInfo saved =
        pack(Files.readString(fileSystem.getPackAssetsDirectory().resolve("test-pack/cache.json")));
    assertEquals("test-pack", saved.getName());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"{}", "{\"name\":null}", "{\"name\":\"   \"}", "{\"name\":\"other-pack\"}"})
  void invalidDiskIdentityIsNotPublishedOrRemembered(String json) throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir);
    Path cacheFile = fileSystem.getPackAssetsDirectory().resolve("test-pack/cache.json");
    Files.createDirectories(cacheFile.getParent());
    Files.writeString(cacheFile, json);
    MutablePlatformApi inner = new MutablePlatformApi();
    inner.offline = true;
    ModpackCachePlatformApi api = new ModpackCachePlatformApi(inner, 0, fileSystem);

    assertThrows(RestfulAPIException.class, () -> api.getPlatformPackInfoForBulk("test-pack"));

    Files.writeString(cacheFile, "{\"name\":\"test-pack\",\"version\":\"disk\"}");
    PlatformPackInfo disk = api.getPlatformPackInfoForBulk("test-pack");
    assertEquals("test-pack", disk.getName());
    assertEquals("disk", disk.getRecommended());
    assertTrue(disk.isLocal());

    inner.offline = false;
    inner.response = pack("{\"name\":\"test-pack\",\"version\":\"live\"}");
    PlatformPackInfo recovered = api.getPlatformPackInfo("test-pack");
    assertEquals("live", recovered.getRecommended());
    assertFalse(recovered.isLocal());
    assertTrue(disk.isLocal());
  }

  @Test
  void offlineFallbackDoesNotChangePublishedOnlineSnapshotAndRecoveryDoesNotChangeOfflineSnapshot()
      throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir);
    MutablePlatformApi inner = new MutablePlatformApi();
    inner.response =
        pack("{\"name\":\"test-pack\",\"displayName\":\"Test Pack\",\"version\":\"1.0\"}");
    ModpackCachePlatformApi api = new ModpackCachePlatformApi(inner, 0, fileSystem);
    PlatformPackInfo online = api.getPlatformPackInfo("test-pack");
    assertFalse(online.isLocal());

    inner.offline = true;
    PlatformPackInfo offline = api.getPlatformPackInfo("test-pack");
    assertNotNull(offline);
    assertTrue(offline.isLocal());
    assertFalse(online.isLocal());
    assertEquals("Test Pack", offline.getDisplayName());
    assertEquals("1.0", offline.getRecommended());
    assertTrue(api.getPlatformPackInfoForBulk("test-pack").isLocal());

    inner.offline = false;
    inner.response = pack("{\"name\":\"test-pack\",\"version\":\"2.0\"}");
    PlatformPackInfo recovered = api.getPlatformPackInfo("test-pack");
    assertFalse(recovered.isLocal());
    assertEquals("2.0", recovered.getRecommended());
    assertFalse(api.getPlatformPackInfoForBulk("test-pack").isLocal());
    assertTrue(offline.isLocal());
    assertEquals("1.0", offline.getRecommended());
    assertFalse(online.isLocal());
  }

  private static PlatformPackInfo pack(String json) {
    return Utils.getGson().fromJson(json, PlatformPackInfo.class);
  }

  private static class MutablePlatformApi extends HttpPlatformApi {
    PlatformPackInfo response;
    boolean offline;

    MutablePlatformApi() {
      super("https://platform.example/", "test");
    }

    @Override
    public PlatformPackInfo getPlatformPackInfoForBulk(String slug) throws RestfulAPIException {
      if (offline) throw new RestfulAPIException("Platform is offline");
      return response;
    }
  }
}
