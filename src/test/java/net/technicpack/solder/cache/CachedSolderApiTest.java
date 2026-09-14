package net.technicpack.solder.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.solder.ISolderApi;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.solder.http.HttpSolderApi;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CachedSolderApiTest {
  private static final String ROOT_A = "https://solder-a.example/api/";
  private static final String ROOT_B = "https://solder-b.example/api/";

  @TempDir Path tempDir;

  @Test
  void publicCatalogCacheAndFailureThrottleAreScopedToRoot() throws Exception {
    MutableSolderApi inner = new MutableSolderApi();
    inner.catalogs.put(ROOT_A, Collections.singletonList(pack("source-a")));
    inner.catalogs.put(ROOT_B, Collections.singletonList(pack("source-b")));
    CachedSolderApi api = cache(inner, 3600);

    assertEquals("source-a", onlyPack(api.getPublicSolderPacks(ROOT_A)).getName());
    assertEquals("source-b", onlyPack(api.getPublicSolderPacks(ROOT_B)).getName());
    inner.offline.add(ROOT_A);
    inner.offline.add(ROOT_B);
    assertEquals("source-a", onlyPack(api.getPublicSolderPacks(ROOT_A)).getName());
    assertEquals("source-b", onlyPack(api.getPublicSolderPacks(ROOT_B)).getName());

    CachedSolderApi fresh = cache(inner, 3600);
    assertThrows(RestfulAPIException.class, () -> fresh.getPublicSolderPacks(ROOT_A));
    inner.offline.remove(ROOT_B);
    assertEquals("source-b", onlyPack(fresh.getPublicSolderPacks(ROOT_B)).getName());
  }

  @Test
  void publicOfflineFallbackAndRecoveryPreserveEarlierSnapshotState() throws Exception {
    MutableSolderApi inner = new MutableSolderApi();
    inner.catalogs.put(ROOT_A, Collections.singletonList(pack("source-a")));
    CachedSolderApi api = cache(inner, 0);
    SolderPackInfo live = onlyPack(api.getPublicSolderPacks(ROOT_A));
    assertFalse(live.isLocal());

    inner.offline.add(ROOT_A);
    SolderPackInfo offline = onlyPack(api.getPublicSolderPacks(ROOT_A));
    assertTrue(offline.isLocal());
    assertEquals(live.getBuilds(), offline.getBuilds());
    assertFalse(live.isLocal());

    inner.offline.clear();
    SolderPackInfo recovered = onlyPack(api.getPublicSolderPacks(ROOT_A));
    assertFalse(recovered.isLocal());
    assertTrue(offline.isLocal());
    assertFalse(live.isLocal());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "null",
        "{}",
        "{\"name\":\" \",\"builds\":[]}",
        "{\"name\":\"test-pack\"}",
        "{\"name\":\"test-pack\",\"builds\":[null]}",
        "{\"name\":\"test-pack\",\"builds\":[\" \"]}"
      })
  void malformedInnerPublicMetadataCannotEnterCache(String json) throws Exception {
    MutableSolderApi inner = new MutableSolderApi();
    inner.catalogs.put(
        ROOT_A, Collections.singletonList(Utils.getGson().fromJson(json, SolderPackInfo.class)));
    CachedSolderApi api = cache(inner, 0);
    assertThrows(RestfulAPIException.class, () -> api.getPublicSolderPacks(ROOT_A));
    inner.catalogs.put(ROOT_A, Collections.singletonList(pack("recovered")));
    assertEquals("recovered", onlyPack(api.getPublicSolderPacks(ROOT_A)).getName());
  }

  @Test
  void nullPublicCatalogIsRejectedButEmptyCatalogIsValid() throws Exception {
    MutableSolderApi inner = new MutableSolderApi();
    CachedSolderApi api = cache(inner, 0);
    assertThrows(RestfulAPIException.class, () -> api.getPublicSolderPacks(ROOT_A));
    inner.catalogs.put(ROOT_A, Collections.emptyList());
    assertEquals(Collections.emptyList(), api.getPublicSolderPacks(ROOT_A));
  }

  private CachedSolderApi cache(MutableSolderApi inner, int seconds) {
    return new CachedSolderApi(new LauncherFileSystem(tempDir), inner, seconds);
  }

  private static SolderPackInfo pack(String name) {
    return Utils.getGson()
        .fromJson("{\"name\":\"" + name + "\",\"builds\":[\"1.0\"]}", SolderPackInfo.class);
  }

  private static SolderPackInfo onlyPack(Collection<SolderPackInfo> packs) {
    assertEquals(1, packs.size());
    return packs.iterator().next();
  }

  private static class MutableSolderApi implements ISolderApi {
    private final Map<String, Collection<SolderPackInfo>> catalogs = new HashMap<>();
    private final Set<String> offline = new HashSet<>();

    @Override
    public ISolderPackApi getSolderPack(String root, String slug, String mirror)
        throws RestfulAPIException {
      return new HttpSolderApi(name -> null).getSolderPack(root, slug, mirror);
    }

    @Override
    public Collection<SolderPackInfo> getPublicSolderPacks(String root) throws RestfulAPIException {
      return internalGetPublicSolderPacks(root, this);
    }

    @Override
    public String getMirrorUrl(String root) {
      return "https://mirror.example/";
    }

    @Override
    public Collection<SolderPackInfo> internalGetPublicSolderPacks(String root, ISolderApi factory)
        throws RestfulAPIException {
      if (offline.contains(root)) {
        throw new RestfulAPIException("Solder unavailable");
      }
      return catalogs.get(root);
    }
  }
}
