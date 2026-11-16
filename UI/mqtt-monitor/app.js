/**
 * Forge Node — Real-Time MQTT Monitor & Topic Inspector
 * - Parses AAU/script/working.fn node headers & topics
 * - Loads/saves broker credentials from/to UI/mqtt-monitor/mqtt.conf
 * - Auto-connects immediately on startup when mqtt.conf is configured
 * - Disconnects & terminates MQTT session on Cancel or browser tab close
 * - Only real MQTT communication (no dummy/simulated data)
 */

const API_BASE =
  window.location.protocol === "file:" || window.location.port !== "8765"
    ? "http://127.0.0.1:8765"
    : "";

const WORKING_FN_SCRIPT = `1("North entrance", "northEntrance", [MASTER]){
    
    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global float   $res02 =        0.0
        global int     $current =      0
        global int     $mode =         0
        global int     $state =        0
        global int     $button =       0
        global int     $l1 =           0
        global int     $t1 =           0
        global int     $c1 =           0

        array          $lv =           [0, 1, 2]
        array          $ln =           ["off", "cooling", "heating"]
        string         $f1 =           ""
        string         $f2 =           ""
        string         $lf1 =          ""
        fun            $lightsON =     0
        string         $start =        "16:00"
        string         $end =          "6:00"
    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
        sensor("current", 2, NULL, $current)
        sensor("AC mode", 3, NULL, $mode)
        sensor("AC mode", 4, NULL, $state)
        sensor("AC button", 5, NULL, $button)
    </sensors>

    <elements>
        list("AC modes", 0, $lv, $ln, $l1, $lf1)
        trigger("AC switch", 1, $t1, $f1)
        switch("light", 0, $c1, $f2)
    </elements>

    <programs>
        $lightsON = valCondition(
            "light on",
            $lightsON == 1,
            $c1 = 1
        );
        valCondition(
            "light off",
            $lightsON == 0,
            $c1 = 0
        );
        timeLim($lightsON, $start, $end);
    </programs>

}

2("1. floor first panel", "1ffirstPanel", [MASTER]){

    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global int     $c1 =           1


	array   $listOfSwitches = [$c1, $3$c1, $4$c1]
	array   $listOfSwitchesCopy = [$c1, $3$c1, $4$c1, $6$c1, $7$c1, $7$c2, $7$c3, $7$c4]
        array   $listOfSwitches02 = [$3$c1 ,$c1, $4$c1, $6$c1, $7$c1, $7$c2, $7$c3, $7$c4]

	int $switchEp = 300
	int $switchEp02 = 450
	string $start = "00:05"
	string $end = "23:55"
	int $time = 0
	int $lastIndex = 0
	int $time02 = 0
	int $lastIndex02 = 0
	fun $fun01 = 0

        int $f1 = 1
    </variables>

    <sensors>
        sensor("dummy", 0, $e1, $res01)
    </sensors>

    <elements>
        switch("IR pannel", 0, $c1, $f1)
    </elements>

    <programs>
	serial("entire row", $listOfSwitchesCopy, $switchEp, $time, $lastIndex);
    </programs>

}

3("1. floor second panel", "1fsecondPanel", [MASTER]){

    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global float   $res02 =        0.0
        global int     $c1 =           0
        int            $f1 =           0
    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
    </sensors>

    <elements>
        switch("IR pannel", 0, $c1, $f1)
    </elements>

    <programs>
    </programs>

}

4("1. floor sleeping room", "1fsleepingRoom", [MASTER]){

    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global float   $res02 =        0.0
        global int     $c1 =           0
        int            $f1 =           0
    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
    </sensors>

    <elements>
        switch("IR pannel", 0, $c1, $f1)
    </elements>

    <programs>
    </programs>

}

5("Terrace light", "terraceLight", [MASTER]){

    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global float   $res02 =        0.0
        global int     $c1 =           0

        string         $f1 =           ""
        fun            $lightsON =     0
        string         $start =        "16:00"
        string         $end =          "6:00"
    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
    </sensors>

    <elements>
        switch("Lights", 0, $c1, $f1)
    </elements>

    <programs>
        $lightsON = valCondition(
            "light on",
            $lightsON == 1,
            $c1 = 1
        );
        valCondition(
            "light off",
            $lightsON == 0,
            $c1 = 0
        );
        timeLim($lightsON, $start, $end);
    </programs>

}

6("2. floor, first room", "2ffirstRoom", [MASTER]){

    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global float   $res02 =        0.0
        global int     $c1 =           0
        global int     $c2 =           0
        array $listOfSwitches = [$c1, $c2]
        int $switchEp = 300
        int $time = 0
        int $lastIndex = 0
        string $start = "20:00"
        string $end = "08:00"			
        int         $f1 =           0
        fun $f = 0
    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
    </sensors>

    <elements>
        switch("IR 1", 0, $c1, $f1)
    </elements>

    <programs>
    </programs>

}

7("2. floor, main hall", "2fmainHall", [MASTER]){

    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global float   $res02 =        0.0
        global int     $c1 =           0
        global int     $c2 =           0
        global int     $c3 =           0
        global int     $c4 =           0

        string         $f1 =           ""
    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
    </sensors>

    <elements>
        switch("IR pannel 1", 0, $c1, $f1)
        switch("IR pannel 2", 1, $c2, $f1)
        switch("IR pannel 3", 2, $c3, $f1)
        switch("IR pannel 4", 3, $c4, $f1)
    </elements>

    <programs>
    </programs>

}

8("2. floor, 1. led control unit", "2f1ledControl", [MASTER]){

    <variables>
        global int     $e1 =           10
        global int     $red =          0
        global int     $green =        0
        global int     $blue =         0
        global int     $r =            70
        global int     $g =            70
        global int     $b =            70
        global int     $mode =         0
        global int     $special =      0

        string         $rf =            ""
        string         $gf =            ""
        string         $bf =            ""
        string         $modef =         ""
        string         $specialf =      ""
        array          $lll =           [0-255]
        array          $prog =          [0, 1]
        array          $progname =      ["Direct", "Predefined programs"]
        array          $proglist =      [0, 1, 2]
    </variables>

    <sensors>
        sensor("red", 0, $e1, $red)
        sensor("green", 1, NULL, $green)
        sensor("green", 2, NULL, $blue)
        sensor("mode", 3, NULL, $mode)
        sensor("special", 4, NULL, $special)
    </sensors>

    <elements>
        list("Color Red", 0, $lll, $lll, $r, $rf)
        list("Color Green", 1, $lll, $lll, $g, $gf)
        list("Color Blue", 2, $lll, $lll, $b, $bf)
        list("Color ribbon mode", 3, $prog, $progname, $mode, $modef)
        list("Color program", 4, $proglist, $proglist, $special, $specialf)
    </elements>

    <programs>
    </programs>

}

9("2. floor, 2. led control unit", "2f2ledControl", [MASTER]){

    <variables>
        global int     $e1 =           10
        global int     $red =          0
        global int     $green =        0
        global int     $blue =         0
        global int     $r =            70
        global int     $g =            70
        global int     $b =            70
        global int     $mode =         0
        global int     $special =      0

        string         $rf =            ""
        string         $gf =            ""
        string         $bf =            ""
        string         $modef =         ""
        string         $specialf =      ""
        array          $lll =           [0-255]
        array          $prog =          [0, 1]
        array          $progname =      ["Direct", "Predefined programs"]
        array          $proglist =      [0, 1, 2]
    </variables>

    <sensors>
        sensor("red", 0, $e1, $red)
        sensor("green", 1, NULL, $green)
        sensor("green", 2, NULL, $blue)
        sensor("mode", 3, NULL, $mode)
        sensor("special", 4, NULL, $special)
    </sensors>

    <elements>
        list("Color Red", 0, $lll, $lll, $r, $rf)
        list("Color Green", 1, $lll, $lll, $g, $gf)
        list("Color Blue", 2, $lll, $lll, $b, $bf)
        list("Color ribbon mode", 3, $prog, $progname, $mode, $modef)
        list("Color program", 4, $proglist, $proglist, $special, $specialf)
    </elements>

    <programs>
    </programs>

}

10("2. floor, 3. led control unit", "2f3ledControl", [MASTER]){

    <variables>
        global int     $e1 =           10
        global int     $red =          0
        global int     $green =        0
        global int     $blue =         0
        global int     $r =            70
        global int     $g =            70
        global int     $b =            70
        global int     $mode =         0
        global int     $special =      0

        string         $rf =            ""
        string         $gf =            ""
        string         $bf =            ""
        string         $modef =         ""
        string         $specialf =      ""
        array          $lll =           [0-255]
        array          $prog =          [0, 1]
        array          $progname =      ["Direct", "Predefined programs"]
        array          $proglist =      [0, 1, 2]
    </variables>

    <sensors>
        sensor("red", 0, $e1, $red)
        sensor("green", 1, NULL, $green)
        sensor("green", 2, NULL, $blue)
        sensor("mode", 3, NULL, $mode)
        sensor("special", 4, NULL, $special)
    </sensors>

    <elements>
        list("Color Red", 0, $lll, $lll, $r, $rf)
        list("Color Green", 1, $lll, $lll, $g, $gf)
        list("Color Blue", 2, $lll, $lll, $b, $bf)
        list("Color ribbon mode", 3, $prog, $progname, $mode, $modef)
        list("Color program", 4, $proglist, $proglist, $special, $specialf)
    </elements>

    <programs>
    </programs>

}

11("test", "AC2f", [MASTER]){

    <variables>
        global int     $switch1 =           0
        global int     $switch2 =           0
        int	       $f1      =	    0
        int            $e1      =	    10
	array          $lll     =           [0-255]
        global int     $r       =           70
	global int     $red     =           0
	string         $rf      =           ""	
        array          $listOfSwitches =    [$switch1, $switch2]    
	int            $time    =           0
	int            $lastIndex      =    0
	int            $switchEp       =    5

    </variables>

    <sensors>
	sensor("red", 0, $e1, $red)
    </sensors>

    <elements>
        switch("led 1", 0, $switch1, $f1)
	switch("led 2", 1, $switch2, $f2)
	list("Color Red", 0, $lll, $lll, $r, $rf)
    </elements>

    <programs>
    </programs>

}`;

let state = {
  scriptSource: "AAU/script/working.fn",
  nodes: [], // parsed from working.fn
  selectedView: "all", // 'all' or index in state.nodes (0..N-1)
  nodeFilter: "",
  streamTopicFilter: "ALL",
  streamPaused: false,
  messages: [], // { id, timestamp, topic, nodeIndex, nodeName, payload, direction, decoded }
  topicStats: {}, // topic -> { count, lastPayload, lastTimestamp }
  conn: {
    configured: false,
    ip: "192.168.0.24",
    port: 8883,
    username: "admin",
    password: "00004444",
    caCertPath: "/home/irishpub/Programming/Forge Node/AAU/certs/ca.crt",
    caCertFileName: "",
    caCertFileContent: "",
    brokerCertPath: "/home/irishpub/Programming/Forge Node/AAU/certs/broker.crt",
    brokerCertFileName: "",
    brokerCertFileContent: "",
    subscribeAdmin: true,
    status: "disconnected", // 'disconnected' | 'connecting' | 'connected'
    activeTransport: "",
  },
  bridgeAvailable: false,
  sseSource: null,
};

// --- Parser for working.fn Node Headers & Telemetry Entities ---
function extractTag(block, tag) {
  const b = block.indexOf(`<${tag}>`);
  const e = block.indexOf(`</${tag}>`);
  if (b === -1 || e === -1 || e < b) return "";
  return block.slice(b + tag.length + 2, e);
}

function parseWorkingFn(scriptText) {
  const units = scriptText.split("}");
  const nodes = [];

  for (const unit of units) {
    if (
      !unit.includes("MASTER") &&
      !unit.includes("SLAVE") &&
      !unit.includes("SHUNT") &&
      !unit.includes("BRIDGE")
    ) {
      continue;
    }

    const closeParen = unit.indexOf(")");
    if (closeParen === -1) continue;
    const headerRaw = unit.slice(0, closeParen).trim();

    // e.g. 1("North entrance", "northEntrance", [MASTER])
    const idxMatch = headerRaw.match(/[0-9]+/);
    const nodeIndex = idxMatch ? parseInt(idxMatch[0], 10) : nodes.length + 1;

    const quoted = headerRaw.match(/"[^"]*"/g) || [];
    const nodeName = quoted[0] ? quoted[0].slice(1, -1) : `Node ${nodeIndex}`;
    const mqttTopic = quoted[1] ? quoted[1].slice(1, -1) : `topic_${nodeIndex}`;

    const roleMatch = headerRaw.match(/(MASTER|SLAVE|SHUNT|BRIDGE)/);
    const nodeRole = roleMatch ? roleMatch[1] : "MASTER";

    // Parse variables
    const varsBlock = extractTag(unit, "variables");
    const variables = [];
    const varMap = {};
    for (const rawLine of varsBlock.split(/\r?\n/)) {
      const line = rawLine.trim().replace(/\s+/g, " ");
      if (!line || !line.includes("=")) continue;
      const eq = line.indexOf("=");
      const left = line.slice(0, eq).trim().split(" ");
      const right = line.slice(eq + 1).trim();
      const isGlobal = left[0] === "global";
      const vType = isGlobal ? left[1] : left[0];
      const vName = isGlobal ? left[2] : left[1];
      let val = right;
      if (vType === "string" && val.startsWith('"') && val.endsWith('"')) {
        val = val.slice(1, -1);
      }
      const vObj = { isGlobal, type: vType, name: vName, initialValue: val, liveValue: val, updatedAt: null };
      variables.push(vObj);
      varMap[vName] = vObj;
    }

    // Parse sensors
    const sensorsBlock = extractTag(unit, "sensors");
    const sensors = [];
    for (const rawLine of sensorsBlock.split(/\r?\n/)) {
      const line = rawLine.trim().replace(/;$/, "");
      const m = line.match(/^sensor\s*\(\s*"([^"]*)"\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\s*\)/);
      if (m) {
        const sIndex = parseInt(m[2].trim(), 10) || 0;
        const epochVar = m[3].trim();
        const readingsVar = m[4].trim();
        sensors.push({
          name: m[1].trim(),
          index: sIndex,
          epochVar,
          readingsVar,
          liveValue: varMap[readingsVar] ? varMap[readingsVar].liveValue : "0.0",
          updatedAt: null,
        });
      }
    }

    // Parse elements (list, trigger, switch)
    const elementsBlock = extractTag(unit, "elements");
    const elements = [];
    for (const rawLine of elementsBlock.split(/\r?\n/)) {
      const line = rawLine.trim().replace(/;$/, "");
      if (line.startsWith("switch") || line.startsWith("trigger")) {
        const elType = line.startsWith("switch") ? "switch" : "trigger";
        const m = line.match(/^(?:switch|trigger)\s*\(\s*"([^"]*)"\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\s*\)/);
        if (m) {
          const valVar = m[3].trim();
          const forceVar = m[4].trim();
          elements.push({
            elType,
            name: m[1].trim(),
            index: parseInt(m[2].trim(), 10) || 0,
            valueVar: valVar,
            forceVar,
            liveValue: varMap[valVar] ? varMap[valVar].liveValue : "0",
            updatedAt: null,
          });
        }
      } else if (line.startsWith("list")) {
        const m = line.match(/^list\s*\(\s*"([^"]*)"\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\s*\)/);
        if (m) {
          const stateVar = m[5].trim();
          elements.push({
            elType: "list",
            name: m[1].trim(),
            index: parseInt(m[2].trim(), 10) || 0,
            listValuesVar: m[3].trim(),
            listNamesVar: m[4].trim(),
            valueVar: stateVar,
            forceVar: m[6].trim(),
            liveValue: varMap[stateVar] ? varMap[stateVar].liveValue : "0",
            updatedAt: null,
          });
        }
      }
    }

    nodes.push({
      index: nodeIndex,
      name: nodeName,
      topic: mqttTopic,
      type: nodeRole,
      headerText: `${nodeIndex}("${nodeName}", "${mqttTopic}", [${nodeRole}])`,
      variables,
      varMap,
      sensors,
      elements,
    });
  }

  return nodes;
}

// --- Utilities ---
function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function showToast(msg) {
  const container = document.getElementById("toastContainer");
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 2800);
}

function formatTime(date = new Date()) {
  return date.toTimeString().split(" ")[0];
}

// --- Initialize App & Auto-Connect from UI/mqtt-monitor/mqtt.conf ---
async function initApp() {
  // 1. Load AAU/script/working.fn
  let scriptToParse = WORKING_FN_SCRIPT;
  try {
    const res = await fetch(`${API_BASE}/api/working-fn`, { cache: "no-store" });
    if (res.ok) {
      const data = await res.json();
      state.bridgeAvailable = true;
      if (data.content && data.content.trim().length > 0) {
        scriptToParse = data.content;
        state.scriptSource = data.path || "AAU/script/working.fn";
      }
    }
  } catch (_) {
    state.bridgeAvailable = false;
  }

  loadScriptIntoState(scriptToParse, state.scriptSource);

  // 2. Load saved credentials from UI/mqtt-monitor/mqtt.conf
  try {
    const cfgRes = await fetch(`${API_BASE}/api/config`, { cache: "no-store" });
    if (cfgRes.ok) {
      const cfgData = await cfgRes.json();
      state.bridgeAvailable = true;
      if (cfgData.config) {
        const c = cfgData.config;
        state.conn.configured = Boolean(c.configured);
        state.conn.ip = c.ip || state.conn.ip;
        state.conn.port = Number(c.port) || 8883;
        state.conn.username = c.username ?? state.conn.username;
        state.conn.password = c.password ?? state.conn.password;
        state.conn.caCertPath = c.ca_cert_path || state.conn.caCertPath;
        state.conn.brokerCertPath = c.broker_cert_path || state.conn.brokerCertPath;
        state.conn.subscribeAdmin = c.subscribe_admin !== false;
      }
    }
  } catch (_) {}

  bindEvents();
  updateConnectionModalUI();
  renderAll();

  // 3. If mqtt.conf already has saved credentials (configured = true), connect immediately!
  //    Otherwise (first time), open the Connection Setup Dialog.
  if (state.conn.configured && state.bridgeAvailable) {
    document.getElementById("connModal").classList.add("hidden");
    showToast(`Loaded credentials from mqtt.conf — connecting to ${state.conn.ip}:${state.conn.port}...`);
    await connectToBroker(true);
  } else {
    openConnectionModal();
  }
}

function loadScriptIntoState(scriptText, sourceLabel) {
  state.scriptSource = sourceLabel;
  state.nodes = parseWorkingFn(scriptText);
  for (const n of state.nodes) {
    if (!state.topicStats[n.topic]) {
      state.topicStats[n.topic] = { count: 0, lastPayload: "—", lastTimestamp: "—" };
    }
  }
  if (!state.topicStats["admin"]) {
    state.topicStats["admin"] = { count: 0, lastPayload: "—", lastTimestamp: "—" };
  }
}

// --- Decode Real Forge Node Protocol Tokens & Update Live Node State ---
function processIncomingMqttMessage(topic, payload, direction = "in") {
  const nowStr = formatTime(new Date());
  if (!state.topicStats[topic]) {
    state.topicStats[topic] = { count: 0, lastPayload: "—", lastTimestamp: "—" };
  }
  state.topicStats[topic].count += 1;
  state.topicStats[topic].lastPayload = payload;
  state.topicStats[topic].lastTimestamp = nowStr;

  const targetNode = state.nodes.find((n) => n.topic === topic);
  const decodedSummaries = [];

  const cleanPayload = String(payload).trim();

  // Handle AAU 'admin' topic commands and '--response--' replies
  if (topic === "admin") {
    if (cleanPayload.startsWith("--response--")) {
      const inner = cleanPayload
        .replace(/^--response--/, "")
        .replace(/--end response--\s*$/, "")
        .trim();
      // If response contains variable lines (from readVariables), update the active/last queried node
      const activeNode =
        state.selectedView !== "all" ? state.nodes[state.selectedView] : state.lastAdminQueryNode;
      if (activeNode && inner.includes("$") && inner.includes("=")) {
        let updatedCount = 0;
        for (const rLine of inner.split(/\r?\n/)) {
          const l = rLine.trim().replace(/\s+/g, " ");
          const eq = l.indexOf("=");
          if (eq === -1) continue;
          const varMatch = l.slice(0, eq).match(/\$[a-zA-Z0-9_]+/);
          if (!varMatch) continue;
          const vName = varMatch[0];
          const vVal = l.slice(eq + 1).trim().replace(/^"|"$/g, "");
          if (activeNode.varMap[vName]) {
            activeNode.varMap[vName].liveValue = vVal;
            activeNode.varMap[vName].updatedAt = nowStr;
            updatedCount++;
          }
          for (const s of activeNode.sensors) {
            if (s.readingsVar === vName) {
              s.liveValue = vVal;
              s.updatedAt = nowStr;
            }
          }
          for (const el of activeNode.elements) {
            if (el.forceVar === vName || el.valueVar === vName) {
              const fVal = activeNode.varMap[el.forceVar] ? String(activeNode.varMap[el.forceVar].liveValue) : "";
              const cVal = activeNode.varMap[el.valueVar] ? String(activeNode.varMap[el.valueVar].liveValue) : "0";
              el.liveValue = fVal !== "" ? fVal : cVal;
              el.updatedAt = nowStr;
            }
          }
        }
        if (updatedCount > 0) {
          decodedSummaries.push(`AAU Admin Response: synced ${updatedCount} variables for Node #${activeNode.index}`);
        } else {
          decodedSummaries.push(`AAU Admin Response: ${inner}`);
        }
      } else {
        decodedSummaries.push(`AAU Admin Response: ${inner}`);
      }
    } else if (cleanPayload.includes("readVariables(")) {
      const m = cleanPayload.match(/readVariables\s*\(\s*([0-9]+)\s*\)/);
      if (m) {
        const nIdx = parseInt(m[1], 10);
        state.lastAdminQueryNode = state.nodes.find((n) => n.index === nIdx) || null;
        decodedSummaries.push(`Admin Query → readVariables(${nIdx})`);
      }
    } else if (cleanPayload.includes("writeVar(")) {
      const m = cleanPayload.match(/writeVar\s*\(\s*([0-9]+)\s*,\s*(\$[a-zA-Z0-9_]+)\s*,\s*([^)]+)\)/);
      if (m) {
        const nIdx = parseInt(m[1], 10);
        const vName = m[2].trim();
        const vVal = m[3].trim().replace(/^"|"$/g, "");
        const nodeObj = state.nodes.find((n) => n.index === nIdx);
        if (nodeObj && nodeObj.varMap[vName]) {
          nodeObj.varMap[vName].liveValue = vVal;
          nodeObj.varMap[vName].updatedAt = nowStr;
          // Refresh any element whose forceVar or valueVar is vName
          for (const el of nodeObj.elements) {
            if (el.forceVar === vName || el.valueVar === vName) {
              const fVal = nodeObj.varMap[el.forceVar] ? String(nodeObj.varMap[el.forceVar].liveValue) : "";
              const cVal = nodeObj.varMap[el.valueVar] ? String(nodeObj.varMap[el.valueVar].liveValue) : "0";
              el.liveValue = fVal !== "" ? fVal : cVal;
              el.updatedAt = nowStr;
            }
          }
        }
        decodedSummaries.push(`Admin Command → Node #${nIdx}: set ${vName} = ${vVal}`);
      }
    }
  }

  const tokens = cleanPayload.split(/\s+/).filter(Boolean);

  for (const tok of tokens) {
    const parts = tok.split("_").filter(Boolean);
    if (parts.length >= 3 && ["I", "Ci", "Cr", "Cq"].includes(parts[0])) {
      const cmd = parts[0];
      const nIdx = parseInt(parts[1], 10);
      const nodeObj = state.nodes.find((n) => n.index === nIdx) || targetNode;

      if (cmd === "Cq") {
        decodedSummaries.push(`Node #${nIdx} Update Request (Cq)`);
        continue;
      }

      const kind = parts[2]; // 'C' (Switch), 'T' (Trigger/List), 'S' or 'R' (Sensor)
      const itemIdx = parseInt(parts[3], 10);
      const val = parts.slice(4).join("_");

      if (nodeObj) {
        if (kind === "S" || kind === "R") {
          const sen = nodeObj.sensors.find((s) => s.index === itemIdx);
          if (sen && val !== "") {
            sen.liveValue = val;
            sen.updatedAt = nowStr;
            if (nodeObj.varMap[sen.readingsVar]) {
              nodeObj.varMap[sen.readingsVar].liveValue = val;
              nodeObj.varMap[sen.readingsVar].updatedAt = nowStr;
            }
            decodedSummaries.push(`Sensor "${sen.name}" (#${itemIdx}) = ${val}`);
          } else if (sen) {
            decodedSummaries.push(`Poll Sensor "${sen.name}" (#${itemIdx})`);
          }
        } else if (kind === "C") {
          const sw = nodeObj.elements.find((e) => e.elType === "switch" && e.index === itemIdx);
          if (sw && val !== "") {
            sw.liveValue = val;
            sw.updatedAt = nowStr;
            if (nodeObj.varMap[sw.valueVar]) {
              nodeObj.varMap[sw.valueVar].liveValue = val;
              nodeObj.varMap[sw.valueVar].updatedAt = nowStr;
            }
            decodedSummaries.push(`Switch "${sw.name}" (#${itemIdx}) → ${val === "1" ? "ON (1)" : "OFF (0)"}`);
          }
        } else if (kind === "T") {
          const el = nodeObj.elements.find(
            (e) => (e.elType === "trigger" || e.elType === "list") && e.index === itemIdx
          );
          if (el && val !== "") {
            el.liveValue = val;
            el.updatedAt = nowStr;
            if (nodeObj.varMap[el.valueVar]) {
              nodeObj.varMap[el.valueVar].liveValue = val;
              nodeObj.varMap[el.valueVar].updatedAt = nowStr;
            }
            decodedSummaries.push(`${el.elType} "${el.name}" (#${itemIdx}) → ${val}`);
          }
        }
      }
    }
  }

  const msgRecord = {
    id: Date.now() + Math.random(),
    timestamp: nowStr,
    topic,
    nodeIndex: targetNode ? targetNode.index : null,
    nodeName: targetNode ? targetNode.name : topic === "admin" ? "AAU Admin" : "External",
    payload: cleanPayload,
    direction,
    decoded:
      decodedSummaries.join(" · ") ||
      (targetNode ? `Topic "${topic}" (Node #${targetNode.index})` : `Topic "${topic}"`),
  };

  state.messages.unshift(msgRecord);
  if (state.messages.length > 250) {
    state.messages.pop();
  }

  renderWorkspace(topic);
  renderStreamFilterOptions();
  renderStreamList();
  renderHeaderMapTab();
}

// --- Connection Management ---
async function connectToBroker(fromAutoConfig = false) {
  if (!fromAutoConfig) {
    readConnectionModalInputs();
  }

  const topics = state.nodes.map((n) => n.topic);
  if (state.conn.subscribeAdmin && !topics.includes("admin")) {
    topics.push("admin");
  }

  state.conn.status = "connecting";
  updateTopbarStatusUI();

  try {
    const resp = await fetch(`${API_BASE}/api/connect`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        ip: state.conn.ip,
        port: Number(state.conn.port),
        username: state.conn.username,
        password: state.conn.password,
        caCertPath: state.conn.caCertPath,
        caCertContent: state.conn.caCertFileContent,
        caCertFileName: state.conn.caCertFileName,
        brokerCertPath: state.conn.brokerCertPath,
        brokerCertContent: state.conn.brokerCertFileContent,
        brokerCertFileName: state.conn.brokerCertFileName,
        subscribeAdmin: state.conn.subscribeAdmin,
        topics,
      }),
    });
    const resData = await resp.json();
    if (!resp.ok || !resData.ok) {
      throw new Error(resData.error || "Failed to connect to MQTT broker");
    }

    state.bridgeAvailable = true;
    state.conn.configured = true;
    if (resData.caCertPath) state.conn.caCertPath = resData.caCertPath;
    if (resData.brokerCertPath) state.conn.brokerCertPath = resData.brokerCertPath;

    state.conn.status = "connected";
    state.conn.activeTransport = "TLS TCP (paho-mqtt)";
    startSseStream();
    document.getElementById("connModal").classList.add("hidden");
    showToast(`Connected to ${state.conn.ip}:${state.conn.port} (saved to mqtt.conf)`);
    renderAll();
  } catch (err) {
    state.conn.status = "disconnected";
    updateTopbarStatusUI();
    showToast(`MQTT Connection Failed: ${err.message}`);
    // If auto-connect failed, open dialog so user can check IP/credentials
    openConnectionModal();
  }
}

function startSseStream() {
  if (state.sseSource) {
    state.sseSource.close();
  }
  const es = new EventSource(`${API_BASE}/api/stream`);
  es.onmessage = (ev) => {
    try {
      const data = JSON.parse(ev.data);
      if (data.type === "mqtt_message") {
        processIncomingMqttMessage(data.topic, data.payload, data.direction || "in");
      } else if (data.type === "status") {
        state.conn.status = data.connected ? "connected" : "disconnected";
        updateTopbarStatusUI();
        renderWorkspace();
      }
    } catch (_) {}
  };
  state.sseSource = es;
}

async function disconnectBroker() {
  if (state.sseSource) {
    state.sseSource.close();
    state.sseSource = null;
  }
  try {
    await fetch(`${API_BASE}/api/disconnect`, { method: "POST" });
  } catch (_) {}
  state.conn.status = "disconnected";
  state.conn.activeTransport = "";
  updateTopbarStatusUI();
  updateConnectionModalUI();
  renderAll();
  showToast("MQTT connection closed");
}

async function publishMessage(topic, payload) {
  if (!topic || !payload) return;
  if (state.conn.status !== "connected") {
    showToast("Not connected to MQTT broker. Connect first to send messages.");
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/api/publish`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ topic, payload }),
    });
    const data = await res.json();
    if (!res.ok || !data.ok) {
      showToast(`Publish failed: ${data.error || "Unknown error"}`);
      return;
    }
    showToast(`Published to "${topic}"`);
  } catch (e) {
    showToast(`Publish error: ${e.message}`);
  }
}

// --- Rendering ---
function renderAll() {
  updateTopbarStatusUI();
  updateStreamPanelVisibility();
  renderWorkspace();
  renderStreamFilterOptions();
  renderStreamList();
  renderHeaderMapTab();
}

function updateStreamPanelVisibility() {
  const layout = document.getElementById("studioLayout");
  const panel = document.getElementById("streamPanel");
  const chk = document.getElementById("chkToggleStream");
  if (!layout || !panel || !chk) return;
  const show = Boolean(chk.checked);
  layout.classList.toggle("show-stream", show);
  panel.classList.toggle("hidden", !show);
}

function updateTopbarStatusUI() {
  const dot = document.getElementById("connStatusDot");
  const label = document.getElementById("connBrokerLabel");
  const badge = document.getElementById("connStateBadge");
  const btnDisc = document.getElementById("btnDisconnectBroker");

  const hostStr = `${state.conn.ip}:${state.conn.port}`;

  if (state.conn.status === "connected") {
    dot.className = "status-dot connected";
    label.textContent = `${state.conn.username ? state.conn.username + "@" : ""}${hostStr}`;
    badge.className = "validation-pill valid";
    badge.textContent = "✓ Connected (TLS)";
    btnDisc.classList.remove("hidden");
  } else if (state.conn.status === "connecting") {
    dot.className = "status-dot connecting";
    label.textContent = `Connecting to ${hostStr}...`;
    badge.className = "validation-pill warn";
    badge.textContent = "Connecting…";
    btnDisc.classList.remove("hidden");
  } else {
    dot.className = "status-dot disconnected";
    label.textContent = `${hostStr} (${state.conn.username || "no-user"})`;
    badge.className = "validation-pill error";
    badge.textContent = "Disconnected";
    btnDisc.classList.add("hidden");
  }
}

function renderWorkspace(pulseTopic = null) {
  const ws = document.getElementById("monitorWorkspace");

  if (state.selectedView === "all") {
    renderAllTopicsOverview(ws, pulseTopic);
  } else {
    const node = state.nodes[state.selectedView];
    if (!node) {
      state.selectedView = "all";
      renderAllTopicsOverview(ws, pulseTopic);
      return;
    }
    renderSingleNodeMonitor(ws, node);
  }
}

function renderAllTopicsOverview(ws, pulseTopic = null) {
  const caCertShort = state.conn.caCertFileName
    ? `${state.conn.caCertFileName} (uploaded)`
    : state.conn.caCertPath.split("/").pop() || "ca.crt";
  const brokerCertShort = state.conn.brokerCertFileName
    ? `${state.conn.brokerCertFileName} (uploaded)`
    : state.conn.brokerCertPath.split("/").pop() || "broker.crt";

  ws.innerHTML = `
    <!-- Connection & Script Summary Banner Card -->
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">MQTT Broker</span>
          <span class="card-title">${escapeHtml(state.conn.ip)}:${escapeHtml(state.conn.port)}</span>
          <span class="card-subtitle">User: <code>${escapeHtml(state.conn.username || "none")}</code> · CA: <code>${escapeHtml(caCertShort)}</code> · Broker Cert: <code>${escapeHtml(brokerCertShort)}</code></span>
        </div>
        <div style="display:flex;gap:8px;">
          <button class="btn btn-sm btn-secondary" onclick="openConnectionModal()">⚙ Edit Broker Credentials (mqtt.conf)</button>
          ${
            state.conn.status === "connected" || state.conn.status === "connecting"
              ? `<button class="btn btn-sm btn-danger" onclick="disconnectBroker()">Cancel / Disconnect</button>`
              : `<button class="btn btn-sm btn-primary" onclick="connectToBroker(false)">🔌 Connect Now</button>`
          }
        </div>
      </div>
      <div class="card-body">
        <div class="telemetry-grid">
          <div class="telemetry-box">
            <div class="telemetry-box-top">
              <span class="telemetry-name">Parsed Nodes</span>
              <span class="subcard-badge badge-sensor">working.fn</span>
            </div>
            <div class="telemetry-value">${state.nodes.length}</div>
            <div class="telemetry-meta">
              <span>From <code>${escapeHtml(state.scriptSource)}</code></span>
            </div>
          </div>
          <div class="telemetry-box">
            <div class="telemetry-box-top">
              <span class="telemetry-name">MQTT Topics</span>
              <span class="subcard-badge badge-switch">Subscribed</span>
            </div>
            <div class="telemetry-value">${state.nodes.length + (state.conn.subscribeAdmin ? 1 : 0)}</div>
            <div class="telemetry-meta">
              <span>${state.nodes.length} node topics ${state.conn.subscribeAdmin ? "+ admin" : ""}</span>
            </div>
          </div>
          <div class="telemetry-box">
            <div class="telemetry-box-top">
              <span class="telemetry-name">Received Packets</span>
              <span class="subcard-badge badge-list">Real-Time</span>
            </div>
            <div class="telemetry-value">${state.messages.length}</div>
            <div class="telemetry-meta">
              <span>${escapeHtml(state.conn.activeTransport || "Disconnected")}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Nodes Dashboard Grid (3-4 Huge Dashboard Buttons per Row) -->
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">Dashboard</span>
          <span class="card-title">Forge Nodes Telemetry Dashboard (${state.nodes.length})</span>
          <span class="card-subtitle">Click any node button to open its live controls, sensors, switches, and variables</span>
        </div>
      </div>
      <div class="card-body">
        <div class="nodes-dashboard-grid">
          ${state.nodes
            .map((n, idx) => {
              const st = state.topicStats[n.topic] || { count: 0, lastTimestamp: "—" };
              const isPulse = pulseTopic && pulseTopic === n.topic;
              const sensorChips = n.sensors
                .slice(0, 3)
                .map(
                  (s) =>
                    `<span class="node-live-chip">${escapeHtml(s.name)}: <strong>${escapeHtml(s.liveValue)}</strong></span>`
                )
                .join("");
              const elementChips = n.elements
                .slice(0, 2)
                .map((el) => {
                  const isSwitch = el.elType === "switch";
                  const disp = isSwitch ? (el.liveValue === "1" ? "ON" : "OFF") : el.liveValue;
                  return `<span class="node-live-chip">${escapeHtml(el.name)}: <strong>${escapeHtml(disp)}</strong></span>`;
                })
                .join("");

              return `
                <button type="button" class="node-dashboard-btn ${isPulse ? "pulse-update" : ""}" onclick="selectNodeIndex(${idx})">
                  <div class="node-btn-header">
                    <div class="node-btn-title-wrap">
                      <div class="node-btn-id-row">
                        <span class="node-btn-idx">Node #${escapeHtml(n.index)}</span>
                        <span class="role-badge role-${escapeHtml(n.type)}">${escapeHtml(n.type)}</span>
                      </div>
                      <div class="node-btn-name">${escapeHtml(n.name)}</div>
                    </div>
                    <span class="msg-count-pill ${st.count > 0 ? "has-msgs" : ""}">${st.count} msg${st.count === 1 ? "" : "s"}</span>
                  </div>

                  <div class="node-btn-topic-row">
                    <span class="topic-pill">📡 ${escapeHtml(n.topic)}</span>
                  </div>

                  <div class="node-btn-metrics">
                    <div class="node-btn-metric">
                      <span class="node-btn-metric-val" style="color:var(--cyan);">${n.sensors.length}</span>
                      <span class="node-btn-metric-lbl">Sensors</span>
                    </div>
                    <div class="node-btn-metric">
                      <span class="node-btn-metric-val" style="color:var(--success);">${n.elements.length}</span>
                      <span class="node-btn-metric-lbl">Elements</span>
                    </div>
                    <div class="node-btn-metric">
                      <span class="node-btn-metric-val" style="color:var(--purple);">${n.variables.length}</span>
                      <span class="node-btn-metric-lbl">Variables</span>
                    </div>
                  </div>

                  ${
                    sensorChips || elementChips
                      ? `<div class="node-btn-live-pills">${sensorChips}${elementChips}</div>`
                      : ""
                  }

                  <div class="node-btn-footer">
                    <span>⏱ Updated: ${escapeHtml(st.lastTimestamp)}</span>
                    <span style="color:var(--primary);font-weight:600;">Open Node →</span>
                  </div>
                </button>
              `;
            })
            .join("")}
        </div>
      </div>
    </div>
  `;
}

function renderSingleNodeMonitor(ws, node) {
  const st = state.topicStats[node.topic] || { count: 0, lastTimestamp: "—" };

  ws.innerHTML = `
    <!-- Node Header & Topic Info Card -->
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <button class="btn btn-xs btn-secondary" onclick="selectAllTopicsView()">← All Nodes Dashboard</button>
          <span class="card-tag">Node #${escapeHtml(node.index)}</span>
          <span class="card-title">${escapeHtml(node.name)}</span>
          <span class="topic-pill">📡 Topic: ${escapeHtml(node.topic)}</span>
          <span class="role-badge role-${escapeHtml(node.type)}">${escapeHtml(node.type)}</span>
        </div>
        <div style="display:flex;align-items:center;gap:8px;">
          <span class="msg-count-pill ${st.count > 0 ? "has-msgs" : ""}">${st.count} MQTT packets</span>
          <span style="font-family:var(--font-mono);font-size:11px;color:var(--text-muted);">Updated: ${escapeHtml(st.lastTimestamp)}</span>
          <button class="btn btn-xs btn-primary" onclick="requestNodeSensors(${node.index})">
            🔄 Read Node Variables via Admin
          </button>
        </div>
      </div>
    </div>

    <!-- Real-Time Sensors Card -->
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">&lt;sensors&gt;</span>
          <span class="card-title">Real-Time Sensor Readings (${node.sensors.length})</span>
          <span class="card-subtitle">Updated live from topic <code>${escapeHtml(node.topic)}</code></span>
        </div>
      </div>
      <div class="card-body">
        ${
          node.sensors.length === 0
            ? `<div class="empty-state">No sensors defined for Node #${node.index}</div>`
            : `<div class="telemetry-grid">
                ${node.sensors
                  .map(
                    (s) => `
                  <div class="telemetry-box">
                    <div class="telemetry-box-top">
                      <span class="telemetry-name">${escapeHtml(s.name)}</span>
                      <span class="subcard-badge badge-sensor">sensor #${s.index}</span>
                    </div>
                    <div class="telemetry-value">${escapeHtml(s.liveValue)}</div>
                    <div class="telemetry-meta">
                      <span>var: <code>${escapeHtml(s.readingsVar)}</code></span>
                      <span>${s.updatedAt ? "⏱ " + escapeHtml(s.updatedAt) : "initial"}</span>
                    </div>
                  </div>
                `
                  )
                  .join("")}
              </div>`
        }
      </div>
    </div>

    <!-- Real-Time Elements Card (Switches, Triggers, Lists) -->
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">&lt;elements&gt;</span>
          <span class="card-title">Interactive Elements &amp; Force Override Controls (${node.elements.length})</span>
          <span class="card-subtitle">Commands are published to <code>admin</code> topic using each element's force variable</span>
        </div>
      </div>
      <div class="card-body">
        ${
          node.elements.length === 0
            ? `<div class="empty-state">No elements defined for Node #${node.index}</div>`
            : `<div class="telemetry-grid">
                ${node.elements
                  .map((el) => {
                    const isSwitch = el.elType === "switch";
                    const ctrlVal = node.varMap[el.valueVar] ? String(node.varMap[el.valueVar].liveValue) : "0";
                    const forceVal = node.varMap[el.forceVar] ? String(node.varMap[el.forceVar].liveValue) : "";
                    const effectiveVal = forceVal !== "" ? forceVal : ctrlVal;
                    const nextSwitchVal = effectiveVal === "1" ? "0" : "1";
                    const adminCmd = `writeVar(${node.index}, ${el.forceVar}, ${isSwitch ? nextSwitchVal : effectiveVal})`;
                    return `
                    <div class="telemetry-box">
                      <div class="telemetry-box-top">
                        <span class="telemetry-name">${escapeHtml(el.name)}</span>
                        <span class="subcard-badge badge-${escapeHtml(el.elType)}">${escapeHtml(el.elType)} #${el.index}</span>
                      </div>
                      <div class="telemetry-value" style="color:${isSwitch ? (effectiveVal === "1" ? "var(--success)" : "var(--text-muted)") : "var(--purple)"}">
                        ${isSwitch ? (effectiveVal === "1" ? "ON (1)" : "OFF (0)") : escapeHtml(effectiveVal)}
                      </div>
                      <div class="telemetry-meta" style="flex-direction:column;align-items:stretch;gap:6px;">
                        <div style="display:flex;justify-content:space-between;font-size:11px;">
                          <span>control: <code>${escapeHtml(el.valueVar)}</code> = <strong>${escapeHtml(ctrlVal)}</strong></span>
                          <span>force: <code>${escapeHtml(el.forceVar)}</code> = <strong style="color:var(--warning)">${escapeHtml(forceVal === "" ? '""' : forceVal)}</strong></span>
                        </div>
                        ${
                          isSwitch
                            ? `<div style="display:flex;gap:6px;justify-content:space-between;align-items:center;flex-wrap:wrap;">
                                <span style="font-size:10px;color:var(--text-dim);">→ admin: <code>${escapeHtml(adminCmd)}</code></span>
                                <div style="display:flex;gap:4px;">
                                  <button class="btn btn-xs btn-primary" onclick="toggleSwitchForce(${node.index}, '${escapeHtml(el.forceVar)}', '${escapeHtml(nextSwitchVal)}')">
                                    Force ${nextSwitchVal === "1" ? "ON (1)" : "OFF (0)"}
                                  </button>
                                  <button class="btn btn-xs btn-secondary" title='Release force override: writeVar(${node.index}, ${escapeHtml(el.forceVar)}, "")' onclick='toggleSwitchForce(${node.index}, "${escapeHtml(el.forceVar)}", "\"\"")'>
                                    Auto ("")
                                  </button>
                                </div>
                              </div>`
                            : `<div style="display:flex;gap:6px;align-items:center;">
                                <input type="text" class="input input-sm input-mono" id="forceInput_${node.index}_${el.elType}_${el.index}" value="${escapeHtml(effectiveVal)}" placeholder="Force value" style="padding:3px 6px;font-size:11px;" />
                                <button class="btn btn-xs btn-primary" onclick="sendElementForce(${node.index}, '${escapeHtml(el.forceVar)}', 'forceInput_${node.index}_${el.elType}_${el.index}')">
                                  Force
                                </button>
                                <button class="btn btn-xs btn-secondary" title='Release force override: writeVar(${node.index}, ${escapeHtml(el.forceVar)}, "")' onclick='toggleSwitchForce(${node.index}, "${escapeHtml(el.forceVar)}", "\"\"")'>
                                  Auto ("")
                                </button>
                              </div>`
                        }
                      </div>
                    </div>
                  `;
                  })
                  .join("")}
              </div>`
        }
      </div>
    </div>

    <!-- Node Variables Table -->
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">&lt;variables&gt;</span>
          <span class="card-title">Node Variables (${node.variables.length})</span>
        </div>
      </div>
      <div class="card-body" style="padding: 8px 14px;">
        <table class="items-table">
          <thead>
            <tr>
              <th>Variable</th>
              <th>Scope</th>
              <th>Type</th>
              <th>Initial Value (working.fn)</th>
              <th>Live MQTT Value</th>
              <th>Last Updated</th>
            </tr>
          </thead>
          <tbody>
            ${node.variables
              .map(
                (v) => `
              <tr>
                <td style="font-family:var(--font-mono);font-weight:600;color:var(--primary);">${escapeHtml(v.name)}</td>
                <td><span class="node-idx-badge">${v.isGlobal ? "global" : "local"}</span></td>
                <td style="font-family:var(--font-mono);">${escapeHtml(v.type)}</td>
                <td style="font-family:var(--font-mono);color:var(--text-muted);">${escapeHtml(v.initialValue)}</td>
                <td style="font-family:var(--font-mono);font-weight:700;color:var(--success);">${escapeHtml(v.liveValue)}</td>
                <td style="font-family:var(--font-mono);font-size:11px;color:var(--text-muted);">${escapeHtml(v.updatedAt || "—")}</td>
              </tr>
            `
              )
              .join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// --- Right Panel: Live Stream & Topic Message Counts ---
function renderStreamFilterOptions() {
  const sel = document.getElementById("streamTopicFilter");
  const pubSel = document.getElementById("pubTopicSelect");
  const currentFilter = state.streamTopicFilter;
  const currentPub = pubSel.value;

  sel.innerHTML = `<option value="ALL">All Subscribed Topics (*) — ${state.messages.length} msgs</option>`;
  pubSel.innerHTML = "";

  // Put 'admin' first in publish selector since AAU commands go to 'admin'
  const pubAdmin = document.createElement("option");
  pubAdmin.value = "admin";
  pubAdmin.textContent = "admin (AAU Commands)";
  pubSel.appendChild(pubAdmin);

  const adminStats = state.topicStats["admin"] || { count: 0 };
  const adminOpt = document.createElement("option");
  adminOpt.value = "admin";
  adminOpt.textContent = `admin (AAU Control) — ${adminStats.count} msg${adminStats.count === 1 ? "" : "s"}`;
  sel.appendChild(adminOpt);

  for (const n of state.nodes) {
    const st = state.topicStats[n.topic] || { count: 0 };
    const opt = document.createElement("option");
    opt.value = n.topic;
    opt.textContent = `#${n.index} ${n.topic} (${n.name}) — ${st.count} msg${st.count === 1 ? "" : "s"}`;
    sel.appendChild(opt);

    const pubOpt = document.createElement("option");
    pubOpt.value = n.topic;
    pubOpt.textContent = `${n.topic} (#${n.index})`;
    pubSel.appendChild(pubOpt);
  }

  sel.value = currentFilter || "ALL";
  pubSel.value = currentPub || "admin";
}

function renderStreamList() {
  document.getElementById("msgCountBadge").textContent = state.messages.length;
  const topbarBadge = document.getElementById("topbarMsgCountBadge");
  if (topbarBadge) topbarBadge.textContent = state.messages.length;
  if (state.streamPaused) return;

  const list = document.getElementById("mqttStreamList");
  const filter = state.streamTopicFilter;
  const filtered =
    filter === "ALL" ? state.messages : state.messages.filter((m) => m.topic === filter);

  if (filtered.length === 0) {
    list.innerHTML = `
      <div class="empty-state" style="margin-top:20px;">
        <p style="margin-bottom:8px;">Waiting for real-time MQTT messages...</p>
        <p style="font-size:11px;color:var(--text-dim);">Connected topics from <code>AAU/script/working.fn</code> will stream here automatically as packets arrive.</p>
      </div>
    `;
    return;
  }

  list.innerHTML = filtered
    .slice(0, 80)
    .map(
      (m) => `
      <div class="stream-msg ${m.direction === "out" ? "outgoing" : "confirm"}">
        <div class="stream-msg-top">
          <span>
            <strong style="color:var(--primary)">${m.nodeIndex ? "#" + m.nodeIndex : "SYS"}</strong>
            <span class="topic-pill" style="margin-left:4px;">${escapeHtml(m.topic)}</span>
          </span>
          <span style="color:var(--text-dim)">${escapeHtml(m.timestamp)}</span>
        </div>
        <div class="stream-payload">${escapeHtml(m.payload)}</div>
        <div class="stream-decoded">${escapeHtml(m.decoded)}</div>
      </div>
    `
    )
    .join("");
}

function renderHeaderMapTab() {
  const topicBadge = document.getElementById("topicCountBadge");
  if (topicBadge) topicBadge.textContent = state.nodes.length;

  const container = document.getElementById("headerMapList");
  const adminSt = state.topicStats["admin"] || { count: 0, lastTimestamp: "—" };

  const adminCard = state.conn.subscribeAdmin
    ? `
      <div class="diag-item" style="border-left: 3px solid var(--purple);">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <strong>AAU Control Topic — <code>admin</code></strong>
          <span class="msg-count-pill ${adminSt.count > 0 ? "has-msgs" : ""}">${adminSt.count} msg${adminSt.count === 1 ? "" : "s"}</span>
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;font-family:var(--font-mono);font-size:11px;color:var(--text-muted);">
          <span>📡 Topic: <code>admin</code></span>
          <span>⏱ ${escapeHtml(adminSt.lastTimestamp)}</span>
        </div>
      </div>
    `
    : "";

  container.innerHTML =
    adminCard +
    state.nodes
      .map((n, idx) => {
        const st = state.topicStats[n.topic] || { count: 0, lastTimestamp: "—" };
        return `
        <div class="diag-item" style="border-left: 3px solid var(--primary); cursor: pointer;" onclick="selectNodeIndex(${idx})">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:6px;">
            <strong>Node #${escapeHtml(n.index)} — ${escapeHtml(n.name)}</strong>
            <div style="display:flex;align-items:center;gap:6px;">
              <span class="msg-count-pill ${st.count > 0 ? "has-msgs" : ""}">${st.count} msg${st.count === 1 ? "" : "s"}</span>
              <span class="role-badge role-${escapeHtml(n.type)}">${escapeHtml(n.type)}</span>
            </div>
          </div>
          <div style="display:flex;align-items:center;justify-content:space-between;font-family:var(--font-mono);font-size:11px;color:var(--text-muted);">
            <span class="topic-pill">📡 ${escapeHtml(n.topic)}</span>
            <span>⏱ ${escapeHtml(st.lastTimestamp)}</span>
          </div>
        </div>
      `;
      })
      .join("");
}

// --- Connection Modal Helpers ---
function updateConnectionModalUI() {
  document.getElementById("mqttIp").value = state.conn.ip;
  document.getElementById("mqttPort").value = state.conn.port;
  document.getElementById("mqttUsername").value = state.conn.username;
  document.getElementById("mqttPassword").value = state.conn.password;
  document.getElementById("mqttCaCertPath").value = state.conn.caCertPath;
  document.getElementById("mqttBrokerCertPath").value = state.conn.brokerCertPath;
  document.getElementById("chkSubscribeAdmin").checked = state.conn.subscribeAdmin;

  const bridgeLabel = document.getElementById("bridgeDetectStatus");
  if (state.bridgeAvailable) {
    bridgeLabel.textContent = state.conn.configured
      ? "✓ Saved in UI/mqtt-monitor/mqtt.conf (Auto-Connect enabled)"
      : "✓ Bridge ready — enter credentials once to save to mqtt.conf";
    bridgeLabel.style.color = "var(--success)";
  } else {
    bridgeLabel.textContent = "Bridge offline";
    bridgeLabel.style.color = "var(--danger)";
  }

  document.getElementById("modalTopicCount").textContent = state.nodes.length;
  const chipsContainer = document.getElementById("modalTopicsChips");
  chipsContainer.innerHTML = state.nodes
    .map(
      (n) => `
      <span class="modal-topic-chip" title="${escapeHtml(n.headerText)}">
        <span class="chip-idx">#${escapeHtml(n.index)}</span>
        <span>${escapeHtml(n.name)}:</span>
        <span class="chip-topic">${escapeHtml(n.topic)}</span>
      </span>
    `
    )
    .join("");
}

function readConnectionModalInputs() {
  state.conn.ip = document.getElementById("mqttIp").value.trim() || "192.168.0.24";
  state.conn.port = parseInt(document.getElementById("mqttPort").value, 10) || 8883;
  state.conn.username = document.getElementById("mqttUsername").value.trim();
  state.conn.password = document.getElementById("mqttPassword").value;
  state.conn.caCertPath = document.getElementById("mqttCaCertPath").value.trim();
  state.conn.brokerCertPath = document.getElementById("mqttBrokerCertPath").value.trim();
  state.conn.subscribeAdmin = document.getElementById("chkSubscribeAdmin").checked;
}

function openConnectionModal() {
  updateConnectionModalUI();
  document.getElementById("connModal").classList.remove("hidden");
}

window.openConnectionModal = openConnectionModal;
window.connectToBroker = connectToBroker;
window.disconnectBroker = disconnectBroker;
window.publishMessage = publishMessage;
window.selectNodeIndex = (idx) => {
  state.selectedView = idx;
  renderAll();
};
window.selectAllTopicsView = () => {
  state.selectedView = "all";
  renderAll();
};
window.toggleSwitchForce = (nodeIndex, forceVar, nextVal) => {
  publishMessage("admin", `writeVar(${nodeIndex}, ${forceVar}, ${nextVal})`);
};
window.sendElementForce = (nodeIndex, forceVar, inputId) => {
  const el = document.getElementById(inputId);
  if (!el) return;
  const val = el.value.trim();
  if (val === "") return;
  publishMessage("admin", `writeVar(${nodeIndex}, ${forceVar}, ${val})`);
};
window.requestNodeSensors = (nodeIndex) => {
  publishMessage("admin", `readVariables(${nodeIndex})`);
};

// --- Event Listeners ---
function bindEvents() {
  // Optional Live MQTT Stream panel checkbox (25% screen width when checked)
  const chkStream = document.getElementById("chkToggleStream");
  if (chkStream) {
    chkStream.addEventListener("change", () => {
      updateStreamPanelVisibility();
    });
  }

  document.getElementById("btnOpenConnModal").addEventListener("click", openConnectionModal);
  document.getElementById("brokerStatusPill").addEventListener("click", openConnectionModal);

  document.getElementById("btnCloseConnModal").addEventListener("click", () => {
    document.getElementById("connModal").classList.add("hidden");
  });

  document.getElementById("btnCancelConnModal").addEventListener("click", async () => {
    document.getElementById("connModal").classList.add("hidden");
    if (state.conn.status === "connecting" || state.conn.status === "connected") {
      await disconnectBroker();
    }
  });

  document.getElementById("btnConnectBroker").addEventListener("click", () => connectToBroker(false));
  document.getElementById("btnDisconnectBroker").addEventListener("click", disconnectBroker);

  // Automatically close MQTT connection when closing browser tab / session
  const handleSessionClose = () => {
    try {
      navigator.sendBeacon(`${API_BASE}/api/session-close`, "{}");
    } catch (_) {}
  };
  window.addEventListener("pagehide", handleSessionClose);
  window.addEventListener("beforeunload", handleSessionClose);

  // Show/hide password toggle
  document.getElementById("btnTogglePwd").addEventListener("click", (e) => {
    const pwdInput = document.getElementById("mqttPassword");
    if (pwdInput.type === "password") {
      pwdInput.type = "text";
      e.target.textContent = "Hide";
    } else {
      pwdInput.type = "password";
      e.target.textContent = "Show";
    }
  });

  // 1. CA Certificate (ca.crt) file picker
  const caCertFileInput = document.getElementById("mqttCaCertFileInput");
  document.getElementById("btnBrowseCaCert").addEventListener("click", () => {
    caCertFileInput.click();
  });

  caCertFileInput.addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      state.conn.caCertFileName = file.name;
      state.conn.caCertFileContent = String(reader.result || "");
      document.getElementById("caCertFileLoadedName").textContent = `✓ Loaded CA cert: ${file.name} (${file.size} bytes)`;
      document.getElementById("caCertFileLoadedBox").classList.remove("hidden");
      showToast(`CA Certificate "${file.name}" loaded`);
    };
    reader.readAsText(file);
  });

  document.getElementById("btnClearCaCertFile").addEventListener("click", () => {
    state.conn.caCertFileName = "";
    state.conn.caCertFileContent = "";
    caCertFileInput.value = "";
    document.getElementById("caCertFileLoadedBox").classList.add("hidden");
  });

  // 2. Broker Certificate (broker.crt) file picker
  const brokerCertFileInput = document.getElementById("mqttBrokerCertFileInput");
  document.getElementById("btnBrowseBrokerCert").addEventListener("click", () => {
    brokerCertFileInput.click();
  });

  brokerCertFileInput.addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      state.conn.brokerCertFileName = file.name;
      state.conn.brokerCertFileContent = String(reader.result || "");
      document.getElementById("brokerCertFileLoadedName").textContent = `✓ Loaded Broker cert: ${file.name} (${file.size} bytes)`;
      document.getElementById("brokerCertFileLoadedBox").classList.remove("hidden");
      showToast(`Broker Certificate "${file.name}" loaded`);
    };
    reader.readAsText(file);
  });

  document.getElementById("btnClearBrokerCertFile").addEventListener("click", () => {
    state.conn.brokerCertFileName = "";
    state.conn.brokerCertFileContent = "";
    brokerCertFileInput.value = "";
    document.getElementById("brokerCertFileLoadedBox").classList.add("hidden");
  });

  // Reload working.fn
  document.getElementById("btnReloadWorkingFn").addEventListener("click", async () => {
    let text = WORKING_FN_SCRIPT;
    try {
      const r = await fetch(`${API_BASE}/api/working-fn`, { cache: "no-store" });
      if (r.ok) {
        const d = await r.json();
        if (d.content) text = d.content;
      }
    } catch (_) {}
    loadScriptIntoState(text, "AAU/script/working.fn");
    updateConnectionModalUI();
    renderAll();
    showToast(`Reloaded AAU/script/working.fn (${state.nodes.length} nodes)`);
  });

  // Open custom .fn file from disk
  const fnInput = document.getElementById("fnFileInput");
  document.getElementById("btnOpenFnFile").addEventListener("click", () => fnInput.click());
  fnInput.addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      loadScriptIntoState(String(reader.result || ""), file.name);
      state.selectedView = "all";
      updateConnectionModalUI();
      renderAll();
      showToast(`Loaded ${state.nodes.length} node topics from ${file.name}`);
    };
    reader.readAsText(file);
  });

  // Right panel tabs
  document.querySelectorAll("[data-stream-tab]").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll("[data-stream-tab]").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      const tab = btn.getAttribute("data-stream-tab");
      document.getElementById("tabMessagesContent").classList.toggle("active", tab === "messages");
      document.getElementById("tabRawScriptContent").classList.toggle("active", tab === "rawscript");
    });
  });

  // Stream filter, pause, clear
  document.getElementById("streamTopicFilter").addEventListener("change", (e) => {
    state.streamTopicFilter = e.target.value;
    renderStreamList();
  });

  document.getElementById("btnPauseStream").addEventListener("click", (e) => {
    state.streamPaused = !state.streamPaused;
    e.currentTarget.textContent = state.streamPaused ? "▶ Resume" : "⏸ Pause";
    if (!state.streamPaused) renderStreamList();
  });

  document.getElementById("btnClearStream").addEventListener("click", () => {
    state.messages = [];
    renderStreamList();
    renderStreamFilterOptions();
  });

  // Publish bar
  const sendPub = () => {
    const topic = document.getElementById("pubTopicSelect").value;
    const payload = document.getElementById("pubPayloadInput").value.trim();
    if (!payload) return;
    publishMessage(topic, payload);
    document.getElementById("pubPayloadInput").value = "";
  };
  document.getElementById("btnPublishMsg").addEventListener("click", sendPub);
  document.getElementById("pubPayloadInput").addEventListener("keydown", (e) => {
    if (e.key === "Enter") sendPub();
  });
}

document.addEventListener("DOMContentLoaded", initApp);
