# Forge Node REST API — Developer Guide

This document explains how external developers, custom dashboards, mobile apps, and third-party automation systems can interact programmatically with a **Forge Node** instance over HTTP/HTTPS using the Forge Node REST API.

---

## 1. Architecture Overview

Forge Node exposes a single, API-key-authenticated HTTP endpoint on the Forge Node web server that forwards administrative commands through the **Administrative Unit (`AU`)** to the **Auxiliary Automation Unit (`AAU`)** over MQTT:

```text
[Developer App / Script]
       │  HTTP / HTTPS POST /api/au/command
       │  Header: X-API-Key: fn_...
       ▼
[Forge Node Web Server (Nginx :10011 / Spring Boot :10001)]
       │  Local loopback TCP (127.0.0.1:10010)
       ▼
[Administrative Unit (AU/au.py)]
       │  MQTT over TLS (topic: admin)
       ▼
[Auxiliary Automation Unit (AAU)]
```

### Default Base URLs
* **Docker deployment (via Nginx reverse proxy):**
  * `http://<host>:10011` (when `FN_SSL_MODE=none`)
  * `https://<host>:10011` (when `FN_SSL_MODE=self-signed`)
* **Native / Bare-Metal Spring Boot deployment:**
  * `http://<host>:10001`

---

## 2. Prerequisites & Setup in the Forge Node Dashboard

Before sending REST API requests, make sure the Administrative Unit is active in the Forge Node Dashboard:

1. Open the **Forge Node Dashboard** in your browser.
2. Locate the **🤖 Expose AU** card:
   * Check **Expose AU** (`exposeAu = true`).
   * If `AU Config` shows `Not set`, click **⚙️ Set up AU** and configure the MQTT broker connection for AU.
   * Click **▶ Start serving** so the status indicator turns green (`AU: Serving (10010)`).
3. Obtain your **API Key**:
   * On first startup, Forge Node automatically generates a random API key and stores it in `AAU/script/api/api.txt`.
   * Click **📋 Copy API** in the **Expose AU** section of the dashboard to copy the active API key to your clipboard.
   * Click **🔄 New API** at any time to revoke the old key and generate a brand-new random API key in `AAU/script/api/api.txt`.

---

## 3. Authentication

Every request to `/api/au/command` must include the active API key in an HTTP request header. Any of the following three header formats is accepted:

| Header Name | Format | Example |
| :--- | :--- | :--- |
| `X-API-Key` *(Recommended)* | `<API_KEY>` | `X-API-Key: fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f` |
| `X-Forge-Node-Key` | `<API_KEY>` | `X-Forge-Node-Key: fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f` |
| `Authorization` | `Bearer <API_KEY>` | `Authorization: Bearer fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f` |

---

## 4. Endpoint Specification

### `POST /api/au/command`

Executes an administrative command against the active Forge Node `.fn` script and live node state.

#### Request Headers
```http
Content-Type: application/json
Accept: application/json
X-API-Key: fn_your_api_key_here
```

#### Request Body (JSON)
```json
{
  "command": "<command-name> [arg1] [arg2] ..."
}
```

#### Successful Response (`200 OK`)
```json
{
  "ok": true,
  "response": "<command output from AU/AAU>"
}
```

#### Error Responses

| HTTP Status | Meaning | Example Response Body |
| :--- | :--- | :--- |
| `400 Bad Request` | Missing `"command"` field in request body | `{"ok": false, "error": "Missing 'command' in request body."}` |
| `401 Unauthorized` | Missing or invalid API key | `{"ok": false, "error": "Unauthorized"}` |
| `403 Forbidden` | `Expose AU` checkbox is disabled in the dashboard | `{"ok": false, "error": "Expose AU is disabled on this Forge Node instance."}` |
| `502 Bad Gateway` | Local `AU` process returned an empty response or socket error | `{"ok": false, "error": "Failed to communicate with local AU server..."}` |
| `503 Service Unavailable` | `AU` is not running (`Start serving` was not clicked) | `{"ok": false, "error": "AU server is not currently running. Start AU in the Forge Node Dashboard first."}` |

---

## 5. Core Forge Node Concepts for Developers

To control hardware and automation logic properly through the API, keep these `.fn` script rules in mind:

1. **Node Indices Start at `1`**:
   * Nodes are numbered `1..N`. Use `read-node-count` to discover `N`, then call `read-header <index>` on each node to inspect its human-readable name/location and type (`MASTER`, `SHUNT`, `BRIDGE`, `SLAVE`).
2. **Programmable Variables vs. Force Variables**:
   * Every controllable element (`switch`, `trigger`, `list`) has both a **programmable variable** (controlled by automated `.fn` programs) and a **force variable** (used for direct manual/API override):
     * `switch("light0", 0, $c1, $f1)` $\rightarrow$ `$c1` is programmable, `$f1` is force.
     * `trigger("heaterValue", 0, $t1, $ft1)` $\rightarrow$ `$t1` is programmable, `$ft1` is force.
     * `list("mode", 1, $lv, $ln, $l1, $lf1)` $\rightarrow$ `$l1` is programmable, `$lf1` is force.
   * **Direct Control Rule**: When your application wants to directly turn a switch on/off, set a trigger value, or select a list item, **update the force variable** (e.g. `$f1`), not the programmable variable (`$c1`).
   * **Releasing a Force Override**: All force variables are of type `string` with an initial empty value `""`. While `""`, automated `.fn` programs control the element. Setting `$f1` to `1` or `0` overrides automated programs. To release the override and return control to automated `.fn` programs, set the force variable to `null`:
     ```json
     {"command": "write-variable 1 $f1 null"}
     ```
3. **Reading Sensor Values**:
   * A sensor declaration looks like `sensor("temp", 0, $e1, $res01)`, where `$e1` is the polling epoch (in seconds) and `$res01` is the variable holding the latest reading.
   * Read the sensor's current value using `read-var <nodeIndex> <readingVar>` (e.g. `read-var 1 $res01`) or `get-sensor-readings <nodeIndex> "<sensorName>"`.
4. **Cross-Node Global Variables**:
   * When referencing a `global` variable `$var` declared on node `Z` from inside another node `X`, prefix it with `$Z` $\rightarrow$ `$Z$var` (for example, `$4$var` references `$var` from node `4`).
5. **Quoting Multi-Word Names**:
   * If a program, sensor, or element name contains spaces (e.g. `"living room light"`), wrap it in double quotes inside the command string:
     ```json
     {"command": "remove-serial-program 1 \"entire row\""}
     ```

---

## 6. Complete Command Reference

All 19 administrative commands supported by `POST /api/au/command`:

### 6.1 Discovery & Inspection Commands

| Command | Arguments | Description | Example `"command"` Value |
| :--- | :--- | :--- | :--- |
| `read-node-count` | *(none)* | Returns the total number of nodes in the system. | `"read-node-count"` |
| `read-header` | `<nodeIndex>` | Returns the `<header>` block of the specified node (location name, short ID, node role `[MASTER]`/`[SHUNT]`/`[BRIDGE]`/`[SLAVE]`). | `"read-header 1"` |
| `read-variables` | `<nodeIndex>` | Returns all `<variables>` declared on the node. | `"read-variables 1"` |
| `read-sensors` | `<nodeIndex>` | Returns all `<sensors>` declared on the node. | `"read-sensors 1"` |
| `read-elements` | `<nodeIndex>` | Returns all `<elements>` (`switch`, `trigger`, `list`) on the node. | `"read-elements 1"` |
| `read-programs` | `<nodeIndex>` | Returns all `<programs>` (`valCondition`, `timeLim`, `serial`) on the node. | `"read-programs 1"` |
| `read-var` | `<nodeIndex> <$varName>` | Reads the current live value of a single variable on the node. | `"read-var 1 $res01"` |
| `get-sensor-readings` | `<nodeIndex> <sensorName>` | Returns the current reading for a named sensor on the node. | `"get-sensor-readings 1 temp"` |
| `help` | *(none)* | Returns the built-in AU command reference text. | `"help"` |

---

### 6.2 Variable & Element Control Commands

#### `write-variable`
Updates the value of an existing variable.
* **Syntax:** `write-variable <nodeIndex> <$varName> <newValue>`
* **Examples:**
  * Turn switch on via force variable: `"write-variable 1 $f1 1"`
  * Turn switch off via force variable: `"write-variable 1 $f1 0"`
  * Reset force variable back to automatic (`""`): `"write-variable 1 $f1 null"`
  * Update an integer/float variable: `"write-variable 1 $targetTemp 22"`
  * Update a string time variable: `"write-variable 1 $start \"08:30\""`
  * Update an array variable (no spaces inside brackets unless quoted): `"write-variable 1 $swList [$c1,$c2,$c3]"`

#### `create-variable`
Creates a new variable on a node.
* **Syntax:** `create-variable <nodeIndex> <$varName> <isGlobal: True|False> <varType: int|float|string|fun|array> <initialValue>`
* **Examples:**
  * Local integer variable: `"create-variable 1 $ threshold False int 25"` $\rightarrow$ `"create-variable 1 $threshold False int 25"`
  * Global integer variable: `"create-variable 1 $sharedFlag True int 0"`
  * String variable: `"create-variable 1 $startTime False string \"18:00\""`
  * Function variable: `"create-variable 1 $nightMode False fun 0"`
  * Array variable: `"create-variable 1 $seqSwitches False array [$c1,$c2]"`

#### `remove-variable`
Deletes a non-essential variable from a node.
* **Syntax:** `remove-variable <nodeIndex> <$varName>`
* **Example:** `"remove-variable 1 $threshold"`

#### `change-trigger-value`
Directly updates a trigger element's value by the trigger's human-readable name.
* **Syntax:** `change-trigger-value <nodeIndex> <triggerName> <newValue>`
* **Example:** `"change-trigger-value 1 \"Color Red\" 180"`

#### `change-list-value`
Directly selects an active index on a list element by the list's human-readable name.
* **Syntax:** `change-list-value <nodeIndex> <listName> <selectedIndex>`
* **Example:** `"change-list-value 1 \"AC mode\" 2"`

---

### 6.3 Automation Program Commands

#### `add-value-condition-program`
Creates a conditional (`valCondition`) program on a node.
* **Syntax:**
  ```text
  add-value-condition-program <nodeIndex> <programName> <numConditions> <numResults>
    [<leftVar> <leftType> <rightVarOrVal> <rightType> <operator>] × numConditions
    [<targetVar> <targetType> <sourceVarOrVal> <sourceType> =] × numResults
    <functionVarName | None>
  ```
* **Supported condition operators:** `==`, `!=`, `<`, `>`, `<=`, `>=`
* **Supported result operator:** `=`
* **Example (1 condition: if `$res01 < 21`, set `$c1 = 1`):**
  ```text
  add-value-condition-program 1 "heater auto on" 1 1 $res01 int 21 int < $c1 int 1 int = None
  ```

#### `remove-value-condition-program`
Removes a `valCondition` program by name from a node.
* **Syntax:** `remove-value-condition-program <nodeIndex> <programName>`
* **Example:** `"remove-value-condition-program 1 \"heater auto on\""`

#### `add-time-limit-program`
Creates a 24-hour time window (`timeLim`) program that sets `<$funVar>` to `1` when the current system time is between `<$startVar>` and `<$endVar>`, and `0` otherwise.
* **Syntax:** `add-time-limit-program <nodeIndex> <$funVar> <$startVar> <$endVar>`
* **Example:** `"add-time-limit-program 1 $lightsON $start $end"`

#### `remove-time-limit-program`
Removes a `timeLim` program by its functional variable name.
* **Syntax:** `remove-time-limit-program <nodeIndex> <$funVar>`
* **Example:** `"remove-time-limit-program 1 $lightsON"`

#### `add-serial-program`
Creates a sequential switch rotation (`serial`) program.
* **Syntax:** `add-serial-program <nodeIndex> <programName> <$switchArrayVar> <$epochVar> [<$timestampVar>] <$lastIndexVar> <$funVar | None>`
* **Example:** `"add-serial-program 1 \"row cycle\" $listOfSwitches $switchEp $time $lastIndex None"`

#### `remove-serial-program`
Removes a `serial` program by name from a node.
* **Syntax:** `remove-serial-program <nodeIndex> <programName>`
* **Example:** `"remove-serial-program 1 \"row cycle\""`

---

## 7. Code Examples

### 7.1 `curl` (Command Line)

```bash
# 1. Get total number of nodes
curl -k -X POST http://127.0.0.1:10001/api/au/command \
  -H "Content-Type: application/json" \
  -H "X-API-Key: fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f" \
  -d '{"command": "read-node-count"}'

# 2. Read all elements (switches, triggers, lists) on Node 1
curl -k -X POST http://127.0.0.1:10001/api/au/command \
  -H "Content-Type: application/json" \
  -H "X-API-Key: fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f" \
  -d '{"command": "read-elements 1"}'

# 3. Turn ON switch force variable $f1 on Node 1
curl -k -X POST http://127.0.0.1:10001/api/au/command \
  -H "Content-Type: application/json" \
  -H "X-API-Key: fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f" \
  -d '{"command": "write-variable 1 $f1 1"}'
```
*(Note: Use `-k` with `curl` when connecting over `https://` to port `10011` with `FN_SSL_MODE=self-signed`.)*

---

### 7.2 Python (Standard Library — Zero Dependencies)

```python
import json
import ssl
import urllib.request

FORGE_NODE_URL = "http://127.0.0.1:10001"
API_KEY = "fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f"


def forge_node_command(command: str) -> str:
    endpoint = f"{FORGE_NODE_URL.rstrip('/')}/api/au/command"
    payload = json.dumps({"command": command}).encode("utf-8")
    req = urllib.request.Request(
        endpoint,
        data=payload,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-API-Key": API_KEY,
        },
    )
    ctx = ssl._create_unverified_context() if endpoint.startswith("https://") else None
    with urllib.request.urlopen(req, timeout=15.0, context=ctx) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        if not data.get("ok"):
            raise RuntimeError(data.get("error", "Unknown Forge Node error"))
        return data["response"]


if __name__ == "__main__":
    print("Node count:", forge_node_command("read-node-count"))
    print("Node 1 header:\n", forge_node_command("read-header 1"))
    print("Turning on switch $f1 on Node 1:", forge_node_command("write-variable 1 $f1 1"))
```

---

### 7.3 JavaScript / TypeScript (Node.js & Browser `fetch`)

```javascript
const FORGE_NODE_URL = "http://127.0.0.1:10001";
const API_KEY = "fn_7a9c4e2b8d1f6a3c5e0b9d4f2a8c6e1b3d5f7a9c2e4b6d8f";

async function sendForgeNodeCommand(command) {
  const response = await fetch(`${FORGE_NODE_URL}/api/au/command`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-API-Key": API_KEY,
    },
    body: JSON.stringify({ command }),
  });

  const data = await response.json();
  if (!response.ok || !data.ok) {
    throw new Error(data.error || `HTTP ${response.status}`);
  }
  return data.response;
}

// Usage example
(async () => {
  const count = await sendForgeNodeCommand("read-node-count");
  console.log("Total nodes:", count);

  const sensorVal = await sendForgeNodeCommand("read-var 1 $res01");
  console.log("Sensor reading:", sensorVal);
})();
```
