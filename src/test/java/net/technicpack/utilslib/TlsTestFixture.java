package net.technicpack.utilslib;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v5.Apache5HttpTransport;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.hc.client5.http.classic.methods.HttpGet;

/** Real offline RSA-2048/SHA-256 fixtures; only the leaf private key is committed. */
final class TlsTestFixture {
  static final String PRODUCTION = "/net/technicpack/launcher/resources/tls/";
  static final String FIXTURES = "/net/technicpack/utilslib/tls/";

  private TlsTestFixture() {}

  static InputStream resource(String path) throws IOException {
    InputStream stream = TlsTestFixture.class.getResourceAsStream(path);
    if (stream == null) throw new IOException("Missing resource: " + path);
    return stream;
  }

  static byte[] bytes(InputStream input) throws IOException {
    try (InputStream stream = input;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int count;
      while ((count = stream.read(buffer)) != -1) out.write(buffer, 0, count);
      return out.toByteArray();
    }
  }

  static X509Certificate certificate(String name) throws Exception {
    try (InputStream input = resource(FIXTURES + name)) {
      return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
    }
  }

  static ByteArrayInputStream json(String value) {
    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
  }

  static String b64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  static JsonObject issuerRecord(X509Certificate certificate) {
    JsonObject record = new JsonObject();
    record.addProperty("enabled", true);
    record.addProperty("issuerName", b64(certificate.getIssuerX500Principal().getEncoded()));
    record.addProperty("serialNumber", b64(certificate.getSerialNumber().toByteArray()));
    return record;
  }

  static JsonObject subjectRecord(X509Certificate certificate) throws Exception {
    JsonObject record = new JsonObject();
    record.addProperty("enabled", true);
    record.addProperty("subject", b64(certificate.getSubjectX500Principal().getEncoded()));
    record.addProperty(
        "pubKeyHash",
        b64(MessageDigest.getInstance("SHA-256").digest(certificate.getPublicKey().getEncoded())));
    return record;
  }

  static String policy(JsonObject... records) {
    JsonArray data = new JsonArray();
    for (JsonObject record : records) data.add(record);
    JsonObject document = new JsonObject();
    document.add("data", data);
    return document.toString();
  }

  static String cleanPolicy() throws Exception {
    JsonObject record = issuerRecord(certificate("root-b.pem"));
    record.addProperty("serialNumber", b64(new byte[] {99}));
    return policy(record);
  }

  static InputStream roots(String... names) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (String name : names) output.write(bytes(resource(FIXTURES + name)));
    return new ByteArrayInputStream(output.toByteArray());
  }

  static SSLContext context(String policy, String... roots) throws Exception {
    try (InputStream input = roots(roots)) {
      return LauncherTls.createContext(input, json(policy));
    }
  }

  static SSLContext ordinaryContext() throws Exception {
    KeyStore store = KeyStore.getInstance("JKS");
    store.load(null, null);
    store.setCertificateEntry("a", certificate("root-a.pem"));
    TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
    factory.init(store);
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, factory.getTrustManagers(), null);
    return context;
  }

  static final class Server implements AutoCloseable {
    final HttpsServer server;
    final AtomicInteger requests = new AtomicInteger();

    Server(String leaf, String intermediate) throws Exception {
      KeyStore keys = KeyStore.getInstance("JKS");
      keys.load(null, null);
      keys.setKeyEntry(
          "server",
          KeyFactory.getInstance("RSA")
              .generatePrivate(
                  new PKCS8EncodedKeySpec(bytes(resource(FIXTURES + "server-key.pk8")))),
          "test".toCharArray(),
          new Certificate[] {certificate(leaf), certificate(intermediate)});
      KeyManagerFactory factory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      factory.init(keys, "test".toCharArray());
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(factory.getKeyManagers(), null, null);
      server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.setHttpsConfigurator(new HttpsConfigurator(context));
      server.createContext(
          "/",
          exchange -> {
            try {
              requests.incrementAndGet();
              if (exchange.getRequestURI().getPath().equals("/redirect")) {
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(302, -1);
              } else {
                byte[] body = "tls-ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
              }
            } finally {
              exchange.close();
            }
          });
      server.start();
    }

    String url() {
      return "https://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }

  static String request(SSLContext context, boolean apache, String url) throws Exception {
    if (apache) {
      HttpTransport transport = LauncherTls.createHttpTransport(context);
      try {
        HttpResponse response =
            transport
                .createRequestFactory()
                .buildGetRequest(new GenericUrl(url))
                .setConnectTimeout(5000)
                .setReadTimeout(5000)
                .execute();
        try {
          return response.parseAsString();
        } finally {
          response.disconnect();
        }
      } finally {
        transport.shutdown();
      }
    }
    HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
    connection.setSSLSocketFactory(context.getSocketFactory());
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(5000);
    try {
      return new String(bytes(connection.getInputStream()), StandardCharsets.UTF_8);
    } finally {
      connection.disconnect();
    }
  }

  static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  static void scenario(
      String label,
      SSLContext context,
      boolean apache,
      String leaf,
      String intermediate,
      boolean accepted)
      throws Exception {
    try (Server server = new Server(leaf, intermediate)) {
      try {
        String body = request(context, apache, server.url());
        require(accepted, label + " unexpectedly accepted");
        require(body.equals("tls-ok"), label + " wrong body: " + body);
        require(server.requests.get() == 1, label + " request count");
      } catch (IOException e) {
        if (accepted) throw e;
        boolean tlsFailure = false;
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
          if (cause instanceof javax.net.ssl.SSLException) tlsFailure = true;
        }
        require(tlsFailure, label + " failed for non-TLS reason: " + e);
        require(server.requests.get() == 0, label + " reached HTTP handler");
      }
    }
    System.out.println("PASS " + (apache ? "Apache " : "URLConnection ") + label);
  }

  static void handshakes(boolean apache) throws Exception {
    SSLContext clean = context(cleanPolicy(), "root-a.pem", "root-b.pem");
    scenario(
        "ordinary PKIX accepts OneCRL-blocked identity (before policy)",
        ordinaryContext(),
        apache,
        "leaf.pem",
        "intermediate-a.pem",
        true);
    scenario("clean", clean, apache, "leaf.pem", "intermediate-a.pem", true);
    scenario(
        "blocked leaf",
        context(policy(issuerRecord(certificate("leaf.pem"))), "root-a.pem", "root-b.pem"),
        apache,
        "leaf.pem",
        "intermediate-a.pem",
        false);
    SSLContext intermediate =
        context(
            policy(subjectRecord(certificate("intermediate-a.pem"))), "root-a.pem", "root-b.pem");
    scenario("blocked intermediate", intermediate, apache, "leaf.pem", "intermediate-a.pem", false);
    scenario("blocked cross-sign", intermediate, apache, "leaf.pem", "intermediate-b.pem", false);
    scenario(
        "blocked root omitted from peer chain",
        context(policy(subjectRecord(certificate("root-a.pem"))), "root-a.pem", "root-b.pem"),
        apache,
        "leaf.pem",
        "intermediate-a.pem",
        false);
    scenario(
        "unknown issuer",
        context(cleanPolicy(), "root-b.pem"),
        apache,
        "leaf.pem",
        "intermediate-a.pem",
        false);
    scenario("expired leaf", clean, apache, "expired.pem", "intermediate-a.pem", false);
    scenario("wrong hostname", clean, apache, "wrong-host.pem", "intermediate-a.pem", false);
  }

  static void redirects() throws Exception {
    try (Server server = new Server("leaf.pem", "intermediate-a.pem")) {
      Apache5HttpTransport transport =
          (Apache5HttpTransport)
              LauncherTls.createHttpTransport(context(cleanPolicy(), "root-a.pem"));
      try {
        int status =
            transport
                .getHttpClient()
                .execute(new HttpGet(server.url() + "redirect"), response -> response.getCode());
        require(status == 302 && server.requests.get() == 1, "Apache followed redirect");
      } finally {
        transport.shutdown();
      }
    }
    System.out.println("PASS Apache redirects remain disabled");
  }

  static void publicRevocations() throws Exception {
    CertificateRevocations policy;
    try (InputStream input = resource(PRODUCTION + "onecrl.json")) {
      policy = CertificateRevocations.load(input);
    }
    // Mozilla Bug 2083449: https://bugzilla.mozilla.org/attachment.cgi?id=9664066 (originals)
    // and https://bugzilla.mozilla.org/attachment.cgi?id=9664067 (Sectigo cross-signs).
    // SHA-256 certificate fingerprints below attribute the public TLS-only evidence; these are NOT
    // anchors.
    String[][] evidence = {
      {
        "9664066-EM-DV-TLS-CA-G3B.crt.pem",
        "42c4accd12c4f59597925e1a08a7a90102608543809308d50ec427e118ba1493"
      },
      {
        "9664066-EM-DV-TLS-CA-G3C.crt.pem",
        "0164d7ceae5bb909c272af151ff44b1177ae9b47d9424b94f1d9558c30934d19"
      },
      {
        "9664066-EM-EV-TLS-CA-G3B.crt.pem",
        "e7568fe6e12331f4fd7de61da4c7559d0f8eb192a218d4741a3ba75184333838"
      },
      {
        "9664066-EM-EV-TLS-CA-G3C.crt.pem",
        "432ec25f0d28de9006d97bf752f17bbb2cd41f669933fcb072d9a1148d0261f3"
      },
      {
        "9664066-EM-OV-TLS-CA-G3B.crt.pem",
        "98b896625c0c230adeec3eb2cf6aad5001ecf9156a68644e82374ce5833bc25b"
      },
      {
        "9664066-EM-OV-TLS-CA-G3C.crt.pem",
        "cb6e15165ac802e9b3f993f16c25d41f4e81c21ebde71ae1b012ac00bf9c7c3f"
      },
      {
        "9664067-EM-DV-TLS-CA-G3B-AAA.crt.pem",
        "45a883f349aedfd5f3a649b5498ffb121712ba8b448f3f2a91dbc5a766fcddbe"
      },
      {
        "9664067-EM-DV-TLS-CA-G3C-AAA.crt.pem",
        "62b7f9868263f72f6911fc78ceaf72c1979158b7368bf648683bb2aa39b02e72"
      },
      {
        "9664067-EM-EV-TLS-CA-G3B-AAA.crt.pem",
        "579480aea166290bd3c643589e795fc0d0e54e86505d180f1dff99efd3199512"
      },
      {
        "9664067-EM-EV-TLS-CA-G3C-AAA.crt.pem",
        "fd9d5c2dbbd4995ab69b8d50c439d9527b00529199c2fd01b2174eb58f07021f"
      },
      {
        "9664067-EM-OV-TLS-CA-G3B-AAA.crt.pem",
        "5f1ded4dd590af74631290cf073b122a2aac6a1fa479778a0bd43579bd832699"
      },
      {
        "9664067-EM-OV-TLS-CA-G3C-AAA.crt.pem",
        "7485b51c0ed192878fa1d2f948fb529ae1842cc5086c4b67f7be5b2039de9489"
      }
    };
    for (String[] entry : evidence) {
      X509Certificate certificate = certificate(entry[0]);
      require(
          hex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()))
              .equals(entry[1]),
          "Evidence fingerprint changed: " + entry[0]);
      require(
          policy.isRevoked(certificate),
          "SECURITY DATA ERROR: missing enabled revocation for " + entry[0]);
      System.out.println("PASS public OneCRL " + entry[0]);
    }
  }

  static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte value : bytes) result.append(String.format("%02x", value & 255));
    return result.toString();
  }
}
