package com.illiad.server.handler.v5.udp;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import org.pcap4j.packet.IpPacket;

import java.util.ArrayList;
import java.util.List;

public class Session {

    @Setter
    @Getter
    private volatile Channel channel;

    @Setter
    @Getter
    private volatile Connection connection;
    private final List<IpPacket> buffer = new ArrayList<>();

    public Session(Channel channel, Connection connection) {
        this.channel = channel;
        this.connection = connection;
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public void writeAndFlush(IpPacket packet) {
        if (channel != null) {
            channel.writeAndFlush(packet);
        }
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

    public void addPacket(IpPacket packet) {
        buffer.add(packet);
    }

    public IpPacket getPacket() {
        if (!buffer.isEmpty()) {
            return buffer.remove(0);
        }
        return null;
    }

    public boolean isBufferEmpty() {
        return buffer.isEmpty();
    }
}
