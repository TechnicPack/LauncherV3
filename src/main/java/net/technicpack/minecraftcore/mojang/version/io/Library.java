/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.annotations.SerializedName;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.utilslib.JavaUtils;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;

@SuppressWarnings({"unused"})
public class Library {
  private static final String[] FALLBACK = {
    "https://libraries.minecraft.net/",
    "https://maven.minecraftforge.net/",
    "https://mirror.technicpack.net/Technic/lib/",
    "https://maven.creeperhost.net/",
  };
  private static final Pattern GRADLE_PATTERN =
      Pattern.compile("^([^:@]+):([^:@]+):([^:@]+)(?::([^:@]+))?(?:@([^:@]+))?$");
  private static final Pattern FORGE_MAVEN_ROOT =
      Pattern.compile(
          "^https://(?:files\\.minecraftforge\\.net/maven|maven\\.minecraftforge\\.net)/(.+)$");
  private static final String MCFORGE_MAVEN_MIRROR = "https://maven.creeperhost.net/";

  // JSON fields
  private String name;
  private List<Rule> rules;
  private Downloads downloads;
  private Map<String, String> natives;
  private ExtractRules extract;
  private String url;

  @SerializedName("MMC-hint")
  private String mmcHint;

  // Gradle specifier/Maven coordinates
  // groupId:artifactId:version[:classifier][@extension]
  private transient String gradleGroupId;
  private transient String gradleArtifactId;
  private transient String gradleVersion;
  private transient String gradleClassifier;
  private transient String gradleExtension;

  public String getNormalizedName() {
    return name.contains("@") ? name : name + "@jar";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Library library = (Library) o;
    // Ensure Maven info is parsed since we're matching based on that
    this.ensureNameIsParsed();
    library.ensureNameIsParsed();

    return Objects.equals(gradleGroupId, library.gradleGroupId)
        && Objects.equals(gradleArtifactId, library.gradleArtifactId)
        && Objects.equals(gradleClassifier, library.gradleClassifier)
        && Objects.equals(gradleExtension, library.gradleExtension)
        && Objects.equals(rules, library.rules)
        && Objects.equals(natives, library.natives);
  }

  @Override
  public int hashCode() {
    ensureNameIsParsed();
    return Objects.hash(
        gradleGroupId, gradleArtifactId, gradleClassifier, gradleExtension, rules, natives);
  }

  public Library() {}

  public Library(String name) {
    setName(name);
  }

  public Library(String name, String artifactUrl, String artifactSha1, int artifactSize) {
    setName(name);

    downloads = new Downloads(artifactUrl, artifactSha1, artifactSize);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isLocal() {
    return "local".equals(mmcHint);
  }

  /**
   * Resolve the local library path within a base directory. Checks flat layout (filename only)
   * first, then Maven layout (group/artifact/version/filename). Returns null if not found.
   */
  public Path resolveLocalPath(Path baseDir) {
    Path libDir = baseDir.resolve("libraries");

    // Flat layout: libraries/lwjgl3ify-2.1.16-forgePatches.jar
    Path flat = libDir.resolve(getArtifactFilename(null));
    if (Files.isRegularFile(flat)) return flat;

    // Maven layout:
    // libraries/com/github/GTNewHorizons/lwjgl3ify/2.1.16/lwjgl3ify-2.1.16-forgePatches.jar
    Path maven = libDir.resolve(getArtifactPath(null));
    if (Files.isRegularFile(maven)) return maven;

    return null;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;

    // Trigger name reparse to update the gradle info
    parseName();
  }

  public List<Rule> getRules() {
    return rules;
  }

  public Map<String, String> getNatives() {
    return natives;
  }

  public ExtractRules getExtract() {
    return extract;
  }

  private void ensureNameIsParsed() {
    // Don't reparse if it's already been parsed
    if (gradleGroupId != null) return;

    parseName();
  }

  private void parseName() {
    Matcher m = GRADLE_PATTERN.matcher(name);

    if (!m.matches()) {
      throw new IllegalStateException("Cannot parse invalid gradle specifier: " + name);
    }

    gradleGroupId = m.group(1);
    gradleArtifactId = m.group(2);
    gradleVersion = m.group(3);
    gradleClassifier = m.group(4);
    String extension = m.group(5);
    if (extension != null && !extension.isEmpty()) {
      gradleExtension = extension;
    } else {
      gradleExtension = "jar";
    }
  }

  public String getGradleGroup() {
    ensureNameIsParsed();

    return gradleGroupId;
  }

  public String getGradleArtifact() {
    ensureNameIsParsed();

    return gradleArtifactId;
  }

  public String getGradleVersion() {
    ensureNameIsParsed();

    return gradleVersion;
  }

  public String getGradleClassifier() {
    ensureNameIsParsed();

    return gradleClassifier;
  }

  public boolean isForCurrentOS(ILaunchOptions options, IJavaRuntime runtime) {
    if (rules == null) {
      return true;
    }
    return Rule.isAllowable(rules, options, runtime);
  }

  public boolean hasNatives() {
    return natives != null && !natives.isEmpty();
  }

  public String resolveNativeClassifierForCurrentOs() {
    return resolveNativeClassifier(OperatingSystem.getOperatingSystem().getName(), JavaUtils.OS_ARCH);
  }

  public String resolveNativeClassifier(String operatingSystemName, String osArch) {
    if (!hasNatives()) {
      return null;
    }

    String normalizedArchitecture = JavaUtils.normalizeNativeArchitecture(osArch);
    if (normalizedArchitecture != null) {
      String architectureSpecificClassifier =
          natives.get(operatingSystemName + "-" + normalizedArchitecture);
      if (architectureSpecificClassifier != null) {
        return architectureSpecificClassifier;
      }
    }

    return natives.get(operatingSystemName);
  }

  public boolean shouldAppearOnClasspath() {
    return !hasNatives();
  }

  public String getArtifactPath() {
    return getArtifactPath(null);
  }

  public String getArtifactPath(String nativeClassifier) {
    if (this.name == null) {
      throw new IllegalStateException("Cannot get artifact path of empty/blank artifact");
    }

    // Make sure the gradle specifier is parsed
    ensureNameIsParsed();

    String filename = getArtifactFilename(nativeClassifier);

    return gradleGroupId.replace('.', '/')
        + '/'
        + gradleArtifactId
        + '/'
        + gradleVersion
        + '/'
        + filename;
  }

  public String getArtifactFilename(String nativeClassifier) {
    if (this.name == null) {
      throw new IllegalStateException("Cannot get artifact filename of empty/blank artifact");
    }

    // Make sure the gradle specifier is parsed
    ensureNameIsParsed();

    String filename = gradleArtifactId + '-' + gradleVersion;

    // The native classifier overrides the regular classifier
    if (nativeClassifier != null) filename += '-' + nativeClassifier;
    else if (gradleClassifier != null && !gradleClassifier.isEmpty())
      filename += '-' + gradleClassifier;

    filename += '.' + gradleExtension;

    return filename;
  }

  public String getArtifactSha1(String nativeClassifier) {
    if (downloads == null) return null;

    Artifact artifact;

    if (nativeClassifier != null) artifact = downloads.getClassifier(nativeClassifier);
    else artifact = downloads.getArtifact();

    if (artifact != null) return artifact.getSha1();

    return null;
  }

  public String getDownloadUrl(String path) throws DownloadException {
    Set<String> possibleUrls = new LinkedHashSet<>(8);

    // Check the old-style URL (Forge 1.6, I think?)
    // It acts as a Maven root URL
    if (this.url != null) {
      possibleUrls.add(this.url + path);
    }

    // Check if an artifact URL is specified (downloads -> artifact -> url), only if it doesn't have
    // natives
    // This is a fully specified URL
    if (!hasNatives()) {
      String artifactUrl = null;

      if (downloads != null) {
        Artifact artifact = downloads.getArtifact();

        if (artifact != null) artifactUrl = artifact.getUrl();
      }

      if (artifactUrl != null && !artifactUrl.isEmpty()) {
        // Check if this URL is in Minecraft Forge's Maven repo and add ours as a primary mirror
        Matcher m = FORGE_MAVEN_ROOT.matcher(artifactUrl);
        if (m.matches()) {
          possibleUrls.add(TechnicConstants.TECHNIC_LIB_REPO + m.group(1));
          possibleUrls.add(MCFORGE_MAVEN_MIRROR + m.group(1));
        }

        possibleUrls.add(artifactUrl);
      }
    }

    // Check if any fallback mirrors we know of have this library
    // These also work as a Maven root URL
    for (String string : FALLBACK) {
      possibleUrls.add(string + path);
    }

    for (String possibleUrl : possibleUrls) {
      if (Utils.pingHttpURL(possibleUrl)) {
        return possibleUrl;
      }
    }

    throw new DownloadException("Failed to download library " + path + ": no mirror found");
  }

  public boolean isMinecraftForge() {
    // "net.minecraftforge:minecraftforge:" is used by MC 1.6.x versions
    return name.startsWith("net.minecraftforge:forge:")
        || name.startsWith("net.minecraftforge:minecraftforge:");
  }

  public boolean isLog4j() {
    return name.startsWith("org.apache.logging.log4j:");
  }

  public Downloads getDownloads() {
    return downloads;
  }

  public void setDownloads(Downloads downloads) {
    this.downloads = downloads;
  }
}
