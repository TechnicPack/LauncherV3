package net.technicpack.launchercore.util;

public enum OperatingSystem {
	LINUX("linux", new String[] {"linux", "unix"}),
	WINDOWS("windows", new String[] {"win"}),
	OSX("osx", new String[] {"mac"}),
	UNKNOWN("unknown", new String[0]);

	private final String name;
	private final String[] aliases;

	private OperatingSystem(String name, String[] aliases) {
		this.name = name;
		this.aliases = aliases;
	}

	public String getName() {
		return name;
	}

	public String[] getAliases() {
		return aliases;
	}

	public boolean isSupported() {
		return this != UNKNOWN;
	}

	public static OperatingSystem getOperatingSystem() {
		String osName = System.getProperty("os.name").toLowerCase();

		for (OperatingSystem operatingSystem : values()) {
			for (String alias : operatingSystem.getAliases()) {
				if (osName.contains(alias)) {
					return operatingSystem;
				}
			}
		}

		return UNKNOWN;
	}
}
