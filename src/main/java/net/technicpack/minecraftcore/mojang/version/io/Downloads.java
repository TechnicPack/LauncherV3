package net.technicpack.minecraftcore.mojang.version.io;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"unused"})
public class Downloads {
    private Artifact artifact;
    private Map<String, Artifact> classifiers;

    public Downloads() {}

    public Downloads(String artifactUrl, String artifactSha1, long artifactSize) {
        artifact = new Artifact(artifactUrl, artifactSha1, artifactSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Downloads downloads = (Downloads) o;
        return Objects.equals(artifact, downloads.artifact) && Objects.equals(classifiers, downloads.classifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, classifiers);
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public Artifact getClassifier(String key) {
        return classifiers.get(key);
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }
}
