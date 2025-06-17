package net.technicpack.launchercore.launch.java.version;

import com.google.gson.*;

import java.lang.reflect.Type;

public class FileBasedJavaRuntimeAdapter implements JsonSerializer<FileBasedJavaRuntime>,
        JsonDeserializer<FileBasedJavaRuntime> {

    @Override
    public JsonElement serialize(FileBasedJavaRuntime src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("path", src.getExecutablePath());
        return obj;
    }

    @Override
    public FileBasedJavaRuntime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        JsonElement pathElement;
        if (obj.has("path")) {
            pathElement = obj.get("path");
        } else {
            pathElement = obj.get("filePath");
        }
        if (pathElement == null) throw new JsonParseException("Missing path field");
        return new FileBasedJavaRuntime(pathElement.getAsString());
    }

}
