package com.aleksandar_damnjanovic.forge_node;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared base activity providing:
 * - Native top header bar with brand title & Sign Out button
 * - Bottom navigation bar switching between the 4 main Activities:
 *   1. Script Studio (ScriptStudioActivity)
 *   2. Real Time Monitor (RealTimeMonitorActivity)
 *   3. Live MQTT Stream (LiveMqttStreamActivity)
 *   4. Meta (MetaActivity)
 * - Auto-syncs Color Theme, UI Style, and Icon Pack from /api/theme on the Spring server
 */
public abstract class BaseForgeActivity extends AppCompatActivity {

    protected SessionManager session;
    protected final ExecutorService bgExecutor = Executors.newCachedThreadPool();

    private LinearLayout rootContainer;
    private LinearLayout topBar;
    private TextView tvTopTitle;
    private TextView tvTopSubtitle;
    private TextView btnSignOut;
    private LinearLayout contentContainer;
    private LinearLayout bottomNav;
    private final android.os.Handler themePollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable themePollRunnable = new Runnable() {
        @Override
        public void run() {
            syncThemeFromServerAsync();
            themePollHandler.postDelayed(this, 4000);
        }
    };

    protected abstract String getScreenTitle();
    protected abstract String getScreenSubtitle();
    protected abstract int getNavTabIndex(); // 0=Studio, 1=Monitor, 2=Stream, 3=Meta

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        if (session.getBaseUrl().isEmpty() || session.getToken().isEmpty()) {
            navigateToLogin();
            return;
        }

        buildShellLayout();
        applyNativeTheme();
        syncThemeFromServerAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        themePollHandler.removeCallbacks(themePollRunnable);
        themePollHandler.post(themePollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        themePollHandler.removeCallbacks(themePollRunnable);
    }

    private void buildShellLayout() {
        float d = getResources().getDisplayMetrics().density;

        rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Top Bar
        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topBar.setLayoutParams(topLp);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleCol.setLayoutParams(titleColLp);

        tvTopTitle = new TextView(this);
        tvTopTitle.setText(getScreenTitle());
        tvTopTitle.setTextSize(15.5f);
        tvTopTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvTopTitle);

        tvTopSubtitle = new TextView(this);
        tvTopSubtitle.setText(getScreenSubtitle());
        tvTopSubtitle.setTextSize(11f);
        titleCol.addView(tvTopSubtitle);

        topBar.addView(titleCol);

        btnSignOut = new TextView(this);
        btnSignOut.setText("Sign Out");
        btnSignOut.setTextSize(12f);
        btnSignOut.setTypeface(Typeface.DEFAULT_BOLD);
        btnSignOut.setPadding((int) (12 * d), (int) (6 * d), (int) (12 * d), (int) (6 * d));
        btnSignOut.setOnClickListener(v -> performLogout());
        topBar.addView(btnSignOut);

        rootContainer.addView(topBar);

        // 1px Divider
        View topDiv = new View(this);
        topDiv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) d)));
        topDiv.setTag("divider");
        rootContainer.addView(topDiv);

        // Content Frame
        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        rootContainer.addView(contentContainer);

        // Bottom 1px Divider
        View botDiv = new View(this);
        botDiv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) d)));
        botDiv.setTag("divider");
        rootContainer.addView(botDiv);

        // Bottom Navigation Bar
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));
        bottomNav.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addNavTab(0, "Script Studio", ScriptStudioActivity.class);
        addNavTab(1, "Real Time Monitor", RealTimeMonitorActivity.class);
        addNavTab(2, "Live Stream", LiveMqttStreamActivity.class);
        addNavTab(3, "Meta", MetaActivity.class);

        rootContainer.addView(bottomNav);
        setContentView(rootContainer);
    }

    private void addNavTab(int idx, String label, Class<?> targetActivity) {
        float d = getResources().getDisplayMetrics().density;
        TextView tab = new TextView(this);
        tab.setText(label);
        tab.setTextSize(11.5f);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding((int) (6 * d), (int) (9 * d), (int) (6 * d), (int) (9 * d));
        tab.setTypeface(null, idx == getNavTabIndex() ? Typeface.BOLD : Typeface.NORMAL);
        tab.setTag("nav_tab_" + idx);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins((int) (3 * d), 0, (int) (3 * d), 0);
        tab.setLayoutParams(lp);

        tab.setOnClickListener(v -> {
            if (idx == getNavTabIndex()) return;
            Intent intent = new Intent(this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        bottomNav.addView(tab);
    }

    protected LinearLayout getContentContainer() {
        return contentContainer;
    }

    protected void applyNativeTheme() {
        ThemeEngine.Palette p = ThemeEngine.getPalette(session.getColorTheme());
        String uiStyle = session.getUiStyle();
        float btnRadius = ThemeEngine.getCornerRadiusDp(uiStyle, true);

        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(p.bgPanel);
            window.setNavigationBarColor(p.bgPanel);
        }

        rootContainer.setBackgroundColor(p.bgApp);
        topBar.setBackgroundColor(p.bgPanel);
        tvTopTitle.setTextColor(p.textMain);
        tvTopSubtitle.setTextColor(p.textMuted);
        ThemeEngine.styleRoundedBox(btnSignOut, p.bgHover, p.border, btnRadius, 2);
        btnSignOut.setTextColor(p.textMain);

        for (int i = 0; i < rootContainer.getChildCount(); i++) {
            View child = rootContainer.getChildAt(i);
            if ("divider".equals(child.getTag())) {
                child.setBackgroundColor(p.border);
            }
        }

        bottomNav.setBackgroundColor(p.bgPanel);
        for (int i = 0; i < 4; i++) {
            View v = bottomNav.findViewWithTag("nav_tab_" + i);
            if (v instanceof TextView tv) {
                boolean active = (i == getNavTabIndex());
                if (active) {
                    ThemeEngine.styleRoundedBox(tv, p.primary, p.primary, btnRadius, 0);
                    tv.setTextColor(0xFFFFFFFF);
                } else {
                    ThemeEngine.styleRoundedBox(tv, p.bgCard, p.border, btnRadius, 2);
                    tv.setTextColor(p.textMuted);
                }
            }
        }
        onThemeUpdated(p, uiStyle);
    }

    protected void onThemeUpdated(ThemeEngine.Palette p, String uiStyle) {
        // Subclasses can override to re-theme custom native views
    }

    protected void syncThemeFromServerAsync() {
        final String baseUrl = session.getBaseUrl();
        final String token = session.getToken();
        if (baseUrl.isEmpty()) return;

        bgExecutor.submit(() -> {
            ForgeApiClient.ApiResponse res = ForgeApiClient.get(baseUrl, "/api/theme", token);
            if (res.isOk()) {
                try {
                    JSONObject obj = new JSONObject(res.body);
                    String colorTheme = obj.optString("colorTheme", obj.optString("theme", "github-dark"));
                    String uiStyle = obj.optString("uiStyle", "forge-classic");
                    String iconPack = obj.optString("iconPack", "emoji");
                    boolean changed = !colorTheme.equals(session.getColorTheme())
                            || !uiStyle.equals(session.getUiStyle())
                            || !iconPack.equals(session.getIconPack());
                    if (changed) {
                        session.saveThemeConfig(colorTheme, uiStyle, iconPack);
                        runOnUiThread(this::applyNativeTheme);
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    protected void configureAuthenticatedWebView(WebView webView, String targetPath, String injectedJsOnFinished) {
        String baseUrl = session.getBaseUrl();
        String token = session.getToken();

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);
        cm.setCookie(baseUrl, "FN_ACCESS_TOKEN=" + token + "; Path=/");
        cm.flush();

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);

        ThemeEngine.Palette p = ThemeEngine.getPalette(session.getColorTheme());
        webView.setBackgroundColor(p.bgApp);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                installAuthPatchJs(view, token);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.contains("/login")) {
                    runOnUiThread(() -> navigateToLogin());
                    return;
                }
                installAuthPatchJs(view, token);
                if (injectedJsOnFinished != null && !injectedJsOnFinished.isEmpty()) {
                    view.evaluateJavascript(injectedJsOnFinished, null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String reqUrl = request.getUrl().toString();
                if (reqUrl.contains("/login")) {
                    navigateToLogin();
                    return true;
                }
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("ngrok-skip-browser-warning", "true");
                view.loadUrl(reqUrl, headers);
                return true;
            }
        });

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("ngrok-skip-browser-warning", "true");
        webView.loadUrl(baseUrl + targetPath, headers);
    }

    private void installAuthPatchJs(WebView view, String token) {
        String authFetchPatch =
                "(function(){" +
                "  document.cookie = 'FN_ACCESS_TOKEN=" + escapeJs(token) + "; path=/; SameSite=Lax';" +
                "  if(window.__fnAndroidFetchPatched) return;" +
                "  window.__fnAndroidFetchPatched = true;" +
                "  const origFetch = window.fetch;" +
                "  window.fetch = function(input, init) {" +
                "    init = init || {};" +
                "    const headers = new Headers(init.headers || {});" +
                "    if(!headers.has('Authorization')) headers.set('Authorization', 'Bearer " + escapeJs(token) + "');" +
                "    headers.set('ngrok-skip-browser-warning', 'true');" +
                "    init.headers = headers;" +
                "    return origFetch(input, init);" +
                "  };" +
                "})();";
        view.evaluateJavascript(authFetchPatch, null);
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    protected void performLogout() {
        final String baseUrl = session.getBaseUrl();
        final String token = session.getToken();
        bgExecutor.submit(() -> {
            ForgeApiClient.post(baseUrl, "/api/auth/logout", new JSONObject(), token);
        });
        session.clearActiveSession();
        CookieManager.getInstance().removeAllCookies(null);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_FROM_SIGN_OUT, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void navigateToLogin() {
        navigateToLoginWithError(null);
    }

    protected void navigateToLoginWithError(String errorMessage) {
        Intent intent = new Intent(this, LoginActivity.class);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            intent.putExtra(LoginActivity.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bgExecutor.shutdownNow();
    }
}
