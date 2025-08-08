package com.illiad.server.handler.v5;

import com.illiad.server.ParamBus;
import com.illiad.server.codec.v5.udp.UdpDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;

public class UdpAssociateHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private final ParamBus bus;

    public UdpAssociateHandler(ParamBus bus) {
        this.bus = bus;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Socks5CommandRequest request) {

        // UDP channel had been created
        NioDatagramChannel dChannel = bus.udpChannel.dChannel;
        if (dChannel != null && dChannel.isActive()) {
            InetSocketAddress localAddr = dChannel.localAddress();
            String host = localAddr.getHostString();
            Socks5CommandResponse response = new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    bus.utils.addressType(host),
                    host,
                    localAddr.getPort()
            );
            // Write the response to the client
            dChannel.writeAndFlush(response);
            ctx.pipeline().remove(this);
        } else {
            // Create a new UDP relay channel
            EventLoopGroup group = new NioEventLoopGroup(2);
            Bootstrap udpBootstrap = new Bootstrap();
            udpBootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    // Enable broadcasting if needed
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) {
                            ch.pipeline()
                                    .addLast(new UdpDecoder())
                                    .addLast(bus.demuxHandler);
                        }
                    })
                    .bind(bus.params.getUdpHost(), bus.params.getUdpPort())
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            Channel ch = future.channel();
                            // Store the UDP channel in ParamBus
                            bus.udpChannel.dChannel = (NioDatagramChannel) ch;
                            InetSocketAddress localAddr = dChannel.localAddress();
                            String host = localAddr.getHostString();
                            // Build a successful UDP_ASSOCIATE response
                            Socks5CommandResponse response = new DefaultSocks5CommandResponse(
                                    Socks5CommandStatus.SUCCESS,
                                    bus.utils.addressType(host),
                                    host,
                                    ((InetSocketAddress) future.channel().localAddress()).getPort()
                            );
                            // Write the response to the client
                            ch.writeAndFlush(response);
                            ctx.pipeline().remove(this);
                        }
                    });
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        bus.utils.closeOnFlush(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }
}