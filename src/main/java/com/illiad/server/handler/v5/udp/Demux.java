package com.illiad.server.handler.v5.udp;

import io.netty.channel.Channel;
import org.pcap4j.packet.IpPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Demux {
    private static final List<Session> pool = new ArrayList<>();

    private Demux() {}

    public static void addSession(Session session) {
        pool.add(session);
    }

    public static Session getSession(Channel channel) {
        for (Session session : pool) {
            if (session.getChannel() != null &&
                Objects.equals(session.getChannel().id().asShortText(), channel.id().asShortText())) {
                return session;
            }
        }
        return null;
    }

    public static Session getSession(Connection connection) {
        for (Session session : pool) {
            if (Objects.equals(session.getConnection(), connection)) {
                return session;
            }
        }
        return null;
    }

    // Should only create by connection, always check if session already exists before creating
    public static Session createSession(Connection connection) {
        Session session = new Session(null, connection);
        pool.add(session);
        return session;
    }

    public static boolean removeSession(Channel channel) {
        Iterator<Session> it = pool.iterator();
        while (it.hasNext()) {
            Session session = it.next();
            if (session.getChannel() != null &&
                Objects.equals(session.getChannel().id().asShortText(), channel.id().asShortText())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public static boolean removeSession(Connection connection) {
        Iterator<Session> it = pool.iterator();
        while (it.hasNext()) {
            Session session = it.next();
            if (Objects.equals(session.getConnection(), connection)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public static IpPacket getPacket(Channel channel) {
        Session session = getSession(channel);
        if (session != null) {
            return session.getPacket();
        }
        return null;
    }
}
