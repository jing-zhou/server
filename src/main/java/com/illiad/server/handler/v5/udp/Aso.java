package com.illiad.server.handler.v5.udp;

import io.netty.channel.Channel;
import lombok.Data;
import java.net.InetSocketAddress;

@Data
public class Aso {

    private final Channel associate;
    private final Channel bind;
    // represent the originasl socket address (IP + port) of UDP packet
    private InetSocketAddress source;
    private Channel forward;

    public Aso(Channel associate, Channel bind) {
        this.associate = associate;
        this.bind = bind;
    }
}
