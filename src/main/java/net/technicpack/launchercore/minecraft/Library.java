package net.technicpack.launchercore.minecraft;

import net.technicpack.launchercore.util.OperatingSystem;

import java.util.List;
import java.util.Map;

public class Library {

	private String name;
	private List<Rule> rules;
	private Map<OperatingSystem, String> natives;
	private ExtractRules extract;
	private String url;
}
