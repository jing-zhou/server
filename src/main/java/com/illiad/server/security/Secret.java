package com.illiad.server.security;


import java.security.NoSuchAlgorithmException;

public interface Secret {

    short getCryptoLength(byte b);

    boolean verify(byte cryptoType, byte[] secret) throws NoSuchAlgorithmException;

}
