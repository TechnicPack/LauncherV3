package net.technicpack.utilslib;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v5.Apache5HttpTransport;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;

/**
 * Launcher-process HTTPS policy. Bundled roots are authoritative, including on old Java runtimes.
 */
public final class LauncherTls {
  private static final String RESOURCES = "/net/technicpack/launcher/resources/tls/";
  private static SSLContext context;

  private LauncherTls() {}

  private static synchronized SSLContext context() {
    if (context == null) {
      SSLContext initialized;
      try (InputStream roots = LauncherTls.class.getResourceAsStream(RESOURCES + "cacert.pem");
          InputStream revocations =
              LauncherTls.class.getResourceAsStream(RESOURCES + "onecrl.json")) {
        initialized = createContext(roots, revocations);
      } catch (GeneralSecurityException | IOException | RuntimeException e) {
        throw new IllegalStateException("Unable to initialize launcher TLS security", e);
      }
      context = initialized;
    }
    return context;
  }

  public static void install() {
    SSLContext policy = context();
    SSLContext.setDefault(policy);
    HttpsURLConnection.setDefaultSSLSocketFactory(policy.getSocketFactory());
  }

  public static HttpTransport createHttpTransport() {
    return createHttpTransport(context());
  }

  static SSLContext createContext(InputStream roots, InputStream revocations)
      throws GeneralSecurityException, IOException {
    CertificateRevocations policy = CertificateRevocations.load(revocations);
    KeyStore store = createTrustStore(roots, policy);
    PKIXBuilderParameters parameters = new PKIXBuilderParameters(store, new X509CertSelector());
    // Disable Java's online CRL/OCSP checker, not the independent offline OneCRL checker.
    parameters.setRevocationEnabled(false);
    parameters.addCertPathChecker(policy.newChecker());
    TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
    factory.init(new CertPathTrustManagerParameters(parameters));
    SSLContext result = SSLContext.getInstance("TLS");
    result.init(null, factory.getTrustManagers(), null);
    return result;
  }

  static KeyStore createTrustStore(InputStream roots, CertificateRevocations policy)
      throws GeneralSecurityException, IOException {
    if (roots == null) throw new IOException("Missing bundled roots");
    Collection<? extends Certificate> certificates =
        CertificateFactory.getInstance("X.509").generateCertificates(roots);
    if (certificates.isEmpty()) throw new CertificateException("Empty bundled roots");
    KeyStore store = KeyStore.getInstance("JKS");
    store.load(null, null);
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    for (Certificate certificate : certificates) {
      if (!(certificate instanceof X509Certificate)
          || ((X509Certificate) certificate).getBasicConstraints() < 0) {
        throw new CertificateException("Bundled trust anchor is not an X509 CA");
      }
      X509Certificate root = (X509Certificate) certificate;
      if (!policy.isRevoked(root)) {
        byte[] fingerprint = sha256.digest(root.getEncoded());
        char[] alias = new char[fingerprint.length * 2];
        for (int i = 0; i < fingerprint.length; i++) {
          alias[i * 2] = Character.forDigit((fingerprint[i] & 255) >>> 4, 16);
          alias[i * 2 + 1] = Character.forDigit(fingerprint[i] & 15, 16);
        }
        store.setCertificateEntry(new String(alias), root);
      }
    }
    if (store.size() == 0) throw new CertificateException("No unrevoked bundled trust anchors");
    return store;
  }

  static HttpTransport createHttpTransport(SSLContext policy) {
    return new Apache5HttpTransport(
        HttpClients.custom()
            .useSystemProperties()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(
                        ClientTlsStrategyBuilder.create().setSslContext(policy).buildClassic())
                    .setMaxConnTotal(200)
                    .setMaxConnPerRoute(20)
                    .setDefaultConnectionConfig(
                        ConnectionConfig.custom().setTimeToLive(-1, TimeUnit.MILLISECONDS).build())
                    .build())
            .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .build());
  }
}
