package com.aleksandar_damnjanovic.forge_node;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/aau")
public class AauConfigController {

	private static final List<String> REQUIRED_PATH_KEYS = List.of(
			"scriptFile",
			"logFile",
			"certificate",
			"serverKey",
			"serverCertificate"
	);

	private volatile boolean remoteAau = true;
	private Process localAauProcess;

	public AauConfigController() {
	}

	private Path resolveProjectRoot() {
		Path cwd = Paths.get("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && cwd != null; i++) {
			if (Files.isDirectory(cwd.resolve("AAU")) || Files.isDirectory(cwd.resolve("SHARED"))) {
				return cwd;
			}
			cwd = cwd.getParent();
		}
		return Paths.get("").toAbsolutePath().normalize();
	}

	private Path resolveAauDir() {
		return resolveProjectRoot().resolve("AAU");
	}

	private Path resolveConfigPath() {
		return resolveAauDir().resolve("configuration.conf");
	}

	private Path resolveLocalAauConfigsDir() {
		return resolveAauDir().resolve("script").resolve("localAAUconfigurations");
	}

	private String readDefaultAauConfigName() {
		Path defaultFile = resolveLocalAauConfigsDir().resolve("default-configuration");
		if (!Files.isRegularFile(defaultFile)) {
			return "";
		}
		try {
			return Files.readString(defaultFile, StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			return "";
		}
	}

	private String sanitizeAauConfFileName(String rawName) {
		if (rawName == null || rawName.isBlank()) {
			return null;
		}
		String name = Paths.get(rawName.trim()).getFileName().toString().trim();
		if (name.isEmpty() || ".".equals(name) || ".." .equals(name) || name.contains("/") || name.contains("\\")) {
			return null;
		}
		if ("default-configuration".equalsIgnoreCase(name)) {
			return null;
		}
		if (!name.toLowerCase().endsWith(".conf")) {
			name = name + ".conf";
		}
		return name;
	}

	private boolean isAauConfigReadyInContainer(Path confPath) {
		if (!Files.isRegularFile(confPath)) {
			return false;
		}
		Map<String, Object> cfg = readConfigurationConf(confPath);
		Path script = resolvePathWithinContainer(stringVal(cfg.get("scriptFile")));
		Path ca = resolvePathWithinContainer(stringVal(cfg.get("certificate")));
		return script != null && Files.isRegularFile(script)
				&& ca != null && Files.isRegularFile(ca)
				&& !stringVal(cfg.get("password")).isEmpty();
	}

	private Path resolveAauModePath() {
		return resolveLocalAauConfigsDir().resolve("aau-mode");
	}

	private boolean resolveInitialRemoteAauFromConfig() {
		Path modeFile = resolveAauModePath();
		if (Files.isRegularFile(modeFile)) {
			try {
				String raw = Files.readString(modeFile, StandardCharsets.UTF_8).trim();
				if (raw.contains("=")) {
					raw = raw.substring(raw.indexOf('=') + 1).trim();
				}
				if ("false".equalsIgnoreCase(raw) || "local".equalsIgnoreCase(raw) || "0".equals(raw)) {
					return false;
				}
				if ("true".equalsIgnoreCase(raw) || "remote".equalsIgnoreCase(raw) || "1".equals(raw)) {
					return true;
				}
			} catch (Exception ignored) {
			}
		}
		if (!readDefaultAauConfigName().isEmpty()) {
			return false;
		}
		return true;
	}

	private void persistRemoteAauToConfig(boolean newRemoteAau) {
		try {
			Path modeFile = resolveAauModePath();
			Files.createDirectories(modeFile.getParent());
			Files.writeString(modeFile, "remote_aau=" + newRemoteAau + "\n", StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}

	@PostConstruct
	public void applyDefaultAauConfigOnStartup() {
		applyDefaultAauPresetIfPresent();
		this.remoteAau = resolveInitialRemoteAauFromConfig();
	}

	private void applyDefaultAauPresetIfPresent() {
		String defaultName = sanitizeAauConfFileName(readDefaultAauConfigName());
		if (defaultName == null) {
			return;
		}
		Path dir = resolveLocalAauConfigsDir();
		Path source = dir.resolve(defaultName).normalize();
		if (!source.startsWith(dir.normalize()) || !Files.isRegularFile(source)) {
			return;
		}
		try {
			Map<String, Object> cfg = readConfigurationConf(source);
			Path confPath = resolveConfigPath();
			Files.createDirectories(confPath.getParent());
			Files.writeString(confPath, formatConfigurationConf(cfg), StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}

	public synchronized boolean isLocalAauRunning() {
		return localAauProcess != null && localAauProcess.isAlive();
	}

	@PreDestroy
	public synchronized void stopLocalAauOnShutdown() {
		if (localAauProcess != null && localAauProcess.isAlive()) {
			localAauProcess.destroy();
		}
	}

	@GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getStatus() {
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("remoteAau", remoteAau);
		resp.put("localAauRunning", isLocalAauRunning());
		return ResponseEntity.ok(resp);
	}

	@PostMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> updateAauSettings(@RequestBody Map<String, Object> body) {
		if (body != null && body.containsKey("remoteAau")) {
			this.remoteAau = Boolean.parseBoolean(String.valueOf(body.get("remoteAau")));
			persistRemoteAauToConfig(this.remoteAau);
		}
		if (!this.remoteAau && !isAauConfigReadyInContainer(resolveConfigPath())) {
			applyDefaultAauPresetIfPresent();
			if (!isAauConfigReadyInContainer(resolveConfigPath())) {
				syncConfigFromLocalMqttInternal(true);
			}
		}
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("remoteAau", this.remoteAau);
		return ResponseEntity.ok(resp);
	}

	@GetMapping(value = "/logs", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getAauLogs() {
		Path defaultLogPath = resolveAauDir().resolve("script").resolve("log.txt");
		Path logPath = defaultLogPath;
		Map<String, Object> cfg = readConfigurationConf(resolveConfigPath());
		String configuredLog = stringVal(cfg.get("logFile"));
		if (!configuredLog.isEmpty()) {
			Path resolved = resolvePathWithinContainer(configuredLog);
			if (resolved != null) {
				logPath = resolved;
			}
		}
		String content = "";
		boolean exists = Files.isRegularFile(logPath);
		if (exists) {
			try {
				content = Files.readString(logPath, StandardCharsets.UTF_8);
			} catch (Exception ignored) {
			}
		}
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("exists", exists);
		resp.put("path", logPath.toAbsolutePath().normalize().toString());
		resp.put("content", content);
		return ResponseEntity.ok(resp);
	}

	@PostMapping(value = "/start", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> startLocalAau() {
		if (isLocalAauRunning()) {
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"localAauRunning", true,
					"message", "Local AAU is already running"
			));
		}

		Path aauDir = resolveAauDir();
		Path basePy = aauDir.resolve("base.py");
		if (!Files.exists(basePy)) {
			return ResponseEntity.status(404).body(Map.of(
					"ok", false,
					"localAauRunning", false,
					"error", "AAU/base.py not found"
			));
		}

		try {
			if (!isAauConfigReadyInContainer(resolveConfigPath())) {
				applyDefaultAauPresetIfPresent();
				if (!isAauConfigReadyInContainer(resolveConfigPath())) {
					syncConfigFromLocalMqttInternal(true);
				}
			}
			Path startupLog = Files.createTempFile("forge-node-aau-", ".log");
			ProcessBuilder pb = new ProcessBuilder("python3", "base.py");
			pb.directory(aauDir.toFile());
			pb.environment().remove("FN_ADMIN_USER");
			pb.environment().remove("FN_ADMIN_PASSWORD");
			pb.redirectErrorStream(true);
			pb.redirectOutput(startupLog.toFile());
			localAauProcess = pb.start();

			Thread.sleep(300);
			if (!localAauProcess.isAlive()) {
				int code = localAauProcess.exitValue();
				localAauProcess = null;
				String errOut = "";
				try {
					errOut = Files.readString(startupLog, StandardCharsets.UTF_8).trim();
					Files.deleteIfExists(startupLog);
				} catch (Exception ignored) {
				}
				String errMsg = errOut.isEmpty()
						? "AAU/base.py exited immediately with code " + code
						: "AAU/base.py exited with code " + code + ": " + errOut;
				return ResponseEntity.status(500).body(Map.of(
						"ok", false,
						"localAauRunning", false,
						"error", errMsg
				));
			}
			startupLog.toFile().deleteOnExit();

			return ResponseEntity.ok(Map.of(
					"ok", true,
					"localAauRunning", true
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"localAauRunning", false,
					"error", "Failed to start AAU/base.py: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/stop", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> stopLocalAau() {
		if (localAauProcess != null && localAauProcess.isAlive()) {
			localAauProcess.destroy();
			localAauProcess = null;
		}
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"localAauRunning", false
		));
	}

	@GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> listVolumeFiles() {
		Path projectRoot = resolveProjectRoot();
		List<Map<String, String>> files = new ArrayList<>();

		Path scriptDir = projectRoot.resolve("AAU").resolve("script");
		collectFiles(scriptDir, "script/", "AAU/script", files);
		Path defaultLog = scriptDir.resolve("log.txt").toAbsolutePath().normalize();
		if (!Files.exists(defaultLog)) {
			files.add(Map.of(
					"name", "log.txt",
					"group", "AAU/script",
					"relativePath", "script/log.txt",
					"absolutePath", defaultLog.toString()
			));
		}
		collectFiles(projectRoot.resolve("AAU").resolve("certs"), "certs/", "AAU/certs", files);
		collectFiles(projectRoot.resolve("SHARED"), "../SHARED/", "SHARED", files);

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

	@GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getConfig() {
		Path confPath = resolveConfigPath();
		Map<String, Object> cfg = (!remoteAau && !isAauConfigReadyInContainer(confPath))
				? syncConfigFromLocalMqttInternal(false)
				: readConfigurationConf(confPath);
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("remoteAau", remoteAau);
		resp.put("localAauRunning", isLocalAauRunning());
		resp.put("localMqttConfigFound", Files.isRegularFile(resolveLocalMqttConfPath()));
		resp.put("defaultConfig", readDefaultAauConfigName());
		resp.put("path", "AAU/configuration.conf");
		resp.put("config", cfg);
		return ResponseEntity.ok(resp);
	}

	@PostMapping(value = "/sync-local-mqtt", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> syncFromLocalMqtt() {
		if (!Files.isRegularFile(resolveLocalMqttConfPath())) {
			return ResponseEntity.status(404).body(Map.of(
					"ok", false,
					"error", "helpers/mqtt-configuration.conf was not found"
			));
		}
		Map<String, Object> cfg = syncConfigFromLocalMqttInternal(true);
		return ResponseEntity.ok(Map.of(
				"ok", true,
				"path", "AAU/configuration.conf",
				"config", cfg
		));
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

	private Map<String, Object> syncConfigFromLocalMqttInternal(boolean writeToDisk) {
		Path confPath = resolveConfigPath();
		Map<String, Object> cfg = readConfigurationConf(confPath);
		Path localMqttConf = resolveLocalMqttConfPath();
		if (!Files.isRegularFile(localMqttConf)) {
			return cfg;
		}

		try {
			String content = Files.readString(localMqttConf, StandardCharsets.UTF_8);
			Map<String, String> local = parseKeyValueLines(content);
			Path projectRoot = resolveProjectRoot();
			Path aauDir = resolveAauDir();

			String currentScript = stringVal(cfg.get("scriptFile"));
			cfg.put("scriptFile", resolveScriptRelativePath(aauDir, currentScript));
			cfg.put("backupScriptFile", "");
			cfg.put("logFile", "script/log.txt");

			cfg.put("certificate", toAauRelativeCertPath(aauDir, projectRoot, local.get("ca.crt"), "ca.crt"));
			cfg.put("serverKey", toAauRelativeCertPath(aauDir, projectRoot, local.get("broker.key"), "broker.key"));
			cfg.put("serverCertificate", toAauRelativeCertPath(aauDir, projectRoot, local.get("broker.crt"), "broker.crt"));

			String addr = stringVal(local.get("address"));
			if (addr.isEmpty() || "0.0.0.0".equals(addr) || "::".equals(addr)) {
				addr = "127.0.0.1";
			}
			cfg.put("address", addr);

			String portStr = stringVal(local.get("port"));
			if (!portStr.isEmpty()) {
				try {
					int p = Integer.parseInt(portStr);
					if (p >= 1 && p <= 65535) {
						cfg.put("port", p);
					}
				} catch (NumberFormatException ignored) {
				}
			}

			String[] adminCreds = extractAdminCredentials(local.get("users"));
			if (!adminCreds[0].isEmpty()) {
				cfg.put("user", adminCreds[0]);
			}
			if (!adminCreds[1].isEmpty()) {
				cfg.put("password", adminCreds[1]);
			}

			if (writeToDisk) {
				Files.createDirectories(confPath.getParent());
				Files.writeString(confPath, formatConfigurationConf(cfg), StandardCharsets.UTF_8);
			}
		} catch (Exception ignored) {
		}
		return cfg;
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

	private String resolveScriptRelativePath(Path aauDir, String currentScriptFile) {
		Path scriptDir = aauDir.resolve("script");
		if (currentScriptFile != null && !currentScriptFile.isBlank() && !isAbsolutePath(currentScriptFile)) {
			Path candidate = aauDir.resolve(currentScriptFile).normalize();
			if (candidate.startsWith(scriptDir) && Files.isRegularFile(candidate)) {
				return currentScriptFile;
			}
		}
		if (Files.isRegularFile(scriptDir.resolve("script.fn"))) {
			return "script/script.fn";
		}
		if (Files.isDirectory(scriptDir)) {
			try (Stream<Path> stream = Files.list(scriptDir)) {
				List<String> fnFiles = stream
						.filter(Files::isRegularFile)
						.map(p -> p.getFileName().toString())
						.filter(name -> name.toLowerCase().endsWith(".fn"))
						.sorted()
						.toList();
				for (String name : fnFiles) {
					if (!"working.fn".equals(name) && !"backup-script.fn".equals(name)) {
						return "script/" + name;
					}
				}
				if (!fnFiles.isEmpty()) {
					return "script/" + fnFiles.get(0);
				}
			} catch (Exception ignored) {
			}
		}
		return "script/script.fn";
	}

	private String toAauRelativeCertPath(Path aauDir, Path projectRoot, String configuredPath, String defaultFileName) {
		String fileName = defaultFileName;
		if (configuredPath != null && !configuredPath.isBlank()) {
			try {
				Path p = Paths.get(configuredPath.trim());
				if (!p.isAbsolute()) {
					p = projectRoot.resolve(p).normalize();
				}
				if (p.getFileName() != null) {
					fileName = p.getFileName().toString();
				}
				if (Files.isRegularFile(p) && p.normalize().startsWith(projectRoot.normalize())) {
					return aauDir.relativize(p.normalize()).toString().replace('\\', '/');
				}
			} catch (Exception ignored) {
			}
		}
		if (Files.isRegularFile(projectRoot.resolve("SHARED").resolve(fileName))) {
			return "../SHARED/" + fileName;
		}
		if (Files.isRegularFile(aauDir.resolve("certs").resolve(defaultFileName))) {
			return "certs/" + defaultFileName;
		}
		return "../SHARED/" + defaultFileName;
	}

	private String validateAndBuildConfig(Map<String, Object> body, Map<String, Object> saved) {
		if (body == null) {
			return "Missing request body";
		}

		for (String key : REQUIRED_PATH_KEYS) {
			String val = stringVal(body.get(key));
			if (val.isEmpty()) {
				return key + " is mandatory and must not be empty";
			}
			Path resolved = resolvePathWithinContainer(val);
			if (resolved == null) {
				return key + " must point inside the container (e.g. " + resolveProjectRoot() + "/AAU/... or relative to AAU/)";
			}
			if (!"logFile".equals(key) && !Files.isRegularFile(resolved)) {
				return key + " does not exist inside the container: " + resolved;
			}
		}

		String backupScriptFile = stringVal(body.get("backupScriptFile"));
		if (!backupScriptFile.isEmpty()) {
			Path resolvedBackup = resolvePathWithinContainer(backupScriptFile);
			if (resolvedBackup == null) {
				return "backupScriptFile must point inside the container";
			}
			if (!Files.isRegularFile(resolvedBackup)) {
				return "backupScriptFile does not exist inside the container: " + resolvedBackup;
			}
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

		saved.put("scriptFile", stringVal(body.get("scriptFile")));
		saved.put("backupScriptFile", backupScriptFile);
		saved.put("logFile", stringVal(body.get("logFile")));
		saved.put("certificate", stringVal(body.get("certificate")));
		saved.put("serverKey", stringVal(body.get("serverKey")));
		saved.put("serverCertificate", stringVal(body.get("serverCertificate")));
		saved.put("address", address);
		saved.put("port", port);
		saved.put("user", user);
		saved.put("password", password);
		return null;
	}

	@PostMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody Map<String, Object> body) {
		Map<String, Object> saved = new LinkedHashMap<>();
		String validationError = validateAndBuildConfig(body, saved);
		if (validationError != null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", validationError));
		}

		try {
			Path confPath = resolveConfigPath();
			Files.createDirectories(confPath.getParent());
			Files.writeString(confPath, formatConfigurationConf(saved), StandardCharsets.UTF_8);

			return ResponseEntity.ok(Map.of(
					"ok", true,
					"path", "AAU/configuration.conf",
					"config", saved
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to write AAU/configuration.conf: " + e.getMessage()
			));
		}
	}

	@GetMapping(value = "/configs/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> listSavedAauConfigs() {
		Path dir = resolveLocalAauConfigsDir();
		String defaultConfig = readDefaultAauConfigName();
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

	@PostMapping(value = "/configs/save", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> saveNamedAauConfig(@RequestBody Map<String, Object> body) {
		if (body == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing request body"));
		}
		String fileName = sanitizeAauConfFileName(body.get("fileName") == null ? "" : String.valueOf(body.get("fileName")));
		if (fileName == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Valid configuration name is required (cannot be 'default-configuration')"));
		}

		Map<String, Object> saved = new LinkedHashMap<>();
		String validationError = validateAndBuildConfig(body, saved);
		if (validationError != null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", validationError));
		}

		try {
			Path dir = resolveLocalAauConfigsDir();
			Files.createDirectories(dir);
			Path target = dir.resolve(fileName).normalize();
			if (!target.startsWith(dir.normalize())) {
				return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid file path"));
			}
			String content = formatConfigurationConf(saved);
			Files.writeString(target, content, StandardCharsets.UTF_8);

			boolean setAsDefault = Boolean.parseBoolean(String.valueOf(body.getOrDefault("setAsDefault", false)));
			if (setAsDefault) {
				Files.writeString(dir.resolve("default-configuration"), fileName, StandardCharsets.UTF_8);
				Path confPath = resolveConfigPath();
				Files.createDirectories(confPath.getParent());
				Files.writeString(confPath, content, StandardCharsets.UTF_8);
			}

			String activeDefault = readDefaultAauConfigName();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"fileName", fileName,
					"isDefault", fileName.equals(activeDefault),
					"defaultConfig", activeDefault,
					"path", "AAU/script/localAAUconfigurations/" + fileName,
					"config", saved
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to save AAU configuration: " + e.getMessage()
			));
		}
	}

	@PostMapping(value = "/configs/load", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> loadNamedAauConfig(@RequestBody Map<String, Object> body) {
		if (body == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing request body"));
		}
		String fileName = sanitizeAauConfFileName(body.get("fileName") == null ? "" : String.valueOf(body.get("fileName")));
		if (fileName == null) {
			return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Invalid configuration filename"));
		}

		try {
			Path dir = resolveLocalAauConfigsDir();
			Path source = dir.resolve(fileName).normalize();
			if (!source.startsWith(dir.normalize()) || !Files.isRegularFile(source)) {
				return ResponseEntity.status(404).body(Map.of(
						"ok", false,
						"error", "AAU configuration not found: " + fileName
				));
			}
			Map<String, Object> cfg = readConfigurationConf(source);
			Path confPath = resolveConfigPath();
			Files.createDirectories(confPath.getParent());
			Files.writeString(confPath, formatConfigurationConf(cfg), StandardCharsets.UTF_8);

			boolean setAsDefault = Boolean.parseBoolean(String.valueOf(body.getOrDefault("setAsDefault", false)));
			if (setAsDefault) {
				Files.createDirectories(dir);
				Files.writeString(dir.resolve("default-configuration"), fileName, StandardCharsets.UTF_8);
			}

			String activeDefault = readDefaultAauConfigName();
			return ResponseEntity.ok(Map.of(
					"ok", true,
					"fileName", fileName,
					"isDefault", fileName.equals(activeDefault),
					"defaultConfig", activeDefault,
					"config", cfg
			));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to load AAU configuration: " + e.getMessage()
			));
		}
	}

	private Map<String, Object> readConfigurationConf(Path confPath) {
		Map<String, Object> cfg = new LinkedHashMap<>();
		cfg.put("scriptFile", "script/script.fn");
		cfg.put("backupScriptFile", "script/backup-script.fn");
		cfg.put("logFile", "script/log.txt");
		cfg.put("certificate", "certs/ca.crt");
		cfg.put("serverKey", "certs/broker.key");
		cfg.put("serverCertificate", "certs/broker.crt");
		cfg.put("address", "192.168.0.24");
		cfg.put("port", 8883);
		cfg.put("user", "admin");
		cfg.put("password", "");

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

	private String formatConfigurationConf(Map<String, Object> cfg) {
		StringBuilder sb = new StringBuilder();
		sb.append("scriptFile = \"").append(escapeConf(cfg.get("scriptFile"))).append("\"\n");
		sb.append("backupScriptFile = \"").append(escapeConf(cfg.get("backupScriptFile"))).append("\"\n");
		sb.append("logFile = \"").append(escapeConf(cfg.get("logFile"))).append("\"\n");
		sb.append("certificate = \"").append(escapeConf(cfg.get("certificate"))).append("\"\n");
		sb.append("serverKey = \"").append(escapeConf(cfg.get("serverKey"))).append("\"\n");
		sb.append("serverCertificate = \"").append(escapeConf(cfg.get("serverCertificate"))).append("\"\n");
		sb.append("address = \"").append(escapeConf(cfg.get("address"))).append("\"\n");
		sb.append("port = ").append(cfg.get("port")).append("\n");
		sb.append("user = \"").append(escapeConf(cfg.get("user"))).append("\"\n");
		sb.append("password = \"").append(escapeConf(cfg.get("password"))).append("\"\n");
		return sb.toString();
	}

	private String stringVal(Object obj) {
		return obj == null ? "" : String.valueOf(obj).trim();
	}

	private Path resolvePathWithinContainer(String rawPath) {
		if (rawPath == null || rawPath.isBlank()) {
			return null;
		}
		String p = rawPath.trim();
		if (p.startsWith("\\") || (p.length() >= 2 && Character.isLetter(p.charAt(0)) && p.charAt(1) == ':')) {
			return null;
		}
		try {
			Path projectRoot = resolveProjectRoot().toAbsolutePath().normalize();
			Path candidate = p.startsWith("/")
					? Paths.get(p).toAbsolutePath().normalize()
					: resolveAauDir().resolve(p).toAbsolutePath().normalize();
			return candidate.startsWith(projectRoot) ? candidate : null;
		} catch (Exception e) {
			return null;
		}
	}

	private boolean isAbsolutePath(String p) {
		if (p.startsWith("/") || p.startsWith("\\")) {
			return true;
		}
		return p.length() >= 2 && Character.isLetter(p.charAt(0)) && p.charAt(1) == ':';
	}

	private String escapeConf(Object obj) {
		String s = obj == null ? "" : String.valueOf(obj);
		return s.replace("\"", "\\\"");
	}

	// --- Package-private helpers for InstanceConfigController ---

	boolean isRemoteAau() {
		return remoteAau;
	}

	synchronized void setRemoteAauInMemory(boolean remoteAau) {
		this.remoteAau = remoteAau;
		persistRemoteAauToConfig(remoteAau);
	}

	String getDefaultAauConfigName() {
		return readDefaultAauConfigName();
	}

	Map<String, Object> getAauConfigSnapshot() {
		return readConfigurationConf(resolveConfigPath());
	}

	synchronized void restorePreconfiguredAau(
			boolean newRemoteAau,
			String defaultConfigName,
			Map<String, Object> cfgFields
	) {
		setRemoteAauInMemory(newRemoteAau);
		String cleanDef = sanitizeAauConfFileName(defaultConfigName);
		boolean appliedFromPreset = false;
		if (cleanDef != null) {
			Path dir = resolveLocalAauConfigsDir();
			Path source = dir.resolve(cleanDef).normalize();
			if (source.startsWith(dir.normalize()) && Files.isRegularFile(source)) {
				try {
					Files.createDirectories(dir);
					Files.writeString(dir.resolve("default-configuration"), cleanDef, StandardCharsets.UTF_8);
					Map<String, Object> cfg = readConfigurationConf(source);
					Path confPath = resolveConfigPath();
					Files.createDirectories(confPath.getParent());
					Files.writeString(confPath, formatConfigurationConf(cfg), StandardCharsets.UTF_8);
					appliedFromPreset = true;
				} catch (Exception ignored) {
				}
			}
		}
		if (!appliedFromPreset && cfgFields != null && !stringVal(cfgFields.get("scriptFile")).isEmpty()) {
			try {
				Map<String, Object> merged = readConfigurationConf(resolveConfigPath());
				for (String k : List.of("scriptFile", "backupScriptFile", "logFile", "certificate", "serverKey", "serverCertificate", "address", "user", "password")) {
					if (cfgFields.containsKey(k)) {
						merged.put(k, stringVal(cfgFields.get(k)));
					}
				}
				if (cfgFields.get("port") != null) {
					try {
						int p = Integer.parseInt(String.valueOf(cfgFields.get("port")).trim());
						if (p >= 1 && p <= 65535) {
							merged.put("port", p);
						}
					} catch (NumberFormatException ignored) {
					}
				}
				Path confPath = resolveConfigPath();
				Files.createDirectories(confPath.getParent());
				Files.writeString(confPath, formatConfigurationConf(merged), StandardCharsets.UTF_8);
			} catch (Exception ignored) {
			}
		}
	}

	ResponseEntity<Map<String, Object>> startLocalAauForStartup() {
		return startLocalAau();
	}
}
