package com.illiad.server.codec.v5;

import com.illiad.server.ParamBus;
import com.illiad.server.handler.v5.VersionHandler;
import com.illiad.server.handler.http.SimpleHttpHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.*;

import java.util.Arrays;
import java.util.List;

/**
 * Decodes a server-side illiad Header from a {@link ByteBuf}.
 * an illiad header is a byte array of variable lenght, ended by CRLF.
 * the first 2 bytes is the length of the header. the next byte is the crypto type. then comes the signature, and a random offset.
 * if the encryption return a fixed-length signature, the length field contains the whole length(length + cryptoType + signature + offset + CRLF).
 * if the encryption return a variable-length signature, the length field contains the length of the signature only(length + cryptoType + signature).
 */

public class HeaderDecoder extends ByteToMessageDecoder {
    private static final byte[] CRLF = new byte[]{0x0D, 0x0A};
    private static final List<Byte> VALID_CRYPTO_TYPES = Arrays.asList(
            (byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40, (byte) 0x50, (byte) 0x60,
            (byte) 0x70, (byte) 0x80, (byte) 0x90, (byte) 0xA0, (byte) 0xB0, (byte) 0xC0,
            (byte) 0xD0, (byte) 0xE0, (byte) 0xF0, (byte) 0x01, (byte) 0x11, (byte) 0x21,
            (byte) 0x31, (byte) 0x41, (byte) 0x51, (byte) 0x61, (byte) 0x71, (byte) 0x81,
            (byte) 0x91, (byte) 0xA1, (byte) 0xB1, (byte) 0xC1, (byte) 0xD1, (byte) 0xE1);


    private final ParamBus bus;

    public HeaderDecoder(ParamBus bus) {
        this.bus = bus;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {

        // keep byteBuf untouched until we can make sure it is an illiad header
        final int readerIndex = byteBuf.readerIndex();
        if (byteBuf.writerIndex() == readerIndex) {
            return;
        }

        // get Crypto type byte
        final byte cryptoType = byteBuf.getByte(readerIndex + 2);
        // Precheck if cryptoType is valid
        if (!VALID_CRYPTO_TYPES.contains(cryptoType)) {
            this.rerouteToHttp(ctx, list);
            return;
        }

        //get the length field of the header
        final int length = byteBuf.getUnsignedShort(readerIndex);
        short signLength = bus.secret.getCryptoLength(cryptoType);
        // simple check for the length of the header
        if (length < 34 || length < signLength || length > byteBuf.capacity()) {
            // if the length of the header is less than 34, it is not an illiad header; 34 = 2 bytes for length + 1 byte for crypto type + 28 bytes for minimum signature + 1 bytes for minimum offset + 2 bytes for CRLF
            // if the length of the header is less than the supposed signature length, it is not an illiad header
            // if the length of the header is greater than the capacity of the buffer, it is not an illiad header
            this.rerouteToHttp(ctx, list);
            return;
        }

        int headerEnd;
        // check if header is ended by CRLF
        if (signLength > 0) {
            // the crypto type indicates that the encryption return a fixed-length signature, the length field contains the whole length( 5 + signature + offset).
            // 5 = 2 bytes for length + 1 byte for crypto type + 2 bytes for CRLF
            // get the last 2 bytes as per length, and convert into a byte array (big-endian)
            byte[] assumedCRLF = short2Bytes(byteBuf.getShort(length - 2));
            if (!Arrays.equals(CRLF, assumedCRLF)) {
                // if the header is not ended by CRLF, it is not an illiad header
                this.rerouteToHttp(ctx, list);
                return;
            }
            //the index for LF
            headerEnd = length - 1;
        } else {
            // the crypto type indicates that the encryption return a variable-length signature, the length field contain the length of the signature only(3 + signature).
            //  3 = 2 bytes for length + 1 byte for crypto type
            int cRLFIndex = findCRLF(byteBuf, length);
            if (cRLFIndex == -1) {
                // if the header is not ended by CRLF, it is not an illiad header
                this.rerouteToHttp(ctx, list);
                return;
            }
            // the index for LF
            headerEnd = cRLFIndex + 1;
        }

        // finally get the secret bytes
        byte[] secretBytes;
        if (signLength > 0) {
            // fixed length signature, get the signature as per length standard
            secretBytes = new byte[signLength];
            byteBuf.getBytes(3, secretBytes);
        } else {
            // variable length signature, get the signature as per length field
            secretBytes = new byte[length - 3];
            byteBuf.getBytes(3, secretBytes);
        }

        // verify the secret
        try {
            if (!bus.secret.verify(cryptoType, secretBytes)) {
                // if the secret is not equal to the length of the secret, then it is not an illiad header
                this.rerouteToHttp(ctx, list);
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // add socks protocol handlers to the pipeline
        ctx.pipeline().addLast(bus.namer.generateName(), new VersionHandler(bus));
        // skip the header
        byteBuf.skipBytes(headerEnd + 1);
        ctx.pipeline().remove(this);

    }


    private void rerouteToHttp(ChannelHandlerContext ctx, List<Object> list) {

        ChannelPipeline frontendPipeline = ctx.pipeline();

        // setup https webpage
        frontendPipeline.addLast(new HttpServerCodec(),
                new HttpObjectAggregator(1048576),
                new SimpleHttpHandler());
        ctx.pipeline().remove(this);
    }

    /**
     * The length of the offset in bytes.
     */
    private int findCRLF(ByteBuf byteBuf, int startIndex) {
        // 256 maximum length of offset
        final int maxIndex = Math.min(byteBuf.writerIndex(), startIndex + 256);
        for (int i = startIndex; i < maxIndex - 1; i++) {
            if (byteBuf.getByte(i) == 0x0D && byteBuf.getByte(i + 1) == 0x0A) {
                return i; // Return the index of the first byte of CRLF
            }
        }
        return -1; // Return -1 if CRLF is not found
    }

    /**
     * Converts a short value to a byte array (big-endian).
     *
     * @param value the short value to convert
     * @return a byte array representing the short value
     */

    static byte[] short2Bytes(short value) {
        return new byte[]{(byte) (value >> 8), // Extract high byte
                (byte) value         // Extract low byte
        };
    }

}
