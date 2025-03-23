package com.illiad.server.codec;

import com.illiad.server.security.Secret;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.Arrays;
import java.util.List;

/**
 * Decodes a server-side illiad Header from a {@link ByteBuf}.
 * an illiad header consists by a variable offset, followed by CRLF, and a variable secret, followed by CRLF.
 * the 1 byte ahead of the offset is the length of the offset, and the 2 bytes ahead of the secret is the length of the secret.
 * this decoder is used to decode the header from a {@link ByteBuf} before processing the request.
 */

public class HeaderDecoder extends ByteToMessageDecoder {
    private final static byte[] CRLF = new byte[]{0x0D, 0x0A};
    private final Secret secret;

    public HeaderDecoder(Secret secret) {
        this.secret = secret;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {

        // keep everything in case it turns out not to be an illiad header and we have to restore the contents and reroute the request to somewhere
        byte firstByte = byteBuf.readByte();
        byte[] offset = byteBuf.readBytes(firstByte).array();
        byte[] offsetEnd = byteBuf.readBytes(2).array();

        // if the offset is not equal to the length of the offset, then it is not an illiad header
        if (!Arrays.equals(CRLF, offsetEnd)) {
            // TODO: reroute the request to somewhere
        }
        // acquire the secret length
        byte byte1 = byteBuf.readByte();
        byte byte2 = byteBuf.readByte();

        // the length of the secret bytes equals the first byte + the second byte * 256
        int secretLength = byte1 + byte2 * 256;
        byte[] secretBytes = byteBuf.readBytes(secretLength).array();
        byte[] secretEnd = byteBuf.readBytes(2).array();

        // if the secret is not equal to the length of the secret, then it is not an illiad header
        if(!Arrays.equals(CRLF, secretEnd)) {
            // TODO: reroute the request to somewhere
        }

        if (secret.verify(secretBytes)) {

        } else {
            // TODO: reroute the request to somewhere
        }
        ctx.pipeline().remove(this);

    }
}
