package net.technicpack.utilslib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UrlsTest {

  @Test
  void pathSegmentEncodesSpaceAsPercent20() {
    assertEquals("Pixelmon%209.3.14", Urls.pathSegment("Pixelmon 9.3.14"));
  }

  @Test
  void pathSegmentEncodesLiteralPlusAsPercent2B() {
    assertEquals("a%2Bb", Urls.pathSegment("a+b"));
  }

  @Test
  void pathSegmentNullThrowsNpeIdentifyingTheHelper() {
    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> Urls.pathSegment(null));
    assertNotNull(ex.getMessage());
    assertTrue(
        ex.getMessage().contains("Urls.pathSegment"),
        "NPE message should name the helper, was: " + ex.getMessage());
  }

  @Test
  void formParameterEncodesSpaceAsPlus() {
    assertEquals("hello+world", Urls.formParameter("hello world"));
  }

  @Test
  void formParameterNullThrowsNpeIdentifyingTheHelper() {
    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> Urls.formParameter(null));
    assertNotNull(ex.getMessage());
    assertTrue(
        ex.getMessage().contains("Urls.formParameter"),
        "NPE message should name the helper, was: " + ex.getMessage());
  }
}
