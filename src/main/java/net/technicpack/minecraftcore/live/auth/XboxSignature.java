package net.technicpack.minecraftcore.live.auth;

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import java.math.BigInteger;
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

    private byte[] decodeSignature(byte[] signature) throws SignatureException {
        try {
            DerInputStream in = new DerInputStream(signature);
            DerValue[] values = in.getSequence(2);
            BigInteger r = values[0].getPositiveBigInteger();
            BigInteger s = values[1].getPositiveBigInteger();
            // trim leading zeroes
            byte[] rBytes = trimZeroes(r.toByteArray());
            byte[] sBytes = trimZeroes(s.toByteArray());
            int k = Math.max(rBytes.length, sBytes.length);
            // r and s each occupy half the array
            byte[] result = new byte[k << 1];
            System.arraycopy(rBytes, 0, result, k - rBytes.length,
                    rBytes.length);
            System.arraycopy(sBytes, 0, result, result.length - sBytes.length,
                    sBytes.length);
            return result;

        } catch (Exception e) {
            throw new SignatureException("Could not decode signature", e);
        }
    }

    private static byte[] trimZeroes(byte[] b) {
        int i = 0;
        while ((i < b.length - 1) && (b[i] == 0)) {
            i++;
        }
        if (i == 0) {
            return b;
        }
        byte[] t = new byte[b.length - i];
        System.arraycopy(b, i, t, 0, t.length);
        return t;
    }

    public String SignRequest(String method, String url, Map<String, String> headers, String data) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong((Instant.now().getEpochSecond() + 11644473600L) * 10000000);
            buffer.flip();
            byte[] time = buffer.array();

            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
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

            byte[] signature = decodeSignature(ecdsaSign.sign());

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
