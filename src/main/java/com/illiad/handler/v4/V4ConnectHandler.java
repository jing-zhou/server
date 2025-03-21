package com.illiad.handler.v4;

import com.illiad.handler.AckHandler;
import com.illiad.handler.RelayHandler;
import com.illiad.handler.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v4.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public final class V4ConnectHandler extends SimpleChannelInboundHandler<Socks4CommandRequest> {

    private final Utils utils;

    public V4ConnectHandler(Utils utils) {
        this.utils = utils;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Socks4CommandRequest socks4Request) {

        Bootstrap b = new Bootstrap();
        Channel frontend = ctx.channel();
        ChannelPipeline frontendPipeline = ctx.pipeline();
        // defind a promise to handle the connection to the remote server
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener((FutureListener<Channel>) future -> {

            final Channel backend = future.getNow();
            final ChannelPipeline backendPipeline = backend.pipeline();
            if (future.isSuccess()) {
                frontend.writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS))
                        .addListeners((ChannelFutureListener) future1 -> {
                            if (future1.isSuccess()) {
                                // remove all handlers except SslHandler from frontend pipeline
                                for (String name : frontendPipeline.names()) {
                                    ChannelHandler handler = frontendPipeline.get(name);
                                    if (!(handler instanceof SslHandler)) {
                                        frontendPipeline.remove(name);
                                    }
                                }
                                // setup Socks direct channel relay between frontend and backend
                                frontendPipeline.addLast(new RelayHandler(backend, utils));
                                backendPipeline.addLast(new RelayHandler(frontend, utils));
                                // resume frontend auto read
                                frontend.config().setAutoRead(true);
                            } else {
                                utils.closeOnFlush(ctx.channel());
                                ctx.fireExceptionCaught(future1.cause());
                            }
                        });
            }
        });


        b.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new AckHandler(promise));

        // connect to the proxy server, and forward the Socks connect command message to the remote server
        b.connect(socks4Request.dstAddr(), socks4Request.dstPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // Connection established use handler provided results
            } else {
                // Close the connection if the connection attempt has failed.
                ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED));
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

