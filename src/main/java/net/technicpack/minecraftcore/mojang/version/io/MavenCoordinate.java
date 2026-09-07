package net.technicpack.minecraftcore.mojang.version.io;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An immutable Maven artifact identity, including its classifier and extension. */
public final class MavenCoordinate {
  private static final Pattern COORDINATE_PATTERN =
      Pattern.compile("^([^:@]+):([^:@]+):([^:@]+)(?::([^:@]+))?(?:@([^:@]+))?$");

  private final String group;
  private final String artifact;
  private final String version;
  private final String classifier;
  private final String extension;
  private final String path;

  private MavenCoordinate(
      String group, String artifact, String version, String classifier, String extension) {
    this.group = group;
    this.artifact = artifact;
    this.version = version;
    this.classifier = classifier;
    this.extension = extension;
    this.path =
        group.replace('.', '/')
            + '/'
            + artifact
            + '/'
            + version
            + '/'
            + artifact
            + '-'
            + version
            + (classifier == null ? "" : '-' + classifier)
            + '.'
            + extension;
  }

  public static MavenCoordinate parse(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Maven coordinate must not be null");
    }
    Matcher matcher = COORDINATE_PATTERN.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid Maven coordinate: " + value);
    }

    String group = matcher.group(1);
    for (String segment : group.split("\\.", -1)) {
      validateComponent(segment);
    }
    String artifact = matcher.group(2);
    String version = matcher.group(3);
    String classifier = matcher.group(4);
    String extension = matcher.group(5) == null ? "jar" : matcher.group(5);
    validateComponent(artifact);
    validateComponent(version);
    if (classifier != null) {
      validateComponent(classifier);
    }
    validateComponent(extension);
    return new MavenCoordinate(group, artifact, version, classifier, extension);
  }

  public MavenCoordinate withClassifier(String classifier) {
    if (classifier != null) {
      validateComponent(classifier);
    }
    if (Objects.equals(this.classifier, classifier)) {
      return this;
    }
    return new MavenCoordinate(group, artifact, version, classifier, extension);
  }

  public String getGroup() {
    return group;
  }

  public String getArtifact() {
    return artifact;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getExtension() {
    return extension;
  }

  public String getPath() {
    return path;
  }

  /** Resolves a safe repository-relative path beneath an absolute, normalized root. */
  public static Path resolve(Path root, String relativePath) {
    if (root == null) {
      throw new IllegalArgumentException("Maven repository root must not be null");
    }
    validateRelativePath(relativePath);
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path target = normalizedRoot.resolve(relativePath).normalize();
    if (target.equals(normalizedRoot) || !target.startsWith(normalizedRoot)) {
      throw new IllegalArgumentException("Maven artifact path escapes repository: " + relativePath);
    }
    return target;
  }

  static void validateRelativePath(String relativePath) {
    if (relativePath == null) {
      throw new IllegalArgumentException("Maven artifact path must not be null");
    }
    for (String segment : relativePath.split("/", -1)) {
      validateComponent(segment);
    }
  }

  private static void validateComponent(String component) {
    if (component.isEmpty()
        || component.equals(".")
        || component.equals("..")
        || component.endsWith(".")
        || component.endsWith(" ")) {
      throw new IllegalArgumentException("Invalid Maven path component: " + component);
    }
    for (int i = 0; i < component.length(); i++) {
      char character = component.charAt(i);
      if (Character.isISOControl(character)
          || character == '/'
          || character == '\\'
          || character == ':'
          || character == '@'
          || character == '<'
          || character == '>'
          || character == '"'
          || character == '|'
          || character == '?'
          || character == '*') {
        throw new IllegalArgumentException("Invalid Maven path component: " + component);
      }
    }
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof MavenCoordinate)) return false;
    MavenCoordinate coordinate = (MavenCoordinate) other;
    return group.equals(coordinate.group)
        && artifact.equals(coordinate.artifact)
        && version.equals(coordinate.version)
        && Objects.equals(classifier, coordinate.classifier)
        && extension.equals(coordinate.extension);
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, artifact, version, classifier, extension);
  }

  @Override
  public String toString() {
    return group
        + ':'
        + artifact
        + ':'
        + version
        + (classifier == null ? "" : ':' + classifier)
        + '@'
        + extension;
  }
}
