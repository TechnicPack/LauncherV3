package net.technicpack.minecraftcore.mojang.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonObject;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JavaRuntimesIndexTest {

  @ParameterizedTest
  @ValueSource(strings = {"missing", "empty"})
  void unavailableComponentReturnsNull(String component) {
    JavaRuntimesIndex index = runtimeIndex();

    assertNull(index.getRuntimeForCurrentOS(component));
  }

  @Test
  void availableComponentKeepsFirstCatalogEntry() {
    JavaRuntimesIndex index = runtimeIndex();

    JavaRuntime runtime = index.getRuntimeForCurrentOS("java-runtime-delta");

    assertNotNull(runtime);
    assertEquals("21.0.5", runtime.getVersion().getName());
  }

  private static JavaRuntimesIndex runtimeIndex() {
    assumeTrue(OperatingSystem.getOperatingSystem() != OperatingSystem.UNKNOWN);
    JsonObject runtimes =
        Utils.getGson()
            .fromJson(
                "{\"java-runtime-delta\":[{\"version\":{\"name\":\"21.0.5\"}},"
                    + "{\"version\":{\"name\":\"21.0.4\"}}],\"empty\":[]}",
                JsonObject.class);
    JsonObject catalog = new JsonObject();
    for (String platform :
        new String[] {
          "linux",
          "linux-i386",
          "mac-os",
          "mac-os-arm64",
          "windows-arm64",
          "windows-x64",
          "windows-x86"
        }) {
      catalog.add(platform, runtimes);
    }
    return Utils.getGson().fromJson(catalog, JavaRuntimesIndex.class);
  }
}
