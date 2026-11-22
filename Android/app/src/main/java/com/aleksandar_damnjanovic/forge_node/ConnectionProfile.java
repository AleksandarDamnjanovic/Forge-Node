package com.aleksandar_damnjanovic.forge_node;

import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionProfile {
    public String id;
    public String profileName;
    public String address;
    public String port;
    public String username;
    public String password;

    public ConnectionProfile(String id, String profileName, String address, String port, String username, String password) {
        this.id = id;
        this.profileName = profileName;
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String buildBaseUrl() {
        String rawAddr = address == null ? "" : address.trim();
        while (rawAddr.endsWith("/")) {
            rawAddr = rawAddr.substring(0, rawAddr.length() - 1);
        }
        String rawPort = port == null ? "" : port.trim();

        boolean hasScheme = rawAddr.startsWith("http://") || rawAddr.startsWith("https://");
        if (!hasScheme) {
            if (rawAddr.contains(".ngrok") || rawAddr.endsWith(".app") || rawAddr.endsWith(".dev") || rawAddr.endsWith(".io")) {
                rawAddr = "https://" + rawAddr;
            } else {
                rawAddr = "http://" + rawAddr;
            }
        }

        if (!rawPort.isEmpty()) {
            // Check if address already has a port after scheme
            int schemeIdx = rawAddr.indexOf("://");
            String hostPart = schemeIdx != -1 ? rawAddr.substring(schemeIdx + 3) : rawAddr;
            if (!hostPart.contains(":")) {
                rawAddr = rawAddr + ":" + rawPort;
            }
        }
        return rawAddr;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("profileName", profileName);
        obj.put("address", address);
        obj.put("port", port);
        obj.put("username", username);
        obj.put("password", password);
        return obj;
    }

    public static ConnectionProfile fromJson(JSONObject obj) {
        return new ConnectionProfile(
                obj.optString("id", String.valueOf(System.currentTimeMillis())),
                obj.optString("profileName", "Default"),
                obj.optString("address", "192.168.0.24"),
                obj.optString("port", "10001"),
                obj.optString("username", "admin"),
                obj.optString("password", "")
        );
    }
}
