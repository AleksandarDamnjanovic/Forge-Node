package com.aleksandar_damnjanovic.forge_node;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static final String PREF_NAME = "forge_node_android_prefs";
    private static final String KEY_PROFILES = "saved_profiles_v1";
    private static final String KEY_LAST_PROFILE_ID = "last_profile_id";
    private static final String KEY_DEFAULT_PROFILE_JSON = "default_profile_json_v1";
    private static final String KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled_v1";
    private static final String KEY_ACTIVE_BASE_URL = "active_base_url";
    private static final String KEY_ACTIVE_TOKEN = "active_token";
    private static final String KEY_ACTIVE_USERNAME = "active_username";
    private static final String KEY_COLOR_THEME = "color_theme";
    private static final String KEY_UI_STYLE = "ui_style";
    private static final String KEY_ICON_PACK = "icon_pack";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<ConnectionProfile> getSavedProfiles() {
        List<ConnectionProfile> list = new ArrayList<>();
        String raw = prefs.getString(KEY_PROFILES, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    list.add(ConnectionProfile.fromJson(o));
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void saveProfile(ConnectionProfile profile) {
        List<ConnectionProfile> existing = getSavedProfiles();
        boolean replaced = false;
        for (int i = 0; i < existing.size(); i++) {
            ConnectionProfile p = existing.get(i);
            if (p.id.equals(profile.id) || p.profileName.equalsIgnoreCase(profile.profileName)) {
                profile.id = p.id;
                existing.set(i, profile);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            existing.add(0, profile);
        }
        persistProfiles(existing);
        setLastProfileId(profile.id);
    }

    public void deleteProfile(String id) {
        List<ConnectionProfile> existing = getSavedProfiles();
        existing.removeIf(p -> p.id.equals(id));
        persistProfiles(existing);
    }

    private void persistProfiles(List<ConnectionProfile> profiles) {
        JSONArray arr = new JSONArray();
        for (ConnectionProfile p : profiles) {
            try {
                arr.put(p.toJson());
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply();
    }

    public void setLastProfileId(String id) {
        prefs.edit().putString(KEY_LAST_PROFILE_ID, id).apply();
    }

    public String getLastProfileId() {
        return prefs.getString(KEY_LAST_PROFILE_ID, "");
    }

    public void setDefaultConnectionProfile(ConnectionProfile profile) {
        if (profile == null) return;
        try {
            prefs.edit()
                    .putString(KEY_DEFAULT_PROFILE_JSON, profile.toJson().toString())
                    .putString(KEY_LAST_PROFILE_ID, profile.id)
                    .putBoolean(KEY_AUTO_LOGIN_ENABLED, true)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public ConnectionProfile getDefaultConnectionProfile() {
        String raw = prefs.getString(KEY_DEFAULT_PROFILE_JSON, "");
        if (raw != null && !raw.isEmpty()) {
            try {
                return ConnectionProfile.fromJson(new JSONObject(raw));
            } catch (Exception ignored) {
            }
        }
        List<ConnectionProfile> saved = getSavedProfiles();
        if (!saved.isEmpty()) {
            String lastId = getLastProfileId();
            for (ConnectionProfile cp : saved) {
                if (cp.id.equals(lastId)) {
                    return cp;
                }
            }
            return saved.get(0);
        }
        return null;
    }

    public boolean isAutoLoginEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, true);
    }

    public void setAutoLoginEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled).apply();
    }

    public void setActiveSession(String baseUrl, String token, String username) {
        prefs.edit()
                .putString(KEY_ACTIVE_BASE_URL, baseUrl)
                .putString(KEY_ACTIVE_TOKEN, token)
                .putString(KEY_ACTIVE_USERNAME, username)
                .putBoolean(KEY_AUTO_LOGIN_ENABLED, true)
                .apply();
    }

    public void clearActiveSession() {
        prefs.edit()
                .remove(KEY_ACTIVE_TOKEN)
                .putBoolean(KEY_AUTO_LOGIN_ENABLED, false)
                .apply();
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_ACTIVE_BASE_URL, "");
    }

    public String getToken() {
        return prefs.getString(KEY_ACTIVE_TOKEN, "");
    }

    public String getUsername() {
        return prefs.getString(KEY_ACTIVE_USERNAME, "admin");
    }

    public void saveThemeConfig(String colorTheme, String uiStyle, String iconPack) {
        SharedPreferences.Editor ed = prefs.edit();
        if (colorTheme != null && !colorTheme.isEmpty()) ed.putString(KEY_COLOR_THEME, colorTheme);
        if (uiStyle != null && !uiStyle.isEmpty()) ed.putString(KEY_UI_STYLE, uiStyle);
        if (iconPack != null && !iconPack.isEmpty()) ed.putString(KEY_ICON_PACK, iconPack);
        ed.apply();
    }

    public String getColorTheme() {
        return prefs.getString(KEY_COLOR_THEME, "github-dark");
    }

    public String getUiStyle() {
        return prefs.getString(KEY_UI_STYLE, "forge-classic");
    }

    public String getIconPack() {
        return prefs.getString(KEY_ICON_PACK, "emoji");
    }
}
