package net.technicpack.minecraftcore.install.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.technicpack.minecraftcore.mojang.version.io.Artifact;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ModernInstallerProfileReaderTest {
  private static final String SHA1 = "0123456789abcdef0123456789abcdef01234567";
  @TempDir Path tempDir;

  @Test
  void absentArchivePayloadAndLegacyProfilesRemainNonModern() throws Exception {
    assertNull(ModernInstallerProfileReader.read(tempDir.resolve("absent.jar")));
    Path payload =
        archive(null, Collections.singletonMap("net/minecraft/Client.class", bytes("payload")));
    assertNull(ModernInstallerProfileReader.read(payload));
    Path legacy = archive("{\"install\":{},\"versionInfo\":{}}", Collections.emptyMap());
    assertNull(ModernInstallerProfileReader.read(legacy));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "[]", "null", "true", "{\"install\":{}}", "{\"versionInfo\":{}}"})
  void unknownProfileShapesAreNotPayloads(String profile) throws Exception {
    Path installer = archive(profile, Collections.emptyMap());
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(installer));
  }

  @ParameterizedTest
  @CsvSource({"0, 0", "1, 1", "0.0, 0", "1e0, 1", "\"1\", 1"})
  void acceptsOnlySupportedNumericValuesAndHistoricalStringSpec(String spec, int expected)
      throws Exception {
    JsonObject profile = profile();
    profile.add("spec", json(spec));
    assertEquals(expected, read(profile).getSpec());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "-1",
        "2",
        "0.5",
        "1.00000000000000000000000000001",
        "\"0\"",
        "\"1.0\"",
        "\" 1\"",
        "true",
        "null",
        "[]",
        "{}"
      })
  void malformedSpecCannotFallBackToLegacy(String spec) throws Exception {
    JsonObject profile = profile();
    profile.add("spec", json(spec));
    profile.add("install", new JsonObject());
    profile.add("versionInfo", new JsonObject());
    assertInvalid(profile);
  }

  @Test
  void emptyDataArrayIsShapeNormalizationNotVersionDispatch() throws Exception {
    JsonObject profile = profile();
    profile.addProperty("spec", 0);
    profile.addProperty("version", "unrelated-display-version");
    profile.add("data", new JsonArray());
    assertTrue(read(profile).getData().isEmpty());
    profile.addProperty("spec", "1");
    assertTrue(read(profile).getData().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[{}]",
        "[[]]",
        "null",
        "true",
        "\"data\"",
        "{\"A\":[]}",
        "{\"A\":null}",
        "{\"A\":{\"client\":4}}",
        "{\"A\":{\"server\":false}}",
        "{\"A\":{\"client\":null}}"
      })
  void rejectsMalformedDataInsteadOfSilentlyDroppingIt(String data) throws Exception {
    JsonObject profile = profile();
    profile.add("data", json(data));
    assertInvalid(profile);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"profile", "version", "minecraft", "json", "libraries", "data", "processors"})
  void requiredModernFieldsCannotBeMissingOrNull(String field) throws Exception {
    JsonObject profile = profile();
    profile.remove(field);
    assertInvalid(profile);
    profile.add(field, null);
    assertInvalid(profile);
  }

  @ParameterizedTest
  @ValueSource(strings = {"profile", "version", "minecraft", "json"})
  void identitiesMustBeNonblankStringsWithoutControls(String field) throws Exception {
    JsonObject profile = profile();
    profile.addProperty(field, 1);
    assertInvalid(profile);
    profile.addProperty(field, " \t");
    assertInvalid(profile);
    profile.addProperty(field, "valid\ninvalid");
    assertInvalid(profile);
  }

  @Test
  void clientSelectionAndArgumentOrderFollowTheRecipeWithoutResolvingResources() throws Exception {
    JsonObject profile = profile();
    profile.addProperty("profile", "Custom Loader Identity");
    profile.add("serverJarPath", new JsonArray());
    profile.add("icon", new JsonObject());
    profile.add(
        "data",
        json(
            "{\"PATCH\":{\"client\":\"/data/missing.lzma\"},\"SERVER_ONLY\":{\"server\":\"/../../server-only\"}}"));
    JsonArray processors = new JsonArray();
    JsonObject server = processor("example:server:1");
    server.add("sides", json("[\"server\"]"));
    processors.add(server);
    JsonObject common = processor("example:common:1");
    common.add("classpath", json("[\"example:b:1\",\"example:a:1\",\"example:b:1\"]"));
    common.add(
        "args", json("[\"--first\",\"two words\",\"\",\"{PATCH}\",\"[example:generated:1@zip]\"]"));
    common.add(
        "outputs",
        json(
            "{\"{ROOT}/first\":\"{HASH}\",\"[example:generated:1@zip]\":\"0123456789abcdef0123456789abcdef01234567\"}"));
    processors.add(common);
    JsonObject neither = processor("example:neither:1");
    neither.add("sides", new JsonArray());
    processors.add(neither);
    JsonObject client = processor("example:client:1");
    client.add("sides", json("[\"client\"]"));
    processors.add(client);
    profile.add("processors", processors);

    ModernInstallerProfile result = read(profile);
    assertEquals(
        Arrays.asList("example:common:1", "example:client:1"), jars(result.getClientProcessors()));
    assertEquals(
        Arrays.asList(
            "example:server:1", "example:common:1", "example:neither:1", "example:client:1"),
        jars(result.getProcessors()));
    ModernInstallerProfile.Processor selected = result.getClientProcessors().get(0);
    assertEquals(
        Arrays.asList("example:b:1", "example:a:1", "example:b:1"), selected.getClasspath());
    assertEquals(
        Arrays.asList("--first", "two words", "", "{PATCH}", "[example:generated:1@zip]"),
        selected.getArgs());
    assertEquals(
        Arrays.asList("{ROOT}/first", "[example:generated:1@zip]"),
        new ArrayList<>(selected.getOutputs().keySet()));
    assertTrue(result.getClientProcessors().get(1).getOutputs().isEmpty());
    assertNull(result.getData().get("SERVER_ONLY").getClient());
    assertNull(result.getData().get("PATCH").getServer());
    assertEquals("/data/missing.lzma", result.getData().get("PATCH").getClient());
    assertFalse(Files.exists(tempDir.resolve("data")));
    assertEquals("Custom Loader Identity", result.getProfile());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"jar\":5}",
        "{\"jar\":\"../tools:jar:1\"}",
        "{\"classpath\":null}",
        "{\"classpath\":[\"example:tool:../1\"]}",
        "{\"args\":{}}",
        "{\"args\":[false]}",
        "{\"sides\":null}",
        "{\"sides\":[1]}",
        "{\"outputs\":[]}",
        "{\"outputs\":{\"out\":42}}"
      })
  void rejectsMalformedProcessorFields(String replacement) throws Exception {
    JsonObject profile = profile();
    JsonObject processor = processor("example:tool:1");
    for (Map.Entry<String, JsonElement> entry : json(replacement).getAsJsonObject().entrySet()) {
      processor.add(entry.getKey(), entry.getValue());
    }
    profile.getAsJsonArray("processors").add(processor);
    assertInvalid(profile);
  }

  @Test
  void usesOnlySelectedMemberAndRetainsItsExactBytes() throws Exception {
    JsonObject profile = profile();
    profile.addProperty("json", "/metadata/chosen.json");
    byte[] selected =
        bytes(" \n" + new String(version("loader-1", "1.20.4"), StandardCharsets.UTF_8) + "\n ");
    Map<String, byte[]> members = new LinkedHashMap<>();
    members.put("version.json", bytes("not selected and not valid JSON"));
    members.put("metadata/chosen.json", selected);
    Path installer = archive(profile.toString(), members);
    Path stale = tempDir.resolve("version.json");
    Files.write(stale, bytes("cached version"));

    ModernInstallerProfile result = ModernInstallerProfileReader.read(installer);
    assertEquals("metadata/chosen.json", result.getJson());
    assertArrayEquals(selected, result.getVersionJsonBytes());
    byte[] returned = result.getVersionJsonBytes();
    returned[0] = '!';
    assertArrayEquals(selected, result.getVersionJsonBytes());
    assertArrayEquals(bytes("cached version"), Files.readAllBytes(stale));
    assertFalse(Files.exists(tempDir.resolve("metadata")));
  }

  @Test
  void missingSelectedMemberDoesNotFallBackToDefaultOrCachedVersion() throws Exception {
    JsonObject profile = profile();
    profile.addProperty("json", "/missing.json");
    Path installer =
        archive(
            profile.toString(),
            Collections.singletonMap("version.json", version("loader-1", "1.20.4")));
    Files.write(tempDir.resolve("version.json"), version("loader-1", "1.20.4"));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(installer));
  }

  @Test
  void directorySelectedAsVersionIsRejected() throws Exception {
    JsonObject profile = profile();
    profile.addProperty("json", "selected.json");
    Path installer =
        archive(profile.toString(), Collections.singletonMap("selected.json/", new byte[0]));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(installer));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[]",
        "null",
        "{broken",
        "{\"id\":\"loader-1\"}",
        "{\"id\":3,\"inheritsFrom\":\"1.20.4\"}",
        "{\"id\":\"loader-1\",\"inheritsFrom\":4}",
        "{\"id\":\"loader-1\",\"inheritsFrom\":\"1.20.4\",\"arguments\":[]}"
      })
  void malformedSelectedJsonFailsImmediately(String embedded) throws Exception {
    Path installer =
        archive(profile().toString(), Collections.singletonMap("version.json", bytes(embedded)));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(installer));
  }

  @Test
  void malformedUtf8AndTrailingDocumentsAreNotAccepted() throws Exception {
    Path invalidUtf8 =
        archive(
            profile().toString(),
            Collections.singletonMap("version.json", new byte[] {'{', '"', (byte) 0xc3, '"', '}'}));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(invalidUtf8));
    Path trailing =
        archive(
            profile().toString(),
            Collections.singletonMap(
                "version.json",
                bytes(new String(version("loader-1", "1.20.4"), StandardCharsets.UTF_8) + " {}")));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(trailing));
  }

  @Test
  void rawMinecraftMismatchFailsButDisplayVersionMismatchIsNonfatal() throws Exception {
    JsonObject profile = profile();
    Path mismatched =
        archive(
            profile.toString(),
            Collections.singletonMap("version.json", version("loader-1", "1.20.5")));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(mismatched));
    byte[] selected = version("embedded-other-id", "1.20.4");
    Path displayMismatch =
        archive(profile.toString(), Collections.singletonMap("version.json", selected));
    ModernInstallerProfile result = ModernInstallerProfileReader.read(displayMismatch);
    assertEquals("loader-1", result.getVersion());
    assertEquals("embedded-other-id", result.getEmbeddedId());
    assertArrayEquals(selected, result.getVersionJsonBytes());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/",
        "//version.json",
        "../version.json",
        "metadata/../version.json",
        "metadata/./version.json",
        "metadata//version.json",
        "metadata/version.json/",
        "metadata\\version.json",
        "C:/version.json",
        "\\\\host\\version.json"
      })
  void selectedMemberMustBeASafeRelativeZipPath(String path) throws Exception {
    JsonObject profile = profile();
    profile.addProperty("json", path);
    assertInvalid(profile);
  }

  @Test
  void declarationsRetainVariantMetadataAndNonJarCoordinates() throws Exception {
    JsonObject profile = profile();
    JsonObject library = library("example:mappings:1@zip");
    library.getAsJsonObject("downloads").getAsJsonObject("artifact").remove("path");
    profile.getAsJsonArray("libraries").add(library);
    JsonObject nativeLibrary = library("example:native:1");
    JsonObject classifiers = new JsonObject();
    classifiers.add("natives-${arch}", artifact("different/native-${arch}.jar"));
    nativeLibrary.getAsJsonObject("downloads").add("classifiers", classifiers);
    profile.getAsJsonArray("libraries").add(nativeLibrary);
    List<Library> result = read(profile).getLibraries();
    assertEquals("example/mappings/1/mappings-1.zip", result.get(0).getArtifactPath());
    assertEquals("different/native-${arch}.jar", result.get(1).getArtifactPath("natives-${arch}"));
    assertEquals(SHA1, result.get(1).getArtifactSha1("natives-${arch}"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"path\":\"../escape.jar\"}",
        "{\"sha1\":\"invalid\"}",
        "{\"sha1\":42}",
        "{\"size\":-1}",
        "{\"size\":0.5}",
        "{\"size\":9223372036854775808}",
        "{\"size\":\"1\"}",
        "{\"url\":false}"
      })
  void rejectsMalformedDeclaredArtifactMetadataInProfileAndVersion(String replacement)
      throws Exception {
    JsonObject library = library("example:tool:1");
    JsonObject artifact = library.getAsJsonObject("downloads").getAsJsonObject("artifact");
    for (Map.Entry<String, JsonElement> entry : json(replacement).getAsJsonObject().entrySet()) {
      artifact.add(entry.getKey(), entry.getValue());
    }
    JsonObject profile = profile();
    profile.getAsJsonArray("libraries").add(library);
    assertInvalid(profile);
    profile.getAsJsonArray("libraries").remove(0);
    JsonObject embedded =
        json(new String(version("loader-1", "1.20.4"), StandardCharsets.UTF_8)).getAsJsonObject();
    embedded.getAsJsonArray("libraries").add(library);
    Path installer =
        archive(
            profile.toString(),
            Collections.singletonMap("version.json", bytes(embedded.toString())));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(installer));
  }

  @Test
  void declarationsCannotOmitIntegrityMetadataOrUseMalformedCoordinates() throws Exception {
    for (String field : Arrays.asList("sha1", "size", "url")) {
      JsonObject profile = profile();
      JsonObject library = library("example:tool:1");
      library.getAsJsonObject("downloads").getAsJsonObject("artifact").remove(field);
      profile.getAsJsonArray("libraries").add(library);
      assertInvalid(profile);
    }
    JsonObject profile = profile();
    JsonObject library = library("example:tool:1");
    library.addProperty("name", "example:../tool:1");
    profile.getAsJsonArray("libraries").add(library);
    assertInvalid(profile);
    library.addProperty("name", "example:tool:1");
    library.remove("downloads");
    assertInvalid(profile);
  }

  @Test
  void callersCannotMutateProfileThroughLibrarySnapshotsOrNestedCollections() throws Exception {
    JsonObject profile = profile();
    JsonObject library = library("example:tool:1");
    library.add("rules", json("[{\"action\":\"allow\"}]"));
    library.add("natives", json("{\"linux\":\"native\"}"));
    library.add("extract", json("{\"exclude\":[\"META-INF/\"]}"));
    JsonObject classifiers = new JsonObject();
    classifiers.add("native", artifact("native.jar"));
    library.getAsJsonObject("downloads").add("classifiers", classifiers);
    profile.getAsJsonArray("libraries").add(library);
    JsonObject processor = processor("example:tool:1");
    processor.add("outputs", json("{\"out\":\"{HASH}\"}"));
    profile.getAsJsonArray("processors").add(processor);
    profile.add("data", json("{\"HASH\":{\"client\":\"'hash'\"}}"));
    ModernInstallerProfile result = read(profile);

    Library copy = result.getLibraries().get(0);
    copy.setName("changed:tool:2");
    copy.setUrl("https://changed.invalid/");
    copy.getArtifact(null).setUrl("changed");
    copy.getArtifact("native").setUrl("changed");
    copy.getDownloads().setArtifact(new Artifact("changed", "changed", 999));
    copy.getRules().clear();
    copy.getNatives().clear();
    copy.getExtract().getExclude().clear();
    Library fresh = result.getLibraries().get(0);
    assertEquals("example:tool:1", fresh.getName());
    assertNull(fresh.getUrl());
    assertEquals("", fresh.getArtifact(null).getUrl());
    assertEquals("", fresh.getArtifact("native").getUrl());
    assertEquals(SHA1, fresh.getArtifactSha1(null));
    assertEquals(1, fresh.getRules().size());
    assertEquals("native", fresh.getNatives().get("linux"));
    assertFalse(fresh.getExtract().shouldExtract("META-INF/MANIFEST.MF"));
    ModernInstallerProfile.Processor selected = result.getClientProcessors().get(0);
    assertThrows(UnsupportedOperationException.class, () -> selected.getArgs().add("changed"));
    assertThrows(
        UnsupportedOperationException.class, () -> selected.getClasspath().add("changed:tool:2"));
    assertThrows(UnsupportedOperationException.class, () -> selected.getSides().clear());
    assertThrows(UnsupportedOperationException.class, () -> selected.getOutputs().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.getProcessors().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.getClientProcessors().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.getData().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.getLibraries().clear());
  }

  private ModernInstallerProfile read(JsonObject profile) throws IOException {
    return ModernInstallerProfileReader.read(
        archive(
            profile.toString(),
            Collections.singletonMap("version.json", version("loader-1", "1.20.4"))));
  }

  private void assertInvalid(JsonObject profile) throws IOException {
    Path installer =
        archive(
            profile.toString(),
            Collections.singletonMap("version.json", version("loader-1", "1.20.4")));
    assertThrows(IOException.class, () -> ModernInstallerProfileReader.read(installer));
  }

  private Path archive(String profile, Map<String, byte[]> entries) throws IOException {
    Path archive = Files.createTempFile(tempDir, "installer-", ".zip");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      if (profile != null) {
        output.putNextEntry(new ZipEntry("install_profile.json"));
        output.write(bytes(profile));
        output.closeEntry();
      }
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        output.putNextEntry(new ZipEntry(entry.getKey()));
        output.write(entry.getValue());
        output.closeEntry();
      }
    }
    return archive;
  }

  private static JsonObject profile() {
    return json("{\"spec\":1,\"profile\":\"forge\",\"version\":\"loader-1\",\"minecraft\":\"1.20.4\",\"json\":\"/version.json\",\"libraries\":[],\"data\":{},\"processors\":[]}")
        .getAsJsonObject();
  }

  private static byte[] version(String id, String parent) {
    JsonObject version = new JsonObject();
    version.addProperty("id", id);
    version.addProperty("inheritsFrom", parent);
    version.addProperty("mainClass", "example.Main");
    version.add("libraries", new JsonArray());
    version.add("arguments", new JsonObject());
    return bytes(version.toString());
  }

  private static JsonObject processor(String jar) {
    JsonObject processor = new JsonObject();
    processor.addProperty("jar", jar);
    processor.add("classpath", new JsonArray());
    processor.add("args", new JsonArray());
    return processor;
  }

  private static JsonObject library(String coordinate) {
    JsonObject library = new JsonObject();
    library.addProperty("name", coordinate);
    JsonObject downloads = new JsonObject();
    downloads.add("artifact", artifact("declared/tool.jar"));
    library.add("downloads", downloads);
    return library;
  }

  private static JsonObject artifact(String path) {
    JsonObject artifact = new JsonObject();
    artifact.addProperty("path", path);
    artifact.addProperty("url", "");
    artifact.addProperty("sha1", SHA1);
    artifact.addProperty("size", 1);
    return artifact;
  }

  private static List<String> jars(List<ModernInstallerProfile.Processor> processors) {
    List<String> result = new ArrayList<>();
    for (ModernInstallerProfile.Processor processor : processors) {
      result.add(processor.getJar());
    }
    return result;
  }

  private static JsonElement json(String value) {
    return JsonParser.parseString(value);
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
