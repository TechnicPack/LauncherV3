package net.technicpack.minecraftcore.install.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.io.Library;

/** The immutable client-relevant contract read from an official modern installer. */
public final class ModernInstallerProfile {
  private final int spec;
  private final String profile;
  private final String version;
  private final String minecraft;
  private final String json;
  private final String embeddedId;
  private final String embeddedParent;
  private final byte[] versionJsonBytes;
  private final List<String> libraryDefinitions;
  private final List<Processor> processors;
  private final List<Processor> clientProcessors;
  private final Map<String, DataValue> data;

  ModernInstallerProfile(
      int spec,
      String profile,
      String version,
      String minecraft,
      String json,
      String embeddedId,
      String embeddedParent,
      byte[] versionJsonBytes,
      List<String> libraryDefinitions,
      List<Processor> processors,
      Map<String, DataValue> data) {
    this.spec = spec;
    this.profile = profile;
    this.version = version;
    this.minecraft = minecraft;
    this.json = json;
    this.embeddedId = embeddedId;
    this.embeddedParent = embeddedParent;
    this.versionJsonBytes = versionJsonBytes.clone();
    this.libraryDefinitions = immutableList(libraryDefinitions);
    this.processors = immutableList(processors);
    List<Processor> selected = new ArrayList<>();
    for (Processor processor : processors) {
      if (processor.isClient()) {
        selected.add(processor);
      }
    }
    this.clientProcessors = immutableList(selected);
    this.data = immutableMap(data);
  }

  public int getSpec() {
    return spec;
  }

  public String getProfile() {
    return profile;
  }

  public String getVersion() {
    return version;
  }

  public String getMinecraft() {
    return minecraft;
  }

  /** Returns the normalized, relative ZIP member selected by the profile. */
  public String getJson() {
    return json;
  }

  public String getEmbeddedId() {
    return embeddedId;
  }

  public String getEmbeddedParent() {
    return embeddedParent;
  }

  public byte[] getVersionJsonBytes() {
    return versionJsonBytes.clone();
  }

  /**
   * Returns independent library snapshots. Library and its nested download, rule and extraction
   * models are mutable, so an unmodifiable list alone would not protect this profile.
   */
  public List<Library> getLibraries() {
    if (libraryDefinitions.isEmpty()) {
      return Collections.emptyList();
    }
    List<Library> libraries = new ArrayList<>(libraryDefinitions.size());
    for (String definition : libraryDefinitions) {
      libraries.add(MojangUtils.getGson().fromJson(definition, Library.class));
    }
    return Collections.unmodifiableList(libraries);
  }

  public List<Processor> getProcessors() {
    return processors;
  }

  public List<Processor> getClientProcessors() {
    return clientProcessors;
  }

  public Map<String, DataValue> getData() {
    return data;
  }

  private static <T> List<T> immutableList(List<T> values) {
    return Collections.unmodifiableList(new ArrayList<>(values));
  }

  private static <T> Map<String, T> immutableMap(Map<String, T> values) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  public static final class Processor {
    private final String jar;
    private final List<String> classpath;
    private final List<String> args;
    private final List<String> sides;
    private final Map<String, String> outputs;

    Processor(
        String jar,
        List<String> classpath,
        List<String> args,
        List<String> sides,
        Map<String, String> outputs) {
      this.jar = jar;
      this.classpath = immutableList(classpath);
      this.args = immutableList(args);
      this.sides = immutableList(sides);
      this.outputs = immutableMap(outputs);
    }

    public String getJar() {
      return jar;
    }

    public List<String> getClasspath() {
      return classpath;
    }

    public List<String> getArgs() {
      return args;
    }

    /** Missing restrictions are represented by both client and server. */
    public List<String> getSides() {
      return sides;
    }

    public Map<String, String> getOutputs() {
      return outputs;
    }

    public boolean isClient() {
      return sides.contains("client");
    }
  }

  public static final class DataValue {
    private final String client;
    private final String server;

    DataValue(String client, String server) {
      this.client = client;
      this.server = server;
    }

    /** Returns null when no client value is declared. */
    public String getClient() {
      return client;
    }

    /** Returns null when no server value is declared. */
    public String getServer() {
      return server;
    }
  }
}
