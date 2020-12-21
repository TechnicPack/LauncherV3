package net.technicpack.minecraftcore.live.auth;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public class XboxSignature {
    private ECPublicKey publicKey;
    private ECPrivateKey privateKey;

    public XboxSignature() {
        try {
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(ecSpec, new SecureRandom());
            KeyPair keypair = g.generateKeyPair();
            publicKey = (ECPublicKey) keypair.getPublic();
            privateKey = (ECPrivateKey) keypair.getPrivate();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    public String SignRequest(String method, String url, Map<String, String> headers, String data) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong((Instant.now().getEpochSecond() + 11644473600L) * 10000000);
            buffer.flip();
            byte[] time = buffer.array();

            Signature ecdsaSign = Signature.getInstance("SHA256withECDSAinP1363format");
            ecdsaSign.initSign(privateKey);

            byte[] policyVersion =  {0, 0, 0, 1};
            ecdsaSign.update(policyVersion);
            ecdsaSign.update((byte) 0);

            ecdsaSign.update(time);
            ecdsaSign.update((byte) 0);

            ecdsaSign.update(method.getBytes());
            ecdsaSign.update((byte) 0);

            URL u = new URL(url);
            ecdsaSign.update(u.getPath().getBytes());
            String query = u.getQuery();
            if (query != null)
                ecdsaSign.update(query.getBytes());
            ecdsaSign.update((byte) 0);

            if (headers.containsKey("Authorization"))
                ecdsaSign.update(headers.get("Authorization").getBytes());
            ecdsaSign.update((byte) 0);

            ecdsaSign.update(data.getBytes());
            ecdsaSign.update((byte) 0);

            byte[] signature = ecdsaSign.sign();

            ByteBuffer out = ByteBuffer.allocate(policyVersion.length + time.length + signature.length);
            out.order(ByteOrder.LITTLE_ENDIAN);
            out.put(policyVersion);
            out.put(time);
            out.put(signature);
            out.flip();
            return Base64.getEncoder().encodeToString(out.array());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ECPublicKey getPublicKey() { return (ECPublicKey) publicKey; }
}
