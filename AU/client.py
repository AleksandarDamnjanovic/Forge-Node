import socket
import sys

HOST = "127.0.0.1"
if len(sys.argv) < 3:
    print("Not enough arguments")
    sys.exit(1)

PORT = int(sys.argv[1])
message = " ".join(sys.argv[2:]).strip()

if not message:
    print("Not enough arguments")
    sys.exit(1)

client = None
try:
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect((HOST, PORT))
    client.sendall(message.encode())
    chunks = []
    while True:
        chunk = client.recv(4096)
        if not chunk:
            break
        chunks.append(chunk)
    response = b"".join(chunks).decode("utf-8", errors="replace")
    print(response.strip())
except ConnectionRefusedError:
    print("ERROR: Cannot connect to server.")
except Exception as e:
    print(f"ERROR: {e}")
finally:
    if client is not None:
        client.close()