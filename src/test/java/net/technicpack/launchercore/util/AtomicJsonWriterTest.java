package net.technicpack.launchercore.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicJsonWriterTest {
  @TempDir Path tempDir;

  private final Gson gson = new Gson();

  @Test
  void writeProducesExpectedJson() throws IOException {
    Path target = tempDir.resolve("out.json");
    Map<String, String> payload = new HashMap<>();
    payload.put("hello", "world");

    AtomicJsonWriter.write(target, payload, gson);

    String content = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
    assertEquals("{\"hello\":\"world\"}", content);
  }

  @Test
  void writeLeavesNoTempSibling() throws IOException {
    Path target = tempDir.resolve("out.json");
    AtomicJsonWriter.write(target, new HashMap<String, String>(), gson);

    assertTrue(Files.exists(target));
    assertFalse(
        Files.exists(tempDir.resolve("out.json.tmp")),
        "successful write must atomically rename its tmp, not leak it");
  }

  @Test
  void writeOverwritesExistingContentFully() throws IOException {
    Path target = tempDir.resolve("out.json");
    Map<String, String> first = new HashMap<>();
    first.put("payload", "first-a-distinct-string");
    AtomicJsonWriter.write(target, first, gson);

    Map<String, String> second = new HashMap<>();
    second.put("payload", "second-a-distinct-string");
    AtomicJsonWriter.write(target, second, gson);

    String content = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
    assertTrue(content.contains("second-a-distinct-string"));
    assertFalse(
        content.contains("first-a-distinct-string"),
        "atomic rename should fully replace, not append or interleave");
  }

  @Test
  void writeCleansUpTmpOnSerializationFailure() {
    Path target = tempDir.resolve("out.json");
    Gson failingGson =
        new GsonBuilder()
            .registerTypeAdapter(
                String.class,
                new TypeAdapter<String>() {
                  @Override
                  public void write(JsonWriter out, String value) throws IOException {
                    throw new IOException("simulated serialization failure");
                  }

                  @Override
                  public String read(JsonReader in) {
                    return null;
                  }
                })
            .create();

    assertThrows(JsonIOException.class, () -> AtomicJsonWriter.write(target, "boom", failingGson));

    assertFalse(Files.exists(target), "target should not exist when write failed");
    assertFalse(
        Files.exists(tempDir.resolve("out.json.tmp")),
        "tmp file must be cleaned up on serialization failure");
  }
}
