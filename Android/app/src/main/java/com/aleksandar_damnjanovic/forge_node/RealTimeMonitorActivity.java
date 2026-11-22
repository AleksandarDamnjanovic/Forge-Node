package com.aleksandar_damnjanovic.forge_node;

import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

/**
 * Activity 2: Real Time Monitor
 * - Shows ONLY the "Forge Nodes Telemetry Dashboard" card and when a node is clicked,
 *   shows all interactive node controls (sensors, switches/triggers/lists force override, variables, admin read)
 *   exactly like the web app.
 * - Hides the top navigation bar, the broker summary card, and the right-side stream panel.
 */
public class RealTimeMonitorActivity extends BaseForgeActivity {

    private WebView webView;

    private static final String MONITOR_MOBILE_INJECTION_JS =
            "(function(){" +
            "  if(!document.getElementById('fn-android-monitor-style')){" +
            "    const st = document.createElement('style');" +
            "    st.id = 'fn-android-monitor-style';" +
            "    st.textContent = `" +
            "      header.topbar, aside#streamPanel, #pageBridgeBanner { display: none !important; }" +
            "      main.studio-layout { display: flex !important; flex-direction: column !important; height: 100vh !important; width: 100vw !important; }" +
            "      section#monitorWorkspace { flex: 1 !important; width: 100% !important; max-width: 100% !important; overflow-y: auto !important; padding: 10px !important; }" +
            "      .nodes-dashboard-grid, .telemetry-grid { grid-template-columns: 1fr !important; }" +
            "      .fn-hide-broker-overview-card { display: none !important; }" +
            "    `;" +
            "    document.head.appendChild(st);" +
            "  }" +
            "  function hideBrokerOverviewCard(){" +
            "    const ws = document.getElementById('monitorWorkspace');" +
            "    if(!ws) return;" +
            "    if(typeof state !== 'undefined' && state.selectedView === 'all'){" +
            "      const cards = ws.querySelectorAll(':scope > .card');" +
            "      if(cards.length >= 2){" +
            "        cards[0].classList.add('fn-hide-broker-overview-card');" +
            "      }" +
            "    }" +
            "  }" +
            "  if(!window.__fnMonitorHooked && typeof window.renderWorkspace === 'function'){" +
            "    window.__fnMonitorHooked = true;" +
            "    const origRW = window.renderWorkspace;" +
            "    window.renderWorkspace = function(pulseTopic){" +
            "      origRW(pulseTopic);" +
            "      hideBrokerOverviewCard();" +
            "    };" +
            "  }" +
            "  hideBrokerOverviewCard();" +
            "  const connModal = document.getElementById('connModal');" +
            "  if(connModal) connModal.classList.add('hidden');" +
            "  window.__ensureAndroidMqttBridgeAndConnect = async function(){" +
            "    try {" +
            "      if(typeof state === 'undefined') return;" +
            "      const stRes = await fetch('/api/bridge/status', {cache:'no-store'});" +
            "      if(stRes.ok){" +
            "        const stData = await stRes.json();" +
            "        if(!stData.running){" +
            "          const port = stData.port || 8765;" +
            "          await fetch('/api/bridge/start', {" +
            "            method: 'POST'," +
            "            headers: {'Content-Type':'application/json'}," +
            "            body: JSON.stringify({port: port})" +
            "          });" +
            "        }" +
            "      }" +
            "      if(typeof refreshBridgeStatus === 'function') await refreshBridgeStatus(true);" +
            "      try {" +
            "        const wfRes = await fetch('/api/working-fn', {cache:'no-store'});" +
            "        if(wfRes.ok){" +
            "          const wfData = await wfRes.json();" +
            "          const newSrc = wfData.path || 'AAU/script/working.fn';" +
            "          if(wfData.content && (state.nodes.length === 0 || state.scriptSource !== newSrc)){" +
            "            if(typeof loadScriptIntoState === 'function') loadScriptIntoState(wfData.content, newSrc);" +
            "          }" +
            "        }" +
            "      } catch(_){}" +
            "      const cfgRes = await fetch('/api/config', {cache:'no-store'});" +
            "      if(cfgRes.ok){" +
            "        const cfgData = await cfgRes.json();" +
            "        if(cfgData.config){" +
            "          const c = cfgData.config;" +
            "          state.conn.configured = true;" +
            "          state.conn.ip = c.ip || state.conn.ip || '127.0.0.1';" +
            "          state.conn.port = Number(c.port) || 8883;" +
            "          state.conn.username = c.username ?? state.conn.username;" +
            "          state.conn.password = c.password ?? state.conn.password;" +
            "          state.conn.caCertPath = c.ca_cert_path || state.conn.caCertPath;" +
            "          state.conn.brokerCertPath = c.broker_cert_path || state.conn.brokerCertPath;" +
            "          state.conn.subscribeAdmin = c.subscribe_admin !== false;" +
            "        }" +
            "        if(!state.conn.password){" +
            "          try {" +
            "            const msRes = await fetch('/api/mqtt/status', {cache:'no-store'});" +
            "            if(msRes.ok){" +
            "              const msData = await msRes.json();" +
            "              if(!msData.remoteMqtt && msData.localConfigDetails){" +
            "                const det = msData.localConfigDetails;" +
            "                state.conn.ip = '127.0.0.1';" +
            "                state.conn.port = Number(det.port) || 8883;" +
            "                const usersRaw = String(det.users || '').replace(/^\\[|\\]$/g, '');" +
            "                for(const part of usersRaw.split(',')){" +
            "                  const eq = part.indexOf('=');" +
            "                  if(eq > 0){" +
            "                    state.conn.username = part.slice(0, eq).trim();" +
            "                    state.conn.password = part.slice(eq + 1).trim();" +
            "                    break;" +
            "                  }" +
            "                }" +
            "              }" +
            "            }" +
            "          } catch(_){}" +
            "        }" +
            "        if(cfgData.connected && (!state.sseSource || state.conn.status !== 'connected')){" +
            "          state.conn.status = 'connected';" +
            "          state.conn.activeTransport = 'TLS TCP (paho-mqtt)';" +
            "          const topics = state.nodes.map(n => n.topic);" +
            "          await fetch('/api/subscribe', {" +
            "            method: 'POST'," +
            "            headers: {'Content-Type':'application/json'}," +
            "            body: JSON.stringify({topics: topics, subscribeAdmin: state.conn.subscribeAdmin})" +
            "          });" +
            "          if(typeof startSseStream === 'function') startSseStream();" +
            "          if(typeof renderAll === 'function') renderAll();" +
            "          return;" +
            "        }" +
            "      }" +
            "      if(state.conn.status !== 'connected' && state.conn.status !== 'connecting'){" +
            "        if(typeof connectToBroker === 'function') await connectToBroker(true);" +
            "        const cm = document.getElementById('connModal');" +
            "        if(cm) cm.classList.add('hidden');" +
            "      }" +
            "    } catch(e){}" +
            "  };" +
            "  setTimeout(window.__ensureAndroidMqttBridgeAndConnect, 200);" +
            "  if(!window.__fnMonitorKeepAliveTimer){" +
            "    window.__fnMonitorKeepAliveTimer = setInterval(function(){" +
            "      if(window.ForgeNodeTheme && window.ForgeNodeTheme.syncThemeFromServer) window.ForgeNodeTheme.syncThemeFromServer();" +
            "      if(typeof state !== 'undefined' && (!state.bridgeRunning || state.conn.status !== 'connected')){" +
            "        window.__ensureAndroidMqttBridgeAndConnect();" +
            "      }" +
            "    }, 4000);" +
            "  }" +
            "})();";

    @Override
    protected String getScreenTitle() {
        return "📡 Real Time Monitor";
    }

    @Override
    protected String getScreenSubtitle() {
        return "Forge Nodes Telemetry Dashboard";
    }

    @Override
    protected int getNavTabIndex() {
        return 1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;

        LinearLayout content = getContentContainer();
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        content.addView(webView);

        configureAuthenticatedWebView(webView, "/mqtt-monitor/index.html", MONITOR_MOBILE_INJECTION_JS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.evaluateJavascript("if(window.__ensureAndroidMqttBridgeAndConnect) window.__ensureAndroidMqttBridgeAndConnect();", null);
        }
    }

    @Override
    public void onBackPressed() {
        // If viewing a single node inside the Telemetry Dashboard, return to All Nodes grid first
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function(){ if(typeof state !== 'undefined' && state.selectedView !== 'all'){ selectAllTopicsView(); return 'handled'; } return 'root'; })();",
                    val -> {
                        if (val == null || !val.contains("handled")) {
                            RealTimeMonitorActivity.super.onBackPressed();
                        }
                    }
            );
        } else {
            super.onBackPressed();
        }
    }
}
