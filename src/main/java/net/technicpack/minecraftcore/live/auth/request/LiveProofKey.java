package net.technicpack.minecraftcore.live.auth.request;

import net.technicpack.minecraftcore.live.Base64;

import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Arrays;

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

        x = Base64.encodeToString(bx, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
        y = Base64.encodeToString(by, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }
}
