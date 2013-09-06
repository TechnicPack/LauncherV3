package net.technicpack.launcher.minecraft;

import net.technicpack.launcher.util.OperatingSystem;

import java.util.List;
import java.util.Map;

public class Library {

	private String name;
	private List<Rule> rules;
	private Map<OperatingSystem, String> natives;
	private ExtractRules extract;
	private String url;
}
