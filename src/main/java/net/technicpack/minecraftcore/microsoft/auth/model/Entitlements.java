package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

import java.util.List;

/**
 *
 */
public class Entitlements {
    @Key(value="items") public List<MinecraftItem> items;
    @Key(value="signature") public String signature;
    @Key(value="keyId") public String keyId;
}
