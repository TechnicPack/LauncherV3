package net.technicpack.minecraftcore.mojang.version.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.chain.ChainedMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.argument.Argument;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import org.junit.jupiter.api.Test;

class MinecraftVersionInfoTest {
  @Test
  void versionJvmInheritanceIsParentFirstButGameAndUserArgumentsRemainChildFirst() {
    ChainedMinecraftVersionInfo version = new ChainedMinecraftVersionInfo(argumentLayer("child"));
    version.addVersionToChain(argumentLayer("parent"));
    version.addVersionToChain(argumentLayer("vanilla"));
    version.addJvmArguments(List.of(Argument.literal("-Dpriority=prism")));

    assertEquals(
        List.of("-Dpriority=vanilla", "-Dpriority=parent", "-Dpriority=child", "-Dpriority=prism"),
        version.getJavaArguments().resolve(null, null, null));
    assertEquals(
        List.of("--layer", "child", "--layer", "parent", "--layer", "vanilla"),
        version.getMinecraftArguments().resolve(null, null, null));
    assertEquals(
        List.of("-Duser=child", "-Duser=parent", "-Duser=vanilla"),
        version.getDefaultUserJavaArguments().resolve(null, null, null));
  }

  private static MinecraftVersionInfo argumentLayer(String name) {
    return MojangUtils.getGson()
        .fromJson(
            "{\"id\":\""
                + name
                + "\",\"type\":\"release\",\"arguments\":{"
                + "\"jvm\":[\"-Dpriority="
                + name
                + "\"],"
                + "\"game\":[\"--layer\",\""
                + name
                + "\"],"
                + "\"default-user-jvm\":[\"-Duser="
                + name
                + "\"]}}",
            MinecraftVersionInfo.class);
  }

  @Test
  void deserializesDefaultUserJvmArgumentsFromArgumentsBlock() {
    MinecraftVersionInfo version =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"test-version\","
                    + "\"type\":\"release\","
                    + "\"arguments\":{"
                    + "\"game\":[\"--demo\"],"
                    + "\"jvm\":[\"-Xmx1G\"],"
                    + "\"default-user-jvm\":[\"-Xms256M\"]"
                    + "}"
                    + "}",
                MinecraftVersionInfo.class);

    ArgumentList defaultUserJvmArguments = version.getDefaultUserJavaArguments();

    assertNotNull(defaultUserJvmArguments);
    assertEquals(1, defaultUserJvmArguments.getArguments().size());
    assertEquals("-Xms256M", defaultUserJvmArguments.getArguments().get(0).getArgStrings().get(0));
  }

  @Test
  void deserializesDefaultUserJvmArgumentsFromValueOnlyObject() {
    MinecraftVersionInfo version =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"test-version\","
                    + "\"type\":\"release\","
                    + "\"arguments\":{"
                    + "\"game\":[\"--demo\"],"
                    + "\"jvm\":[\"-Xmx1G\"],"
                    + "\"default-user-jvm\":[{\"value\":[\"-Xms2G\",\"-XX:+UseStringDeduplication\"]}]"
                    + "}"
                    + "}",
                MinecraftVersionInfo.class);

    assertEquals(
        List.of("-Xms2G", "-XX:+UseStringDeduplication"),
        version.getDefaultUserJavaArguments().resolve(null, null, null));
  }

  @Test
  void deserializesTrimmedOfficialMinecraft1201Fixture() throws IOException {
    MinecraftVersionInfo version;
    try (Reader reader =
        new InputStreamReader(
            MinecraftVersionInfoTest.class.getResourceAsStream("minecraft-1.20.1-lwjgl.json"),
            StandardCharsets.UTF_8)) {
      version = MojangUtils.getGson().fromJson(reader, MinecraftVersionInfo.class);
    }

    assertEquals("1.20.1", version.getId());
    assertEquals("net.minecraft.client.main.Main", version.getMainClass());
    assertEquals(10, version.getLibraries().size());
    assertTrue(
        version.getLibraries().stream()
            .anyMatch(
                library -> library.getName().equals("org.lwjgl:lwjgl:3.3.1:natives-macos-arm64")));
    assertTrue(
        version.getLibraries().stream()
            .anyMatch(
                library ->
                    library.getName().equals("org.lwjgl:lwjgl:3.3.1:natives-windows-arm64")));
  }
}
