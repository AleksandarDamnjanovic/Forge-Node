package com.aleksandar_damnjanovic.forge_node;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/au")
public class AuConfigController {

	private static final int DEFAULT_AU_PORT = 10010;
	private static final String AU_CONF_REL_PATH = "AAU/script/au configuration/au-configuration.conf";
	private static final String API_KEY_REL_PATH = "AAU/script/api/api.txt";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private volatile boolean exposeAu;
	private volatile int auPort = DEFAULT_AU_PORT;
	private volatile int serverPort = 10001;
	private volatile String apiKey = "";
	private Process localAuProcess;

	public AuConfigController(
			@Value("${expose-au:false}") String exposeAuValue,
			@Value("${server.port:10001}") int serverPort
	) {
		String ea = exposeAuValue == null ? "false" : exposeAuValue.trim();
		this.exposeAu = "true".equalsIgnoreCase(ea) || "1".equals(ea) || "yes".equalsIgnoreCase(ea);
		this.serverPort = serverPort > 0 ? serverPort : 10001;
	}

	@PostConstruct
	public synchronized void initApiKeyOnStartup() {
		ensureOrLoadApiKey();
	}

	private Path resolveApiKeyFilePath() {
		return resolveProjectRoot().resolve("AAU").resolve("script").resolve("api").resolve("api.txt");
	}

	private String generateRandomApiKey() {
		byte[] bytes = new byte[24];
		SECURE_RANDOM.nextBytes(bytes);
		return "fn_" + HexFormat.of().formatHex(bytes);
	}

	public synchronized String ensureOrLoadApiKey() {
		Path apiFile = resolveApiKeyFilePath();
		try {
			if (Files.isRegularFile(apiFile)) {
				String existing = Files.readString(apiFile, StandardCharsets.UTF_8).trim();
				if (!existing.isEmpty()) {
					this.apiKey = existing;
					syncForgeNodeSkillConfiguration(this.apiKey);
					return this.apiKey;
				}
			}
			String created = generateRandomApiKey();
			Files.createDirectories(apiFile.getParent());
			Files.writeString(apiFile, created + "\n", StandardCharsets.UTF_8);
			this.apiKey = created;
			syncForgeNodeSkillConfiguration(this.apiKey);
			return this.apiKey;
		} catch (Exception e) {
			if (this.apiKey == null || this.apiKey.isEmpty()) {
				this.apiKey = generateRandomApiKey();
			}
			return this.apiKey;
		}
	}

	public synchronized String regenerateApiKey() throws Exception {
		Path apiFile = resolveApiKeyFilePath();
		String created = generateRandomApiKey();
		Files.createDirectories(apiFile.getParent());
		Files.writeString(apiFile, created + "\n", StandardCharsets.UTF_8);
		this.apiKey = created;
		syncForgeNodeSkillConfiguration(this.apiKey);
		return this.apiKey;
	}

	public String getApiKey() {
		if (this.apiKey == null || this.apiKey.isEmpty()) {
			return ensureOrLoadApiKey();
		}
		return this.apiKey;
	}

	public boolean isValidApiKey(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return false;
		}
		String expected = getApiKey();
		if (expected == null || expected.isEmpty()) {
			return false;
		}
		return MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				candidate.trim().getBytes(StandardCharsets.UTF_8)
		);
	}

	private void syncForgeNodeSkillConfiguration(String currentApiKey) {
		if (currentApiKey == null || currentApiKey.isEmpty()) {
			return;
		}
		Path projectRoot = resolveProjectRoot();
		Path skillDir = projectRoot.resolve("forge-node-skill");
		if (!Files.isDirectory(skillDir)) {
			// Modular setup: forge-node-skill is not present in project root, do not write config in it
			return;
		}
		Path scriptsDir = skillDir.resolve("scripts");
		if (!Files.isDirectory(scriptsDir)) {
			return;
		}
		Path confPath = scriptsDir.resolve("configuration.conf");
		String url = "http://127.0.0.1:" + this.serverPort;
		if (Files.isRegularFile(confPath)) {
			try {
				for (String raw : Files.readAllLines(confPath, StandardCharsets.UTF_8)) {
					String line = raw.trim();
					if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
						continue;
					}
					int eq = line.indexOf('=');
					String k = line.substring(0, eq).trim();
					String v = line.substring(eq + 1).trim();
					if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
						v = v.substring(1, v.length() - 1);
					}
					if ("url".equals(k) && !v.isBlank()) {
						url = v.trim();
					}
				}
			} catch (Exception ignored) {
			}
		}
		String content = "url = \"" + escapeConf(url) + "\"\n"
				+ "apiKey = \"" + escapeConf(currentApiKey) + "\"\n";
		try {
			Files.writeString(confPath, content, StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}

	private Path resolveProjectRoot() {
		Path cwd = Paths.get("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && cwd != null; i++) {
			if (Files.isDirectory(cwd.resolve("AU")) || Files.isDirectory(cwd.resolve("SHARED"))) {
				return cwd;
			}
			cwd = cwd.getParent();
		}
		return Paths.get("").toAbsolutePath().normalize();
	}

	private Path resolveAuDir() {
		return resolveProjectRoot().resolve("AU");
	}

	private Path resolveAuConfigPath() {
		return resolveProjectRoot().resolve("AAU").resolve("script").resolve("au configuration").resolve("au-configuration.conf");
	}

	private Path resolveLocalMqttConfPath() {
		Path root = resolveProjectRoot();
		Path p1 = root.resolve("helpers").resolve("mqtt-configuration.conf");
		if (Files.isRegularFile(p1)) {
			return p1;
		}
		Path p2 = root.resolve("heplers").resolve("mqtt-configuration.conf");
		if (Files.isRegularFile(p2)) {
			return p2;
		}
		return p1;
	}

	private Path resolveApplicationPropertiesPath() {
		return resolveProjectRoot().resolve("UI/forge-node/src/main/resources/application.properties");
	}

	private void persistExposeAuProperty(boolean newExposeAu) {
		Path propsPath = resolveApplicationPropertiesPath();
		if (!Files.isRegularFile(propsPath)) {
			return;
		}
		try {
			List<String> lines = Files.readAllLines(propsPath, StandardCharsets.UTF_8);
			List<String> updated = new ArrayList<>();
			boolean foundExposeAu = false;
			for (String line : lines) {
				String trimmed = line.trim();
				if (trimmed.startsWith("expose-au=")) {
					updated.add("expose-au=${FN_EXPOSE_AU:" + newExposeAu + "}");
					foundExposeAu = true;
				} else {
					updated.add(line);
				}
			}
			if (!foundExposeAu) {
				updated.add("expose-au=${FN_EXPOSE_AU:" + newExposeAu + "}");
			}
			Files.writeString(propsPath, String.join("\n", updated) + "\n", StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}

	private void syncForgeNodeSkillPort(int port) {
		Path projectRoot = resolveProjectRoot();
		Path skillDir = projectRoot.resolve("forge-node-skill");
		if (!Files.isDirectory(skillDir)) {
			return;
		}
		Path skillPath = skillDir.resolve("SKILL.md");
		if (Files.isRegularFile(skillPath)) {
			try {
				String content = Files.readString(skillPath, StandardCharsets.UTF_8);
				String updated = content
						.replaceAll("port number [0-9]{4,5}", "port number " + port)
						.replaceAll("client\\.py [0-9]{4,5}", "client.py " + port);
				if (!updated.equals(content)) {
					Files.writeString(skillPath, updated, StandardCharsets.UTF_8);
				}
			} catch (Exception ignored) {
			}
		}
		syncForgeNodeSkillConfiguration(this.apiKey);
	}

	private Path resolveAuFileWithinContainer(String rawPath) {
		if (rawPath == null || rawPath.isBlank()) {
			return null;
		}
		String p = rawPath.trim();
		if (p.startsWith("\\") || (p.length() >= 2 && Character.isLetter(p.charAt(0)) && p.charAt(1) == ':')) {
			return null;
		}
		try {
			Path projectRoot = resolveProjectRoot().toAbsolutePath().normalize();
			if (p.startsWith("/")) {
				Path abs = Paths.get(p).toAbsolutePath().normalize();
				return abs.startsWith(projectRoot) ? abs : null;
			}
			Path fromConfDir = resolveAuConfigPath().getParent().resolve(p).toAbsolutePath().normalize();
			if (fromConfDir.startsWith(projectRoot) && Files.isRegularFile(fromConfDir)) {
				return fromConfDir;
			}
			Path fromAu = resolveAuDir().resolve(p).toAbsolutePath().normalize();
			if (fromAu.startsWith(projectRoot) && Files.isRegularFile(fromAu)) {
				return fromAu;
			}
			Path fromRoot = projectRoot.resolve(p).toAbsolutePath().normalize();
			if (fromRoot.startsWith(projectRoot) && Files.isRegularFile(fromRoot)) {
				return fromRoot;
			}
			if (fromConfDir.startsWith(projectRoot)) {
				return fromConfDir;
			}
			return fromAu.startsWith(projectRoot) ? fromAu : null;
		} catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> readAuConfigurationConf(Path confPath) {
		Map<String, Object> cfg = new LinkedHashMap<>();
		cfg.put("ca", "");
		cfg.put("serverKey", "");
		cfg.put("serverCertificate", "");
		cfg.put("address", "127.0.0.1");
		cfg.put("port", 8883);
		cfg.put("user", "admin");
		cfg.put("password", "");

		if (confPath == null || !Files.isRegularFile(confPath)) {
			return cfg;
		}
		try {
			for (String raw : Files.readAllLines(confPath, StandardCharsets.UTF_8)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
					continue;
				}
				int eq = line.indexOf('=');
				String key = line.substring(0, eq).trim();
				if ("certificate".equals(key)) {
					key = "ca";
				}
				String val = line.substring(eq + 1).trim();
				if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
					val = val.substring(1, val.length() - 1);
				}
				if ("port".equals(key)) {
					try {
						cfg.put("port", Integer.parseInt(val));
					} catch (NumberFormatException ignored) {
					}
				} else if (cfg.containsKey(key)) {
					cfg.put(key, val);
				}
			}
		} catch (Exception ignored) {
		}
		return cfg;
	}

	private String formatAuConfigurationConf(Map<String, Object> cfg) {
		StringBuilder sb = new StringBuilder();
		sb.append("ca = \"").append(escapeConf(cfg.get("ca"))).append("\"\n");
		sb.append("serverKey = \"").append(escapeConf(cfg.get("serverKey"))).append("\"\n");
		sb.append("serverCertificate = \"").append(escapeConf(cfg.get("serverCertificate"))).append("\"\n");
		sb.append("address = \"").append(escapeConf(cfg.get("address"))).append("\"\n");
		sb.append("port = ").append(cfg.get("port")).append("\n");
		sb.append("user = \"").append(escapeConf(cfg.get("user"))).append("\"\n");
		sb.append("password = \"").append(escapeConf(cfg.get("password"))).append("\"\n");
		return sb.toString();
	}

	private String validateAndBuildAuConfig(Map<String, Object> body, Map<String, Object> saved) {
		if (body == null) {
			return "Missing request body";
		}
		for (String key : List.of("ca", "serverKey", "serverCertificate")) {
			String val = stringVal(body.get(key));
			if (val.isEmpty()) {
				return key + " is mandatory and must not be empty";
			}
			Path resolved = resolveAuFileWithinContainer(val);
			if (resolved == null) {
				return key + " must point inside the container (e.g. " + resolveProjectRoot() + "/SHARED/...)";
			}
			if (!Files.isRegularFile(resolved)) {
				return key + " does not exist inside the container: " + resolved;
			}
			saved.put(key, resolved.toString());
		}

		String address = stringVal(body.get("address"));
		if (address.isEmpty()) {
			return "MQTT IP address is mandatory";
		}

		int port;
		try {
			port = Integer.parseInt(String.valueOf(body.get("port")).trim());
			if (port < 1 || port > 65535) {
				throw new NumberFormatException("out of range");
			}
		} catch (Exception e) {
			return "MQTT server port must be a number between 1 and 65535";
		}

		String user = stringVal(body.get("user"));
		if (user.isEmpty()) {
			return "user is mandatory";
		}

		String password = body.get("password") == null ? "" : String.valueOf(body.get("password"));
		if (password.isEmpty()) {
			return "password is mandatory";
		}

		saved.put("address", address);
		saved.put("port", port);
		saved.put("user", user);
		saved.put("password", password);
		return null;
	}

	private String validateStoredAuConfig() {
		Path confPath = resolveAuConfigPath();
		if (!Files.isRegularFile(confPath)) {
			return "AU configuration is not set (" + AU_CONF_REL_PATH + " is missing). Please configure AU first.";
		}
		Map<String, Object> cfg = readAuConfigurationConf(confPath);
		return validateAndBuildAuConfig(cfg, new LinkedHashMap<>());
	}

	public synchronized boolean isLocalAuRunning() {
		return localAuProcess != null && localAuProcess.isAlive();
	}

	@PreDestroy
	public synchronized void stopAuOnShutdown() {
		if (localAuProcess != null && localAuProcess.isAlive()) {
			localAuProcess.destroy();
		}
	}

	@GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getStatus() {
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("exposeAu", exposeAu);
		resp.put("auRunning", isLocalAuRunning());
		resp.put("auPort", auPort);
		resp.put("auConfigured", isAuConfigReady());
		resp.put("auConfigPath", AU_CONF_REL_PATH);
		resp.put("apiKey", getApiKey());
		resp.put("apiKeyPath", API_KEY_REL_PATH);
		return ResponseEntity.ok(resp);
	}

	@GetMapping(value = "/api-key", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getApiKeyEndpoint() {
		String key = getApiKey();
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"apiKey", key,
				"path", API_KEY_REL_PATH
		));
	}

	@PostMapping(value = "/api-key/new", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> createNewApiKeyEndpoint() {
		try {
			String key = regenerateApiKey();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"apiKey", key,
					"path", API_KEY_REL_PATH
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to generate new API key: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/command", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> executeAuCommand(@RequestBody(required = false) Map<String, Object> body) {
		if (!this.exposeAu) {
			return ResponseEntity.status(403).body(Map.of(
					"ok", false,
					"error", "Expose AU is disabled on this Forge Node instance."
			));
		}
		if (!isLocalAuRunning()) {
			return ResponseEntity.status(503).body(Map.of(
					"ok", false,
					"error", "AU server is not currently running. Start AU in the Forge Node Dashboard first."
			));
		}
		String command = body == null ? "" : stringVal(body.get("command"));
		if (command.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of(
					"ok", false,
					"error", "Missing 'command' in request body."
			));
		}
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("127.0.0.1", this.auPort), 5000);
			socket.setSoTimeout(15000);
			OutputStream out = socket.getOutputStream();
			out.write(command.getBytes(StandardCharsets.UTF_8));
			out.flush();

			InputStream in = socket.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int read;
			while ((read = in.read(chunk)) != -1) {
				buffer.write(chunk, 0, read);
			}
			String respText = buffer.toString(StandardCharsets.UTF_8).trim();
			if (respText.isEmpty()) {
				return ResponseEntity.status(502).body(Map.of(
						"ok", false,
						"error", "Empty response received from AU server."
				));
			}
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"response", respText
			));
		} catch (Exception e) {
			return ResponseEntity.status(502).body(Map.of(
					"ok", false,
					"error", "Failed to communicate with local AU server on 127.0.0.1:" + this.auPort + " (" + e.getMessage() + ")"
			));
		}
	}

	@PostMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> updateAuSettings(@RequestBody Map<String, Object> body) {
		if (body != null && body.containsKey("exposeAu")) {
			this.exposeAu = Boolean.parseBoolean(String.valueOf(body.get("exposeAu")));
			persistExposeAuProperty(this.exposeAu);
			if (!this.exposeAu && localAuProcess != null && localAuProcess.isAlive()) {
				localAuProcess.destroy();
				localAuProcess = null;
			}
		}
		if (body != null && body.get("auPort") != null) {
			try {
				int p = Integer.parseInt(String.valueOf(body.get("auPort")).trim());
				if (p >= 1000 && p <= 65535) {
					this.auPort = p;
					syncForgeNodeSkillPort(p);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("exposeAu", this.exposeAu);
		resp.put("auPort", this.auPort);
		resp.put("auRunning", isLocalAuRunning());
		resp.put("auConfigured", isAuConfigReady());
		return ResponseEntity.ok(resp);
	}

	@GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getAuConfig() {
		Path confPath = resolveAuConfigPath();
		boolean exists = Files.isRegularFile(confPath);
		Map<String, Object> cfg = readAuConfigurationConf(confPath);
		String validationError = validateStoredAuConfig();

		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("exists", exists);
		resp.put("configured", validationError == null);
		resp.put("validationError", validationError == null ? "" : validationError);
		resp.put("path", AU_CONF_REL_PATH);
		resp.put("localMqttConfigFound", Files.isRegularFile(resolveLocalMqttConfPath()));
		resp.put("config", cfg);
		return ResponseEntity.ok(resp);
	}

	@PostMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> saveAuConfig(@RequestBody Map<String, Object> body) {
		Map<String, Object> saved = new LinkedHashMap<>();
		String validationError = validateAndBuildAuConfig(body, saved);
		if (validationError != null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", validationError));
		}

		try {
			Path confPath = resolveAuConfigPath();
			Files.createDirectories(confPath.getParent());
			Files.writeString(confPath, formatAuConfigurationConf(saved), StandardCharsets.UTF_8);

			return ResponseEntity.ok(Map.of(
					"ok", true,
					"configured", true,
					"path", AU_CONF_REL_PATH,
					"config", saved
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to save AU configuration: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> listAuContainerFiles() {
		Path projectRoot = resolveProjectRoot();
		List<Map<String, String>> files = new ArrayList<>();
		collectFiles(projectRoot.resolve("SHARED"), "SHARED/", "SHARED", files);
		collectFiles(projectRoot.resolve("AU").resolve("certs"), "certs/", "AU/certs", files);
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"containerRoot", projectRoot.toAbsolutePath().normalize().toString(),
				"files", files
		));
	}

	private void collectFiles(Path dir, String relPrefix, String group, List<Map<String, String>> out) {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (Stream<Path> stream = Files.list(dir)) {
			stream.filter(Files::isRegularFile)
					.sorted()
					.forEach(p -> {
						String name = p.getFileName().toString();
						out.add(Map.of(
								"name", name,
								"group", group,
								"relativePath", relPrefix + name,
								"absolutePath", p.toAbsolutePath().normalize().toString()
						));
					});
		} catch (Exception ignored) {
		}
	}

	@PostMapping(value = "/sync-local-mqtt", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> syncAuFromLocalMqtt() {
		Path localMqttConf = resolveLocalMqttConfPath();
		if (!Files.isRegularFile(localMqttConf)) {
			return ResponseEntity.status(404).body(Map.of(
					"ok", false,
					"error", "helpers/mqtt-configuration.conf was not found"
			));
		}
		try {
			String content = Files.readString(localMqttConf, StandardCharsets.UTF_8);
			Map<String, String> local = parseKeyValueLines(content);
			Path projectRoot = resolveProjectRoot();

			Map<String, Object> cfg = new LinkedHashMap<>();
			cfg.put("ca", resolveCertForLocalMqtt(projectRoot, local.get("ca.crt"), "ca.crt"));
			cfg.put("serverKey", resolveCertForLocalMqtt(projectRoot, local.get("broker.key"), "broker.key"));
			cfg.put("serverCertificate", resolveCertForLocalMqtt(projectRoot, local.get("broker.crt"), "broker.crt"));

			String addr = stringVal(local.get("address"));
			if (addr.isEmpty() || "0.0.0.0".equals(addr) || "::".equals(addr)) {
				addr = "127.0.0.1";
			}
			cfg.put("address", addr);

			int p = 8883;
			String portStr = stringVal(local.get("port"));
			if (!portStr.isEmpty()) {
				try {
					int parsed = Integer.parseInt(portStr);
					if (parsed >= 1 && parsed <= 65535) {
						p = parsed;
					}
				} catch (NumberFormatException ignored) {
				}
			}
			cfg.put("port", p);

			String[] adminCreds = extractAdminCredentials(local.get("users"));
			cfg.put("user", adminCreds[0].isEmpty() ? "admin" : adminCreds[0]);
			cfg.put("password", adminCreds[1]);

			return ResponseEntity.ok(Map.of(
					"ok", true,
					"config", cfg
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to read helpers/mqtt-configuration.conf: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> startAu(@RequestBody(required = false) Map<String, Object> body) {
		if (!this.exposeAu) {
			return ResponseEntity.status(403).body(Map.of(
					"ok", false,
					"auRunning", isLocalAuRunning(),
					"auConfigured", isAuConfigReady(),
					"auPort", this.auPort,
					"error", "Expose AU is disabled. Enable 'Expose AU' first before starting the AU TCP server."
			));
		}
		int requestedPort = this.auPort;
		if (body != null && body.get("port") != null) {
			try {
				requestedPort = Integer.parseInt(String.valueOf(body.get("port")).trim());
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of(
						"ok", false,
						"auRunning", isLocalAuRunning(),
						"auConfigured", isAuConfigReady(),
						"auPort", this.auPort,
						"error", "Port must be a valid number between 1000 and 65535"
				));
			}
		}
		if (requestedPort < 1000 || requestedPort > 65535) {
			return ResponseEntity.badRequest().body(Map.of(
					"ok", false,
					"auRunning", isLocalAuRunning(),
					"auConfigured", isAuConfigReady(),
					"auPort", this.auPort,
					"error", "Port must be between 1000 and 65535"
			));
		}

		if (isLocalAuRunning()) {
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"auRunning", true,
					"auConfigured", isAuConfigReady(),
					"auPort", this.auPort,
					"message", "AU is already serving on port " + this.auPort
			));
		}

		String auConfigError = validateStoredAuConfig();
		if (auConfigError != null) {
			return ResponseEntity.badRequest().body(Map.of(
					"ok", false,
					"auRunning", false,
					"auConfigured", false,
					"auPort", requestedPort,
					"error", auConfigError
			));
		}

		Path auDir = resolveAuDir();
		Path auPy = auDir.resolve("au.py");
		if (!Files.isRegularFile(auPy)) {
			return ResponseEntity.status(404).body(Map.of(
					"ok", false,
					"auRunning", false,
					"auConfigured", true,
					"auPort", requestedPort,
					"error", "AU/au.py not found"
			));
		}

		try {
			this.auPort = requestedPort;
			syncForgeNodeSkillPort(requestedPort);

			Path startupLog = Files.createTempFile("forge-node-au-", ".log");
			ProcessBuilder pb = new ProcessBuilder("python3", "au.py", "tcp", String.valueOf(requestedPort), "127.0.0.1");
			pb.directory(auDir.toFile());
			pb.environment().remove("FN_ADMIN_USER");
			pb.environment().remove("FN_ADMIN_PASSWORD");
			pb.redirectErrorStream(true);
			pb.redirectOutput(startupLog.toFile());
			localAuProcess = pb.start();

			Thread.sleep(300);
			if (!localAuProcess.isAlive()) {
				int code = localAuProcess.exitValue();
				localAuProcess = null;
				String errOut = "";
				try {
					errOut = Files.readString(startupLog, StandardCharsets.UTF_8).trim();
					Files.deleteIfExists(startupLog);
				} catch (Exception ignored) {
				}
				String errMsg = errOut.isEmpty()
						? "AU/au.py exited immediately with code " + code
						: "AU/au.py exited with code " + code + ": " + errOut;
				return ResponseEntity.status(500).body(Map.of(
						"ok", false,
						"auRunning", false,
						"auConfigured", isAuConfigReady(),
						"auPort", requestedPort,
						"error", errMsg
				));
			}
			startupLog.toFile().deleteOnExit();

			return ResponseEntity.ok(Map.of(
					"ok", true,
					"auRunning", true,
					"auConfigured", true,
					"auPort", this.auPort
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"auRunning", false,
					"auConfigured", isAuConfigReady(),
					"auPort", requestedPort,
					"error", "Failed to start AU/au.py: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/stop", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> stopAu() {
		if (localAuProcess != null && localAuProcess.isAlive()) {
			localAuProcess.destroy();
			localAuProcess = null;
		}
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"auRunning", false,
				"auConfigured", isAuConfigReady(),
				"auPort", this.auPort
		));
	}

	private Map<String, String> parseKeyValueLines(String content) {
		Map<String, String> map = new LinkedHashMap<>();
		if (content == null) {
			return map;
		}
		for (String raw : content.split("\\R")) {
			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
				continue;
			}
			int eq = line.indexOf('=');
			map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
		}
		return map;
	}

	private String[] extractAdminCredentials(String usersRaw) {
		if (usersRaw == null || usersRaw.isBlank()) {
			return new String[] { "", "" };
		}
		String trimmed = usersRaw.trim();
		if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
		if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
		String firstUser = "";
		String firstPass = "";
		for (String part : trimmed.split(",")) {
			String seg = part.trim();
			int eq = seg.indexOf('=');
			if (eq <= 0) continue;
			String u = seg.substring(0, eq).trim();
			String p = seg.substring(eq + 1).trim();
			if (firstUser.isEmpty()) {
				firstUser = u;
				firstPass = p;
			}
			if ("admin".equalsIgnoreCase(u)) {
				return new String[] { u, p };
			}
		}
		return new String[] { firstUser, firstPass };
	}

	private String resolveCertForLocalMqtt(Path projectRoot, String configuredPath, String defaultFileName) {
		if (configuredPath != null && !configuredPath.isBlank()) {
			try {
				Path p = Paths.get(configuredPath.trim());
				if (!p.isAbsolute()) {
					p = projectRoot.resolve(p).normalize();
				}
				if (Files.isRegularFile(p) && p.normalize().startsWith(projectRoot.normalize())) {
					return p.toAbsolutePath().normalize().toString();
				}
			} catch (Exception ignored) {
			}
		}
		Path shared = projectRoot.resolve("SHARED").resolve(defaultFileName).toAbsolutePath().normalize();
		if (Files.isRegularFile(shared)) {
			return shared.toString();
		}
		return projectRoot.resolve("AU").resolve("certs").resolve(defaultFileName).toAbsolutePath().normalize().toString();
	}

	private String stringVal(Object obj) {
		return obj == null ? "" : String.valueOf(obj).trim();
	}

	private String escapeConf(Object obj) {
		String s = obj == null ? "" : String.valueOf(obj);
		return s.replace("\"", "\\\"");
	}

	// --- Package-private helpers for InstanceConfigController ---

	boolean isExposeAu() {
		return exposeAu;
	}

	int getAuPort() {
		return auPort;
	}

	boolean isAuConfigReady() {
		return validateStoredAuConfig() == null;
	}

	synchronized void setAuSettingsInMemory(boolean exposeAu, int auPort) {
		this.exposeAu = exposeAu;
		if (auPort >= 1000 && auPort <= 65535) {
			this.auPort = auPort;
			syncForgeNodeSkillPort(auPort);
		}
	}

	Map<String, Object> getAuConfigSnapshot() {
		return readAuConfigurationConf(resolveAuConfigPath());
	}

	synchronized void restorePreconfiguredAuConfig(Map<String, Object> auCfgFields) {
		if (auCfgFields == null || auCfgFields.isEmpty()) {
			return;
		}
		Map<String, Object> saved = new LinkedHashMap<>();
		if (validateAndBuildAuConfig(auCfgFields, saved) == null) {
			try {
				Path confPath = resolveAuConfigPath();
				Files.createDirectories(confPath.getParent());
				Files.writeString(confPath, formatAuConfigurationConf(saved), StandardCharsets.UTF_8);
			} catch (Exception ignored) {
			}
		}
	}

	ResponseEntity<Map<String, Object>> startAuForStartup(int port) {
		return startAu(Map.of("port", port));
	}
}
