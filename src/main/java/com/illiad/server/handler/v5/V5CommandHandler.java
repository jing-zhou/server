package com.illiad.server.handler.v5;

import com.illiad.server.HandlerNamer;
import com.illiad.server.handler.Utils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;

public class V5CommandHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    private final HandlerNamer namer;
    private final Utils utils;

    public V5CommandHandler(HandlerNamer namer, Utils utils) {
        this.namer = namer;
        this.utils = utils;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest socksRequest) {

        // only socks5 commandS "CONNECT", "UDP_ASSOCIATE" are expected
        Socks5CommandType socksCommandType = socksRequest.type();
        if (socksCommandType == Socks5CommandType.CONNECT || socksCommandType == Socks5CommandType.UDP_ASSOCIATE) {
            ctx.pipeline().addLast(namer.generateName(), new V5ConnectHandler(namer, utils));
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
