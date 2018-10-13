package net.technicpack.minecraftcore.mojang.version.io.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArgumentList {

	private final List<Argument> args;

	private ArgumentList(List<Argument> args) {
		this.args = Collections.unmodifiableList(args);
	}

	public static ArgumentList fromString(String args) {
		if (args == null) return null;
		Builder argsBuilder = new Builder();
		for (String arg : args.split(" ")) argsBuilder.addArgument(Argument.literal(arg));
		return argsBuilder.build();
	}

	public List<Argument> getArguments() {
		return args;
	}

	public List<String> resolve(ILaunchOptions opts, StrSubstitutor derefs) {
		List<String> resolved = new ArrayList<>();
		if (derefs == null) derefs = new StrSubstitutor();
		for (Argument arg : args) {
			if (arg.doesApply(opts)) {
				for (String argStr : arg.getArgStrings()) {
					resolved.add(derefs.replace(argStr));
				}
			}
		}
		return resolved;
	}

	public JsonElement serialize() {
		JsonArray json = new JsonArray();
		for (Argument arg : args) json.add(arg.serialize());
		return json;
	}

	public static class Builder {

		private final List<Argument> args = new ArrayList<>();

		public void addArgument(Argument arg) {
			args.add(arg);
		}

		public ArgumentList build() {
			return new ArgumentList(args);
		}

	}

}
