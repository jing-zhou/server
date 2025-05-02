package com.illiad.server.security;

import org.springframework.stereotype.Component;

@Component
public class CryptoByte {

    byte toByte(Cryptos c) {
        switch (c) {
            case SHA_224:
                return 0x10;
            case SHA_256:
                return 0x20;
            case SHA_384:
                return 0x30;
            case SHA_512:
                return 0x40;
            case SHA_512_224:
                return 0x50;
            case SHA_512_256:
                return 0x60;
            case HMAC_SHA224:
                return 0x70;
            case HMAC_SHA256:
                return 0x80;
            case HMAC_SHA384:
                return 0x90;
            case HMAC_SHA512:
                return 0xA0;
            case SHA224_WITH_RSA:
                return 0xB0;
            case SHA256_WITH_RSA:
                return 0xC0;
            case SHA384_WITH_RSA:
                return 0xD0;
            case SHA512_WITH_RSA:
                return 0xE0;
            case SHA224_WITH_DSA:
                return 0xF0;
            case SHA256_WITH_DSA:
                return 0x01;
            case SHA384_WITH_DSA:
                return 0x11;
            case SHA512_WITH_DSA:
                return 0x21;
            case SHA224_WITH_ECDSA:
                return 0x31;
            case SHA256_WITH_ECDSA:
                return 0x41;
            case SHA384_WITH_ECDSA:
                return 0x51;
            case SHA512_WITH_ECDSA:
                return 0x61;
            case SHA3_224:
                return 0x71;
            case SHA3_256:
                return 0x81;
            case SHA3_384:
                return 0x91;
            case SHA3_512:
                return 0xA1;
            case HMAC_SHA3_224:
                return 0xB1;
            case HMAC_SHA3_256:
                return 0xC1;
            case HMAC_SHA3_384:
                return 0xD1;
            case HMAC_SHA3_512:
                return 0xE1;
            default:
                throw new IllegalArgumentException("Unknown Cryptos standard: " + c);
        }
    }


    public Cryptos toCrypto(byte value) {
        switch (value) {
            case 0x10:
                return Cryptos.SHA_224;
            case 0x20:
                return Cryptos.SHA_256;
            case 0x30:
                return Cryptos.SHA_384;
            case 0x40:
                return Cryptos.SHA_512;
            case 0x50:
                return Cryptos.SHA_512_224;
            case 0x60:
                return Cryptos.SHA_512_256;
            case 0x70:
                return Cryptos.HMAC_SHA224;
            case 0x80:
                return Cryptos.HMAC_SHA256;
            case 0x90:
                return Cryptos.HMAC_SHA384;
            case 0xA0:
                return Cryptos.HMAC_SHA512;
            case 0xB0:
                return Cryptos.SHA224_WITH_RSA;
            case 0xC0:
                return Cryptos.SHA256_WITH_RSA;
            case 0xD0:
                return Cryptos.SHA384_WITH_RSA;
            case 0xE0:
                return Cryptos.SHA512_WITH_RSA;
            case 0xF0:
                return Cryptos.SHA224_WITH_DSA;
            case 0x01:
                return Cryptos.SHA256_WITH_DSA;
            case 0x11:
                return Cryptos.SHA384_WITH_DSA;
            case 0x21:
                return Cryptos.SHA512_WITH_DSA;
            case 0x31:
                return Cryptos.SHA224_WITH_ECDSA;
            case 0x41:
                return Cryptos.SHA256_WITH_ECDSA;
            case 0x51:
                return Cryptos.SHA384_WITH_ECDSA;
            case 0x61:
                return Cryptos.SHA512_WITH_ECDSA;
            case 0x71:
                return Cryptos.SHA3_224;
            case 0x81:
                return Cryptos.SHA3_256;
            case 0x91:
                return Cryptos.SHA3_384;
            case 0xA1:
                return Cryptos.SHA3_512;
            case 0xB1:
                return Cryptos.HMAC_SHA3_224;
            case 0xC1:
                return Cryptos.HMAC_SHA3_256;
            case 0xD1:
                return Cryptos.HMAC_SHA3_384;
            case 0xE1:
                return Cryptos.HMAC_SHA3_512;
            default:
                throw new IllegalArgumentException("Unknown byte value: " + value);
        }
    }
}