package com.illiad.codec;

import com.illiad.security.Secret;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ChannelHandler.Sharable
public class HeaderDecoder extends ByteToMessageDecoder {
    private final Secret secret;

    public HeaderDecoder(Secret secret) {
        this.secret = secret;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {

        // Iilliad protocol header format: [offset length][offset bytes][secret length][secret bytes]
        //skip offset, the length of the offset is the first byte
        byteBuf.skipBytes((byteBuf.readByte()));
       // read the secret bytes, the length of the secret bytes equals the first byte + the second byte * 256
        byte[] secretBytes = byteBuf.readBytes(byteBuf.readByte() + byteBuf.readByte() * 256).array();

        if (secret.verify(secretBytes)) {

        } else {

        }
        ctx.pipeline().remove(this);

    }
}
