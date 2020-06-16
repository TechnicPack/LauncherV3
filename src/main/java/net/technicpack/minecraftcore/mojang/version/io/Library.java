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

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.text.StringSubstitutor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Library {
    private static final String[] fallback = {
        "http://mirror.technicpack.net/Technic/lib/",
        "https://libraries.minecraft.net/",
        "https://files.minecraftforge.net/maven/",
        "https://search.maven.org/remotecontent?filepath="
    };
    private static final Pattern gradlePattern = Pattern.compile("^([^:@]+):([^:@]+):([^:@]+)(?::([^:@]+))?(?:@([^:@]+))?$");

    // JSON fields
    private String name;
    private List<Rule> rules;
    private Downloads downloads;
    private Map<OperatingSystem, String> natives;
    private ExtractRules extract;
    private String url;

    private transient String gradleGroupId;
    private transient String gradleArtifactId;
    private transient String gradleVersion;
    private transient String gradleClassifier;
    private transient String gradleExtension = "jar";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public Map<OperatingSystem, String> getNatives() {
        return natives;
    }

    public ExtractRules getExtract() {
        return extract;
    }

    private void parseName() {
        // Don't reparse if it's already been parsed
        if (gradleGroupId != null)
            return;

        Matcher m = gradlePattern.matcher(name);

        if (!m.matches()) {
            throw new IllegalStateException("Cannot parse empty gradle specifier");
        }

        gradleGroupId = m.group(1);
        gradleArtifactId = m.group(2);
        gradleVersion = m.group(3);
        gradleClassifier = m.group(4);
        String extension = m.group(5);
        if (extension != null && !extension.isEmpty())
            gradleExtension = extension;
    }

    public boolean isForCurrentOS() {
        if (rules == null) {
            return true;
        }
        return Rule.isAllowable(rules, null);
    }

    public boolean hasNatives() {
        return natives != null && !natives.isEmpty();
    }

    public String getArtifactPath() {
        return getArtifactPath(null);
    }

    public String getArtifactPath(String nativeClassifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact path of empty/blank artifact");
        }

        // Make sure the gradle specifier is parsed
        parseName();

        String filename = getArtifactFilename(nativeClassifier);

        return gradleGroupId.replace('.', '/') + '/' + gradleArtifactId + '/' + gradleVersion + '/' + filename;
    }

    public String getArtifactFilename(String nativeClassifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact filename of empty/blank artifact");
        }

        // Make sure the gradle specifier is parsed
        parseName();

        String filename = gradleArtifactId + '-' + gradleVersion;

        // The native classifier overrides the regular classifier
        if (nativeClassifier != null)
            filename += '-' + nativeClassifier;
        else if (gradleClassifier != null && !gradleClassifier.isEmpty())
            filename += '-' + gradleClassifier;

        filename += '.' + gradleExtension;

        return filename;
    }

    public String getDownloadUrl(String path, MirrorStore mirrorStore) throws DownloadException {
        // Check the old-style URL (Forge 1.6, I think?)
        // It acts as a Maven root URL
        if (this.url != null) {
            String checkUrl = url + path;
            if (Utils.pingHttpURL(checkUrl, mirrorStore)) {
                return checkUrl;
            }
        }

        // Check if any mirrors we know of have this library
        for (String string : fallback) {
            String checkUrl = string + path;
            if (Utils.pingHttpURL(checkUrl, mirrorStore)) {
                return checkUrl;
            }
        }

        // Check if an artifact URL is specified (downloads -> artifact -> url), only if it doesn't have natives
        // This is a fully specified URL
        if (!hasNatives()) {
            String artifactUrl = null;

            if (downloads != null) {
                Artifact artifact = downloads.getArtifact();

                if (artifact != null)
                    artifactUrl = artifact.getUrl();
            }

            if (artifactUrl != null) {
                if (Utils.pingHttpURL(artifactUrl, mirrorStore)) {
                    return artifactUrl;
                }
            }
        }

        throw new DownloadException("Failed to download library " + path + ": no mirror found");
    }

}
