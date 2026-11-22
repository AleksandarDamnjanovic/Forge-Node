package com.aleksandar_damnjanovic.forge_node;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity 1: .fn Script Studio (Mobile Filtered View)
 * - Loads with the active default script in Spring app (/api/working-fn)
 * - Cannot load local files or create a brand new script file
 * - Can change the active default script from AAU/script/ using a simpler native picker (/api/working-fn/default)
 * - Shows ONLY the middle graphic UI elements of the node + a simple bar to choose nodes, create new nodes, and save to the active default script
 */
public class ScriptStudioActivity extends BaseForgeActivity {

    private LinearLayout controlBar;
    private TextView tvActiveDefaultScript;
    private TextView btnChangeDefaultScript;
    private TextView btnSaveActiveScript;
    private WebView webView;
    private String currentDefaultScript = "script.fn";
    private final android.os.Handler studioPollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable studioPollRunnable = new Runnable() {
        @Override
        public void run() {
            fetchDefaultScriptName();
            if (webView != null) {
                webView.evaluateJavascript("if(window.__reloadDefaultScriptInStudio) window.__reloadDefaultScriptInStudio(true);", null);
            }
            studioPollHandler.postDelayed(this, 4000);
        }
    };

    // Injected CSS & JS that transforms /script-editor/index.html into a clean mobile node editor:
    // - Hides topbar, left desktop sidebar/topology, and right code/diagnostics panel
    // - Adds a compact mobile Node Selector dropdown + "+ New Node" button right above the middle visual editor workspace
    private static final String STUDIO_MOBILE_INJECTION_JS =
            "(function(){" +
            "  if(!document.getElementById('fn-android-studio-style')){" +
            "    const st = document.createElement('style');" +
            "    st.id = 'fn-android-studio-style';" +
            "    st.textContent = `" +
            "      header.topbar, aside.sidebar, aside.code-panel { display: none !important; }" +
            "      main.studio-layout { display: flex !important; flex-direction: column !important; height: 100vh !important; width: 100vw !important; overflow: hidden !important; }" +
            "      section.editor-workspace { flex: 1 !important; width: 100% !important; max-width: 100% !important; overflow-y: auto !important; padding: 10px !important; }" +
            "      .header-grid, .meta-grid { grid-template-columns: 1fr !important; }" +
            "      .node-toolbar { flex-wrap: wrap !important; gap: 8px !important; }" +
            "      .section-tabs { overflow-x: auto !important; max-width: 100% !important; }" +
            "      #fnMobileNodeBar { display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: var(--bg-panel); border-bottom: 1px solid var(--border); flex-shrink: 0; }" +
            "      #fnMobileNodeSelect { flex: 1; }" +
            "    `;" +
            "    document.head.appendChild(st);" +
            "  }" +
            "  const layout = document.querySelector('main.studio-layout');" +
            "  if(layout && !document.getElementById('fnMobileNodeBar')){" +
            "    const bar = document.createElement('div');" +
            "    bar.id = 'fnMobileNodeBar';" +
            "    bar.innerHTML = `" +
            "      <span style='font-size:11.5px;font-weight:700;color:var(--text-muted);white-space:nowrap;'>Node:</span>" +
            "      <select id='fnMobileNodeSelect' class='select select-sm input-mono'></select>" +
            "      <button type='button' id='fnMobileAddNodeBtn' class='btn btn-sm btn-primary' style='white-space:nowrap;'>＋ New Node</button>" +
            "    `;" +
            "    layout.insertBefore(bar, layout.firstChild);" +
            "    const sel = document.getElementById('fnMobileNodeSelect');" +
            "    sel.addEventListener('change', function(){" +
            "      if(typeof appState !== 'undefined'){" +
            "        appState.selectedNodeIdx = parseInt(this.value, 10) || 0;" +
            "        if(typeof renderAll === 'function') renderAll(false);" +
            "      }" +
            "    });" +
            "    document.getElementById('fnMobileAddNodeBtn').addEventListener('click', function(){" +
            "      if(typeof addNewNode === 'function') addNewNode();" +
            "    });" +
            "  }" +
            "  window.__syncMobileNodeDropdown = function(){" +
            "    const sel = document.getElementById('fnMobileNodeSelect');" +
            "    if(!sel || typeof appState === 'undefined' || !appState.model) return;" +
            "    const nodes = appState.model.nodes || [];" +
            "    sel.innerHTML = nodes.map((n, i) => `<option value='${i}' ${i === appState.selectedNodeIdx ? 'selected' : ''}>#${n.index} — ${n.name} (${n.topic}) [${n.type}]</option>`).join('');" +
            "  };" +
            "  if(!window.__fnRenderHooked && typeof window.renderAll === 'function'){" +
            "    window.__fnRenderHooked = true;" +
            "    const origRenderAll = window.renderAll;" +
            "    window.renderAll = function(updateCode){" +
            "      origRenderAll(updateCode);" +
            "      if(window.__syncMobileNodeDropdown) window.__syncMobileNodeDropdown();" +
            "    };" +
            "  }" +
            "  window.__reloadDefaultScriptInStudio = async function(onlyIfUnmodified){" +
            "    try {" +
            "      if(onlyIfUnmodified && typeof appState !== 'undefined' && appState.isDirty) return;" +
            "      const r = await fetch('/api/working-fn', {cache:'no-store'});" +
            "      if(!r.ok) return;" +
            "      const d = await r.json();" +
            "      const name = (d.defaultScript && d.defaultScript.trim()) ? d.defaultScript.trim() : ((d.path || 'script.fn').split('/').pop());" +
            "      if(d.content && typeof parseFnScript === 'function'){" +
            "        const curCode = typeof serializeFnScript === 'function' ? serializeFnScript(appState.model) : '';" +
            "        if(onlyIfUnmodified && appState.fileName === name && curCode.trim() === d.content.trim()) return;" +
            "        const prevIdx = appState.selectedNodeIdx || 0;" +
            "        appState.defaultScript = name;" +
            "        appState.fileName = name;" +
            "        appState.model = parseFnScript(d.content);" +
            "        appState.selectedNodeIdx = Math.min(prevIdx, Math.max(0, appState.model.nodes.length - 1));" +
            "        appState.isDirty = false;" +
            "        renderAll(true);" +
            "      }" +
            "    } catch(e){}" +
            "  };" +
            "  window.__saveActiveDefaultScriptFromAndroid = async function(){" +
            "    try {" +
            "      const fileName = (appState.defaultScript && appState.defaultScript.trim()) ? appState.defaultScript.trim() : (appState.fileName || 'script.fn');" +
            "      const code = serializeFnScript(appState.model);" +
            "      const res = await fetch('/api/files/scripts/save', {" +
            "        method: 'POST'," +
            "        headers: {'Content-Type': 'application/json'}," +
            "        body: JSON.stringify({ fileName: fileName, content: code })" +
            "      });" +
            "      const data = await res.json();" +
            "      if(res.ok && data.ok){" +
            "        await fetch('/api/working-fn/default', {" +
            "          method: 'POST'," +
            "          headers: {'Content-Type': 'application/json'}," +
            "          body: JSON.stringify({ fileName: fileName })" +
            "        });" +
            "        appState.isDirty = false;" +
            "        renderAll(false);" +
            "        if(typeof showToast === 'function') showToast('Saved ' + fileName);" +
            "      } else {" +
            "        if(typeof showToast === 'function') showToast(data.error || 'Failed to save script');" +
            "      }" +
            "    } catch(e){" +
            "      if(typeof showToast === 'function') showToast(e.message || 'Save error');" +
            "    }" +
            "  };" +
            "  window.__reloadDefaultScriptInStudio(false);" +
            "  if(!window.__fnStudioPollTimer){" +
            "    window.__fnStudioPollTimer = setInterval(function(){" +
            "      if(window.ForgeNodeTheme && window.ForgeNodeTheme.syncThemeFromServer) window.ForgeNodeTheme.syncThemeFromServer();" +
            "    }, 4000);" +
            "  }" +
            "})();";

    @Override
    protected String getScreenTitle() {
        return "📝 .fn Script Studio";
    }

    @Override
    protected String getScreenSubtitle() {
        return "Visual Node Editor · Active Default Script";
    }

    @Override
    protected int getNavTabIndex() {
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;

        float d = getResources().getDisplayMetrics().density;
        LinearLayout content = getContentContainer();

        // Simplified Default Script Bar
        controlBar = new LinearLayout(this);
        controlBar.setOrientation(LinearLayout.HORIZONTAL);
        controlBar.setGravity(Gravity.CENTER_VERTICAL);
        controlBar.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));

        tvActiveDefaultScript = new TextView(this);
        tvActiveDefaultScript.setText("Default: loading…");
        tvActiveDefaultScript.setTextSize(12f);
        tvActiveDefaultScript.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvActiveDefaultScript.setLayoutParams(lblLp);
        controlBar.addView(tvActiveDefaultScript);

        btnChangeDefaultScript = new TextView(this);
        btnChangeDefaultScript.setText("🗄 Change Default");
        btnChangeDefaultScript.setTextSize(11.5f);
        btnChangeDefaultScript.setTypeface(Typeface.DEFAULT_BOLD);
        btnChangeDefaultScript.setPadding((int) (10 * d), (int) (6 * d), (int) (10 * d), (int) (6 * d));
        LinearLayout.LayoutParams chgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chgLp.setMargins((int) (6 * d), 0, (int) (6 * d), 0);
        btnChangeDefaultScript.setLayoutParams(chgLp);
        btnChangeDefaultScript.setOnClickListener(v -> openChangeDefaultScriptDialog());
        controlBar.addView(btnChangeDefaultScript);

        btnSaveActiveScript = new TextView(this);
        btnSaveActiveScript.setText("💾 Save");
        btnSaveActiveScript.setTextSize(11.5f);
        btnSaveActiveScript.setTypeface(Typeface.DEFAULT_BOLD);
        btnSaveActiveScript.setPadding((int) (12 * d), (int) (6 * d), (int) (12 * d), (int) (6 * d));
        btnSaveActiveScript.setOnClickListener(v -> {
            if (webView != null) {
                webView.evaluateJavascript("if(window.__saveActiveDefaultScriptFromAndroid) window.__saveActiveDefaultScriptFromAndroid();", null);
            }
        });
        controlBar.addView(btnSaveActiveScript);

        content.addView(controlBar);

        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        content.addView(webView);

        applyNativeTheme();
        fetchDefaultScriptName();
        configureAuthenticatedWebView(webView, "/script-editor/index.html", STUDIO_MOBILE_INJECTION_JS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        studioPollHandler.removeCallbacks(studioPollRunnable);
        studioPollHandler.post(studioPollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        studioPollHandler.removeCallbacks(studioPollRunnable);
    }

    @Override
    protected void onThemeUpdated(ThemeEngine.Palette p, String uiStyle) {
        if (controlBar == null) return;
        float btnRadius = ThemeEngine.getCornerRadiusDp(uiStyle, true);
        controlBar.setBackgroundColor(p.bgCard);
        tvActiveDefaultScript.setTextColor(p.textMain);
        ThemeEngine.styleRoundedBox(btnChangeDefaultScript, p.bgHover, p.border, btnRadius, 2);
        btnChangeDefaultScript.setTextColor(p.textMain);
        ThemeEngine.styleRoundedBox(btnSaveActiveScript, p.primary, p.primary, btnRadius, 0);
        btnSaveActiveScript.setTextColor(0xFFFFFFFF);
    }

    private void fetchDefaultScriptName() {
        final String baseUrl = session.getBaseUrl();
        final String token = session.getToken();
        bgExecutor.submit(() -> {
            ForgeApiClient.ApiResponse res = ForgeApiClient.get(baseUrl, "/api/working-fn", token);
            if (res.isOk()) {
                try {
                    JSONObject obj = new JSONObject(res.body);
                    String def = obj.optString("defaultScript", "").trim();
                    String path = obj.optString("path", "AAU/script/script.fn");
                    if (def.isEmpty()) {
                        int slash = path.lastIndexOf('/');
                        def = slash != -1 ? path.substring(slash + 1) : path;
                    }
                    final String finalDef = def;
                    runOnUiThread(() -> {
                        currentDefaultScript = finalDef;
                        tvActiveDefaultScript.setText("Default: " + finalDef);
                    });
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void openChangeDefaultScriptDialog() {
        final String baseUrl = session.getBaseUrl();
        final String token = session.getToken();
        bgExecutor.submit(() -> {
            ForgeApiClient.ApiResponse listRes = ForgeApiClient.get(baseUrl, "/api/files/scripts/list", token);
            if (!listRes.isOk()) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to list scripts in AAU/script/", Toast.LENGTH_SHORT).show());
                return;
            }
            try {
                JSONObject obj = new JSONObject(listRes.body);
                JSONArray arr = obj.optJSONArray("files");
                List<String> fnFiles = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        String name = arr.optString(i, "");
                        if (name.toLowerCase().endsWith(".fn")) {
                            fnFiles.add(name);
                        }
                    }
                }
                runOnUiThread(() -> {
                    if (fnFiles.isEmpty()) {
                        Toast.makeText(this, "No .fn scripts found in AAU/script/", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] items = new String[fnFiles.size()];
                    int checkedItem = 0;
                    for (int i = 0; i < fnFiles.size(); i++) {
                        String f = fnFiles.get(i);
                        items[i] = f.equals(currentDefaultScript) ? f + " (active default)" : f;
                        if (f.equals(currentDefaultScript)) {
                            checkedItem = i;
                        }
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Select Default .fn Script")
                            .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                                String chosen = fnFiles.get(which);
                                dialog.dismiss();
                                setNewDefaultScript(chosen);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error parsing script list", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setNewDefaultScript(String fileName) {
        final String baseUrl = session.getBaseUrl();
        final String token = session.getToken();
        bgExecutor.submit(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("fileName", fileName);
                ForgeApiClient.ApiResponse res = ForgeApiClient.post(baseUrl, "/api/working-fn/default", body, token);
                runOnUiThread(() -> {
                    if (res.isOk()) {
                        currentDefaultScript = fileName;
                        tvActiveDefaultScript.setText("Default: " + fileName);
                        Toast.makeText(this, "Default script changed to " + fileName, Toast.LENGTH_SHORT).show();
                        if (webView != null) {
                            webView.evaluateJavascript("if(window.__reloadDefaultScriptInStudio) window.__reloadDefaultScriptInStudio();", null);
                        }
                    } else {
                        Toast.makeText(this, "Failed to change default script", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }
}
