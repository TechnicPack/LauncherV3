package net.technicpack.minecraftcore.msa.auth.request;

import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Arrays;
import java.util.Base64;

public class LiveProofKey {
    private String crv = "P-256";
    private String alg = "ES256";
    private String use = "sig";
    private String kty = "EC";
    private String x;
    private String y;
    public LiveProofKey() {

    }

    public void setPublicKey(ECPublicKey pk)
    {
        ECPoint point = pk.getW();
        byte[] bx = point.getAffineX().toByteArray();
        if (bx[0] == 0x00)
            bx = Arrays.copyOfRange(bx, 1, bx.length);

        byte[] by = point.getAffineY().toByteArray();
        if (by[0] == 0x00)
            by = Arrays.copyOfRange(by, 1, by.length);

        x = Base64.getUrlEncoder().encodeToString(bx);
        y = Base64.getUrlEncoder().encodeToString(by);
    }
}
