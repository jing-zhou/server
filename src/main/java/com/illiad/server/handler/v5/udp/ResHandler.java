package com.illiad.server.handler.v5.udp;

import com.illiad.server.ParamBus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

public class ResHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final ParamBus bus;

    public ResHandler(ParamBus bus) {
        this.bus = bus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket res) throws Exception {
        if (res != null) {
            Aso aso = bus.asos.getAsobyForward(ctx.channel());
            if (aso != null) {
                // form the SOCKS5 UDP response
                InetSocketAddress destSocketAddr = aso.getSource();
                ByteBuf buf = Unpooled.buffer();
                //create  SOCKS5 UDP header
                bus.utils.createSocks5UdpHeader(buf, destSocketAddr);
                DatagramPacket udpData = new DatagramPacket(res.content(), destSocketAddr, res.sender());
                // write UDP payload
                buf.writeBytes(udpData.content());
                // write response back to the bind channel
                aso.getBind().writeAndFlush(buf);
            }
        }
    }
}

