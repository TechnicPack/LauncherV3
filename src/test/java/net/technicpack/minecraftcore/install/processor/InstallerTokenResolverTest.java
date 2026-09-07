package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstallerTokenResolverTest {
  @TempDir Path temp;

  @Test
  void honorsEscapesInsideKeysAndLiteralsWithoutRescanningSubstitutions() throws Exception {
    Map<String, String> data = new LinkedHashMap<>();
    data.put("Aq", "value");
    data.put("A", "'{B}'\\q");
    assertEquals("q {A} value", InstallerTokenResolver.expand("\\q '{A}' {A\\q}", data));
    assertEquals("one'two", InstallerTokenResolver.expand("'one\\'two'", data));
    assertEquals("'{B}'\\q", InstallerTokenResolver.expand("{A}", data));
    data.put("A}", "escaped delimiter");
    assertEquals("escaped delimiter", InstallerTokenResolver.expand("{A\\}}", data));
  }

  @Test
  void rejectsMissingKeysUnclosedDelimitersAndTrailingEscapes() {
    for (String value :
        Arrays.asList("{MISSING}", "{open", "'open", "trailing\\", "{A\\", "'A\\")) {
      assertThrows(
          IOException.class,
          () -> InstallerTokenResolver.expand(value, Collections.emptyMap()),
          value);
    }
  }

  @Test
  void onlyWholeRawBracketArgumentsBecomeMavenPaths() throws Exception {
    Path libraries = temp.resolve("libraries");
    assertEquals(
        libraries.resolve("example/tool/1/tool-1.jar").toString(),
        InstallerTokenResolver.resolveArgument(
            "[example:tool:1]", Collections.emptyMap(), libraries));
    assertEquals(
        "[example:tool:1]",
        InstallerTokenResolver.resolveArgument(
            "'[example:tool:1]'", Collections.emptyMap(), libraries));
    assertEquals(
        "one argument with spaces",
        InstallerTokenResolver.resolveArgument(
            "{VALUE}", Collections.singletonMap("VALUE", "one argument with spaces"), libraries));
    assertEquals(
        "[example:tool:1]",
        InstallerTokenResolver.resolveArgument(
            "{VALUE}", Collections.singletonMap("VALUE", "[example:tool:1]"), libraries));
    assertThrows(
        IOException.class,
        () ->
            InstallerTokenResolver.resolveArgument(
                "[example:../tool:1]", Collections.emptyMap(), libraries));
  }

  @Test
  void resolvesClientResourcesOnlyAndProtectsReservedValues() throws Exception {
    Path cache = Files.createDirectories(temp.resolve("cache"));
    Path work = Files.createTempDirectory(cache, "processor-");
    Path installer = temp.resolve("installer.jar");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(installer))) {
      entry(zip, "nested/input.dat", "payload");
      entry(zip, "unreferenced.dat", "must remain in archive");
    }
    Map<String, ModernInstallerProfile.DataValue> values = new LinkedHashMap<>();
    values.put(
        "INPUT", new ModernInstallerProfile.DataValue("/nested/input.dat", "/missing-server.dat"));
    values.put("SERVER_ONLY", new ModernInstallerProfile.DataValue(null, "/missing-server.dat"));
    values.put("ROOT", new ModernInstallerProfile.DataValue("/missing-reserved.dat", null));
    values.put("LITERAL", new ModernInstallerProfile.DataValue("'{MISSING} \\q'", null));
    values.put("OUTPUT", new ModernInstallerProfile.DataValue("[example:generated:1]", null));
    Map<String, String> resolved =
        InstallerTokenResolver.resolveData(
            profile(values), temp, installer, cache.resolve("minecraft_1.20.1.jar"), work);
    assertEquals("payload", Files.readString(Path.of(resolved.get("INPUT"))));
    assertFalse(Files.exists(work.resolve("unreferenced.dat")));
    assertFalse(resolved.containsKey("SERVER_ONLY"));
    assertEquals(temp.toString(), resolved.get("ROOT"));
    assertEquals("{MISSING} \\q", resolved.get("LITERAL"));
    Path generated = temp.resolve("libraries/example/generated/1/generated-1.jar");
    assertEquals(generated.toString(), resolved.get("OUTPUT"));
    assertFalse(Files.exists(generated));
    assertThrows(IOException.class, () -> InstallerTokenResolver.expand("{SERVER_ONLY}", resolved));
  }

  @Test
  void rejectsUnsafeResourceMembersBeforeExtraction() throws Exception {
    Path work =
        Files.createTempDirectory(Files.createDirectories(temp.resolve("cache")), "processor-");
    for (String resource :
        Arrays.asList("/../outside", "//absolute", "dir\\file", "dir//file", "./file", "")) {
      Map<String, ModernInstallerProfile.DataValue> data =
          Collections.singletonMap("INPUT", new ModernInstallerProfile.DataValue(resource, null));
      assertThrows(
          IOException.class,
          () ->
              InstallerTokenResolver.resolveData(
                  profile(data),
                  temp,
                  temp.resolve("installer.jar"),
                  temp.resolve("cache/minecraft_1.20.1.jar"),
                  work));
    }
  }

  @Test
  void outputsRequireExactHashesAndContainedTargets() throws Exception {
    String hash = "0123456789012345678901234567890123456789";
    Map<String, String> data = Collections.singletonMap("HASH", hash);
    ModernInstallerProfile.Processor processor =
        processor(Collections.singletonMap("relative/output.jar", "{HASH}"));
    assertEquals(
        Collections.singletonMap(temp.resolve("relative/output.jar"), hash),
        InstallerTokenResolver.resolveOutputs(processor, data, temp, temp.resolve("libraries")));
    assertThrows(
        IOException.class,
        () ->
            InstallerTokenResolver.resolveOutputs(
                processor(Collections.singletonMap("../outside.jar", hash)),
                data,
                temp,
                temp.resolve("libraries")));
    assertThrows(
        IOException.class,
        () ->
            InstallerTokenResolver.resolveOutputs(
                processor(Collections.singletonMap("output.jar", "'" + hash + " '")),
                data,
                temp,
                temp.resolve("libraries")));
    Path outside = Files.createTempDirectory("token-output-outside-");
    try {
      Files.createSymbolicLink(temp.resolve("escape"), outside);
      assertThrows(
          IOException.class,
          () ->
              InstallerTokenResolver.resolveOutputs(
                  processor(Collections.singletonMap("escape/output.jar", hash)),
                  data,
                  temp,
                  temp.resolve("libraries")));
    } finally {
      Files.delete(outside);
    }
  }

  private static ModernInstallerProfile.Processor processor(Map<String, String> outputs) {
    return new ModernInstallerProfile.Processor(
        "example:tool:1",
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.singletonList("client"),
        outputs);
  }

  private static ModernInstallerProfile profile(
      Map<String, ModernInstallerProfile.DataValue> data) {
    return new ModernInstallerProfile(
        1,
        "test",
        "loader",
        "1.20.1",
        "version.json",
        "loader",
        "1.20.1",
        new byte[0],
        Collections.emptyList(),
        Collections.emptyList(),
        data);
  }

  private static void entry(ZipOutputStream zip, String name, String contents) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(contents.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }
}
