package com.aleksandar_damnjanovic.forge_node;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter extends OncePerRequestFilter {

	private final TokenService tokenService;

	public AuthFilter(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String path = request.getRequestURI();

		// Never expose internal backend scripts or credential configs over HTTP
		if (path.endsWith(".conf") || path.endsWith(".py")) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getOutputStream().write("{\"ok\":false,\"error\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8));
			return;
		}

		if (isPublicPath(path)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = AuthController.extractToken(request);
		String subject = tokenService.validateAndGetSubject(token);

		if (subject == null) {
			if (path.startsWith("/api/")) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				response.getOutputStream().write("{\"ok\":false,\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8));
			} else {
				response.sendRedirect("/login.html");
			}
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean isPublicPath(String path) {
		return "/login".equals(path)
				|| "/login.html".equals(path)
				|| "/api/auth/login".equals(path)
				|| "/api/auth/logout".equals(path)
				|| "/api/auth/status".equals(path)
				|| "/api/theme".equals(path)
				|| "/theme.js".equals(path)
				|| "/script-editor/styles.css".equals(path)
				|| "/Forge Node Studio Icon.png".equals(path)
				|| "/Forge%20Node%20Studio%20Icon.png".equals(path)
				|| "/favicon.ico".equals(path);
	}
}
