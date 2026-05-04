package net.technicpack.utilslib;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class Urls {
  private Urls() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Percent-encode a string for inclusion in a URI path segment. Java's URLEncoder produces
   * application/x-www-form-urlencoded output (space encoded as {@code +}), which is the wrong
   * encoding for a path segment (where {@code +} is the literal plus character per RFC 3986).
   * Re-mapping {@code +} to {@code %20} after URLEncoder converts form-encoding to path-encoding
   * unambiguously: any literal {@code +} in the input was already encoded as {@code %2B} by
   * URLEncoder, so the only {@code +} characters left in the output came from spaces.
   */
  public static String pathSegment(String input) {
    try {
      return URLEncoder.encode(input, StandardCharsets.UTF_8.name()).replace("+", "%20");
    } catch (UnsupportedEncodingException impossible) {
      throw new AssertionError("UTF-8 must be supported", impossible);
    }
  }

  /**
   * Percent-encode a string for inclusion as an {@code application/x-www-form-urlencoded} query
   * parameter value (so a space becomes {@code +}). Matches Java's {@link URLEncoder} semantics but
   * hides the {@link UnsupportedEncodingException} that's unreachable for UTF-8.
   */
  public static String formParameter(String input) {
    try {
      return URLEncoder.encode(input, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException impossible) {
      throw new AssertionError("UTF-8 must be supported", impossible);
    }
  }

  /**
   * Parse {@code input} via the legacy {@link URL#URL(String)} constructor while also probing
   * whether the strict {@link URI#URI(String)} parser would accept the same string. When URI
   * rejects the input, captures a Sentry warning with the offending URL in extras and the call site
   * as a tag, so we can identify the real-world inputs that block migrating these sites away from
   * the deprecated URL constructor. The returned URL still uses the legacy path so existing callers
   * keep working unchanged.
   *
   * <p>This is diagnostic-only. See {@code project_launcher_url_uri_revert.md} memory entry for the
   * migration history and follow-up plan.
   */
  public static URL parseAndDiagnose(String input, String callSite) throws MalformedURLException {
    URL result = new URL(input);
    try {
      new URI(input);
    } catch (URISyntaxException uriError) {
      try {
        Sentry.captureMessage(
            "URL parses with URL(String) but would fail new URI() at " + callSite,
            SentryLevel.WARNING,
            scope -> {
              scope.setTag("url-uri-diagnostic", "true");
              scope.setTag("url-uri-call-site", callSite);
              scope.setExtra("input-url", input);
              scope.setExtra("uri-error", uriError.getMessage());
            });
      } catch (RuntimeException ignored) {
        // Diagnostics must never break the calling code.
      }
    }
    return result;
  }
}
