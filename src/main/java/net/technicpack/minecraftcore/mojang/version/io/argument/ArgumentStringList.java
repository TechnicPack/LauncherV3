package net.technicpack.minecraftcore.mojang.version.io.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Collections;
import java.util.List;

public class ArgumentStringList extends Argument {

	private final List<String> values;

	public ArgumentStringList(List<String> values) {
		this.values = Collections.unmodifiableList(values);
	}

	@Override
	public List<String> getArgStrings() {
		return values;
	}

	@Override
	public JsonElement serialize() {
		JsonArray json = new JsonArray();
		for (String value : values) json.add(new JsonPrimitive(value));
		return json;
	}

}
