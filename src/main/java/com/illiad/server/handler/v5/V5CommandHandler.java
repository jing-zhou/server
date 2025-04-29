package com.illiad.server.handler.v5;

import com.illiad.server.HandlerNamer;
import com.illiad.server.handler.Utils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class V5CommandHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    private final HandlerNamer namer;
    private final V5ConnectHandler connectHandler;
    private final Utils utils;

    public V5CommandHandler(HandlerNamer namer, V5ConnectHandler connectHandler, Utils utils) {
        this.namer = namer;
        this.connectHandler = connectHandler;
        this.utils = utils;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest socksRequest) {

        // only socks5 command (connect or udp) are expected
        if (socksRequest.type() == Socks5CommandType.CONNECT) {
            ctx.pipeline().addLast(namer.generateName(), connectHandler);
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
        } else {
            ctx.close();
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        ctx.fireExceptionCaught(throwable);
        utils.closeOnFlush(ctx.channel());
    }
}


