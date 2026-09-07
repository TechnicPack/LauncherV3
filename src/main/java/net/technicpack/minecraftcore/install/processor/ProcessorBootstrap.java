package net.technicpack.minecraftcore.install.processor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/** Standalone Java 8 entry point: this class alone is copied into the child bootstrap JAR. */
public final class ProcessorBootstrap {
  static final int MAGIC = 0x54504950; // TPIP
  static final int VERSION = 1;
  static final int MAX_DESCRIPTOR_BYTES = 16 * 1024 * 1024;
  static final int MAX_STRING_BYTES = 1024 * 1024;
  static final int MAX_LIST_ENTRIES = 65535;

  private ProcessorBootstrap() {}

  public static void main(String[] command) throws Throwable {
    if (command.length != 2) {
      throw new IOException("Expected processor descriptor and completion file");
    }
    Path descriptor = Paths.get(command[0]);
    Path completion = Paths.get(command[1]);
    Files.deleteIfExists(completion);
    if (!Files.isRegularFile(descriptor) || Files.size(descriptor) > MAX_DESCRIPTOR_BYTES) {
      throw new IOException("Invalid processor descriptor size or file");
    }

    String mainClass;
    String[] classpath;
    String[] arguments;
    // Track consumption as well as the initial size, so a growing file cannot bypass the cap.
    int[] remaining = {MAX_DESCRIPTOR_BYTES};
    try (DataInputStream input = new DataInputStream(Files.newInputStream(descriptor))) {
      if (readInt(input, remaining) != MAGIC || readInt(input, remaining) != VERSION) {
        throw new IOException("Unsupported processor descriptor magic or version");
      }
      mainClass = readString(input, remaining);
      classpath = readList(input, remaining);
      arguments = readList(input, remaining);
      if (input.read() != -1) {
        throw new IOException("Trailing processor descriptor data");
      }
    }
    if (mainClass.trim().isEmpty() || classpath.length == 0) {
      throw new IOException("Processor descriptor requires Main-Class and classpath");
    }
    URL[] urls = new URL[classpath.length];
    for (int index = 0; index < classpath.length; index++) {
      Path entry = Paths.get(classpath[index]);
      if (!Files.isRegularFile(entry)) {
        throw new IOException("Processor classpath is not a regular file: " + entry);
      }
      urls[index] = entry.toUri().toURL();
    }

    ClassLoader parent = null;
    try {
      parent = (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
    } catch (NoSuchMethodException java8) {
      // Java 8 has no platform loader. A null parent exposes only bootstrap classes.
    }
    Thread thread = Thread.currentThread();
    ClassLoader previous = thread.getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(urls, parent)) {
      thread.setContextClassLoader(loader);
      try {
        Class<?> processor = Class.forName(mainClass, true, loader);
        Method main = processor.getMethod("main", String[].class);
        if (!Modifier.isStatic(main.getModifiers()) || main.getReturnType() != Void.TYPE) {
          throw new IOException("Processor main must be public static void: " + mainClass);
        }
        try {
          main.invoke(null, (Object) arguments);
        } catch (InvocationTargetException failure) {
          throw failure.getCause();
        }
      } finally {
        thread.setContextClassLoader(previous);
      }
    }

    // System.exit(0) inside a processor never reaches this point. This is a temporary protocol
    // marker, not an installation receipt or a security boundary against hostile processors.
    try (DataOutputStream output =
        new DataOutputStream(
            Files.newOutputStream(
                completion, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
      output.writeInt(MAGIC);
      output.writeInt(VERSION);
    }
  }

  private static int readInt(DataInputStream input, int[] remaining) throws IOException {
    if (remaining[0] < 4) {
      throw new IOException("Processor descriptor exceeds size limit");
    }
    remaining[0] -= 4;
    return input.readInt();
  }

  private static String readString(DataInputStream input, int[] remaining) throws IOException {
    int length = readInt(input, remaining);
    if (length < 0 || length > MAX_STRING_BYTES || length > remaining[0]) {
      throw new IOException("Invalid processor descriptor string length: " + length);
    }
    remaining[0] -= length;
    byte[] bytes = new byte[length];
    input.readFully(bytes);
    return StandardCharsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString();
  }

  private static String[] readList(DataInputStream input, int[] remaining) throws IOException {
    int count = readInt(input, remaining);
    if (count < 0 || count > MAX_LIST_ENTRIES || count > remaining[0] / 4) {
      throw new IOException("Invalid processor descriptor list count: " + count);
    }
    String[] entries = new String[count];
    for (int index = 0; index < count; index++) {
      entries[index] = readString(input, remaining);
    }
    return entries;
  }
}
