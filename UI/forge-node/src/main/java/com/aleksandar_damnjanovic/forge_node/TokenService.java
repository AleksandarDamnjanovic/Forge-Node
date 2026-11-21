package com.aleksandar_damnjanovic.forge_node;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TokenService {

	private static final String HMAC_ALG = "HmacSHA256";
	private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\":\"([^\"]*)\"");
	private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\":(\\d+)");

	private final byte[] signingKey;
	private final long ttlSeconds;

	public TokenService(@Value("${auth.token.ttl-seconds:43200}") long ttlSeconds) {
		this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 43200;
		byte[] key = new byte[32];
		new SecureRandom().nextBytes(key);
		this.signingKey = key;
	}

	public long getTtlSeconds() {
		return ttlSeconds;
	}

	public String createToken(String username) {
		long now = Instant.now().getEpochSecond();
		long exp = now + ttlSeconds;
		String safeSub = username.replace("\\", "\\\\").replace("\"", "\\\"");
		String payloadJson = "{\"sub\":\"" + safeSub + "\",\"iat\":" + now + ",\"exp\":" + exp + "}";
		String payloadB64 = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
		String sigB64 = sign(payloadB64);
		return payloadB64 + "." + sigB64;
	}

	public String validateAndGetSubject(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		int dot = token.indexOf('.');
		if (dot <= 0 || dot >= token.length() - 1 || token.indexOf('.', dot + 1) != -1) {
			return null;
		}
		String payloadB64 = token.substring(0, dot);
		String sigB64 = token.substring(dot + 1);
		String expectedSig = sign(payloadB64);

		if (!MessageDigest.isEqual(
				sigB64.getBytes(StandardCharsets.UTF_8),
				expectedSig.getBytes(StandardCharsets.UTF_8))) {
			return null;
		}

		try {
			byte[] decoded = Base64.getUrlDecoder().decode(payloadB64);
			String json = new String(decoded, StandardCharsets.UTF_8);

			Matcher expMatcher = EXP_PATTERN.matcher(json);
			if (!expMatcher.find()) {
				return null;
			}
			long exp = Long.parseLong(expMatcher.group(1));
			if (Instant.now().getEpochSecond() >= exp) {
				return null;
			}

			Matcher subMatcher = SUB_PATTERN.matcher(json);
			if (!subMatcher.find()) {
				return null;
			}
			return subMatcher.group(1);
		} catch (Exception e) {
			return null;
		}
	}

	private String sign(String data) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALG);
			mac.init(new SecretKeySpec(signingKey, HMAC_ALG));
			byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to sign access token", e);
		}
	}
}
