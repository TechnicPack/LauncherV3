package net.technicpack.minecraftcore;

import java.util.*;

/**
 * Provides a data-driven mapping of Minecraft version to required FML (Forge Mod Loader) libraries.
 * Usage:
 *   Map<String, String> fmlLibs = FmlLibsManager.getLibsForVersion("1.5.2");
 */
public class FmlLibsManager {
    private static final Map<String, Map<String, String>> versionFmlLibs = new HashMap<>();

    static {
        // Base for 1.3.2
        Map<String, String> libs132 = new LinkedHashMap<>();
        libs132.put("argo-2.25.jar", "bb672829fde76cb163004752b86b0484bd0a7f4b");
        libs132.put("guava-12.0.1.jar", "b8e78b9af7bf45900e14c6f958486b6ca682195f");
        libs132.put("asm-all-4.0.jar", "98308890597acb64047f7e896638e0d98753ae82");
        versionFmlLibs.put("1.3.2", libs132);

        // Base for 1.4.x (inherits 1.3.2)
        Map<String, String> libs14x = new LinkedHashMap<>(libs132);
        libs14x.put("bcprov-jdk15on-147.jar", "b6f5d9926b0afbde9f4dbe3db88c5247be7794bb");
        for (String v : Arrays.asList("1.4", "1.4.1", "1.4.2", "1.4.3", "1.4.4", "1.4.5", "1.4.6", "1.4.7")) {
            versionFmlLibs.put(v, libs14x);
        }

        // Base for 1.5.x
        Map<String, String> libs15xBase = new LinkedHashMap<>();
        libs15xBase.put("argo-small-3.2.jar", "58912ea2858d168c50781f956fa5b59f0f7c6b51");
        libs15xBase.put("guava-14.0-rc3.jar", "931ae21fa8014c3ce686aaa621eae565fefb1a6a");
        libs15xBase.put("asm-all-4.1.jar", "054986e962b88d8660ae4566475658469595ef58");
        libs15xBase.put("bcprov-jdk15on-148.jar", "960dea7c9181ba0b17e8bab0c06a43f0a5f04e65");
        libs15xBase.put("scala-library.jar", "458d046151ad179c85429ed7420ffb1eaf6ddf85");

        // 1.5, 1.5.1, 1.5.2 with their specific file
        Map<String, String> libs15 = new LinkedHashMap<>(libs15xBase);
        libs15.put("deobfuscation_data_1.5.zip", "5f7c142d53776f16304c0bbe10542014abad6af8");
        versionFmlLibs.put("1.5", libs15);

        Map<String, String> libs151 = new LinkedHashMap<>(libs15xBase);
        libs151.put("deobfuscation_data_1.5.1.zip", "22e221a0d89516c1f721d6cab056a7e37471d0a6");
        versionFmlLibs.put("1.5.1", libs151);

        Map<String, String> libs152 = new LinkedHashMap<>(libs15xBase);
        libs152.put("deobfuscation_data_1.5.2.zip", "446e55cd986582c70fcf12cb27bc00114c5adfd9");
        versionFmlLibs.put("1.5.2", libs152);
    }

    /**
     * Retrieves a copy of the FML libraries for the specified Minecraft version.
     * @param version Minecraft version string, e.g. "1.5.2"
     * @return Map of library file name to hash, or empty map if none are applicable
     */
    public static Map<String, String> getLibsForVersion(String version) {
        Map<String, String> libs = versionFmlLibs.get(version);
        return libs != null ? new LinkedHashMap<>(libs) : Collections.emptyMap();
    }
}
