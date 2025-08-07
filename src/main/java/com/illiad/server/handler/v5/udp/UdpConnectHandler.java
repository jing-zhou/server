package com.illiad.server.handler.v5.udp;

import com.illiad.server.ParamBus;
import com.illiad.server.codec.v5.udp.UdpHeaderDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.ssl.SslHandler;

import java.net.InetSocketAddress;

public class UdpConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private final ParamBus bus;

    public UdpConnectHandler(ParamBus bus) {
        this.bus = bus;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Socks5CommandRequest request) {

        // UDP channel had been created
        if (bus.udpChannel.dChannel != null && !bus.udpChannel.dChannel.isActive()) {

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
                    .bind(bus.params.getUdpHost(), bus.params.getUdpPort())
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            Channel ch = future.channel();
                            bus.udpChannel.dChannel = (NioDatagramChannel) ch;
                            SslHandler sslHandler = bus.ssl.sslCtx.newHandler(ch.alloc(), bus.params.getUdpHost(), bus.params.getUdpPort());
                            ch.pipeline()
                                    .addLast(sslHandler)
                                    .addLast(new UdpHeaderDecoder(bus));

                            // Build a successful UDP_ASSOCIATE response
                            Socks5CommandResponse response = new DefaultSocks5CommandResponse(
                                    Socks5CommandStatus.SUCCESS,
                                    request.dstAddrType(),
                                    bus.params.getUdpHost(),
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
                    bus.params.getLocalHost(),
                    bus.params.getLocalPort()
            );
            // Write the response to the client
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        bus.utils.closeOnFlush(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }
}