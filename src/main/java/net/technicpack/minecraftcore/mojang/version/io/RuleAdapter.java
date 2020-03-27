package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.*;

import java.lang.reflect.Type;

public class RuleAdapter implements JsonSerializer<Rule>, JsonDeserializer<Rule> {

	@Override
	public Rule deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
		return new Rule(json.getAsJsonObject());
	}

	@Override
	public JsonElement serialize(Rule rule, Type type, JsonSerializationContext ctx) {
		return rule.serialize();
	}

}
