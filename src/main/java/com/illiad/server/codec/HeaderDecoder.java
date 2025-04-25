package com.illiad.server.codec;

import com.illiad.server.handler.http.HttpServerHandler;
import com.illiad.server.security.Secret;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
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

        // keep byteBuf untouched until we can make sure it is an illiad header
        final int readerIndex = byteBuf.readerIndex();
        if (byteBuf.writerIndex() == readerIndex) {
            return;
        }
        // the first byte is assumed to be the length of the offset
        final int offsetLength = byteBuf.getByte(readerIndex);
        // Get the following 2 bytes after offset as per offsetLength, and convert into a byte array (big-endian)
        byte[] assumedCRLF = short2Bytes(byteBuf.getShort(offsetLength + 1));

        // if the length of offset mismatches, it is not an illiad header; route the request to a preset https webpage
        if (!Arrays.equals(CRLF, assumedCRLF)) {

            // remove all handlers except SslHandler from frontendPipeline
            ChannelPipeline frontendPipeline = ctx.pipeline();
            for (String name : frontendPipeline.names()) {
                ChannelHandler handler = frontendPipeline.get(name);
                if (handler instanceof SslHandler || handler instanceof LoggingHandler) {
                    continue;
                }
                frontendPipeline.remove(name);
            }
            // setup https webpage
            frontendPipeline.addLast(
                    new HttpServerCodec(),
                    new HttpObjectAggregator(1048576),
                    new HttpContentCompressor(),
                    new HttpServerExpectContinueHandler(),
                    new HttpServerHandler());
            frontendPipeline.remove(this);
            ctx.fireChannelRead(list);
            return;
        }

        // skip the offset plus 1 length byte, 2 bytes for CRLF
        byteBuf.skipBytes(offsetLength + 3);

        // acquire the secret length as unsigned short
        int secretLength = byteBuf.readUnsignedShort();
        byte[] secretBytes = new byte[secretLength];
        byteBuf.readBytes(secretBytes);

        byte[] secretEnd = new byte[2];
        byteBuf.readBytes(secretEnd);

        // if the secret is not equal to the length of the secret, then it is not an illiad header
        if (!Arrays.equals(CRLF, secretEnd)) {
            // TODO: reroute the request to somewhere
        }

        if (secret.verify(secretBytes)) {

        } else {
            // TODO: reroute the request to somewhere
        }
        ctx.pipeline().remove(this);

    }

    static byte[] short2Bytes(short value) {
        return new byte[] {
                (byte) (value >> 8), // Extract high byte
                (byte) value         // Extract low byte
        };
    }

}
