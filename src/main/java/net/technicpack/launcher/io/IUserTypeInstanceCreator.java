package net.technicpack.launcher.io;

import com.google.gson.*;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;
import net.technicpack.utilslib.Utils;

import java.lang.reflect.Type;
import java.util.Locale;

public class IUserTypeInstanceCreator implements JsonDeserializer<IUserType>, JsonSerializer<IUserType> {
    private static final String USER_TYPE_FIELD = "userType";
    // Test builds previously used this value for the type saved in the user.json for MS accounts
    private static final String BETA_MSA_USER_TYPE = "microsoft";

    @Override
    public IUserType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Utils.getLogger().info("User deserialization");

        JsonObject rootObject = json.getAsJsonObject();


        String userString = rootObject.has(USER_TYPE_FIELD) ?
                rootObject.get(USER_TYPE_FIELD).getAsString().toLowerCase(Locale.ROOT) : null;

        if (userString == null || userString.isEmpty()) {
            return null;
        }

        switch (userString) {
            case MicrosoftUser.USER_TYPE:
            case BETA_MSA_USER_TYPE:
                Utils.getLogger().info("Deserializing microsoft user");
                return context.deserialize(rootObject, MicrosoftUser.class);
            default:
                Utils.getLogger().info(String.format("Unknown user type: %s", userString));
                return null;
        }
    }

    @Override
    public JsonElement serialize(IUserType src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof MicrosoftUser) {
            JsonObject obj = context.serialize(src, MicrosoftUser.class).getAsJsonObject();
            obj.addProperty(USER_TYPE_FIELD, MicrosoftUser.USER_TYPE);
            return obj;
        }
        return null;
    }
}
