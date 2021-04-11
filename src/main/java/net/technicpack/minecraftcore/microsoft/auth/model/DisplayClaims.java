package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;
import net.technicpack.minecraftcore.microsoft.auth.model.XuiObject;

import java.util.ArrayList;
import java.util.List;

public class DisplayClaims {
    @Key(value = "xui")
    public List<XuiObject> xuiObjects = new ArrayList<>();

    public String getUserhash() {
        if (xuiObjects == null || xuiObjects.isEmpty()) {
            throw new RuntimeException("TODO: No user hash");
        }
        return xuiObjects.get(0).getUserhash();
    }
}
