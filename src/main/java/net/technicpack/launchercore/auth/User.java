package net.technicpack.launchercore.auth;

import com.google.gson.JsonObject;

public class User {
    private String id;
    private JsonObject userProperties;

    public User() {

    }

    public String getId() {
        return id;
    }

    public JsonObject getUserProperties() {
        return userProperties;
    }
}
