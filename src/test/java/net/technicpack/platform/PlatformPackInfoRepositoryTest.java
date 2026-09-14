package net.technicpack.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.cache.ModpackCachePlatformApi;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.cache.CachedSolderApi;
import net.technicpack.solder.http.HttpSolderApi;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlatformPackInfoRepositoryTest {
  @TempDir Path tempDir;
  private final Map<String, String> responses = new ConcurrentHashMap<>();
  private HttpServer server;
  private String root;
  private PlatformPackInfoRepository repository;

  @BeforeEach
  void startApis() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try (exchange) {
            String response = responses.get(exchange.getRequestURI().getPath());
            byte[] body = (response == null ? "{}" : response).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(response == null ? 503 : 200, body.length);
            exchange.getResponseBody().write(body);
          }
        });
    server.start();
    root = "http://127.0.0.1:" + server.getAddress().getPort();
    repository =
        new PlatformPackInfoRepository(
            new HttpPlatformApi(root + "/platform/", "999"), new HttpSolderApi(slug -> null));
    platform("/solder-a/", "Platform title");
    solder("/solder-a/", "solder-1", "1.20.1");
  }

  @AfterEach
  void stopApis() {
    server.stop(0);
  }

  @Test
  void bulkAndRefreshUsePlatformPresentationAndSolderInstallationData() throws Exception {
    PackInfo bulk = repository.getPackInfo(new InstalledPack("testpack", "recommended"));
    PackInfo refreshed = repository.refreshPackInfo("testpack", false);
    for (PackInfo info : new PackInfo[] {bulk, refreshed}) {
      assertEquals("Platform title", info.getDisplayName());
      assertEquals("solder-1", info.getRecommended());
      assertEquals("solder-1", info.getLatest());
      assertEquals(Collections.singletonList("solder-1"), info.getBuilds());
      assertEquals("1.20.1", info.getModpack("solder-1").getGameVersion());
      assertFalse(info.isLocal());
    }
  }

  @Test
  void unavailableSolderReplacesOldBuildsInsteadOfMergingThemBack() throws Exception {
    ModpackModel model = model();
    responses.remove("/solder-a/modpack/testpack");
    platform("/solder-a/", "Updated Platform title");

    model.setPackInfo(repository.refreshPackInfo("testpack", false));

    assertEquals("Updated Platform title", model.getDisplayName());
    assertTrue(model.isLocalOnly());
    assertTrue(model.getPackInfo().isComplete());
    assertTrue(model.getBuilds().isEmpty());
    assertNull(model.getPackInfo().getRecommended());
    assertThrows(
        BuildInaccessibleException.class, () -> model.getPackInfo().getModpack("solder-1"));
  }

  @Test
  void changingSolderEndpointCannotReuseOldEndpointsBuilds() throws Exception {
    ModpackModel model = model();
    platform("/solder-b/", "Moved pack");
    responses.put("/solder-b/modpack", "{\"mirror_url\":\"" + root + "/mirror/\"}");

    model.setPackInfo(repository.refreshPackInfo("testpack", false));
    assertTrue(model.getBuilds().isEmpty());
    assertTrue(model.isLocalOnly());

    solder("/solder-b/", "solder-2", "1.21.1");
    model.setPackInfo(repository.refreshPackInfo("testpack", false));
    assertEquals(Collections.singletonList("solder-2"), model.getBuilds());
    assertEquals("1.21.1", model.getPackInfo().getModpack("solder-2").getGameVersion());
    assertFalse(model.isLocalOnly());
  }

  @Test
  void removingSolderSwitchesBothCatalogAndInstallationBackToPlatform() throws Exception {
    ModpackModel model = model();
    platform(null, "ZIP pack");

    model.setPackInfo(repository.refreshPackInfo("testpack", false));

    assertFalse(model.getPackInfo().hasSolder());
    assertEquals(Collections.singletonList("platform-zip"), model.getBuilds());
    assertEquals("platform-zip", model.getPackInfo().getLatest());
    assertEquals("1.12.2", model.getPackInfo().getModpack("platform-zip").getGameVersion());
  }

  @Test
  void lateDiscoveryCannotOverwriteResolvedPresentationOrPrivateBuilds() throws Exception {
    ModpackModel model = model();
    SolderPackInfo publicInfo =
        Utils.getGson()
            .fromJson(
                "{\"name\":\"testpack\",\"display_name\":\"Public discovery\","
                    + "\"builds\":[],\"recommended\":null}",
                SolderPackInfo.class);

    model.setPackInfo(publicInfo);

    assertEquals("Platform title", model.getDisplayName());
    assertEquals(Collections.singletonList("solder-1"), model.getBuilds());
    assertFalse(model.isLocalOnly());
  }

  @Test
  void modelRejectsForeignMetadataRatherThanInstallingItUnderExistingIdentity() throws Exception {
    ModpackModel model = model();
    PlatformPackInfo otherPack =
        Utils.getGson()
            .fromJson(
                "{\"name\":\"otherpack\",\"version\":\"foreign\",\"minecraft\":\"1.7.10\"}",
                PlatformPackInfo.class);

    model.setPackInfo(otherPack);

    assertEquals("testpack", model.getName());
    assertEquals("1.20.1", model.getPackInfo().getModpack("solder-1").getGameVersion());
  }

  @Test
  void unreachableSolderDoesNotAdvertisePlatformVersionAsAnUpdate() throws Exception {
    // Regression from 519cefd7: Platform's ZIP version is unrelated to Solder builds.
    responses.remove("/solder-a/modpack/testpack");
    ModpackModel model = installedModel(repository.refreshPackInfo("testpack", false));

    assertTrue(model.isLocalOnly());
    assertFalse(model.hasRecommendedUpdate());

    platform(null, "ZIP pack");
    model.setPackInfo(repository.refreshPackInfo("testpack", false));
    assertTrue(model.hasRecommendedUpdate());
  }

  @Test
  void cachedSolderRemainsOfflineWithoutInventingAnUpdateAndRecovers() throws Exception {
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher"));
    PlatformPackInfoRepository cachedRepository =
        new PlatformPackInfoRepository(
            new HttpPlatformApi(root + "/platform/", "999"),
            new CachedSolderApi(fileSystem, new HttpSolderApi(slug -> null), 0));
    ModpackModel model = installedModel(cachedRepository.refreshPackInfo("testpack", false));
    assertFalse(model.isLocalOnly());
    assertFalse(model.hasRecommendedUpdate());

    responses.remove("/solder-a/modpack/testpack");
    model.setPackInfo(cachedRepository.refreshPackInfo("testpack", false));

    assertTrue(model.isLocalOnly());
    assertEquals(Collections.singletonList("solder-1"), model.getBuilds());
    assertFalse(model.hasRecommendedUpdate());

    solder("/solder-a/", "solder-2", "1.21.1");
    model.setPackInfo(cachedRepository.refreshPackInfo("testpack", false));
    assertFalse(model.isLocalOnly());
    assertTrue(model.hasRecommendedUpdate());
  }

  @Test
  void emptySolderCatalogIsOfflineAndDoesNotAdvertiseAnUpdate() throws Exception {
    responses.put(
        "/solder-a/modpack/testpack",
        "{\"name\":\"testpack\",\"builds\":[],\"recommended\":null,\"latest\":null}");
    ModpackModel model = installedModel(repository.refreshPackInfo("testpack", false));

    assertTrue(model.isLocalOnly());
    assertFalse(model.hasRecommendedUpdate());
    assertTrue(model.getPackInfo().hasSolder());
  }

  @Test
  void liveSolderRemainsUsableWhenPlatformFallsBackToCachedMetadata() throws Exception {
    // a8e6e376: for a Solder pack, Solder availability takes precedence over Platform availability.
    LauncherFileSystem fileSystem = new LauncherFileSystem(tempDir.resolve("launcher"));
    PlatformPackInfoRepository cachedRepository =
        new PlatformPackInfoRepository(
            new ModpackCachePlatformApi(
                new HttpPlatformApi(root + "/platform/", "999"), 0, fileSystem),
            new HttpSolderApi(slug -> null));
    ModpackModel model = installedModel(cachedRepository.refreshPackInfo("testpack", false));
    responses.remove("/platform/modpack/testpack");
    solder("/solder-a/", "solder-2", "1.21.1");

    model.setPackInfo(cachedRepository.refreshPackInfo("testpack", false));

    assertEquals("Platform title", model.getDisplayName());
    assertFalse(model.isLocalOnly());
    assertTrue(model.hasRecommendedUpdate());
    assertEquals("1.21.1", model.getPackInfo().getModpack("solder-2").getGameVersion());
  }

  private ModpackModel installedModel(PackInfo info) throws Exception {
    Path directory = tempDir.resolve("modpacks/testpack");
    Files.createDirectories(directory.resolve("bin"));
    Files.writeString(
        directory.resolve("bin/version"), "{\"version\":\"solder-1\",\"legacy\":false}");
    return new ModpackModel(
        new InstalledPack("testpack", InstalledPack.RECOMMENDED, directory.toString()),
        info,
        new InstalledPackStore(tempDir.resolve("installedpacks.json")),
        new LauncherFileSystem(tempDir.resolve("launcher")));
  }

  private ModpackModel model() throws Exception {
    return new ModpackModel(null, repository.refreshPackInfo("testpack", false), null, null);
  }

  private void platform(String solderPath, String displayName) {
    String solderField = solderPath == null ? "" : ",\"solder\":\"" + root + solderPath + "\"";
    responses.put(
        "/platform/modpack/testpack",
        "{\"name\":\"testpack\",\"displayName\":\""
            + displayName
            + "\","
            + "\"version\":\"platform-zip\",\"minecraft\":\"1.12.2\",\"url\":\""
            + root
            + "/pack.zip\""
            + solderField
            + "}");
  }

  private void solder(String path, String build, String minecraft) {
    responses.put(path + "modpack", "{\"mirror_url\":\"" + root + "/mirror/\"}");
    responses.put(
        path + "modpack/testpack",
        "{\"name\":\"testpack\",\"display_name\":\"Solder title\",\"builds\":[\""
            + build
            + "\"],\"recommended\":\""
            + build
            + "\",\"latest\":\""
            + build
            + "\"}");
    responses.put(
        path + "modpack/testpack/" + build, "{\"minecraft\":\"" + minecraft + "\",\"mods\":[]}");
  }
}
