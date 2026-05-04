package net.technicpack.utilslib;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class Urls {
  private Urls() {
    throw new IllegalStateException("Utility class");
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
