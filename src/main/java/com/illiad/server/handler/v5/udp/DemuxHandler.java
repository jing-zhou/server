package com.illiad.server.handler.v5.udp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.pcap4j.packet.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ChannelHandler.Sharable
public class DemuxHandler extends SimpleChannelInboundHandler<List<IpPacket>> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<IpPacket> packets) {
        if (ctx == null || packets == null || packets.isEmpty()) {
            return;
        }
        for (IpPacket packet : packets) {

            if (packet == null) {
                continue;
            }

            Connection connection = Connection.extractConnection(packet);
            if (connection != null) {
                Session session = Demux.getSession(connection);
                if (session == null) {
                    // new session
                    session = Demux.createSession(connection);
                    session.addPacket(packet);
                    ctx.pipeline().addLast(new UdpConnectHandler());
                    ctx.fireChannelRead(connection);
                } else if (!session.isBufferEmpty()) {
                    // buffer the packet
                    session.addPacket(packet);
                } else if (session.isActive()) {
                    // write to active channel
                    session.writeAndFlush(packet);
                }
                // else: inactive channel, do nothing
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println(getClass().getSimpleName() + " caught exception: " + cause);
        ctx.fireExceptionCaught(cause);
        ctx.channel().close().addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Channel closed successfully");
            } else {
                System.out.println("Failed to close channel: " + future.cause());
            }
        });
    }

}
