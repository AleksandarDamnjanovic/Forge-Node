#!/usr/bin/env python3
"""
Forge Node — MQTT Real-Time Web Monitor Bridge Server
- Reads/writes persistent broker credentials in UI/mqtt-monitor/mqtt.conf
- Connects to the Mosquitto TLS MQTT broker (using paho-mqtt + ca.crt & broker.crt)
  ONLY when the web session connects, and immediately disconnects when the user
  cancels the connection or closes the browser tab/session.
"""

import glob
import json
import os
import queue
import ssl
import sys
import threading
import time
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# Ensure user site-packages are on sys.path even when launched from Flatpak/IDE terminals
_extra_site_dirs = (
    glob.glob(os.path.expanduser("~/.local/lib/python*/site-packages"))
    + glob.glob(os.path.expanduser("~/.var/app/*/data/python/lib/python*/site-packages"))
)
for _p in _extra_site_dirs:
    if _p not in sys.path:
        sys.path.insert(0, _p)

try:
    import paho.mqtt.client as mqttClient
except ImportError:
    mqttClient = None

MONITOR_DIR = Path(__file__).resolve().parent
UI_ROOT = MONITOR_DIR.parent
PROJECT_ROOT = UI_ROOT.parent
WORKING_FN_PATH = PROJECT_ROOT / "AAU" / "script" / "working.fn"
CONF_PATH = MONITOR_DIR / "mqtt.conf"
UPLOADED_CERTS_DIR = MONITOR_DIR / "certs"

sse_clients = []
sse_lock = threading.Lock()


def load_mqtt_conf():
    """Parse UI/mqtt-monitor/mqtt.conf into a dictionary."""
    defaults = {
        "configured": False,
        "ip": "192.168.0.24",
        "port": 8883,
        "username": "admin",
        "password": "",
        "ca_cert_path": str(PROJECT_ROOT / "AAU" / "certs" / "ca.crt"),
        "broker_cert_path": str(PROJECT_ROOT / "AAU" / "certs" / "broker.crt"),
        "subscribe_admin": True,
    }
    if not CONF_PATH.exists():
        save_mqtt_conf(defaults)
        return defaults

    try:
        for raw_line in CONF_PATH.read_text(encoding="utf-8", errors="replace").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            key = k.strip()
            val = v.strip()
            if key in ("configured", "subscribe_admin"):
                defaults[key] = val.lower() in ("true", "1", "yes")
            elif key == "port":
                try:
                    defaults["port"] = int(val)
                except ValueError:
                    defaults["port"] = 8883
            elif key in defaults:
                defaults[key] = val
    except Exception:
        pass
    return defaults


def save_mqtt_conf(cfg):
    """Write configuration dictionary to UI/mqtt-monitor/mqtt.conf."""
    lines = [
        "# Forge Node MQTT Monitor — Saved Connection Configuration",
        "# Once you connect through the web UI dialog, 'configured' is set to true",
        "# and the page will connect automatically on startup using these credentials.",
        "",
        f"configured = {'true' if cfg.get('configured') else 'false'}",
        f"ip = {cfg.get('ip', '192.168.0.24')}",
        f"port = {int(cfg.get('port', 8883))}",
        f"username = {cfg.get('username', '')}",
        f"password = {cfg.get('password', '')}",
        f"ca_cert_path = {cfg.get('ca_cert_path', '')}",
        f"broker_cert_path = {cfg.get('broker_cert_path', '')}",
        f"subscribe_admin = {'true' if cfg.get('subscribe_admin', True) else 'false'}",
        "",
    ]
    CONF_PATH.write_text("\n".join(lines), encoding="utf-8")


def broadcast_event(payload_dict):
    data_str = f"data: {json.dumps(payload_dict)}\n\n"
    with sse_lock:
        dead = []
        for q in sse_clients:
            try:
                q.put_nowait(data_str)
            except Exception:
                dead.append(q)
        for d in dead:
            if d in sse_clients:
                sse_clients.remove(d)


class MqttBridgeState:
    def __init__(self):
        self.client = None
        self.connected = False
        self.last_error = ""
        self.topics = []
        self.lock = threading.Lock()

    def disconnect(self):
        with self.lock:
            if self.client is not None:
                try:
                    self.client.loop_stop()
                    self.client.disconnect()
                except Exception:
                    pass
                self.client = None
            was_connected = self.connected
            self.connected = False
            if was_connected:
                broadcast_event({"type": "status", "connected": False})

    def _persist_uploaded_cert(self, filename, content):
        UPLOADED_CERTS_DIR.mkdir(parents=True, exist_ok=True)
        safe_name = os.path.basename(filename or "cert.crt")
        target = UPLOADED_CERTS_DIR / safe_name
        target.write_text(content, encoding="utf-8")
        return str(target)

    def connect(
        self,
        ip,
        port,
        username,
        password,
        ca_cert_path,
        ca_cert_content,
        ca_cert_filename,
        broker_cert_path,
        broker_cert_content,
        broker_cert_filename,
        topics,
        subscribe_admin=True,
    ):
        if mqttClient is None:
            raise RuntimeError("paho-mqtt is not installed. Install with: pip install paho-mqtt")

        self.disconnect()

        # If the user uploaded certificate files from the dialog, persist them locally
        if ca_cert_content and ca_cert_content.strip():
            ca_cert_path = self._persist_uploaded_cert(ca_cert_filename or "ca.crt", ca_cert_content)
        if broker_cert_content and broker_cert_content.strip():
            broker_cert_path = self._persist_uploaded_cert(
                broker_cert_filename or "broker.crt", broker_cert_content
            )

        with self.lock:
            self.last_error = ""
            if hasattr(mqttClient, "CallbackAPIVersion"):
                client = mqttClient.Client(mqttClient.CallbackAPIVersion.VERSION1)
            else:
                client = mqttClient.Client()
            connect_event = threading.Event()
            connect_rc = [None]

            def on_connect(c, userdata, flags, rc):
                connect_rc[0] = rc
                if rc == 0:
                    self.connected = True
                    for t in self.topics:
                        c.subscribe(t)
                    broadcast_event({"type": "status", "connected": True})
                else:
                    self.connected = False
                    self.last_error = f"Broker refused connection (return code {rc})"
                    broadcast_event({"type": "status", "connected": False, "rc": rc, "error": self.last_error})
                connect_event.set()

            def on_message(c, userdata, msg):
                try:
                    text = msg.payload.decode("utf-8", errors="replace")
                except Exception:
                    text = str(msg.payload)
                broadcast_event({
                    "type": "mqtt_message",
                    "topic": msg.topic,
                    "payload": text,
                    "direction": "in",
                })

            def on_disconnect(c, userdata, rc):
                self.connected = False
                broadcast_event({"type": "status", "connected": False})

            client.on_connect = on_connect
            client.on_message = on_message
            client.on_disconnect = on_disconnect

            # Configure TLS context with ca.crt and broker.crt
            if ca_cert_path or broker_cert_path or int(port) == 8883:
                context = ssl._create_unverified_context()
                if ca_cert_path and os.path.exists(ca_cert_path):
                    context.load_verify_locations(cafile=ca_cert_path)
                if broker_cert_path and os.path.exists(broker_cert_path):
                    try:
                        context.load_verify_locations(cafile=broker_cert_path)
                    except Exception:
                        pass
                client.tls_set_context(context)
                client.tls_insecure_set(True)

            if username:
                client.username_pw_set(username, password or "")

            self.topics = list(topics or [])
            client.connect(ip, int(port), keepalive=60)
            client.loop_start()
            self.client = client

        # Save credentials to UI/mqtt-monitor/mqtt.conf
        save_mqtt_conf({
            "configured": True,
            "ip": ip,
            "port": int(port),
            "username": username,
            "password": password,
            "ca_cert_path": ca_cert_path,
            "broker_cert_path": broker_cert_path,
            "subscribe_admin": subscribe_admin,
        })

        # Wait briefly for initial broker handshake
        connect_event.wait(timeout=3.0)
        if connect_rc[0] is not None and connect_rc[0] != 0:
            self.disconnect()
            raise RuntimeError(f"MQTT broker returned error code {connect_rc[0]}")

        return {
            "ca_cert_path": ca_cert_path,
            "broker_cert_path": broker_cert_path,
        }

    def publish(self, topic, payload):
        with self.lock:
            if not self.client or not self.connected:
                raise RuntimeError("Not connected to MQTT broker")
            self.client.publish(topic, payload)


bridge = MqttBridgeState()


class ForgeNodeRequestHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(UI_ROOT), **kwargs)

    def log_message(self, format, *args):
        # Keep console output clean
        pass

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.end_headers()

    def send_json(self, status_code, data):
        body = json.dumps(data).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.startswith("/api/config"):
            cfg = load_mqtt_conf()
            self.send_json(200, {
                "ok": True,
                "config": cfg,
                "confPath": "UI/mqtt-monitor/mqtt.conf",
                "connected": bridge.connected,
            })
            return

        if self.path.startswith("/api/working-fn"):
            if WORKING_FN_PATH.exists():
                content = WORKING_FN_PATH.read_text(encoding="utf-8", errors="replace")
                self.send_json(200, {
                    "ok": True,
                    "path": "AAU/script/working.fn",
                    "content": content,
                })
            else:
                self.send_json(404, {"ok": False, "error": "AAU/script/working.fn not found"})
            return

        if self.path.startswith("/api/stream"):
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Connection", "keep-alive")
            self.end_headers()

            q = queue.Queue()
            with sse_lock:
                sse_clients.append(q)

            try:
                init_msg = f"data: {json.dumps({'type': 'status', 'connected': bridge.connected})}\n\n"
                self.wfile.write(init_msg.encode("utf-8"))
                self.wfile.flush()

                while True:
                    try:
                        msg = q.get(timeout=10)
                        self.wfile.write(msg.encode("utf-8"))
                        self.wfile.flush()
                    except queue.Empty:
                        self.wfile.write(b": keepalive\n\n")
                        self.wfile.flush()
            except Exception:
                pass
            finally:
                with sse_lock:
                    if q in sse_clients:
                        sse_clients.remove(q)
                    remaining = len(sse_clients)
                # If the browser tab/session closed and no active UI tabs remain, disconnect the MQTT client
                if remaining == 0:
                    def _delayed_cleanup():
                        time.sleep(1.5)
                        with sse_lock:
                            still_empty = len(sse_clients) == 0
                        if still_empty and bridge.connected:
                            bridge.disconnect()
                    threading.Thread(target=_delayed_cleanup, daemon=True).start()
            return

        return super().do_GET()

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        raw_body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
        try:
            body = json.loads(raw_body)
        except Exception:
            body = {}

        if self.path.startswith("/api/config"):
            cfg = load_mqtt_conf()
            cfg.update({
                "configured": bool(body.get("configured", cfg["configured"])),
                "ip": body.get("ip", cfg["ip"]),
                "port": int(body.get("port", cfg["port"])),
                "username": body.get("username", cfg["username"]),
                "password": body.get("password", cfg["password"]),
                "ca_cert_path": body.get("caCertPath", cfg["ca_cert_path"]),
                "broker_cert_path": body.get("brokerCertPath", cfg["broker_cert_path"]),
                "subscribe_admin": bool(body.get("subscribeAdmin", cfg["subscribe_admin"])),
            })
            save_mqtt_conf(cfg)
            self.send_json(200, {"ok": True, "config": cfg})
            return

        if self.path.startswith("/api/connect"):
            try:
                resolved = bridge.connect(
                    ip=body.get("ip", "192.168.0.24"),
                    port=body.get("port", 8883),
                    username=body.get("username", ""),
                    password=body.get("password", ""),
                    ca_cert_path=body.get("caCertPath", ""),
                    ca_cert_content=body.get("caCertContent", ""),
                    ca_cert_filename=body.get("caCertFileName", ""),
                    broker_cert_path=body.get("brokerCertPath", ""),
                    broker_cert_content=body.get("brokerCertContent", ""),
                    broker_cert_filename=body.get("brokerCertFileName", ""),
                    topics=body.get("topics", []),
                    subscribe_admin=bool(body.get("subscribeAdmin", True)),
                )
                self.send_json(200, {
                    "ok": True,
                    "caCertPath": resolved["ca_cert_path"],
                    "brokerCertPath": resolved["broker_cert_path"],
                })
            except Exception as e:
                self.send_json(400, {"ok": False, "error": str(e)})
            return

        if self.path.startswith("/api/disconnect") or self.path.startswith("/api/session-close"):
            bridge.disconnect()
            self.send_json(200, {"ok": True})
            return

        if self.path.startswith("/api/publish"):
            try:
                topic = body.get("topic", "")
                payload = body.get("payload", "")
                bridge.publish(topic, payload)
                broadcast_event({
                    "type": "mqtt_message",
                    "topic": topic,
                    "payload": payload,
                    "direction": "out",
                })
                self.send_json(200, {"ok": True})
            except Exception as e:
                self.send_json(400, {"ok": False, "error": str(e)})
            return

        self.send_json(404, {"ok": False, "error": "Unknown endpoint"})


class ReusableThreadingHTTPServer(ThreadingHTTPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
    server = ReusableThreadingHTTPServer(("0.0.0.0", port), ForgeNodeRequestHandler)
    paho_ver = getattr(mqttClient, "__version__", "loaded") if mqttClient else "NOT FOUND"
    print(
        f"Forge Node MQTT Monitor Bridge ready on http://127.0.0.1:{port}/mqtt-monitor/index.html (paho-mqtt: {paho_ver})",
        flush=True,
    )
    print(
        f"Type 'exit' and press Enter at any time to shut down the server and release port {port}.",
        flush=True,
    )

    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()

    try:
        while server_thread.is_alive():
            line = sys.stdin.readline()
            if not line:
                # stdin closed / running in background without interactive tty
                server_thread.join()
                break
            if line.strip().lower() == "exit":
                print(f"Shutting down server and releasing port {port}...", flush=True)
                break
    except KeyboardInterrupt:
        print(f"\nShutting down server and releasing port {port}...", flush=True)
    finally:
        bridge.disconnect()
        server.shutdown()
        server.server_close()
        print(f"Server stopped. Port {port} is now released.", flush=True)
