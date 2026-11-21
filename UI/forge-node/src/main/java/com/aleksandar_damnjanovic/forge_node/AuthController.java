package com.aleksandar_damnjanovic.forge_node;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	public static final String TOKEN_COOKIE_NAME = "FN_ACCESS_TOKEN";

	private final String adminUsername;
	private final String adminPassword;
	private final TokenService tokenService;

	public AuthController(
			@Value("${admin.username:}") String adminUsername,
			@Value("${admin.password:}") String adminPassword,
			TokenService tokenService) {
		this.adminUsername = adminUsername == null ? "" : adminUsername;
		this.adminPassword = adminPassword == null ? "" : adminPassword;
		this.tokenService = tokenService;
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, String> body) {
		if (adminUsername.isBlank() || adminPassword.isBlank()) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Server admin credentials are not configured (FN_ADMIN_USER / FN_ADMIN_PASSWORD)"
			));
		}

		String username = body != null && body.get("username") != null ? body.get("username") : "";
		String password = body != null && body.get("password") != null ? body.get("password") : "";

		boolean userMatches = constantTimeEquals(username, adminUsername);
		boolean passMatches = constantTimeEquals(password, adminPassword);

		if (!userMatches || !passMatches) {
			return ResponseEntity.status(401).body(Map.of(
					"ok", false,
					"error", "Invalid username or password"
			));
		}

		String token = tokenService.createToken(adminUsername);
		long ttl = tokenService.getTtlSeconds();

		ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, token)
				.httpOnly(true)
				.path("/")
				.sameSite("Lax")
				.maxAge(ttl)
				.build();

		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("username", adminUsername);
		resp.put("token", token);
		resp.put("expiresIn", ttl);

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(resp);
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, Object>> logout() {
		ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, "")
				.httpOnly(true)
				.path("/")
				.sameSite("Lax")
				.maxAge(0)
				.build();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(Map.of("ok", true));
	}

	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> status(HttpServletRequest request) {
		String token = extractToken(request);
		String subject = tokenService.validateAndGetSubject(token);
		if (subject != null) {
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"authenticated", true,
					"username", subject
			));
		}
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"authenticated", false
		));
	}

	public static String extractToken(HttpServletRequest request) {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7).trim();
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (TOKEN_COOKIE_NAME.equals(c.getName())) {
					return c.getValue();
				}
			}
		}
		return null;
	}

	private boolean constantTimeEquals(String a, String b) {
		return MessageDigest.isEqual(
				a.getBytes(StandardCharsets.UTF_8),
				b.getBytes(StandardCharsets.UTF_8)
		);
	}
}
