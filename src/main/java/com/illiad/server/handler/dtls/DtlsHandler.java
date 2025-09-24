package com.illiad.server.handler.dtls;

import com.illiad.server.ParamBus;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.net.ssl.*;
import java.nio.ByteBuffer;

public class DtlsHandler extends ChannelDuplexHandler {
    private final SSLEngine sslEngine;

    public DtlsHandler(ParamBus bus) {
        try {
            this.sslEngine = bus.dtls.sslCtx.createSSLEngine();
            this.sslEngine.setUseClientMode(false);
            this.sslEngine.beginHandshake();
        } catch (SSLException e) {
            throw new RuntimeException("Failed to initialize DTLS handshake", e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        DatagramPacket datagram = (DatagramPacket) packet;
        ByteBuffer inNetBuf = datagram.content().nioBuffer();

        ByteBuffer appBuf = ByteBuffer.allocate(2048);
        SSLEngineResult result = sslEngine.unwrap(inNetBuf, appBuf);

        if (result.getStatus() == SSLEngineResult.Status.OK) {
            appBuf.flip();
            // Process decrypted data in appBuf
            // For example, echo back
            ByteBuffer outNetBuf = ByteBuffer.allocate(2048);
            sslEngine.wrap(appBuf, outNetBuf);
            outNetBuf.flip();
            ByteBuf outBuf = Unpooled.wrappedBuffer(outNetBuf);
            ctx.writeAndFlush(new DatagramPacket(outBuf, datagram.sender()));
        }
        // Handle handshake and other statuses as needed
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    }

}