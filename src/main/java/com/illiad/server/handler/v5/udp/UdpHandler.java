package com.illiad.server.handler.v5.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.pcap4j.packet.IpPacket;

import java.util.List;

public class UdpHandler extends SimpleChannelInboundHandler<List<IpPacket>> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<IpPacket> msg) throws Exception {

    }
}
