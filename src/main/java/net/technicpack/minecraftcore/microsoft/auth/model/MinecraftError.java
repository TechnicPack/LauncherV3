package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

public class MinecraftError {
    @Key(value="path") public String path;
    @Key(value="errorType") public String errorType;
    @Key(value="error") public String error;
    @Key(value="errorMessage") public String errorMessage;
    @Key(value="developerMessage") public String developerMessage;
}
