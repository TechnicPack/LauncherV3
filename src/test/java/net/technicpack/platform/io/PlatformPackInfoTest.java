package net.technicpack.platform.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.io.Modpack;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlatformPackInfoTest {
  @Test
  void unresolvedSolderCannotFallBackToPlatformDownload() {
    PlatformPackInfo info =
        Utils.getGson()
            .fromJson(
                "{\"name\":\"test-pack\",\"solder\":\"https://solder.example/api/\","
                    + "\"version\":\"1.0\",\"url\":\"https://platform.example/pack.zip\"}",
                PlatformPackInfo.class);

    assertTrue(info.hasSolder());
    assertFalse(info.isLocal());
    assertTrue(info.getBuilds().isEmpty());
    assertThrows(BuildInaccessibleException.class, () -> info.getModpack("1.0"));
  }

  @Test
  void platformOnlyPackRetainsItsDownload() throws BuildInaccessibleException {
    PlatformPackInfo info =
        Utils.getGson()
            .fromJson(
                "{\"name\":\"test-pack\",\"minecraft\":\"1.20.1\","
                    + "\"version\":\"1.0\",\"url\":\"https://platform.example/pack.zip\"}",
                PlatformPackInfo.class);

    Modpack modpack = info.getModpack("1.0");

    assertEquals(List.of("1.0"), info.getBuilds());
    assertEquals("1.20.1", modpack.getGameVersion());
    assertEquals(1, modpack.getMods().size());
    assertEquals("https://platform.example/pack.zip", modpack.getMods().get(0).getUrl());
    assertEquals("1.0", modpack.getMods().get(0).getVersion());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void absentVersionDoesNotAdvertiseAnInstallableBuild(String version) {
    PlatformPackInfo info =
        Utils.getGson()
            .fromJson(
                "{\"name\":\"test-pack\",\"version\":" + Utils.getGson().toJson(version) + "}",
                PlatformPackInfo.class);

    assertEquals(List.of(), info.getBuilds());
  }
}
