package com.illiad.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;

@Component
public class Dtls {

    public final SSLContext sslCtx;

    public Dtls(@Value("${server.ssl.key-store}") Resource keyStore,
                @Value("${server.ssl.key-store-password}") String keyStorePassword,
                @Value("${server.ssl.key-store-type}") String keyStoreType,
                @Value("${server.ssl.key-alias}") String keyAlias) throws Exception {

        KeyStore ks = KeyStore.getInstance(keyStoreType);
        try (InputStream is = keyStore.getInputStream()) {
            ks.load(is, keyStorePassword.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        TrustManager[] trustManagers = tmf.getTrustManagers();

        this.sslCtx = SSLContext.getInstance("DTLS");
        this.sslCtx.init(keyManagers, trustManagers, null);
    }

}
