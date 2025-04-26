import ssl
import socket

from pathlib import Path

def find_file_path(filename, search_directory):
    search_path = Path(search_directory)
    for file in search_path.rglob(filename):
        return file.resolve()
    return None

def illiad_header(offset, secret):
    # Convert offset and secret to bytes if they are strings
    if isinstance(offset, str):
        offset = offset.encode('utf-8')
    if isinstance(secret, str):
        secret = secret.encode('utf-8')

    # Calculate lengths
    offset_length = len(offset)
    secret_length = len(secret)

    # Construct the header
    header = bytearray()
    header.append(offset_length)  # 1 byte for offset length
    header.extend(offset)        # Offset
    header.extend(b'\r\n')       # CRLF after offset
    header.extend(secret_length.to_bytes(2, 'big'))  # 2 bytes for secret length
    header.extend(secret)        # Secret
    header.extend(b'\r\n')       # CRLF after secret

    return header

# Example usage
offset1 = bytearray([0x08, 0x08, 0x08])  # Example offset
secret1 = bytearray([0x09, 0x09, 0x09])  # Example secret
header = illiad_header(offset1, secret1)
print("example illiad header:")
print(header)

def socks5_connect_request(offset, secret, address_type, address, port):

    # create illiad header
    request = illiad_header(offset, secret)

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

# Example usage
socks_request = socks5_connect_request(offset1, secret1, "Domain", "example.com", 80)
print("example SOCKS5 CONNECT request:")
print(socks_request)

def ssl_socks_connect(offset, secret, cert_path, host, port):
    try:
        # Create an SSL context
                # Create an SSL context and load the self-signed certificate
                context = ssl.create_default_context(cafile=cert_path)

                # Connect to the server
                with socket.create_connection((host, port)) as sock:
                    with context.wrap_socket(sock, server_hostname=host) as ssock:
                        # Print server certificate details
                        print("SSL established. Peer:", ssock.getpeercert())
                        req = socks5_connect_request(offset, secret, "Domain", "sina.com.cn", 80)
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
print()
print(cert_path)
# Query the SOCKS5 proxy
ssl_socks_connect(offset1, secret1, cert_path, "127.0.0.1", 2080)  # Replace with your proxy's IP and port