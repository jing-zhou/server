package com.illiad.server.handler.v5;

import com.illiad.server.HandlerNamer;
import com.illiad.server.UdpChannel;
import com.illiad.server.codec.v5.V5AddressDecoder;
import com.illiad.server.config.Params;
import com.illiad.server.handler.Utils;
import com.illiad.server.handler.v5.udp.UdpConnectHandler;
import com.illiad.server.security.Ssl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;

public class V5CommandHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    private final Ssl ssl;
    private final Params params;
    private final HandlerNamer namer;
    private final V5AddressDecoder v5AddressDecoder;
    private final UdpChannel updChannel;
    private final Utils utils;

    public V5CommandHandler(Ssl ssl, Params params, HandlerNamer namer, V5AddressDecoder v5AddressDecoder, UdpChannel updChannel, Utils utils) {
        this.ssl = ssl;
        this.params = params;
        this.namer = namer;
        this.v5AddressDecoder = v5AddressDecoder;
        this.updChannel = updChannel;
        this.utils = utils;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest socksRequest) {

        // only socks5 commandS "CONNECT", "UDP_ASSOCIATE" are expected
        Socks5CommandType commandType = socksRequest.type();
        if (commandType == Socks5CommandType.CONNECT) {
            ctx.pipeline().addLast(namer.generateName(), new V5ConnectHandler(namer, utils));
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
        } else if (commandType == Socks5CommandType.UDP_ASSOCIATE) {
            ctx.pipeline().addLast(namer.generateName(), new UdpConnectHandler(ssl, params, namer, v5AddressDecoder, updChannel, utils));
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(socksRequest);
        } else if (commandType == Socks5CommandType.BIND) {
            // BIND is not supported in this implementation
            ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socksRequest.dstAddrType()));
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
