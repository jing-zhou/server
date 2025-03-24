package com.illiad.server.security;

public interface Secret {
    byte[] getSecret();

    boolean verify(byte[] secret);
}
