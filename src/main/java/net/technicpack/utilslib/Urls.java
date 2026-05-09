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
import java.util.Objects;

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
    Objects.requireNonNull(input, "Urls.pathSegment: input must not be null");
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
    Objects.requireNonNull(input, "Urls.formParameter: input must not be null");
    try {
      return URLEncoder.encode(input, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException impossible) {
      throw new AssertionError("UTF-8 must be supported", impossible);
    }
  }

  /**
   * Parse a URL string that may have arrived from upstream metadata with RFC 3986-illegal
   * characters in path/query/fragment (commonly {@code [}, {@code ]}, spaces in third-party Solder
   * mod-resource URLs). Tries strict {@link URI#URI(String)} first; if that succeeds AND {@link
   * URI#parseServerAuthority()} confirms a server-based authority, returns it unchanged. Otherwise
   * applies a narrow compatibility sanitizer that percent-encodes only the illegal raw characters
   * in each component while preserving existing valid {@code %XX} escapes (so already- encoded
   * inputs are NOT double-encoded). Authority text is never repaired (IDN/punycode is out of
   * scope).
   *
   * <p>Does NOT use {@link URL#URL(String)} for parsing.
   */
  public static URI parseDownloadUri(String raw) throws URISyntaxException {
    if (raw == null) {
      throw new URISyntaxException("<null>", "input must not be null");
    }
    URI uri;
    try {
      uri = new URI(raw).parseServerAuthority();
    } catch (URISyntaxException first) {
      uri = new URI(percentEncodeIllegalUriChars(raw)).parseServerAuthority();
    }
    if (uri.getScheme() == null) {
      throw new URISyntaxException(raw, "missing scheme");
    }
    if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new URISyntaxException(raw, "scheme must be http or https, was: " + uri.getScheme());
    }
    if (uri.getAuthority() == null) {
      throw new URISyntaxException(raw, "missing authority");
    }
    return uri;
  }

  static String percentEncodeIllegalUriChars(String raw) {
    StringBuilder out = new StringBuilder(raw.length());
    int index = copySchemeAndAuthority(raw, out);
    index = encodeUntil(raw, index, out, '?', '#', Urls::isPathChar);
    if (index < raw.length() && raw.charAt(index) == '?') {
      out.append('?');
      index = encodeUntil(raw, index + 1, out, '#', NO_DELIMITER, Urls::isQueryOrFragmentChar);
    }
    if (index < raw.length() && raw.charAt(index) == '#') {
      out.append('#');
      encodeUntil(raw, index + 1, out, NO_DELIMITER, NO_DELIMITER, Urls::isQueryOrFragmentChar);
    }
    return out.toString();
  }

  private static final int NO_DELIMITER = -1;

  private static int copySchemeAndAuthority(String raw, StringBuilder out) {
    int index = 0;
    int schemeEnd = raw.indexOf(':');
    if (schemeEnd >= 0) {
      out.append(raw, 0, schemeEnd + 1);
      index = schemeEnd + 1;
    }
    if (raw.startsWith("//", index)) {
      out.append("//");
      index += 2;
      while (index < raw.length()) {
        char c = raw.charAt(index);
        if (c == '/' || c == '?' || c == '#') {
          break;
        }
        out.append(c);
        index++;
      }
    }
    return index;
  }

  private static int encodeUntil(
      String raw,
      int index,
      StringBuilder out,
      int delimiter1,
      int delimiter2,
      CharPredicate allowed) {
    while (index < raw.length()) {
      char c = raw.charAt(index);
      if (c == delimiter1 || c == delimiter2) {
        break;
      }
      if (c == '%'
          && index + 2 < raw.length()
          && isHex(raw.charAt(index + 1))
          && isHex(raw.charAt(index + 2))) {
        out.append(raw, index, index + 3);
        index += 3;
        continue;
      }
      int codePoint = raw.codePointAt(index);
      if (codePoint < 128 && allowed.test((char) codePoint)) {
        out.append((char) codePoint);
      } else {
        percentEncode(codePoint, out);
      }
      index += Character.charCount(codePoint);
    }
    return index;
  }

  private static boolean isPathChar(char c) {
    return isUnreserved(c) || isSubDelimiter(c) || c == ':' || c == '@' || c == '/';
  }

  private static boolean isQueryOrFragmentChar(char c) {
    return isPathChar(c) || c == '?';
  }

  private static boolean isUnreserved(char c) {
    return (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || (c >= '0' && c <= '9')
        || c == '-'
        || c == '.'
        || c == '_'
        || c == '~';
  }

  private static boolean isSubDelimiter(char c) {
    return c == '!' || c == '$' || c == '&' || c == '\'' || c == '(' || c == ')' || c == '*'
        || c == '+' || c == ',' || c == ';' || c == '=';
  }

  private static boolean isHex(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static void percentEncode(int codePoint, StringBuilder out) {
    byte[] bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
    for (byte b : bytes) {
      out.append('%');
      int value = b & 0xff;
      out.append(Character.toUpperCase(Character.forDigit(value >>> 4, 16)));
      out.append(Character.toUpperCase(Character.forDigit(value & 0x0f, 16)));
    }
  }

  private interface CharPredicate {
    boolean test(char c);
  }

  /**
   * Parse {@code input} into a strict-URI-clean {@link URL} via {@link #parseDownloadUri}, and
   * report a Sentry warning when the input was non-strict (i.e., the sanitizer fallback was
   * needed). Used at call sites that construct URLs from launcher-controlled templates: any
   * sanitization here indicates a missed encoding spot upstream of the parser, which is a bug to
   * surface and fix.
   *
   * <p>See {@code project_launcher_url_uri_revert.md} memory entry for the migration history.
   */
  public static URL parseAndDiagnose(String input, String callSite) throws MalformedURLException {
    if (input == null) {
      throw new MalformedURLException("input must not be null");
    }
    boolean needsSanitization = false;
    try {
      new URI(input).parseServerAuthority();
    } catch (URISyntaxException probe) {
      needsSanitization = true;
    }
    URL result;
    try {
      result = parseDownloadUri(input).toURL();
    } catch (URISyntaxException e) {
      MalformedURLException wrapped =
          new MalformedURLException("URL did not parse: " + e.getMessage());
      wrapped.initCause(e);
      throw wrapped;
    }
    if (needsSanitization) {
      final URL sanitized = result;
      try {
        Sentry.captureMessage(
            "URL needed sanitization at " + callSite,
            SentryLevel.WARNING,
            scope -> {
              scope.setTag("url-uri-diagnostic", "true");
              scope.setTag("url-uri-call-site", callSite);
              scope.setExtra("input-url", input);
              scope.setExtra("sanitized-url", sanitized.toExternalForm());
            });
      } catch (RuntimeException ignored) {
        // Diagnostics must never break the calling code.
      }
    }
    return result;
  }
}
