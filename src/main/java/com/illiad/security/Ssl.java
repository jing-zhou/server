package com.illiad.security;

import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Component
@ChannelHandler.Sharable
public class Ssl {

    public final SslContext sslCtx;

    public Ssl(@Value("${server.ssl.key-store}") Resource keyStore,
               @Value("${server.ssl.key-store-password}") String keyStorePassword,
               @Value("${server.ssl.key-alias}") String keyAlias) throws Exception {

        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream is = keyStore.getInputStream()) {
            ks.load(is, keyStorePassword.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.toCharArray());

        this.sslCtx = SslContextBuilder.forServer(kmf).build();
    }
}

