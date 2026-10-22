package net.technicpack.utilslib;

import static net.technicpack.utilslib.TlsTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LauncherTlsTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preservesNormalTlsChecksAndRejectsRevokedConstructedPaths(boolean googleTransport)
      throws Exception {
    handshakes(googleTransport);
  }

  @Test
  void transportRespectsDisabledRedirects() throws Exception {
    redirects();
  }

  @Test
  void malformedMissingNonCaOrFullyRevokedRootsAbortInitialization() throws Exception {
    assertThrows(IOException.class, () -> LauncherTls.createContext(null, json(cleanPolicy())));
    for (String invalid :
        new String[] {
          "", "garbage", "-----BEGIN CERTIFICATE-----\nAAAA\n-----END CERTIFICATE-----"
        }) {
      assertThrows(
          CertificateException.class,
          () -> LauncherTls.createContext(json(invalid), json(cleanPolicy())));
    }
    assertThrows(CertificateException.class, () -> context(cleanPolicy(), "leaf.pem"));
    assertThrows(
        CertificateException.class,
        () -> context(policy(subjectRecord(certificate("root-a.pem"))), "root-a.pem"));
    try (InputStream roots = roots("root-a.pem")) {
      assertThrows(IOException.class, () -> LauncherTls.createContext(roots, null));
    }
  }

  @Test
  void ignoresJavaTrustStoreEvenWhenItWouldTrustThePeer() throws Exception {
    Path storePath = temporary.resolve("private-company.jks");
    KeyStore store = KeyStore.getInstance("JKS");
    store.load(null, null);
    store.setCertificateEntry("private-company", certificate("root-a.pem"));
    try (OutputStream output = Files.newOutputStream(storePath)) {
      store.store(output, "test".toCharArray());
    }
    String[] names = {
      "javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword", "javax.net.ssl.trustStoreType"
    };
    String[] saved = new String[names.length];
    for (int i = 0; i < names.length; i++) saved[i] = System.getProperty(names[i]);
    try {
      System.setProperty(names[0], storePath.toString());
      System.setProperty(names[1], "test");
      System.setProperty(names[2], "JKS");
      TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
      factory.init((KeyStore) null);
      SSLContext ordinary = SSLContext.getInstance("TLS");
      ordinary.init(null, factory.getTrustManagers(), null);
      scenario(
          "simulated Java store baseline", ordinary, false, "leaf.pem", "intermediate-a.pem", true);
      for (boolean googleTransport : new boolean[] {false, true}) {
        scenario(
            "Java-only private root is not trusted",
            context(cleanPolicy(), "root-b.pem"),
            googleTransport,
            "leaf.pem",
            "intermediate-a.pem",
            false);
      }
    } finally {
      for (int i = 0; i < names.length; i++) {
        if (saved[i] == null) System.clearProperty(names[i]);
        else System.setProperty(names[i], saved[i]);
      }
    }
  }

  @Test
  void anchorStoreDeduplicatesAndRemovesRevokedRoots() throws Exception {
    CertificateRevocations revoked =
        CertificateRevocations.load(json(policy(subjectRecord(certificate("root-a.pem")))));
    try (InputStream roots = roots("root-a.pem", "root-b.pem", "root-b.pem")) {
      KeyStore store = LauncherTls.createTrustStore(roots, revoked);
      assertEquals(1, store.size());
      assertEquals(certificate("root-b.pem"), store.getCertificate(store.aliases().nextElement()));
    }
  }

  @Test
  void packagedPolicyAnchorsMatchBundledPemMinusRevocations() throws Exception {
    TlsSmoke.verifyAnchors();
  }
}
