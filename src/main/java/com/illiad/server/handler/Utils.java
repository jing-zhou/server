package com.illiad.server.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.NetUtil;
import org.springframework.stereotype.Component;

@Component
public class Utils {
    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public Socks5AddressType addressType(String address) {
        if (NetUtil.isValidIpV4Address(address)) {
            return Socks5AddressType.IPv4;
        } else if (NetUtil.isValidIpV6Address(address)) {
            return Socks5AddressType.IPv6;
        } else {
            return Socks5AddressType.DOMAIN;
        }
    }
}
