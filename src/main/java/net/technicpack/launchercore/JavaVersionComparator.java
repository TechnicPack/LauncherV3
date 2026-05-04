package net.technicpack.launchercore;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A comparator for Java version strings, supporting both legacy and modern formats. It can compare
 * versions like "1.8.0_221", "11.0.10", "17", "21.0.1+12", "22-ea+1", "8u202", "16.0.1.9.1",
 * "16.0.1.9.1_3", etc. The 8uNNN form is normalized to [major, 0, update] so it sorts identically
 * to "1.8.0_NNN"; modern dotted forms are kept variable-length and compared lexicographically.
 */
public class JavaVersionComparator implements Comparator<String> {

  // Legacy: 1.<major>[.0[_<update>]] (optionally with -ea, -open, +12, etc)
  private static final Pattern LEGACY_PATTERN =
      Pattern.compile("^1\\.(\\d+)(?:\\.0(?:_(\\d+))?)?(?:[-+].*)?$");

  // Legacy <major>u<update> form (e.g. "8u202", "8u51-cacert462b08") used by Mojang's jre-legacy.
  private static final Pattern LEGACY_U_PATTERN =
      Pattern.compile("^(\\d+)u(\\d+)(?:[-+].*)?$");

  // Modern: <major>(.<n>)* with optional _<build> suffix (Microsoft's java-runtime-alpha
  // publishes "16.0.1.9.1" and "16.0.1.9.1_3"). Trailing -ea/-open/+12/etc still ignored.
  private static final Pattern NEW_PATTERN =
      Pattern.compile("^(\\d+(?:\\.\\d+)*)(?:_(\\d+))?(?:[-+].*)?$");

  /**
   * Returns the major Java version parsed from a version string.
   * E.g. "1.8.0_221" → 8, "21.0.4" → 21, "17-ea" → 17, "1.8" → 8.
   *
   * @throws IllegalArgumentException if the string isn't a recognized Java version format.
   */
  public int getMajor(String version) {
    return parseVersion(version)[0];
  }

  @Override
  public int compare(String v1, String v2) {
    int[] arr1 = parseVersion(v1);
    int[] arr2 = parseVersion(v2);

    int len1 = arr1.length;
    int len2 = arr2.length;

    int a;
    int b;

    int len = Math.max(len1, len2);
    for (int i = 0; i < len; i++) {
      a = i < len1 ? arr1[i] : 0;
      b = i < len2 ? arr2[i] : 0;
      if (a != b) return a - b;
    }
    return 0;
  }

  private int[] parseVersion(String version) {
    Matcher legacy = LEGACY_PATTERN.matcher(version);
    if (legacy.matches()) {
      int major = Integer.parseInt(legacy.group(1));
      int update = legacy.group(2) != null ? Integer.parseInt(legacy.group(2)) : 0;
      // Normalize: legacy 1.8.0_221 -> [8,0,221], 1.8 -> [8,0,0]
      return new int[] {major, 0, update};
    }

    Matcher legacyU = LEGACY_U_PATTERN.matcher(version);
    if (legacyU.matches()) {
      int major = Integer.parseInt(legacyU.group(1));
      int update = Integer.parseInt(legacyU.group(2));
      // Normalize: 8u202 -> [8,0,202], parallel to 1.8.0_202
      return new int[] {major, 0, update};
    }

    Matcher modern = NEW_PATTERN.matcher(version);
    if (modern.matches()) {
      String[] dotParts = modern.group(1).split("\\.");
      String build = modern.group(2);
      int[] parts = new int[dotParts.length + (build != null ? 1 : 0)];
      for (int i = 0; i < dotParts.length; i++) {
        parts[i] = Integer.parseInt(dotParts[i]);
      }
      if (build != null) {
        parts[dotParts.length] = Integer.parseInt(build);
      }
      return parts;
    }

    throw new IllegalArgumentException("Invalid Java version string: " + version);
  }
}
