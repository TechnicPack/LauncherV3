package net.technicpack.minecraftcore.mojang.version.io;

import java.util.Objects;

@SuppressWarnings({"unused"})
public class Artifact {
  private String url;
  private String sha1;
  private long size;
  private String path;

  private Artifact() {}

  public Artifact(String url, String sha1, long size) {
    this.url = url;
    this.sha1 = sha1;
    this.size = size;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Artifact artifact = (Artifact) o;
    return size == artifact.size
        && Objects.equals(url, artifact.url)
        && Objects.equals(sha1, artifact.sha1)
        && Objects.equals(path, artifact.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, sha1, size, path);
  }

  public String getUrl() {
    return url;
  }

  public String getSha1() {
    return sha1;
  }

  public long getSize() {
    return size;
  }

  public String getPath() {
    return path;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
