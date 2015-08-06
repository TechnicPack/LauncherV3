package net.technicpack.minecraftcore.mojang.auth.io;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;

public class UserPropertiesAdapter implements JsonSerializer<UserProperties>, JsonDeserializer<UserProperties> {
    @Override
    public UserProperties deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonArray())
            return deserializeFromNameValuePairs(jsonElement.getAsJsonArray());
        else if (jsonElement.isJsonObject())
            return deserializeFromMultimap(jsonElement.getAsJsonObject());
        else
            return null;
    }

    private UserProperties deserializeFromNameValuePairs(JsonArray array) {
        UserProperties properties = new UserProperties();

        for (JsonElement element : array) {
            if (!element.isJsonObject())
                continue;
            JsonObject obj = element.getAsJsonObject();

            if (obj.get("name") == null || obj.get("value") == null)
                continue;

            properties.add(obj.get("name").getAsString(), obj.get("value").getAsString());
        }

        return properties;
    }

    private UserProperties deserializeFromMultimap(JsonObject multimap) {
        UserProperties properties = new UserProperties();

        for (Map.Entry<String, JsonElement> field : multimap.entrySet()) {
            String name = field.getKey();
            JsonElement allValues = field.getValue();

            if (allValues.isJsonArray()) {
                for (JsonElement value : allValues.getAsJsonArray()) {
                    if (value.isJsonPrimitive())
                        properties.add(name, value.getAsString());
                }
            } else if (allValues.isJsonPrimitive()) {
                properties.add(name, allValues.getAsString());
            }
        }

        return properties;
    }

    @Override
    public JsonElement serialize(UserProperties userProperties, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        for (String key : userProperties.keys()) {
            JsonArray values = new JsonArray();
            for (String value : userProperties.values(key)) {
                values.add(new JsonPrimitive(value));
            }
            object.add(key, values);
        }

        return object;
    }
}
