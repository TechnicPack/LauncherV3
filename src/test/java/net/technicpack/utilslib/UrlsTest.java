package net.technicpack.utilslib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.jupiter.api.Test;

class UrlsTest {

  @Test
  void pathSegmentEncodesSpaceAsPercent20() {
    assertEquals("hello%20world", Urls.pathSegment("hello world"));
  }

  @Test
  void pathSegmentEncodesLiteralPlusAsPercent2B() {
    assertEquals("a%2Bb", Urls.pathSegment("a+b"));
  }

  @Test
  void pathSegmentEncodesUnicodeAsUtf8() {
    assertEquals("caf%C3%A9", Urls.pathSegment("café"));
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
  void formParameterEncodesUnicodeAsUtf8() {
    assertEquals("caf%C3%A9", Urls.formParameter("café"));
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

  @Test
  void parseDownloadUriPassesThroughStrictlyValidUrl() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/a/b-._~?x=1&y=2#frag");
    assertEquals("https://example.com/a/b-._~?x=1&y=2#frag", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesBracketsInPath() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/path/[solder]asset.zip");
    assertEquals("https://example.com/path/%5Bsolder%5Dasset.zip", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriPreservesAlreadyEncodedSequences() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/a%20b/%5Bx%5D");
    assertEquals("https://example.com/a%20b/%5Bx%5D", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesIllegalCharactersAcrossPathQueryFragment() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/a b/[x]?q=a b&x=[y]#frag ment");
    assertEquals(
        "https://example.com/a%20b/%5Bx%5D?q=a%20b&x=%5By%5D#frag%20ment", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriPreservesEscapesInPathQueryAndFragment() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/[x]?q=a%20b#f%5Bx%5D");
    assertEquals("https://example.com/%5Bx%5D?q=a%20b#f%5Bx%5D", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriPreservesIpv6AuthorityBrackets() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("http://[::1]/a[x]");
    assertEquals("http://[::1]/a%5Bx%5D", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesInvalidPercentSign() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/a%zzb");
    assertEquals("https://example.com/a%25zzb", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesInvalidPercentInQueryAndFragment() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/path?q=100%done#frag%zz");
    assertEquals("https://example.com/path?q=100%25done#frag%25zz", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriPreservesLiteralPlusInPath() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/a+b.zip");
    assertEquals("https://example.com/a+b.zip", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriPreservesRawQuestionAndSlashInQueryValue() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/file?next=/a/b?fallback=z");
    assertEquals("https://example.com/file?next=/a/b?fallback=z", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriHandlesEmptyPathWithQuery() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com?foo=bar");
    assertEquals("https://example.com?foo=bar", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesUnicodeAndIllegalsInPath() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/café/[x]");
    assertEquals("https://example.com/caf%C3%A9/%5Bx%5D", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesNulInPathWithoutTruncating() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/path\u0000more/[x]");
    assertEquals("https://example.com/path%00more/%5Bx%5D", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesNulInQueryWithoutTruncating() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/path?q=a\u0000b[x]");
    assertEquals("https://example.com/path?q=a%00b%5Bx%5D", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriEncodesNulInFragmentWithoutTruncating() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("https://example.com/path#frag\u0000tail[x]");
    assertEquals("https://example.com/path#frag%00tail%5Bx%5D", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriRejectsRelativeInput() {
    assertThrows(URISyntaxException.class, () -> Urls.parseDownloadUri("not-a-url"));
  }

  @Test
  void parseDownloadUriRejectsAuthorityLessPath() {
    assertThrows(URISyntaxException.class, () -> Urls.parseDownloadUri("/path/file.zip"));
  }

  @Test
  void parseDownloadUriRejectsSingleSlashAfterScheme() {
    assertThrows(
        URISyntaxException.class, () -> Urls.parseDownloadUri("https:/example.com/file.zip"));
  }

  @Test
  void parseDownloadUriRejectsAuthorityWithSpace() {
    assertThrows(
        URISyntaxException.class, () -> Urls.parseDownloadUri("https://exa mple.com/a[x]"));
  }

  @Test
  void parseDownloadUriRejectsAuthorityWithBadPort() {
    assertThrows(
        URISyntaxException.class, () -> Urls.parseDownloadUri("https://example.com:bad/a[x]"));
  }

  @Test
  void parseDownloadUriRejectsMalformedIpv6() {
    assertThrows(URISyntaxException.class, () -> Urls.parseDownloadUri("https://[::1/path"));
  }

  @Test
  void parseDownloadUriRejectsFtpScheme() {
    assertThrows(
        URISyntaxException.class, () -> Urls.parseDownloadUri("ftp://example.com/file.zip"));
  }

  @Test
  void parseDownloadUriRejectsFileScheme() {
    assertThrows(URISyntaxException.class, () -> Urls.parseDownloadUri("file:///etc/passwd"));
  }

  @Test
  void parseDownloadUriAcceptsHttpScheme() throws URISyntaxException {
    URI uri = Urls.parseDownloadUri("http://example.com/foo.jar");
    assertEquals("http://example.com/foo.jar", uri.toASCIIString());
  }

  @Test
  void parseDownloadUriNullThrowsUriSyntaxException() {
    URISyntaxException ex =
        assertThrows(URISyntaxException.class, () -> Urls.parseDownloadUri(null));
    assertNotNull(ex.getMessage());
  }

  @Test
  void parseDownloadUriProductionPixelmonResourceUrl() throws URISyntaxException {
    URI uri =
        Urls.parseDownloadUri(
            "https://download.nodecdn.net/containers/reforged/resources/music/9.0.11/[solder]ThePixelmonOST.zip.zip");
    assertEquals(
        "https://download.nodecdn.net/containers/reforged/resources/music/9.0.11/%5Bsolder%5DThePixelmonOST.zip.zip",
        uri.toASCIIString());
  }

  @Test
  void parseAndDiagnoseReturnsUrlForStrictlyValidInput() throws MalformedURLException {
    URL url = Urls.parseAndDiagnose("https://example.com/foo.jar", "test-callsite");
    assertEquals("https://example.com/foo.jar", url.toExternalForm());
  }

  @Test
  void parseAndDiagnoseSanitizesUriIllegalInput() throws MalformedURLException {
    URL url = Urls.parseAndDiagnose("https://example.com/[x]", "test-callsite");
    assertEquals("https://example.com/%5Bx%5D", url.toExternalForm());
  }

  @Test
  void parseAndDiagnoseRejectsNullInput() {
    assertThrows(MalformedURLException.class, () -> Urls.parseAndDiagnose(null, "test-callsite"));
  }

  @Test
  void parseAndDiagnoseWrapsUnrepairableUriAsMalformedUrl() {
    MalformedURLException ex =
        assertThrows(
            MalformedURLException.class,
            () -> Urls.parseAndDiagnose("https://exa mple.com/[x]", "test-callsite"));
    assertNotNull(ex.getCause());
    assertInstanceOf(URISyntaxException.class, ex.getCause());
  }
}
