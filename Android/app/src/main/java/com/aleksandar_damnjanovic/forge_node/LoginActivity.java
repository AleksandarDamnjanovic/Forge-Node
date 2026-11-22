package com.aleksandar_damnjanovic.forge_node;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_FROM_SIGN_OUT = "extra_from_sign_out";
    public static final String EXTRA_ERROR_MESSAGE = "extra_error_message";

    private SessionManager session;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ScrollView rootScroll;
    private LinearLayout mainCard;
    private LinearLayout savedProfilesSection;
    private LinearLayout savedProfilesList;

    private EditText inpAddress;
    private EditText inpPort;
    private EditText inpUsername;
    private EditText inpPassword;
    private CheckBox chkSaveProfile;
    private EditText inpProfileName;
    private TextView tvError;
    private TextView btnSignIn;

    private String selectedProfileId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        buildUi();
        applyTheme();
        populateSavedProfiles();

        boolean fromSignOut = getIntent().getBooleanExtra(EXTRA_FROM_SIGN_OUT, false);
        String incomingError = getIntent().getStringExtra(EXTRA_ERROR_MESSAGE);
        if (incomingError != null && !incomingError.isEmpty()) {
            showError(incomingError);
            Toast.makeText(this, incomingError, Toast.LENGTH_LONG).show();
        } else if (!fromSignOut && session.isAutoLoginEnabled()) {
            ConnectionProfile defaultProfile = session.getDefaultConnectionProfile();
            if (defaultProfile != null
                    && defaultProfile.address != null && !defaultProfile.address.trim().isEmpty()
                    && defaultProfile.username != null && !defaultProfile.username.trim().isEmpty()
                    && defaultProfile.password != null && !defaultProfile.password.isEmpty()) {
                fillFormFromProfile(defaultProfile);
                attemptAutoLogin(defaultProfile);
            }
        }
    }

    private void buildUi() {
        float d = getResources().getDisplayMetrics().density;

        rootScroll = new ScrollView(this);
        rootScroll.setFillViewport(true);
        rootScroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout outerWrap = new LinearLayout(this);
        outerWrap.setOrientation(LinearLayout.VERTICAL);
        outerWrap.setGravity(Gravity.CENTER);
        outerWrap.setPadding((int) (18 * d), (int) (24 * d), (int) (18 * d), (int) (24 * d));
        rootScroll.addView(outerWrap);

        mainCard = new LinearLayout(this);
        mainCard.setOrientation(LinearLayout.VERTICAL);
        mainCard.setPadding((int) (20 * d), (int) (22 * d), (int) (20 * d), (int) (22 * d));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mainCard.setLayoutParams(cardLp);

        // Brand Header
        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        brandRow.setPadding(0, 0, 0, (int) (16 * d));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_forge_node);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams((int) (54 * d), (int) (54 * d));
        logoLp.setMargins(0, 0, (int) (14 * d), 0);
        logo.setLayoutParams(logoLp);
        brandRow.addView(logo);

        LinearLayout brandTextCol = new LinearLayout(this);
        brandTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Forge Node Android");
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTag("text_main");
        brandTextCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Connect locally (LAN IP + port) or via Ngrok URL");
        tvSub.setTextSize(12f);
        tvSub.setTag("text_muted");
        brandTextCol.addView(tvSub);

        brandRow.addView(brandTextCol);
        mainCard.addView(brandRow);

        // Saved Configurations Section
        savedProfilesSection = new LinearLayout(this);
        savedProfilesSection.setOrientation(LinearLayout.VERTICAL);
        savedProfilesSection.setPadding(0, 0, 0, (int) (14 * d));

        TextView lblSaved = makeLabel("SAVED CONFIGURATIONS (TAP TO LOAD)");
        savedProfilesSection.addView(lblSaved);

        savedProfilesList = new LinearLayout(this);
        savedProfilesList.setOrientation(LinearLayout.VERTICAL);
        savedProfilesSection.addView(savedProfilesList);

        mainCard.addView(savedProfilesSection);

        // Error Banner
        tvError = new TextView(this);
        tvError.setVisibility(View.GONE);
        tvError.setTextSize(12.5f);
        tvError.setPadding((int) (12 * d), (int) (10 * d), (int) (12 * d), (int) (10 * d));
        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errLp.setMargins(0, 0, 0, (int) (12 * d));
        tvError.setLayoutParams(errLp);
        mainCard.addView(tvError);

        // Address & Port Row
        LinearLayout hostPortRow = new LinearLayout(this);
        hostPortRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams hpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hpLp.setMargins(0, 0, 0, (int) (12 * d));
        hostPortRow.setLayoutParams(hpLp);

        LinearLayout addrCol = new LinearLayout(this);
        addrCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams addrColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.2f);
        addrColLp.setMargins(0, 0, (int) (8 * d), 0);
        addrCol.setLayoutParams(addrColLp);
        addrCol.addView(makeLabel("SERVER ADDRESS / NGROK URL"));
        inpAddress = makeInput("192.168.0.24 or https://xxx.ngrok-free.app", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addrCol.addView(inpAddress);
        hostPortRow.addView(addrCol);

        LinearLayout portCol = new LinearLayout(this);
        portCol.setOrientation(LinearLayout.VERTICAL);
        portCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        portCol.addView(makeLabel("PORT (OPTIONAL)"));
        inpPort = makeInput("10001", InputType.TYPE_CLASS_NUMBER);
        portCol.addView(inpPort);
        hostPortRow.addView(portCol);

        mainCard.addView(hostPortRow);

        // Username
        mainCard.addView(makeLabel("ADMIN USERNAME"));
        inpUsername = makeInput("admin", InputType.TYPE_CLASS_TEXT);
        setBottomMargin(inpUsername, (int) (12 * d));
        mainCard.addView(inpUsername);

        // Password
        mainCard.addView(makeLabel("ADMIN PASSWORD"));
        inpPassword = makeInput("••••••••", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        setBottomMargin(inpPassword, (int) (12 * d));
        mainCard.addView(inpPassword);

        // Save Profile Checkbox & Name
        chkSaveProfile = new CheckBox(this);
        chkSaveProfile.setText("Save login data under configuration for later use");
        chkSaveProfile.setChecked(true);
        chkSaveProfile.setTextSize(12.5f);
        chkSaveProfile.setTag("text_main");
        mainCard.addView(chkSaveProfile);

        LinearLayout profileNameWrap = new LinearLayout(this);
        profileNameWrap.setOrientation(LinearLayout.VERTICAL);
        profileNameWrap.setPadding(0, (int) (6 * d), 0, (int) (14 * d));
        profileNameWrap.addView(makeLabel("CONFIGURATION NAME"));
        inpProfileName = makeInput("e.g. Local Home Server or Ngrok Remote", InputType.TYPE_CLASS_TEXT);
        profileNameWrap.addView(inpProfileName);
        mainCard.addView(profileNameWrap);

        chkSaveProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            profileNameWrap.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Sign In Button
        btnSignIn = new TextView(this);
        btnSignIn.setText("Sign In & Connect");
        btnSignIn.setTextSize(14f);
        btnSignIn.setTypeface(Typeface.DEFAULT_BOLD);
        btnSignIn.setGravity(Gravity.CENTER);
        btnSignIn.setPadding((int) (16 * d), (int) (13 * d), (int) (16 * d), (int) (13 * d));
        btnSignIn.setOnClickListener(v -> attemptLogin());
        mainCard.addView(btnSignIn);

        outerWrap.addView(mainCard);
        setContentView(rootScroll);
    }

    private TextView makeLabel(String text) {
        float d = getResources().getDisplayMetrics().density;
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 0, 0, (int) (5 * d));
        tv.setTag("text_muted");
        return tv;
    }

    private EditText makeInput(String hint, int inputType) {
        float d = getResources().getDisplayMetrics().density;
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextSize(13.5f);
        et.setSingleLine(true);
        et.setTypeface(Typeface.MONOSPACE);
        et.setPadding((int) (12 * d), (int) (10 * d), (int) (12 * d), (int) (10 * d));
        et.setTag("input_box");
        return et;
    }

    private void setBottomMargin(View v, int bottomPx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, bottomPx);
        v.setLayoutParams(lp);
    }

    private void populateSavedProfiles() {
        float d = getResources().getDisplayMetrics().density;
        ThemeEngine.Palette p = ThemeEngine.getPalette(session.getColorTheme());
        String uiStyle = session.getUiStyle();
        float btnRadius = ThemeEngine.getCornerRadiusDp(uiStyle, true);

        List<ConnectionProfile> profiles = session.getSavedProfiles();
        savedProfilesList.removeAllViews();

        if (profiles.isEmpty()) {
            savedProfilesSection.setVisibility(View.GONE);
            inpAddress.setText("192.168.0.24");
            inpPort.setText("10001");
            inpUsername.setText("admin");
            inpProfileName.setText("Local Forge Node");
            return;
        }

        savedProfilesSection.setVisibility(View.VISIBLE);
        ConnectionProfile initialToFill = session.getDefaultConnectionProfile();
        if (initialToFill == null) {
            initialToFill = profiles.get(0);
        }
        if (selectedProfileId == null) {
            fillFormFromProfile(initialToFill);
        }

        String defaultId = initialToFill != null ? initialToFill.id : "";
        for (ConnectionProfile cp : profiles) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, (int) (6 * d));
            row.setLayoutParams(rowLp);

            boolean isSelected = cp.id.equals(selectedProfileId);
            boolean isDefault = cp.id.equals(defaultId);
            ThemeEngine.styleRoundedBox(row, isSelected ? p.bgHover : p.bgInput,
                    isSelected ? p.borderFocus : p.border, btnRadius, 2);

            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(this);
            tvName.setText(isDefault ? cp.profileName + " ★ DEFAULT" : cp.profileName);
            tvName.setTextSize(13f);
            tvName.setTypeface(Typeface.DEFAULT_BOLD);
            tvName.setTextColor(isDefault ? p.primary : p.textMain);
            infoCol.addView(tvName);

            TextView tvMeta = new TextView(this);
            String hostDisp = cp.address + (cp.port != null && !cp.port.isEmpty() ? ":" + cp.port : "");
            tvMeta.setText(cp.username + " @ " + hostDisp);
            tvMeta.setTextSize(11f);
            tvMeta.setTypeface(Typeface.MONOSPACE);
            tvMeta.setTextColor(p.textMuted);
            infoCol.addView(tvMeta);

            row.addView(infoCol);

            TextView btnLoad = new TextView(this);
            btnLoad.setText("Use");
            btnLoad.setTextSize(11.5f);
            btnLoad.setTypeface(Typeface.DEFAULT_BOLD);
            btnLoad.setTextColor(0xFFFFFFFF);
            btnLoad.setPadding((int) (10 * d), (int) (5 * d), (int) (10 * d), (int) (5 * d));
            ThemeEngine.styleRoundedBox(btnLoad, p.primary, p.primary, btnRadius, 0);
            btnLoad.setOnClickListener(v -> {
                fillFormFromProfile(cp);
                populateSavedProfiles();
            });
            row.addView(btnLoad);

            TextView btnDel = new TextView(this);
            btnDel.setText("✕");
            btnDel.setTextSize(12f);
            btnDel.setTextColor(p.danger);
            btnDel.setPadding((int) (10 * d), (int) (5 * d), (int) (6 * d), (int) (5 * d));
            btnDel.setOnClickListener(v -> {
                session.deleteProfile(cp.id);
                if (cp.id.equals(selectedProfileId)) {
                    selectedProfileId = null;
                }
                populateSavedProfiles();
            });
            row.addView(btnDel);

            row.setOnClickListener(v -> {
                fillFormFromProfile(cp);
                populateSavedProfiles();
            });

            savedProfilesList.addView(row);
        }
    }

    private void fillFormFromProfile(ConnectionProfile cp) {
        selectedProfileId = cp.id;
        inpAddress.setText(cp.address);
        inpPort.setText(cp.port);
        inpUsername.setText(cp.username);
        inpPassword.setText(cp.password);
        inpProfileName.setText(cp.profileName);
    }

    private void applyTheme() {
        ThemeEngine.Palette p = ThemeEngine.getPalette(session.getColorTheme());
        String uiStyle = session.getUiStyle();
        float cardRadius = ThemeEngine.getCornerRadiusDp(uiStyle, false);
        float btnRadius = ThemeEngine.getCornerRadiusDp(uiStyle, true);

        Window w = getWindow();
        if (w != null) {
            w.setStatusBarColor(p.bgApp);
            w.setNavigationBarColor(p.bgApp);
        }

        rootScroll.setBackgroundColor(p.bgApp);
        ThemeEngine.styleRoundedBox(mainCard, p.bgPanel, p.border, cardRadius, 2);
        ThemeEngine.styleRoundedBox(tvError, 0x28F85149, p.danger, btnRadius, 2);
        tvError.setTextColor(p.danger);

        ThemeEngine.styleRoundedBox(btnSignIn, p.primary, p.primary, btnRadius, 0);
        btnSignIn.setTextColor(0xFFFFFFFF);

        styleChildrenRecursively(mainCard, p, btnRadius);
    }

    private void styleChildrenRecursively(ViewGroup group, ThemeEngine.Palette p, float inputRadius) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            Object tag = child.getTag();
            if ("text_main".equals(tag) && child instanceof TextView tv) {
                tv.setTextColor(p.textMain);
            } else if ("text_muted".equals(tag) && child instanceof TextView tv) {
                tv.setTextColor(p.textMuted);
            } else if ("input_box".equals(tag) && child instanceof EditText et) {
                et.setTextColor(p.textMain);
                et.setHintTextColor(p.textMuted);
                ThemeEngine.styleRoundedBox(et, p.bgInput, p.border, inputRadius, 2);
            } else if (child instanceof ViewGroup vg && child != savedProfilesList) {
                styleChildrenRecursively(vg, p, inputRadius);
            }
        }
    }

    private void attemptAutoLogin(ConnectionProfile defaultProfile) {
        final String baseUrl = defaultProfile.buildBaseUrl();
        tvError.setVisibility(View.GONE);
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Auto-connecting to default server (" + baseUrl + ")…");
        executeLoginRequest(defaultProfile, true, true);
    }

    private void attemptLogin() {
        String address = inpAddress.getText().toString().trim();
        String port = inpPort.getText().toString().trim();
        String username = inpUsername.getText().toString().trim();
        String password = inpPassword.getText().toString();
        boolean saveCfg = chkSaveProfile.isChecked();
        String profileName = inpProfileName.getText().toString().trim();

        tvError.setVisibility(View.GONE);

        if (address.isEmpty()) {
            showError("Server address or Ngrok URL is required.");
            return;
        }
        if (username.isEmpty() || password.isEmpty()) {
            showError("Admin username and password are required.");
            return;
        }
        if (profileName.isEmpty()) {
            profileName = address + (port.isEmpty() ? "" : ":" + port);
            inpProfileName.setText(profileName);
        }

        String profId = selectedProfileId != null ? selectedProfileId : String.valueOf(System.currentTimeMillis());
        ConnectionProfile profile = new ConnectionProfile(profId, profileName, address, port, username, password);
        final String baseUrl = profile.buildBaseUrl();

        btnSignIn.setEnabled(false);
        btnSignIn.setText("Connecting to " + baseUrl + "…");

        executeLoginRequest(profile, saveCfg, false);
    }

    private void executeLoginRequest(ConnectionProfile profile, boolean saveInProfilesList, boolean isAutoConnect) {
        final String baseUrl = profile.buildBaseUrl();
        executor.submit(() -> {
            try {
                // 1. Fetch Web App Theme first (/api/theme is public)
                ForgeApiClient.ApiResponse themeRes = ForgeApiClient.get(baseUrl, "/api/theme", null);
                if (themeRes.isOk()) {
                    try {
                        JSONObject tObj = new JSONObject(themeRes.body);
                        session.saveThemeConfig(
                                tObj.optString("colorTheme", tObj.optString("theme", "github-dark")),
                                tObj.optString("uiStyle", "forge-classic"),
                                tObj.optString("iconPack", "emoji")
                        );
                    } catch (Exception ignored) {
                    }
                }

                // 2. Authenticate with /api/auth/login
                JSONObject loginBody = new JSONObject();
                loginBody.put("username", profile.username);
                loginBody.put("password", profile.password);
                ForgeApiClient.ApiResponse loginRes = ForgeApiClient.post(baseUrl, "/api/auth/login", loginBody, null);

                runOnUiThread(() -> {
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("Sign In & Connect");
                    applyTheme();

                    if (!loginRes.isOk()) {
                        String msg = "Connection parameters are not valid or remote server is not available (" + baseUrl + ")";
                        if (loginRes.statusCode == 401) {
                            msg = "Connection parameters are not valid (invalid username or password for " + baseUrl + ")";
                        }
                        showError(msg);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        JSONObject respJson = new JSONObject(loginRes.body);
                        if (!respJson.optBoolean("ok", false)) {
                            String msg = respJson.optString("error", "Connection parameters are not valid");
                            showError(msg);
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            return;
                        }
                        String token = respJson.optString("token", "");
                        if (token.isEmpty()) {
                            String msg = "Server did not return an access token";
                            showError(msg);
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Every last established connection becomes the default connection
                        if (saveInProfilesList) {
                            session.saveProfile(profile);
                        }
                        session.setDefaultConnectionProfile(profile);
                        session.setActiveSession(baseUrl, token, profile.username);
                        Toast.makeText(this,
                                (isAutoConnect ? "Auto-connected to default server: " : "Connected (set as default): ") + baseUrl,
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, ScriptStudioActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        String msg = "Connection parameters are not valid or remote server is not available";
                        showError(msg);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("Sign In & Connect");
                    String msg = "Connection parameters are not valid or remote server is not available (" + baseUrl + ")";
                    showError(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
