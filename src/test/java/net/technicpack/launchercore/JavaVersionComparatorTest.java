package net.technicpack.launchercore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaVersionComparatorTest {
  private final JavaVersionComparator comparator = new JavaVersionComparator();

  // --- Legacy full form (1.x.0_update) ---

  @Test
  void legacyFullFormEqual() {
    assertEquals(0, comparator.compare("1.8.0_221", "1.8.0_221"));
  }

  @Test
  void legacyFullFormHigherUpdate() {
    assertTrue(comparator.compare("1.8.0_381", "1.8.0_221") > 0);
  }

  @Test
  void legacyFullFormLowerMajor() {
    assertTrue(comparator.compare("1.7.0_80", "1.8.0_221") < 0);
  }

  // --- Legacy short form (1.x) ---

  @Test
  void legacyShortFormEqual() {
    assertEquals(0, comparator.compare("1.8", "1.8"));
  }

  @Test
  void legacyShortFormHigher() {
    assertTrue(comparator.compare("1.8", "1.7") > 0);
  }

  // --- Legacy short vs full ---

  @Test
  void legacyShortVsFullSameMajor() {
    // 1.8 should be <= 1.8.0_221 (no update vs update 221)
    assertTrue(comparator.compare("1.8", "1.8.0_221") < 0);
  }

  @Test
  void legacyFullVsShortSameMajor() {
    assertTrue(comparator.compare("1.8.0_221", "1.8") > 0);
  }

  @Test
  void legacyShortVsFullDifferentMajor() {
    // Solder sends "1.8", user has "1.7.0_80" — should fail the check
    assertTrue(comparator.compare("1.7.0_80", "1.8") < 0);
  }

  @Test
  void legacyShortHigherThanFullLowerMajor() {
    // Solder sends "1.7", user has "1.8.0_221" — should pass
    assertTrue(comparator.compare("1.8.0_221", "1.7") > 0);
  }

  // --- Modern versions ---

  @Test
  void modernEqual() {
    assertEquals(0, comparator.compare("17", "17"));
  }

  @Test
  void modernWithMinor() {
    assertTrue(comparator.compare("17.0.2", "17.0.1") > 0);
  }

  @Test
  void modernHigherMajor() {
    assertTrue(comparator.compare("21", "17") > 0);
  }

  // --- Cross-format: legacy vs modern ---

  @Test
  void modernHigherThanLegacyFull() {
    // Java 17 > Java 8
    assertTrue(comparator.compare("17.0.2", "1.8.0_381") > 0);
  }

  @Test
  void legacyFullLowerThanModern() {
    // Java 8 < Java 17
    assertTrue(comparator.compare("1.8.0_381", "17") < 0);
  }

  @Test
  void modernHigherThanLegacyShort() {
    assertTrue(comparator.compare("17", "1.8") > 0);
  }

  @Test
  void legacyShortLowerThanModern() {
    assertTrue(comparator.compare("1.8", "17") < 0);
  }

  // --- Solder enum values against real Java versions ---
  // Solder JavaVersionsEnum: '1.6', '1.7', '1.8', '16', '17'

  @Test
  void solderJre6VsUserJava6() {
    assertTrue(comparator.compare("1.6.0_45", "1.6") >= 0);
  }

  @Test
  void solderJre6VsUserJava8() {
    assertTrue(comparator.compare("1.8.0_221", "1.6") >= 0);
  }

  @Test
  void solderJre7VsUserJava6() {
    assertTrue(comparator.compare("1.6.0_45", "1.7") < 0);
  }

  @Test
  void solderJre7VsUserJava8() {
    assertTrue(comparator.compare("1.8.0_221", "1.7") >= 0);
  }

  @Test
  void solderJre8VsUserJava8() {
    assertTrue(comparator.compare("1.8.0_221", "1.8") >= 0);
  }

  @Test
  void solderJre8VsUserJava7() {
    assertTrue(comparator.compare("1.7.0_80", "1.8") < 0);
  }

  @Test
  void solderJre8VsUserJava17() {
    assertTrue(comparator.compare("17.0.2", "1.8") >= 0);
  }

  @Test
  void solderJre16VsUserJava8() {
    assertTrue(comparator.compare("1.8.0_381", "16") < 0);
  }

  @Test
  void solderJre16VsUserJava16() {
    assertTrue(comparator.compare("16.0.1", "16") >= 0);
  }

  @Test
  void solderJre16VsUserJava17() {
    assertTrue(comparator.compare("17.0.2", "16") >= 0);
  }

  @Test
  void solderJre17VsUserJava17() {
    assertTrue(comparator.compare("17.0.2", "17") >= 0);
  }

  @Test
  void solderJre17VsUserJava8() {
    assertTrue(comparator.compare("1.8.0_381", "17") < 0);
  }

  @Test
  void solderJre17VsUserJava21() {
    assertTrue(comparator.compare("21.0.1", "17") >= 0);
  }

  // --- Java 9 transition (used in Installer.java as "1.9") ---

  @Test
  void java9ShortLegacyVsUserJava8() {
    // Installer.java checks isJavaVersionAtLeast(version, "1.9")
    assertTrue(comparator.compare("1.8.0_381", "1.9") < 0);
  }

  @Test
  void java9ShortLegacyVsUserJava9Modern() {
    // "1.9" (legacy path → [9,0,0]) vs "9.0.4" (modern path → [9,0,4,0])
    assertTrue(comparator.compare("9.0.4", "1.9") >= 0);
  }

  @Test
  void java9ShortLegacyVsUserJava17() {
    assertTrue(comparator.compare("17.0.2", "1.9") >= 0);
  }

  // --- Edge cases ---

  @Test
  void legacyWithEaSuffix() {
    assertTrue(comparator.compare("1.8.0_221", "1.8.0_221-ea") == 0);
  }

  @Test
  void modernWithEaSuffix() {
    assertEquals(0, comparator.compare("17-ea", "17"));
  }

  @Test
  void modernWithBuildSuffix() {
    assertEquals(0, comparator.compare("21.0.1+12", "21.0.1"));
  }

  @Test
  void legacyMiddleForm() {
    // "1.8.0" without update — should normalize to [8, 0, 0]
    assertEquals(0, comparator.compare("1.8.0", "1.8"));
  }

  @Test
  void legacyShortWithEaSuffix() {
    assertEquals(0, comparator.compare("1.8-ea", "1.8"));
  }

  @Test
  void legacyShortWithBuildSuffix() {
    assertEquals(0, comparator.compare("1.8+12", "1.8"));
  }

  @Test
  void legacyShortVsBareModernSameJava() {
    // "1.8" (legacy → [8,0,0]) vs "8" (modern → [8,0,0,0]) — same Java version
    assertEquals(0, comparator.compare("1.8", "8"));
  }

  @Test
  void invalidVersionThrows() {
    assertThrows(IllegalArgumentException.class, () -> comparator.compare("invalid", "17"));
  }

  // --- getMajor ---

  @Test
  void getMajorParsesLegacyFullForm() {
    assertEquals(8, comparator.getMajor("1.8.0_221"));
  }

  @Test
  void getMajorParsesLegacyShortForm() {
    assertEquals(8, comparator.getMajor("1.8"));
  }

  @Test
  void getMajorParsesModernShort() {
    assertEquals(21, comparator.getMajor("21"));
  }

  @Test
  void getMajorParsesModernFullWithBuild() {
    assertEquals(21, comparator.getMajor("21.0.4+7"));
  }

  @Test
  void getMajorThrowsOnGarbage() {
    // "8u51" used to be the canonical invalid input here, but Mojang's jre-legacy publishes
    // exactly that shape ("8u202") so it's now a recognized format. Use input with no valid
    // numeric prefix instead.
    assertThrows(IllegalArgumentException.class, () -> comparator.getMajor("not-a-version"));
  }

  // --- Mojang manifest formats (java-runtime-* and jre-legacy) ---

  @Test
  void getMajorParsesLegacyUForm() {
    assertEquals(8, comparator.getMajor("8u202"));
  }

  @Test
  void getMajorParsesLegacyUFormWithSuffix() {
    assertEquals(8, comparator.getMajor("8u51-cacert462b08"));
  }

  @Test
  void getMajorParsesFiveComponent() {
    assertEquals(16, comparator.getMajor("16.0.1.9.1"));
  }

  @Test
  void getMajorParsesFiveComponentWithBuild() {
    assertEquals(16, comparator.getMajor("16.0.1.9.1_3"));
  }

  @Test
  void getMajorParsesBetaFiveComponent() {
    assertEquals(17, comparator.getMajor("17.0.1.12.1"));
  }

  @Test
  void getMajorParsesGammaFourComponent() {
    assertEquals(17, comparator.getMajor("17.0.8.1"));
  }

  @Test
  void fiveComponentBuildHigherThanBare() {
    // "16.0.1.9.1_3" is build revision 3 of "16.0.1.9.1", so it orders higher.
    assertTrue(comparator.compare("16.0.1.9.1_3", "16.0.1.9.1") > 0);
  }

  @Test
  void fiveComponentHigherThanThreeComponentSameMajor() {
    assertTrue(comparator.compare("16.0.1.9.1", "16.0.1") > 0);
  }

  @Test
  void fiveComponentLexicographic() {
    // Lex compare: 17.0.1.12.1 vs 17.0.8.1 differ first at index 2 (1 < 8), so the first is lower.
    assertTrue(comparator.compare("17.0.1.12.1", "17.0.8.1") < 0);
  }

  @Test
  void legacyUEqualsLegacyDotted() {
    // "8u202" and "1.8.0_202" both normalize to [8,0,202].
    assertEquals(0, comparator.compare("8u202", "1.8.0_202"));
  }

  @Test
  void legacyULowerThanHigherUpdate() {
    assertTrue(comparator.compare("8u202", "1.8.0_221") < 0);
  }

  @Test
  void legacyUSuffixIgnored() {
    // Trailing "-cacert462b08" is metadata, parallel to "-ea" on the dotted form.
    assertEquals(0, comparator.compare("8u51-cacert462b08", "8u51"));
  }

  @Test
  void fiveComponentLowerMajorThanModern() {
    assertTrue(comparator.compare("16.0.1.9.1", "17") < 0);
  }

  @Test
  void threeComponentOrderingStillWorks() {
    // Sanity check: pre-existing forms still compare correctly after parser change.
    assertTrue(comparator.compare("21.0.7", "17.0.15") > 0);
  }
}
