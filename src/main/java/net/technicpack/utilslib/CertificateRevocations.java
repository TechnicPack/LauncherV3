package net.technicpack.utilslib;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

/** Immutable, release-bundled Mozilla OneCRL matching policy; no network revocation checks. */
final class CertificateRevocations {
  private final Map<Bytes, Set<Bytes>> issuerSerials;
  private final Map<Bytes, Set<Bytes>> subjectKeys;

  private CertificateRevocations(Map<Bytes, Set<Bytes>> issuers, Map<Bytes, Set<Bytes>> subjects) {
    issuerSerials = freeze(issuers);
    subjectKeys = freeze(subjects);
  }

  private static Map<Bytes, Set<Bytes>> freeze(Map<Bytes, Set<Bytes>> source) {
    Map<Bytes, Set<Bytes>> result = new HashMap<>();
    source.forEach(
        (key, values) -> result.put(key, Collections.unmodifiableSet(new HashSet<>(values))));
    return Collections.unmodifiableMap(result);
  }

  static CertificateRevocations load(InputStream input) throws IOException {
    if (input == null) throw new IOException("Missing bundled OneCRL");
    try {
      JsonElement parsed =
          new GsonBuilder()
              .setStrictness(Strictness.STRICT)
              .create()
              .fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonElement.class);
      if (parsed == null || !parsed.isJsonObject())
        throw new IllegalArgumentException("Expected object");
      JsonElement data = parsed.getAsJsonObject().get("data");
      if (data == null || !data.isJsonArray())
        throw new IllegalArgumentException("Expected data array");
      Map<Bytes, Set<Bytes>> issuers = new HashMap<>();
      Map<Bytes, Set<Bytes>> subjects = new HashMap<>();
      int enabledCount = 0;
      for (JsonElement element : data.getAsJsonArray()) {
        if (!element.isJsonObject()) throw new IllegalArgumentException("Expected record object");
        JsonObject record = element.getAsJsonObject();
        JsonElement enabled = record.get("enabled");
        if (enabled == null
            || !enabled.isJsonPrimitive()
            || !enabled.getAsJsonPrimitive().isBoolean()) {
          throw new IllegalArgumentException("enabled must be Boolean");
        }
        if (!enabled.getAsBoolean()) continue;
        enabledCount++;
        boolean issuer = record.has("issuerName") || record.has("serialNumber");
        boolean subject = record.has("subject") || record.has("pubKeyHash");
        if (issuer == subject)
          throw new IllegalArgumentException("Ambiguous or missing matching form");
        if (issuer) {
          byte[] name = name(record, "issuerName");
          byte[] serial = normalizeSerial(decode(record, "serialNumber"));
          issuers
              .computeIfAbsent(new Bytes(name), ignored -> new HashSet<>())
              .add(new Bytes(serial));
        } else {
          byte[] name = name(record, "subject");
          byte[] hash = decode(record, "pubKeyHash");
          if (hash.length != 32) throw new IllegalArgumentException("Expected SHA-256 SPKI hash");
          subjects
              .computeIfAbsent(new Bytes(name), ignored -> new HashSet<>())
              .add(new Bytes(hash));
        }
      }
      if (enabledCount == 0) throw new IllegalArgumentException("No enabled OneCRL entries");
      return new CertificateRevocations(issuers, subjects);
    } catch (RuntimeException e) {
      throw new IOException("Invalid bundled Mozilla OneCRL", e);
    }
  }

  private static byte[] decode(JsonObject record, String field) {
    JsonElement value = record.get(field);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException("Expected Base64 string: " + field);
    }
    byte[] decoded = Base64.getDecoder().decode(value.getAsString());
    if (decoded.length == 0) throw new IllegalArgumentException("Empty " + field);
    return decoded;
  }

  private static byte[] name(JsonObject record, String field) {
    byte[] encoded = decode(record, field);
    new X500Principal(encoded); // Validate DER Name, retaining exact DER matching semantics.
    return encoded;
  }

  private static byte[] normalizeSerial(byte[] serial) {
    int start = 0;
    while (start < serial.length - 1 && serial[start] == 0) start++;
    return start == 0 ? serial : Arrays.copyOfRange(serial, start, serial.length);
  }

  boolean isRevoked(X509Certificate certificate) throws CertificateException {
    Set<Bytes> serials =
        issuerSerials.get(new Bytes(certificate.getIssuerX500Principal().getEncoded()));
    if (serials != null
        && serials.contains(
            new Bytes(normalizeSerial(certificate.getSerialNumber().toByteArray())))) {
      return true;
    }
    Set<Bytes> keys =
        subjectKeys.get(new Bytes(certificate.getSubjectX500Principal().getEncoded()));
    if (keys == null) return false;
    try {
      return keys.contains(
          new Bytes(
              MessageDigest.getInstance("SHA-256")
                  .digest(certificate.getPublicKey().getEncoded())));
    } catch (NoSuchAlgorithmException e) {
      throw new CertificateException("SHA-256 unavailable", e);
    }
  }

  PKIXCertPathChecker newChecker() {
    return new PKIXCertPathChecker() {
      @Override
      public void init(boolean forward) {}

      @Override
      public boolean isForwardCheckingSupported() {
        return true;
      }

      @Override
      public Set<String> getSupportedExtensions() {
        return Collections.emptySet();
      }

      @Override
      public void check(Certificate certificate, Collection<String> unresolvedCriticalExtensions)
          throws CertPathValidatorException {
        if (!(certificate instanceof X509Certificate)) {
          throw new CertPathValidatorException("Expected X509Certificate");
        }
        try {
          if (isRevoked((X509Certificate) certificate)) {
            throw new CertPathValidatorException(
                "Certificate revoked by bundled Mozilla OneCRL",
                null,
                null,
                -1,
                CertPathValidatorException.BasicReason.REVOKED);
          }
        } catch (CertificateException e) {
          throw new CertPathValidatorException(e);
        }
      }
    };
  }

  /** Arrays are freshly decoded/encoded and never escape this class. */
  private static final class Bytes {
    private final byte[] value;
    private final int hash;

    Bytes(byte[] value) {
      this.value = value;
      hash = Arrays.hashCode(value);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Bytes && Arrays.equals(value, ((Bytes) other).value);
    }
  }
}
