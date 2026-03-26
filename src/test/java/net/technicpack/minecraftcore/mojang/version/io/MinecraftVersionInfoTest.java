package net.technicpack.minecraftcore.mojang.version.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import org.junit.jupiter.api.Test;

class MinecraftVersionInfoTest {
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
}
