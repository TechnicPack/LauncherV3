package net.technicpack.launcher.io;

import com.google.gson.*;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;

import java.lang.reflect.Type;

public class IUserTypeInstanceCreator implements JsonDeserializer<IUserType>, JsonSerializer<IUserType> {
    // Test builds previously used this value for the type saved in the user.json for MS accounts
    private static final String BETA_MS_USER_TYPE = "microsoft";

    @Override
    public IUserType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        System.out.println("User deserialization");
        JsonObject rootObject = json.getAsJsonObject();
        JsonElement userType = rootObject.get("userType");
        String userString = userType == null ? null: userType.getAsString();
        if (userString == null) {
            return null;
        }
        if (userString.equals(BETA_MS_USER_TYPE) || userString.equals(MicrosoftUser.MC_MS_USER_TYPE)) {
            System.out.println("Deserializing microsoft user");
            return MojangUtils.getGson().fromJson(rootObject, MicrosoftUser.class);
        }
        System.out.println("Unknown user type: " + userString);
        return null;
    }

    @Override
    public JsonElement serialize(IUserType src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof MicrosoftUser) {
            JsonElement userElement = MojangUtils.getGson().toJsonTree(src);
            userElement.getAsJsonObject().addProperty("userType", MicrosoftUser.MC_MS_USER_TYPE);
            return userElement;
        }
        return null;
    }
}
