/**
 * Forge Node (.fn) Parser, Serializer, and Validator
 * Matches AAU/accessories/script_parser.py specification
 */

const SAMPLE_FN_SCRIPT = `1("Emiter", "1f", [SHUNT]){

    <meta>
        slaves = [2, 3, 4, 5]
    </meta>

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

2("Receiver", "2f", [BRIDGE]){

    <meta>
        master = 1
        slaves = [3, 4, 5]
    </meta>

    <variables>
        global int     $e1 =           10
        global float   $res01 =        0.0
        global int     $c1 =           1

        array   $listOfSwitchesCopy = [$c1, $3$c1, $4$c1, $6$c1, $7$c1, $7$c2, $7$c3, $7$c4]

        int $switchEp = 300
        int $time = 0
        int $lastIndex = 0
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

3("First slave", "3f", [SLAVE]){

    <meta>
        master = 2
    </meta>

    <variables>
        global      int         $e1 =               10
        global      float       $res01 =            0.0
        global      float       $res02 =            0.0
        global      int         $c1 =               0
        global      int         $c2 =               0
                    int         $f1 =               0
                    int         $f2 =               0
                    string      $t1 =               "3"
                    string      $ft1 =              ""
                    string      $start =            "19:20"
                    string      $end =              "6:00"
                    fun         $lightsON =         0
                    int         $opop =             0
                    array       $ha =               [$c1, $c2]
                    int         $haha =             4
                    int         $bo =               0
                    array       $listOfSwitches =   [$c1, $c2]
                    int         $switchEp =         3
                    int         $time =             0
                    int         $lastIndex =        0
                    array       $lv =               [0, 1, 2]
                    array       $ln =               ["off", "cooling", "heating"]
                    int         $l1 =               1
                    string      $lf1 =              "1"
    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
    </sensors>

    <elements>
        switch("light0", 0, $c1, $f1)
        switch("light1", 1, $c2, $f2)
        trigger("TempValue", 0, $t1, $ft1)
        list("lista", 1, $lv, $ln, $l1, $lf1)
    </elements>

    <programs>
        valCondition(
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
        serial("entire row", $listOfSwitches, $switchEp, $time, $lastIndex);
    </programs>

}

4("Free", "4f", [MASTER]){

    <meta>
        slaves = [2, 3, 4, 5]
    </meta>

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

}`;

function extractTagContent(block, tagName) {
  const openTag = `<${tagName}>`;
  const closeTag = `</${tagName}>`;
  const b = block.indexOf(openTag);
  const e = block.indexOf(closeTag);
  if (b === -1 || e === -1 || e < b) return null;
  return block.slice(b + openTag.length, e);
}

function cleanLines(text) {
  if (!text) return [];
  return text
    .split(/\r?\n/)
    .map((l) => l.trim().replace(/\s+/g, " "))
    .filter((l) => l.length > 0);
}

function parseVariableLine(line) {
  // e.g. global int $e1 = 10 or array $ha = [$c1, $c2] or string $ft1 = ""
  const eqIdx = line.indexOf("=");
  if (eqIdx === -1) return null;
  const left = line.slice(0, eqIdx).trim().split(/\s+/);
  let rawRight = line.slice(eqIdx + 1).trim();

  let isGlobal = false;
  let type = "int";
  let name = "$var";

  if (left[0] === "global") {
    isGlobal = true;
    type = left[1] || "int";
    name = left[2] || "$var";
  } else {
    type = left[0] || "int";
    name = left[1] || "$var";
  }

  let value = rawRight;
  if (type === "string") {
    if (value.startsWith('"') && value.endsWith('"') && value.length >= 2) {
      value = value.slice(1, -1);
    }
  } else if (type === "array") {
    const bIdx = rawRight.indexOf("[");
    const eIdx = rawRight.lastIndexOf("]");
    if (bIdx !== -1 && eIdx !== -1 && eIdx > bIdx) {
      value = rawRight.slice(bIdx, eIdx + 1);
    }
  }

  return { isGlobal, type, name, value };
}

function parseSensorLine(line) {
  // sensor("temp", 0, $e1, $res01)
  const clean = line.replace(/;$/, "").trim();
  const match = clean.match(/^sensor\s*\(\s*"([^"]*)"\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\s*\)/);
  if (!match) return null;
  return {
    name: match[1].trim(),
    index: parseInt(match[2].trim(), 10) || 0,
    epoch: match[3].trim(),
    readings: match[4].trim(),
  };
}

function parseElementLine(line) {
  const clean = line.replace(/;$/, "").trim();
  if (clean.startsWith("switch")) {
    const m = clean.match(/^switch\s*\(\s*"([^"]*)"\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\s*\)/);
    if (!m) return null;
    return {
      elType: "switch",
      name: m[1].trim(),
      index: parseInt(m[2].trim(), 10) || 0,
      value: m[3].trim(),
      force: m[4].trim(),
    };
  } else if (clean.startsWith("trigger")) {
    const m = clean.match(/^trigger\s*\(\s*"([^"]*)"\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\s*\)/);
    if (!m) return null;
    return {
      elType: "trigger",
      name: m[1].trim(),
      index: parseInt(m[2].trim(), 10) || 0,
      value: m[3].trim(),
      force: m[4].trim(),
    };
  } else if (clean.startsWith("list")) {
    const m = clean.match(
      /^list\s*\(\s*"([^"]*)"\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\s*\)/
    );
    if (!m) return null;
    return {
      elType: "list",
      name: m[1].trim(),
      index: parseInt(m[2].trim(), 10) || 0,
      listValues: m[3].trim(),
      listNames: m[4].trim(),
      state: m[5].trim(),
      force: m[6].trim(),
    };
  }
  return null;
}

function parseStatementsBlock(blockText) {
  const lines = blockText
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean);
  const statements = [];
  for (const l of lines) {
    const m = l.match(/^(\$[a-zA-Z0-9_$]+)\s*([<>=]+)\s*(.+)$/);
    if (m) {
      statements.push({
        left: m[1].trim(),
        op: m[2].trim(),
        right: m[3].trim(),
      });
    }
  }
  return statements;
}

function parseProgramsSection(rawProg) {
  if (!rawProg) return [];
  const programs = [];
  const chunks = rawProg
    .split(";")
    .map((c) => c.trim())
    .filter(Boolean);

  for (const chunk of chunks) {
    if (chunk.includes("valCondition")) {
      let funName = "";
      const fnMatch = chunk.match(/^(\$[a-zA-Z0-9_]+)\s*=\s*valCondition/);
      if (fnMatch) {
        funName = fnMatch[1];
      }
      const vcIdx = chunk.indexOf("valCondition(");
      if (vcIdx === -1) continue;
      let inner = chunk.slice(vcIdx + "valCondition(".length).trim();
      if (inner.endsWith(")")) {
        inner = inner.slice(0, -1).trim();
      }
      const q1 = inner.indexOf('"');
      const q2 = inner.indexOf('"', q1 + 1);
      if (q1 === -1 || q2 === -1) continue;
      const name = inner.slice(q1 + 1, q2);
      const afterName = inner.slice(q2 + 1);
      const c1 = afterName.indexOf(",");
      const c2 = afterName.indexOf(",", c1 + 1);
      if (c1 === -1 || c2 === -1) continue;

      const condText = afterName.slice(c1 + 1, c2);
      const resText = afterName.slice(c2 + 1);

      const conditions = parseStatementsBlock(condText);
      const results = parseStatementsBlock(resText);

      programs.push({
        progType: "valCondition",
        funName,
        name,
        conditions: conditions.length ? conditions : [{ left: "$c1", op: "==", right: "1" }],
        results: results.length ? results : [{ left: "$c1", op: "=", right: "1" }],
      });
    } else if (chunk.includes("serial")) {
      let funName = "";
      const fnMatch = chunk.match(/^(\$[a-zA-Z0-9_]+)\s*=\s*serial/);
      if (fnMatch) {
        funName = fnMatch[1];
      }
      const sIdx = chunk.indexOf("serial(");
      if (sIdx === -1) continue;
      let inner = chunk.slice(sIdx + "serial(".length).trim();
      if (inner.endsWith(")")) {
        inner = inner.slice(0, -1).trim();
      }
      const parts = inner.split(",").map((p) => p.trim());
      if (parts.length >= 5) {
        const rawName = parts[0].replace(/^"|"$/g, "");
        programs.push({
          progType: "serial",
          funName,
          name: rawName,
          switches: parts[1],
          epoch: parts[2],
          time: parts[3],
          lastIndex: parts[4],
        });
      }
    } else if (chunk.includes("timeLim")) {
      const tIdx = chunk.indexOf("timeLim(");
      if (tIdx === -1) continue;
      let inner = chunk.slice(tIdx + "timeLim(".length).trim();
      if (inner.endsWith(")")) {
        inner = inner.slice(0, -1).trim();
      }
      const parts = inner.split(",").map((p) => p.trim());
      if (parts.length >= 3) {
        programs.push({
          progType: "timeLim",
          functionVar: parts[0],
          startVar: parts[1],
          endVar: parts[2],
          onProgName: "",
          offProgName: "",
          targets: [],
        });
      }
    }
  }

  hydrateTimeLimTargetsFromPrograms(programs);
  return programs;
}

function findLinkedValCondition(programs, funVar, targetStateStr) {
  if (!funVar) return null;
  return (
    programs.find(
      (p) =>
        p.progType === "valCondition" &&
        Array.isArray(p.conditions) &&
        p.conditions.length === 1 &&
        p.conditions[0].left === funVar &&
        p.conditions[0].op === "==" &&
        String(p.conditions[0].right).trim() === String(targetStateStr)
    ) || null
  );
}

function hydrateTimeLimTargetsFromPrograms(programs) {
  for (const tl of programs) {
    if (tl.progType !== "timeLim" || !tl.functionVar) continue;
    const onVc = findLinkedValCondition(programs, tl.functionVar, "1");
    const offVc = findLinkedValCondition(programs, tl.functionVar, "0");

    const cleanFun = tl.functionVar.replace(/^\$/, "") || "timeLim";
    tl.onProgName = onVc ? onVc.name : `${cleanFun} on`;
    tl.offProgName = offVc ? offVc.name : `${cleanFun} off`;

    const targetMap = new Map();
    if (onVc && Array.isArray(onVc.results)) {
      for (const r of onVc.results) {
        targetMap.set(r.left, {
          variable: r.left,
          onValue: r.right ?? "1",
          offValue: "0",
        });
      }
    }
    if (offVc && Array.isArray(offVc.results)) {
      for (const r of offVc.results) {
        if (targetMap.has(r.left)) {
          targetMap.get(r.left).offValue = r.right ?? "0";
        } else {
          targetMap.set(r.left, {
            variable: r.left,
            onValue: "1",
            offValue: r.right ?? "0",
          });
        }
      }
    }
    tl.targets = Array.from(targetMap.values());
  }
}

function syncTimeLimValConditions(node, tlProg, oldFunVar = null) {
  if (!node || !tlProg || tlProg.progType !== "timeLim") return;
  const searchFunVar = oldFunVar || tlProg.functionVar;
  const currentFunVar = tlProg.functionVar || "$lightsON";
  const cleanFun = currentFunVar.replace(/^\$/, "") || "timeLim";

  if (!tlProg.onProgName) tlProg.onProgName = `${cleanFun} on`;
  if (!tlProg.offProgName) tlProg.offProgName = `${cleanFun} off`;

  let onVc =
    findLinkedValCondition(node.programs, searchFunVar, "1") ||
    findLinkedValCondition(node.programs, currentFunVar, "1");
  let offVc =
    findLinkedValCondition(node.programs, searchFunVar, "0") ||
    findLinkedValCondition(node.programs, currentFunVar, "0");

  const validTargets = (tlProg.targets || []).filter((t) => t.variable && t.variable.trim() !== "");

  if (validTargets.length === 0) {
    if (onVc) {
      const idx = node.programs.indexOf(onVc);
      if (idx !== -1) node.programs.splice(idx, 1);
    }
    if (offVc) {
      const idx = node.programs.indexOf(offVc);
      if (idx !== -1) node.programs.splice(idx, 1);
    }
    return;
  }

  const onResults = validTargets.map((t) => ({
    left: t.variable.trim(),
    op: "=",
    right: String(t.onValue ?? "1").trim() || "1",
  }));

  const offResults = validTargets.map((t) => ({
    left: t.variable.trim(),
    op: "=",
    right: String(t.offValue ?? "0").trim() || "0",
  }));

  if (onVc) {
    onVc.name = tlProg.onProgName;
    onVc.conditions = [{ left: currentFunVar, op: "==", right: "1" }];
    onVc.results = onResults;
  } else {
    onVc = {
      progType: "valCondition",
      funName: "",
      name: tlProg.onProgName,
      conditions: [{ left: currentFunVar, op: "==", right: "1" }],
      results: onResults,
    };
    const tlIdx = node.programs.indexOf(tlProg);
    if (tlIdx !== -1) {
      node.programs.splice(tlIdx, 0, onVc);
    } else {
      node.programs.push(onVc);
    }
  }

  if (offVc) {
    offVc.name = tlProg.offProgName;
    offVc.conditions = [{ left: currentFunVar, op: "==", right: "0" }];
    offVc.results = offResults;
  } else {
    offVc = {
      progType: "valCondition",
      funName: "",
      name: tlProg.offProgName,
      conditions: [{ left: currentFunVar, op: "==", right: "0" }],
      results: offResults,
    };
    const tlIdx = node.programs.indexOf(tlProg);
    if (tlIdx !== -1) {
      node.programs.splice(tlIdx, 0, offVc);
    } else {
      node.programs.push(offVc);
    }
  }
}

function parseFnScript(text) {
  const units = text.split("}");
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
    const headerPart = unit.slice(0, closeParen);

    const idxMatch = headerPart.match(/[0-9]+/);
    const unitIndex = idxMatch ? parseInt(idxMatch[0], 10) : nodes.length + 1;

    const quoted = headerPart.match(/"[^"]*"/g) || [];
    const unitName = quoted[0] ? quoted[0].slice(1, -1) : `Node ${unitIndex}`;
    const unitTopic = quoted[1] ? quoted[1].slice(1, -1) : `${unitIndex}f`;

    const typeMatch = headerPart.match(/(MASTER|SLAVE|SHUNT|BRIDGE)/);
    const unitType = typeMatch ? typeMatch[1] : "MASTER";

    const metaRaw = extractTagContent(unit, "meta");
    let master = null;
    let slaves = [];
    const hasMetaBlock = metaRaw !== null;

    if (metaRaw !== null) {
      const metaLines = metaRaw
        .split(/\r?\n/)
        .map((l) => l.trim())
        .filter(Boolean);
      for (const mLine of metaLines) {
        if (mLine.startsWith("master")) {
          const nums = mLine.match(/[0-9]+/g);
          if (nums && nums.length > 0) master = parseInt(nums[0], 10);
        } else if (mLine.startsWith("slaves")) {
          const nums = mLine.match(/[0-9]+/g);
          if (nums) slaves = nums.map((n) => parseInt(n, 10));
        }
      }
    }

    const varsRaw = extractTagContent(unit, "variables") || "";
    const variables = cleanLines(varsRaw)
      .map(parseVariableLine)
      .filter(Boolean);

    const sensorsRaw = extractTagContent(unit, "sensors") || "";
    const sensors = cleanLines(sensorsRaw)
      .map(parseSensorLine)
      .filter(Boolean);

    const elementsRaw = extractTagContent(unit, "elements") || "";
    const elements = cleanLines(elementsRaw)
      .map(parseElementLine)
      .filter(Boolean);

    const programsRaw = extractTagContent(unit, "programs") || "";
    const programs = parseProgramsSection(programsRaw);

    nodes.push({
      index: unitIndex,
      name: unitName,
      topic: unitTopic,
      type: unitType,
      hasMetaBlock: hasMetaBlock || unitType !== "MASTER",
      meta: {
        master,
        slaves,
      },
      variables,
      sensors,
      elements,
      programs,
    });
  }

  return { nodes };
}

function serializeFnScript(model) {
  const blocks = [];

  for (const node of model.nodes) {
    let out = `${node.index}("${node.name}", "${node.topic}", [${node.type}]){\n\n`;

    // <meta> block: required for SHUNT, BRIDGE, SLAVE by script_parser.py, or if master/slaves set
    const needsMeta =
      node.type === "SHUNT" ||
      node.type === "BRIDGE" ||
      node.type === "SLAVE" ||
      node.hasMetaBlock ||
      node.meta.master !== null ||
      (node.meta.slaves && node.meta.slaves.length > 0);

    if (needsMeta) {
      out += `    <meta>\n`;
      if (node.meta.master !== null && node.meta.master !== "" && !isNaN(node.meta.master)) {
        out += `        master = ${node.meta.master}\n`;
      }
      if (node.meta.slaves && node.meta.slaves.length > 0) {
        out += `        slaves = [${node.meta.slaves.join(", ")}]\n`;
      }
      out += `    </meta>\n\n`;
    }

    // <variables>
    out += `    <variables>\n`;
    for (const v of node.variables) {
      const scopeStr = v.isGlobal && v.type !== "fun" ? "global " : "       ";
      const typeStr = v.type.padEnd(8, " ");
      const nameStr = (v.name.startsWith("$") ? v.name : `$${v.name}`).padEnd(18, " ");
      let formattedVal = String(v.value ?? "").trim();

      if (v.type === "string") {
        const stripped = formattedVal.replace(/^"|"$/g, "");
        formattedVal = `"${stripped}"`;
      } else if (v.type === "array") {
        if (!formattedVal.startsWith("[")) formattedVal = `[${formattedVal}`;
        if (!formattedVal.endsWith("]")) formattedVal = `${formattedVal}]`;
      } else if (v.type === "float") {
        if (formattedVal !== "" && !formattedVal.includes(".")) {
          formattedVal = `${formattedVal}.0`;
        }
      }

      out += `        ${scopeStr}${typeStr}${nameStr}= ${formattedVal}\n`;
    }
    out += `    </variables>\n\n`;

    // <sensors>
    out += `    <sensors>\n`;
    for (const s of node.sensors) {
      const ep = !s.epoch || s.epoch.trim() === "" ? "NULL" : s.epoch.trim();
      out += `        sensor("${s.name}", ${s.index}, ${ep}, ${s.readings})\n`;
    }
    out += `    </sensors>\n\n`;

    // <elements>
    out += `    <elements>\n`;
    for (const el of node.elements) {
      if (el.elType === "switch") {
        out += `        switch("${el.name}", ${el.index}, ${el.value}, ${el.force})\n`;
      } else if (el.elType === "trigger") {
        out += `        trigger("${el.name}", ${el.index}, ${el.value}, ${el.force})\n`;
      } else if (el.elType === "list") {
        out += `        list("${el.name}", ${el.index}, ${el.listValues}, ${el.listNames}, ${el.state}, ${el.force})\n`;
      }
    }
    out += `    </elements>\n\n`;

    // <programs>
    out += `    <programs>\n`;
    for (const p of node.programs) {
      if (p.progType === "valCondition") {
        const prefix = p.funName && p.funName.trim() !== "" ? `${p.funName.trim()} = ` : "";
        out += `        ${prefix}valCondition(\n`;
        out += `            "${p.name}",\n`;
        const condLines = (p.conditions || []).map(
          (c) => `            ${c.left} ${c.op} ${c.right}`
        );
        out += condLines.join("\n") + ",\n";
        const resLines = (p.results || []).map(
          (r) => `            ${r.left} ${r.op} ${r.right}`
        );
        out += resLines.join("\n") + "\n";
        out += `        );\n`;
      } else if (p.progType === "serial") {
        const prefix = p.funName && p.funName.trim() !== "" ? `${p.funName.trim()} = ` : "";
        out += `        ${prefix}serial("${p.name}", ${p.switches}, ${p.epoch}, ${p.time}, ${p.lastIndex});\n`;
      } else if (p.progType === "timeLim") {
        out += `        timeLim(${p.functionVar}, ${p.startVar}, ${p.endVar});\n`;
      }
    }
    out += `    </programs>\n\n`;
    out += `}`;
    blocks.push(out);
  }

  return blocks.join("\n\n");
}

function validateFnModel(model) {
  const diagnostics = [];
  const seenNodeIds = new Set();
  const nodeMap = new Map();

  for (const n of model.nodes) {
    nodeMap.set(Number(n.index), n);
  }

  for (const node of model.nodes) {
    const nIdx = Number(node.index);
    if (seenNodeIds.has(nIdx)) {
      diagnostics.push({
        severity: "error",
        nodeIndex: nIdx,
        section: "Header",
        message: `Duplicate node index ${nIdx}. Each node must have a unique integer index.`,
      });
    }
    seenNodeIds.add(nIdx);

    if (!node.name || !/^[0-9a-zA-Z\s.,\-_]+$/.test(node.name)) {
      diagnostics.push({
        severity: "error",
        nodeIndex: nIdx,
        section: "Header",
        message: `Node name "${node.name}" contains characters not matched by AAU script_parser.py.`,
      });
    }

    if (!node.topic || /\s/.test(node.topic)) {
      diagnostics.push({
        severity: "error",
        nodeIndex: nIdx,
        section: "Header",
        message: `Node machine topic "${node.topic}" cannot be empty or contain spaces.`,
      });
    }

    // Meta checks
    if ((node.type === "BRIDGE" || node.type === "SLAVE") && !node.meta.master) {
      diagnostics.push({
        severity: "warn",
        nodeIndex: nIdx,
        section: "Meta",
        message: `${node.type} node usually specifies a "master = <id>" inside <meta>.`,
      });
    }
    if ((node.type === "SHUNT" || node.type === "BRIDGE") && (!node.meta.slaves || node.meta.slaves.length === 0)) {
      diagnostics.push({
        severity: "warn",
        nodeIndex: nIdx,
        section: "Meta",
        message: `${node.type} node usually specifies "slaves = [...]" inside <meta>.`,
      });
    }

    // Variables check
    const localVars = new Map();
    for (const v of node.variables) {
      if (!v.name.startsWith("$") || /\s/.test(v.name)) {
        diagnostics.push({
          severity: "error",
          nodeIndex: nIdx,
          section: "Variables",
          message: `Variable "${v.name}" must start with "$" and contain no spaces.`,
        });
      }
      if (v.type === "fun" && v.isGlobal) {
        diagnostics.push({
          severity: "error",
          nodeIndex: nIdx,
          section: "Variables",
          message: `Variable "${v.name}" is of type "fun" and cannot be marked global.`,
        });
      }
      if (v.type === "string" && /\s/.test(String(v.value))) {
        diagnostics.push({
          severity: "warn",
          nodeIndex: nIdx,
          section: "Variables",
          message: `String variable "${v.name}" contains spaces ("${v.value}"). Note: AAU parseVariables splits lines by space.`,
        });
      }
      if (localVars.has(v.name)) {
        diagnostics.push({
          severity: "warn",
          nodeIndex: nIdx,
          section: "Variables",
          message: `Duplicate variable "${v.name}" in Node ${nIdx}.`,
        });
      }
      localVars.set(v.name, v);
    }

    const checkVarExists = (varRef, section, contextDesc, allowNull = false) => {
      if (!varRef) return;
      if (allowNull && varRef === "NULL") return;
      if (!varRef.startsWith("$")) return;

      // Check cross-node global reference e.g. $3$c1
      const crossMatch = varRef.match(/^\$([0-9]+)(\$[a-zA-Z0-9_]+)$/);
      if (crossMatch) {
        const targetNodeId = parseInt(crossMatch[1], 10);
        const targetVarName = crossMatch[2];
        const targetNode = nodeMap.get(targetNodeId);
        if (!targetNode) {
          diagnostics.push({
            severity: "warn",
            nodeIndex: nIdx,
            section,
            message: `${contextDesc} references "${varRef}", but Node ${targetNodeId} is not defined in this file.`,
          });
        } else {
          const found = targetNode.variables.find((tv) => tv.name === targetVarName && tv.isGlobal);
          if (!found) {
            diagnostics.push({
              severity: "error",
              nodeIndex: nIdx,
              section,
              message: `${contextDesc} references "${varRef}", but Node ${targetNodeId} has no global variable "${targetVarName}".`,
            });
          }
        }
        return;
      }

      if (!localVars.has(varRef)) {
        diagnostics.push({
          severity: "error",
          nodeIndex: nIdx,
          section,
          message: `${contextDesc} references undefined variable "${varRef}".`,
        });
      }
    };

    // Check array variable references
    for (const v of node.variables) {
      if (v.type === "array") {
        const refs = String(v.value).match(/\$[0-9a-zA-Z_$]+/g) || [];
        for (const r of refs) {
          checkVarExists(r, "Variables", `Array "${v.name}"`);
        }
      }
    }

    // Check sensors
    for (const s of node.sensors) {
      checkVarExists(s.epoch, "Sensors", `Sensor "${s.name}" epoch`, true);
      checkVarExists(s.readings, "Sensors", `Sensor "${s.name}" readings`, false);
    }

    // Check elements
    for (const el of node.elements) {
      if (el.elType === "switch" || el.elType === "trigger") {
        checkVarExists(el.value, "Elements", `${el.elType} "${el.name}" value`);
        checkVarExists(el.force, "Elements", `${el.elType} "${el.name}" force`);
      } else if (el.elType === "list") {
        checkVarExists(el.listValues, "Elements", `list "${el.name}" values`);
        checkVarExists(el.listNames, "Elements", `list "${el.name}" names`);
        checkVarExists(el.state, "Elements", `list "${el.name}" state`);
        checkVarExists(el.force, "Elements", `list "${el.name}" force`);
      }
    }

    // Check programs
    for (const p of node.programs) {
      if (p.progType === "valCondition") {
        if (p.funName) checkVarExists(p.funName, "Programs", `valCondition "${p.name}" function var`);
        for (const c of p.conditions || []) {
          checkVarExists(c.left, "Programs", `valCondition "${p.name}" condition`);
          if (String(c.right).startsWith("$")) {
            checkVarExists(c.right, "Programs", `valCondition "${p.name}" condition`);
          }
        }
        for (const r of p.results || []) {
          checkVarExists(r.left, "Programs", `valCondition "${p.name}" result`);
          if (String(r.right).startsWith("$")) {
            checkVarExists(r.right, "Programs", `valCondition "${p.name}" result`);
          }
        }
      } else if (p.progType === "serial") {
        if (p.funName) checkVarExists(p.funName, "Programs", `serial "${p.name}" function var`);
        checkVarExists(p.switches, "Programs", `serial "${p.name}" switches`);
        if (String(p.epoch).startsWith("$")) checkVarExists(p.epoch, "Programs", `serial "${p.name}" epoch`);
        if (String(p.time).startsWith("$")) checkVarExists(p.time, "Programs", `serial "${p.name}" time`);
        if (String(p.lastIndex).startsWith("$")) checkVarExists(p.lastIndex, "Programs", `serial "${p.name}" lastIndex`);
      } else if (p.progType === "timeLim") {
        checkVarExists(p.functionVar, "Programs", `timeLim function`);
        checkVarExists(p.startVar, "Programs", `timeLim start`);
        checkVarExists(p.endVar, "Programs", `timeLim end`);
      }
    }
  }

  return diagnostics;
}
