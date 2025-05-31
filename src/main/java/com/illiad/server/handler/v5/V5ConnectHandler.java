package com.illiad.server.handler.v5;

import com.illiad.server.HandlerNamer;
import com.illiad.server.handler.RelayHandler;
import com.illiad.server.handler.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;

public final class V5ConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private final HandlerNamer namer;
    private final Utils utils;

    public V5ConnectHandler(HandlerNamer namer, Utils utils) {
        this.namer = namer;
        this.utils = utils;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Socks5CommandRequest socks5Request) {

        Bootstrap b = new Bootstrap();
        b.group(ctx.channel().eventLoop()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                    }
                })
                .connect(socks5Request.dstAddr(), socks5Request.dstPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        Channel frontend = ctx.channel();
                        ChannelPipeline frontendPipeline = ctx.pipeline();
                        final Channel backend = future.channel();
                        final ChannelPipeline backendPipeline = backend.pipeline();
                        frontend.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5Request.dstAddrType(), socks5Request.dstAddr(), socks5Request.dstPort()))
                                .addListeners((ChannelFutureListener) future1 -> {
                                    if (future1.isSuccess()) {
                                        // setup Socks direct channel relay between frontend and backend
                                        frontendPipeline.addLast(new RelayHandler(backend, utils));
                                        backendPipeline.addLast(new RelayHandler(frontend, utils));
                                        // remove all handlers except , SslHandler from frontendPipeline
                                        String prefix = namer.getPrefix();
                                        for (String name : frontendPipeline.names()) {
                                            if (name.startsWith(prefix)) {
                                                frontendPipeline.remove(name);
                                            }
                                        }
                                    } else {
                                        utils.closeOnFlush(frontend);
                                        ctx.fireExceptionCaught(future1.cause());
                                        utils.closeOnFlush(backend);
                                    }
                                });
                    } else {
                        // Close the connection if the connection attempt has failed.
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5Request.dstAddrType()));
                        ctx.fireExceptionCaught(future.cause());
                        utils.closeOnFlush(ctx.channel());
                        utils.closeOnFlush(future.channel());
                    }
                });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        utils.closeOnFlush(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }
}
