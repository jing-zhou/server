package com.illiad.handler;

import com.illiad.codec.HeaderEncoder;
import com.illiad.config.Params;
import com.illiad.security.Ssl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v4.*;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public final class ConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private final Ssl ssl;
    private final Params params;
    private final HeaderEncoder headerEncoder;
    private final Utils utils;

    public ConnectHandler(Ssl ssl, Params params, HeaderEncoder headerEncoder, Utils utils) {

        this.ssl = ssl;
        this.params = params;
        this.headerEncoder = headerEncoder;
        this.utils = utils;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) {

        Bootstrap b = new Bootstrap();
        Channel frontend = ctx.channel();
        ChannelPipeline frontendPipeline = ctx.pipeline();
        // defind a promise to handle the connection to the remote server
        Promise<Channel> promise = ctx.executor().newPromise();

        if (message instanceof Socks5CommandRequest socks5Request) {
            promise.addListener(new FutureListener<>() {
                @Override
                public void operationComplete(Future<Channel> future) {
                    final Channel backend = future.getNow();
                    final ChannelPipeline backendPipeline = backend.pipeline();
                    if (future.isSuccess()) {

                        frontend.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5Request.dstAddrType(), socks5Request.dstAddr(), socks5Request.dstPort()))
                                .addListeners(future1 -> {
                                    if (future1.isSuccess()) {
                                        // put frontend input on-hold until both frontend and backend are ready
                                        frontend.config().setAutoRead(false);
                                        // remove all handlers from backpipeline
                                        for (String name : backendPipeline.names()) {
                                            backendPipeline.remove(name);
                                        }
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
                                        ctx.channel().config().setAutoRead(true);
                                    } else {
                                        utils.closeOnFlush(ctx.channel());
                                        ctx.fireExceptionCaught(future1.cause());
                                    }
                                });
                    }
                }
            });

            b.group(ctx.channel().eventLoop()).channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ResponseHandler(promise));

            // connect to the proxy server, and forward the Socks connect command message to the remote server
            b.connect(socks5Request.dstAddr(), socks5Request.dstPort())
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                // Connection established use handler provided results
                            } else {
                                // Close the connection if the connection attempt has failed.
                                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5Request.dstAddrType()));
                                utils.closeOnFlush(ctx.channel());
                            }
                        }
                    });

        } else if (message instanceof Socks4CommandRequest socks4Request) {
            promise.addListener(new FutureListener<>() {
                @Override
                public void operationComplete(Future<Channel> future) {

                    final Channel backend = future.getNow();
                    final ChannelPipeline backendPipeline = backend.pipeline();
                    if (future.isSuccess()) {
                        frontend.writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS))
                                .addListeners(future1 -> {
                                    if (future1.isSuccess()) {
                                        // put frontend input on-hold until both frontend and backend are ready
                                        frontend.config().setAutoRead(false);
                                        // remove all handlers from backpipeline
                                        for (String name : backendPipeline.names()) {
                                            backendPipeline.remove(name);
                                        }
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
                                        ctx.channel().config().setAutoRead(true);
                                    } else {
                                        utils.closeOnFlush(ctx.channel());
                                        ctx.fireExceptionCaught(future1.cause());
                                    }
                                });
                    }
                }
            });


            b.group(ctx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ResponseHandler(promise));

            // connect to the proxy server, and forward the Socks connect command message to the remote server
            b.connect(socks4Request.dstAddr(), socks4Request.dstPort()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // Connection established use handler provided results
                    } else {
                        // Close the connection if the connection attempt has failed.
                        ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED));
                        utils.closeOnFlush(ctx.channel());
                    }
                }
            });

        } else {
            ctx.close();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        utils.closeOnFlush(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }
}
