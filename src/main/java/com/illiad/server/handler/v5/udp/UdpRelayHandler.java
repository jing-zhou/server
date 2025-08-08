package com.illiad.server.handler.v5.udp;

import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.*;

public class UdpRelayHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws UnknownHostException {
        ByteBuf buf = packet.content();
        buf.skipBytes(3); // RSV (2 bytes) + FRAG (1 byte)
        byte atyp = buf.readByte();
        InetSocketAddress destAddr = parseAddress(buf, atyp);
        ByteBuf data = buf.slice(buf.readerIndex(), buf.readableBytes());

        // Forward payload to destination
        DatagramPacket forwardPacket = new DatagramPacket(data.retain(), destAddr);
        ctx.writeAndFlush(forwardPacket);

        // To relay response: receive from destAddr, wrap with SOCKS5 UDP header, send to client
    }

    private InetSocketAddress parseAddress(ByteBuf buf, byte atyp) throws UnknownHostException {
        if (atyp == 0x01) { // IPv4
            byte[] ip = new byte[4];
            buf.readBytes(ip);
            int port = buf.readUnsignedShort();
            return new InetSocketAddress(
                (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF),
                port
            );
        } else if (atyp == 0x03) { // Domain
            int len = buf.readByte();
            byte[] domain = new byte[len];
            buf.readBytes(domain);
            int port = buf.readUnsignedShort();
            return new InetSocketAddress(new String(domain), port);
        } else if (atyp == 0x04) { // IPv6
            byte[] ip = new byte[16];
            buf.readBytes(ip);
            int port = buf.readUnsignedShort();
            return new InetSocketAddress(InetAddress.getByAddress(ip), port);
        }
        throw new IllegalArgumentException("Unknown ATYP: " + atyp);
    }
}
