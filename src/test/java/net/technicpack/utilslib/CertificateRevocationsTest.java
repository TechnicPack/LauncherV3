package net.technicpack.utilslib;

import static net.technicpack.utilslib.TlsTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class CertificateRevocationsTest {
  @Test
  void issuerSerialUsesBothComponentsAndNormalizesSignPadding() throws Exception {
    JsonObject record = issuerRecord(certificate("intermediate-a.pem"));
    for (byte[] serial : new byte[][] {{(byte) 128}, {0, (byte) 128}, {0, 0, (byte) 128}}) {
      record.addProperty("serialNumber", b64(serial));
      CertificateRevocations policy = CertificateRevocations.load(json(policy(record)));
      assertTrue(policy.isRevoked(certificate("intermediate-a.pem")));
      assertFalse(policy.isRevoked(certificate("intermediate-b.pem")));
      assertFalse(policy.isRevoked(certificate("leaf.pem")));
    }
  }

  @Test
  void subjectAndSpkiBlockBothCrossSignsButNotAnotherKey() throws Exception {
    CertificateRevocations policy =
        CertificateRevocations.load(json(policy(subjectRecord(certificate("intermediate-a.pem")))));
    assertTrue(policy.isRevoked(certificate("intermediate-a.pem")));
    assertTrue(policy.isRevoked(certificate("intermediate-b.pem")));
    assertFalse(policy.isRevoked(certificate("intermediate-other-key.pem")));
    JsonObject differentSubject = subjectRecord(certificate("intermediate-a.pem"));
    differentSubject.addProperty(
        "subject", b64(certificate("root-a.pem").getSubjectX500Principal().getEncoded()));
    assertFalse(
        CertificateRevocations.load(json(policy(differentSubject)))
            .isRevoked(certificate("intermediate-a.pem")));
  }

  @Test
  void disabledRecordsNeverBlockOrCancelEnabledRecords() throws Exception {
    JsonObject disabled = issuerRecord(certificate("leaf.pem"));
    disabled.addProperty("enabled", false);
    JsonObject unrelated = issuerRecord(certificate("root-b.pem"));
    assertFalse(
        CertificateRevocations.load(json(policy(disabled, unrelated)))
            .isRevoked(certificate("leaf.pem")));
    JsonObject enabled = issuerRecord(certificate("leaf.pem"));
    assertTrue(
        CertificateRevocations.load(json(policy(enabled, disabled, enabled)))
            .isRevoked(certificate("leaf.pem")));
    assertTrue(
        CertificateRevocations.load(json(policy(disabled, enabled)))
            .isRevoked(certificate("leaf.pem")));
  }

  @Test
  void malformedOrMissingPoliciesFailClosed() throws Exception {
    assertThrows(IOException.class, () -> CertificateRevocations.load(null));
    for (String invalid :
        new String[] {
          "",
          "{",
          "null",
          "[]",
          "{}",
          "{\"data\":{}}",
          "{\"data\":[]}",
          "{\"data\":[false]}",
          "{\"data\":[{\"enabled\":false}]}",
          "{\"data\":[{\"enabled\":true}]}",
          "{\"data\":[{\"enabled\":\"true\"}]}",
          "{\"data\":[{\"enabled\":1}]}",
          "{data:[]}",
          cleanPolicy() + "garbage"
        }) {
      assertThrows(IOException.class, () -> CertificateRevocations.load(json(invalid)), invalid);
    }
    JsonObject original = issuerRecord(certificate("leaf.pem"));
    for (String field : new String[] {"issuerName", "serialNumber"}) {
      for (String invalid : new String[] {"", "!", "AA=="}) {
        if (field.equals("serialNumber") && invalid.equals("AA==")) continue;
        JsonObject record = original.deepCopy();
        record.addProperty(field, invalid);
        assertThrows(IOException.class, () -> CertificateRevocations.load(json(policy(record))));
      }
      JsonObject missing = original.deepCopy();
      missing.remove(field);
      assertThrows(IOException.class, () -> CertificateRevocations.load(json(policy(missing))));
      JsonObject wrongType = original.deepCopy();
      wrongType.addProperty(field, 123);
      assertThrows(IOException.class, () -> CertificateRevocations.load(json(policy(wrongType))));
    }
    JsonObject shortHash = subjectRecord(certificate("leaf.pem"));
    shortHash.addProperty("pubKeyHash", b64(new byte[31]));
    assertThrows(IOException.class, () -> CertificateRevocations.load(json(policy(shortHash))));
    JsonObject ambiguous = original.deepCopy();
    ambiguous.addProperty("subject", original.get("issuerName").getAsString());
    assertThrows(IOException.class, () -> CertificateRevocations.load(json(policy(ambiguous))));
    JsonObject incomplete = subjectRecord(certificate("leaf.pem"));
    incomplete.remove("pubKeyHash");
    assertThrows(IOException.class, () -> CertificateRevocations.load(json(policy(incomplete))));
  }

  @Test
  void checkerClonesCheckBothDirectionsAndReportRevokedReason() throws Exception {
    PKIXCertPathChecker checker =
        CertificateRevocations.load(json(policy(subjectRecord(certificate("intermediate-a.pem")))))
            .newChecker();
    assertTrue(checker.isForwardCheckingSupported());
    assertTrue(checker.getSupportedExtensions().isEmpty());
    for (boolean forward : new boolean[] {true, false}) {
      PKIXCertPathChecker clone = (PKIXCertPathChecker) checker.clone();
      clone.init(forward);
      clone.check(certificate("leaf.pem"), Collections.emptySet());
      CertPathValidatorException error =
          assertThrows(
              CertPathValidatorException.class,
              () -> clone.check(certificate("intermediate-b.pem"), Collections.emptySet()));
      assertEquals(CertPathValidatorException.BasicReason.REVOKED, error.getReason());
      assertEquals("Certificate revoked by bundled Mozilla OneCRL", error.getMessage());
    }
  }

  @Test
  void checksIssuerSuppliedByPathBuilderRatherThanPeerChain() throws Exception {
    X509CertSelector leaf = new X509CertSelector();
    leaf.setCertificate(certificate("leaf.pem"));
    PKIXBuilderParameters parameters =
        new PKIXBuilderParameters(
            Collections.singleton(new TrustAnchor(certificate("root-a.pem"), null)), leaf);
    parameters.setRevocationEnabled(false);
    parameters.addCertStore(
        CertStore.getInstance(
            "Collection",
            new CollectionCertStoreParameters(
                Arrays.asList(certificate("leaf.pem"), certificate("intermediate-a.pem")))));
    assertEquals(
        2,
        CertPathBuilder.getInstance("PKIX")
            .build(parameters)
            .getCertPath()
            .getCertificates()
            .size());
    parameters.addCertPathChecker(
        CertificateRevocations.load(json(policy(subjectRecord(certificate("intermediate-a.pem")))))
            .newChecker());
    assertThrows(
        CertPathBuilderException.class,
        () -> CertPathBuilder.getInstance("PKIX").build(parameters));
  }

  @Test
  void bundledFeedBlocksAllPublicCompromisedTlsIdentities() throws Exception {
    publicRevocations();
  }
}
