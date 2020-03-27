package net.technicpack.minecraftcore.mojang.version.io.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.mojang.version.io.Rule;

import java.util.List;

public class PredicatedArgument extends Argument {

	private final List<Rule> rules;
	private final Argument value;

	public PredicatedArgument(List<Rule> rules, Argument value) {
		this.rules = rules;
		this.value = value;
	}

	@Override
	public boolean doesApply(ILaunchOptions opts) {
		return Rule.isAllowable(rules, opts);
	}

	@Override
	public List<String> getArgStrings() {
		return value.getArgStrings();
	}

	@Override
	public JsonElement serialize() {
		JsonObject json = new JsonObject();
		JsonArray serializedRules = new JsonArray();
		for (Rule rule : rules) serializedRules.add(rule.serialize());
		json.add("rules", serializedRules);
		json.add("value", value.serialize());
		return json;
	}

}
