package com.illiad.server.security;

import com.illiad.server.config.Params;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Component
public class SecretImp implements Secret {
    private final Params params;
    private final CryptoByte cryptoByte;

    public SecretImp(Params params, CryptoByte cryptoByte) {
        this.params = params;
        this.cryptoByte = cryptoByte;
    }

    @Override
    public short getCryptoLength(byte b) {
        return this.cryptoByte.byteLength(this.cryptoByte.toCrypto(b));
    }

    @Override
    public boolean verify(byte cryptoType, byte[] secret) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(this.cryptoByte.toCrypto(cryptoType).getValue());
        return Arrays.equals(secret, digest.digest(params.getSecret().getBytes(StandardCharsets.UTF_8)));
    }

}
