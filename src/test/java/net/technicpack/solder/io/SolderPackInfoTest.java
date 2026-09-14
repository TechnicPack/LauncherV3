package net.technicpack.solder.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.solder.ISolderPackApi;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;

class SolderPackInfoTest {
  @Test
  void missingCatalogHasSafeAccessorsButCannotPassValidation() {
    SolderPackInfo info = pack("{\"name\":\"test-pack\"}");
    assertEquals(Collections.emptyList(), info.getBuilds());
    assertTrue(info.isLocal());
    assertThrows(RestfulAPIException.class, () -> info.validate("test-pack"));
  }

  @Test
  void emptyCatalogIsValidButCannotBeInstalled() throws Exception {
    SolderPackInfo info = pack("{\"name\":\"test-pack\",\"builds\":[]}");
    info.validate("test-pack");
    assertEquals(Collections.emptyList(), info.getBuilds());
    assertTrue(info.isLocal());
  }

  @Test
  void unboundOrUnavailableBuildFailsWithBuildInaccessible() {
    SolderPackInfo info = pack("{\"name\":\"test-pack\",\"builds\":[\"1.0\"]}");
    assertThrows(BuildInaccessibleException.class, () -> info.getModpack("1.0"));
    info.setSolder(
        new ISolderPackApi() {
          @Override
          public String getMirrorUrl() {
            return "https://mirror.example/";
          }

          @Override
          public SolderPackInfo getPackInfoForBulk() {
            return info;
          }

          @Override
          public SolderPackInfo getPackInfo() {
            return info;
          }

          @Override
          public Modpack getPackBuild(String build) {
            return null;
          }
        });
    assertThrows(BuildInaccessibleException.class, () -> info.getModpack("1.0"));
  }

  private static SolderPackInfo pack(String json) {
    return Utils.getGson().fromJson(json, SolderPackInfo.class);
  }
}
