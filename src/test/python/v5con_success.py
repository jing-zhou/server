import ssl
import socket
import hashlib

def create_header_with_sha256(data, crypto_type):
    """
    Creates a header byte array with SHA256 and a fixed offset of 8.

    :param data: Data to hash (bytes)
    :param crypto_type: Crypto type (1 byte)
    :return: Header byte array
    """
    offset = 8  # Fixed offset

    # Ensure crypto_type is 1 byte
    if not (0 <= crypto_type <= 0xFF):
        raise ValueError("Crypto type must be 1 byte")

    # Generate SHA256 hash
    signature = hashlib.sha256(data).digest()

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

def socks5_connect_request(data, crypto_type, address_type, address, port):

    # create illiad header
    request = create_header_with_sha256(data, crypto_type);

    # SOCKS5 version and CONNECT command
    request.extend(bytearray([0x05, 0x01, 0x00]))

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

def ssl_socks_connect(data, crypto_type, cert_path, host, port):
    try:
        # Create an SSL context
                # Create an SSL context and load the self-signed certificate
                context = ssl.create_default_context(cafile=cert_path)

                # Connect to the server
                with socket.create_connection((host, port)) as sock:
                    with context.wrap_socket(sock, server_hostname=host) as ssock:
                        # Print server certificate details
                        print("SSL established. Peer:", ssock.getpeercert())
                        req = socks5_connect_request(data, crypto_type, "Domain", "sina.com.cn", 80)
                        print("Sending SOCKS5 request:")
                        print(req)
                        ssock.sendall(req)

                        # Receive and print the response
                        response = ssock.recv(4096)
                        print("Server response:")
                        print(response.decode('utf-8'))

    except Exception as e:
            print("Error testing SSL server:", e)

cert_path = find_file_path("server.pem", "/home/wjz/pro/proxy/server/src/main/resources")
print(cert_path)
data = b"password"
print(data)
crypto_type = 0x20  # Example crypto type for SHA256
print(crypto_type)

# Query the SOCKS5 proxy
ssl_socks_connect(data, crypto_type, cert_path, "127.0.0.1", 2080)  # Replace with your proxy's IP and port