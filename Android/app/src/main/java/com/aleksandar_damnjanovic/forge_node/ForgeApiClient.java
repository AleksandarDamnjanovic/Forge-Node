package com.aleksandar_damnjanovic.forge_node;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ForgeApiClient {

    public static class ApiResponse {
        public final int statusCode;
        public final String body;
        public final String error;

        public ApiResponse(int statusCode, String body, String error) {
            this.statusCode = statusCode;
            this.body = body;
            this.error = error;
        }

        public boolean isOk() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    public static ApiResponse get(String baseUrl, String path, String token) {
        return request("GET", baseUrl, path, null, token);
    }

    public static ApiResponse post(String baseUrl, String path, JSONObject jsonBody, String token) {
        return request("POST", baseUrl, path, jsonBody != null ? jsonBody.toString() : "{}", token);
    }

    public static ApiResponse request(String method, String baseUrl, String path, String bodyStr, String token) {
        HttpURLConnection conn = null;
        try {
            String fullUrl = baseUrl + (path.startsWith("/") ? path : "/" + path);
            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("ngrok-skip-browser-warning", "true");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Cookie", "FN_ACCESS_TOKEN=" + token);
            }
            if ("POST".equalsIgnoreCase(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] bytes = (bodyStr == null ? "{}" : bodyStr).getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                    os.flush();
                }
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            String respBody = "";
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                respBody = sb.toString().trim();
            }
            return new ApiResponse(code, respBody, null);
        } catch (Exception e) {
            return new ApiResponse(0, "", e.getMessage() != null ? e.getMessage() : e.toString());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
