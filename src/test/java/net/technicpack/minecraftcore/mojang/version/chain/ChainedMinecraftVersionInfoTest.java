package net.technicpack.minecraftcore.mojang.version.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.MinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import org.junit.jupiter.api.Test;

class ChainedMinecraftVersionInfoTest {
  @Test
  void mergesDefaultUserJvmArgumentsInChainOrder() {
    IMinecraftVersionInfo rootVersion =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"root\","
                    + "\"type\":\"release\","
                    + "\"arguments\":{"
                    + "\"default-user-jvm\":[\"-Xms128M\"]"
                    + "}"
                    + "}",
                MinecraftVersionInfo.class);
    IMinecraftVersionInfo childVersion =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"child\","
                    + "\"type\":\"release\","
                    + "\"arguments\":{"
                    + "\"default-user-jvm\":[\"-Xms256M\"]"
                    + "}"
                    + "}",
                MinecraftVersionInfo.class);

    ChainedMinecraftVersionInfo chain = new ChainedMinecraftVersionInfo(rootVersion);
    chain.addVersionToChain(childVersion);

    ArgumentList mergedArguments = chain.getDefaultUserJavaArguments();

    assertEquals(2, mergedArguments.getArguments().size());
    assertEquals(List.of("-Xms128M"), mergedArguments.getArguments().get(0).getArgStrings());
    assertEquals(List.of("-Xms256M"), mergedArguments.getArguments().get(1).getArgStrings());
  }

  @Test
  void returnsNullWhenNoVersionInChainDefinesDefaultUserJvm() {
    IMinecraftVersionInfo rootVersion =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"root\","
                    + "\"type\":\"release\","
                    + "\"arguments\":{"
                    + "\"game\":[],"
                    + "\"jvm\":[]"
                    + "}"
                    + "}",
                MinecraftVersionInfo.class);
    IMinecraftVersionInfo childVersion =
        MojangUtils.getGson()
            .fromJson(
                "{"
                    + "\"id\":\"child\","
                    + "\"type\":\"release\","
                    + "\"arguments\":{"
                    + "\"game\":[],"
                    + "\"jvm\":[]"
                    + "}"
                    + "}",
                MinecraftVersionInfo.class);

    ChainedMinecraftVersionInfo chain = new ChainedMinecraftVersionInfo(rootVersion);
    chain.addVersionToChain(childVersion);

    assertNull(chain.getDefaultUserJavaArguments());
  }
}
