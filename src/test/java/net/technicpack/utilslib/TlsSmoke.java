package net.technicpack.utilslib;

import static net.technicpack.utilslib.TlsTestFixture.*;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Java-8-source-compatible packaged smoke: no JUnit, no external network, no launcher user data.
 */
public final class TlsSmoke {
  private TlsSmoke() {}

  public static void main(String[] args) throws Exception {
    Path jar =
        Paths.get(LauncherTls.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    require(jar.toString().endsWith(".jar"), "Production classes must come from the shadow JAR");
    for (String name : new String[] {"cacert.pem", "onecrl.json", "sources.json"}) {
      URL resource = LauncherTls.class.getResource(PRODUCTION + name);
      require(
          resource != null && resource.getProtocol().equals("jar"),
          "Not a packaged TLS input: " + name);
    }
    try (JarFile file = new JarFile(jar.toFile())) {
      require(
          file.getEntry("net/technicpack/launcher/resources/technicKeystore.jks") == null,
          "Legacy JKS still shipped");
      Enumeration<java.util.jar.JarEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        require(
            !name.contains("9664066")
                && !name.contains("9664067")
                && !name.endsWith("server-key.pk8"),
            "Test certificate/key leaked into production JAR: " + name);
      }
    }
    System.out.println(
        "PASS packaged classes and TLS resources: "
            + jar
            + " on Java "
            + System.getProperty("java.version"));
    verifyAnchors();
    publicRevocations();
    handshakes(false);
    handshakes(true);
    redirects();
    publicEntryPoints();
    startupFailure(jar);
  }

  static void verifyAnchors() throws Exception {
    CertificateRevocations policy;
    try (InputStream input = resource(PRODUCTION + "onecrl.json")) {
      policy = CertificateRevocations.load(input);
    }
    Set<String> expected = new HashSet<>();
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    try (InputStream input = resource(PRODUCTION + "cacert.pem")) {
      for (Certificate certificate :
          CertificateFactory.getInstance("X.509").generateCertificates(input)) {
        if (!policy.isRevoked((X509Certificate) certificate))
          expected.add(hex(sha256.digest(certificate.getEncoded())));
      }
    }
    Set<String> actual = new HashSet<>();
    try (InputStream input = resource(PRODUCTION + "cacert.pem")) {
      KeyStore store = LauncherTls.createTrustStore(input, policy);
      Enumeration<String> aliases = store.aliases();
      while (aliases.hasMoreElements()) {
        actual.add(hex(sha256.digest(store.getCertificate(aliases.nextElement()).getEncoded())));
      }
    }
    require(
        actual.equals(expected) && !actual.isEmpty(),
        "Anchors differ from bundled PEM minus OneCRL");
    System.out.println(
        "PASS exact bundled anchor fingerprints minus revocations: " + actual.size());
  }

  private static void publicEntryPoints() throws Exception {
    SSLContext savedContext = SSLContext.getDefault();
    SSLSocketFactory savedFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
    try (Server server = new Server("leaf.pem", "intermediate-a.pem")) {
      SSLContext ordinary = ordinaryContext();
      SSLContext.setDefault(ordinary);
      HttpsURLConnection.setDefaultSSLSocketFactory(ordinary.getSocketFactory());
      HttpTransport transport = LauncherTls.createHttpTransport();
      try {
        try {
          transport
              .createRequestFactory()
              .buildGetRequest(new GenericUrl(server.url()))
              .setConnectTimeout(5000)
              .setReadTimeout(5000)
              .execute()
              .disconnect();
          throw new AssertionError("HTTP transport before install used unrelated default context");
        } catch (javax.net.ssl.SSLException expected) {
          /* bundled roots reject test CA */
        }
      } finally {
        transport.shutdown();
      }
      LauncherTls.install();
      require(SSLContext.getDefault() != ordinary, "install did not replace JVM default context");
      HttpsURLConnection connection = (HttpsURLConnection) new URL(server.url()).openConnection();
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      try {
        connection.getInputStream().close();
        throw new AssertionError("URLConnection default accepted private CA");
      } catch (javax.net.ssl.SSLException expected) {
        /* same bundled policy */
      } finally {
        connection.disconnect();
      }
      require(server.requests.get() == 0, "Public entry points reached private CA HTTP server");
    } finally {
      SSLContext.setDefault(savedContext);
      HttpsURLConnection.setDefaultSSLSocketFactory(savedFactory);
    }
    System.out.println(
        "PASS HTTP transport before install and process defaults use authoritative bundled trust");
  }

  private static void startupFailure(Path jar) throws Exception {
    Path temporary = Files.createTempDirectory("launcher-tls-startup-");
    Path corrupt = temporary.resolve("net/technicpack/launcher/resources/tls/onecrl.json");
    Path output = temporary.resolve("stderr.txt");
    Process process = null;
    try {
      Files.createDirectories(corrupt.getParent());
      Files.write(corrupt, new byte[] {'{'});
      process =
          new ProcessBuilder(
                  Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                  "-Djava.awt.headless=true",
                  "-cp",
                  temporary + java.io.File.pathSeparator + jar,
                  "net.technicpack.launcher.LauncherMain")
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();
      require(process.waitFor(30, TimeUnit.SECONDS), "Malformed-policy startup timed out");
      String text = new String(Files.readAllBytes(output), java.nio.charset.StandardCharsets.UTF_8);
      require(process.exitValue() == 1, "Wrong startup exit: " + text);
      require(
          text.contains(
              "Unable to initialize secure HTTPS. Please reinstall or update the Technic Launcher."),
          text);
      require(
          !text.toLowerCase(java.util.Locale.ROOT).contains("headless"),
          "Started GUI before TLS: " + text);
      System.out.println("PASS real LauncherMain malformed policy exits 1 before services/UI");
    } finally {
      if (process != null && process.isAlive()) process.destroyForcibly().waitFor();
      try (java.util.stream.Stream<Path> paths = Files.walk(temporary)) {
        for (Path path :
            (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator)
          Files.delete(path);
      }
    }
  }
}
