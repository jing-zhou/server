package com.illiad.server.handler.v5.udp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6ExtHopByHopOptionsPacket;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.namednumber.IpNumber;

import java.util.List;

@ChannelHandler.Sharable
public class DemuxHandler extends SimpleChannelInboundHandler<List<IpPacket>> {

    public static final DemuxHandler INSTANCE = new DemuxHandler();

    private DemuxHandler() {}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<IpPacket> packets) {
        if (ctx == null || packets == null || packets.isEmpty()) {
            return;
        }
        for (IpPacket packet : packets) {
            if (packet == null) {
                continue;
            } else if (packet instanceof IpV4Packet) {
                IpV4Packet ipV4Packet = (IpV4Packet) packet;
                if (IpNumber.ICMPV4.equals(ipV4Packet.getHeader().getProtocol())) {
                    // ICMPv4 packet, skip
                    continue;
                }
            } else if (packet instanceof IpV6Packet) {
                IpV6Packet ipV6Packet = (IpV6Packet) packet;
                if (IpNumber.ICMPV6.equals(ipV6Packet.getHeader().getNextHeader())) {
                    // ICMPv6 packet, skip
                    continue;
                } else if (IpNumber.IPV6_HOPOPT.equals(ipV6Packet.getHeader().getNextHeader())) {
                    if (ipV6Packet.getPayload() instanceof IpV6ExtHopByHopOptionsPacket) {
                        IpV6ExtHopByHopOptionsPacket hopByHopPacket =
                                (IpV6ExtHopByHopOptionsPacket) ipV6Packet.getPayload();
                        if (IpNumber.ICMPV6.equals(hopByHopPacket.getHeader().getNextHeader())) {
                            continue;
                        }
                    }
                }
            }

            Connection connection = Connection.extractConnection(packet);
            if (connection != null) {
                Session session = Demux.getSession(connection);
                if (session == null) {
                    // new session
                    session = Demux.createSession(connection);
                    session.addPacket(packet);
                    ctx.pipeline().addLast(HandlerNamer.name, new ConnectionHandler());
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
