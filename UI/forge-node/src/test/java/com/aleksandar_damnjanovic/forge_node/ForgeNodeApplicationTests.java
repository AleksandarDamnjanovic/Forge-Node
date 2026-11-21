package com.aleksandar_damnjanovic.forge_node;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"admin.username=testadmin",
		"admin.password=testpass123",
		"expose-mqtt=true",
		"expose-au=false",
		"run-on-preconfigured=false"
})
@AutoConfigureMockMvc
class ForgeNodeApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unauthenticatedPageRequestsRedirectToLogin() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login.html"));

		mockMvc.perform(get("/script-editor/index.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login.html"));

		mockMvc.perform(get("/mqtt-monitor/index.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login.html"));
	}

	@Test
	void unauthenticatedApiReturns401() throws Exception {
		mockMvc.perform(get("/api/working-fn"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.ok").value(false));
	}

	@Test
	void loginPageAndPublicStylesAreAccessibleWithoutAuth() throws Exception {
		mockMvc.perform(get("/login.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Forge Node Web Interface")))
				.andExpect(content().string(containsString("Administrator Sign In")));

		mockMvc.perform(get("/script-editor/styles.css"))
				.andExpect(status().isOk());
	}

	@Test
	void wrongCredentialsReturn401() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"testadmin\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.ok").value(false));
	}

	@Test
	void validLoginGrantsTokenAndAccessToAllPages() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"testadmin\",\"password\":\"testpass123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(cookie().exists(AuthController.TOKEN_COOKIE_NAME))
				.andReturn();

		Cookie tokenCookie = loginResult.getResponse().getCookie(AuthController.TOKEN_COOKIE_NAME);
		assertNotNull(tokenCookie);

		mockMvc.perform(get("/index.html").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"fullConfigStatusPill\"")))
				.andExpect(content().string(containsString("id=\"fullConfigStatusLabel\"")))
				.andExpect(content().string(containsString("id=\"btnSaveFullConfig\"")))
				.andExpect(content().string(containsString("Save Present Forge Node Instance Configuration")));

		mockMvc.perform(get("/script-editor/index.html").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"btnSetDefaultScript\"")))
				.andExpect(content().string(containsString("id=\"chkSaveScriptDefault\"")));

		mockMvc.perform(get("/mqtt-monitor/index.html").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"pageBridgeBanner\"")))
				.andExpect(content().string(containsString("id=\"btnPageStartBridge\"")))
				.andExpect(content().string(containsString("id=\"modalBridgeAlert\"")))
				.andExpect(content().string(containsString("id=\"btnModalStartBridge\"")));

		mockMvc.perform(get("/api/bridge/status").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true))
				.andExpect(jsonPath("$.running").isBoolean())
				.andExpect(jsonPath("$.port").value(8765));

		mockMvc.perform(get("/api/working-fn").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true));

		// Sensitive files must remain forbidden even when authenticated
		mockMvc.perform(get("/mqtt-monitor/mqtt.conf").cookie(tokenCookie))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/mqtt-monitor/server.py").cookie(tokenCookie))
				.andExpect(status().isForbidden());

		// AAU setup page and status/config APIs require auth and reject absolute paths
		mockMvc.perform(get("/aau-setup.html").cookie(tokenCookie))
				.andExpect(status().isOk());
		mockMvc.perform(get("/au-setup.html").cookie(tokenCookie))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/aau/status").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.remoteAau").isBoolean())
				.andExpect(jsonPath("$.localAauRunning").value(false));

		mockMvc.perform(get("/api/au/status").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exposeAu").value(false))
				.andExpect(jsonPath("$.auRunning").value(false))
				.andExpect(jsonPath("$.auConfigured").isBoolean());

		// AU configuration & start guard tests (independent of AAU)
		java.nio.file.Path auRoot = java.nio.file.Path.of("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && auRoot != null; i++) {
			if (java.nio.file.Files.isDirectory(auRoot.resolve("AAU"))) break;
			auRoot = auRoot.getParent();
		}
		java.nio.file.Path auCfgDir = auRoot.resolve("AAU/script/au configuration");
		java.nio.file.Path auCfgFile = auCfgDir.resolve("au-configuration.conf");
		String origAuCfg = java.nio.file.Files.exists(auCfgFile)
				? java.nio.file.Files.readString(auCfgFile) : null;
		try {
			java.nio.file.Files.deleteIfExists(auCfgFile);
			mockMvc.perform(post("/api/au/start")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"port\":10010}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.ok").value(false))
					.andExpect(jsonPath("$.auConfigured").value(false));

			String absCaAu = auRoot.resolve("SHARED/ca.crt").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			String absKeyAu = auRoot.resolve("SHARED/broker.key").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			String absCertAu = auRoot.resolve("SHARED/broker.crt").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			mockMvc.perform(post("/api/au/config")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"ca\":\"" + absCaAu + "\",\"serverKey\":\"" + absKeyAu + "\",\"serverCertificate\":\"" + absCertAu + "\",\"address\":\"127.0.0.1\",\"port\":8883,\"user\":\"admin\",\"password\":\"00004444\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.configured").value(true));

			mockMvc.perform(get("/api/au/config").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.configured").value(true))
					.andExpect(jsonPath("$.config.address").value("127.0.0.1"));
		} finally {
			if (origAuCfg != null) {
				java.nio.file.Files.createDirectories(auCfgDir);
				java.nio.file.Files.writeString(auCfgFile, origAuCfg);
			} else {
				java.nio.file.Files.deleteIfExists(auCfgFile);
				if (java.nio.file.Files.isDirectory(auCfgDir)) {
					try (var s = java.nio.file.Files.list(auCfgDir)) {
						if (s.findAny().isEmpty()) {
							java.nio.file.Files.deleteIfExists(auCfgDir);
						}
					}
				}
			}
		}
		mockMvc.perform(get("/api/aau/files").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.containerRoot").isNotEmpty())
				.andExpect(jsonPath("$.files").isArray());
		mockMvc.perform(get("/api/aau/config").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.config.scriptFile").value("script/script.fn"));
		mockMvc.perform(post("/api/aau/config")
						.cookie(tokenCookie)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scriptFile\":\"/abs/path.fn\",\"backupScriptFile\":\"script/backup-script.fn\",\"logFile\":\"script/log.txt\",\"certificate\":\"certs/ca.crt\",\"serverKey\":\"certs/broker.key\",\"serverCertificate\":\"certs/broker.crt\",\"address\":\"192.168.0.24\",\"port\":8883,\"user\":\"admin\",\"password\":\"00004444\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.ok").value(false));
		mockMvc.perform(post("/api/aau/config")
						.cookie(tokenCookie)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scriptFile\":\"\",\"backupScriptFile\":\"\",\"logFile\":\"script/log.txt\",\"certificate\":\"certs/ca.crt\",\"serverKey\":\"certs/broker.key\",\"serverCertificate\":\"certs/broker.crt\",\"address\":\"192.168.0.24\",\"port\":8883,\"user\":\"admin\",\"password\":\"00004444\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.ok").value(false));

		// MQTT status and config endpoints
		mockMvc.perform(get("/api/mqtt/status").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true))
				.andExpect(jsonPath("$.remoteMqtt").isBoolean())
				.andExpect(jsonPath("$.exposeMqtt").value(true))
				.andExpect(jsonPath("$.localConfigExists").isBoolean())
				.andExpect(jsonPath("$.config.ip").isNotEmpty());

		// Toggle remoteMqtt in MQTT config without touching application.properties
		java.nio.file.Path rootForProps = java.nio.file.Path.of("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && rootForProps != null; i++) {
			if (java.nio.file.Files.isDirectory(rootForProps.resolve("AAU"))) break;
			rootForProps = rootForProps.getParent();
		}
		java.nio.file.Path mqttConfForSettings = rootForProps.resolve("UI/forge-node/src/main/resources/static/mqtt-monitor/mqtt.conf");
		String origMqttConfForSettings = java.nio.file.Files.exists(mqttConfForSettings)
				? java.nio.file.Files.readString(mqttConfForSettings) : null;
		java.nio.file.Path appPropsPath = rootForProps.resolve("UI/forge-node/src/main/resources/application.properties");
		String origAppProps = java.nio.file.Files.exists(appPropsPath)
				? java.nio.file.Files.readString(appPropsPath) : "";
		try {
			mockMvc.perform(post("/api/mqtt/settings")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"remoteMqtt\":false}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.remoteMqtt").value(false));

			mockMvc.perform(post("/api/mqtt/settings")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"remoteMqtt\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.remoteMqtt").value(true));

			String afterAppProps = java.nio.file.Files.exists(appPropsPath)
					? java.nio.file.Files.readString(appPropsPath) : "";
			org.junit.jupiter.api.Assertions.assertEquals(origAppProps, afterAppProps);
		} finally {
			if (origMqttConfForSettings != null) {
				java.nio.file.Files.writeString(mqttConfForSettings, origMqttConfForSettings);
			}
		}

		// Toggle remoteAau in AAU config without writing remote-aau to application.properties
		java.nio.file.Path aauModeFile = rootForProps.resolve("AAU/script/localAAUconfigurations/aau-mode");
		String origAauMode = java.nio.file.Files.exists(aauModeFile)
				? java.nio.file.Files.readString(aauModeFile) : null;
		try {
			mockMvc.perform(post("/api/aau/settings")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"remoteAau\":false}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.remoteAau").value(false));

			mockMvc.perform(post("/api/aau/settings")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"remoteAau\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.remoteAau").value(true));

			String afterAppProps = java.nio.file.Files.exists(appPropsPath)
					? java.nio.file.Files.readString(appPropsPath) : "";
			org.junit.jupiter.api.Assertions.assertFalse(afterAppProps.contains("remote-aau="));
		} finally {
			if (origAauMode != null) {
				java.nio.file.Files.writeString(aauModeFile, origAauMode);
			} else {
				java.nio.file.Files.deleteIfExists(aauModeFile);
			}
			java.nio.file.Path localAauDirCleanup = rootForProps.resolve("AAU/script/localAAUconfigurations");
			if (java.nio.file.Files.isDirectory(localAauDirCleanup)) {
				try (var s = java.nio.file.Files.list(localAauDirCleanup)) {
					if (s.findAny().isEmpty()) {
						java.nio.file.Files.deleteIfExists(localAauDirCleanup);
					}
				}
			}
		}

		// Save and inspect full Forge Node instance configuration in AAU/script/full-configuration/
		java.nio.file.Path fullCfgDir = rootForProps.resolve("AAU/script/full-configuration");
		java.nio.file.Path fullCfgFile = fullCfgDir.resolve("full-configuration.conf");
		String origFullCfg = java.nio.file.Files.exists(fullCfgFile)
				? java.nio.file.Files.readString(fullCfgFile) : null;
		try {
			mockMvc.perform(post("/api/instance/full-config/save")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"bridgePort\":8765,\"auPort\":10015,\"exposeAu\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.path").value("AAU/script/full-configuration/full-configuration.conf"))
					.andExpect(jsonPath("$.bridgePort").value(8765))
					.andExpect(jsonPath("$.auExposed").value(true))
					.andExpect(jsonPath("$.auPort").value(10015));

			mockMvc.perform(get("/api/instance/full-config").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.exists").value(true))
					.andExpect(jsonPath("$.runOnPreconfigured").value(false))
					.andExpect(jsonPath("$.bridgePort").value(8765))
					.andExpect(jsonPath("$.auExposed").value(true))
					.andExpect(jsonPath("$.auPort").value(10015));

			String savedContent = java.nio.file.Files.readString(fullCfgFile);
			org.junit.jupiter.api.Assertions.assertTrue(savedContent.contains("mqtt.remoteMqtt="));
			org.junit.jupiter.api.Assertions.assertTrue(savedContent.contains("mqtt.exposeMqtt=true"));
			org.junit.jupiter.api.Assertions.assertTrue(savedContent.contains("aau.remoteAau="));
			org.junit.jupiter.api.Assertions.assertTrue(savedContent.contains("bridge.port=8765"));
			org.junit.jupiter.api.Assertions.assertTrue(savedContent.contains("# AU Settings"));
			org.junit.jupiter.api.Assertions.assertTrue(savedContent.contains("au.exposed=true"));
			org.junit.jupiter.api.Assertions.assertTrue(savedContent.contains("au.port=10015"));
		} finally {
			if (origFullCfg != null) {
				java.nio.file.Files.writeString(fullCfgFile, origFullCfg);
			} else {
				java.nio.file.Files.deleteIfExists(fullCfgFile);
			}
		}

		// Theme / UI Style / Icon Pack persistence endpoint (/api/theme)
		java.nio.file.Path testRoot = java.nio.file.Path.of("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && testRoot != null; i++) {
			if (java.nio.file.Files.isDirectory(testRoot.resolve("AAU"))) break;
			testRoot = testRoot.getParent();
		}
		java.nio.file.Path themesDir = testRoot != null ? testRoot.resolve("AAU/script/themes") : null;
		java.nio.file.Path themeConf = themesDir != null ? themesDir.resolve("theme-config.conf") : null;
		String origThemeConf = (themeConf != null && java.nio.file.Files.exists(themeConf)) ? java.nio.file.Files.readString(themeConf) : null;
		try {
			mockMvc.perform(post("/api/theme")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"colorTheme\":\"rose-pine\",\"uiStyle\":\"android\",\"iconPack\":\"android\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.colorTheme").value("rose-pine"))
					.andExpect(jsonPath("$.uiStyle").value("android"))
					.andExpect(jsonPath("$.iconPack").value("android"))
					.andExpect(jsonPath("$.path").value("AAU/script/themes/theme-config.conf"));

			mockMvc.perform(get("/api/theme").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.colorTheme").value("rose-pine"))
					.andExpect(jsonPath("$.uiStyle").value("android"))
					.andExpect(jsonPath("$.iconPack").value("android"))
					.andExpect(jsonPath("$.path").value("AAU/script/themes/theme-config.conf"));
		} finally {
			if (themeConf != null) {
				if (origThemeConf != null) java.nio.file.Files.writeString(themeConf, origThemeConf);
				else java.nio.file.Files.deleteIfExists(themeConf);
			}
			if (themesDir != null && java.nio.file.Files.isDirectory(themesDir)) {
				try (var s = java.nio.file.Files.list(themesDir)) {
					if (s.findAny().isEmpty()) {
						java.nio.file.Files.deleteIfExists(themesDir);
					}
				}
			}
		}
		mockMvc.perform(get("/mqtt-local-setup.html").cookie(tokenCookie))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/certs/status").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true))
				.andExpect(jsonPath("$.present").isBoolean());
		mockMvc.perform(get("/api/certs/details").cookie(tokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ok").value(true))
				.andExpect(jsonPath("$.certificates").isArray())
				.andExpect(jsonPath("$.certificates[0].sha1Fingerprint").isNotEmpty())
				.andExpect(jsonPath("$.certificates[0].sha256Fingerprint").isNotEmpty())
				.andExpect(jsonPath("$.certificates[0].subject").isNotEmpty());

		// Script save, list, download, and default-script.conf endpoints for AAU/script/
		String tempScriptName = "test_save_check.fn";
		java.nio.file.Path defaultScriptConf = testRoot != null ? testRoot.resolve("AAU/script/default-script.conf") : null;
		String origDefaultScriptConf = (defaultScriptConf != null && java.nio.file.Files.exists(defaultScriptConf))
				? java.nio.file.Files.readString(defaultScriptConf)
				: null;
		String validScript = "1(\"Test Node\", \"1f\", [MASTER]){\n\n"
				+ "    <variables>\n"
				+ "        global int $c1 = 0\n"
				+ "    </variables>\n\n"
				+ "    <sensors>\n"
				+ "    </sensors>\n\n"
				+ "    <elements>\n"
				+ "    </elements>\n\n"
				+ "    <programs>\n"
				+ "    </programs>\n\n"
				+ "}";
		String validScriptJson = validScript
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n");
		try {
			// Invalid filename (.txt) must be rejected
			mockMvc.perform(post("/api/files/scripts/save")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"fileName\":\"invalid_name.txt\",\"content\":\"" + validScriptJson + "\"}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.ok").value(false));

			// Invalid .fn script content must be rejected
			mockMvc.perform(post("/api/files/scripts/save")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"fileName\":\"" + tempScriptName + "\",\"content\":\"// test script\\n\"}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.ok").value(false));

			// Valid .fn script succeeds
			mockMvc.perform(post("/api/files/scripts/save")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"fileName\":\"" + tempScriptName + "\",\"content\":\"" + validScriptJson + "\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.fileName").value(tempScriptName));

			mockMvc.perform(get("/api/files/scripts/list").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(content().string(containsString(tempScriptName)));

			mockMvc.perform(get("/api/files/scripts/download")
							.param("name", tempScriptName)
							.cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(content().string(validScript));

			// Set default script -> stored in AAU/script/default-script.conf and returned by /api/working-fn
			mockMvc.perform(post("/api/working-fn/default")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"fileName\":\"" + tempScriptName + "\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.defaultScript").value(tempScriptName))
					.andExpect(jsonPath("$.path").value("AAU/script/default-script.conf"));

			mockMvc.perform(get("/api/working-fn/default").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.defaultScript").value(tempScriptName));

			mockMvc.perform(get("/api/working-fn").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.defaultScript").value(tempScriptName))
					.andExpect(jsonPath("$.path").value("AAU/script/" + tempScriptName));
		} finally {
			java.nio.file.Path root = java.nio.file.Path.of("").toAbsolutePath().normalize();
			for (int i = 0; i < 6 && root != null; i++) {
				if (java.nio.file.Files.isDirectory(root.resolve("AAU"))) {
					java.nio.file.Files.deleteIfExists(root.resolve("AAU/script").resolve(tempScriptName));
					break;
				}
				root = root.getParent();
			}
			if (defaultScriptConf != null) {
				if (origDefaultScriptConf != null) {
					java.nio.file.Files.writeString(defaultScriptConf, origDefaultScriptConf);
				} else {
					java.nio.file.Files.deleteIfExists(defaultScriptConf);
				}
			}
		}

		// Sync AAU configuration from local MQTT server configuration
		java.nio.file.Path root = java.nio.file.Path.of("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && root != null; i++) {
			if (java.nio.file.Files.isDirectory(root.resolve("AAU"))) {
				break;
			}
			root = root.getParent();
		}
		java.nio.file.Path aauConf = root.resolve("AAU/configuration.conf");
		String originalAauConf = java.nio.file.Files.exists(aauConf)
				? java.nio.file.Files.readString(aauConf)
				: null;
		try {
			mockMvc.perform(post("/api/aau/sync-local-mqtt").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.config.logFile").value("script/log.txt"))
					.andExpect(jsonPath("$.config.backupScriptFile").value(""))
					.andExpect(jsonPath("$.config.address").value("127.0.0.1"))
					.andExpect(jsonPath("$.config.port").value(8883))
					.andExpect(jsonPath("$.config.user").value("admin"))
					.andExpect(jsonPath("$.config.certificate").value("../SHARED/ca.crt"))
					.andExpect(jsonPath("$.config.serverKey").value("../SHARED/broker.key"))
					.andExpect(jsonPath("$.config.serverCertificate").value("../SHARED/broker.crt"));

			// Absolute paths inside container/project root are accepted in POST /api/aau/config
			String absScript = root.resolve("AAU/script/script.fn").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			String absLog = root.resolve("AAU/script/log.txt").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			String absCa = root.resolve("SHARED/ca.crt").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			String absKey = root.resolve("SHARED/broker.key").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			String absBroker = root.resolve("SHARED/broker.crt").toAbsolutePath().normalize().toString().replace("\\", "\\\\");
			mockMvc.perform(post("/api/aau/config")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"scriptFile\":\"" + absScript + "\",\"backupScriptFile\":\"\",\"logFile\":\"" + absLog + "\",\"certificate\":\"" + absCa + "\",\"serverKey\":\"" + absKey + "\",\"serverCertificate\":\"" + absBroker + "\",\"address\":\"127.0.0.1\",\"port\":8883,\"user\":\"admin\",\"password\":\"00004444\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true));

			// Save, list, load, and set default in AAU/script/localAAUconfigurations/
			java.nio.file.Path localAauDir = root.resolve("AAU/script/localAAUconfigurations");
			try {
				mockMvc.perform(post("/api/aau/configs/save")
								.cookie(tokenCookie)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"fileName\":\"test-aau.conf\",\"setAsDefault\":true,\"scriptFile\":\"" + absScript + "\",\"backupScriptFile\":\"\",\"logFile\":\"" + absLog + "\",\"certificate\":\"" + absCa + "\",\"serverKey\":\"" + absKey + "\",\"serverCertificate\":\"" + absBroker + "\",\"address\":\"127.0.0.1\",\"port\":8883,\"user\":\"admin\",\"password\":\"00004444\"}"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.ok").value(true))
						.andExpect(jsonPath("$.fileName").value("test-aau.conf"))
						.andExpect(jsonPath("$.isDefault").value(true));

				mockMvc.perform(get("/api/aau/configs/list").cookie(tokenCookie))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.defaultConfig").value("test-aau.conf"))
						.andExpect(content().string(containsString("test-aau.conf")));

				mockMvc.perform(post("/api/aau/configs/load")
								.cookie(tokenCookie)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"fileName\":\"test-aau.conf\",\"setAsDefault\":false}"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.ok").value(true))
						.andExpect(jsonPath("$.config.address").value("127.0.0.1"));
			} finally {
				java.nio.file.Files.deleteIfExists(localAauDir.resolve("test-aau.conf"));
				java.nio.file.Files.deleteIfExists(localAauDir.resolve("default-configuration"));
				if (java.nio.file.Files.isDirectory(localAauDir)) {
					try (var s = java.nio.file.Files.list(localAauDir)) {
						if (s.findAny().isEmpty()) {
							java.nio.file.Files.deleteIfExists(localAauDir);
						}
					}
				}
			}
		} finally {
			if (originalAauConf != null) {
				java.nio.file.Files.writeString(aauConf, originalAauConf);
			}
		}

		// Save (download) and load (upload) for both Remote and Local MQTT configurations
		java.nio.file.Path remoteMqttConf = root.resolve("UI/forge-node/src/main/resources/static/mqtt-monitor/mqtt.conf");
		java.nio.file.Path localMqttConf = root.resolve("heplers/mqtt-configuration.conf");
		java.nio.file.Path localUsersSh = root.resolve("heplers/setupMosquitto_users.sh");
		String origRemote = java.nio.file.Files.exists(remoteMqttConf) ? java.nio.file.Files.readString(remoteMqttConf) : null;
		String origLocal = java.nio.file.Files.exists(localMqttConf) ? java.nio.file.Files.readString(localMqttConf) : null;
		String origUsers = java.nio.file.Files.exists(localUsersSh) ? java.nio.file.Files.readString(localUsersSh) : null;
		try {
			MvcResult remoteDl = mockMvc.perform(get("/api/mqtt/remote-config/download").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("config_type = remote")))
					.andReturn();

			MvcResult localDl = mockMvc.perform(get("/api/mqtt/local-config/download").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("config_type=local")))
					.andReturn();

			// Uploading local config to remote endpoint must be rejected, and vice versa
			MockMultipartFile localAsRemote = new MockMultipartFile(
					"file", "local.conf", "text/plain", localDl.getResponse().getContentAsByteArray());
			mockMvc.perform(multipart("/api/mqtt/remote-config/upload").file(localAsRemote).cookie(tokenCookie))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.ok").value(false));

			MockMultipartFile remoteAsLocal = new MockMultipartFile(
					"file", "remote.conf", "text/plain", remoteDl.getResponse().getContentAsByteArray());
			mockMvc.perform(multipart("/api/mqtt/local-config/upload").file(remoteAsLocal).cookie(tokenCookie))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.ok").value(false));

			// Valid remote and local uploads succeed
			MockMultipartFile validRemote = new MockMultipartFile(
					"file", "forge-node-remote-mqtt.conf", "text/plain", remoteDl.getResponse().getContentAsByteArray());
			mockMvc.perform(multipart("/api/mqtt/remote-config/upload").file(validRemote).cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true));

			MockMultipartFile validLocal = new MockMultipartFile(
					"file", "forge-node-local-mqtt.conf", "text/plain", localDl.getResponse().getContentAsByteArray());
			mockMvc.perform(multipart("/api/mqtt/local-config/upload").file(validLocal).cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.localConfigExists").value(true));

			// Save and load named MQTT configs in AAU/script/
			mockMvc.perform(post("/api/mqtt/configs/save")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"type\":\"remote\",\"fileName\":\"test-remote.conf\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.fileName").value("test-remote.conf"));

			mockMvc.perform(post("/api/mqtt/configs/save")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"type\":\"local\",\"fileName\":\"test-local.conf\",\"setAsDefault\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.fileName").value("test-local.conf"))
					.andExpect(jsonPath("$.isDefault").value(true))
					.andExpect(jsonPath("$.defaultConfig").value("test-local.conf"));

			mockMvc.perform(get("/api/mqtt/configs/list").param("type", "local").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.defaultConfig").value("test-local.conf"))
					.andExpect(content().string(containsString("test-local.conf")));

			mockMvc.perform(post("/api/mqtt/configs/load")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"type\":\"remote\",\"fileName\":\"test-remote.conf\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true));

			mockMvc.perform(post("/api/mqtt/configs/load")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"type\":\"local\",\"fileName\":\"test-local.conf\",\"setAsDefault\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.isDefault").value(true))
					.andExpect(jsonPath("$.defaultConfig").value("test-local.conf"));

			// Save/list/load/default-configuration for MQTT Monitor Connection Setup
			mockMvc.perform(post("/api/monitor/configs/save")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"fileName\":\"test-monitor.conf\",\"setAsDefault\":true,\"ip\":\"127.0.0.1\",\"port\":8883,\"username\":\"admin\",\"password\":\"pass\",\"subscribeAdmin\":true}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.fileName").value("test-monitor.conf"))
					.andExpect(jsonPath("$.isDefault").value(true));

			mockMvc.perform(get("/api/monitor/configs/list").cookie(tokenCookie))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.defaultConfig").value("test-monitor.conf"))
					.andExpect(content().string(containsString("test-monitor.conf")));

			mockMvc.perform(post("/api/monitor/configs/load")
							.cookie(tokenCookie)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"fileName\":\"test-monitor.conf\",\"setAsDefault\":false}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ok").value(true))
					.andExpect(jsonPath("$.config.ip").value("127.0.0.1"));
		} finally {
			if (origRemote != null) java.nio.file.Files.writeString(remoteMqttConf, origRemote);
			if (origLocal != null) java.nio.file.Files.writeString(localMqttConf, origLocal);
			if (origUsers != null) java.nio.file.Files.writeString(localUsersSh, origUsers);
			java.nio.file.Files.deleteIfExists(root.resolve("helpers/mqtt-configuration.conf"));
			java.nio.file.Files.deleteIfExists(root.resolve("helpers/setupMosquitto_users.sh"));
			java.nio.file.Files.deleteIfExists(root.resolve("AAU/script/test-remote.conf"));
			java.nio.file.Path localMqttDir = root.resolve("AAU/script/local-mqtt-server");
			java.nio.file.Files.deleteIfExists(localMqttDir.resolve("test-local.conf"));
			java.nio.file.Files.deleteIfExists(localMqttDir.resolve("default-configuration"));
			if (java.nio.file.Files.isDirectory(localMqttDir)) {
				try (var s = java.nio.file.Files.list(localMqttDir)) {
					if (s.findAny().isEmpty()) {
						java.nio.file.Files.deleteIfExists(localMqttDir);
					}
				}
			}
			java.nio.file.Path monDir = root.resolve("AAU/script/mqtt-monitor-configurations");
			java.nio.file.Files.deleteIfExists(monDir.resolve("test-monitor.conf"));
			java.nio.file.Files.deleteIfExists(monDir.resolve("default-configuration"));
			if (java.nio.file.Files.isDirectory(monDir)) {
				try (var s = java.nio.file.Files.list(monDir)) {
					if (s.findAny().isEmpty()) {
						java.nio.file.Files.deleteIfExists(monDir);
					}
				}
			}
		}
	}

	@Test
	void startupStartsAuAfterOtherServicesWhenPreconfiguredExposeAuAndConfigReady() throws Exception {
		java.nio.file.Path root = java.nio.file.Path.of("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && root != null; i++) {
			if (java.nio.file.Files.isDirectory(root.resolve("AAU"))) break;
			root = root.getParent();
		}
		assertNotNull(root);
		java.nio.file.Path fullCfgFile = root.resolve("AAU/script/full-configuration/full-configuration.conf");
		String origFullCfg = java.nio.file.Files.exists(fullCfgFile)
				? java.nio.file.Files.readString(fullCfgFile) : null;
		java.nio.file.Path auCfgFile = root.resolve("AAU/script/au configuration/au-configuration.conf");
		String origAuCfg = java.nio.file.Files.exists(auCfgFile)
				? java.nio.file.Files.readString(auCfgFile) : null;

		try {
			// Full config has au.exposed=false, but expose-au=true env/property must still enable AU startup
			java.nio.file.Files.createDirectories(fullCfgFile.getParent());
			java.nio.file.Files.writeString(fullCfgFile,
					"mqtt.remoteMqtt=true\n"
							+ "aau.remoteAau=true\n"
							+ "bridge.port=8765\n"
							+ "au.exposed=false\n"
							+ "au.port=10018\n");

			MqttBridgeController mockBridge = org.mockito.Mockito.mock(MqttBridgeController.class);
			org.mockito.Mockito.when(mockBridge.resolveFullConfigDir()).thenReturn(fullCfgFile.getParent());
			org.mockito.Mockito.when(mockBridge.getDefaultScriptName()).thenReturn("");
			org.mockito.Mockito.when(mockBridge.applyDefaultScriptAsOperational()).thenReturn("");
			org.mockito.Mockito.when(mockBridge.isRemoteMqtt()).thenReturn(true);
			org.mockito.Mockito.when(mockBridge.getActiveBridgePort()).thenReturn(8765);

			AauConfigController mockAau = org.mockito.Mockito.mock(AauConfigController.class);
			org.mockito.Mockito.when(mockAau.isRemoteAau()).thenReturn(true);
			org.mockito.Mockito.when(mockAau.getDefaultAauConfigName()).thenReturn("");

			AuConfigController mockAu = org.mockito.Mockito.mock(AuConfigController.class);
			org.mockito.Mockito.when(mockAu.isExposeAu()).thenReturn(true);
			org.mockito.Mockito.when(mockAu.getAuPort()).thenReturn(10010);
			org.mockito.Mockito.when(mockAu.isAuConfigReady()).thenReturn(true);
			org.mockito.Mockito.when(mockAu.startAuForStartup(10018))
					.thenReturn(org.springframework.http.ResponseEntity.ok(java.util.Map.of("ok", true, "auRunning", true, "auPort", 10018)));

			InstanceConfigController controller = new InstanceConfigController(mockBridge, mockAau, mockAu, "true");
			controller.onApplicationReady();

			org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(mockBridge, mockAu);
			inOrder.verify(mockBridge).startBridgeOnPort(8765);
			inOrder.verify(mockAu).setAuSettingsInMemory(true, 10018);
			inOrder.verify(mockAu).startAuForStartup(10018);

			// When AU configuration is not saved/ready, AU must NOT start
			AuConfigController mockAuNotReady = org.mockito.Mockito.mock(AuConfigController.class);
			org.mockito.Mockito.when(mockAuNotReady.isExposeAu()).thenReturn(true);
			org.mockito.Mockito.when(mockAuNotReady.getAuPort()).thenReturn(10010);
			org.mockito.Mockito.when(mockAuNotReady.isAuConfigReady()).thenReturn(false);

			InstanceConfigController controllerNotReady = new InstanceConfigController(mockBridge, mockAau, mockAuNotReady, "true");
			controllerNotReady.onApplicationReady();
			org.mockito.Mockito.verify(mockAuNotReady, org.mockito.Mockito.never()).startAuForStartup(org.mockito.ArgumentMatchers.anyInt());

			// When run-on-preconfigured=false, AU must NOT start on startup
			AuConfigController mockAuDisabled = org.mockito.Mockito.mock(AuConfigController.class);
			InstanceConfigController controllerDisabled = new InstanceConfigController(mockBridge, mockAau, mockAuDisabled, "false");
			controllerDisabled.onApplicationReady();
			org.mockito.Mockito.verifyNoInteractions(mockAuDisabled);
		} finally {
			if (origFullCfg != null) {
				java.nio.file.Files.writeString(fullCfgFile, origFullCfg);
			} else {
				java.nio.file.Files.deleteIfExists(fullCfgFile);
			}
			if (origAuCfg != null) {
				java.nio.file.Files.writeString(auCfgFile, origAuCfg);
			}
		}
	}
}
