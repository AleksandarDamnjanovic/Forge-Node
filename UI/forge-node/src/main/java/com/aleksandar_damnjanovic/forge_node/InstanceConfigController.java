package com.aleksandar_damnjanovic.forge_node;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/instance")
public class InstanceConfigController {

	private static final Logger LOG = Logger.getLogger(InstanceConfigController.class.getName());
	private static final String FULL_CONFIG_REL_PATH = "AAU/script/full-configuration/full-configuration.conf";

	private final MqttBridgeController mqttBridgeController;
	private final AauConfigController aauConfigController;
	private final AuConfigController auConfigController;
	private final boolean runOnPreconfigured;

	public InstanceConfigController(
			MqttBridgeController mqttBridgeController,
			AauConfigController aauConfigController,
			AuConfigController auConfigController,
			@Value("${run-on-preconfigured:false}") String runOnPreconfiguredValue
	) {
		this.mqttBridgeController = mqttBridgeController;
		this.aauConfigController = aauConfigController;
		this.auConfigController = auConfigController;
		String v = runOnPreconfiguredValue == null ? "false" : runOnPreconfiguredValue.trim();
		this.runOnPreconfigured = "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
	}

	private Path resolveFullConfigFile() {
		return mqttBridgeController.resolveFullConfigDir().resolve("full-configuration.conf");
	}

	private Map<String, String> readFullConfigFile() {
		Map<String, String> map = new LinkedHashMap<>();
		Path file = resolveFullConfigFile();
		if (!Files.isRegularFile(file)) {
			return map;
		}
		try {
			for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
					continue;
				}
				int eq = line.indexOf('=');
				String key = line.substring(0, eq).trim();
				String val = line.substring(eq + 1).trim();
				map.put(key, val);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Failed to read full-configuration.conf: " + e.getMessage(), e);
		}
		return map;
	}

	private static boolean parseBool(String raw, boolean fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}
		String v = raw.trim();
		if ("true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v)) {
			return true;
		}
		if ("false".equalsIgnoreCase(v) || "0".equals(v) || "no".equalsIgnoreCase(v)) {
			return false;
		}
		return fallback;
	}

	private static int parseInt(String raw, int fallback, int min, int max) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}
		try {
			int val = Integer.parseInt(raw.trim());
			return (val >= min && val <= max) ? val : fallback;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static String sanitizeSingleLine(Object val) {
		if (val == null) {
			return "";
		}
		return String.valueOf(val).replace("\r", "").replace("\n", " ").trim();
	}

	@GetMapping(value = "/full-config", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getFullConfigStatus() {
		Path file = resolveFullConfigFile();
		boolean exists = Files.isRegularFile(file);
		Map<String, String> saved = exists ? readFullConfigFile() : Map.of();

		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("ok", true);
		resp.put("exists", exists);
		resp.put("path", FULL_CONFIG_REL_PATH);
		resp.put("runOnPreconfigured", runOnPreconfigured);
		resp.put("savedAt", saved.getOrDefault("saved_at", ""));
		resp.put("defaultScript", saved.getOrDefault("default_script", mqttBridgeController.getDefaultScriptName()));
		resp.put("bridgePort", parseInt(saved.get("bridge.port"), mqttBridgeController.getActiveBridgePort(), 1000, 65535));
		resp.put("auExposed", parseBool(saved.getOrDefault("au.exposed", saved.get("aau.exposeAu")), auConfigController.isExposeAu()));
		resp.put("auPort", parseInt(saved.getOrDefault("au.port", saved.get("aau.auPort")), auConfigController.getAuPort(), 1000, 65535));
		return ResponseEntity.ok(resp);
	}

	@PostMapping(value = "/full-config/save", produces = MediaType.APPLICATION_JSON_VALUE)
	public synchronized ResponseEntity<Map<String, Object>> saveFullConfiguration(
			@RequestBody(required = false) Map<String, Object> body
	) {
		try {
			if (body != null) {
				if (body.containsKey("remoteMqtt")) {
					mqttBridgeController.setRemoteMqttMode(Boolean.parseBoolean(String.valueOf(body.get("remoteMqtt"))));
				}
				if (body.containsKey("remoteAau")) {
					aauConfigController.setRemoteAauInMemory(Boolean.parseBoolean(String.valueOf(body.get("remoteAau"))));
				}
				boolean exposeAu = body.containsKey("exposeAu")
						? Boolean.parseBoolean(String.valueOf(body.get("exposeAu")))
						: auConfigController.isExposeAu();
				int auPort = body.get("auPort") != null
						? parseInt(String.valueOf(body.get("auPort")), auConfigController.getAuPort(), 1000, 65535)
						: auConfigController.getAuPort();
				auConfigController.setAuSettingsInMemory(exposeAu, auPort);
			}

			int bridgePort = (body != null && body.get("bridgePort") != null)
					? parseInt(String.valueOf(body.get("bridgePort")), mqttBridgeController.getActiveBridgePort(), 1000, 65535)
					: mqttBridgeController.getActiveBridgePort();

			String savedAt = Instant.now().toString();
			String defaultScript = mqttBridgeController.getDefaultScriptName();
			Map<String, String> theme = mqttBridgeController.getThemeConfigSnapshot();
			Map<String, Object> monitorCfg = mqttBridgeController.getMonitorConfigSnapshot();
			String monitorDefault = mqttBridgeController.getDefaultMonitorConfigName();
			String localMqttDefault = mqttBridgeController.getDefaultLocalMqttConfigName();
			Map<String, String> localMqttDetails = mqttBridgeController.getLocalMqttDetailsSnapshot();
			String aauDefault = aauConfigController.getDefaultAauConfigName();
			Map<String, Object> aauCfg = aauConfigController.getAauConfigSnapshot();
			Map<String, Object> auCfg = auConfigController.getAuConfigSnapshot();

			StringBuilder sb = new StringBuilder();
			sb.append("# Forge Node — Full Instance Configuration\n");
			sb.append("# Saved from Main Dashboard -> Save Present Forge Node Instance Configuration\n\n");
			sb.append("saved_at=").append(savedAt).append("\n");
			sb.append("default_script=").append(sanitizeSingleLine(defaultScript)).append("\n\n");

			sb.append("# Appearance\n");
			sb.append("theme.colorTheme=").append(sanitizeSingleLine(theme.get("colorTheme"))).append("\n");
			sb.append("theme.uiStyle=").append(sanitizeSingleLine(theme.get("uiStyle"))).append("\n");
			sb.append("theme.iconPack=").append(sanitizeSingleLine(theme.get("iconPack"))).append("\n\n");

			sb.append("# MQTT Monitor Settings\n");
			sb.append("monitor.defaultConfig=").append(sanitizeSingleLine(monitorDefault)).append("\n");
			sb.append("monitor.configured=").append(Boolean.TRUE.equals(monitorCfg.get("configured"))).append("\n");
			sb.append("monitor.ip=").append(sanitizeSingleLine(monitorCfg.get("ip"))).append("\n");
			sb.append("monitor.port=").append(sanitizeSingleLine(monitorCfg.get("port"))).append("\n");
			sb.append("monitor.username=").append(sanitizeSingleLine(monitorCfg.get("username"))).append("\n");
			sb.append("monitor.password=").append(sanitizeSingleLine(monitorCfg.get("password"))).append("\n");
			sb.append("monitor.caCertPath=").append(sanitizeSingleLine(monitorCfg.get("ca_cert_path"))).append("\n");
			sb.append("monitor.brokerCertPath=").append(sanitizeSingleLine(monitorCfg.get("broker_cert_path"))).append("\n");
			sb.append("monitor.subscribeAdmin=").append(!Boolean.FALSE.equals(monitorCfg.get("subscribe_admin"))).append("\n\n");

			sb.append("# MQTT Server & Bridge Settings\n");
			sb.append("mqtt.remoteMqtt=").append(mqttBridgeController.isRemoteMqtt()).append("\n");
			sb.append("mqtt.exposeMqtt=").append(mqttBridgeController.isExposeMqtt()).append("\n");
			sb.append("mqtt.localDefaultConfig=").append(sanitizeSingleLine(localMqttDefault)).append("\n");
			sb.append("mqtt.localConfigExists=").append(mqttBridgeController.isLocalMqttConfigPresent()).append("\n");
			sb.append("mqtt.localConfigApplied=").append(mqttBridgeController.isLocalMqttConfigApplied()).append("\n");
			sb.append("mqtt.localServerRunning=").append(mqttBridgeController.isLocalMqttServerRunning()).append("\n");
			sb.append("mqtt.local.address=").append(sanitizeSingleLine(localMqttDetails.get("address"))).append("\n");
			sb.append("mqtt.local.port=").append(sanitizeSingleLine(localMqttDetails.get("port"))).append("\n");
			sb.append("mqtt.local.users=").append(sanitizeSingleLine(localMqttDetails.get("users"))).append("\n");
			sb.append("mqtt.local.caCert=").append(sanitizeSingleLine(localMqttDetails.get("ca.crt"))).append("\n");
			sb.append("mqtt.local.brokerCert=").append(sanitizeSingleLine(localMqttDetails.get("broker.crt"))).append("\n");
			sb.append("mqtt.local.brokerKey=").append(sanitizeSingleLine(localMqttDetails.get("broker.key"))).append("\n");
			sb.append("bridge.running=").append(mqttBridgeController.isBridgeRunning()).append("\n");
			sb.append("bridge.port=").append(bridgePort).append("\n\n");

			sb.append("# AAU Settings\n");
			sb.append("aau.remoteAau=").append(aauConfigController.isRemoteAau()).append("\n");
			sb.append("aau.localDefaultConfig=").append(sanitizeSingleLine(aauDefault)).append("\n");
			sb.append("aau.localAauRunning=").append(aauConfigController.isLocalAauRunning()).append("\n");
			sb.append("aau.config.scriptFile=").append(sanitizeSingleLine(aauCfg.get("scriptFile"))).append("\n");
			sb.append("aau.config.backupScriptFile=").append(sanitizeSingleLine(aauCfg.get("backupScriptFile"))).append("\n");
			sb.append("aau.config.logFile=").append(sanitizeSingleLine(aauCfg.get("logFile"))).append("\n");
			sb.append("aau.config.certificate=").append(sanitizeSingleLine(aauCfg.get("certificate"))).append("\n");
			sb.append("aau.config.serverKey=").append(sanitizeSingleLine(aauCfg.get("serverKey"))).append("\n");
			sb.append("aau.config.serverCertificate=").append(sanitizeSingleLine(aauCfg.get("serverCertificate"))).append("\n");
			sb.append("aau.config.address=").append(sanitizeSingleLine(aauCfg.get("address"))).append("\n");
			sb.append("aau.config.port=").append(sanitizeSingleLine(aauCfg.get("port"))).append("\n");
			sb.append("aau.config.user=").append(sanitizeSingleLine(aauCfg.get("user"))).append("\n");
			sb.append("aau.config.password=").append(sanitizeSingleLine(aauCfg.get("password"))).append("\n\n");

			sb.append("# AU Settings\n");
			sb.append("au.exposed=").append(auConfigController.isExposeAu()).append("\n");
			sb.append("au.port=").append(auConfigController.getAuPort()).append("\n");
			sb.append("au.running=").append(auConfigController.isLocalAuRunning()).append("\n");
			sb.append("au.configured=").append(auConfigController.isAuConfigReady()).append("\n");
			sb.append("au.config.ca=").append(sanitizeSingleLine(auCfg.get("ca"))).append("\n");
			sb.append("au.config.serverKey=").append(sanitizeSingleLine(auCfg.get("serverKey"))).append("\n");
			sb.append("au.config.serverCertificate=").append(sanitizeSingleLine(auCfg.get("serverCertificate"))).append("\n");
			sb.append("au.config.address=").append(sanitizeSingleLine(auCfg.get("address"))).append("\n");
			sb.append("au.config.port=").append(sanitizeSingleLine(auCfg.get("port"))).append("\n");
			sb.append("au.config.user=").append(sanitizeSingleLine(auCfg.get("user"))).append("\n");
			sb.append("au.config.password=").append(sanitizeSingleLine(auCfg.get("password"))).append("\n\n");

			sb.append("# General\n");
			sb.append("general.runOnPreconfigured=").append(runOnPreconfigured).append("\n");

			Path targetDir = mqttBridgeController.resolveFullConfigDir();
			Files.createDirectories(targetDir);
			Files.writeString(resolveFullConfigFile(), sb.toString(), StandardCharsets.UTF_8);

			Map<String, Object> resp = new LinkedHashMap<>();
			resp.put("ok", true);
			resp.put("path", FULL_CONFIG_REL_PATH);
			resp.put("savedAt", savedAt);
			resp.put("defaultScript", defaultScript);
			resp.put("remoteMqtt", mqttBridgeController.isRemoteMqtt());
			resp.put("remoteAau", aauConfigController.isRemoteAau());
			resp.put("bridgePort", bridgePort);
			resp.put("auExposed", auConfigController.isExposeAu());
			resp.put("auPort", auConfigController.getAuPort());
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of(
					"ok", false,
					"error", "Failed to save full Forge Node configuration: " + e.getMessage()
			));
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		Map<String, String> saved = readFullConfigFile();
		boolean hasFullConfig = !saved.isEmpty();

		// 1. First, set default script as operational
		if (runOnPreconfigured && hasFullConfig) {
			String savedDefaultScript = saved.getOrDefault("default_script", "").trim();
			if (!savedDefaultScript.isEmpty() && mqttBridgeController.getDefaultScriptName().isEmpty()) {
				mqttBridgeController.setDefaultScriptNameIfPresent(savedDefaultScript);
			}
		}
		String operationalScript = mqttBridgeController.applyDefaultScriptAsOperational();
		if (!operationalScript.isEmpty()) {
			LOG.info("[Forge Node Startup] Operational script set from default-script: " + operationalScript);
		}

		// 2. Check FN_RUN_ON_PRECONFIGURED
		if (!runOnPreconfigured) {
			return;
		}
		LOG.info("[Forge Node Startup] FN_RUN_ON_PRECONFIGURED=true — applying preconfigured instance settings.");

		// Apply general appearance & MQTT monitor defaults from full configuration if present
		if (hasFullConfig) {
			mqttBridgeController.applyThemeSnapshot(
					saved.get("theme.colorTheme"),
					saved.get("theme.uiStyle"),
					saved.get("theme.iconPack")
			);

			Map<String, Object> monitorFields = new LinkedHashMap<>();
			if (saved.containsKey("monitor.configured")) {
				monitorFields.put("configured", parseBool(saved.get("monitor.configured"), false));
			}
			if (saved.containsKey("monitor.ip")) monitorFields.put("ip", saved.get("monitor.ip"));
			if (saved.containsKey("monitor.port")) {
				monitorFields.put("port", parseInt(saved.get("monitor.port"), 8883, 1, 65535));
			}
			if (saved.containsKey("monitor.username")) monitorFields.put("username", saved.get("monitor.username"));
			if (saved.containsKey("monitor.password")) monitorFields.put("password", saved.get("monitor.password"));
			if (saved.containsKey("monitor.caCertPath")) monitorFields.put("ca_cert_path", saved.get("monitor.caCertPath"));
			if (saved.containsKey("monitor.brokerCertPath")) monitorFields.put("broker_cert_path", saved.get("monitor.brokerCertPath"));
			if (saved.containsKey("monitor.subscribeAdmin")) {
				monitorFields.put("subscribe_admin", parseBool(saved.get("monitor.subscribeAdmin"), true));
			}
			if (saved.containsKey("mqtt.remoteMqtt")) {
				monitorFields.put("remote_mqtt", parseBool(saved.get("mqtt.remoteMqtt"), true));
			}
			mqttBridgeController.applyMonitorSnapshot(saved.get("monitor.defaultConfig"), monitorFields);
		}

		// 3. First check MQTT server settings; if local configuration, apply & start MQTT server
		boolean useRemoteMqtt = hasFullConfig && saved.containsKey("mqtt.remoteMqtt")
				? parseBool(saved.get("mqtt.remoteMqtt"), mqttBridgeController.isRemoteMqtt())
				: mqttBridgeController.isRemoteMqtt();
		mqttBridgeController.setRemoteMqttMode(useRemoteMqtt);

		if (!useRemoteMqtt) {
			try {
				String localDefault = hasFullConfig
						? saved.getOrDefault("mqtt.localDefaultConfig", "")
						: mqttBridgeController.getDefaultLocalMqttConfigName();
				Map<String, String> localDetails = new LinkedHashMap<>();
				if (hasFullConfig) {
					localDetails.put("address", saved.getOrDefault("mqtt.local.address", "0.0.0.0"));
					localDetails.put("port", saved.getOrDefault("mqtt.local.port", "8883"));
					localDetails.put("users", saved.getOrDefault("mqtt.local.users", ""));
					localDetails.put("ca.crt", saved.getOrDefault("mqtt.local.caCert", ""));
					localDetails.put("broker.crt", saved.getOrDefault("mqtt.local.brokerCert", ""));
					localDetails.put("broker.key", saved.getOrDefault("mqtt.local.brokerKey", ""));
				}
				mqttBridgeController.restoreAndStartLocalMqtt(localDefault, localDetails);
				LOG.info("[Forge Node Startup] Local MQTT server configured and started.");
			} catch (Exception e) {
				LOG.log(Level.WARNING, "[Forge Node Startup] Failed to start preconfigured local MQTT server: " + e.getMessage(), e);
			}
		}

		// 4. Check AAU settings; if local AAU is configured/default, apply config & run AAU
		boolean useRemoteAau = hasFullConfig && saved.containsKey("aau.remoteAau")
				? parseBool(saved.get("aau.remoteAau"), aauConfigController.isRemoteAau())
				: aauConfigController.isRemoteAau();
		String aauDefaultName = hasFullConfig
				? saved.getOrDefault("aau.localDefaultConfig", aauConfigController.getDefaultAauConfigName())
				: aauConfigController.getDefaultAauConfigName();

		if (hasFullConfig) {
			Map<String, Object> aauCfgFields = new LinkedHashMap<>();
			for (String k : new String[] {
					"scriptFile", "backupScriptFile", "logFile",
					"certificate", "serverKey", "serverCertificate",
					"address", "port", "user", "password"
			}) {
				if (saved.containsKey("aau.config." + k)) {
					aauCfgFields.put(k, saved.get("aau.config." + k));
				}
			}
			aauConfigController.restorePreconfiguredAau(useRemoteAau, aauDefaultName, aauCfgFields);
		}

		boolean localAauIsDefault = !useRemoteAau || (!hasFullConfig && !aauDefaultName.isEmpty() && !aauConfigController.isRemoteAau());
		if (localAauIsDefault) {
			try {
				aauConfigController.startLocalAauForStartup();
				LOG.info("[Forge Node Startup] Local AAU started.");
			} catch (Exception e) {
				LOG.log(Level.WARNING, "[Forge Node Startup] Failed to start preconfigured local AAU: " + e.getMessage(), e);
			}
		}

		// 5. Start MQTT bridge
		int bridgePort = hasFullConfig
				? parseInt(saved.get("bridge.port"), mqttBridgeController.getActiveBridgePort(), 1000, 65535)
				: mqttBridgeController.getActiveBridgePort();
		try {
			mqttBridgeController.startBridgeOnPort(bridgePort);
			LOG.info("[Forge Node Startup] MQTT Bridge started on port " + bridgePort);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "[Forge Node Startup] Failed to start MQTT Bridge: " + e.getMessage(), e);
		}

		// 6. After rest of services are started, restore AU settings & start AU on saved port if AU was exposed
		boolean savedExposeAu = hasFullConfig
				&& parseBool(saved.getOrDefault("au.exposed", saved.get("aau.exposeAu")), false);
		boolean useExposeAu = auConfigController.isExposeAu() || savedExposeAu;
		int targetAuPort = hasFullConfig
				? parseInt(saved.getOrDefault("au.port", saved.get("aau.auPort")), auConfigController.getAuPort(), 1000, 65535)
				: auConfigController.getAuPort();
		auConfigController.setAuSettingsInMemory(useExposeAu, targetAuPort);

		if (hasFullConfig) {
			Map<String, Object> auCfgFields = new LinkedHashMap<>();
			for (String k : new String[] { "ca", "serverKey", "serverCertificate", "address", "port", "user", "password" }) {
				if (saved.containsKey("au.config." + k)) {
					auCfgFields.put(k, saved.get("au.config." + k));
				}
			}
			auConfigController.restorePreconfiguredAuConfig(auCfgFields);
		}
		boolean auWasExposed = useExposeAu
				|| (hasFullConfig && parseBool(saved.getOrDefault("au.running", saved.get("aau.auRunning")), false));
		if (auWasExposed) {
			try {
				if (auConfigController.isAuConfigReady()) {
					ResponseEntity<Map<String, Object>> startRes = auConfigController.startAuForStartup(targetAuPort);
					Map<String, Object> startBody = startRes != null ? startRes.getBody() : null;
					if (startRes != null && startRes.getStatusCode().is2xxSuccessful()
							&& startBody != null && Boolean.TRUE.equals(startBody.get("ok"))) {
						LOG.info("[Forge Node Startup] AU TCP server started on port " + targetAuPort);
					} else {
						String err = startBody != null && startBody.get("error") != null
								? String.valueOf(startBody.get("error"))
								: "Unknown error";
						LOG.warning("[Forge Node Startup] Failed to start preconfigured AU server on port " + targetAuPort + ": " + err);
					}
				} else {
					LOG.warning("[Forge Node Startup] Skipping AU startup: AU configuration is not properly set.");
				}
			} catch (Exception e) {
				LOG.log(Level.WARNING, "[Forge Node Startup] Failed to start preconfigured AU server: " + e.getMessage(), e);
			}
		}
	}
}
