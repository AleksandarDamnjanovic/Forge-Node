package com.aleksandar_damnjanovic.forge_node;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Activity 4: Meta (Strictly Read-Only)
 * - Displays MQTT Server Settings & Bridge Status (/api/mqtt/status, /api/bridge/status)
 * - Displays Metadata of used TLS Certificates (/api/certs/details)
 * - If Local AAU is used (remoteAau == false in /api/aau/status), displays AAU Logs (/api/aau/logs)
 * - Android user cannot change or edit any settings here.
 */
public class MetaActivity extends BaseForgeActivity {

    private ScrollView scrollView;
    private LinearLayout contentList;
    private TextView btnRefresh;
    private String lastMetaSignature = "";
    private String lastLogText = "";
    private boolean autoScrollLogs = true;
    private android.webkit.WebView logWebView;
    private TextView btnToggleLogAutoScroll;
    private final android.os.Handler metaPollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable metaPollRunnable = new Runnable() {
        @Override
        public void run() {
            loadAllMetaAsync(true);
            metaPollHandler.postDelayed(this, 2500);
        }
    };

    @Override
    protected String getScreenTitle() {
        return "🔐 Meta & Diagnostics";
    }

    @Override
    protected String getScreenSubtitle() {
        return "Read-Only MQTT Settings, TLS Certificates & Local AAU Logs";
    }

    @Override
    protected int getNavTabIndex() {
        return 3;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;

        float d = getResources().getDisplayMetrics().density;
        LinearLayout container = getContentContainer();

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        actionRow.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));

        TextView tvReadOnlyBadge = new TextView(this);
        tvReadOnlyBadge.setText("🔒 Read-Only View");
        tvReadOnlyBadge.setTextSize(11.5f);
        tvReadOnlyBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvReadOnlyBadge.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvReadOnlyBadge.setTag("readonly_badge");
        actionRow.addView(tvReadOnlyBadge);

        btnRefresh = new TextView(this);
        btnRefresh.setText("🔄 Refresh Meta");
        btnRefresh.setTextSize(11.5f);
        btnRefresh.setTypeface(Typeface.DEFAULT_BOLD);
        btnRefresh.setPadding((int) (12 * d), (int) (6 * d), (int) (12 * d), (int) (6 * d));
        btnRefresh.setOnClickListener(v -> loadAllMetaAsync(false));
        actionRow.addView(btnRefresh);

        container.addView(actionRow);

        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        contentList = new LinearLayout(this);
        contentList.setOrientation(LinearLayout.VERTICAL);
        contentList.setPadding((int) (12 * d), (int) (10 * d), (int) (12 * d), (int) (18 * d));
        scrollView.addView(contentList);

        container.addView(scrollView);

        applyNativeTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();
        metaPollHandler.removeCallbacks(metaPollRunnable);
        metaPollHandler.post(metaPollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        metaPollHandler.removeCallbacks(metaPollRunnable);
    }

    @Override
    protected void onThemeUpdated(ThemeEngine.Palette p, String uiStyle) {
        if (scrollView == null) return;
        float btnRadius = ThemeEngine.getCornerRadiusDp(uiStyle, true);
        scrollView.setBackgroundColor(p.bgApp);
        if (btnRefresh != null) {
            ThemeEngine.styleRoundedBox(btnRefresh, p.bgHover, p.border, btnRadius, 2);
            btnRefresh.setTextColor(p.textMain);
        }
        View badge = getContentContainer().findViewWithTag("readonly_badge");
        if (badge instanceof TextView tv) {
            tv.setTextColor(p.textMuted);
        }
    }

    private void loadAllMetaAsync(boolean silentAutoRefresh) {
        final String baseUrl = session.getBaseUrl();
        final String token = session.getToken();
        if (!silentAutoRefresh && btnRefresh != null) {
            btnRefresh.setText("Loading…");
            btnRefresh.setEnabled(false);
        }

        bgExecutor.submit(() -> {
            ForgeApiClient.ApiResponse mqttRes = ForgeApiClient.get(baseUrl, "/api/mqtt/status", token);
            ForgeApiClient.ApiResponse bridgeRes = ForgeApiClient.get(baseUrl, "/api/bridge/status", token);
            ForgeApiClient.ApiResponse certsRes = ForgeApiClient.get(baseUrl, "/api/certs/details", token);
            ForgeApiClient.ApiResponse aauStatusRes = ForgeApiClient.get(baseUrl, "/api/aau/status", token);
            ForgeApiClient.ApiResponse aauConfigRes = ForgeApiClient.get(baseUrl, "/api/aau/config", token);

            boolean isLocalAau = false;
            boolean localAauRunning = false;
            if (aauStatusRes.isOk()) {
                try {
                    JSONObject aauObj = new JSONObject(aauStatusRes.body);
                    isLocalAau = !aauObj.optBoolean("remoteAau", true);
                    localAauRunning = aauObj.optBoolean("localAauRunning", false);
                } catch (Exception ignored) {
                }
            }

            ForgeApiClient.ApiResponse aauLogsRes = null;
            if (isLocalAau) {
                aauLogsRes = ForgeApiClient.get(baseUrl, "/api/aau/logs", token);
            }

            final boolean finalIsLocalAau = isLocalAau;
            final boolean finalLocalAauRunning = localAauRunning;
            final ForgeApiClient.ApiResponse finalLogsRes = aauLogsRes;
            final String nonLogSignature = mqttRes.body + "|" + bridgeRes.body + "|" + certsRes.body + "|"
                    + aauStatusRes.body + "|" + aauConfigRes.body + "|" + isLocalAau + "|" + localAauRunning;
            final String newLogContent = extractLogText(aauLogsRes);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (!silentAutoRefresh && btnRefresh != null) {
                    btnRefresh.setText("🔄 Refresh Meta");
                    btnRefresh.setEnabled(true);
                }
                if (!nonLogSignature.equals(lastMetaSignature) || contentList.getChildCount() == 0) {
                    lastMetaSignature = nonLogSignature;
                    lastLogText = newLogContent;
                    renderMetaCards(mqttRes, bridgeRes, certsRes, aauConfigRes, finalIsLocalAau, finalLocalAauRunning, newLogContent);
                } else if (finalIsLocalAau && !newLogContent.equals(lastLogText)) {
                    lastLogText = newLogContent;
                    updateLogViewerTextOnly(newLogContent);
                }
            });
        });
    }

    private String extractLogText(ForgeApiClient.ApiResponse aauLogsRes) {
        String logText = "No log entries available.";
        if (aauLogsRes != null && aauLogsRes.isOk()) {
            try {
                JSONObject logObj = new JSONObject(aauLogsRes.body);
                String rawContent = logObj.optString("content", "").trim();
                if (!rawContent.isEmpty()) {
                    // Keep at most the last 300 lines so rendering is instantaneous even on huge log files
                    String[] lines = rawContent.split("\\r?\\n");
                    if (lines.length > 300) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = lines.length - 300; i < lines.length; i++) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(lines[i]);
                        }
                        logText = sb.toString();
                    } else {
                        logText = rawContent;
                    }
                } else if (!logObj.optBoolean("exists", false)) {
                    logText = "Log file does not exist yet (" + logObj.optString("path", "AAU/script/log.txt") + ").";
                }
            } catch (Exception ignored) {
            }
        }
        return logText;
    }

    private void updateLogViewerTextOnly(String newLogContent) {
        if (logWebView == null) return;
        String escaped = JSONObject.quote(newLogContent);
        String js = "if(window.__setLogContent) window.__setLogContent(" + escaped + ", " + autoScrollLogs + ");";
        logWebView.evaluateJavascript(js, null);
    }

    private void scrollLogToBottom() {
        if (logWebView == null) return;
        logWebView.evaluateJavascript("if(window.__scrollToBottom) window.__scrollToBottom();", null);
    }

    private void styleAutoScrollButton(ThemeEngine.Palette p, String uiStyle) {
        if (btnToggleLogAutoScroll == null) return;
        float btnRadius = ThemeEngine.getCornerRadiusDp(uiStyle, true);
        if (autoScrollLogs) {
            btnToggleLogAutoScroll.setText("⏸ Stop Auto-Scroll");
            ThemeEngine.styleRoundedBox(btnToggleLogAutoScroll, p.primary, p.primary, btnRadius, 0);
            btnToggleLogAutoScroll.setTextColor(0xFFFFFFFF);
        } else {
            btnToggleLogAutoScroll.setText("▶ Resume Auto-Scroll");
            ThemeEngine.styleRoundedBox(btnToggleLogAutoScroll, p.bgHover, p.warning, btnRadius, 2);
            btnToggleLogAutoScroll.setTextColor(p.warning);
        }
    }

    private void renderMetaCards(
            ForgeApiClient.ApiResponse mqttRes,
            ForgeApiClient.ApiResponse bridgeRes,
            ForgeApiClient.ApiResponse certsRes,
            ForgeApiClient.ApiResponse aauConfigRes,
            boolean isLocalAau,
            boolean localAauRunning,
            String logText
    ) {
        contentList.removeAllViews();
        ThemeEngine.Palette p = ThemeEngine.getPalette(session.getColorTheme());
        String uiStyle = session.getUiStyle();
        float cardRadius = ThemeEngine.getCornerRadiusDp(uiStyle, false);

        // 1. MQTT Server Settings Card
        LinearLayout mqttCard = makeCard("🔌 MQTT Server Settings", p, cardRadius);
        try {
            JSONObject mObj = new JSONObject(mqttRes.body);
            boolean remoteMqtt = mObj.optBoolean("remoteMqtt", true);
            boolean exposeMqtt = mObj.optBoolean("exposeMqtt", true);
            JSONObject cfg = mObj.optJSONObject("config");

            addKeyValueRow(mqttCard, "MQTT Server Mode", remoteMqtt ? "Remote MQTT Broker" : "Local Mosquitto Server", p);
            addKeyValueRow(mqttCard, "Expose Local MQTT", String.valueOf(exposeMqtt), p);

            if (remoteMqtt && cfg != null) {
                addKeyValueRow(mqttCard, "Broker IP / Host", cfg.optString("ip", "—"), p);
                addKeyValueRow(mqttCard, "Broker Port", String.valueOf(cfg.optInt("port", 8883)), p);
                addKeyValueRow(mqttCard, "Username", cfg.optString("username", "—"), p);
                addKeyValueRow(mqttCard, "Subscribe Admin Topic", String.valueOf(cfg.optBoolean("subscribe_admin", true)), p);
                addKeyValueRow(mqttCard, "CA Cert Path", cfg.optString("ca_cert_path", "—"), p);
                addKeyValueRow(mqttCard, "Broker Cert Path", cfg.optString("broker_cert_path", "—"), p);
            } else {
                JSONObject localDetails = mObj.optJSONObject("localConfigDetails");
                addKeyValueRow(mqttCard, "Local Mosquitto Active", String.valueOf(mObj.optBoolean("localMqttRunning", false)), p);
                addKeyValueRow(mqttCard, "Local Config Applied", String.valueOf(mObj.optBoolean("localConfigApplied", false)), p);
                if (localDetails != null) {
                    addKeyValueRow(mqttCard, "Bind Address", localDetails.optString("address", "0.0.0.0"), p);
                    addKeyValueRow(mqttCard, "Port", localDetails.optString("port", "8883"), p);
                    addKeyValueRow(mqttCard, "CA Certificate", localDetails.optString("ca.crt", "—"), p);
                    addKeyValueRow(mqttCard, "Broker Certificate", localDetails.optString("broker.crt", "—"), p);
                }
            }

            if (bridgeRes != null && bridgeRes.isOk()) {
                JSONObject bObj = new JSONObject(bridgeRes.body);
                boolean bRun = bObj.optBoolean("running", false);
                int bPort = bObj.optInt("port", 8765);
                addKeyValueRow(mqttCard, "MQTT Monitor Bridge", (bRun ? "Running" : "Stopped") + " (Port " + bPort + ")", p);
            }
        } catch (Exception e) {
            addKeyValueRow(mqttCard, "Status", "Could not load MQTT settings", p);
        }
        contentList.addView(mqttCard);

        // 2. TLS Certificates Metadata Card
        LinearLayout certsCard = makeCard("🔐 TLS Certificates Metadata", p, cardRadius);
        try {
            JSONObject cObj = new JSONObject(certsRes.body);
            JSONArray certsArr = cObj.optJSONArray("certificates");
            if (certsArr == null || certsArr.length() == 0) {
                addKeyValueRow(certsCard, "Certificates", cObj.optString("error", "No certificates found in SHARED/"), p);
            } else {
                for (int i = 0; i < certsArr.length(); i++) {
                    JSONObject cert = certsArr.getJSONObject(i);
                    addSectionHeader(certsCard, cert.optString("label", "Certificate") + " (" + cert.optString("fileName", "") + ")", p);
                    if (cert.has("error")) {
                        addKeyValueRow(certsCard, "Error", cert.optString("error"), p);
                        continue;
                    }
                    addKeyValueRow(certsCard, "Validity Status", cert.optString("validityStatus", "—"), p);
                    addKeyValueRow(certsCard, "Subject", cert.optString("subject", "—"), p);
                    addKeyValueRow(certsCard, "Issuer", cert.optString("issuer", "—"), p);
                    addKeyValueRow(certsCard, "Valid From", cert.optString("validFrom", "—"), p);
                    addKeyValueRow(certsCard, "Valid Until", cert.optString("validUntil", "—"), p);
                    addKeyValueRow(certsCard, "Serial Number", cert.optString("serialNumber", "—"), p);
                    addKeyValueRow(certsCard, "Signature Algorithm", cert.optString("signatureAlgorithm", "—") + " (" + cert.optString("version", "") + ")", p);
                    addKeyValueRow(certsCard, "Public Key", cert.optString("publicKey", "—"), p);
                    JSONArray sans = cert.optJSONArray("subjectAltNames");
                    if (sans != null && sans.length() > 0) {
                        StringBuilder sanSb = new StringBuilder();
                        for (int j = 0; j < sans.length(); j++) {
                            if (j > 0) sanSb.append(", ");
                            sanSb.append(sans.optString(j));
                        }
                        addKeyValueRow(certsCard, "Subject Alt Names", sanSb.toString(), p);
                    }
                    addKeyValueRow(certsCard, "SHA-256 Fingerprint", cert.optString("sha256Fingerprint", "—"), p);
                    addKeyValueRow(certsCard, "SHA-1 Fingerprint", cert.optString("sha1Fingerprint", "—"), p);
                }
            }
        } catch (Exception e) {
            addKeyValueRow(certsCard, "Certificates", "Unable to read certificate details", p);
        }
        contentList.addView(certsCard);

        // 3. AAU Status & Conditional Local AAU Logs Card
        if (isLocalAau) {
            LinearLayout aauCard = makeCard("📜 Local AAU Configuration & Logs", p, cardRadius);
            addKeyValueRow(aauCard, "AAU Mode", "Local AAU (" + (localAauRunning ? "Running" : "Stopped") + ")", p);
            if (aauConfigRes != null && aauConfigRes.isOk()) {
                try {
                    JSONObject aauCfgWrapper = new JSONObject(aauConfigRes.body);
                    JSONObject aauCfg = aauCfgWrapper.optJSONObject("config");
                    if (aauCfg != null) {
                        addKeyValueRow(aauCard, "Operational Script", aauCfg.optString("scriptFile", "—"), p);
                        addKeyValueRow(aauCard, "Log File Path", aauCfg.optString("logFile", "—"), p);
                        addKeyValueRow(aauCard, "MQTT Target", aauCfg.optString("address", "—") + ":" + aauCfg.optInt("port", 8883), p);
                    }
                } catch (Exception ignored) {
                }
            }

            float d = getResources().getDisplayMetrics().density;

            // Header row for AAU Log Output + Auto-Scroll Toggle Button
            LinearLayout logHeaderRow = new LinearLayout(this);
            logHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
            logHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
            logHeaderRow.setPadding(0, (int) (10 * d), 0, (int) (4 * d));

            TextView tvLogHeader = new TextView(this);
            tvLogHeader.setText("AAU Log Output (10-line live window)");
            tvLogHeader.setTextSize(12.5f);
            tvLogHeader.setTypeface(Typeface.DEFAULT_BOLD);
            tvLogHeader.setTextColor(p.primary);
            tvLogHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            logHeaderRow.addView(tvLogHeader);

            btnToggleLogAutoScroll = new TextView(this);
            btnToggleLogAutoScroll.setTextSize(11f);
            btnToggleLogAutoScroll.setTypeface(Typeface.DEFAULT_BOLD);
            btnToggleLogAutoScroll.setPadding((int) (10 * d), (int) (5 * d), (int) (10 * d), (int) (5 * d));
            styleAutoScrollButton(p, uiStyle);
            btnToggleLogAutoScroll.setOnClickListener(v -> {
                autoScrollLogs = !autoScrollLogs;
                styleAutoScrollButton(p, uiStyle);
                if (autoScrollLogs) {
                    scrollLogToBottom();
                }
            });
            logHeaderRow.addView(btnToggleLogAutoScroll);
            aauCard.addView(logHeaderRow);

            // Compact 10-line self-contained HTML log viewport inside a fixed-height container
            logWebView = new android.webkit.WebView(this) {
                @Override
                public boolean onTouchEvent(android.view.MotionEvent event) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return super.onTouchEvent(event);
                }
            };
            logWebView.getSettings().setJavaScriptEnabled(true);
            logWebView.setBackgroundColor(p.bgInput);
            LinearLayout.LayoutParams logLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (175 * d));
            logLp.setMargins(0, (int) (4 * d), 0, 0);
            logWebView.setLayoutParams(logLp);

            String bgHex = String.format("#%06X", (0xFFFFFF & p.bgInput));
            String textHex = String.format("#%06X", (0xFFFFFF & p.textMain));
            String borderHex = String.format("#%06X", (0xFFFFFF & p.border));
            int radiusPx = (int) ThemeEngine.getCornerRadiusDp(uiStyle, true);
            String initialLogJs = JSONObject.quote(logText);

            String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1.0'/>"
                    + "<style>"
                    + "*{box-sizing:border-box;margin:0;padding:0;}"
                    + "html,body{height:100%;width:100%;background:" + bgHex + ";color:" + textHex + ";overflow:hidden;}"
                    + "#logBox{"
                    + "  height:100%;width:100%;overflow-y:auto;overflow-x:auto;"
                    + "  padding:8px 10px;border:1px solid " + borderHex + ";border-radius:" + radiusPx + "px;"
                    + "  font-family:monospace;font-size:11.5px;line-height:15.5px;white-space:pre-wrap;word-break:break-word;"
                    + "}"
                    + "</style></head><body>"
                    + "<div id='logBox'></div>"
                    + "<script>"
                    + "const box = document.getElementById('logBox');"
                    + "window.__scrollToBottom = function(){ box.scrollTop = box.scrollHeight; };"
                    + "window.__setLogContent = function(txt, doScroll){"
                    + "  const prev = box.scrollTop;"
                    + "  box.textContent = txt;"
                    + "  if(doScroll){ box.scrollTop = box.scrollHeight; } else { box.scrollTop = prev; }"
                    + "};"
                    + "window.__setLogContent(" + initialLogJs + ", " + autoScrollLogs + ");"
                    + "</script></body></html>";

            logWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

            aauCard.addView(logWebView);
            contentList.addView(aauCard);
        } else {
            logWebView = null;
            btnToggleLogAutoScroll = null;
        }
    }

    private LinearLayout makeCard(String title, ThemeEngine.Palette p, float radiusDp) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (14 * d), (int) (14 * d), (int) (14 * d), (int) (14 * d));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int) (14 * d));
        card.setLayoutParams(lp);
        ThemeEngine.styleRoundedBox(card, p.bgCard, p.border, radiusDp, 2);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(p.textMain);
        tvTitle.setPadding(0, 0, 0, (int) (10 * d));
        card.addView(tvTitle);

        return card;
    }

    private void addSectionHeader(LinearLayout card, String headerText, ThemeEngine.Palette p) {
        float d = getResources().getDisplayMetrics().density;
        TextView tv = new TextView(this);
        tv.setText(headerText);
        tv.setTextSize(12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(p.primary);
        tv.setPadding(0, (int) (10 * d), 0, (int) (4 * d));
        card.addView(tv);
    }

    private void addKeyValueRow(LinearLayout card, String key, String value, ThemeEngine.Palette p) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, (int) (4 * d), 0, (int) (6 * d));

        TextView tvKey = new TextView(this);
        tvKey.setText(key);
        tvKey.setTextSize(11f);
        tvKey.setTypeface(Typeface.DEFAULT_BOLD);
        tvKey.setTextColor(p.textMuted);
        row.addView(tvKey);

        TextView tvVal = new TextView(this);
        tvVal.setText(value == null || value.isEmpty() ? "—" : value);
        tvVal.setTextSize(12.5f);
        tvVal.setTypeface(Typeface.MONOSPACE);
        tvVal.setTextColor(p.textMain);
        row.addView(tvVal);

        card.addView(row);
    }
}
