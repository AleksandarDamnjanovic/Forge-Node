import json
import os
from pathlib import Path
import ssl
import sys
import urllib.error
import urllib.request

SCRIPT_DIR = Path(__file__).resolve().parent
CONF_FILE = SCRIPT_DIR / "configuration.conf"

DEFAULT_URL = "http://127.0.0.1:10001"

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


def load_configuration():
    cfg = {
        "url": os.environ.get("FORGE_NODE_URL", DEFAULT_URL),
        "apiKey": os.environ.get("FORGE_NODE_API_KEY", ""),
    }
    if CONF_FILE.is_file():
        try:
            for raw in CONF_FILE.read_text(encoding="utf-8").splitlines():
                line = raw.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, v = line.split("=", 1)
                k = k.strip()
                v = v.strip()
                if len(v) >= 2 and v.startswith('"') and v.endswith('"'):
                    v = v[1:-1].replace('\\"', '"')
                elif len(v) >= 2 and v.startswith("'") and v.endswith("'"):
                    v = v[1:-1]
                if k == "url" and v:
                    cfg["url"] = v
                elif k in ("apiKey", "api_key", "api") and v:
                    cfg["apiKey"] = v
        except Exception:
            pass
    return cfg


def _parse_port(raw):
    try:
        val = int(raw)
    except ValueError:
        return None
    return val if 1 <= val <= 65535 else None


def _build_url_from_host_port(base_url, host_override, port_override):
    if host_override and (host_override.startswith("http://") or host_override.startswith("https://")):
        base = host_override.rstrip("/")
        if port_override is not None:
            scheme, rest = base.split("://", 1)
            host_only = rest.split("/", 1)[0].split(":", 1)[0]
            return f"{scheme}://{host_only}:{port_override}"
        return base

    scheme = "http"
    host = "127.0.0.1"
    port = 10001
    if base_url and "://" in base_url:
        s, rest = base_url.split("://", 1)
        scheme = s
        authority = rest.split("/", 1)[0]
        if ":" in authority:
            h, p = authority.rsplit(":", 1)
            host = h
            parsed_p = _parse_port(p)
            if parsed_p is not None:
                port = parsed_p
        else:
            host = authority
            port = 443 if scheme == "https" else 80

    if host_override:
        host = host_override
    if port_override is not None:
        port = port_override

    if (scheme == "http" and port == 80) or (scheme == "https" and port == 443):
        return f"{scheme}://{host}"
    return f"{scheme}://{host}:{port}"


def parse_args(argv):
    cfg = load_configuration()
    url = cfg["url"]
    api_key = cfg["apiKey"]
    host_override = None
    port_override = None
    args = list(argv)

    idx = 0
    remaining = []
    while idx < len(args):
        tok = args[idx]
        if tok in ("--url", "-u"):
            if idx + 1 >= len(args):
                raise ValueError("Missing value after --url")
            url = args[idx + 1].strip()
            idx += 2
        elif tok.startswith("--url="):
            url = tok.split("=", 1)[1].strip()
            idx += 1
        elif tok in ("--api-key", "--key", "-k"):
            if idx + 1 >= len(args):
                raise ValueError("Missing value after --api-key")
            api_key = args[idx + 1].strip()
            idx += 2
        elif tok.startswith("--api-key=") or tok.startswith("--key="):
            api_key = tok.split("=", 1)[1].strip()
            idx += 1
        elif tok in ("--host", "-H"):
            if idx + 1 >= len(args):
                raise ValueError("Missing value after --host")
            host_override = args[idx + 1].strip()
            idx += 2
        elif tok.startswith("--host="):
            host_override = tok.split("=", 1)[1].strip()
            idx += 1
        elif tok in ("--port", "-p"):
            if idx + 1 >= len(args):
                raise ValueError("Missing value after --port")
            parsed = _parse_port(args[idx + 1])
            if parsed is None:
                raise ValueError(f"Invalid port: {args[idx + 1]}")
            port_override = parsed
            idx += 2
        elif tok.startswith("--port="):
            parsed = _parse_port(tok.split("=", 1)[1])
            if parsed is None:
                raise ValueError(f"Invalid port: {tok.split('=', 1)[1]}")
            port_override = parsed
            idx += 1
        else:
            remaining = args[idx:]
            break

    # Support legacy positional forms (<PORT> <CMD...>, <HOST> <PORT> <CMD...>, <HOST> <CMD...>)
    if remaining:
        first = remaining[0]
        first_port = _parse_port(first)
        if first_port is not None:
            remaining = remaining[1:]
        elif first not in KNOWN_COMMANDS:
            if len(remaining) >= 2:
                second_port = _parse_port(remaining[1])
                if second_port is not None:
                    host_override = first
                    port_override = second_port
                    remaining = remaining[2:]
                elif remaining[1] in KNOWN_COMMANDS:
                    host_override = first
                    remaining = remaining[1:]

    if not remaining:
        raise ValueError(
            "Not enough arguments. Usage: client.py [--url URL] [--api-key KEY] <command> [args...]"
        )

    if host_override is not None or port_override is not None:
        url = _build_url_from_host_port(url, host_override, port_override)

    return url.rstrip("/"), api_key, " ".join(remaining)


def main():
    try:
        base_url, api_key, command_str = parse_args(sys.argv[1:])
    except ValueError as e:
        print(f"ERROR: {e}")
        sys.exit(1)

    if not api_key:
        print(
            f"ERROR: Missing API key. Set 'apiKey' in {CONF_FILE} or pass --api-key <KEY>."
        )
        sys.exit(1)

    endpoint = (
        base_url
        if base_url.endswith("/api/au/command")
        else f"{base_url}/api/au/command"
    )
    payload = json.dumps({"command": command_str}).encode("utf-8")
    req = urllib.request.Request(
        endpoint,
        data=payload,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-API-Key": api_key,
            "Authorization": f"Bearer {api_key}",
        },
    )

    ssl_ctx = ssl._create_unverified_context() if endpoint.startswith("https://") else None

    try:
        with urllib.request.urlopen(req, timeout=20.0, context=ssl_ctx) as resp:
            raw = resp.read().decode("utf-8", errors="replace").strip()
            try:
                data = json.loads(raw)
                if isinstance(data, dict):
                    if not data.get("ok", False):
                        print(f"ERROR: {data.get('error', 'AU command failed')}")
                        sys.exit(1)
                    print(str(data.get("response", "")).strip())
                    return
            except json.JSONDecodeError:
                pass
            print(raw)
    except urllib.error.HTTPError as e:
        err_body = ""
        try:
            err_body = e.read().decode("utf-8", errors="replace").strip()
            err_json = json.loads(err_body)
            if isinstance(err_json, dict) and err_json.get("error"):
                err_body = err_json["error"]
        except Exception:
            pass
        if e.code == 401:
            print(
                f"ERROR: Unauthorized (HTTP 401). Check that apiKey in {CONF_FILE} matches the Forge Node dashboard API key."
            )
        elif e.code == 403:
            print(
                f"ERROR: Forbidden (HTTP 403). {err_body or 'Ensure Expose AU is enabled in the Forge Node dashboard.'}"
            )
        else:
            print(f"ERROR: HTTP {e.code} from {endpoint}: {err_body or e.reason}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"ERROR: Cannot connect to Forge Node server at {endpoint} ({e.reason}).")
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
