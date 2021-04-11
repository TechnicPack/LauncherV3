package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;

import java.util.Collections;
import java.util.List;

public class XSTSProperties {
    @Key(value="SandboxId") private String sandboxId = "RETAIL";
    @Key(value="UserTokens") private List<String> userTokens;

    public XSTSProperties(String token) {
        userTokens = Collections.singletonList(token);
    }
}
