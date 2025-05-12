import requests

from pathlib import Path

def find_file_path(filename, search_directory):
    search_path = Path(search_directory)
    for file in search_path.rglob(filename):
        return file.resolve()
    return None

def make_https_request_with_self_signed_cert(url, cert_path):
    try:
        # Print the request details
        print(f"Making HTTPS GET request to: {url}")
        print(f"Using certificate: {cert_path}")

        # Verify the server's certificate using the provided self-signed certificate
        response = requests.get(url, verify=cert_path)
        print("Status Code:", response.status_code)
        print("Response Body:", response.text)
    except requests.RequestException as e:
        print("An error occurred:", e)

# Example usage
cert_path = find_file_path("server.pem", "/home/wjz/pro/proxy/server/src/main/resources")
url = "https://127.0.0.1:2080"  # Replace with your target URL
make_https_request_with_self_signed_cert(url, cert_path)