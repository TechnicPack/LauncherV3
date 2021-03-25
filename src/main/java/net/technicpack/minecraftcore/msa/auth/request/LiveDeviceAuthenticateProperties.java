package net.technicpack.minecraftcore.msa.auth.request;

import java.security.interfaces.ECPublicKey;
import java.util.UUID;

public class LiveDeviceAuthenticateProperties {
    private String AuthMethod = "ProofOfPossession";
    private String Id = "{" + UUID.randomUUID().toString().toUpperCase() + "}";
    private String DeviceType = "Win32";
    private String Version = "10.0.18363";
    private LiveProofKey ProofKey = new LiveProofKey();

    public LiveDeviceAuthenticateProperties() {
    }

    public void setPublicKey(ECPublicKey pk)
    {
        ProofKey.setPublicKey(pk);
    }
}
