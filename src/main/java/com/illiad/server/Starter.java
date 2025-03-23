package com.illiad.server;

import com.illiad.server.codec.HeaderDecoder;
import com.illiad.server.codec.v4.V4ServerEncoder;
import com.illiad.server.codec.v5.V5ServerEncoder;
import com.illiad.server.config.Params;
import com.illiad.server.handler.VersionHandler;
import com.illiad.server.handler.v4.V4CommandHandler;
import com.illiad.server.handler.v5.V5CommandHandler;
import com.illiad.server.security.Secret;
import com.illiad.server.security.Ssl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import org.springframework.stereotype.Component;

@Component
public class Starter {
    public Starter(Params params, V4ServerEncoder v4ServerEncoder, V4CommandHandler v4CommandHandler, V5ServerEncoder v5ServerEncoder, V5CommandHandler v5CommandHandler, Ssl ssl, Secret secret) {
        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup(3);
        EventLoopGroup workerGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(
                                    new SslHandler(ssl.sslCtx.newEngine(ch.alloc())),
                                    new LoggingHandler(LogLevel.INFO),
                                    new HeaderDecoder(secret),
                                    new VersionHandler(v4ServerEncoder, v4CommandHandler, v5ServerEncoder, v5CommandHandler));
                        }
                    });
            b.bind(params.getLocalPort()).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
