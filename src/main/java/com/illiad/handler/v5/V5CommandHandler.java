package com.illiad.handler.v5;

import com.illiad.handler.Utils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class V5CommandHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    private final V5ConnectHandler connectHandler;
    private final Utils utils;

    public V5CommandHandler(V5ConnectHandler connectHandler, Utils utils) {
        this.connectHandler = connectHandler;
        this.utils = utils;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest socksRequest) {

        // only socks5 command (connect or udp) are expected
        if (socksRequest.type() == Socks5CommandType.CONNECT) {
            ctx.pipeline().addLast(connectHandler);
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


