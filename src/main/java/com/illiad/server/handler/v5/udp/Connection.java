package com.illiad.server.handler.v5.udp;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.Packet;
import java.net.InetAddress;

public class Connection {
    private final String sourceAddress;
    private final String destinationAddress;
    private final Integer sourcePort;
    private final Integer destinationPort;
    private final String protocol;
    private final int ipVersion;

    public Connection(String sourceAddress, String destinationAddress, Integer sourcePort, Integer destinationPort, String protocol, int ipVersion) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
        this.ipVersion = ipVersion;
    }

    public String getSourceAddress() { return sourceAddress; }
    public String getDestinationAddress() { return destinationAddress; }
    public Integer getSourcePort() { return sourcePort; }
    public Integer getDestinationPort() { return destinationPort; }
    public String getProtocol() { return protocol; }
    public int getIpVersion() { return ipVersion; }

    public static Connection extractConnection(Packet packet) {
        if (packet == null) return null;

        IpPacket ipPacket = null;
        if (packet instanceof IpPacket) {
            ipPacket = (IpPacket) packet;
        } else if (packet.contains(IpPacket.class)) {
            ipPacket = packet.get(IpPacket.class);
        }

        if (ipPacket == null) {
            System.out.println("Packet does not contain an IP layer.");
            return null;
        }

        int determinedIpVersion;
        if (ipPacket instanceof IpV4Packet) {
            determinedIpVersion = 4;
        } else if (ipPacket instanceof IpV6Packet) {
            determinedIpVersion = 6;
        } else {
            int versionFromHeader = ipPacket.getHeader().getVersion().value();
            if (versionFromHeader == 4 || versionFromHeader == 6) {
                determinedIpVersion = versionFromHeader;
            } else {
                System.out.println("Unknown IP version in packet header: " + ipPacket.getHeader().getVersion());
                return null;
            }
        }

        InetAddress sourceAddress = ipPacket.getHeader().getSrcAddr();
        InetAddress destinationAddress = ipPacket.getHeader().getDstAddr();
        Integer sourcePort = null;
        Integer destinationPort = null;
        String protocolName = ipPacket.getHeader().getProtocol().name();

        if (ipPacket.getPayload() instanceof TcpPacket) {
            TcpPacket tcpPacket = (TcpPacket) ipPacket.getPayload();
            sourcePort = tcpPacket.getHeader().getSrcPort().valueAsInt();
            destinationPort = tcpPacket.getHeader().getDstPort().valueAsInt();
        } else if (ipPacket.getPayload() instanceof UdpPacket) {
            UdpPacket udpPacket = (UdpPacket) ipPacket.getPayload();
            sourcePort = udpPacket.getHeader().getSrcPort().valueAsInt();
            destinationPort = udpPacket.getHeader().getDstPort().valueAsInt();
        } else {
            System.out.println("IP packet payload is not TCP or UDP. Protocol: " + ipPacket.getHeader().getProtocol() + " (Name: " + protocolName + ")");
        }

        return new Connection(
            sourceAddress.getHostAddress(),
            destinationAddress.getHostAddress(),
            sourcePort,
            destinationPort,
            protocolName,
            determinedIpVersion
        );
    }
}
