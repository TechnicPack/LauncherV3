package net.technicpack.minecraftcore.live.auth;

import java.util.Map;

public interface OnLoggedInListener {
    void onLoggedIn(Map<String, String> params);
}