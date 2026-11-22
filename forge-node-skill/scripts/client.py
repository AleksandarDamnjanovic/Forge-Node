import os
import socket
import sys

DEFAULT_HOST = os.environ.get("FORGE_NODE_HOST", "127.0.0.1")
DEFAULT_PORT = int(os.environ.get("FORGE_NODE_PORT", "10010"))

KNOWN_COMMANDS = {
    "read-header",
    "read-variables",
    "read-sensors",
    "read-elements",
    "read-programs",
    "read-node-count",
    "read-var",
    "get-sensor-readings",
    "add-serial-program",
    "remove-serial-program",
    "add-time-limit-program",
    "remove-time-limit-program",
    "add-value-condition-program",
    "remove-value-condition-program",
    "create-variable",
    "remove-variable",
    "change-list-value",
    "change-trigger-value",
    "write-variable",
    "help",
    "exit",
}


def _parse_port(raw):
    try:
        val = int(raw)
    except ValueError:
        return None
    return val if 1 <= val <= 65535 else None


def parse_args(argv):
    host = DEFAULT_HOST
    port = DEFAULT_PORT
    args = list(argv)

    # 1. Parse optional --host / -H and --port / -p flags in any order before command
    idx = 0
    remaining = []
    while idx < len(args):
        tok = args[idx]
        if tok in ("--host", "-H"):
            if idx + 1 >= len(args):
                raise ValueError("Missing value after --host")
            host = args[idx + 1]
            idx += 2
        elif tok.startswith("--host="):
            host = tok.split("=", 1)[1]
            idx += 1
        elif tok in ("--port", "-p"):
            if idx + 1 >= len(args):
                raise ValueError("Missing value after --port")
            parsed = _parse_port(args[idx + 1])
            if parsed is None:
                raise ValueError(f"Invalid port: {args[idx + 1]}")
            port = parsed
            idx += 2
        elif tok.startswith("--port="):
            parsed = _parse_port(tok.split("=", 1)[1])
            if parsed is None:
                raise ValueError(f"Invalid port: {tok.split('=', 1)[1]}")
            port = parsed
            idx += 1
        else:
            remaining = args[idx:]
            break

    # 2. Support positional forms for LLM/backward compatibility:
    #    - <PORT> <CMD...>
    #    - <HOST> <PORT> <CMD...>
    #    - <HOST> <CMD...>
    #    - <CMD...>
    if remaining:
        first = remaining[0]
        first_port = _parse_port(first)
        if first_port is not None:
            port = first_port
            remaining = remaining[1:]
        elif first not in KNOWN_COMMANDS:
            if len(remaining) >= 2:
                second_port = _parse_port(remaining[1])
                if second_port is not None:
                    host = first
                    port = second_port
                    remaining = remaining[2:]
                elif remaining[1] in KNOWN_COMMANDS:
                    host = first
                    remaining = remaining[1:]

    if not remaining:
        raise ValueError(
            "Not enough arguments. Usage: client.py [--host HOST] [--port PORT] <command> [args...]"
        )

    return host, port, " ".join(remaining)


def main():
    try:
        host, port, message = parse_args(sys.argv[1:])
    except ValueError as e:
        print(f"ERROR: {e}")
        sys.exit(1)

    client = None
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.settimeout(15.0)
        client.connect((host, port))
        client.sendall(message.encode("utf-8"))

        chunks = []
        while True:
            chunk = client.recv(4096)
            if not chunk:
                break
            chunks.append(chunk)
        response = b"".join(chunks).decode("utf-8", errors="replace").strip()
        if not response:
            print("ERROR: Empty response received from AU server.")
            sys.exit(1)
        print(response)
    except ConnectionRefusedError:
        print(f"ERROR: Cannot connect to AU server at {host}:{port} (connection refused).")
        sys.exit(1)
    except socket.timeout:
        print(f"ERROR: Timed out waiting for response from AU server at {host}:{port}.")
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)
    finally:
        if client is not None:
            client.close()


if __name__ == "__main__":
    main()
