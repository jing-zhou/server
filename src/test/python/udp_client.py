import socket

# Configuration for the UDP server
# Ensure these match the server's IP address and port
UDP_IP = "127.0.0.1"  # Replace with the actual IP address of your UDP server
UDP_PORT = 5005     # Replace with the actual port your UDP server is listening on

# The message you want to send to the server
#  You need to encode the string to bytes for transmission over the network
message = "Hello, UDP Server!".encode('utf-8')

# Create a UDP socket
# socket.AF_INET for IPv4, socket.SOCK_DGRAM for UDP
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

try:
    # Send the message to the UDP server
    print(f"Sending message: '{message.decode()}' to {UDP_IP}:{UDP_PORT}")
    sock.sendto(message, (UDP_IP, UDP_PORT))

    # Optional: If you expect a response from the server, you can listen for it
    # Set a timeout for receiving a response (in seconds)
    sock.settimeout(5)
    print("Waiting for response...")
    data, addr = sock.recvfrom(1024)  # Receive data (buffer size 1024 bytes)
    print(f"Received response: '{data.decode()}' from {addr}")

except socket.timeout:
    print("No response received within the timeout period.")
except Exception as e:
    print(f"An error occurred: {e}")
finally:
    # Close the socket when done
    sock.close()
    print("Socket closed.")
