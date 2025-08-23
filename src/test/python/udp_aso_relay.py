import ssl
import socket
import hashlib
import struct

def create_header_with_sha256(secret, crypto_type):
    """
    Creates a header byte array with SHA256 and a fixed offset of 8.

    :param secret: Data to hash (bytes)
    :param crypto_type: Crypto type (1 byte)
    :return: Header byte array
    """
    offset = 8  # Fixed offset

    # Ensure crypto_type is 1 byte
    if not (0 <= crypto_type <= 0xFF):
        raise ValueError("Crypto type must be 1 byte")

    # Generate SHA256 hash
    signature = hashlib.sha256(secret).digest()

    # Calculate the length (2 bytes for length + 1 byte for crypto_type + signature length + 1 byte for offset + 2 bytes for CRLF)
    length = 2 + 1 + len(signature) + 1 + 2

    # Ensure the length fits in 2 bytes
    if length > 0xFFFF:
        raise ValueError("Length exceeds 2 bytes")

    # Construct the header
    header = bytearray()
    header.extend(length.to_bytes(2, byteorder='big'))  # Length (2 bytes)
    header.append(crypto_type)  # Crypto type (1 byte)
    header.extend(signature)  # SHA256 signature (32 bytes)
    header.append(offset)  # Offset (1 byte)
    header.extend(b'\x0D\x0A')  # CRLF (2 bytes)

    return header

from pathlib import Path

def find_file_path(filename, search_directory):
    search_path = Path(search_directory)
    for file in search_path.rglob(filename):
        return file.resolve()
    return None

def socks5_associate_request(secret, crypto_type, address_type, address, port):

    # create illiad header
    request = create_header_with_sha256(secret, crypto_type)

    # SOCKS5 version and ASSOCIATE command
    request.extend(bytearray([0x05, 0x03, 0x00]))

    if address_type == "IPv4":
        # Address type: IPv4
        request.append(0x01)
        # Convert IPv4 address to bytes
        request.extend(map(int, address.split('.')))
    elif address_type == "Domain":
        # Address type: Domain name
        request.append(0x03)
        # Add the length of the domain name
        request.append(len(address))
        # Add the domain name bytes
        request.extend(address.encode('utf-8'))
    elif address_type == "IPv6":
        # Address type: IPv6
        request.append(0x04)
        # Convert IPv6 address to bytes
        request.extend(socket.inet_pton(socket.AF_INET6, address))
    else:
        raise ValueError("Invalid address type")

    # Add the port (2 bytes, big-endian)
    request.extend(port.to_bytes(2, 'big'))

    return request

# SOCKS5 UDP request header
# +----+------+------+----------+----------+----------+
# |RSV | FRAG | ATYP | DST.ADDR | DST.PORT |   DATA   |
# +----+------+------+----------+----------+----------+
# | 2  |  1   |  1   | Variable |    2     | Variable |
# +----+------+------+----------+----------+----------+

def socks5_udp_packet(ip, port, data):
    rsv = b'\x00\x00'
    frag = b'\x00'
    atyp = b'\x01'
    addr = socket.inet_aton(ip)
    port_bytes = struct.pack('>H', port)
    return rsv + frag + atyp + addr + port_bytes + data

def ssl_socks_udp(secret, crypto_type, cert_path, host, port, address_type, targetHost, targetPort, dataGram):
    try:
        # Create an SSL context
                # Create an SSL context and load the self-signed certificate
                context = ssl.create_default_context(cafile=cert_path)

                # Connect to the server
                with socket.create_connection((host, port)) as sock:
                    with context.wrap_socket(sock, server_hostname=host) as ssock:
                        # Print server certificate details
                        print("SSL established. Peer:", ssock.getpeercert())
                        req = socks5_associate_request(secret, crypto_type, address_type, targetHost, targetPort)
                        print("Sending SOCKS5 request:")
                        print(req)
                        ssock.sendall(req)

                        # Receive and interpret the response
                        resp = ssock.recv(10)
                        if resp[:2] != b'\x05\x00':
                            raise Exception('SOCKS5 UDP associate failed')
                        # Parse relay address
                        atyp = resp[3]
                        if atyp == 1:  # IPv4
                            relay_ip = socket.inet_ntoa(resp[4:8])
                            relay_port = struct.unpack('>H', resp[8:10])[0]
                            print(f'UDP relay at {relay_ip}:{relay_port}')
                            udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                            udp_sock.settimeout(5)
                            packet = socks5_udp_packet(targetHost, targetPort, dataGram)
                            print("relaying UDP packet:")
                            print(packet)
                            udp_sock.sendto(packet, (relay_ip, relay_port))

                            # Optional: If you expect a response from the server, you can listen for it
                            # Set a timeout for receiving a response (in seconds)
                            # set a much longer timer when debug, so the UDP channel won't close
                            # udp_sock.settimeout(600)
                            udp_sock.settimeout(5)
                            print("Waiting for response...")
                            data, addr = udp_sock.recvfrom(1024)  # Receive data (buffer size 1024 bytes)
                            print(data)
                            print(f"Received response: '{data.decode()}' from {addr}")
                        else:
                            raise Exception('Only IPv4 relay supported in this example')

    except Exception as e:
            print("Error testing SSL server:", e)

TARGET_HOST = '127.0.0.1'
TARGET_PORT = 5005
DNS_QUERY = "Hello, UDP Server!".encode('utf-8')
# TARGET_HOST = '8.8.8.8'
# TARGET_PORT = 53
# DNS_QUERY = b'\x12\x34\x01\x00\x00\x01\x00\x00\x00\x00\x00\x00\x03www\x06google\x03com\x00\x00\x01\x00\x01'

cert_path = find_file_path("server.pem", "/home/wjz/pro/proxy/server/src/main/resources")
print(cert_path)
secret = b"password"
print(secret)
crypto_type = 0x20  # Example crypto type for SHA256
print(crypto_type)

# UDP relay
ssl_socks_udp(secret, crypto_type, cert_path, "127.0.0.1", 2080, "IPv4", TARGET_HOST, TARGET_PORT, DNS_QUERY)  # Replace with your proxy's IP and port