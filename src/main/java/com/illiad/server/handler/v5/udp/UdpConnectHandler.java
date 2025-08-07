package com.illiad.server.handler.v5.udp;

import com.illiad.server.HandlerNamer;
import com.illiad.server.UdpChannel;
import com.illiad.server.codec.v5.V5AddressDecoder;
import com.illiad.server.codec.v5.udp.UdpDecoder;
import com.illiad.server.codec.v5.udp.UdpHeaderDecoder;
import com.illiad.server.config.Params;
import com.illiad.server.handler.Utils;
import com.illiad.server.security.Ssl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.ssl.SslHandler;

import java.net.InetSocketAddress;

public class UdpConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private final Ssl ssl;
    private final Params params;
    private final HandlerNamer namer;
    private final V5AddressDecoder v5AddressDecoder;
    private final UdpChannel udpChannel;
    private final Utils utils;

    public UdpConnectHandler(Ssl ssl, Params params, HandlerNamer namer, V5AddressDecoder v5AddressDecoder, UdpChannel udpChannel, Utils utils) {

        this.ssl = ssl;
        this.params = params;
        this.namer = namer;
        this.v5AddressDecoder = v5AddressDecoder;
        this.udpChannel = udpChannel;
        this.utils = utils;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Socks5CommandRequest request) {

        // UDP channel had been created
        if (udpChannel.dChannel != null && !udpChannel.dChannel.isActive()) {

        } else {
            // Create a new UDP relay channel
            EventLoopGroup group = new NioEventLoopGroup(2);
            Bootstrap udpBootstrap = new Bootstrap();
            udpBootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {

                        }
                    })
                    .bind(params.getUdpHost(), params.getUdpPort())
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            Channel ch = future.channel();
                            udpChannel.dChannel = (NioDatagramChannel) ch;
                            SslHandler sslHandler = ssl.sslCtx.newHandler(ch.alloc(), params.getUdpHost(), params.getUdpPort());
                            ch.pipeline()
                                    .addLast(sslHandler)
                                    .addLast(new UdpHeaderDecoder());

                            // Build a successful UDP_ASSOCIATE response
                            Socks5CommandResponse response = new DefaultSocks5CommandResponse(
                                    Socks5CommandStatus.SUCCESS,
                                    request.dstAddrType(),
                                    params.getUdpHost(),
                                    ((InetSocketAddress) future.channel().localAddress()).getPort()
                            );
                            // Write the response to the client
                            ctx.writeAndFlush(response);
                        }
                    });
            // Build a successful UDP_ASSOCIATE response
            Socks5CommandResponse response = new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    Socks5AddressType.IPv4,
                    params.getLocalHost(),
                    params.getLocalPort()
            );
            // Write the response to the client
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        utils.closeOnFlush(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }
}