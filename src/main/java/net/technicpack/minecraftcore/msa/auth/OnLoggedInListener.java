package net.technicpack.minecraftcore.msa.auth;

import java.util.Map;

public interface OnLoggedInListener {
    void onLoggedIn(Map<String, String> params);
}