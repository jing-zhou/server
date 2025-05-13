package com.illiad.server.handler.v5;

import com.illiad.server.HandlerNamer;
import com.illiad.server.handler.RelayHandler;
import com.illiad.server.handler.AckHandler;
import com.illiad.server.handler.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
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
        Channel frontend = ctx.channel();
        ChannelPipeline frontendPipeline = ctx.pipeline();
        // defind a promise to handle the connection to the remote server
        Promise<Channel> promise = ctx.executor().newPromise();

        promise.addListener((FutureListener<Channel>) future -> {
            final Channel backend = future.getNow();
            final ChannelPipeline backendPipeline = backend.pipeline();
            if (future.isSuccess()) {
                frontend.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5Request.dstAddrType(), socks5Request.dstAddr(), socks5Request.dstPort()))
                        .addListeners((ChannelFutureListener) future1 -> {
                            if (future1.isSuccess()) {
                                // setup Socks direct channel relay between frontend and backend
                                frontendPipeline.addLast(new RelayHandler(backend, utils));
                                backendPipeline.addLast(new RelayHandler(frontend, utils));
                                // remove all handlers except , SslHandler from frontendPipeline
                                // bug here we have removed pipeline tail
                                for (String name : frontendPipeline.names()) {
                                    if (name.startsWith(namer.getPrefix())) {
                                        frontendPipeline.remove(name);
                                    }
                                }
                            } else {
                                utils.closeOnFlush(frontend);
                                ctx.fireExceptionCaught(future1.cause());
                            }
                        });
            } else {
                frontend.close();
            }
        });

        b.group(ctx.channel().eventLoop()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new AckHandler(promise))
                .connect(socks5Request.dstAddr(), socks5Request.dstPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        // Connection established use handler provided results
                    } else {
                        // Close the connection if the connection attempt has failed.
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5Request.dstAddrType()));
                        utils.closeOnFlush(ctx.channel());
                    }
                });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        utils.closeOnFlush(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }
}
