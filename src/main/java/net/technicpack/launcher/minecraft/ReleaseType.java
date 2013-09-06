package net.technicpack.launcher.minecraft;

import java.util.HashMap;
import java.util.Map;

public enum ReleaseType {
	SNAPSHOT("snapshot"),
	RELEASE("release"),
	OLD_BETA("old-beta"),
	OLD_ALPHA("old-alpha");

	private static final Map<String, ReleaseType> lookup = new HashMap<String, ReleaseType>();
	private final String name;

	private ReleaseType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static ReleaseType get(String name) {
		return lookup.get(name);
	}

	static {
		for (ReleaseType type : values()) {
			lookup.put(type.getName(), type);
		}
	}
}
