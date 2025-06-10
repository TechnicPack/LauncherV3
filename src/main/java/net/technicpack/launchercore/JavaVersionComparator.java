package net.technicpack.launchercore;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A comparator for Java version strings, supporting both legacy and modern formats.
 * It can compare versions like "1.8.0_221", "11.0.10", "17", "21.0.1+12", "22-ea+1", etc.
 */
public class JavaVersionComparator implements Comparator<String> {

    // Legacy: 1.<major>.0_<update> (optionally with -ea, -open, +12, etc)
    private static final Pattern LEGACY_PATTERN = Pattern.compile("^1\\.(\\d+)\\.0_(\\d+)(?:[-+].*)?$");

    // Modern: <major>(.<minor>(.<security>(.<patch>)?)?)? (optionally with -ea, -open, +12, etc)
    private static final Pattern NEW_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+].*)?$");

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
            int update = Integer.parseInt(legacy.group(2));
            // Normalize: legacy 1.8.0_221 -> [8,0,221]
            return new int[]{major, 0, update};
        }

        Matcher modern = NEW_PATTERN.matcher(version);
        if (modern.matches()) {
            int[] parts = new int[4];
            for (int i = 1; i <= 4; i++) {
                String group = modern.group(i);
                parts[i - 1] = (group != null) ? Integer.parseInt(group) : 0;
            }
            return parts;
        }

        throw new IllegalArgumentException("Invalid Java version string: " + version);
    }
}
