package net.technicpack.minecraftcore.launch;

import java.util.*;

public class LaunchCommandCollector {

	private final List<String> commands = new ArrayList<>();
	private final Set<String> seen = new HashSet<>();

	public void addRaw(String command) {
		commands.add(command);
	}

	public void add(String command) {
		int eqIndex = command.indexOf("=");
		if (eqIndex != -1) {
			String commandPart = command.substring(0, eqIndex);
			markExists(commandPart);
		} else {
			markExists(command);
		}
		commands.add(command);
	}

	public void add(String command, String param) {
		markExists(command);
		commands.add(command);
		commands.add(param);
	}

	public void addUnique(String command) {
		int eqIndex = command.indexOf("=");
		if (eqIndex != -1) {
			String commandPart = command.substring(0, eqIndex);
			if (exists(commandPart)) return;
			markExists(commandPart);
		} else if (exists(command)) {
			return;
		} else {
			markExists(command);
		}
		commands.add(command);
	}

	public void addUnique(String command, String param) {
		if (!exists(command)) add(command, param);
	}

	public boolean exists(String command) {
		return seen.contains(command.toLowerCase().trim());
	}

	private void markExists(String command) {
		seen.add(command.toLowerCase().trim());
	}

	public List<String> collect() {
		return Collections.unmodifiableList(commands);
	}

}
