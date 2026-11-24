package com.aleksandar_damnjanovic.forge_node;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class MqttBridgeController {

	private static final int DEFAULT_BRIDGE_PORT = 8765;

	private volatile boolean remoteMqtt = true;
	private volatile boolean exposeMqtt;
	private volatile boolean localConfigApplied = false;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final HttpClient httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(5))
			.build();
	private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
	private Process bridgeProcess;
	private volatile int activeBridgePort = DEFAULT_BRIDGE_PORT;

	public MqttBridgeController(
			@Value("${expose-mqtt:true}") String exposeMqttValue
	) {
		String em = exposeMqttValue == null ? "true" : exposeMqttValue.trim();
		this.exposeMqtt = "true".equalsIgnoreCase(em) || "1".equals(em) || "yes".equals(em);
	}

	@PostConstruct
	public void initDefaultsOnStartup() {
		try {
			Files.createDirectories(resolveThemesDir());
			Files.createDirectories(resolveFullConfigDir());
		} catch (Exception ignored) {
		}
		applyDefaultMonitorConfigIfPresent();
		applyDefaultLocalMqttConfigIfPresent();
		this.remoteMqtt = resolveInitialRemoteMqttFromConfig();
	}

	private boolean resolveInitialRemoteMqttFromConfig() {
		Path confPath = resolveMonitorDir().resolve("mqtt.conf");
		if (Files.isRegularFile(confPath)) {
			Map<String, Object> cfg = loadMqttConfFile(confPath);
			Object explicit = cfg.get("remote_mqtt");
			if (explicit instanceof Boolean b) {
				return b;
			}
		}
		if (!readDefaultLocalMqttConfigName().isEmpty() || isLocalConfigApplied() || isMosquittoServiceActive()) {
			return false;
		}
		return true;
	}

	private Path resolveMonitorConfigsDir() {
		return resolveProjectRoot().resolve("AAU").resolve("script").resolve("mqtt-monitor-configurations");
	}

	private Path resolveLocalMqttConfigsDir() {
		return resolveProjectRoot().resolve("AAU").resolve("script").resolve("local-mqtt-server");
	}

	private Path resolveThemesDir() {
		return resolveProjectRoot().resolve("AAU").resolve("script").resolve("themes");
	}

	Path resolveFullConfigDir() {
		return resolveProjectRoot().resolve("AAU").resolve("script").resolve("full-configuration");
	}

	private String readDefaultConfigName(Path dir) {
		Path defaultFile = dir.resolve("default-configuration");
		if (!Files.isRegularFile(defaultFile)) {
			return "";
		}
		try {
			return Files.readString(defaultFile, StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			return "";
		}
	}

	private String readDefaultMonitorConfigName() {
		return readDefaultConfigName(resolveMonitorConfigsDir());
	}

	private String readDefaultLocalMqttConfigName() {
		return readDefaultConfigName(resolveLocalMqttConfigsDir());
	}

	private void applyDefaultMonitorConfigIfPresent() {
		String defaultName = sanitizeConfFileName(readDefaultMonitorConfigName());
		if (defaultName == null) {
			return;
		}
		Path dir = resolveMonitorConfigsDir();
		Path target = dir.resolve(defaultName).normalize();
		if (!target.startsWith(dir.normalize()) || !Files.isRegularFile(target)) {
			return;
		}
		try {
			applyRemoteConfigFromContent(Files.readString(target, StandardCharsets.UTF_8));
		} catch (Exception ignored) {
		}
	}

	private void applyDefaultLocalMqttConfigIfPresent() {
		String defaultName = sanitizeConfFileName(readDefaultLocalMqttConfigName());
		if (defaultName == null) {
			return;
		}
		Path dir = resolveLocalMqttConfigsDir();
		Path target = dir.resolve(defaultName).normalize();
		if (!target.startsWith(dir.normalize()) || !Files.isRegularFile(target)) {
			return;
		}
		try {
			applyLocalConfigFromContent(Files.readString(target, StandardCharsets.UTF_8));
		} catch (Exception ignored) {
		}
	}

	Path resolveProjectRoot() {
		Path cwd = Paths.get("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && cwd != null; i++) {
			if (Files.isDirectory(cwd.resolve("SHARED")) || Files.isDirectory(cwd.resolve("AAU"))) {
				return cwd;
			}
			cwd = cwd.getParent();
		}
		return Paths.get("").toAbsolutePath().normalize();
	}

	private Path resolveMonitorDir() {
		return resolveProjectRoot().resolve("UI/forge-node/src/main/resources/static/mqtt-monitor");
	}

	private Path resolveSharedCert(String certFileName) {
		Path sharedCert = resolveProjectRoot().resolve("SHARED").resolve(certFileName);
		if (Files.exists(sharedCert)) {
			return sharedCert;
		}
		return resolveProjectRoot().resolve("AAU/certs").resolve(certFileName);
	}

	private String bridgeBase() {
		return "http://127.0.0.1:" + activeBridgePort;
	}

	private boolean isBridgeListening(int port) {
		try (Socket s = new Socket()) {
			s.connect(new InetSocketAddress("127.0.0.1", port), 300);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isBridgeListening() {
		return isBridgeListening(activeBridgePort);
	}

	@GetMapping(value = "/bridge/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getBridgeStatus() {
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"running", isBridgeListening(),
				"port", activeBridgePort
		));
	}

	@PostMapping(value = "/bridge/start", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> startBridge(@RequestBody(required = false) Map<String, Object> body) {
		int requestedPort = activeBridgePort;
		if (body != null && body.get("port") != null) {
			try {
				requestedPort = Integer.parseInt(String.valueOf(body.get("port")).trim());
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of(
						"ok", false,
						"running", isBridgeListening(),
						"port", activeBridgePort,
						"error", "Bridge port must be a valid number (1000-65535)"
				));
			}
		}
		if (requestedPort < 1000 || requestedPort > 65535) {
			return ResponseEntity.badRequest().body(Map.of(
					"ok", false,
					"running", isBridgeListening(),
					"port", activeBridgePort,
					"error", "Bridge port must be between 1000 and 65535"
			));
		}

		if (bridgeProcess != null && bridgeProcess.isAlive() && isBridgeListening(activeBridgePort)) {
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"running", true,
					"port", activeBridgePort,
					"message", "MQTT bridge is already running on port " + activeBridgePort
			));
		}

		if (isBridgeListening(requestedPort)) {
			return ResponseEntity.status(409).body(Map.of(
					"ok", false,
					"running", false,
					"port", requestedPort,
					"error", "Port " + requestedPort + " is already in use. Please choose another bridge port."
			));
		}

		try {
			Path serverPy = resolveMonitorDir().resolve("server.py");
			if (!Files.exists(serverPy)) {
				serverPy = resolveProjectRoot().resolve("UI/mqtt-monitor/server.py");
			}
			if (!Files.exists(serverPy)) {
				return ResponseEntity.status(404).body(Map.of(
						"ok", false,
						"running", false,
						"port", requestedPort,
						"error", "server.py not found"
				));
			}

			Path logFile = Files.createTempFile("forge-bridge-", ".log");
			ProcessBuilder pb = new ProcessBuilder("python3", serverPy.toString(), String.valueOf(requestedPort));
			pb.directory(serverPy.getParent().toFile());
			pb.environment().remove("FN_ADMIN_USER");
			pb.environment().remove("FN_ADMIN_PASSWORD");
			pb.redirectErrorStream(true);
			pb.redirectOutput(logFile.toFile());
			bridgeProcess = pb.start();

			// Wait up to 2 seconds for the bridge to bind to requestedPort
			for (int i = 0; i < 20; i++) {
				if (isBridgeListening(requestedPort)) {
					break;
				}
				if (!bridgeProcess.isAlive()) {
					break;
				}
				Thread.sleep(100);
			}

			boolean listening = isBridgeListening(requestedPort) && bridgeProcess.isAlive();
			if (!listening) {
				if (bridgeProcess != null && bridgeProcess.isAlive()) {
					bridgeProcess.destroyForcibly();
				}
				bridgeProcess = null;
				String details = "";
				try {
					details = Files.readString(logFile, StandardCharsets.UTF_8).trim();
					if (details.length() > 300) {
						details = details.substring(0, 300) + "...";
					}
				} catch (Exception ignored) {
				}
				Files.deleteIfExists(logFile);
				String errMsg = details.isEmpty()
						? "MQTT bridge did not start within 2 seconds on port " + requestedPort + " and was stopped."
						: "MQTT bridge failed on port " + requestedPort + ": " + details;
				return ResponseEntity.status(500).body(Map.of(
						"ok", false,
						"running", false,
						"port", requestedPort,
						"error", errMsg
				));
			}

			Files.deleteIfExists(logFile);
			activeBridgePort = requestedPort;
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"running", true,
					"port", activeBridgePort
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"running", false,
					"port", requestedPort,
					"error", e.getMessage()
			));
		}
	}

	@PostMapping(value = "/bridge/stop", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> stopBridgeEndpoint() {
		stopBridgeGently();
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"running", isBridgeListening(),
				"port", activeBridgePort
		));
	}

	private void stopBridgeGently() {
		if (bridgeProcess != null && bridgeProcess.isAlive()) {
			try {
				OutputStream os = bridgeProcess.getOutputStream();
				os.write("exit\n".getBytes(StandardCharsets.UTF_8));
				os.flush();
				bridgeProcess.waitFor(2, TimeUnit.SECONDS);
			} catch (Exception ignored) {
			}
			if (bridgeProcess.isAlive()) {
				bridgeProcess.destroy();
			}
			bridgeProcess = null;
		}
	}

	@PreDestroy
	public synchronized void stopBridge() {
		sseExecutor.shutdownNow();
		stopBridgeGently();
	}

	private Path resolveDefaultMonitorScriptConfPath() {
		return resolveProjectRoot().resolve("AAU").resolve("script").resolve("default-script.conf");
	}

	private String sanitizeFnFileName(String rawName) {
		if (rawName == null || rawName.isBlank()) {
			return null;
		}
		String clean = Path.of(rawName.trim()).getFileName().toString().trim();
		if (clean.isEmpty() || clean.equals(".") || clean.equals("..")
				|| clean.contains("/") || clean.contains("\\")
				|| !clean.toLowerCase().endsWith(".fn")) {
			return null;
		}
		return clean;
	}

	private String readDefaultMonitorScriptName() {
		Path confPath = resolveDefaultMonitorScriptConfPath();
		if (!Files.isRegularFile(confPath)) {
			return "";
		}
		try {
			for (String raw : Files.readAllLines(confPath, StandardCharsets.UTF_8)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String candidate = line.contains("=")
						? line.substring(line.indexOf('=') + 1).trim()
						: line;
				String clean = sanitizeFnFileName(candidate);
				if (clean != null) {
					return clean;
				}
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	@GetMapping(value = "/working-fn/default", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getDefaultWorkingFn() {
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"defaultScript", readDefaultMonitorScriptName(),
				"path", "AAU/script/default-script.conf"
		));
	}

	@PostMapping(value = "/working-fn/default", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> setDefaultWorkingFn(@RequestBody Map<String, Object> body) {
		String rawName = body != null && body.get("fileName") != null ? String.valueOf(body.get("fileName")) : "";
		String fileName = sanitizeFnFileName(rawName);
		if (fileName == null) {
			return ResponseEntity.badRequest().body(Map.of(
					"ok", false,
					"error", "Invalid script filename (must end with .fn)"
			));
		}
		Path scriptDir = resolveProjectRoot().resolve("AAU").resolve("script");
		Path scriptFile = scriptDir.resolve(fileName).normalize();
		if (!scriptFile.startsWith(scriptDir.normalize()) || !Files.isRegularFile(scriptFile)) {
			return ResponseEntity.status(404).body(Map.of(
					"ok", false,
					"error", "Script not found in AAU/script/: " + fileName
			));
		}
		try {
			Files.createDirectories(scriptDir);
			Files.writeString(resolveDefaultMonitorScriptConfPath(), "default_script=" + fileName + "\n", StandardCharsets.UTF_8);
			applyDefaultScriptAsOperational();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"defaultScript", fileName,
					"path", "AAU/script/default-script.conf"
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to save default script config: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/working-fn", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getWorkingFn() {
		Path root = resolveProjectRoot();
		String defaultScript = readDefaultMonitorScriptName();
		List<Path> candidates = new ArrayList<>();
		if (!defaultScript.isEmpty()) {
			candidates.add(root.resolve("AAU/script").resolve(defaultScript));
		}
		candidates.addAll(List.of(
				root.resolve("AAU/script/working.fn"),
				root.resolve("AAU/script/script.fn"),
				root.resolve("AAU/scripts/script.fn")
		));
		Path scriptDir = root.resolve("AAU/script");
		if (Files.isDirectory(scriptDir)) {
			try (Stream<Path> stream = Files.list(scriptDir)) {
				stream.filter(Files::isRegularFile)
						.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".fn"))
						.sorted()
						.forEach(candidates::add);
			} catch (Exception ignored) {
			}
		}
		for (Path candidate : candidates) {
			if (Files.isRegularFile(candidate)) {
				try {
					String content = Files.readString(candidate, StandardCharsets.UTF_8);
					Map<String, Object> body = new LinkedHashMap<>();
					body.put("ok", true);
					body.put("path", root.relativize(candidate).toString().replace('\\', '/'));
					body.put("defaultScript", defaultScript);
					body.put("content", content);
					return ResponseEntity.ok(body);
				} catch (Exception e) {
					return ResponseEntity.status(500).body(Map.of("ok", false, "error", e.getMessage()));
				}
			}
		}
		return ResponseEntity.status(404).body(Map.of("ok", false, "error", "No .fn script found in AAU/script/"));
	}

	private Path resolveDefaultFnScriptPath() {
		Path root = resolveProjectRoot();
		List<Path> candidates = List.of(
				root.resolve("AAU/script/script.fn"),
				root.resolve("AAU/scripts/script.fn"),
				root.resolve("AAU/script/working.fn")
		);
		for (Path p : candidates) {
			if (Files.isRegularFile(p)) {
				return p;
			}
		}
		return null;
	}

	private List<String> extractTopicsFromFnScript(String scriptText) {
		List<String> topics = new ArrayList<>();
		if (scriptText == null || scriptText.isBlank()) {
			return topics;
		}
		for (String unit : scriptText.split("}")) {
			if (!unit.contains("MASTER") && !unit.contains("SLAVE") && !unit.contains("SHUNT") && !unit.contains("BRIDGE")) {
				continue;
			}
			int closeParen = unit.indexOf(')');
			if (closeParen == -1) {
				continue;
			}
			String header = unit.substring(0, closeParen).trim();
			List<String> quoted = new ArrayList<>();
			int idx = 0;
			while (idx < header.length()) {
				int q1 = header.indexOf('"', idx);
				if (q1 == -1) break;
				int q2 = header.indexOf('"', q1 + 1);
				if (q2 == -1) break;
				quoted.add(header.substring(q1 + 1, q2).trim());
				idx = q2 + 1;
			}
			if (quoted.size() >= 2) {
				String topic = quoted.get(1);
				if (!topic.isEmpty() && !"admin".equalsIgnoreCase(topic) && !topics.contains(topic)) {
					topics.add(topic);
				}
			}
		}
		return topics;
	}

	private Path resolveHelpersDir() {
		Path root = resolveProjectRoot();
		Path helpers = root.resolve("helpers");
		if (Files.isDirectory(helpers)) {
			return helpers;
		}
		Path heplers = root.resolve("heplers");
		if (Files.isDirectory(heplers)) {
			return heplers;
		}
		return helpers;
	}

	private Path resolveLocalMqttConfPath() {
		Path root = resolveProjectRoot();
		Path p1 = root.resolve("helpers").resolve("mqtt-configuration.conf");
		if (Files.exists(p1)) {
			return p1;
		}
		Path p2 = root.resolve("heplers").resolve("mqtt-configuration.conf");
		if (Files.exists(p2)) {
			return p2;
		}
		return resolveHelpersDir().resolve("mqtt-configuration.conf");
	}

	private Map<String, String> parseLocalMqttConf(String content) {
		Map<String, String> parsed = new LinkedHashMap<>();
		if (content == null || content.isBlank()) {
			return parsed;
		}
		for (String raw : content.split("\\r?\\n")) {
			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			int eq = line.indexOf('=');
			if (eq > 0) {
				String key = line.substring(0, eq).trim();
				String val = line.substring(eq + 1).trim();
				if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
					val = val.substring(1, val.length() - 1);
				}
				parsed.put(key, val);
			} else {
				parsed.put(line, "");
			}
		}
		return parsed;
	}

	private boolean isLocalConfigApplied() {
		if (localConfigApplied) {
			return true;
		}
		return Files.exists(Paths.get("/etc/mosquitto/users.conf"))
				&& Files.exists(Paths.get("/etc/mosquitto/certs/broker.crt"));
	}

	private boolean isMosquittoServiceActive() {
		try {
			ProcessBuilder pb = new ProcessBuilder("systemctl", "is-active", "mosquitto");
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			proc.waitFor(5, TimeUnit.SECONDS);
			if (proc.exitValue() == 0 && "active".equalsIgnoreCase(out)) {
				return true;
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	private void runCommandWithTimeout(List<String> command, Path workingDir) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(command);
		if (workingDir != null && Files.isDirectory(workingDir)) {
			pb.directory(workingDir.toFile());
		}
		pb.environment().remove("FN_ADMIN_USER");
		pb.environment().remove("FN_ADMIN_PASSWORD");
		pb.redirectErrorStream(true);
		Process proc = pb.start();
		try {
			proc.getOutputStream().close();
		} catch (Exception ignored) {
		}
		String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		boolean finished = proc.waitFor(30, TimeUnit.SECONDS);
		if (!finished) {
			proc.destroyForcibly();
			throw new RuntimeException("Command timed out: " + String.join(" ", command));
		}
		if (proc.exitValue() != 0) {
			throw new RuntimeException("Command failed (" + proc.exitValue() + "): " + output.trim());
		}
	}

	private synchronized void applyLocalMosquittoSetupInternal() throws Exception {
		Path localConfPath = resolveLocalMqttConfPath();
		if (!Files.exists(localConfPath) || !Files.isRegularFile(localConfPath)) {
			throw new RuntimeException("helpers/mqtt-configuration.conf does not exist");
		}
		String content = Files.readString(localConfPath, StandardCharsets.UTF_8);
		Map<String, String> details = parseLocalMqttConf(content);
		String address = details.getOrDefault("address", "0.0.0.0").trim();
		if (!this.exposeMqtt) {
			address = "127.0.0.1";
		} else if (address.isEmpty()) {
			address = "0.0.0.0";
		}

		Path rootDir = resolveProjectRoot();
		Path removeOldScript = resolveHelperScript("removeOldMosquittoConf.sh");
		Path startScript = resolveHelperScript("setupMosquitto_start.sh");
		Path usersScript = resolveHelperScript("setupMosquitto_users.sh");
		Path endScript = resolveHelperScript("setupMosquitto_end.sh");

		if (Files.exists(removeOldScript)) {
			runCommandWithTimeout(List.of("bash", removeOldScript.toAbsolutePath().toString()), rootDir);
		}
		if (Files.exists(startScript)) {
			runCommandWithTimeout(List.of("bash", startScript.toAbsolutePath().toString()), rootDir);
		}
		if (Files.exists(usersScript)) {
			runCommandWithTimeout(List.of("bash", usersScript.toAbsolutePath().toString()), rootDir);
		}
		if (Files.exists(endScript)) {
			runCommandWithTimeout(List.of("bash", endScript.toAbsolutePath().toString(), address), rootDir);
		}
		localConfigApplied = true;
	}

	private synchronized void startLocalMosquittoServerInternal() throws Exception {
		Path rootDir = resolveProjectRoot();
		runCommandWithTimeout(List.of("systemctl", "enable", "mosquitto"), rootDir);
		runCommandWithTimeout(List.of("systemctl", "start", "mosquitto"), rootDir);
	}

	private synchronized void stopLocalMosquittoServerInternal() throws Exception {
		Path rootDir = resolveProjectRoot();
		runCommandWithTimeout(List.of("systemctl", "stop", "mosquitto"), rootDir);
	}

	@GetMapping(value = "/mqtt/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getMqttStatus() {
		Path confPath = resolveMonitorDir().resolve("mqtt.conf");
		Map<String, Object> cfg = loadMqttConfFile(confPath);

		Path localConfPath = resolveLocalMqttConfPath();
		boolean localConfigExists = Files.exists(localConfPath) && Files.isRegularFile(localConfPath);
		String localConfigContent = "";
		Map<String, String> localConfigDetails = new LinkedHashMap<>();
		if (localConfigExists) {
			try {
				localConfigContent = Files.readString(localConfPath, StandardCharsets.UTF_8);
				localConfigDetails = parseLocalMqttConf(localConfigContent);
			} catch (Exception ignored) {
			}
		}

		Path sharedDir = resolveSharedDir();
		Map<String, String> defaultCerts = new LinkedHashMap<>();
		defaultCerts.put("caCert", sharedDir.resolve("ca.crt").toAbsolutePath().normalize().toString());
		defaultCerts.put("brokerCert", sharedDir.resolve("broker.crt").toAbsolutePath().normalize().toString());
		defaultCerts.put("brokerKey", sharedDir.resolve("broker.key").toAbsolutePath().normalize().toString());

		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("remoteMqtt", remoteMqtt);
		resp.put("exposeMqtt", exposeMqtt);
		resp.put("confPath", "UI/forge-node/src/main/resources/static/mqtt-monitor/mqtt.conf");
		resp.put("config", cfg);
		resp.put("localConfigExists", localConfigExists);
		resp.put("localConfigApplied", localConfigExists && isLocalConfigApplied());
		resp.put("localMqttRunning", isMosquittoServiceActive());
		resp.put("localConfigPath", "helpers/mqtt-configuration.conf");
		resp.put("localConfigContent", localConfigContent);
		resp.put("localConfigDetails", localConfigDetails);
		resp.put("localDefaultConfig", readDefaultLocalMqttConfigName());
		resp.put("defaultCerts", defaultCerts);
		return ResponseEntity.ok(resp);
	}

	private void persistRemoteMqttToMqttConf(boolean newRemoteMqtt) {
		try {
			Path confPath = resolveMonitorDir().resolve("mqtt.conf");
			Map<String, Object> cfg = loadMqttConfFile(confPath);
			cfg.put("remote_mqtt", newRemoteMqtt);
			writeMqttConfFile(confPath, cfg);
		} catch (Exception ignored) {
		}
	}

	@PostMapping(value = "/mqtt/settings", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> updateMqttSettings(@RequestBody Map<String, Object> body) {
		boolean newRemote = body != null && body.containsKey("remoteMqtt")
				? Boolean.parseBoolean(String.valueOf(body.get("remoteMqtt")))
				: this.remoteMqtt;

		this.remoteMqtt = newRemote;
		persistRemoteMqttToMqttConf(newRemote);

		return ResponseEntity.ok(Map.of(
				"ok", true,
				"remoteMqtt", this.remoteMqtt,
				"exposeMqtt", this.exposeMqtt
		));
	}

	private Path resolveThemeConfigPath() {
		Path primary = resolveThemesDir().resolve("theme-config.conf");
		if (Files.exists(primary)) {
			return primary;
		}
		Path root = resolveProjectRoot();
		Path p1 = root.resolve("helpers").resolve("theme-config.conf");
		if (Files.exists(p1)) {
			return p1;
		}
		Path p2 = root.resolve("heplers").resolve("theme-config.conf");
		if (Files.exists(p2)) {
			return p2;
		}
		Path p3 = root.resolve("helpers").resolve("theme-config");
		if (Files.exists(p3)) {
			return p3;
		}
		Path p4 = root.resolve("heplers").resolve("theme-config");
		if (Files.exists(p4)) {
			return p4;
		}
		return primary;
	}

	private Map<String, String> readSavedThemeConfig() {
		Map<String, String> result = new LinkedHashMap<>();
		result.put("colorTheme", "github-dark");
		result.put("uiStyle", "forge-classic");
		result.put("iconPack", "emoji");

		Path themePath = resolveThemeConfigPath();
		if (!Files.isRegularFile(themePath)) {
			return result;
		}
		try {
			for (String raw : Files.readAllLines(themePath, StandardCharsets.UTF_8)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.contains("=")) {
					int eq = line.indexOf('=');
					String key = line.substring(0, eq).trim();
					String val = line.substring(eq + 1).trim();
					if (val.isEmpty()) {
						continue;
					}
					if ("theme".equalsIgnoreCase(key) || "colorTheme".equalsIgnoreCase(key)) {
						result.put("colorTheme", val);
					} else if ("uiStyle".equalsIgnoreCase(key)) {
						result.put("uiStyle", val);
					} else if ("iconPack".equalsIgnoreCase(key)) {
						result.put("iconPack", val);
					}
				} else {
					result.put("colorTheme", line);
				}
			}
		} catch (Exception ignored) {
		}
		return result;
	}

	@GetMapping(value = "/theme", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getTheme() {
		Map<String, String> cfg = readSavedThemeConfig();
		String colorTheme = cfg.get("colorTheme");
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"theme", colorTheme,
				"colorTheme", colorTheme,
				"uiStyle", cfg.get("uiStyle"),
				"iconPack", cfg.get("iconPack"),
				"path", "AAU/script/themes/theme-config.conf"
		));
	}

	@PostMapping(value = "/theme", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> saveTheme(@RequestBody Map<String, Object> body) {
		Map<String, String> existing = readSavedThemeConfig();
		String colorTheme = existing.get("colorTheme");
		String uiStyle = existing.get("uiStyle");
		String iconPack = existing.get("iconPack");

		if (body != null) {
			Object rawColor = body.containsKey("colorTheme") ? body.get("colorTheme") : body.get("theme");
			if (rawColor != null && !String.valueOf(rawColor).trim().isEmpty()) {
				colorTheme = String.valueOf(rawColor).trim();
			}
			if (body.get("uiStyle") != null && !String.valueOf(body.get("uiStyle")).trim().isEmpty()) {
				uiStyle = String.valueOf(body.get("uiStyle")).trim();
			}
			if (body.get("iconPack") != null && !String.valueOf(body.get("iconPack")).trim().isEmpty()) {
				iconPack = String.valueOf(body.get("iconPack")).trim();
			}
		}

		try {
			Path themesDir = resolveThemesDir();
			Files.createDirectories(themesDir);
			String content = "theme=" + colorTheme + "\n"
					+ "uiStyle=" + uiStyle + "\n"
					+ "iconPack=" + iconPack + "\n";
			Files.writeString(themesDir.resolve("theme-config.conf"), content, StandardCharsets.UTF_8);
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"theme", colorTheme,
					"colorTheme", colorTheme,
					"uiStyle", uiStyle,
					"iconPack", iconPack,
					"path", "AAU/script/themes/theme-config.conf"
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to save theme config: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/mqtt/local-config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getLocalMqttConfig() {
		Path localConfPath = resolveLocalMqttConfPath();
		boolean exists = Files.exists(localConfPath) && Files.isRegularFile(localConfPath);
		String content = "";
		Map<String, String> details = new LinkedHashMap<>();
		if (exists) {
			try {
				content = Files.readString(localConfPath, StandardCharsets.UTF_8);
				details = parseLocalMqttConf(content);
			} catch (Exception ignored) {
			}
		}
		Path sharedDir = resolveSharedDir();
		Path defaultCa = sharedDir.resolve("ca.crt").toAbsolutePath().normalize();
		Path defaultBroker = sharedDir.resolve("broker.crt").toAbsolutePath().normalize();
		Path defaultKey = sharedDir.resolve("broker.key").toAbsolutePath().normalize();

		boolean caCertPresent = Files.isRegularFile(defaultCa);
		boolean brokerCertPresent = Files.isRegularFile(defaultBroker);
		boolean brokerKeyPresent = Files.isRegularFile(defaultKey);

		Map<String, String> defaultCerts = new LinkedHashMap<>();
		defaultCerts.put("caCert", defaultCa.toString());
		defaultCerts.put("brokerCert", defaultBroker.toString());
		defaultCerts.put("brokerKey", defaultKey.toString());

		Path fnScriptPath = resolveDefaultFnScriptPath();
		boolean fnScriptExists = fnScriptPath != null && Files.isRegularFile(fnScriptPath);
		String fnScriptRelPath = "";
		String fnScriptContent = "";
		List<String> fnScriptTopics = new ArrayList<>();
		if (fnScriptExists) {
			try {
				fnScriptRelPath = resolveProjectRoot().relativize(fnScriptPath).toString().replace('\\', '/');
				fnScriptContent = Files.readString(fnScriptPath, StandardCharsets.UTF_8);
				fnScriptTopics = extractTopicsFromFnScript(fnScriptContent);
			} catch (Exception ignored) {
			}
		}

		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("exists", exists);
		resp.put("applied", exists && isLocalConfigApplied());
		resp.put("running", isMosquittoServiceActive());
		resp.put("path", "helpers/mqtt-configuration.conf");
		resp.put("content", content);
		resp.put("details", details);
		resp.put("defaultCerts", defaultCerts);
		resp.put("caCertPresent", caCertPresent);
		resp.put("brokerCertPresent", brokerCertPresent);
		resp.put("brokerKeyPresent", brokerKeyPresent);
		resp.put("certsPresent", caCertPresent && brokerCertPresent && brokerKeyPresent);
		resp.put("fnScriptExists", fnScriptExists);
		resp.put("fnScriptPath", fnScriptRelPath);
		resp.put("fnScriptContent", fnScriptContent);
		resp.put("fnScriptTopics", fnScriptTopics);
		return ResponseEntity.ok(resp);
	}

	@PostMapping(value = "/mqtt/local-config", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> createLocalMqttConfig(@RequestBody Map<String, Object> body) {
		if (body == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing request body"));
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> usersList = (body.get("users") instanceof List<?>)
				? (List<Map<String, Object>>) body.get("users")
				: List.of();

		if (usersList.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "At least the admin user must be provided"));
		}

		List<String> pyArgs = new ArrayList<>();
		List<String> formattedUsers = new ArrayList<>();
		String adminPassword = "";
		for (int i = 0; i < usersList.size(); i++) {
			Map<String, Object> u = usersList.get(i);
			String username = u.get("username") == null ? "" : String.valueOf(u.get("username")).trim();
			String password = u.get("password") == null ? "" : String.valueOf(u.get("password")).trim();
			if (i == 0) {
				username = "admin";
				adminPassword = password;
			}
			if (username.isEmpty() || password.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of(
						"ok", false,
						"error", "Every user must have both a username and a password"
				));
			}
			pyArgs.add(username);
			pyArgs.add(password);
			formattedUsers.add(username + "=" + password);
		}

		Path sharedDir = resolveSharedDir();
		String caCert = body.get("caCert") == null || String.valueOf(body.get("caCert")).trim().isEmpty()
				? sharedDir.resolve("ca.crt").toAbsolutePath().normalize().toString()
				: String.valueOf(body.get("caCert")).trim();
		String brokerCert = body.get("brokerCert") == null || String.valueOf(body.get("brokerCert")).trim().isEmpty()
				? sharedDir.resolve("broker.crt").toAbsolutePath().normalize().toString()
				: String.valueOf(body.get("brokerCert")).trim();
		String brokerKey = body.get("brokerKey") == null || String.valueOf(body.get("brokerKey")).trim().isEmpty()
				? sharedDir.resolve("broker.key").toAbsolutePath().normalize().toString()
				: String.valueOf(body.get("brokerKey")).trim();

		try {
			// Delete old mqtt-configuration.conf and setupMosquitto_users.sh before saving new configuration
			Path root = resolveProjectRoot();
			Files.deleteIfExists(root.resolve("helpers").resolve("mqtt-configuration.conf"));
			Files.deleteIfExists(root.resolve("heplers").resolve("mqtt-configuration.conf"));
			Files.deleteIfExists(root.resolve("helpers").resolve("setupMosquitto_users.sh"));
			Files.deleteIfExists(root.resolve("heplers").resolve("setupMosquitto_users.sh"));
			localConfigApplied = false;

			Files.createDirectories(sharedDir);
			String caCertContent = body.get("caCertContent") == null ? "" : String.valueOf(body.get("caCertContent")).trim();
			if (!caCertContent.isEmpty()) {
				Path target = sharedDir.resolve("ca.crt");
				Files.writeString(target, caCertContent, StandardCharsets.UTF_8);
				caCert = target.toAbsolutePath().normalize().toString();
			}
			String brokerCertContent = body.get("brokerCertContent") == null ? "" : String.valueOf(body.get("brokerCertContent")).trim();
			if (!brokerCertContent.isEmpty()) {
				Path target = sharedDir.resolve("broker.crt");
				Files.writeString(target, brokerCertContent, StandardCharsets.UTF_8);
				brokerCert = target.toAbsolutePath().normalize().toString();
			}
			String brokerKeyContent = body.get("brokerKeyContent") == null ? "" : String.valueOf(body.get("brokerKeyContent")).trim();
			if (!brokerKeyContent.isEmpty()) {
				Path target = sharedDir.resolve("broker.key");
				Files.writeString(target, brokerKeyContent, StandardCharsets.UTF_8);
				brokerKey = target.toAbsolutePath().normalize().toString();
			}

			String confContent =
					"address=0.0.0.0\n" +
					"port=8883\n" +
					"users=[" + String.join(", ", formattedUsers) + "]\n" +
					"ca.crt=" + caCert + "\n" +
					"broker.crt=" + brokerCert + "\n" +
					"broker.key=" + brokerKey + "\n";

			Path helpersDir = resolveHelpersDir();
			Files.createDirectories(helpersDir);
			Path confPath = helpersDir.resolve("mqtt-configuration.conf");
			Files.writeString(confPath, confContent, StandardCharsets.UTF_8);

			Path createUserListPy = resolveHelperScript("createUserList.py");
			if (!Files.exists(createUserListPy)) {
				return ResponseEntity.status(404).body(Map.of(
						"ok", false,
						"error", "createUserList.py not found in helpers directory"
				));
			}

			List<String> cmd = new ArrayList<>();
			cmd.add("python3");
			cmd.add(createUserListPy.toAbsolutePath().toString());
			cmd.addAll(pyArgs);
			runCommandWithTimeout(cmd, createUserListPy.getParent());

			// If the user loaded a script from this page (or requested updating mqtt.conf), update mqtt-monitor/mqtt.conf and AAU/script/script.fn & working.fn
			String loadedScriptContent = body.get("loadedScriptContent") == null ? "" : String.valueOf(body.get("loadedScriptContent"));
			boolean scriptLoadedOnPage = Boolean.parseBoolean(String.valueOf(body.getOrDefault("scriptLoadedOnPage", false)))
					|| !loadedScriptContent.trim().isEmpty();

			if (!loadedScriptContent.trim().isEmpty()) {
				Path aauScriptDir = root.resolve("AAU").resolve("script");
				Files.createDirectories(aauScriptDir);
				Files.writeString(aauScriptDir.resolve("script.fn"), loadedScriptContent, StandardCharsets.UTF_8);
				Files.writeString(aauScriptDir.resolve("working.fn"), loadedScriptContent, StandardCharsets.UTF_8);
			}

			if (scriptLoadedOnPage) {
				Path mqttMonitorConfPath = resolveMonitorDir().resolve("mqtt.conf");
				Map<String, Object> monitorCfg = loadMqttConfFile(mqttMonitorConfPath);
				monitorCfg.put("configured", true);
				monitorCfg.put("ip", "127.0.0.1");
				monitorCfg.put("port", 8883);
				monitorCfg.put("username", "admin");
				monitorCfg.put("password", adminPassword);
				monitorCfg.put("ca_cert_path", caCert);
				monitorCfg.put("broker_cert_path", brokerCert);
				writeMqttConfFile(mqttMonitorConfPath, monitorCfg);
			}

			this.remoteMqtt = false;
			persistRemoteMqttToMqttConf(false);

			Map<String, Object> resp = new LinkedHashMap<>();
			resp.put("ok", true);
			resp.put("path", "helpers/mqtt-configuration.conf");
			resp.put("content", confContent);
			resp.put("details", parseLocalMqttConf(confContent));
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to create local MQTT configuration: " + e.getMessage()
			));
		}
	}

	private String formatRemoteConfigText() {
		Path confPath = resolveMonitorDir().resolve("mqtt.conf");
		return formatRemoteConfigText(loadMqttConfFile(confPath));
	}

	private String formatRemoteConfigText(Map<String, Object> cfg) {
		StringBuilder sb = new StringBuilder();
		sb.append("# Forge Node Remote MQTT Configuration\n");
		sb.append("config_type = remote\n");
		sb.append("configured = ").append(Boolean.TRUE.equals(cfg.get("configured")) ? "true" : "false").append("\n");
		sb.append("ip = ").append(cfg.get("ip")).append("\n");
		sb.append("port = ").append(cfg.get("port")).append("\n");
		sb.append("username = ").append(cfg.get("username")).append("\n");
		sb.append("password = ").append(cfg.get("password")).append("\n");
		sb.append("ca_cert_path = ").append(cfg.get("ca_cert_path")).append("\n");
		sb.append("broker_cert_path = ").append(cfg.get("broker_cert_path")).append("\n");
		sb.append("subscribe_admin = ").append(Boolean.TRUE.equals(cfg.get("subscribe_admin")) ? "true" : "false").append("\n");
		return sb.toString();
	}

	private String formatLocalConfigText() throws Exception {
		Path localConfPath = resolveLocalMqttConfPath();
		if (!Files.isRegularFile(localConfPath)) {
			return null;
		}
		String raw = Files.readString(localConfPath, StandardCharsets.UTF_8);
		Map<String, String> details = parseLocalMqttConf(raw);
		StringBuilder sb = new StringBuilder();
		sb.append("# Forge Node Local MQTT Configuration\n");
		sb.append("config_type=local\n");
		sb.append("address=").append(details.getOrDefault("address", "0.0.0.0")).append("\n");
		sb.append("port=").append(details.getOrDefault("port", "8883")).append("\n");
		sb.append("users=").append(details.getOrDefault("users", "[]")).append("\n");
		sb.append("ca.crt=").append(details.getOrDefault("ca.crt", "")).append("\n");
		sb.append("broker.crt=").append(details.getOrDefault("broker.crt", "")).append("\n");
		sb.append("broker.key=").append(details.getOrDefault("broker.key", "")).append("\n");
		return sb.toString();
	}

	private ResponseEntity<Map<String, Object>> applyRemoteConfigFromContent(String content) {
		try {
			Map<String, String> parsed = parseLocalMqttConf(content);
			String configType = parsed.getOrDefault("config_type", "").trim().toLowerCase();
			if ("local".equals(configType) || (!parsed.containsKey("ip") && (parsed.containsKey("users") || parsed.containsKey("broker.key")))) {
				return ResponseEntity.badRequest().body(Map.of(
						"ok", false,
						"error", "This file is a Local MQTT configuration. Switch to Local MQTT mode to load it."
				));
			}

			String ip = parsed.getOrDefault("ip", "").trim();
			if (ip.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Remote MQTT configuration must include 'ip'"));
			}

			int port;
			try {
				port = Integer.parseInt(parsed.getOrDefault("port", "8883").trim());
				if (port < 1 || port > 65535) {
					throw new NumberFormatException("out of range");
				}
			} catch (Exception e) {
				return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid 'port' in configuration file (must be 1-65535)"));
			}

			Path confPath = resolveMonitorDir().resolve("mqtt.conf");
			Map<String, Object> cfg = loadMqttConfFile(confPath);
			cfg.put("configured", !"false".equalsIgnoreCase(parsed.getOrDefault("configured", "true").trim()));
			cfg.put("ip", ip);
			cfg.put("port", port);
			cfg.put("username", parsed.getOrDefault("username", String.valueOf(cfg.get("username"))).trim());
			cfg.put("password", parsed.getOrDefault("password", ""));

			String caPath = resolveUploadedCertPath(parsed.get("ca_cert_path"), "ca.crt");
			String brokerPath = resolveUploadedCertPath(parsed.get("broker_cert_path"), "broker.crt");
			cfg.put("ca_cert_path", caPath);
			cfg.put("broker_cert_path", brokerPath);

			String subAdmin = parsed.getOrDefault("subscribe_admin", "true").trim();
			cfg.put("subscribe_admin", "true".equalsIgnoreCase(subAdmin) || "1".equals(subAdmin) || "yes".equalsIgnoreCase(subAdmin));

			writeMqttConfFile(confPath, cfg);
			return ResponseEntity.ok(Map.of("ok", true, "config", cfg));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to load remote MQTT configuration: " + e.getMessage()
			));
		}
	}

	private ResponseEntity<Map<String, Object>> applyLocalConfigFromContent(String content) {
		try {
			Map<String, String> parsed = parseLocalMqttConf(content);
			String configType = parsed.getOrDefault("config_type", "").trim().toLowerCase();
			if ("remote".equals(configType) || (!parsed.containsKey("users") && (parsed.containsKey("ip") || parsed.containsKey("ca_cert_path")))) {
				return ResponseEntity.badRequest().body(Map.of(
						"ok", false,
						"error", "This file is a Remote MQTT configuration. Switch to Remote MQTT mode to load it."
				));
			}

			String usersRaw = parsed.getOrDefault("users", "").trim();
			List<String> pyArgs = new ArrayList<>();
			List<String> formattedUsers = new ArrayList<>();
			parseUsersListEntries(usersRaw, pyArgs, formattedUsers);
			if (formattedUsers.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of(
						"ok", false,
						"error", "Local MQTT configuration must contain at least one user in 'users=[user=password, ...]'"
				));
			}

			String address = parsed.getOrDefault("address", "0.0.0.0").trim();
			if (address.isEmpty()) {
				address = "0.0.0.0";
			}
			int port;
			try {
				port = Integer.parseInt(parsed.getOrDefault("port", "8883").trim());
				if (port < 1 || port > 65535) {
					throw new NumberFormatException("out of range");
				}
			} catch (Exception e) {
				return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid 'port' in configuration file (must be 1-65535)"));
			}

			String caCert = resolveUploadedCertPath(parsed.get("ca.crt"), "ca.crt");
			String brokerCert = resolveUploadedCertPath(parsed.get("broker.crt"), "broker.crt");
			String brokerKey = resolveUploadedCertPath(parsed.get("broker.key"), "broker.key");

			Path root = resolveProjectRoot();
			Files.deleteIfExists(root.resolve("helpers").resolve("mqtt-configuration.conf"));
			Files.deleteIfExists(root.resolve("heplers").resolve("mqtt-configuration.conf"));
			Files.deleteIfExists(root.resolve("helpers").resolve("setupMosquitto_users.sh"));
			Files.deleteIfExists(root.resolve("heplers").resolve("setupMosquitto_users.sh"));
			localConfigApplied = false;

			String confContent =
					"address=" + address + "\n" +
					"port=" + port + "\n" +
					"users=[" + String.join(", ", formattedUsers) + "]\n" +
					"ca.crt=" + caCert + "\n" +
					"broker.crt=" + brokerCert + "\n" +
					"broker.key=" + brokerKey + "\n";

			Path helpersDir = resolveHelpersDir();
			Files.createDirectories(helpersDir);
			Files.writeString(helpersDir.resolve("mqtt-configuration.conf"), confContent, StandardCharsets.UTF_8);

			Path createUserListPy = resolveHelperScript("createUserList.py");
			if (!Files.exists(createUserListPy)) {
				return ResponseEntity.status(404).body(Map.of(
						"ok", false,
						"error", "createUserList.py not found in helpers directory"
				));
			}
			List<String> cmd = new ArrayList<>();
			cmd.add("python3");
			cmd.add(createUserListPy.toAbsolutePath().toString());
			cmd.addAll(pyArgs);
			runCommandWithTimeout(cmd, createUserListPy.getParent());

			Map<String, Object> resp = new LinkedHashMap<>();
			resp.put("ok", true);
			resp.put("localConfigExists", true);
			resp.put("localConfigApplied", false);
			resp.put("path", "helpers/mqtt-configuration.conf");
			resp.put("content", confContent);
			resp.put("details", parseLocalMqttConf(confContent));
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to load local MQTT configuration: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/mqtt/remote-config/download")
	public ResponseEntity<byte[]> downloadRemoteMqttConfig() {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"forge-node-remote-mqtt.conf\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(formatRemoteConfigText().getBytes(StandardCharsets.UTF_8));
	}

	@PostMapping(value = "/mqtt/remote-config/upload", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> uploadRemoteMqttConfig(@RequestParam("file") MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Uploaded file is empty"));
		}
		try {
			return applyRemoteConfigFromContent(new String(file.getBytes(), StandardCharsets.UTF_8));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to read uploaded file: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/mqtt/local-config/download")
	public ResponseEntity<byte[]> downloadLocalMqttConfig() {
		try {
			String text = formatLocalConfigText();
			if (text == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"forge-node-local-mqtt.conf\"")
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.body(text.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			return ResponseEntity.status(500).build();
		}
	}

	@PostMapping(value = "/mqtt/local-config/upload", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> uploadLocalMqttConfig(@RequestParam("file") MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Uploaded file is empty"));
		}
		try {
			ResponseEntity<Map<String, Object>> result = applyLocalConfigFromContent(new String(file.getBytes(), StandardCharsets.UTF_8));
			if (result.getStatusCode().is2xxSuccessful()) {
				this.remoteMqtt = false;
				persistRemoteMqttToMqttConf(false);
			}
			return result;
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to read uploaded file: " + e.getMessage()
			));
		}
	}

	private String sanitizeConfFileName(String rawName) {
		if (rawName == null || rawName.isBlank()) {
			return null;
		}
		String name = Paths.get(rawName.trim()).getFileName().toString().trim();
		if (name.isEmpty() || ".".equals(name) || "..".equals(name) || name.contains("/") || name.contains("\\")) {
			return null;
		}
		if (!name.toLowerCase().endsWith(".conf")) {
			name = name + ".conf";
		}
		return name;
	}

	private String detectMqttConfigType(Map<String, String> parsed) {
		String explicit = parsed.getOrDefault("config_type", "").trim().toLowerCase();
		if ("local".equals(explicit) || "remote".equals(explicit)) {
			return explicit;
		}
		if (parsed.containsKey("users") || parsed.containsKey("broker.key")) {
			return "local";
		}
		if (parsed.containsKey("ip") || parsed.containsKey("ca_cert_path")) {
			return "remote";
		}
		return "";
	}

	@GetMapping(value = "/mqtt/configs/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> listSavedMqttConfigs(@RequestParam(value = "type", required = false) String type) {
		String filterType = type == null ? "" : type.trim().toLowerCase();
		if ("local".equals(filterType)) {
			Path localDir = resolveLocalMqttConfigsDir();
			String defaultConfig = readDefaultLocalMqttConfigName();
			List<Map<String, Object>> files = new ArrayList<>();
			if (Files.isDirectory(localDir)) {
				try (Stream<Path> stream = Files.list(localDir)) {
					stream.filter(Files::isRegularFile)
							.map(p -> p.getFileName().toString())
							.filter(name -> !"default-configuration".equals(name) && name.toLowerCase().endsWith(".conf"))
							.sorted()
							.forEach(name -> files.add(Map.of(
									"name", name,
									"type", "local",
									"isDefault", name.equals(defaultConfig)
							)));
				} catch (Exception ignored) {
				}
			}
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"defaultConfig", defaultConfig,
					"files", files
			));
		}

		Path scriptDir = resolveProjectRoot().resolve("AAU").resolve("script");
		List<Map<String, Object>> files = new ArrayList<>();
		if (Files.isDirectory(scriptDir)) {
			try (Stream<Path> stream = Files.list(scriptDir)) {
				stream.filter(Files::isRegularFile)
						.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".conf"))
						.sorted()
						.forEach(p -> {
							String name = p.getFileName().toString();
							String detected = "";
							try {
								detected = detectMqttConfigType(parseLocalMqttConf(Files.readString(p, StandardCharsets.UTF_8)));
							} catch (Exception ignored) {
							}
							if (filterType.isEmpty() || filterType.equals(detected)) {
								files.add(Map.of("name", name, "type", detected, "isDefault", false));
							}
						});
			} catch (Exception ignored) {
			}
		}
		return ResponseEntity.ok(Map.of("ok", true, "defaultConfig", "", "files", files));
	}

	@PostMapping(value = "/mqtt/configs/save", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> saveNamedMqttConfig(@RequestBody Map<String, Object> body) {
		if (body == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Request body is required"));
		}
		String type = body.get("type") == null ? "" : String.valueOf(body.get("type")).trim().toLowerCase();
		if (!"local".equals(type) && !"remote".equals(type)) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Config type must be 'local' or 'remote'"));
		}
		String rawName = body.get("fileName") == null ? "" : String.valueOf(body.get("fileName")).trim();
		if ("default-configuration".equalsIgnoreCase(rawName)) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "'default-configuration' is reserved"));
		}
		String fileName = sanitizeConfFileName(rawName);
		if (fileName == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid configuration filename"));
		}

		try {
			String text = "local".equals(type) ? formatLocalConfigText() : formatRemoteConfigText();
			if (text == null) {
				return ResponseEntity.badRequest().body(Map.of(
						"ok", false,
						"error", "No present Local MQTT configuration found to save"
				));
			}
			Path targetDir = "local".equals(type)
					? resolveLocalMqttConfigsDir()
					: resolveProjectRoot().resolve("AAU").resolve("script");
			Files.createDirectories(targetDir);
			Path target = targetDir.resolve(fileName).normalize();
			if (!target.startsWith(targetDir.normalize())) {
				return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid file path"));
			}
			Files.writeString(target, text, StandardCharsets.UTF_8);

			boolean setAsDefault = "local".equals(type)
					&& Boolean.parseBoolean(String.valueOf(body.getOrDefault("setAsDefault", false)));
			if (setAsDefault) {
				Files.writeString(targetDir.resolve("default-configuration"), fileName, StandardCharsets.UTF_8);
			}

			String activeDefault = "local".equals(type) ? readDefaultLocalMqttConfigName() : "";
			String relPath = "local".equals(type)
					? "AAU/script/local-mqtt-server/" + fileName
					: "AAU/script/" + fileName;

			return ResponseEntity.ok(Map.of(
					"ok", true,
					"fileName", fileName,
					"type", type,
					"isDefault", "local".equals(type) && fileName.equals(activeDefault),
					"defaultConfig", activeDefault,
					"path", relPath
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to save MQTT configuration: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/mqtt/configs/load", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> loadNamedMqttConfig(@RequestBody Map<String, Object> body) {
		if (body == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Request body is required"));
		}
		String type = body.get("type") == null ? "" : String.valueOf(body.get("type")).trim().toLowerCase();
		if (!"local".equals(type) && !"remote".equals(type)) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Config type must be 'local' or 'remote'"));
		}
		String fileName = sanitizeConfFileName(body.get("fileName") == null ? "" : String.valueOf(body.get("fileName")));
		if (fileName == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid configuration filename"));
		}

		try {
			Path sourceDir = "local".equals(type)
					? resolveLocalMqttConfigsDir()
					: resolveProjectRoot().resolve("AAU").resolve("script");
			Path source = sourceDir.resolve(fileName).normalize();
			if (!source.startsWith(sourceDir.normalize()) || !Files.isRegularFile(source)) {
				return ResponseEntity.status(404).body(Map.of(
						"ok", false,
						"error", "Configuration file not found: " + fileName
				));
			}
			String content = Files.readString(source, StandardCharsets.UTF_8);
			ResponseEntity<Map<String, Object>> result = "local".equals(type)
					? applyLocalConfigFromContent(content)
					: applyRemoteConfigFromContent(content);
			if (!result.getStatusCode().is2xxSuccessful()) {
				return result;
			}
			if ("local".equals(type)) {
				this.remoteMqtt = false;
				persistRemoteMqttToMqttConf(false);
				boolean setAsDefault = Boolean.parseBoolean(String.valueOf(body.getOrDefault("setAsDefault", false)));
				if (setAsDefault) {
					Files.createDirectories(sourceDir);
					Files.writeString(sourceDir.resolve("default-configuration"), fileName, StandardCharsets.UTF_8);
				}
				String activeDefault = readDefaultLocalMqttConfigName();
				Map<String, Object> resp = new LinkedHashMap<>(result.getBody());
				resp.put("fileName", fileName);
				resp.put("isDefault", fileName.equals(activeDefault));
				resp.put("defaultConfig", activeDefault);
				return ResponseEntity.ok(resp);
			}
			return result;
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to load MQTT configuration: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/monitor/configs/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> listMonitorConfigs() {
		Path dir = resolveMonitorConfigsDir();
		String defaultConfig = readDefaultMonitorConfigName();
		List<Map<String, Object>> files = new ArrayList<>();
		if (Files.isDirectory(dir)) {
			try (Stream<Path> stream = Files.list(dir)) {
				stream.filter(Files::isRegularFile)
						.map(p -> p.getFileName().toString())
						.filter(name -> !"default-configuration".equals(name) && name.toLowerCase().endsWith(".conf"))
						.sorted()
						.forEach(name -> files.add(Map.of(
								"name", name,
								"isDefault", name.equals(defaultConfig)
						)));
			} catch (Exception ignored) {
			}
		}
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"defaultConfig", defaultConfig,
				"files", files
		));
	}

	@PostMapping(value = "/monitor/configs/save", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> saveMonitorConfig(@RequestBody Map<String, Object> body) {
		if (body == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Request body is required"));
		}
		String rawName = body.get("fileName") == null ? "" : String.valueOf(body.get("fileName")).trim();
		if ("default-configuration".equalsIgnoreCase(rawName)) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "'default-configuration' is reserved"));
		}
		String fileName = sanitizeConfFileName(rawName);
		if (fileName == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Configuration name is required"));
		}

		String ip = body.get("ip") == null ? "" : String.valueOf(body.get("ip")).trim();
		if (ip.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "MQTT server IP address is required"));
		}
		int port;
		try {
			port = Integer.parseInt(String.valueOf(body.getOrDefault("port", "8883")).trim());
			if (port < 1 || port > 65535) {
				throw new NumberFormatException("out of range");
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Port number must be between 1 and 65535"));
		}

		try {
			String caCertContent = body.get("caCertContent") == null ? "" : String.valueOf(body.get("caCertContent")).trim();
			String caCertFileName = body.get("caCertFileName") == null ? "" : String.valueOf(body.get("caCertFileName")).trim();
			String caCertPath = body.get("caCertPath") == null ? "" : String.valueOf(body.get("caCertPath")).trim();
			if (!caCertContent.isEmpty()) {
				caCertPath = persistUploadedCert(caCertFileName.isEmpty() ? "ca.crt" : caCertFileName, caCertContent).toString();
			} else {
				caCertPath = resolveUploadedCertPath(caCertPath, "ca.crt");
			}

			String brokerCertContent = body.get("brokerCertContent") == null ? "" : String.valueOf(body.get("brokerCertContent")).trim();
			String brokerCertFileName = body.get("brokerCertFileName") == null ? "" : String.valueOf(body.get("brokerCertFileName")).trim();
			String brokerCertPath = body.get("brokerCertPath") == null ? "" : String.valueOf(body.get("brokerCertPath")).trim();
			if (!brokerCertContent.isEmpty()) {
				brokerCertPath = persistUploadedCert(brokerCertFileName.isEmpty() ? "broker.crt" : brokerCertFileName, brokerCertContent).toString();
			} else {
				brokerCertPath = resolveUploadedCertPath(brokerCertPath, "broker.crt");
			}

			Map<String, Object> cfg = new LinkedHashMap<>();
			cfg.put("configured", true);
			cfg.put("ip", ip);
			cfg.put("port", port);
			cfg.put("username", body.get("username") == null ? "" : String.valueOf(body.get("username")).trim());
			cfg.put("password", body.get("password") == null ? "" : String.valueOf(body.get("password")));
			cfg.put("ca_cert_path", caCertPath);
			cfg.put("broker_cert_path", brokerCertPath);
			cfg.put("subscribe_admin", !Boolean.FALSE.equals(body.get("subscribeAdmin")));

			Path dir = resolveMonitorConfigsDir();
			Files.createDirectories(dir);
			Path target = dir.resolve(fileName).normalize();
			if (!target.startsWith(dir.normalize())) {
				return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid file path"));
			}
			Files.writeString(target, formatRemoteConfigText(cfg), StandardCharsets.UTF_8);

			boolean setAsDefault = Boolean.parseBoolean(String.valueOf(body.getOrDefault("setAsDefault", false)));
			if (setAsDefault) {
				Files.writeString(dir.resolve("default-configuration"), fileName, StandardCharsets.UTF_8);
				writeMqttConfFile(resolveMonitorDir().resolve("mqtt.conf"), cfg);
			}

			String activeDefault = readDefaultMonitorConfigName();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"fileName", fileName,
					"isDefault", fileName.equals(activeDefault),
					"defaultConfig", activeDefault,
					"path", "AAU/script/mqtt-monitor-configurations/" + fileName,
					"config", cfg
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to save monitor configuration: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/monitor/configs/load", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> loadMonitorConfig(@RequestBody Map<String, Object> body) {
		if (body == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Request body is required"));
		}
		String fileName = sanitizeConfFileName(body.get("fileName") == null ? "" : String.valueOf(body.get("fileName")));
		if (fileName == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid configuration filename"));
		}

		try {
			Path dir = resolveMonitorConfigsDir();
			Path source = dir.resolve(fileName).normalize();
			if (!source.startsWith(dir.normalize()) || !Files.isRegularFile(source)) {
				return ResponseEntity.status(404).body(Map.of(
						"ok", false,
						"error", "Configuration not found: " + fileName
				));
			}
			String content = Files.readString(source, StandardCharsets.UTF_8);
			ResponseEntity<Map<String, Object>> applied = applyRemoteConfigFromContent(content);
			if (!applied.getStatusCode().is2xxSuccessful()) {
				return applied;
			}
			boolean setAsDefault = Boolean.parseBoolean(String.valueOf(body.getOrDefault("setAsDefault", false)));
			if (setAsDefault) {
				Files.createDirectories(dir);
				Files.writeString(dir.resolve("default-configuration"), fileName, StandardCharsets.UTF_8);
			}
			String activeDefault = readDefaultMonitorConfigName();
			Map<String, Object> resp = new LinkedHashMap<>();
			resp.put("ok", true);
			resp.put("fileName", fileName);
			resp.put("isDefault", fileName.equals(activeDefault));
			resp.put("defaultConfig", activeDefault);
			resp.put("config", applied.getBody().get("config"));
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to load monitor configuration: " + e.getMessage()
			));
		}
	}

	private String resolveUploadedCertPath(String configuredPath, String defaultFileName) {
		if (configuredPath != null && !configuredPath.trim().isEmpty()) {
			try {
				Path p = Paths.get(configuredPath.trim());
				if (Files.isRegularFile(p)) {
					return p.toAbsolutePath().normalize().toString();
				}
			} catch (Exception ignored) {
			}
		}
		Path sharedDefault = resolveSharedDir().resolve(defaultFileName).toAbsolutePath().normalize();
		if (Files.isRegularFile(sharedDefault)) {
			return sharedDefault.toString();
		}
		if (configuredPath != null && !configuredPath.trim().isEmpty()) {
			return configuredPath.trim();
		}
		return resolveSharedCert(defaultFileName).toAbsolutePath().normalize().toString();
	}

	private void parseUsersListEntries(String usersRaw, List<String> pyArgs, List<String> formattedUsers) {
		if (usersRaw == null || usersRaw.isBlank()) {
			return;
		}
		String trimmed = usersRaw.trim();
		if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
		if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
		for (String part : trimmed.split(",")) {
			String seg = part.trim();
			int eq = seg.indexOf('=');
			if (eq <= 0) continue;
			String u = seg.substring(0, eq).trim();
			String p = seg.substring(eq + 1).trim();
			if (!u.isEmpty() && !p.isEmpty()) {
				pyArgs.add(u);
				pyArgs.add(p);
				formattedUsers.add(u + "=" + p);
			}
		}
	}

	@PostMapping(value = "/mqtt/local-apply", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> applyLocalMqttConfig() {
		try {
			applyLocalMosquittoSetupInternal();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"localConfigApplied", true,
					"localMqttRunning", isMosquittoServiceActive()
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"localConfigApplied", isLocalConfigApplied(),
					"localMqttRunning", isMosquittoServiceActive(),
					"error", "Failed to apply local Mosquitto configuration: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/mqtt/local-start", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> startLocalMqttServer() {
		try {
			startLocalMosquittoServerInternal();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"localMqttRunning", isMosquittoServiceActive()
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"localMqttRunning", isMosquittoServiceActive(),
					"error", "Failed to start Mosquitto service: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/mqtt/local-stop", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> stopLocalMqttServer() {
		try {
			stopLocalMosquittoServerInternal();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"localMqttRunning", isMosquittoServiceActive()
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"localMqttRunning", isMosquittoServiceActive(),
					"error", "Failed to stop Mosquitto service: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/mqtt/files", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> listMqttCertFiles() {
		Path projectRoot = resolveProjectRoot();
		List<Map<String, String>> files = new ArrayList<>();
		collectCertFiles(projectRoot.resolve("SHARED"), "SHARED", files);
		collectCertFiles(projectRoot.resolve("SHARED").resolve("backup"), "SHARED/backup", files);
		collectCertFiles(projectRoot.resolve("AAU").resolve("certs"), "AAU/certs", files);
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"files", files
		));
	}

	private void collectCertFiles(Path dir, String group, List<Map<String, String>> out) {
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
								"path", p.toAbsolutePath().normalize().toString()
						));
					});
		} catch (Exception ignored) {
		}
	}

	private Path resolveSharedDir() {
		return resolveProjectRoot().resolve("SHARED");
	}

	private Path resolveHelperScript(String scriptName) {
		Path root = resolveProjectRoot();
		Path p1 = root.resolve("helpers").resolve(scriptName);
		if (Files.exists(p1)) {
			return p1;
		}
		Path p2 = root.resolve("heplers").resolve(scriptName);
		if (Files.exists(p2)) {
			return p2;
		}
		return resolveHelpersDir().resolve(scriptName);
	}

	private boolean hasFullCertSet(Path dir) {
		return Files.isDirectory(dir)
				&& Files.isRegularFile(dir.resolve("ca.crt"))
				&& Files.isRegularFile(dir.resolve("broker.crt"))
				&& Files.isRegularFile(dir.resolve("broker.key"));
	}

	private Path resolveRestoreBackupDir() {
		Path sharedDir = resolveSharedDir();
		Path plainBackup = sharedDir.resolve("backup");
		if (hasFullCertSet(plainBackup)) {
			return plainBackup;
		}
		if (Files.isDirectory(sharedDir)) {
			try (Stream<Path> stream = Files.list(sharedDir)) {
				List<Path> candidates = stream
						.filter(Files::isDirectory)
						.filter(p -> {
							String name = p.getFileName().toString();
							return "backup".equals(name) || name.startsWith("backup-");
						})
						.filter(this::hasFullCertSet)
						.sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
						.toList();
				if (!candidates.isEmpty()) {
					return candidates.get(0);
				}
			} catch (Exception ignored) {
			}
		}
		return plainBackup;
	}

	private Map<String, Object> buildCertsStatusMap() {
		Path sharedDir = resolveSharedDir();
		boolean hasCaCrt = Files.isRegularFile(sharedDir.resolve("ca.crt"));
		boolean hasBrokerCrt = Files.isRegularFile(sharedDir.resolve("broker.crt"));
		boolean hasBrokerKey = Files.isRegularFile(sharedDir.resolve("broker.key"));
		boolean present = hasCaCrt && hasBrokerCrt && hasBrokerKey;

		Path backupCandidate = resolveRestoreBackupDir();
		boolean backupPresent = hasFullCertSet(backupCandidate);

		Map<String, Object> status = new LinkedHashMap<>();
		status.put("ok", true);
		status.put("present", present);
		status.put("caCrt", hasCaCrt);
		status.put("brokerCrt", hasBrokerCrt);
		status.put("brokerKey", hasBrokerKey);
		status.put("backupPresent", backupPresent);
		status.put("backupDir", backupPresent ? ("SHARED/" + backupCandidate.getFileName().toString()) : "");
		return status;
	}

	private void executeHelperScript(String scriptName, String stdinInput) throws Exception {
		executeHelperScript(scriptName, stdinInput, Map.of());
	}

	private void executeHelperScript(String scriptName, String stdinInput, Map<String, String> extraEnv) throws Exception {
		Path scriptPath = resolveHelperScript(scriptName);
		if (!Files.exists(scriptPath)) {
			throw new RuntimeException("Helper script not found: " + scriptName);
		}
		ProcessBuilder pb = new ProcessBuilder("bash", scriptPath.toAbsolutePath().toString());
		pb.directory(scriptPath.getParent().toFile());
		pb.environment().remove("FN_ADMIN_USER");
		pb.environment().remove("FN_ADMIN_PASSWORD");
		if (extraEnv != null) {
			pb.environment().putAll(extraEnv);
		}
		pb.redirectErrorStream(true);
		Process proc = pb.start();
		if (stdinInput != null) {
			try (OutputStream os = proc.getOutputStream()) {
				os.write(stdinInput.getBytes(StandardCharsets.UTF_8));
				os.flush();
			}
		} else {
			try {
				proc.getOutputStream().close();
			} catch (Exception ignored) {
			}
		}
		String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		boolean finished = proc.waitFor(60, TimeUnit.SECONDS);
		if (!finished) {
			proc.destroyForcibly();
			throw new RuntimeException(scriptName + " timed out");
		}
		if ("createCerts.sh".equals(scriptName) && proc.exitValue() != 0) {
			throw new RuntimeException(scriptName + " failed (exit " + proc.exitValue() + "): " + output.trim());
		}
	}

	@GetMapping(value = "/certs/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getCertsStatus() {
		return ResponseEntity.ok(buildCertsStatusMap());
	}

	@GetMapping(value = "/certs/details", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getCertsDetails() {
		Path sharedDir = resolveSharedDir();
		Path caCrt = sharedDir.resolve("ca.crt");
		Path brokerCrt = sharedDir.resolve("broker.crt");

		List<Map<String, Object>> certificates = new ArrayList<>();
		if (Files.isRegularFile(brokerCrt)) {
			certificates.add(readCertificateDetails(brokerCrt, "broker.crt", "Broker Certificate"));
		}
		if (Files.isRegularFile(caCrt)) {
			certificates.add(readCertificateDetails(caCrt, "ca.crt", "CA Certificate"));
		}

		Map<String, Object> resp = new LinkedHashMap<>(buildCertsStatusMap());
		if (certificates.isEmpty()) {
			resp.put("ok", false);
			resp.put("error", "No certificates found in SHARED/");
			return ResponseEntity.badRequest().body(resp);
		}
		resp.put("certificates", certificates);
		return ResponseEntity.ok(resp);
	}

	private Map<String, Object> readCertificateDetails(Path certPath, String fileName, String label) {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("fileName", fileName);
		info.put("label", label);
		try (InputStream in = Files.newInputStream(certPath)) {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			Certificate cert = cf.generateCertificate(in);
			if (!(cert instanceof X509Certificate x509)) {
				info.put("error", "Not an X.509 certificate");
				return info;
			}
			byte[] encoded = x509.getEncoded();
			info.put("sha1Fingerprint", formatDigest(encoded, "SHA-1"));
			info.put("sha256Fingerprint", formatDigest(encoded, "SHA-256"));
			info.put("subject", x509.getSubjectX500Principal().getName());
			info.put("issuer", x509.getIssuerX500Principal().getName());
			info.put("serialNumber", formatSerialHex(x509.getSerialNumber().toByteArray()));
			info.put("version", "v" + x509.getVersion());
			info.put("signatureAlgorithm", x509.getSigAlgName());
			info.put("publicKey", formatPublicKey(x509.getPublicKey()));

			Date notBefore = x509.getNotBefore();
			Date notAfter = x509.getNotAfter();
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
			info.put("validFrom", notBefore != null ? fmt.format(notBefore.toInstant()) : "");
			info.put("validUntil", notAfter != null ? fmt.format(notAfter.toInstant()) : "");

			Instant now = Instant.now();
			String validityStatus = "Valid";
			if (notBefore != null && now.isBefore(notBefore.toInstant())) {
				validityStatus = "Not Yet Valid";
			} else if (notAfter != null && now.isAfter(notAfter.toInstant())) {
				validityStatus = "Expired";
			}
			info.put("validityStatus", validityStatus);

			List<String> sans = extractSubjectAltNames(x509);
			info.put("subjectAltNames", sans);
		} catch (Exception e) {
			info.put("error", "Failed to parse certificate: " + e.getMessage());
		}
		return info;
	}

	private String formatDigest(byte[] data, String algorithm) throws Exception {
		byte[] digest = MessageDigest.getInstance(algorithm).digest(data);
		return formatColonHex(digest);
	}

	private String formatSerialHex(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return "";
		}
		return formatColonHex(bytes);
	}

	private String formatColonHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (int i = 0; i < bytes.length; i++) {
			if (i > 0) {
				sb.append(':');
			}
			sb.append(String.format("%02X", bytes[i] & 0xFF));
		}
		return sb.toString();
	}

	private String formatPublicKey(PublicKey key) {
		if (key == null) {
			return "";
		}
		String algo = key.getAlgorithm();
		if (key instanceof RSAPublicKey rsa) {
			return algo + " (" + rsa.getModulus().bitLength() + " bits)";
		}
		if (key instanceof ECPublicKey ec && ec.getParams() != null) {
			return algo + " (" + ec.getParams().getCurve().getField().getFieldSize() + " bits)";
		}
		return algo;
	}

	private List<String> extractSubjectAltNames(X509Certificate x509) {
		List<String> result = new ArrayList<>();
		try {
			Collection<List<?>> altNames = x509.getSubjectAlternativeNames();
			if (altNames == null) {
				return result;
			}
			for (List<?> entry : altNames) {
				if (entry == null || entry.size() < 2) {
					continue;
				}
				int type = ((Number) entry.get(0)).intValue();
				String val = String.valueOf(entry.get(1));
				String prefix = switch (type) {
					case 1 -> "Email:";
					case 2 -> "DNS:";
					case 6 -> "URI:";
					case 7 -> "IP:";
					default -> "Other:";
				};
				result.add(prefix + val);
			}
		} catch (Exception ignored) {
		}
		return result;
	}

	@PostMapping(value = "/certs/backup", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> backupCerts() {
		Path sharedDir = resolveSharedDir();
		Path caCrt = sharedDir.resolve("ca.crt");
		Path brokerCrt = sharedDir.resolve("broker.crt");
		Path brokerKey = sharedDir.resolve("broker.key");

		if (!Files.isRegularFile(caCrt) || !Files.isRegularFile(brokerCrt) || !Files.isRegularFile(brokerKey)) {
			Map<String, Object> err = new LinkedHashMap<>(buildCertsStatusMap());
			err.put("ok", false);
			err.put("error", "Cannot backup: one or more certificates (ca.crt, broker.crt, broker.key) are missing in SHARED/");
			return ResponseEntity.badRequest().body(err);
		}

		try {
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
			String folderName = "backup-" + timestamp;
			Path backupDir = sharedDir.resolve(folderName);
			Files.createDirectories(backupDir);
			Files.copy(caCrt, backupDir.resolve("ca.crt"), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(brokerCrt, backupDir.resolve("broker.crt"), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(brokerKey, backupDir.resolve("broker.key"), StandardCopyOption.REPLACE_EXISTING);

			// Also maintain SHARED/backup with the latest backup for direct restore
			Path plainBackupDir = sharedDir.resolve("backup");
			Files.createDirectories(plainBackupDir);
			Files.copy(caCrt, plainBackupDir.resolve("ca.crt"), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(brokerCrt, plainBackupDir.resolve("broker.crt"), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(brokerKey, plainBackupDir.resolve("broker.key"), StandardCopyOption.REPLACE_EXISTING);

			Map<String, Object> resp = new LinkedHashMap<>(buildCertsStatusMap());
			resp.put("backupDir", "SHARED/" + folderName);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>(buildCertsStatusMap());
			err.put("ok", false);
			err.put("error", "Failed to backup certificates: " + e.getMessage());
			return ResponseEntity.status(500).body(err);
		}
	}

	@PostMapping(value = "/certs/restore", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> restoreCerts() {
		Path sharedDir = resolveSharedDir();
		Path backupDir = resolveRestoreBackupDir();
		if (!hasFullCertSet(backupDir)) {
			Map<String, Object> err = new LinkedHashMap<>(buildCertsStatusMap());
			err.put("ok", false);
			err.put("error", "No certificate backup found in SHARED/backup (missing ca.crt, broker.crt, or broker.key)");
			return ResponseEntity.badRequest().body(err);
		}

		try {
			// First delete present certificates from SHARED/ if present
			executeHelperScript("removeCerts.sh", null);
			Files.deleteIfExists(sharedDir.resolve("ca.crt"));
			Files.deleteIfExists(sharedDir.resolve("broker.crt"));
			Files.deleteIfExists(sharedDir.resolve("broker.key"));

			// Copy certificates from backup directory into SHARED/
			Files.createDirectories(sharedDir);
			Files.copy(backupDir.resolve("ca.crt"), sharedDir.resolve("ca.crt"), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(backupDir.resolve("broker.crt"), sharedDir.resolve("broker.crt"), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(backupDir.resolve("broker.key"), sharedDir.resolve("broker.key"), StandardCopyOption.REPLACE_EXISTING);

			Map<String, Object> resp = new LinkedHashMap<>(buildCertsStatusMap());
			resp.put("restoredFrom", "SHARED/" + backupDir.getFileName().toString());
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>(buildCertsStatusMap());
			err.put("ok", false);
			err.put("error", "Failed to restore certificates: " + e.getMessage());
			return ResponseEntity.status(500).body(err);
		}
	}

	@PostMapping(value = "/certs/delete", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> deleteCerts() {
		try {
			executeHelperScript("removeCerts.sh", null);
			return ResponseEntity.ok(buildCertsStatusMap());
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>(buildCertsStatusMap());
			err.put("ok", false);
			err.put("error", "Failed to delete certificates: " + e.getMessage());
			return ResponseEntity.status(500).body(err);
		}
	}

	@PostMapping(value = "/certs/create", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> createOrReplaceCerts(@RequestBody(required = false) Map<String, Object> body) {
		try {
			Files.createDirectories(resolveSharedDir());
			executeHelperScript("removeCerts.sh", null);

			String commonName = "127.0.0.1";
			if (body != null && body.get("commonName") != null && !String.valueOf(body.get("commonName")).trim().isEmpty()) {
				commonName = String.valueOf(body.get("commonName")).trim();
			} else {
				try {
					Map<String, Object> mqttCfg = loadMqttConfFile(resolveMonitorDir().resolve("mqtt.conf"));
					String ip = String.valueOf(mqttCfg.getOrDefault("ip", "")).trim();
					if (!ip.isEmpty()) {
						commonName = ip;
					}
				} catch (Exception ignored) {
				}
			}

			String safeCommonName = commonName.replaceAll("[^A-Za-z0-9._-]", "");
			if (safeCommonName.isEmpty()) {
				safeCommonName = "127.0.0.1";
			}

			executeHelperScript(
					"createCerts.sh",
					null,
					Map.of(
							"FN_NON_INTERACTIVE", "1",
							"FN_COMMON_NAME", safeCommonName
					)
			);

			Map<String, Object> status = new LinkedHashMap<>(buildCertsStatusMap());
			if (!Boolean.TRUE.equals(status.get("present"))) {
				status.put("ok", false);
				status.put("error", "createCerts.sh completed, but certificates were not found in SHARED/");
				return ResponseEntity.status(500).body(status);
			}
			return ResponseEntity.ok(status);
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>(buildCertsStatusMap());
			err.put("ok", false);
			err.put("error", "Failed to create certificates: " + e.getMessage());
			return ResponseEntity.status(500).body(err);
		}
	}

	@GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getConfig() {
		if (isBridgeListening()) {
			return proxyGet("/api/config");
		}
		try {
			Path confPath = resolveMonitorDir().resolve("mqtt.conf");
			Map<String, Object> cfg = loadMqttConfFile(confPath);
			String json = String.format(
					"{\"ok\":true,\"remoteMqtt\":%s,\"confPath\":\"UI/forge-node/src/main/resources/static/mqtt-monitor/mqtt.conf\",\"connected\":false,\"config\":{\"configured\":%s,\"ip\":\"%s\",\"port\":%s,\"username\":\"%s\",\"password\":\"%s\",\"ca_cert_path\":\"%s\",\"broker_cert_path\":\"%s\",\"subscribe_admin\":%s}}",
					remoteMqtt,
					cfg.get("configured"),
					escapeJson(String.valueOf(cfg.get("ip"))),
					cfg.get("port"),
					escapeJson(String.valueOf(cfg.get("username"))),
					escapeJson(String.valueOf(cfg.get("password"))),
					escapeJson(String.valueOf(cfg.get("ca_cert_path"))),
					escapeJson(String.valueOf(cfg.get("broker_cert_path"))),
					cfg.get("subscribe_admin")
			);
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("{\"ok\":false}");
		}
	}

	@PostMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> saveConfig(@RequestBody(required = false) String rawBody) {
		try {
			Path confPath = resolveMonitorDir().resolve("mqtt.conf");
			Map<String, Object> cfg = loadMqttConfFile(confPath);

			@SuppressWarnings("unchecked")
			Map<String, Object> body = (rawBody == null || rawBody.isBlank())
					? Map.of()
					: objectMapper.readValue(rawBody, Map.class);

			String ip = body.containsKey("ip") ? String.valueOf(body.get("ip")).trim() : String.valueOf(cfg.get("ip")).trim();
			if (ip.isEmpty()) {
				return ResponseEntity.badRequest()
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"ok\":false,\"error\":\"MQTT server IP address is mandatory\"}");
			}

			int port;
			try {
				Object portObj = body.containsKey("port") ? body.get("port") : cfg.get("port");
				port = Integer.parseInt(String.valueOf(portObj).trim());
				if (port < 1 || port > 65535) {
					throw new NumberFormatException("out of range");
				}
			} catch (Exception e) {
				return ResponseEntity.badRequest()
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"ok\":false,\"error\":\"Port number must be between 1 and 65535\"}");
			}

			String username = body.containsKey("username")
					? String.valueOf(body.get("username")).trim()
					: String.valueOf(cfg.get("username")).trim();
			String password = body.containsKey("password")
					? (body.get("password") == null ? "" : String.valueOf(body.get("password")))
					: String.valueOf(cfg.get("password"));

			String caCertPath = body.containsKey("ca_cert_path")
					? String.valueOf(body.get("ca_cert_path")).trim()
					: (body.containsKey("caCertPath")
							? String.valueOf(body.get("caCertPath")).trim()
							: String.valueOf(cfg.get("ca_cert_path")).trim());

			String brokerCertPath = body.containsKey("broker_cert_path")
					? String.valueOf(body.get("broker_cert_path")).trim()
					: (body.containsKey("brokerCertPath")
							? String.valueOf(body.get("brokerCertPath")).trim()
							: String.valueOf(cfg.get("broker_cert_path")).trim());

			String caCertContent = body.get("caCertContent") == null ? "" : String.valueOf(body.get("caCertContent")).trim();
			String caCertFileName = body.get("caCertFileName") == null ? "" : String.valueOf(body.get("caCertFileName")).trim();
			if (!caCertContent.isEmpty()) {
				caCertPath = persistUploadedCert(caCertFileName.isEmpty() ? "ca.crt" : caCertFileName, caCertContent).toString();
			} else if (caCertPath.isEmpty()) {
				caCertPath = resolveSharedCert("ca.crt").toString();
			}

			String brokerCertContent = body.get("brokerCertContent") == null ? "" : String.valueOf(body.get("brokerCertContent")).trim();
			String brokerCertFileName = body.get("brokerCertFileName") == null ? "" : String.valueOf(body.get("brokerCertFileName")).trim();
			if (!brokerCertContent.isEmpty()) {
				brokerCertPath = persistUploadedCert(brokerCertFileName.isEmpty() ? "broker.crt" : brokerCertFileName, brokerCertContent).toString();
			} else if (brokerCertPath.isEmpty()) {
				brokerCertPath = resolveSharedCert("broker.crt").toString();
			}

			boolean configured = body.containsKey("configured")
					? Boolean.parseBoolean(String.valueOf(body.get("configured")))
					: true;
			boolean subscribeAdmin = body.containsKey("subscribe_admin")
					? Boolean.parseBoolean(String.valueOf(body.get("subscribe_admin")))
					: (body.containsKey("subscribeAdmin")
							? Boolean.parseBoolean(String.valueOf(body.get("subscribeAdmin")))
							: Boolean.parseBoolean(String.valueOf(cfg.get("subscribe_admin"))));

			cfg.put("configured", configured);
			cfg.put("ip", ip);
			cfg.put("port", port);
			cfg.put("username", username);
			cfg.put("password", password);
			cfg.put("ca_cert_path", caCertPath);
			cfg.put("broker_cert_path", brokerCertPath);
			cfg.put("subscribe_admin", subscribeAdmin);

			writeMqttConfFile(confPath, cfg);

			Map<String, Object> resp = new LinkedHashMap<>();
			resp.put("ok", true);
			resp.put("remoteMqtt", remoteMqtt);
			resp.put("confPath", "UI/forge-node/src/main/resources/static/mqtt-monitor/mqtt.conf");
			resp.put("config", cfg);
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(objectMapper.writeValueAsString(resp));
		} catch (Exception e) {
			return ResponseEntity.status(500)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
		}
	}

	private Path persistUploadedCert(String fileName, String content) throws Exception {
		Path backupDir = resolveProjectRoot().resolve("SHARED").resolve("backup");
		Files.createDirectories(backupDir);
		String safeName = Paths.get(fileName).getFileName().toString();
		if (safeName.isEmpty()) {
			safeName = "cert.crt";
		}
		Path target = backupDir.resolve(safeName);
		Files.writeString(target, content, StandardCharsets.UTF_8);
		return target.toAbsolutePath().normalize();
	}

	private void writeMqttConfFile(Path confPath, Map<String, Object> cfg) throws Exception {
		Files.createDirectories(confPath.getParent());
		StringBuilder sb = new StringBuilder();
		sb.append("# Forge Node MQTT Monitor — Saved Connection Configuration\n");
		sb.append("# Once you connect through the web UI dialog, 'configured' is set to true\n");
		sb.append("# and the page will connect automatically on startup using these credentials.\n\n");
		sb.append("configured = ").append(Boolean.TRUE.equals(cfg.get("configured")) ? "true" : "false").append("\n");
		sb.append("ip = ").append(cfg.get("ip")).append("\n");
		sb.append("port = ").append(cfg.get("port")).append("\n");
		sb.append("username = ").append(cfg.get("username")).append("\n");
		sb.append("password = ").append(cfg.get("password")).append("\n");
		sb.append("ca_cert_path = ").append(cfg.get("ca_cert_path")).append("\n");
		sb.append("broker_cert_path = ").append(cfg.get("broker_cert_path")).append("\n");
		sb.append("subscribe_admin = ").append(Boolean.TRUE.equals(cfg.get("subscribe_admin")) ? "true" : "false").append("\n");
		if (cfg.get("remote_mqtt") instanceof Boolean rm) {
			sb.append("remote_mqtt = ").append(rm ? "true" : "false").append("\n");
		}
		Files.writeString(confPath, sb.toString(), StandardCharsets.UTF_8);
	}

	@PostMapping(value = "/connect", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> connect(@RequestBody(required = false) String rawBody) {
		return proxyPost("/api/connect", rawBody);
	}

	@PostMapping(value = "/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> subscribe(@RequestBody(required = false) String rawBody) {
		return proxyPost("/api/subscribe", rawBody);
	}

	@PostMapping(value = "/disconnect", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> disconnect(@RequestBody(required = false) String rawBody) {
		return proxyPost("/api/disconnect", rawBody);
	}

	@PostMapping(value = "/session-close", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> sessionClose(@RequestBody(required = false) String rawBody) {
		if (isBridgeListening()) {
			return proxyPost("/api/session-close", rawBody);
		}
		return ResponseEntity.ok("{\"ok\":true}");
	}

	@PostMapping(value = "/publish", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> publish(@RequestBody(required = false) String rawBody) {
		return proxyPost("/api/publish", rawBody);
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() {
		SseEmitter emitter = new SseEmitter(0L);
		sseExecutor.submit(() -> {
			try {
				HttpRequest req = HttpRequest.newBuilder()
						.uri(URI.create(bridgeBase() + "/api/stream"))
						.GET()
						.build();
				HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("data:")) {
							String payload = line.substring(5).trim();
							emitter.send(SseEmitter.event().data(payload));
						} else if (line.startsWith(":")) {
							emitter.send(SseEmitter.event().comment("keepalive"));
						}
					}
				}
				emitter.complete();
			} catch (Exception e) {
				emitter.complete();
			}
		});
		return emitter;
	}

	private ResponseEntity<String> proxyGet(String path) {
		try {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(bridgeBase() + path))
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			return ResponseEntity.status(resp.statusCode())
					.contentType(MediaType.APPLICATION_JSON)
					.body(resp.body());
		} catch (Exception e) {
			return ResponseEntity.status(502)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
		}
	}

	private ResponseEntity<String> proxyPost(String path, String rawBody) {
		try {
			String body = (rawBody == null || rawBody.isBlank()) ? "{}" : rawBody;
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(bridgeBase() + path))
					.timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
					.build();
			HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			return ResponseEntity.status(resp.statusCode())
					.contentType(MediaType.APPLICATION_JSON)
					.body(resp.body());
		} catch (Exception e) {
			return ResponseEntity.status(502)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
		}
	}

	private Map<String, Object> loadMqttConfFile(Path confPath) {
		Map<String, Object> cfg = new LinkedHashMap<>();
		cfg.put("configured", false);
		cfg.put("ip", "192.168.0.24");
		cfg.put("port", 8883);
		cfg.put("username", "admin");
		cfg.put("password", "");
		cfg.put("ca_cert_path", resolveSharedCert("ca.crt").toString());
		cfg.put("broker_cert_path", resolveSharedCert("broker.crt").toString());
		cfg.put("subscribe_admin", true);

		if (!Files.exists(confPath)) {
			return cfg;
		}
		try {
			List<String> lines = Files.readAllLines(confPath, StandardCharsets.UTF_8);
			for (String raw : lines) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
					continue;
				}
				int eq = line.indexOf('=');
				String key = line.substring(0, eq).trim();
				String val = line.substring(eq + 1).trim();
				if ("configured".equals(key) || "subscribe_admin".equals(key)) {
					cfg.put(key, "true".equalsIgnoreCase(val) || "1".equals(val) || "yes".equalsIgnoreCase(val));
				} else if ("remote_mqtt".equals(key)) {
					cfg.put("remote_mqtt", "true".equalsIgnoreCase(val) || "1".equals(val) || "yes".equalsIgnoreCase(val));
				} else if ("port".equals(key)) {
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

		// Always prefer SHARED/ca.crt and SHARED/broker.crt if present and config points to old/missing certs path
		Path sharedCa = resolveSharedCert("ca.crt");
		String curCa = String.valueOf(cfg.get("ca_cert_path"));
		if (Files.exists(sharedCa) && (curCa.contains("mqtt-monitor/certs") || !Files.exists(Paths.get(curCa)))) {
			cfg.put("ca_cert_path", sharedCa.toString());
		}
		Path sharedBroker = resolveSharedCert("broker.crt");
		String curBroker = String.valueOf(cfg.get("broker_cert_path"));
		if (Files.exists(sharedBroker) && (curBroker.contains("mqtt-monitor/certs") || !Files.exists(Paths.get(curBroker)))) {
			cfg.put("broker_cert_path", sharedBroker.toString());
		}

		return cfg;
	}

	private String escapeJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	// --- Package-private helpers for FullConfigurationService / InstanceConfigController ---

	boolean isRemoteMqtt() {
		return remoteMqtt;
	}

	void setRemoteMqttMode(boolean remote) {
		this.remoteMqtt = remote;
		persistRemoteMqttToMqttConf(remote);
	}

	boolean isExposeMqtt() {
		return exposeMqtt;
	}

	int getActiveBridgePort() {
		return activeBridgePort;
	}

	boolean isBridgeRunning() {
		return isBridgeListening();
	}

	String getDefaultScriptName() {
		return readDefaultMonitorScriptName();
	}

	boolean setDefaultScriptNameIfPresent(String rawName) {
		String clean = sanitizeFnFileName(rawName);
		if (clean == null) {
			return false;
		}
		Path scriptDir = resolveProjectRoot().resolve("AAU").resolve("script");
		Path scriptFile = scriptDir.resolve(clean).normalize();
		if (!scriptFile.startsWith(scriptDir.normalize()) || !Files.isRegularFile(scriptFile)) {
			return false;
		}
		try {
			Files.createDirectories(scriptDir);
			Files.writeString(resolveDefaultMonitorScriptConfPath(), "default_script=" + clean + "\n", StandardCharsets.UTF_8);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	String applyDefaultScriptAsOperational() {
		String defaultScript = readDefaultMonitorScriptName();
		if (defaultScript.isEmpty()) {
			return "";
		}
		Path scriptDir = resolveProjectRoot().resolve("AAU").resolve("script");
		Path source = scriptDir.resolve(defaultScript).normalize();
		if (!source.startsWith(scriptDir.normalize()) || !Files.isRegularFile(source)) {
			return "";
		}
		try {
			String content = Files.readString(source, StandardCharsets.UTF_8);
			Path workingFn = scriptDir.resolve("working.fn").normalize();
			if (!source.equals(workingFn)) {
				Files.writeString(workingFn, content, StandardCharsets.UTF_8);
			}
			Path scriptFn = scriptDir.resolve("script.fn").normalize();
			if (!source.equals(scriptFn)) {
				Files.writeString(scriptFn, content, StandardCharsets.UTF_8);
			}
			return defaultScript;
		} catch (Exception e) {
			return "";
		}
	}

	Map<String, Object> getMonitorConfigSnapshot() {
		return loadMqttConfFile(resolveMonitorDir().resolve("mqtt.conf"));
	}

	String getDefaultMonitorConfigName() {
		return readDefaultMonitorConfigName();
	}

	void applyMonitorSnapshot(String defaultConfigName, Map<String, Object> monitorFields) {
		String cleanDef = sanitizeConfFileName(defaultConfigName);
		if (cleanDef != null) {
			Path dir = resolveMonitorConfigsDir();
			Path target = dir.resolve(cleanDef).normalize();
			if (target.startsWith(dir.normalize()) && Files.isRegularFile(target)) {
				try {
					Files.createDirectories(dir);
					Files.writeString(dir.resolve("default-configuration"), cleanDef, StandardCharsets.UTF_8);
					applyRemoteConfigFromContent(Files.readString(target, StandardCharsets.UTF_8));
				} catch (Exception ignored) {
				}
			}
		}
		if (monitorFields != null && !monitorFields.isEmpty()) {
			try {
				Path confPath = resolveMonitorDir().resolve("mqtt.conf");
				Map<String, Object> cfg = loadMqttConfFile(confPath);
				if (monitorFields.containsKey("configured")) cfg.put("configured", monitorFields.get("configured"));
				if (monitorFields.get("ip") != null && !String.valueOf(monitorFields.get("ip")).isBlank()) {
					cfg.put("ip", String.valueOf(monitorFields.get("ip")).trim());
				}
				if (monitorFields.get("port") != null) cfg.put("port", monitorFields.get("port"));
				if (monitorFields.containsKey("username")) cfg.put("username", monitorFields.get("username"));
				if (monitorFields.containsKey("password")) cfg.put("password", monitorFields.get("password"));
				if (monitorFields.get("ca_cert_path") != null && !String.valueOf(monitorFields.get("ca_cert_path")).isBlank()) {
					cfg.put("ca_cert_path", resolveUploadedCertPath(String.valueOf(monitorFields.get("ca_cert_path")), "ca.crt"));
				}
				if (monitorFields.get("broker_cert_path") != null && !String.valueOf(monitorFields.get("broker_cert_path")).isBlank()) {
					cfg.put("broker_cert_path", resolveUploadedCertPath(String.valueOf(monitorFields.get("broker_cert_path")), "broker.crt"));
				}
				if (monitorFields.containsKey("subscribe_admin")) cfg.put("subscribe_admin", monitorFields.get("subscribe_admin"));
				if (monitorFields.containsKey("remote_mqtt")) cfg.put("remote_mqtt", monitorFields.get("remote_mqtt"));
				writeMqttConfFile(confPath, cfg);
			} catch (Exception ignored) {
			}
		}
	}

	String getDefaultLocalMqttConfigName() {
		return readDefaultLocalMqttConfigName();
	}

	boolean isLocalMqttConfigPresent() {
		Path p = resolveLocalMqttConfPath();
		return Files.exists(p) && Files.isRegularFile(p);
	}

	boolean isLocalMqttConfigApplied() {
		return isLocalConfigApplied();
	}

	boolean isLocalMqttServerRunning() {
		return isMosquittoServiceActive();
	}

	Map<String, String> getLocalMqttDetailsSnapshot() {
		Path localConfPath = resolveLocalMqttConfPath();
		if (!Files.isRegularFile(localConfPath)) {
			return Map.of();
		}
		try {
			return parseLocalMqttConf(Files.readString(localConfPath, StandardCharsets.UTF_8));
		} catch (Exception e) {
			return Map.of();
		}
	}

	Map<String, String> getThemeConfigSnapshot() {
		return readSavedThemeConfig();
	}

	void applyThemeSnapshot(String colorTheme, String uiStyle, String iconPack) {
		try {
			Map<String, String> cur = readSavedThemeConfig();
			String c = (colorTheme != null && !colorTheme.isBlank()) ? colorTheme.trim() : cur.get("colorTheme");
			String u = (uiStyle != null && !uiStyle.isBlank()) ? uiStyle.trim() : cur.get("uiStyle");
			String i = (iconPack != null && !iconPack.isBlank()) ? iconPack.trim() : cur.get("iconPack");
			Path themesDir = resolveThemesDir();
			Files.createDirectories(themesDir);
			String content = "theme=" + c + "\n"
					+ "uiStyle=" + u + "\n"
					+ "iconPack=" + i + "\n";
			Files.writeString(themesDir.resolve("theme-config.conf"), content, StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}

	void restoreAndStartLocalMqtt(String localDefaultName, Map<String, String> localDetails) throws Exception {
		String cleanDef = sanitizeConfFileName(localDefaultName);
		boolean appliedConf = false;
		if (cleanDef != null) {
			Path dir = resolveLocalMqttConfigsDir();
			Path source = dir.resolve(cleanDef).normalize();
			if (source.startsWith(dir.normalize()) && Files.isRegularFile(source)) {
				Files.createDirectories(dir);
				Files.writeString(dir.resolve("default-configuration"), cleanDef, StandardCharsets.UTF_8);
				ResponseEntity<Map<String, Object>> res = applyLocalConfigFromContent(Files.readString(source, StandardCharsets.UTF_8));
				if (res.getStatusCode().is2xxSuccessful()) {
					appliedConf = true;
				}
			}
		}
		if (!appliedConf && localDetails != null && localDetails.get("users") != null && !localDetails.get("users").isBlank()) {
			StringBuilder sb = new StringBuilder();
			sb.append("config_type=local\n");
			sb.append("address=").append(localDetails.getOrDefault("address", "0.0.0.0")).append("\n");
			sb.append("port=").append(localDetails.getOrDefault("port", "8883")).append("\n");
			sb.append("users=").append(localDetails.get("users")).append("\n");
			sb.append("ca.crt=").append(localDetails.getOrDefault("ca.crt", "")).append("\n");
			sb.append("broker.crt=").append(localDetails.getOrDefault("broker.crt", "")).append("\n");
			sb.append("broker.key=").append(localDetails.getOrDefault("broker.key", "")).append("\n");
			ResponseEntity<Map<String, Object>> res = applyLocalConfigFromContent(sb.toString());
			if (!res.getStatusCode().is2xxSuccessful()) {
				throw new RuntimeException(String.valueOf(res.getBody() != null ? res.getBody().get("error") : "Failed to apply saved local MQTT config"));
			}
		}
		if (!isLocalMqttConfigPresent()) {
			throw new RuntimeException("Local MQTT configuration is missing (helpers/mqtt-configuration.conf)");
		}
		applyLocalMosquittoSetupInternal();
		startLocalMosquittoServerInternal();
	}

	void startBridgeOnPort(int port) {
		int targetPort = (port >= 1000 && port <= 65535) ? port : DEFAULT_BRIDGE_PORT;
		startBridge(Map.of("port", targetPort));
	}
}
