package net.technicpack.minecraftcore.mojang.version.io.argument;

import com.google.gson.*;
import net.technicpack.minecraftcore.mojang.version.io.Rule;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ArgumentListAdapter implements JsonSerializer<ArgumentList>, JsonDeserializer<ArgumentList> {

	@Override
	public ArgumentList deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
		if (!json.isJsonArray()) throw new JsonParseException("Expected argument array, got: " + json);
		ArgumentList.Builder argsBuilder = new ArgumentList.Builder();
		for (JsonElement arg : json.getAsJsonArray()) {
			if (arg.isJsonObject()) {
				JsonObject argObj = arg.getAsJsonObject();
				JsonArray rules = argObj.get("rules").getAsJsonArray();
				List<Rule> predicates = new ArrayList<>();
				for (JsonElement rule : rules) {
					predicates.add(new Rule(rule.getAsJsonObject()));
				}
				JsonElement value = argObj.get("value");
				Argument argValue;
				if (value.isJsonArray()) {
					List<String> argStrings = new ArrayList<>();
					for (JsonElement valueElem : value.getAsJsonArray()) {
						if (valueElem.isJsonPrimitive() && valueElem.getAsJsonPrimitive().isString()) {
							argStrings.add(valueElem.getAsString());
						} else {
							throw new JsonParseException("Expected argument string, got: " + arg);
						}
					}
					argValue = new ArgumentStringList(argStrings);
				} else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
					argValue = Argument.literal(value.getAsString());
				} else {
					throw new JsonParseException("Expected argument literal, got: " + arg);
				}
				argsBuilder.addArgument(new PredicatedArgument(predicates, argValue));
			} else if (arg.isJsonPrimitive() && arg.getAsJsonPrimitive().isString()) {
				argsBuilder.addArgument(Argument.literal(arg.getAsString()));
			} else {
				throw new JsonParseException("Expected argument, got: " + arg);
			}
		}
		return argsBuilder.build();
	}

	@Override
	public JsonElement serialize(ArgumentList args, Type type, JsonSerializationContext ctx) {
		return args.serialize();
	}

}
