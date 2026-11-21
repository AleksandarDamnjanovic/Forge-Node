/**
 * Forge Node Studio — Interactive Application Logic
 */

const STORAGE_KEY = "forge_node_studio_script_v1";
const STORAGE_FILENAME_KEY = "forge_node_studio_filename_v1";

let appState = {
  model: { nodes: [] },
  selectedNodeIdx: 0, // array index in model.nodes
  activeSectionTab: "all", // 'all' | 'variables' | 'sensors' | 'elements' | 'programs'
  fileName: "script.fn",
  defaultScript: "",
  fileHandle: null,
  isDirty: false,
  nodeFilter: "",
  quickVarCallback: null,
  autoAddVarsSensors: true,
  autoAddVarsElements: true,
  autoAddVarsPrograms: true,
};

// DOM References
const elNodeCount = document.getElementById("nodeCount");
const elNodeList = document.getElementById("nodeList");
const elNodeSearchInput = document.getElementById("nodeSearchInput");
const elTopologyCanvas = document.getElementById("topologyCanvas");
const elEditorWorkspace = document.getElementById("editorWorkspace");
const elCodeTextarea = document.getElementById("codeTextarea");
const elLineNumbers = document.getElementById("lineNumbers");
const elCodeStats = document.getElementById("codeStats");
const elDiagnosticsList = document.getElementById("diagnosticsList");
const elDiagCount = document.getElementById("diagCount");
const elValidationBadge = document.getElementById("validationBadge");
const elCurrentFileName = document.getElementById("currentFileName");
const elStatusDot = document.getElementById("statusDot");
const elChkAutoSyncCode = document.getElementById("chkAutoSyncCode");
const elFileInput = document.getElementById("fileInput");

function isFnFileName(name) {
  return typeof name === "string" && name.trim().toLowerCase().endsWith(".fn");
}

function updateDefaultScriptButton() {
  const btn = document.getElementById("btnSetDefaultScript");
  const lbl = document.getElementById("setDefaultScriptBtnLabel");
  if (!btn || !lbl) return;
  const isDefault = Boolean(appState.defaultScript && appState.fileName === appState.defaultScript && !appState.isDirty);
  lbl.textContent = isDefault ? "Default Script" : "Set Default";
  btn.title = appState.defaultScript
    ? `Default script: ${appState.defaultScript} (AAU/script/default-script.conf)`
    : "Set the active .fn script as default in AAU/script/default-script.conf";
}

// Initialize Studio
async function initApp() {
  const savedScript = localStorage.getItem(STORAGE_KEY);
  const savedFileName = localStorage.getItem(STORAGE_FILENAME_KEY);

  let initialScript = savedScript && savedScript.trim().length > 0 ? savedScript : SAMPLE_FN_SCRIPT;
  let initialFileName = isFnFileName(savedFileName) ? savedFileName.trim() : "script.fn";

  try {
    const res = await fetch("/api/working-fn", { cache: "no-store" });
    if (res.ok) {
      const data = await res.json();
      if (data.defaultScript) {
        appState.defaultScript = String(data.defaultScript).trim();
        if (data.content && data.content.trim().length > 0) {
          initialScript = data.content;
          initialFileName = appState.defaultScript;
        }
      }
    }
  } catch (_) {}

  appState.fileName = initialFileName;
  appState.model = parseFnScript(initialScript);
  appState.selectedNodeIdx = 0;
  appState.isDirty = false;

  bindTopbarEvents();
  bindCodePanelEvents();
  bindQuickVarModalEvents();
  renderAll(true);
}

function showToast(msg) {
  const container = document.getElementById("toastContainer");
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => {
    toast.remove();
  }, 2600);
}

function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// Render Everything
function renderAll(updateCodeFromModel = true) {
  if (appState.selectedNodeIdx >= appState.model.nodes.length) {
    appState.selectedNodeIdx = Math.max(0, appState.model.nodes.length - 1);
  }

  elCurrentFileName.textContent = appState.fileName;
  elStatusDot.className = "status-dot" + (appState.isDirty ? " dirty" : "");
  updateDefaultScriptButton();

  renderSidebarNodes();
  renderTopology();
  renderWorkspace();

  if (updateCodeFromModel) {
    const code = serializeFnScript(appState.model);
    elCodeTextarea.value = code;
    updateCodeLineNumbers();
    localStorage.setItem(STORAGE_KEY, code);
    localStorage.setItem(STORAGE_FILENAME_KEY, appState.fileName);
  }

  renderDiagnostics();
}

function markDirtyAndSync() {
  appState.isDirty = true;
  renderAll(true);
}

// Sidebar Node List
function renderSidebarNodes() {
  const nodes = appState.model.nodes;
  elNodeCount.textContent = nodes.length;
  elNodeList.innerHTML = "";

  const q = appState.nodeFilter.trim().toLowerCase();

  nodes.forEach((node, idx) => {
    const matchText = `${node.index} ${node.name} ${node.topic} ${node.type}`.toLowerCase();
    if (q && !matchText.includes(q)) return;

    const item = document.createElement("div");
    item.className = "node-item" + (idx === appState.selectedNodeIdx ? " active" : "");
    item.innerHTML = `
      <div class="node-item-top">
        <div class="node-id-name">
          <span class="node-idx-badge">#${escapeHtml(node.index)}</span>
          <span>${escapeHtml(node.name)}</span>
        </div>
        <span class="role-badge role-${escapeHtml(node.type)}">${escapeHtml(node.type)}</span>
      </div>
      <div class="node-item-meta">
        <span>topic: "${escapeHtml(node.topic)}"</span>
        <span>${node.variables.length}v · ${node.sensors.length}s · ${node.elements.length}e · ${node.programs.length}p</span>
      </div>
    `;
    item.addEventListener("click", () => {
      appState.selectedNodeIdx = idx;
      renderAll(false);
    });
    elNodeList.appendChild(item);
  });
}

// Network Topology Mini-Panel
function renderTopology() {
  const nodes = appState.model.nodes;
  if (nodes.length === 0) {
    elTopologyCanvas.innerHTML = `<div class="empty-state">No nodes defined</div>`;
    return;
  }

  elTopologyCanvas.innerHTML = "";
  for (const n of nodes) {
    const row = document.createElement("div");
    row.className = "topo-row";
    const masterPart =
      n.meta.master !== null && n.meta.master !== "" ? `↑ master: #${n.meta.master}` : "";
    const slavesPart =
      n.meta.slaves && n.meta.slaves.length > 0
        ? `↓ slaves: [${n.meta.slaves.map((s) => "#" + s).join(", ")}]`
        : "";
    const links = [masterPart, slavesPart].filter(Boolean).join("  |  ") || "Standalone / Direct AAU";

    row.innerHTML = `
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <span><strong>#${escapeHtml(n.index)}</strong> ${escapeHtml(n.name)} <code style="color:var(--text-muted)">(${escapeHtml(n.topic)})</code></span>
        <span class="role-badge role-${escapeHtml(n.type)}">${escapeHtml(n.type)}</span>
      </div>
      <div class="topo-links">${escapeHtml(links)}</div>
    `;
    elTopologyCanvas.appendChild(row);
  }
}

// Center Workspace: Active Node Editor
function renderWorkspace() {
  const node = appState.model.nodes[appState.selectedNodeIdx];
  if (!node) {
    elEditorWorkspace.innerHTML = `
      <div class="empty-state" style="margin-top:40px;">
        <p style="font-size:14px;margin-bottom:10px;">No node selected or script is empty.</p>
        <button class="btn btn-primary" onclick="addNewNode()">＋ Create First Node</button>
      </div>
    `;
    return;
  }

  // Build datalist of variables for autocomplete in inputs
  const localVarNames = node.variables.map((v) => v.name);
  const crossNodeGlobalVars = [];
  for (const other of appState.model.nodes) {
    if (other.index !== node.index) {
      for (const v of other.variables) {
        if (v.isGlobal) {
          crossNodeGlobalVars.push(`$${other.index}${v.name}`);
        }
      }
    }
  }
  const allAutocompleteVars = [...localVarNames, ...crossNodeGlobalVars];

  const datalistHtml = `
    <datalist id="nodeVarsList">
      ${allAutocompleteVars.map((v) => `<option value="${escapeHtml(v)}"></option>`).join("")}
    </datalist>
    <datalist id="nodeVarsWithNullList">
      <option value="NULL"></option>
      ${allAutocompleteVars.map((v) => `<option value="${escapeHtml(v)}"></option>`).join("")}
    </datalist>
    <datalist id="nodeFunVarsList">
      ${node.variables
        .filter((v) => v.type === "fun")
        .map((v) => `<option value="${escapeHtml(v.name)}"></option>`)
        .join("")}
    </datalist>
  `;

  const showSection = (sec) => appState.activeSectionTab === "all" || appState.activeSectionTab === sec;

  elEditorWorkspace.innerHTML = `
    ${datalistHtml}

    <!-- Node Toolbar -->
    <div class="node-toolbar">
      <div class="section-tabs">
        <button class="section-tab ${appState.activeSectionTab === "all" ? "active" : ""}" data-sec="all">All Sections</button>
        <button class="section-tab ${appState.activeSectionTab === "variables" ? "active" : ""}" data-sec="variables">Variables (${node.variables.length})</button>
        <button class="section-tab ${appState.activeSectionTab === "sensors" ? "active" : ""}" data-sec="sensors">Sensors (${node.sensors.length})</button>
        <button class="section-tab ${appState.activeSectionTab === "elements" ? "active" : ""}" data-sec="elements">Elements (${node.elements.length})</button>
        <button class="section-tab ${appState.activeSectionTab === "programs" ? "active" : ""}" data-sec="programs">Programs (${node.programs.length})</button>
      </div>

      <div style="display:flex;gap:8px;">
        <button class="btn btn-sm btn-secondary" id="btnQuickAddVarModal">＋ Quick Variable</button>
        <button class="btn btn-sm btn-secondary" id="btnDuplicateNode">⧉ Duplicate Node</button>
        <button class="btn btn-sm btn-danger" id="btnDeleteNode">🗑 Delete Node</button>
      </div>
    </div>

    <!-- Header & Meta Card -->
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">HEADER &amp; &lt;meta&gt;</span>
          <span class="card-title">Node #${escapeHtml(node.index)} — ${escapeHtml(node.name)}</span>
        </div>
        <span class="role-badge role-${escapeHtml(node.type)}">${escapeHtml(node.type)}</span>
      </div>
      <div class="card-body">
        <div class="header-grid">
          <div class="field-group">
            <label class="field-label">Node Index</label>
            <input type="number" min="1" class="input input-mono" id="inpNodeIndex" value="${escapeHtml(node.index)}" />
          </div>
          <div class="field-group">
            <label class="field-label">Human Name</label>
            <input type="text" class="input" id="inpNodeName" value="${escapeHtml(node.name)}" placeholder="e.g. First floor" />
          </div>
          <div class="field-group">
            <label class="field-label">Machine Topic (no spaces)</label>
            <input type="text" class="input input-mono" id="inpNodeTopic" value="${escapeHtml(node.topic)}" placeholder="e.g. 1f" />
          </div>
          <div class="field-group">
            <label class="field-label">Node Role</label>
            <select class="select input-mono" id="selNodeType">
              <option value="MASTER" ${node.type === "MASTER" ? "selected" : ""}>MASTER</option>
              <option value="SHUNT" ${node.type === "SHUNT" ? "selected" : ""}>SHUNT</option>
              <option value="BRIDGE" ${node.type === "BRIDGE" ? "selected" : ""}>BRIDGE</option>
              <option value="SLAVE" ${node.type === "SLAVE" ? "selected" : ""}>SLAVE</option>
            </select>
          </div>
        </div>

        <div class="meta-grid">
          <div class="field-group">
            <label class="field-label">Master Node ID <span>(optional)</span></label>
            <input type="number" min="1" class="input input-mono input-sm" id="inpMetaMaster" value="${node.meta.master !== null ? escapeHtml(node.meta.master) : ""}" placeholder="None" />
          </div>
          <div class="field-group">
            <label class="field-label">
              <span>Slaves Array <code>[id, id, ...]</code></span>
              <span style="font-weight:400;color:var(--text-dim)">Click chips to toggle or edit comma list</span>
            </label>
            <input type="text" class="input input-mono input-sm" id="inpMetaSlaves" value="${escapeHtml((node.meta.slaves || []).join(", "))}" placeholder="e.g. 2, 3, 4, 5" />
            <div class="slave-chips" id="slaveChipsContainer">
              ${appState.model.nodes
                .filter((other) => other.index !== node.index)
                .map((other) => {
                  const isSel = (node.meta.slaves || []).includes(Number(other.index));
                  return `<span class="slave-chip ${isSel ? "selected" : ""}" data-slave-id="${other.index}">#${other.index} (${escapeHtml(other.name)})</span>`;
                })
                .join("")}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- VARIABLES SECTION -->
    ${showSection("variables") ? renderVariablesCardHtml(node) : ""}

    <!-- SENSORS SECTION -->
    ${showSection("sensors") ? renderSensorsCardHtml(node) : ""}

    <!-- ELEMENTS SECTION -->
    ${showSection("elements") ? renderElementsCardHtml(node) : ""}

    <!-- PROGRAMS SECTION -->
    ${showSection("programs") ? renderProgramsCardHtml(node) : ""}
  `;

  bindWorkspaceEvents(node);
}

function buildScriptVarOptionsHtml(node, excludeVarName = null) {
  const localVarOpts = node.variables
    .filter((v) => v.name !== excludeVarName)
    .map(
      (v) =>
        `<option value="${escapeHtml(v.name)}">${escapeHtml(v.name)} (Local Node #${escapeHtml(node.index)}, ${v.isGlobal ? "global " : ""}${escapeHtml(v.type)})</option>`
    )
    .join("");

  const remoteGlobalOpts = appState.model.nodes
    .filter((other) => other.index !== node.index)
    .flatMap((other) =>
      other.variables
        .filter((v) => v.isGlobal)
        .map((v) => {
          const ref = `$${other.index}${v.name}`;
          return `<option value="${escapeHtml(ref)}">${escapeHtml(ref)} — Node #${escapeHtml(other.index)} (${escapeHtml(other.name)}, global ${escapeHtml(v.type)})</option>`;
        })
    )
    .join("");

  return `
    <optgroup label="Node #${escapeHtml(node.index)} Variables (Local &amp; Global)">
      ${localVarOpts || `<option value="$c1">$c1</option>`}
    </optgroup>
    ${
      remoteGlobalOpts
        ? `<optgroup label="Global Variables From Other Nodes">${remoteGlobalOpts}</optgroup>`
        : ""
    }
  `;
}

function parseArrayTokens(rawVal) {
  const str = String(rawVal ?? "").trim();
  const inner = str.replace(/^\[|\]$/g, "").trim();
  if (!inner) return [];
  return inner
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

function appendTokenToArrayString(rawVal, token) {
  const items = parseArrayTokens(rawVal);
  items.push(token);
  return `[${items.join(", ")}]`;
}

function renderVariablesCardHtml(node) {
  const rowsHtml = node.variables
    .map((v, i) => {
      let valueCellHtml = `
        <input type="text" class="input input-sm input-mono var-val" data-idx="${i}" value="${escapeHtml(v.value)}" placeholder="value" />
      `;

      if (v.type === "array") {
        const items = parseArrayTokens(v.value);
        const chipsHtml = items
          .map(
            (item, itemIdx) => `
            <span class="array-chip">
              <span>${escapeHtml(item)}</span>
              <span class="array-chip-del" data-vidx="${i}" data-tidx="${itemIdx}" title="Remove from array">✕</span>
            </span>
          `
          )
          .join("");

        valueCellHtml = `
          <div class="array-val-cell">
            <input type="text" class="input input-sm input-mono var-val" data-idx="${i}" value="${escapeHtml(v.value)}" placeholder="[$c1, $3$c1] or [0, 1, 2]" />
            <div class="array-picker-bar">
              <select class="select select-sm input-mono var-arr-picker" data-idx="${i}" style="width:auto;min-width:220px;flex:1;">
                ${buildScriptVarOptionsHtml(node, v.name)}
              </select>
              <button type="button" class="btn btn-xs btn-primary var-arr-add" data-idx="${i}">＋ Add to Array</button>
            </div>
            ${chipsHtml ? `<div class="array-chips-wrap">${chipsHtml}</div>` : ""}
          </div>
        `;
      }

      return `
      <tr>
        <td style="width:74px;text-align:center;">
          <input type="checkbox" class="var-global" data-idx="${i}" ${v.isGlobal && v.type !== "fun" ? "checked" : ""} ${v.type === "fun" ? "disabled title='fun variables cannot be global'" : ""} />
        </td>
        <td style="width:120px;">
          <select class="select select-sm input-mono var-type type-select-${escapeHtml(v.type)}" data-idx="${i}">
            <option value="int" ${v.type === "int" ? "selected" : ""}>int</option>
            <option value="float" ${v.type === "float" ? "selected" : ""}>float</option>
            <option value="string" ${v.type === "string" ? "selected" : ""}>string</option>
            <option value="array" ${v.type === "array" ? "selected" : ""}>array</option>
            <option value="fun" ${v.type === "fun" ? "selected" : ""}>fun</option>
          </select>
        </td>
        <td style="width:180px;">
          <input type="text" class="input input-sm input-mono var-name" data-idx="${i}" value="${escapeHtml(v.name)}" placeholder="$varName" />
        </td>
        <td>
          ${valueCellHtml}
        </td>
        <td style="width:42px;text-align:right;">
          <button class="btn-icon danger var-del" data-idx="${i}" title="Remove variable">✕</button>
        </td>
      </tr>
    `;
    })
    .join("");

  return `
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">&lt;variables&gt;</span>
          <span class="card-title">Node Variables (${node.variables.length})</span>
        </div>
        <div style="display:flex;gap:6px;flex-wrap:wrap;">
          <button class="btn btn-xs btn-secondary" id="btnAddVarPresetSensor" title="Add $e1 and $res01 preset">+ Sensor Vars</button>
          <button class="btn btn-xs btn-secondary" id="btnAddVarPresetSwitch" title="Add $c and $f switch variables">+ Switch Vars</button>
          <button class="btn btn-xs btn-secondary" id="btnAddArrayVariable" title="Create a new array variable with variable dropdown picker">＋ Array Variable</button>
          <button class="btn btn-xs btn-primary" id="btnAddVariable">＋ Add Variable</button>
        </div>
      </div>
      <div class="card-body">
        ${
          node.variables.length === 0
            ? `<div class="empty-state">No variables defined in this node. Click "＋ Add Variable" to add one.</div>`
            : `
          <table class="items-table">
            <thead>
              <tr>
                <th style="text-align:center;">Global</th>
                <th>Type</th>
                <th>Name (<code>$</code>)</th>
                <th>Initial Value / Array Elements</th>
                <th></th>
              </tr>
            </thead>
            <tbody>${rowsHtml}</tbody>
          </table>
        `
        }
      </div>
    </div>
  `;
}

function renderSensorsCardHtml(node) {
  const rowsHtml = node.sensors
    .map(
      (s, i) => `
      <tr>
        <td>
          <input type="text" class="input input-sm sen-name" data-idx="${i}" value="${escapeHtml(s.name)}" placeholder="temp" />
        </td>
        <td style="width:95px;">
          <input type="number" min="0" class="input input-sm input-mono sen-index" data-idx="${i}" value="${escapeHtml(s.index)}" />
        </td>
        <td>
          <input type="text" list="nodeVarsWithNullList" class="input input-sm input-mono sen-epoch" data-idx="${i}" value="${escapeHtml(s.epoch)}" placeholder="$e1 or NULL" />
        </td>
        <td>
          <input type="text" list="nodeVarsList" class="input input-sm input-mono sen-readings" data-idx="${i}" value="${escapeHtml(s.readings)}" placeholder="$res01" />
        </td>
        <td style="width:42px;text-align:right;">
          <button class="btn-icon danger sen-del" data-idx="${i}" title="Delete sensor">✕</button>
        </td>
      </tr>
    `
    )
    .join("");

  return `
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">&lt;sensors&gt;</span>
          <span class="card-title">Sensors (${node.sensors.length})</span>
          <span class="card-subtitle">sensor("name", hwIndex, epochVar|NULL, readingsVar)</span>
        </div>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
          <label class="checkbox-label" style="font-size:11.5px;font-weight:600;color:var(--text-muted);" title="Automatically create missing variables in <variables> when adding or editing sensors">
            <input type="checkbox" id="chkAutoVarsSensors" ${appState.autoAddVarsSensors ? "checked" : ""} />
            <span>Add necessary variables</span>
          </label>
          <button class="btn btn-xs btn-primary" id="btnAddSensor">＋ Add Sensor</button>
        </div>
      </div>
      <div class="card-body">
        ${
          node.sensors.length === 0
            ? `<div class="empty-state">No sensors in this node.</div>`
            : `
          <table class="items-table">
            <thead>
              <tr>
                <th>Sensor Name</th>
                <th>HW Index</th>
                <th>Epoch Variable (or <code>NULL</code>)</th>
                <th>Readings Variable</th>
                <th></th>
              </tr>
            </thead>
            <tbody>${rowsHtml}</tbody>
          </table>
        `
        }
      </div>
    </div>
  `;
}

function renderElementsCardHtml(node) {
  const itemsHtml = node.elements
    .map((el, i) => {
      if (el.elType === "switch" || el.elType === "trigger") {
        return `
          <div class="subcard">
            <div class="subcard-top">
              <span class="subcard-badge badge-${escapeHtml(el.elType)}">${escapeHtml(el.elType)}(...)</span>
              <button class="btn-icon danger el-del" data-idx="${i}" title="Delete element">✕</button>
            </div>
            <div class="grid-4">
              <div class="field-group">
                <label class="field-label">Element Name</label>
                <input type="text" class="input input-sm el-name" data-idx="${i}" value="${escapeHtml(el.name)}" />
              </div>
              <div class="field-group">
                <label class="field-label">HW Index</label>
                <input type="number" min="0" class="input input-sm input-mono el-index" data-idx="${i}" value="${escapeHtml(el.index)}" />
              </div>
              <div class="field-group">
                <label class="field-label">Programmable Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono el-value" data-idx="${i}" value="${escapeHtml(el.value)}" placeholder="$c1" />
              </div>
              <div class="field-group">
                <label class="field-label">Force Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono el-force" data-idx="${i}" value="${escapeHtml(el.force)}" placeholder="$f1" />
              </div>
            </div>
          </div>
        `;
      } else {
        return `
          <div class="subcard">
            <div class="subcard-top">
              <span class="subcard-badge badge-list">list(...)</span>
              <button class="btn-icon danger el-del" data-idx="${i}" title="Delete element">✕</button>
            </div>
            <div class="grid-6">
              <div class="field-group">
                <label class="field-label">List Name</label>
                <input type="text" class="input input-sm el-name" data-idx="${i}" value="${escapeHtml(el.name)}" />
              </div>
              <div class="field-group">
                <label class="field-label">HW Index</label>
                <input type="number" min="0" class="input input-sm input-mono el-index" data-idx="${i}" value="${escapeHtml(el.index)}" />
              </div>
              <div class="field-group">
                <label class="field-label">Values Array Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono el-listValues" data-idx="${i}" value="${escapeHtml(el.listValues)}" placeholder="$lv" />
              </div>
              <div class="field-group">
                <label class="field-label">Names Array Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono el-listNames" data-idx="${i}" value="${escapeHtml(el.listNames)}" placeholder="$ln" />
              </div>
              <div class="field-group">
                <label class="field-label">State Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono el-state" data-idx="${i}" value="${escapeHtml(el.state)}" placeholder="$l1" />
              </div>
              <div class="field-group">
                <label class="field-label">Force Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono el-force" data-idx="${i}" value="${escapeHtml(el.force)}" placeholder="$lf1" />
              </div>
            </div>
          </div>
        `;
      }
    })
    .join("");

  return `
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">&lt;elements&gt;</span>
          <span class="card-title">Elements (${node.elements.length})</span>
          <span class="card-subtitle">Switches, Triggers &amp; Mode Lists</span>
        </div>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
          <label class="checkbox-label" style="font-size:11.5px;font-weight:600;color:var(--text-muted);" title="Automatically create missing variables in <variables> when adding or editing elements">
            <input type="checkbox" id="chkAutoVarsElements" ${appState.autoAddVarsElements ? "checked" : ""} />
            <span>Add necessary variables</span>
          </label>
          <div style="display:flex;gap:6px;">
            <button class="btn btn-xs btn-primary" id="btnAddSwitch">＋ Switch</button>
            <button class="btn btn-xs btn-primary" id="btnAddTrigger">＋ Trigger</button>
            <button class="btn btn-xs btn-primary" id="btnAddList">＋ List</button>
          </div>
        </div>
      </div>
      <div class="card-body">
        ${
          node.elements.length === 0
            ? `<div class="empty-state">No elements in this node. Add a Switch, Trigger, or List above.</div>`
            : `<div class="subcard-list">${itemsHtml}</div>`
        }
      </div>
    </div>
  `;
}

function renderProgramsCardHtml(node) {
  const itemsHtml = node.programs
    .map((p, i) => {
      if (p.progType === "valCondition") {
        const condRows = (p.conditions || [])
          .map(
            (c, ci) => `
            <div class="stmt-row">
              <input type="text" list="nodeVarsList" class="input input-sm input-mono vc-cond-left" data-pidx="${i}" data-cidx="${ci}" value="${escapeHtml(c.left)}" placeholder="$var" />
              <select class="select select-sm input-mono vc-cond-op" data-pidx="${i}" data-cidx="${ci}">
                <option value="==" ${c.op === "==" ? "selected" : ""}>==</option>
                <option value="<" ${c.op === "<" ? "selected" : ""}>&lt;</option>
                <option value=">" ${c.op === ">" ? "selected" : ""}>&gt;</option>
                <option value="<=" ${c.op === "<=" ? "selected" : ""}>&lt;=</option>
                <option value=">=" ${c.op === ">=" ? "selected" : ""}>&gt;=</option>
              </select>
              <input type="text" list="nodeVarsList" class="input input-sm input-mono vc-cond-right" data-pidx="${i}" data-cidx="${ci}" value="${escapeHtml(c.right)}" placeholder="val or $var" />
              <button class="btn-icon danger vc-cond-del" data-pidx="${i}" data-cidx="${ci}" title="Remove condition" ${(p.conditions || []).length <= 1 ? "disabled" : ""}>✕</button>
            </div>
          `
          )
          .join("");

        const resRows = (p.results || [])
          .map(
            (r, ri) => `
            <div class="stmt-row">
              <input type="text" list="nodeVarsList" class="input input-sm input-mono vc-res-left" data-pidx="${i}" data-ridx="${ri}" value="${escapeHtml(r.left)}" placeholder="$var" />
              <select class="select select-sm input-mono vc-res-op" data-pidx="${i}" data-ridx="${ri}">
                <option value="=" ${r.op === "=" ? "selected" : ""}>=</option>
                <option value="<" ${r.op === "<" ? "selected" : ""}>&lt;</option>
                <option value=">" ${r.op === ">" ? "selected" : ""}>&gt;</option>
              </select>
              <input type="text" list="nodeVarsList" class="input input-sm input-mono vc-res-right" data-pidx="${i}" data-ridx="${ri}" value="${escapeHtml(r.right)}" placeholder="val or $var" />
              <button class="btn-icon danger vc-res-del" data-pidx="${i}" data-ridx="${ri}" title="Remove result" ${(p.results || []).length <= 1 ? "disabled" : ""}>✕</button>
            </div>
          `
          )
          .join("");

        return `
          <div class="subcard">
            <div class="subcard-top">
              <div style="display:flex;align-items:center;gap:10px;flex:1;">
                <span class="subcard-badge badge-valCondition">valCondition</span>
                <input type="text" class="input input-sm prog-vc-name" data-idx="${i}" style="max-width:200px;" value="${escapeHtml(p.name)}" placeholder="Condition Name" />
                <span style="font-size:11px;color:var(--text-muted)">Assign to fun (optional):</span>
                <input type="text" list="nodeFunVarsList" class="input input-sm input-mono prog-vc-fun" data-idx="${i}" style="max-width:140px;" value="${escapeHtml(p.funName || "")}" placeholder="$funVar" />
              </div>
              <button class="btn-icon danger prog-del" data-idx="${i}" title="Delete program">✕</button>
            </div>
            <div class="cond-res-split">
              <div class="stmt-box">
                <div class="stmt-box-header">
                  <span>IF ALL CONDITIONS TRUE</span>
                  <button class="btn btn-xs btn-secondary vc-add-cond" data-pidx="${i}">＋ Condition</button>
                </div>
                ${condRows}
              </div>
              <div class="stmt-box">
                <div class="stmt-box-header">
                  <span>THEN EXECUTE RESULTS</span>
                  <button class="btn btn-xs btn-secondary vc-add-res" data-pidx="${i}">＋ Result</button>
                </div>
                ${resRows}
              </div>
            </div>
          </div>
        `;
      } else if (p.progType === "serial") {
        return `
          <div class="subcard">
            <div class="subcard-top">
              <div style="display:flex;align-items:center;gap:10px;">
                <span class="subcard-badge badge-serial">serial</span>
                <span style="font-size:11px;color:var(--text-muted)">Assign to fun (optional):</span>
                <input type="text" list="nodeFunVarsList" class="input input-sm input-mono prog-ser-fun" data-idx="${i}" style="max-width:140px;" value="${escapeHtml(p.funName || "")}" placeholder="$funVar" />
              </div>
              <button class="btn-icon danger prog-del" data-idx="${i}" title="Delete program">✕</button>
            </div>
            <div class="grid-5">
              <div class="field-group">
                <label class="field-label">Program Name</label>
                <input type="text" class="input input-sm prog-ser-name" data-idx="${i}" value="${escapeHtml(p.name)}" placeholder="entire row" />
              </div>
              <div class="field-group">
                <label class="field-label">Switches Array Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono prog-ser-switches" data-idx="${i}" value="${escapeHtml(p.switches)}" placeholder="$listOfSwitches" />
              </div>
              <div class="field-group">
                <label class="field-label">Epoch Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono prog-ser-epoch" data-idx="${i}" value="${escapeHtml(p.epoch)}" placeholder="$switchEp" />
              </div>
              <div class="field-group">
                <label class="field-label">Timestamp Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono prog-ser-time" data-idx="${i}" value="${escapeHtml(p.time)}" placeholder="$time" />
              </div>
              <div class="field-group">
                <label class="field-label">Last Index Var</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono prog-ser-last" data-idx="${i}" value="${escapeHtml(p.lastIndex)}" placeholder="$lastIndex" />
              </div>
            </div>
          </div>
        `;
      } else {
        const localVarOpts = node.variables
          .filter((v) => v.type !== "fun")
          .map(
            (v) =>
              `<option value="${escapeHtml(v.name)}">${escapeHtml(v.name)} (${v.isGlobal ? "global " : "local "}${escapeHtml(v.type)})</option>`
          )
          .join("");

        const remoteGlobalOpts = appState.model.nodes
          .filter((other) => other.index !== node.index)
          .flatMap((other) =>
            other.variables
              .filter((v) => v.isGlobal)
              .map((v) => {
                const ref = `$${other.index}${v.name}`;
                return `<option value="${escapeHtml(ref)}">${escapeHtml(ref)} — Node #${escapeHtml(other.index)} (${escapeHtml(other.name)}, global ${escapeHtml(v.type)})</option>`;
              })
          )
          .join("");

        const targets = p.targets || [];
        const cleanFun = (p.functionVar || "$lightsON").replace(/^\$/, "") || "lightsON";
        const onProgName = p.onProgName || `${cleanFun} on`;
        const offProgName = p.offProgName || `${cleanFun} off`;

        const targetRowsHtml = targets
          .map(
            (t, ti) => `
            <div class="timelim-target-row">
              <input type="text" list="nodeVarsList" class="input input-sm input-mono tl-target-var" data-pidx="${i}" data-tidx="${ti}" value="${escapeHtml(t.variable)}" placeholder="$c1 or $2$c1" />
              <input type="text" list="nodeVarsList" class="input input-sm input-mono tl-target-on" data-pidx="${i}" data-tidx="${ti}" value="${escapeHtml(t.onValue ?? "1")}" placeholder="1" />
              <input type="text" list="nodeVarsList" class="input input-sm input-mono tl-target-off" data-pidx="${i}" data-tidx="${ti}" value="${escapeHtml(t.offValue ?? "0")}" placeholder="0" />
              <button class="btn-icon danger tl-target-del" data-pidx="${i}" data-tidx="${ti}" title="Remove variable from timeLim">✕</button>
            </div>
          `
          )
          .join("");

        return `
          <div class="subcard">
            <div class="subcard-top">
              <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
                <span class="subcard-badge badge-timeLim">timeLim</span>
                <span style="font-size:11px;color:var(--text-muted)">Controls <code>${escapeHtml(p.functionVar || "$fun")}</code> (1 between start &amp; end, 0 otherwise) + auto-syncs ON/OFF <code>valCondition</code>s</span>
              </div>
              <button class="btn-icon danger prog-del" data-idx="${i}" title="Delete program">✕</button>
            </div>
            <div class="grid-5">
              <div class="field-group">
                <label class="field-label">Target Function Var (<code>fun</code>)</label>
                <input type="text" list="nodeFunVarsList" class="input input-sm input-mono prog-tl-fun" data-idx="${i}" value="${escapeHtml(p.functionVar)}" placeholder="$lightsON" />
              </div>
              <div class="field-group">
                <label class="field-label">Start Time Var (<code>string</code>)</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono prog-tl-start" data-idx="${i}" value="${escapeHtml(p.startVar)}" placeholder="$start" />
              </div>
              <div class="field-group">
                <label class="field-label">End Time Var (<code>string</code>)</label>
                <input type="text" list="nodeVarsList" class="input input-sm input-mono prog-tl-end" data-idx="${i}" value="${escapeHtml(p.endVar)}" placeholder="$end" />
              </div>
              <div class="field-group">
                <label class="field-label">ON valCondition Name (<code>== 1</code>)</label>
                <input type="text" class="input input-sm prog-tl-onname" data-idx="${i}" value="${escapeHtml(onProgName)}" placeholder="light on" />
              </div>
              <div class="field-group">
                <label class="field-label">OFF valCondition Name (<code>== 0</code>)</label>
                <input type="text" class="input input-sm prog-tl-offname" data-idx="${i}" value="${escapeHtml(offProgName)}" placeholder="light off" />
              </div>
            </div>

            <div class="stmt-box">
              <div class="timelim-picker-bar">
                <span style="font-size:11px;font-weight:700;color:var(--text-muted);margin-right:auto;">
                  AFFECTED VARIABLES (SYNCS 2 <code>valCondition</code> PROGRAMS FOR <code>${escapeHtml(p.functionVar || "$fun")} == 1</code> &amp; <code>== 0</code>)
                </span>
                <select class="select select-sm input-mono tl-var-picker" data-pidx="${i}" style="width:auto;min-width:220px;">
                  <optgroup label="Node #${escapeHtml(node.index)} Variables (Local &amp; Global)">
                    ${localVarOpts || `<option value="$c1">$c1</option>`}
                  </optgroup>
                  ${
                    remoteGlobalOpts
                      ? `<optgroup label="Global Variables From Other Nodes">${remoteGlobalOpts}</optgroup>`
                      : ""
                  }
                </select>
                <button class="btn btn-xs btn-primary tl-add-picked-var" data-pidx="${i}">＋ Add Variable</button>
              </div>

              ${
                targets.length === 0
                  ? `<div class="empty-state" style="padding:10px;">No controlled variables added yet. Select a local or global variable above and click "＋ Add Variable" to generate the ON/OFF <code>valCondition</code> programs.</div>`
                  : `
                <div class="timelim-target-header">
                  <span>Variable (Local or <code>$node$var</code>)</span>
                  <span>Value when <code>${escapeHtml(p.functionVar || "$fun")} == 1</code> (ON)</span>
                  <span>Value when <code>${escapeHtml(p.functionVar || "$fun")} == 0</code> (OFF)</span>
                  <span></span>
                </div>
                ${targetRowsHtml}
              `
              }
            </div>
          </div>
        `;
      }
    })
    .join("");

  return `
    <div class="card">
      <div class="card-header">
        <div class="card-title-group">
          <span class="card-tag">&lt;programs&gt;</span>
          <span class="card-title">Automation Programs (${node.programs.length})</span>
          <span class="card-subtitle">valCondition, serial &amp; timeLim</span>
        </div>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
          <label class="checkbox-label" style="font-size:11.5px;font-weight:600;color:var(--text-muted);" title="Automatically create missing variables in <variables> when adding or editing programs">
            <input type="checkbox" id="chkAutoVarsPrograms" ${appState.autoAddVarsPrograms ? "checked" : ""} />
            <span>Add necessary variables</span>
          </label>
          <div style="display:flex;gap:6px;">
            <button class="btn btn-xs btn-primary" id="btnAddValCondition">＋ valCondition</button>
            <button class="btn btn-xs btn-primary" id="btnAddSerial">＋ serial</button>
            <button class="btn btn-xs btn-primary" id="btnAddTimeLim">＋ timeLim</button>
          </div>
        </div>
      </div>
      <div class="card-body">
        ${
          node.programs.length === 0
            ? `<div class="empty-state">No automation programs in this node.</div>`
            : `<div class="subcard-list">${itemsHtml}</div>`
        }
      </div>
    </div>
  `;
}

// Bind Events Inside Workspace
function bindWorkspaceEvents(node) {
  // Section tabs
  document.querySelectorAll(".section-tab").forEach((btn) => {
    btn.addEventListener("click", () => {
      appState.activeSectionTab = btn.dataset.sec;
      renderWorkspace();
    });
  });

  // Quick Variable Modal Button
  const btnQuickVar = document.getElementById("btnQuickAddVarModal");
  if (btnQuickVar) {
    btnQuickVar.addEventListener("click", () => openQuickVarModal());
  }

  // Duplicate & Delete Node
  const btnDup = document.getElementById("btnDuplicateNode");
  if (btnDup) {
    btnDup.addEventListener("click", () => {
      const clone = JSON.parse(JSON.stringify(node));
      const maxId = appState.model.nodes.reduce((m, n) => Math.max(m, Number(n.index) || 0), 0);
      clone.index = maxId + 1;
      clone.name = `${node.name} Copy`;
      clone.topic = `${clone.index}f`;
      appState.model.nodes.push(clone);
      appState.selectedNodeIdx = appState.model.nodes.length - 1;
      markDirtyAndSync();
      showToast(`Duplicated as Node #${clone.index}`);
    });
  }

  const btnDel = document.getElementById("btnDeleteNode");
  if (btnDel) {
    btnDel.addEventListener("click", () => {
      appState.model.nodes.splice(appState.selectedNodeIdx, 1);
      appState.selectedNodeIdx = Math.max(0, appState.selectedNodeIdx - 1);
      markDirtyAndSync();
      showToast(`Deleted Node #${node.index}`);
    });
  }

  // Header & Meta fields
  const inpNodeIndex = document.getElementById("inpNodeIndex");
  if (inpNodeIndex) {
    inpNodeIndex.addEventListener("input", (e) => {
      node.index = parseInt(e.target.value, 10) || 1;
      syncWithoutFullWorkspaceRerender();
    });
  }

  const inpNodeName = document.getElementById("inpNodeName");
  if (inpNodeName) {
    inpNodeName.addEventListener("input", (e) => {
      node.name = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
  }

  const inpNodeTopic = document.getElementById("inpNodeTopic");
  if (inpNodeTopic) {
    inpNodeTopic.addEventListener("input", (e) => {
      node.topic = e.target.value.replace(/\s+/g, "");
      syncWithoutFullWorkspaceRerender();
    });
  }

  const selNodeType = document.getElementById("selNodeType");
  if (selNodeType) {
    selNodeType.addEventListener("change", (e) => {
      node.type = e.target.value;
      if (node.type !== "MASTER") node.hasMetaBlock = true;
      markDirtyAndSync();
    });
  }

  const inpMetaMaster = document.getElementById("inpMetaMaster");
  if (inpMetaMaster) {
    inpMetaMaster.addEventListener("input", (e) => {
      const val = e.target.value.trim();
      node.meta.master = val === "" ? null : parseInt(val, 10);
      node.hasMetaBlock = true;
      syncWithoutFullWorkspaceRerender();
    });
  }

  const inpMetaSlaves = document.getElementById("inpMetaSlaves");
  if (inpMetaSlaves) {
    inpMetaSlaves.addEventListener("input", (e) => {
      const nums = e.target.value.match(/[0-9]+/g);
      node.meta.slaves = nums ? nums.map((n) => parseInt(n, 10)) : [];
      node.hasMetaBlock = true;
      syncWithoutFullWorkspaceRerender();
    });
  }

  document.querySelectorAll(".slave-chip").forEach((chip) => {
    chip.addEventListener("click", () => {
      const targetId = parseInt(chip.dataset.slaveId, 10);
      const list = node.meta.slaves || [];
      const idx = list.indexOf(targetId);
      if (idx === -1) {
        list.push(targetId);
      } else {
        list.splice(idx, 1);
      }
      node.meta.slaves = list;
      node.hasMetaBlock = true;
      markDirtyAndSync();
    });
  });

  // Variables Events
  const btnAddVariable = document.getElementById("btnAddVariable");
  if (btnAddVariable) {
    btnAddVariable.addEventListener("click", () => {
      const nextNum = node.variables.length + 1;
      node.variables.push({
        isGlobal: false,
        type: "int",
        name: `$var${nextNum}`,
        value: "0",
      });
      markDirtyAndSync();
    });
  }

  const btnAddArrayVariable = document.getElementById("btnAddArrayVariable");
  if (btnAddArrayVariable) {
    btnAddArrayVariable.addEventListener("click", () => {
      const arrCount = node.variables.filter((v) => v.type === "array").length + 1;
      node.variables.push({
        isGlobal: false,
        type: "array",
        name: `$arr${arrCount}`,
        value: "[]",
      });
      markDirtyAndSync();
      showToast(`Created array variable $arr${arrCount} — pick variables from its dropdown`);
    });
  }

  const btnAddVarPresetSensor = document.getElementById("btnAddVarPresetSensor");
  if (btnAddVarPresetSensor) {
    btnAddVarPresetSensor.addEventListener("click", () => {
      ensureVariable(node, "$e1", "int", "10", true);
      const resIdx = node.variables.filter((v) => v.name.startsWith("$res")).length + 1;
      const resName = `$res0${resIdx}`;
      ensureVariable(node, resName, "float", "0.0", true);
      markDirtyAndSync();
      showToast(`Added sensor variables $e1 & ${resName}`);
    });
  }

  const btnAddVarPresetSwitch = document.getElementById("btnAddVarPresetSwitch");
  if (btnAddVarPresetSwitch) {
    btnAddVarPresetSwitch.addEventListener("click", () => {
      const swIdx = node.variables.filter((v) => /^\$c[0-9]+$/.test(v.name)).length + 1;
      ensureVariable(node, `$c${swIdx}`, "int", "0", true);
      ensureVariable(node, `$f${swIdx}`, "string", "", false);
      markDirtyAndSync();
      showToast(`Added switch variables $c${swIdx} & $f${swIdx}`);
    });
  }

  document.querySelectorAll(".var-global").forEach((el) => {
    el.addEventListener("change", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      node.variables[idx].isGlobal = e.target.checked;
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".var-type").forEach((el) => {
    el.addEventListener("change", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      const newType = e.target.value;
      node.variables[idx].type = newType;
      if (newType === "fun") node.variables[idx].isGlobal = false;
      if (newType === "array" && !String(node.variables[idx].value).trim().startsWith("[")) {
        node.variables[idx].value = "[]";
      }
      markDirtyAndSync();
    });
  });

  document.querySelectorAll(".var-name").forEach((el) => {
    el.addEventListener("input", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      let val = e.target.value.trim();
      if (val && !val.startsWith("$")) val = "$" + val;
      node.variables[idx].name = val;
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".var-val").forEach((el) => {
    el.addEventListener("input", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      node.variables[idx].value = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      if (node.variables[idx].type === "array") {
        markDirtyAndSync();
      }
    });
  });

  document.querySelectorAll(".var-arr-add").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const idx = parseInt(e.currentTarget.dataset.idx, 10);
      const picker = document.querySelector(`.var-arr-picker[data-idx="${idx}"]`);
      if (!picker || !picker.value) return;
      node.variables[idx].value = appendTokenToArrayString(node.variables[idx].value, picker.value);
      markDirtyAndSync();
      showToast(`Added ${picker.value} to ${node.variables[idx].name}`);
    });
  });

  document.querySelectorAll(".array-chip-del").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const vidx = parseInt(e.currentTarget.dataset.vidx, 10);
      const tidx = parseInt(e.currentTarget.dataset.tidx, 10);
      const items = parseArrayTokens(node.variables[vidx].value);
      items.splice(tidx, 1);
      node.variables[vidx].value = `[${items.join(", ")}]`;
      markDirtyAndSync();
    });
  });

  document.querySelectorAll(".var-del").forEach((el) => {
    el.addEventListener("click", (e) => {
      const idx = parseInt(e.currentTarget.dataset.idx, 10);
      node.variables.splice(idx, 1);
      markDirtyAndSync();
    });
  });

  // Sensors Events
  const chkAutoVarsSensors = document.getElementById("chkAutoVarsSensors");
  if (chkAutoVarsSensors) {
    chkAutoVarsSensors.addEventListener("change", (e) => {
      appState.autoAddVarsSensors = e.target.checked;
      if (appState.autoAddVarsSensors) {
        let added = 0;
        for (const s of node.sensors) {
          if (s.epoch && s.epoch !== "NULL" && ensureVariable(node, s.epoch, "int", "10", true)) added++;
          if (s.readings && ensureVariable(node, s.readings, "float", "0.0", true)) added++;
        }
        if (added > 0) {
          markDirtyAndSync();
          showToast(`Auto-created ${added} missing sensor variable${added > 1 ? "s" : ""}`);
        }
      }
    });
  }

  const btnAddSensor = document.getElementById("btnAddSensor");
  if (btnAddSensor) {
    btnAddSensor.addEventListener("click", () => {
      const hwIdx = node.sensors.length;
      const resName = `$res0${hwIdx + 1}`;
      if (appState.autoAddVarsSensors) {
        ensureVariable(node, "$e1", "int", "10", true);
        ensureVariable(node, resName, "float", "0.0", true);
      }
      node.sensors.push({
        name: hwIdx === 0 ? "temp" : `sensor${hwIdx}`,
        index: hwIdx,
        epoch: "$e1",
        readings: resName,
      });
      markDirtyAndSync();
    });
  }

  document.querySelectorAll(".sen-name").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.sensors[parseInt(e.target.dataset.idx, 10)].name = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".sen-index").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.sensors[parseInt(e.target.dataset.idx, 10)].index = parseInt(e.target.value, 10) || 0;
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".sen-epoch").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.sensors[parseInt(e.target.dataset.idx, 10)].epoch = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsSensors && val && val !== "NULL") {
        if (ensureVariable(node, val, "int", "10", true)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".sen-readings").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.sensors[parseInt(e.target.dataset.idx, 10)].readings = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsSensors && val) {
        if (ensureVariable(node, val, "float", "0.0", true)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".sen-del").forEach((el) => {
    el.addEventListener("click", (e) => {
      node.sensors.splice(parseInt(e.currentTarget.dataset.idx, 10), 1);
      markDirtyAndSync();
    });
  });

  // Elements Events
  const chkAutoVarsElements = document.getElementById("chkAutoVarsElements");
  if (chkAutoVarsElements) {
    chkAutoVarsElements.addEventListener("change", (e) => {
      appState.autoAddVarsElements = e.target.checked;
      if (appState.autoAddVarsElements) {
        let added = 0;
        for (const el of node.elements) {
          if (el.elType === "switch") {
            if (ensureVariable(node, el.value, "int", "0", true)) added++;
            if (ensureVariable(node, el.force, "string", "", false)) added++;
          } else if (el.elType === "trigger") {
            if (ensureVariable(node, el.value, "string", "0", false)) added++;
            if (ensureVariable(node, el.force, "string", "", false)) added++;
          } else if (el.elType === "list") {
            if (ensureVariable(node, el.listValues, "array", "[0, 1, 2]", false)) added++;
            if (ensureVariable(node, el.listNames, "array", '["off", "cooling", "heating"]', false)) added++;
            if (ensureVariable(node, el.state, "int", "0", false)) added++;
            if (ensureVariable(node, el.force, "string", "", false)) added++;
          }
        }
        if (added > 0) {
          markDirtyAndSync();
          showToast(`Auto-created ${added} missing element variable${added > 1 ? "s" : ""}`);
        }
      }
    });
  }

  const btnAddSwitch = document.getElementById("btnAddSwitch");
  if (btnAddSwitch) {
    btnAddSwitch.addEventListener("click", () => {
      const swCount = node.elements.filter((e) => e.elType === "switch").length;
      const cVar = `$c${swCount + 1}`;
      const fVar = `$f${swCount + 1}`;
      if (appState.autoAddVarsElements) {
        ensureVariable(node, cVar, "int", "0", true);
        ensureVariable(node, fVar, "string", "", false);
      }
      node.elements.push({
        elType: "switch",
        name: `switch${swCount}`,
        index: swCount,
        value: cVar,
        force: fVar,
      });
      markDirtyAndSync();
    });
  }

  const btnAddTrigger = document.getElementById("btnAddTrigger");
  if (btnAddTrigger) {
    btnAddTrigger.addEventListener("click", () => {
      const trCount = node.elements.filter((e) => e.elType === "trigger").length;
      const tVar = `$t${trCount + 1}`;
      const ftVar = `$ft${trCount + 1}`;
      if (appState.autoAddVarsElements) {
        ensureVariable(node, tVar, "string", "0", false);
        ensureVariable(node, ftVar, "string", "", false);
      }
      node.elements.push({
        elType: "trigger",
        name: `trigger${trCount}`,
        index: trCount,
        value: tVar,
        force: ftVar,
      });
      markDirtyAndSync();
    });
  }

  const btnAddList = document.getElementById("btnAddList");
  if (btnAddList) {
    btnAddList.addEventListener("click", () => {
      const lCount = node.elements.filter((e) => e.elType === "list").length;
      const suffix = lCount + 1;
      if (appState.autoAddVarsElements) {
        ensureVariable(node, `$lv${suffix}`, "array", "[0, 1, 2]", false);
        ensureVariable(node, `$ln${suffix}`, "array", '["off", "cooling", "heating"]', false);
        ensureVariable(node, `$l${suffix}`, "int", "0", false);
        ensureVariable(node, `$lf${suffix}`, "string", "", false);
      }
      node.elements.push({
        elType: "list",
        name: `list${lCount}`,
        index: lCount,
        listValues: `$lv${suffix}`,
        listNames: `$ln${suffix}`,
        state: `$l${suffix}`,
        force: `$lf${suffix}`,
      });
      markDirtyAndSync();
    });
  }

  document.querySelectorAll(".el-name").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.elements[parseInt(e.target.dataset.idx, 10)].name = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".el-index").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.elements[parseInt(e.target.dataset.idx, 10)].index = parseInt(e.target.value, 10) || 0;
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".el-value").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.elements[parseInt(e.target.dataset.idx, 10)].value = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      const elem = node.elements[idx];
      const val = e.target.value.trim();
      if (appState.autoAddVarsElements && val) {
        const isSw = elem.elType === "switch";
        if (ensureVariable(node, val, isSw ? "int" : "string", "0", isSw)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".el-force").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.elements[parseInt(e.target.dataset.idx, 10)].force = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsElements && val) {
        if (ensureVariable(node, val, "string", "", false)) {
          markDirtyAndSync();
          showToast(`Auto-added force variable ${val} (string = "")`);
        }
      }
    });
  });

  document.querySelectorAll(".el-listValues").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.elements[parseInt(e.target.dataset.idx, 10)].listValues = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsElements && val) {
        if (ensureVariable(node, val, "array", "[0, 1, 2]", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".el-listNames").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.elements[parseInt(e.target.dataset.idx, 10)].listNames = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsElements && val) {
        if (ensureVariable(node, val, "array", '["off", "cooling", "heating"]', false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".el-state").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.elements[parseInt(e.target.dataset.idx, 10)].state = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsElements && val) {
        if (ensureVariable(node, val, "int", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".el-del").forEach((el) => {
    el.addEventListener("click", (e) => {
      node.elements.splice(parseInt(e.currentTarget.dataset.idx, 10), 1);
      markDirtyAndSync();
    });
  });

  // Programs Events
  const chkAutoVarsPrograms = document.getElementById("chkAutoVarsPrograms");
  if (chkAutoVarsPrograms) {
    chkAutoVarsPrograms.addEventListener("change", (e) => {
      appState.autoAddVarsPrograms = e.target.checked;
      if (appState.autoAddVarsPrograms) {
        let added = 0;
        for (const p of node.programs) {
          if (p.funName && ensureVariable(node, p.funName, "fun", "0", false)) added++;
          if (p.progType === "valCondition") {
            for (const c of p.conditions || []) {
              if (c.left && c.left.startsWith("$") && ensureVariable(node, c.left, "int", "0", true)) added++;
              if (c.right && c.right.startsWith("$") && ensureVariable(node, c.right, "int", "0", false)) added++;
            }
            for (const r of p.results || []) {
              if (r.left && r.left.startsWith("$") && ensureVariable(node, r.left, "int", "0", true)) added++;
              if (r.right && r.right.startsWith("$") && ensureVariable(node, r.right, "int", "0", false)) added++;
            }
          } else if (p.progType === "serial") {
            if (p.switches && ensureVariable(node, p.switches, "array", "[$c1]", false)) added++;
            if (p.epoch && ensureVariable(node, p.epoch, "int", "10", false)) added++;
            if (p.time && ensureVariable(node, p.time, "int", "0", false)) added++;
            if (p.lastIndex && ensureVariable(node, p.lastIndex, "int", "0", false)) added++;
          } else if (p.progType === "timeLim") {
            if (p.functionVar && ensureVariable(node, p.functionVar, "fun", "0", false)) added++;
            if (p.startVar && ensureVariable(node, p.startVar, "string", "19:20", false)) added++;
            if (p.endVar && ensureVariable(node, p.endVar, "string", "6:00", false)) added++;
            for (const t of p.targets || []) {
              if (t.variable && ensureVariable(node, t.variable, "int", "0", true)) added++;
            }
          }
        }
        if (added > 0) {
          markDirtyAndSync();
          showToast(`Auto-created ${added} missing program variable${added > 1 ? "s" : ""}`);
        }
      }
    });
  }

  const btnAddValCondition = document.getElementById("btnAddValCondition");
  if (btnAddValCondition) {
    btnAddValCondition.addEventListener("click", () => {
      if (appState.autoAddVarsPrograms) {
        ensureVariable(node, "$c1", "int", "0", true);
      }
      node.programs.push({
        progType: "valCondition",
        funName: "",
        name: `condition ${node.programs.length + 1}`,
        conditions: [{ left: "$c1", op: "==", right: "0" }],
        results: [{ left: "$c1", op: "=", right: "1" }],
      });
      markDirtyAndSync();
    });
  }

  const btnAddSerial = document.getElementById("btnAddSerial");
  if (btnAddSerial) {
    btnAddSerial.addEventListener("click", () => {
      if (appState.autoAddVarsPrograms) {
        ensureVariable(node, "$c1", "int", "0", true);
        ensureVariable(node, "$listOfSwitches", "array", "[$c1]", false);
        ensureVariable(node, "$switchEp", "int", "10", false);
        ensureVariable(node, "$time", "int", "0", false);
        ensureVariable(node, "$lastIndex", "int", "0", false);
      }
      node.programs.push({
        progType: "serial",
        funName: "",
        name: "serial row",
        switches: "$listOfSwitches",
        epoch: "$switchEp",
        time: "$time",
        lastIndex: "$lastIndex",
      });
      markDirtyAndSync();
    });
  }

  const btnAddTimeLim = document.getElementById("btnAddTimeLim");
  if (btnAddTimeLim) {
    btnAddTimeLim.addEventListener("click", () => {
      const existingTimeLims = node.programs.filter((p) => p.progType === "timeLim").length;
      const funVarName = existingTimeLims === 0 ? "$lightsON" : `$lightsON${existingTimeLims + 1}`;
      const cleanName = funVarName.replace(/^\$/, "");

      if (appState.autoAddVarsPrograms) {
        ensureVariable(node, funVarName, "fun", "0", false);
        ensureVariable(node, "$start", "string", "19:20", false);
        ensureVariable(node, "$end", "string", "6:00", false);
        ensureVariable(node, "$c1", "int", "0", true);
      }

      const newTl = {
        progType: "timeLim",
        functionVar: funVarName,
        startVar: "$start",
        endVar: "$end",
        onProgName: existingTimeLims === 0 ? "light on" : `${cleanName} on`,
        offProgName: existingTimeLims === 0 ? "light off" : `${cleanName} off`,
        targets: [{ variable: "$c1", onValue: "1", offValue: "0" }],
      };

      node.programs.push(newTl);
      syncTimeLimValConditions(node, newTl);
      markDirtyAndSync();
      showToast(`Created timeLim(${funVarName}) + 2 linked valCondition programs`);
    });
  }

  document.querySelectorAll(".prog-del").forEach((el) => {
    el.addEventListener("click", (e) => {
      const idx = parseInt(e.currentTarget.dataset.idx, 10);
      const targetProg = node.programs[idx];
      if (targetProg && targetProg.progType === "timeLim") {
        targetProg.targets = [];
        syncTimeLimValConditions(node, targetProg);
        const newIdx = node.programs.indexOf(targetProg);
        if (newIdx !== -1) node.programs.splice(newIdx, 1);
      } else {
        node.programs.splice(idx, 1);
        hydrateTimeLimTargetsFromPrograms(node.programs);
      }
      markDirtyAndSync();
    });
  });

  // valCondition inputs
  document.querySelectorAll(".prog-vc-name").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].name = e.target.value;
      hydrateTimeLimTargetsFromPrograms(node.programs);
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".prog-vc-fun").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].funName = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "fun", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added fun variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".vc-add-cond").forEach((el) => {
    el.addEventListener("click", (e) => {
      const pidx = parseInt(e.currentTarget.dataset.pidx, 10);
      if (appState.autoAddVarsPrograms) {
        ensureVariable(node, "$c1", "int", "0", true);
      }
      node.programs[pidx].conditions.push({ left: "$c1", op: "==", right: "1" });
      hydrateTimeLimTargetsFromPrograms(node.programs);
      markDirtyAndSync();
    });
  });

  document.querySelectorAll(".vc-add-res").forEach((el) => {
    el.addEventListener("click", (e) => {
      const pidx = parseInt(e.currentTarget.dataset.pidx, 10);
      if (appState.autoAddVarsPrograms) {
        ensureVariable(node, "$c1", "int", "0", true);
      }
      node.programs[pidx].results.push({ left: "$c1", op: "=", right: "1" });
      hydrateTimeLimTargetsFromPrograms(node.programs);
      markDirtyAndSync();
    });
  });

  document.querySelectorAll(".vc-cond-left").forEach((el) => {
    el.addEventListener("input", (e) => {
      const { pidx, cidx } = e.target.dataset;
      node.programs[pidx].conditions[cidx].left = e.target.value.trim();
      hydrateTimeLimTargetsFromPrograms(node.programs);
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val.startsWith("$")) {
        if (ensureVariable(node, val, "int", "0", true)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".vc-cond-op").forEach((el) => {
    el.addEventListener("change", (e) => {
      const { pidx, cidx } = e.target.dataset;
      node.programs[pidx].conditions[cidx].op = e.target.value;
      hydrateTimeLimTargetsFromPrograms(node.programs);
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".vc-cond-right").forEach((el) => {
    el.addEventListener("input", (e) => {
      const { pidx, cidx } = e.target.dataset;
      node.programs[pidx].conditions[cidx].right = e.target.value.trim();
      hydrateTimeLimTargetsFromPrograms(node.programs);
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val.startsWith("$")) {
        if (ensureVariable(node, val, "int", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".vc-cond-del").forEach((el) => {
    el.addEventListener("click", (e) => {
      const { pidx, cidx } = e.currentTarget.dataset;
      if (node.programs[pidx].conditions.length > 1) {
        node.programs[pidx].conditions.splice(parseInt(cidx, 10), 1);
        hydrateTimeLimTargetsFromPrograms(node.programs);
        markDirtyAndSync();
      }
    });
  });

  document.querySelectorAll(".vc-res-left").forEach((el) => {
    el.addEventListener("input", (e) => {
      const { pidx, ridx } = e.target.dataset;
      node.programs[pidx].results[ridx].left = e.target.value.trim();
      hydrateTimeLimTargetsFromPrograms(node.programs);
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val.startsWith("$")) {
        if (ensureVariable(node, val, "int", "0", true)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".vc-res-op").forEach((el) => {
    el.addEventListener("change", (e) => {
      const { pidx, ridx } = e.target.dataset;
      node.programs[pidx].results[ridx].op = e.target.value;
      hydrateTimeLimTargetsFromPrograms(node.programs);
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".vc-res-right").forEach((el) => {
    el.addEventListener("input", (e) => {
      const { pidx, ridx } = e.target.dataset;
      node.programs[pidx].results[ridx].right = e.target.value.trim();
      hydrateTimeLimTargetsFromPrograms(node.programs);
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val.startsWith("$")) {
        if (ensureVariable(node, val, "int", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".vc-res-del").forEach((el) => {
    el.addEventListener("click", (e) => {
      const { pidx, ridx } = e.currentTarget.dataset;
      if (node.programs[pidx].results.length > 1) {
        node.programs[pidx].results.splice(parseInt(ridx, 10), 1);
        hydrateTimeLimTargetsFromPrograms(node.programs);
        markDirtyAndSync();
      }
    });
  });

  // Serial inputs
  document.querySelectorAll(".prog-ser-fun").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].funName = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "fun", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added fun variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".prog-ser-name").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].name = e.target.value;
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".prog-ser-switches").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].switches = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "array", "[$c1]", false)) {
          markDirtyAndSync();
          showToast(`Auto-added array variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".prog-ser-epoch").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].epoch = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "int", "10", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".prog-ser-time").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].time = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "int", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".prog-ser-last").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].lastIndex = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "int", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  // timeLim inputs & linked valConditions
  document.querySelectorAll(".prog-tl-fun").forEach((el) => {
    el.addEventListener("input", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      const tlProg = node.programs[idx];
      const oldFun = tlProg.functionVar;
      let newFun = e.target.value.trim();
      if (newFun && !newFun.startsWith("$")) newFun = "$" + newFun;
      tlProg.functionVar = newFun;
      syncTimeLimValConditions(node, tlProg, oldFun);
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "fun", "0", false)) {
          markDirtyAndSync();
          showToast(`Auto-added fun variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".prog-tl-start").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].startVar = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "string", "19:20", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".prog-tl-end").forEach((el) => {
    el.addEventListener("input", (e) => {
      node.programs[parseInt(e.target.dataset.idx, 10)].endVar = e.target.value.trim();
      syncWithoutFullWorkspaceRerender();
    });
    el.addEventListener("change", (e) => {
      const val = e.target.value.trim();
      if (appState.autoAddVarsPrograms && val) {
        if (ensureVariable(node, val, "string", "6:00", false)) {
          markDirtyAndSync();
          showToast(`Auto-added variable ${val}`);
        }
      }
    });
  });

  document.querySelectorAll(".prog-tl-onname").forEach((el) => {
    el.addEventListener("input", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      const tlProg = node.programs[idx];
      tlProg.onProgName = e.target.value;
      syncTimeLimValConditions(node, tlProg);
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".prog-tl-offname").forEach((el) => {
    el.addEventListener("input", (e) => {
      const idx = parseInt(e.target.dataset.idx, 10);
      const tlProg = node.programs[idx];
      tlProg.offProgName = e.target.value;
      syncTimeLimValConditions(node, tlProg);
      syncWithoutFullWorkspaceRerender();
    });
  });

  document.querySelectorAll(".tl-add-picked-var").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const pidx = parseInt(e.currentTarget.dataset.pidx, 10);
      const tlProg = node.programs[pidx];
      const picker = document.querySelector(`.tl-var-picker[data-pidx="${pidx}"]`);
      const pickedVar = picker ? picker.value : "$c1";
      if (!tlProg.targets) tlProg.targets = [];
      if (tlProg.targets.some((t) => t.variable === pickedVar)) {
        showToast(`${pickedVar} is already in this timeLim's controlled variables`);
        return;
      }
      tlProg.targets.push({ variable: pickedVar, onValue: "1", offValue: "0" });
      syncTimeLimValConditions(node, tlProg);
      markDirtyAndSync();
      showToast(`Added ${pickedVar} to timeLim (${tlProg.functionVar}) ON/OFF valConditions`);
    });
  });

  document.querySelectorAll(".tl-target-var").forEach((el) => {
    el.addEventListener("change", (e) => {
      const { pidx, tidx } = e.target.dataset;
      const tlProg = node.programs[parseInt(pidx, 10)];
      let val = e.target.value.trim();
      if (val && !val.startsWith("$")) val = "$" + val;
      tlProg.targets[parseInt(tidx, 10)].variable = val;
      syncTimeLimValConditions(node, tlProg);
      markDirtyAndSync();
    });
  });

  document.querySelectorAll(".tl-target-on").forEach((el) => {
    el.addEventListener("change", (e) => {
      const { pidx, tidx } = e.target.dataset;
      const tlProg = node.programs[parseInt(pidx, 10)];
      tlProg.targets[parseInt(tidx, 10)].onValue = e.target.value.trim();
      syncTimeLimValConditions(node, tlProg);
      markDirtyAndSync();
    });
  });

  document.querySelectorAll(".tl-target-off").forEach((el) => {
    el.addEventListener("change", (e) => {
      const { pidx, tidx } = e.target.dataset;
      const tlProg = node.programs[parseInt(pidx, 10)];
      tlProg.targets[parseInt(tidx, 10)].offValue = e.target.value.trim();
      syncTimeLimValConditions(node, tlProg);
      markDirtyAndSync();
    });
  });

  document.querySelectorAll(".tl-target-del").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const { pidx, tidx } = e.currentTarget.dataset;
      const tlProg = node.programs[parseInt(pidx, 10)];
      tlProg.targets.splice(parseInt(tidx, 10), 1);
      syncTimeLimValConditions(node, tlProg);
      markDirtyAndSync();
    });
  });
}

// Updates sidebar, topology, code, and diagnostics without destroying input focus in center workspace
function syncWithoutFullWorkspaceRerender() {
  appState.isDirty = true;
  elStatusDot.className = "status-dot dirty";
  renderSidebarNodes();
  renderTopology();
  const code = serializeFnScript(appState.model);
  elCodeTextarea.value = code;
  updateCodeLineNumbers();
  localStorage.setItem(STORAGE_KEY, code);
  renderDiagnostics();
}

function ensureVariable(node, name, type, defaultValue, isGlobal = false) {
  let cleanName = String(name ?? "").trim();
  if (!cleanName || cleanName === "NULL") return false;
  if (!cleanName.startsWith("$")) cleanName = "$" + cleanName;
  // Cross-node global references ($2$c1) belong to another node
  if (/^\$[0-9]+\$/.test(cleanName)) return false;

  const exists = node.variables.some((v) => v.name === cleanName);
  if (!exists) {
    node.variables.push({
      isGlobal: type === "fun" ? false : isGlobal,
      type,
      name: cleanName,
      value: defaultValue,
    });
    return true;
  }
  return false;
}

function addNewNode() {
  const maxId = appState.model.nodes.reduce((m, n) => Math.max(m, Number(n.index) || 0), 0);
  const nextId = maxId + 1;
  appState.model.nodes.push({
    index: nextId,
    name: `Node ${nextId}`,
    topic: `${nextId}f`,
    type: "MASTER",
    hasMetaBlock: false,
    meta: { master: null, slaves: [] },
    variables: [
      { isGlobal: true, type: "int", name: "$e1", value: "10" },
      { isGlobal: true, type: "float", name: "$res01", value: "0.0" },
      { isGlobal: true, type: "int", name: "$c1", value: "0" },
      { isGlobal: false, type: "string", name: "$f1", value: "" },
    ],
    sensors: [{ name: "temp", index: 0, epoch: "$e1", readings: "$res01" }],
    elements: [{ elType: "switch", name: "light0", index: 0, value: "$c1", force: "$f1" }],
    programs: [],
  });
  appState.selectedNodeIdx = appState.model.nodes.length - 1;
  markDirtyAndSync();
  showToast(`Added Node #${nextId}`);
}

// Diagnostics & Validation
function getCurrentScriptValidation() {
  const codeText = elCodeTextarea ? elCodeTextarea.value : "";
  const syntaxDiags =
    typeof validateFnTextSyntax === "function" && elCodeTextarea
      ? validateFnTextSyntax(codeText)
      : [];

  let modelToValidate = appState.model;
  const serializedModel = serializeFnScript(appState.model);
  if (elCodeTextarea && codeText.trim() !== serializedModel.trim()) {
    try {
      const parsedFromCode = parseFnScript(codeText);
      if (parsedFromCode && Array.isArray(parsedFromCode.nodes)) {
        modelToValidate = parsedFromCode;
      }
    } catch (_) {
      // Syntax diagnostics above already capture parse failures
    }
  }

  const modelDiags = validateFnModel(modelToValidate);
  const diags = syntaxDiags.length > 0 ? [...syntaxDiags, ...modelDiags] : modelDiags;
  const errors = diags.filter((d) => d.severity === "error").length;
  const warns = diags.filter((d) => d.severity === "warn").length;
  return { diags, errors, warns, valid: errors === 0 };
}

function updateSaveButtonsState(valid, errorCount) {
  const btnSaveDirect = document.getElementById("btnSaveDirect");
  const btnDownloadFile = document.getElementById("btnDownloadFile");
  const btnConfirmSaveScript = document.getElementById("btnConfirmSaveScript");
  const blockedTitle = `Cannot save: ${errorCount} validation error(s). Fix errors in Diagnostics first.`;

  if (btnSaveDirect) {
    btnSaveDirect.disabled = !valid;
    btnSaveDirect.title = valid ? "Save script to AAU/script/ on server" : blockedTitle;
  }
  if (btnDownloadFile) {
    btnDownloadFile.disabled = !valid;
    btnDownloadFile.title = valid ? "Download script.fn file" : blockedTitle;
  }
  if (btnConfirmSaveScript) {
    btnConfirmSaveScript.disabled = !valid;
    btnConfirmSaveScript.title = valid ? "Save .fn script" : blockedTitle;
  }
}

function renderDiagnostics() {
  const { diags, errors, warns, valid } = getCurrentScriptValidation();
  elDiagCount.textContent = diags.length;
  updateSaveButtonsState(valid, errors);

  if (errors > 0) {
    elValidationBadge.className = "validation-pill error";
    elValidationBadge.textContent = `✕ ${errors} Error${errors > 1 ? "s" : ""}${warns > 0 ? `, ${warns} Warn` : ""}`;
  } else if (warns > 0) {
    elValidationBadge.className = "validation-pill warn";
    elValidationBadge.textContent = `⚠ ${warns} Warning${warns > 1 ? "s" : ""}`;
  } else {
    elValidationBadge.className = "validation-pill valid";
    elValidationBadge.textContent = "✓ Valid .fn";
  }

  if (diags.length === 0) {
    elDiagnosticsList.innerHTML = `
      <div class="empty-state" style="color:var(--success);border-color:rgba(63,185,80,0.3);">
        ✓ All nodes, variables, sensors, elements, and programs pass AAU .fn validation checks.
      </div>
    `;
    return;
  }

  elDiagnosticsList.innerHTML = diags
    .map((d) => {
      const hasNodeBtn = d.nodeIndex !== "-" && !isNaN(Number(d.nodeIndex));
      const locLabel = hasNodeBtn ? `Node #${escapeHtml(d.nodeIndex)}` : "Script";
      return `
      <div class="diag-item ${d.severity}">
        <div class="diag-item-header">
          <span>${d.severity === "error" ? "✕ Error" : "⚠ Warning"} — ${locLabel} (${escapeHtml(d.section)})</span>
          ${hasNodeBtn ? `<button class="btn btn-xs btn-secondary diag-jump" data-node-id="${escapeHtml(d.nodeIndex)}">Inspect Node</button>` : ""}
        </div>
        <div style="color:var(--text-muted);">${escapeHtml(d.message)}</div>
      </div>
    `;
    })
    .join("");

  document.querySelectorAll(".diag-jump").forEach((btn) => {
    btn.addEventListener("click", () => {
      const targetId = parseInt(btn.dataset.nodeId, 10);
      const foundIdx = appState.model.nodes.findIndex((n) => Number(n.index) === targetId);
      if (foundIdx !== -1) {
        appState.selectedNodeIdx = foundIdx;
        renderAll(false);
      }
    });
  });
}

// Topbar & File I/O Events
function bindTopbarEvents() {
  document.getElementById("btnAddNode").addEventListener("click", () => addNewNode());

  elNodeSearchInput.addEventListener("input", (e) => {
    appState.nodeFilter = e.target.value;
    renderSidebarNodes();
  });

  document.getElementById("btnNewScript").addEventListener("click", () => {
    appState.fileName = "new_script.fn";
    appState.fileHandle = null;
    appState.model = { nodes: [] };
    addNewNode();
    appState.isDirty = false;
    renderAll(true);
    showToast("Started a fresh .fn script");
  });

  document.getElementById("btnLoadSample").addEventListener("click", () => {
    appState.fileName = "script.fn";
    appState.fileHandle = null;
    appState.model = parseFnScript(SAMPLE_FN_SCRIPT);
    appState.selectedNodeIdx = 0;
    appState.isDirty = false;
    renderAll(true);
    showToast("Loaded sample AAU/script/script.fn");
  });

  function loadFnFileIntoStudio(fileName, text, fileHandle = null, toastMessage = null) {
    if (!isFnFileName(fileName)) {
      showToast("Only .fn files can be loaded into FN Script Studio");
      return false;
    }
    appState.fileHandle = fileHandle;
    appState.fileName = fileName;
    appState.model = parseFnScript(text);
    appState.selectedNodeIdx = 0;
    appState.isDirty = false;
    renderAll(true);
    showToast(toastMessage || `Opened ${fileName}`);
    return true;
  }

  function ensureScriptValidForSave() {
    const validation = getCurrentScriptValidation();
    if (!validation.valid) {
      renderDiagnostics();
      switchCodeTab("diagnostics");
      showToast(`Cannot save: ${validation.errors} validation error(s). Check Diagnostics.`);
      return false;
    }
    return true;
  }

  // Open .fn file (File System Access API with fallback to <input type="file">)
  document.getElementById("btnOpenFile").addEventListener("click", async () => {
    if (window.showOpenFilePicker) {
      try {
        const [handle] = await window.showOpenFilePicker({
          types: [
            {
              description: "Forge Node Script (.fn)",
              accept: { "text/plain": [".fn"] },
            },
          ],
          excludeAcceptAllOption: true,
          multiple: false,
        });
        const file = await handle.getFile();
        if (!isFnFileName(file.name)) {
          showToast("Only .fn files can be loaded into FN Script Studio");
          return;
        }
        const text = await file.text();
        loadFnFileIntoStudio(file.name, text, handle, `Opened ${file.name}`);
        return;
      } catch (err) {
        if (err.name === "AbortError") return;
      }
    }
    elFileInput.click();
  });

  elFileInput.addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      if (!isFnFileName(file.name)) {
        showToast("Only .fn files can be loaded into FN Script Studio");
        return;
      }
      const text = await file.text();
      loadFnFileIntoStudio(file.name, text, null, `Loaded ${file.name}`);
    } finally {
      elFileInput.value = "";
    }
  });

  // Save script directly into AAU/script/ on the server (persisted in Docker volume)
  const saveScriptModal = document.getElementById("saveScriptModal");
  const inpSaveScriptName = document.getElementById("inpSaveScriptName");
  const chkSaveScriptDefault = document.getElementById("chkSaveScriptDefault");
  const saveServerFileList = document.getElementById("saveServerFileList");
  const openServerModal = document.getElementById("openServerModal");
  const openServerFileList = document.getElementById("openServerFileList");
  const chkOpenServerSetDefault = document.getElementById("chkOpenServerSetDefault");
  const defaultOpenScriptHint = document.getElementById("defaultOpenScriptHint");

  async function saveDefaultScript(fileName) {
    const res = await fetch("/api/working-fn/default", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fileName }),
    });
    const data = await res.json();
    if (!res.ok || !data.ok) {
      throw new Error(data.error || "Failed to set default script");
    }
    appState.defaultScript = data.defaultScript || fileName;
    updateDefaultScriptButton();
    return appState.defaultScript;
  }

  async function fetchServerScriptsList() {
    const res = await fetch("/api/files/scripts/list", { cache: "no-store" });
    if (!res.ok) throw new Error("Failed to list scripts in AAU/script/");
    const data = await res.json();
    const files = Array.isArray(data.files) ? data.files : [];
    return files.filter(isFnFileName);
  }

  function renderServerFileButtons(container, files, emptyText, onSelect) {
    container.innerHTML = "";
    if (!files.length) {
      const empty = document.createElement("div");
      empty.textContent = emptyText;
      empty.style.cssText = "font-size:12px;color:var(--text-muted);padding:6px 2px;";
      container.appendChild(empty);
      return;
    }
    for (const name of files) {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn btn-secondary btn-sm";
      btn.textContent = name === appState.defaultScript ? `${name} (default)` : name;
      btn.style.cssText = "justify-content:flex-start;font-family:var(--font-mono);text-align:left;";
      btn.addEventListener("click", () => onSelect(name));
      container.appendChild(btn);
    }
  }

  function closeSaveScriptModal() {
    saveScriptModal.classList.add("hidden");
  }

  async function openSaveScriptModal(precheckDefault = false) {
    if (!ensureScriptValidForSave()) return;
    inpSaveScriptName.value = isFnFileName(appState.fileName) ? appState.fileName : "script.fn";
    if (chkSaveScriptDefault) {
      chkSaveScriptDefault.checked = Boolean(
        precheckDefault || (appState.defaultScript && appState.fileName === appState.defaultScript)
      );
    }
    saveScriptModal.classList.remove("hidden");
    inpSaveScriptName.focus();
    inpSaveScriptName.select();
    try {
      const files = await fetchServerScriptsList();
      renderServerFileButtons(saveServerFileList, files, "No .fn scripts in AAU/script/ yet.", (name) => {
        inpSaveScriptName.value = name;
      });
    } catch (_) {
      renderServerFileButtons(saveServerFileList, [], "Could not load existing files.", () => {});
    }
  }

  document.getElementById("btnCloseSaveScript").addEventListener("click", closeSaveScriptModal);
  document.getElementById("btnCancelSaveScript").addEventListener("click", closeSaveScriptModal);

  document.getElementById("btnSaveDirect").addEventListener("click", () => openSaveScriptModal(false));

  const btnSetDefaultScript = document.getElementById("btnSetDefaultScript");
  if (btnSetDefaultScript) {
    btnSetDefaultScript.addEventListener("click", async () => {
      if (!ensureScriptValidForSave()) return;
      try {
        const files = await fetchServerScriptsList();
        const activeName = isFnFileName(appState.fileName) ? appState.fileName.trim() : "";
        if (activeName && files.includes(activeName) && !appState.isDirty) {
          await saveDefaultScript(activeName);
          showToast(`Set "${activeName}" as default script (AAU/script/default-script.conf)`);
          return;
        }
      } catch (_) {}
      await openSaveScriptModal(true);
    });
  }

  document.getElementById("btnConfirmSaveScript").addEventListener("click", async () => {
    if (!ensureScriptValidForSave()) return;
    let fileName = inpSaveScriptName.value.trim();
    if (!fileName) {
      showToast("Filename cannot be empty");
      return;
    }
    if (!isFnFileName(fileName)) {
      if (fileName.includes(".")) {
        showToast("Only .fn script files can be saved");
        return;
      }
      fileName += ".fn";
    }

    const code = elCodeTextarea.value.trim() !== serializeFnScript(appState.model).trim()
      ? elCodeTextarea.value
      : serializeFnScript(appState.model);
    try {
      const res = await fetch("/api/files/scripts/save", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fileName, content: code }),
      });
      const data = await res.json();
      if (!res.ok || !data.ok) {
        throw new Error(data.error || "Failed to save script");
      }
      const savedName = data.fileName || fileName;
      appState.fileName = savedName;
      appState.model = parseFnScript(code);
      appState.isDirty = false;
      const shouldSetDefault = chkSaveScriptDefault && chkSaveScriptDefault.checked;
      if (shouldSetDefault) {
        await saveDefaultScript(savedName);
      }
      closeSaveScriptModal();
      renderAll(false);
      localStorage.setItem(STORAGE_KEY, code);
      localStorage.setItem(STORAGE_FILENAME_KEY, savedName);
      showToast(
        shouldSetDefault
          ? `Saved ${savedName} to AAU/script/ and set as default`
          : `Saved ${savedName} to AAU/script/`
      );
    } catch (err) {
      showToast(err.message || "Failed to save script");
    }
  });

  function closeOpenServerModal() {
    openServerModal.classList.add("hidden");
  }

  document.getElementById("btnCloseOpenServer").addEventListener("click", closeOpenServerModal);
  document.getElementById("btnCancelOpenServer").addEventListener("click", closeOpenServerModal);

  document.getElementById("btnOpenServer").addEventListener("click", async () => {
    openServerModal.classList.remove("hidden");
    if (chkOpenServerSetDefault) chkOpenServerSetDefault.checked = false;
    openServerFileList.innerHTML = '<div style="font-size:12px;color:var(--text-muted);">Loading…</div>';
    try {
      const [files, defRes] = await Promise.all([
        fetchServerScriptsList(),
        fetch("/api/working-fn/default", { cache: "no-store" }).catch(() => null),
      ]);
      if (defRes && defRes.ok) {
        const defData = await defRes.json();
        appState.defaultScript = defData.defaultScript || "";
        updateDefaultScriptButton();
      }
      if (defaultOpenScriptHint) {
        defaultOpenScriptHint.textContent = appState.defaultScript
          ? `Default: ${appState.defaultScript}`
          : "No default set";
      }
      renderServerFileButtons(openServerFileList, files, "No .fn scripts found in AAU/script/.", async (name) => {
        if (!isFnFileName(name)) {
          showToast("Only .fn files can be loaded into FN Script Studio");
          return;
        }
        try {
          const res = await fetch(`/api/files/scripts/download?name=${encodeURIComponent(name)}`, { cache: "no-store" });
          if (!res.ok) throw new Error(`Failed to load ${name}`);
          const text = await res.text();
          const shouldSetDefault = chkOpenServerSetDefault && chkOpenServerSetDefault.checked;
          if (shouldSetDefault) {
            await saveDefaultScript(name);
          }
          if (loadFnFileIntoStudio(name, text, null, shouldSetDefault ? `Opened ${name} and set as default` : `Opened ${name} from AAU/script/`)) {
            closeOpenServerModal();
          }
        } catch (err) {
          showToast(err.message || "Failed to open script");
        }
      });
    } catch (err) {
      renderServerFileButtons(openServerFileList, [], err.message || "Failed to list AAU/script/", () => {});
    }
  });

  // Download .fn file
  document.getElementById("btnDownloadFile").addEventListener("click", () => {
    if (!ensureScriptValidForSave()) return;
    const code = elCodeTextarea.value.trim() !== serializeFnScript(appState.model).trim()
      ? elCodeTextarea.value
      : serializeFnScript(appState.model);
    downloadFnFile(code, appState.fileName || "script.fn");
  });

  // Click validation badge to jump to Diagnostics tab
  elValidationBadge.addEventListener("click", () => {
    switchCodeTab("diagnostics");
  });

  // Drag & Drop .fn file anywhere
  window.addEventListener("dragover", (e) => e.preventDefault());
  window.addEventListener("drop", async (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (!isFnFileName(file.name)) {
        showToast("Only .fn files can be loaded into FN Script Studio");
        return;
      }
      const text = await file.text();
      loadFnFileIntoStudio(file.name, text, null, `Opened dropped file: ${file.name}`);
    }
  });
}

function downloadFnFile(content, filename) {
  const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = isFnFileName(filename) ? filename : `${filename}.fn`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  appState.isDirty = false;
  renderAll(false);
  showToast(`Downloaded ${a.download}`);
}

// Code Panel Events & Sync
function bindCodePanelEvents() {
  document.querySelectorAll(".code-tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      switchCodeTab(tab.dataset.codeTab);
    });
  });

  elCodeTextarea.addEventListener("scroll", () => {
    elLineNumbers.scrollTop = elCodeTextarea.scrollTop;
  });

  elCodeTextarea.addEventListener("input", () => {
    updateCodeLineNumbers();
    appState.isDirty = true;
    elStatusDot.className = "status-dot dirty";

    if (elChkAutoSyncCode.checked) {
      try {
        const parsed = parseFnScript(elCodeTextarea.value);
        if (parsed && parsed.nodes.length > 0) {
          appState.model = parsed;
          renderAll(false);
          localStorage.setItem(STORAGE_KEY, elCodeTextarea.value);
          return;
        }
      } catch (_) {
        // Fall through to update diagnostics & disable save buttons
      }
    }
    renderDiagnostics();
  });

  document.getElementById("btnApplyCode").addEventListener("click", () => {
    const syntaxDiags = typeof validateFnTextSyntax === "function" ? validateFnTextSyntax(elCodeTextarea.value) : [];
    if (syntaxDiags.some((d) => d.severity === "error")) {
      renderDiagnostics();
      switchCodeTab("diagnostics");
      showToast("Cannot sync: code has syntax error(s). Check Diagnostics.");
      return;
    }
    const parsed = parseFnScript(elCodeTextarea.value);
    appState.model = parsed;
    renderAll(true);
    showToast("Synced .fn code to visual editor");
  });

  document.getElementById("btnCopyCode").addEventListener("click", async () => {
    await navigator.clipboard.writeText(elCodeTextarea.value);
    showToast("Copied .fn script to clipboard");
  });
}

function switchCodeTab(tabName) {
  document.querySelectorAll(".code-tab").forEach((t) => {
    t.classList.toggle("active", t.dataset.codeTab === tabName);
  });
  document.getElementById("tabCodeContent").classList.toggle("active", tabName === "code");
  document.getElementById("tabDiagnosticsContent").classList.toggle("active", tabName === "diagnostics");
}

function updateCodeLineNumbers() {
  const lines = elCodeTextarea.value.split("\n").length;
  const nums = [];
  for (let i = 1; i <= lines; i++) nums.push(i);
  elLineNumbers.textContent = nums.join("\n");
  elCodeStats.textContent = `${lines} lines · ${elCodeTextarea.value.length} chars`;
}

// Quick Variable Modal
function bindQuickVarModalEvents() {
  const modal = document.getElementById("quickVarModal");
  const close = () => modal.classList.add("hidden");

  document.getElementById("btnCloseQuickVar").addEventListener("click", close);
  document.getElementById("btnCancelQuickVar").addEventListener("click", close);

  document.getElementById("qvType").addEventListener("change", (e) => {
    const gSel = document.getElementById("qvGlobal");
    const vInp = document.getElementById("qvValue");
    const arrRow = document.getElementById("qvArrayPickerRow");
    const arrSel = document.getElementById("qvArrayVarSelect");
    const node = appState.model.nodes[appState.selectedNodeIdx];

    if (e.target.value === "array") {
      arrRow.classList.remove("hidden");
      if (node && arrSel) {
        arrSel.innerHTML = buildScriptVarOptionsHtml(node);
      }
    } else {
      arrRow.classList.add("hidden");
    }

    if (e.target.value === "fun") {
      gSel.value = "false";
      gSel.disabled = true;
      vInp.value = "0";
    } else {
      gSel.disabled = false;
      if (e.target.value === "float") vInp.value = "0.0";
      else if (e.target.value === "array") vInp.value = "[]";
      else if (e.target.value === "string") vInp.value = "";
      else vInp.value = "0";
    }
  });

  const btnQvAddArrayVar = document.getElementById("btnQvAddArrayVar");
  if (btnQvAddArrayVar) {
    btnQvAddArrayVar.addEventListener("click", () => {
      const arrSel = document.getElementById("qvArrayVarSelect");
      const vInp = document.getElementById("qvValue");
      if (!arrSel || !arrSel.value || !vInp) return;
      vInp.value = appendTokenToArrayString(vInp.value, arrSel.value);
    });
  }

  document.getElementById("btnConfirmQuickVar").addEventListener("click", () => {
    const node = appState.model.nodes[appState.selectedNodeIdx];
    if (!node) return;
    let name = document.getElementById("qvName").value.trim();
    if (!name) return;
    if (!name.startsWith("$")) name = "$" + name;
    const type = document.getElementById("qvType").value;
    const isGlobal = type === "fun" ? false : document.getElementById("qvGlobal").value === "true";
    const value = document.getElementById("qvValue").value;

    node.variables.push({ isGlobal, type, name, value });
    close();
    markDirtyAndSync();
    showToast(`Created variable ${name} in Node #${node.index}`);
  });
}

function openQuickVarModal() {
  const modal = document.getElementById("quickVarModal");
  document.getElementById("qvName").value = "$newVar";
  document.getElementById("qvType").value = "int";
  document.getElementById("qvGlobal").value = "false";
  document.getElementById("qvGlobal").disabled = false;
  document.getElementById("qvValue").value = "0";
  document.getElementById("qvArrayPickerRow").classList.add("hidden");
  modal.classList.remove("hidden");
  document.getElementById("qvName").focus();
}

// Start
initApp();
