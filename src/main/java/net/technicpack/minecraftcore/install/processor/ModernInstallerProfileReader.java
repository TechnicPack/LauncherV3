package net.technicpack.minecraftcore.install.processor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.io.Library;
import net.technicpack.minecraftcore.mojang.version.io.MavenCoordinate;
import net.technicpack.minecraftcore.mojang.version.io.MinecraftVersionInfo;
import net.technicpack.utilslib.Utils;

/** Reads modern installer metadata without extracting resources or modifying the filesystem. */
public final class ModernInstallerProfileReader {
  private static final Pattern SHA1 = Pattern.compile("[0-9a-fA-F]{40}");
  private static final List<String> BOTH_SIDES = Arrays.asList("client", "server");

  private ModernInstallerProfileReader() {}

  /**
   * Returns null for an absent archive/profile, a payload archive, or an old install/versionInfo
   * profile. A present spec always selects modern parsing; malformed modern input never falls back.
   */
  public static ModernInstallerProfile read(Path installer) throws IOException {
    if (Files.notExists(installer)) {
      return null;
    }
    try (ZipFile archive = new ZipFile(installer.toFile())) {
      ZipEntry entry = archive.getEntry("install_profile.json");
      if (entry == null) {
        return null;
      }
      JsonObject root = parseObject(readEntry(archive, entry), "install_profile.json");
      if (!root.has("spec")) {
        if (root.has("install") && root.has("versionInfo")) {
          return null;
        }
        throw invalid("install_profile.json", "unknown installer profile shape (missing spec)");
      }

      int spec = readSpec(root.get("spec"));
      String profile = identity(root.get("profile"), "profile");
      String version = identity(root.get("version"), "version");
      String minecraft = identity(root.get("minecraft"), "minecraft");
      String json = memberName(identity(root.get("json"), "json"));
      List<String> libraries =
          readLibraries(array(root.get("libraries"), "libraries"), "libraries");
      Map<String, ModernInstallerProfile.DataValue> data = readData(root.get("data"));
      List<ModernInstallerProfile.Processor> processors =
          readProcessors(array(root.get("processors"), "processors"));

      ZipEntry selected = archive.getEntry(json);
      if (selected == null) {
        throw invalid("json", "selected ZIP member is missing: " + json);
      }
      byte[] versionBytes = readEntry(archive, selected);
      JsonObject embedded = parseObject(versionBytes, json);
      String embeddedId = identity(embedded.get("id"), json + ".id");
      String embeddedParent = identity(embedded.get("inheritsFrom"), json + ".inheritsFrom");
      if (!minecraft.equals(embeddedParent)) {
        throw invalid(
            "minecraft", minecraft + " does not match " + json + " inheritsFrom " + embeddedParent);
      }
      validateVersion(embedded, json);
      if (!version.equals(embeddedId)) {
        Utils.getLogger()
            .warning(
                "Installer profile version "
                    + version
                    + " differs from embedded version ID "
                    + embeddedId
                    + " in "
                    + installer
                    + "; using the embedded version metadata");
      }
      return new ModernInstallerProfile(
          spec,
          profile,
          version,
          minecraft,
          json,
          embeddedId,
          embeddedParent,
          versionBytes,
          libraries,
          processors,
          data);
    } catch (RuntimeException e) {
      // Gson adapters and MavenCoordinate reject malformed metadata with unchecked exceptions.
      throw new IOException(
          "Malformed installer metadata in " + installer + ": " + e.getMessage(), e);
    }
  }

  private static int readSpec(JsonElement element) throws IOException {
    if (element != null && element.isJsonPrimitive()) {
      JsonPrimitive value = element.getAsJsonPrimitive();
      if (value.isString() && "1".equals(value.getAsString())) {
        return 1;
      }
      if (value.isNumber()) {
        BigDecimal number = value.getAsBigDecimal();
        if (number.compareTo(BigDecimal.ZERO) == 0) {
          return 0;
        }
        if (number.compareTo(BigDecimal.ONE) == 0) {
          return 1;
        }
      }
    }
    throw invalid("spec", "expected numeric 0 or 1, or the exact string \"1\"");
  }

  private static Map<String, ModernInstallerProfile.DataValue> readData(JsonElement element)
      throws IOException {
    if (element != null && element.isJsonArray() && element.getAsJsonArray().isEmpty()) {
      return Collections.emptyMap();
    }
    JsonObject values = object(element, "data");
    Map<String, ModernInstallerProfile.DataValue> data = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
      String field = "data." + entry.getKey();
      JsonObject sides = object(entry.getValue(), field);
      data.put(
          entry.getKey(),
          new ModernInstallerProfile.DataValue(
              optionalString(sides, "client", field), optionalString(sides, "server", field)));
    }
    return data;
  }

  private static List<ModernInstallerProfile.Processor> readProcessors(JsonArray elements)
      throws IOException {
    List<ModernInstallerProfile.Processor> processors = new ArrayList<>(elements.size());
    for (int index = 0; index < elements.size(); index++) {
      String field = "processors[" + index + "]";
      JsonObject processor = object(elements.get(index), field);
      String jar = string(processor.get("jar"), field + ".jar");
      MavenCoordinate.parse(jar);
      List<String> classpath = strings(processor.get("classpath"), field + ".classpath");
      for (String coordinate : classpath) {
        MavenCoordinate.parse(coordinate);
      }
      List<String> args = strings(processor.get("args"), field + ".args");
      List<String> sides =
          processor.has("sides") ? strings(processor.get("sides"), field + ".sides") : BOTH_SIDES;
      Map<String, String> outputs =
          processor.has("outputs")
              ? stringMap(processor.get("outputs"), field + ".outputs")
              : Collections.emptyMap();
      processors.add(new ModernInstallerProfile.Processor(jar, classpath, args, sides, outputs));
    }
    return processors;
  }

  private static List<String> readLibraries(JsonArray libraries, String field) throws IOException {
    List<String> definitions = new ArrayList<>(libraries.size());
    for (int index = 0; index < libraries.size(); index++) {
      String location = field + "[" + index + "]";
      JsonObject library = object(libraries.get(index), location);
      validateLibrary(library, location);
      // Use the launcher's adapters too, so accepted snapshots can always become ordinary
      // libraries.
      MojangUtils.getGson().fromJson(library, Library.class);
      definitions.add(library.toString());
    }
    return definitions;
  }

  private static void validateLibrary(JsonObject library, String location) throws IOException {
    MavenCoordinate coordinate =
        MavenCoordinate.parse(string(library.get("name"), location + ".name"));
    optionalString(library, "url", location);
    optionalString(library, "MMC-hint", location);
    JsonObject downloads = object(library.get("downloads"), location + ".downloads");
    boolean declared = false;
    if (downloads.has("artifact")) {
      validateArtifact(downloads.get("artifact"), location + ".downloads.artifact");
      declared = true;
    }
    if (downloads.has("classifiers")) {
      JsonObject classifiers =
          object(downloads.get("classifiers"), location + ".downloads.classifiers");
      for (Map.Entry<String, JsonElement> classifier : classifiers.entrySet()) {
        coordinate.withClassifier(classifier.getKey());
        validateArtifact(
            classifier.getValue(), location + ".downloads.classifiers." + classifier.getKey());
        declared = true;
      }
    }
    if (!declared) {
      throw invalid(location + ".downloads", "expected at least one artifact declaration");
    }
    if (library.has("natives")) {
      for (String classifier : stringMap(library.get("natives"), location + ".natives").values()) {
        coordinate.withClassifier(classifier);
      }
    }
    if (library.has("rules")) {
      validateRules(library.get("rules"), location + ".rules");
    }
    if (library.has("extract")) {
      JsonObject extract = object(library.get("extract"), location + ".extract");
      if (extract.has("exclude")) {
        strings(extract.get("exclude"), location + ".extract.exclude");
      }
    }
  }

  private static void validateArtifact(JsonElement element, String field) throws IOException {
    JsonObject artifact = object(element, field);
    String sha1 = string(artifact.get("sha1"), field + ".sha1");
    if (!SHA1.matcher(sha1).matches()) {
      throw invalid(field + ".sha1", "expected a 40-character hexadecimal SHA-1");
    }
    nonnegativeInteger(artifact.get("size"), field + ".size");
    string(artifact.get("url"), field + ".url");
    if (artifact.has("path")) {
      MavenCoordinate.resolve(Paths.get("."), string(artifact.get("path"), field + ".path"));
    }
  }

  private static void validateVersion(JsonObject version, String field) throws IOException {
    for (String name : Arrays.asList("mainClass", "minecraftArguments", "type", "assets")) {
      optionalString(version, name, field);
    }
    if (version.has("libraries")) {
      JsonArray libraries = array(version.get("libraries"), field + ".libraries");
      for (int index = 0; index < libraries.size(); index++) {
        String location = field + ".libraries[" + index + "]";
        validateLibrary(object(libraries.get(index), location), location);
      }
    }
    if (version.has("arguments")) {
      JsonObject arguments = object(version.get("arguments"), field + ".arguments");
      for (String name : Arrays.asList("game", "jvm")) {
        if (arguments.has(name)) {
          JsonArray values = array(arguments.get(name), field + ".arguments." + name);
          for (JsonElement value : values) {
            if (value.isJsonObject()) {
              JsonObject argument = value.getAsJsonObject();
              if (argument.has("rules")) {
                validateRules(argument.get("rules"), field + ".arguments." + name + ".rules");
              }
              JsonElement text = argument.get("value");
              if (text != null && text.isJsonArray()) {
                strings(text, field + ".arguments." + name + ".value");
              } else {
                string(text, field + ".arguments." + name + ".value");
              }
            } else {
              string(value, field + ".arguments." + name);
            }
          }
        }
      }
    }
    if (version.has("rules")) {
      validateRules(version.get("rules"), field + ".rules");
    }
    // The version's remaining launch fields belong to the existing Mojang metadata parser.
    MojangUtils.getGson().fromJson(version, MinecraftVersionInfo.class);
  }

  private static void validateRules(JsonElement element, String field) throws IOException {
    JsonArray rules = array(element, field);
    for (int index = 0; index < rules.size(); index++) {
      String location = field + "[" + index + "]";
      JsonObject rule = object(rules.get(index), location);
      string(rule.get("action"), location + ".action");
      if (rule.has("os")) {
        JsonObject os = object(rule.get("os"), location + ".os");
        for (String name : Arrays.asList("name", "version", "arch")) {
          optionalString(os, name, location + ".os");
        }
        if (os.has("versionRange")) {
          JsonObject range = object(os.get("versionRange"), location + ".os.versionRange");
          optionalString(range, "min", location + ".os.versionRange");
          optionalString(range, "max", location + ".os.versionRange");
        }
      }
      if (rule.has("features")) {
        JsonObject features = object(rule.get("features"), location + ".features");
        for (Map.Entry<String, JsonElement> feature : features.entrySet()) {
          JsonElement value = feature.getValue();
          if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw invalid(location + ".features." + feature.getKey(), "expected a boolean");
          }
        }
      }
    }
  }

  private static JsonObject parseObject(byte[] bytes, String field) throws IOException {
    try (JsonReader reader =
        new JsonReader(
            new InputStreamReader(
                new ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)))) {
      reader.setStrictness(Strictness.STRICT);
      JsonObject result = object(JsonParser.parseReader(reader), field);
      if (reader.peek() != JsonToken.END_DOCUMENT) {
        throw invalid(field, "unexpected trailing JSON content");
      }
      return result;
    }
  }

  private static byte[] readEntry(ZipFile archive, ZipEntry entry) throws IOException {
    if (entry.isDirectory()) {
      throw invalid(entry.getName(), "expected a file, not a ZIP directory");
    }
    try (InputStream input = archive.getInputStream(entry);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    }
  }

  private static String memberName(String value) throws IOException {
    String relative = value.startsWith("/") ? value.substring(1) : value;
    try {
      MavenCoordinate.resolve(Paths.get("."), relative);
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid installer json ZIP member: " + value, e);
    }
    return relative;
  }

  private static JsonObject object(JsonElement value, String field) throws IOException {
    if (value == null || !value.isJsonObject()) {
      throw invalid(field, "expected an object");
    }
    return value.getAsJsonObject();
  }

  private static JsonArray array(JsonElement value, String field) throws IOException {
    if (value == null || !value.isJsonArray()) {
      throw invalid(field, "expected an array");
    }
    return value.getAsJsonArray();
  }

  private static String string(JsonElement value, String field) throws IOException {
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw invalid(field, "expected a string");
    }
    return value.getAsString();
  }

  private static String identity(JsonElement value, String field) throws IOException {
    String text = string(value, field);
    if (text.trim().isEmpty()) {
      throw invalid(field, "expected a nonblank string");
    }
    for (int index = 0; index < text.length(); index++) {
      if (Character.isISOControl(text.charAt(index))) {
        throw invalid(field, "control characters are not allowed");
      }
    }
    return text;
  }

  private static String optionalString(JsonObject value, String name, String field)
      throws IOException {
    return value.has(name) ? string(value.get(name), field + "." + name) : null;
  }

  private static List<String> strings(JsonElement value, String field) throws IOException {
    JsonArray values = array(value, field);
    List<String> result = new ArrayList<>(values.size());
    for (int index = 0; index < values.size(); index++) {
      result.add(string(values.get(index), field + "[" + index + "]"));
    }
    return result;
  }

  private static Map<String, String> stringMap(JsonElement value, String field) throws IOException {
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : object(value, field).entrySet()) {
      result.put(entry.getKey(), string(entry.getValue(), field + "." + entry.getKey()));
    }
    return result;
  }

  private static void nonnegativeInteger(JsonElement value, String field) throws IOException {
    if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
      try {
        if (value.getAsBigDecimal().longValueExact() >= 0) {
          return;
        }
      } catch (ArithmeticException e) {
        throw new IOException("Invalid " + field + ": expected a nonnegative 64-bit integer", e);
      }
    }
    throw invalid(field, "expected a nonnegative 64-bit integer");
  }

  private static IOException invalid(String field, String reason) {
    return new IOException("Invalid installer " + field + ": " + reason);
  }
}
