package net.technicpack.minecraftcore.mojang.version.io.argument;

import com.google.gson.*;
import net.technicpack.minecraftcore.mojang.version.io.Rule;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ArgumentListAdapter implements JsonSerializer<ArgumentList>, JsonDeserializer<ArgumentList> {

	@Override
	public ArgumentList deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        /*
         * Arguments are a list of mixed item types. Each item can be one of 2 types: string or object.
         * String arguments are interpreted as a literal argument.
         * Object arguments have "rules" and a "value".
         * - The "rules" key is always a list of Rule objects.
         * - The "value" key can either be a string or a list of strings.
         * All arguments can have strings that need to be interpolated.
         */
		if (!json.isJsonArray()) throw new JsonSyntaxException("Expected argument array, got: " + json);

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
                Argument argValue = getArgumentValue(arg, value);
                argsBuilder.addArgument(new PredicatedArgument(predicates, argValue));
			} else if (arg.isJsonPrimitive() && arg.getAsJsonPrimitive().isString()) {
				argsBuilder.addArgument(Argument.literal(arg.getAsString()));
			} else {
				throw new JsonSyntaxException("Expected argument, got: " + arg);
			}
		}

		return argsBuilder.build();
	}

    private static @NotNull Argument getArgumentValue(JsonElement arg, JsonElement value) {
        Argument argValue;
        if (value.isJsonArray()) {
            List<String> argStrings = new ArrayList<>();
            for (JsonElement valueElem : value.getAsJsonArray()) {
                if (valueElem.isJsonPrimitive() && valueElem.getAsJsonPrimitive().isString()) {
                    argStrings.add(valueElem.getAsString());
                } else {
                    throw new JsonSyntaxException("Expected argument string, got: " + arg);
                }
            }
            argValue = new ArgumentStringList(argStrings);
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            argValue = Argument.literal(value.getAsString());
        } else {
            throw new JsonSyntaxException("Expected argument literal, got: " + arg);
        }
        return argValue;
    }

    @Override
	public JsonElement serialize(ArgumentList args, Type type, JsonSerializationContext ctx) {
		return args.serialize();
	}

}
