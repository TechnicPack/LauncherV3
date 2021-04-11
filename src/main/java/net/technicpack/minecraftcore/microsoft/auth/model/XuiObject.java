package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

public class XuiObject {
    @Key(value = "uhs")
    public String userhash;

    public String getUserhash() {
        return userhash;
    }
}
