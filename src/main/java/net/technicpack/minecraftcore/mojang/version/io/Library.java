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

import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.util.List;
import java.util.Map;

public class Library {
	private static final StrSubstitutor SUBSTITUTOR = new StrSubstitutor();
	private static final String[] fallback = {"http://mirror.technicpack.net/Technic/lib/", "http://search.maven.org/remotecontent?filepath="};
	private String name;
	private List<Rule> rules;
	private Map<OperatingSystem, String> natives;
	private ExtractRules extract;
	private String url;

    public String getUrl() { return url; }

	public String getName() {
		return name;
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

	public boolean isForCurrentOS() {
		if (rules == null) {
			return true;
		}

		Rule.Action lastAction = Rule.Action.DISALLOW;

		for (Rule rule : rules) {
			Rule.Action action = rule.getAction();
			if (action != null) {
				lastAction = action;
			}
		}

		return lastAction.equals(Rule.Action.ALLOW);
	}

	public String getArtifactPath() {
		return getArtifactPath(null);
	}

	public String getArtifactPath(String classifier) {
		if (this.name == null) {
			throw new IllegalStateException("Cannot get artifact path of empty/blank artifact");
		}
		return String.format("%s/%s", getArtifactBaseDir(), getArtifactFilename(classifier));
	}

	public String getArtifactBaseDir() {
		if (this.name == null) {
			throw new IllegalStateException("Cannot get artifact dir of empty/blank artifact");
		}
		String[] parts = this.name.split(":", 3);
		return String.format("%s/%s/%s", parts[0].replaceAll("\\.", "/"), parts[1], parts[2]);
	}

	public String getArtifactFilename(String classifier) {
		if (this.name == null) {
			throw new IllegalStateException("Cannot get artifact filename of empty/blank artifact");
		}

		String[] parts = this.name.split(":", 3);
		String result;

		if (classifier != null) {
			result = String.format("%s-%s%s.jar", parts[1], parts[2], "-" + classifier);
		} else {
			result = String.format("%s-%s%s.jar", parts[1], parts[2], "");
		}

		return SUBSTITUTOR.replace(result);
	}

	public String getDownloadUrl(String path, MirrorStore mirrorStore) {
		if (this.url != null) {
			String checkUrl = url + path;
			if (Utils.pingHttpURL(checkUrl, mirrorStore)) {
				return checkUrl;
			}
			for (String string : fallback) {
				checkUrl = string + path;
				if (Utils.pingHttpURL(checkUrl, mirrorStore)) {
					return checkUrl;
				}
			}
		}
		return " https://libraries.minecraft.net/" + path;
	}
}
