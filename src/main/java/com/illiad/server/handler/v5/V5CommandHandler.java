package com.illiad.server.handler.v5;

import com.illiad.server.ParamBus;
import com.illiad.server.handler.v5.associate.AssociateHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;

public class V5CommandHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    private final ParamBus bus;
    public V5CommandHandler(ParamBus bus) {
        this.bus = bus;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest socksRequest) {

        // only socks5 commandS "CONNECT", "UDP_ASSOCIATE" are expected
        Socks5CommandType commandType = socksRequest.type();
        if (commandType == Socks5CommandType.CONNECT) {
            ctx.pipeline().addLast(bus.namer.generateName(), new V5ConnectHandler(bus));
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
        } else if (commandType == Socks5CommandType.UDP_ASSOCIATE) {
            ctx.pipeline().addLast(new AssociateHandler(bus));
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
        } else {
            // BIND is not supported in this implementation
            ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socksRequest.dstAddrType()));
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
        bus.utils.closeOnFlush(ctx.channel());
    }

}
