package net.technicpack.minecraftcore.install.processor;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Packaged by tests into a processor-only JAR and run only on the current test JVM. */
public final class ProcessorTestFixture {
  public static void main(String[] args) throws Exception {
    switch (args[0]) {
      case "capture":
        try (DataOutputStream output =
            new DataOutputStream(Files.newOutputStream(Paths.get(args[1])))) {
          output.writeInt(args.length - 2);
          for (int index = 2; index < args.length; index++) {
            byte[] bytes = args[index].getBytes(StandardCharsets.UTF_8);
            output.writeInt(bytes.length);
            output.write(bytes);
          }
        }
        break;
      case "isolation":
        ClassLoader loader = ProcessorTestFixture.class.getClassLoader();
        if (Thread.currentThread().getContextClassLoader() != loader) {
          throw new IllegalStateException("Processor TCCL is not its own loader");
        }
        for (String hidden :
            new String[] {
              "net.technicpack.utilslib.Utils",
              "net.technicpack.minecraftcore.install.processor.ProcessorBootstrap",
              "org.junit.jupiter.api.Test"
            }) {
          try {
            Class.forName(hidden, false, loader);
            throw new IllegalStateException("Unexpected parent visibility: " + hidden);
          } catch (ClassNotFoundException expected) {
            // Neither the launcher, bootstrap app loader, nor test framework may leak in.
          }
        }
        try (DataOutputStream output =
            new DataOutputStream(Files.newOutputStream(Paths.get(args[1])))) {
          output.writeUTF(System.getProperty("user.dir"));
          output.writeUTF(System.getProperty("java.home"));
          output.writeUTF(System.getenv("JAVA_HOME"));
          output.writeUTF(System.getenv("PATH"));
          URL[] urls = ((URLClassLoader) loader).getURLs();
          output.writeInt(urls.length);
          for (URL url : urls) output.writeUTF(Paths.get(url.toURI()).toString());
          try (InputStream resource = loader.getResourceAsStream("classpath-order.txt")) {
            if (resource == null)
              throw new IllegalStateException("Missing ordered classpath resource");
            output.writeUTF(new String(resource.readAllBytes(), StandardCharsets.UTF_8));
          }
        }
        break;
      case "throw":
        for (int index = 0; index < 60; index++) {
          System.out.println("diagnostic-" + index);
        }
        throw new IllegalStateException("fixture-thrown-cause");
      case "nonzero":
        System.err.println("fixture-nonzero");
        System.exit(7);
        break;
      case "early-exit":
        System.out.println("fixture-early-exit");
        System.exit(0);
        break;
      case "sleep":
        Files.writeString(Paths.get(args[1]), Long.toString(ProcessHandle.current().pid()));
        Thread.sleep(Long.MAX_VALUE);
        break;
      case "hold-pipe":
        Thread.sleep(Long.MAX_VALUE);
        break;
      case "inherited-pipe":
        String executable =
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Paths.get(System.getProperty("java.home"), "bin", executable);
        Path jar =
            Paths.get(
                ProcessorTestFixture.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        Process descendant =
            new ProcessBuilder(
                    java.toString(),
                    "-cp",
                    jar.toString(),
                    ProcessorTestFixture.class.getName(),
                    "hold-pipe")
                .inheritIO()
                .start();
        Files.writeString(Paths.get(args[1]), Long.toString(descendant.pid()));
        break;
      default:
        throw new IllegalArgumentException("Unknown fixture mode: " + args[0]);
    }
  }
}
