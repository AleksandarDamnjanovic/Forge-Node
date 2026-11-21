package com.aleksandar_damnjanovic.forge_node;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private Path resolveProjectRoot() {
		Path cwd = Paths.get("").toAbsolutePath().normalize();
		for (int i = 0; i < 6 && cwd != null; i++) {
			if (Files.isDirectory(cwd.resolve("SHARED")) || Files.isDirectory(cwd.resolve("AAU"))) {
				return cwd;
			}
			cwd = cwd.getParent();
		}
		return Paths.get("/home/irishpub/Programming/Forge Node");
	}

	private Path resolveStaticRoot() {
		return resolveProjectRoot().resolve("UI/forge-node/src/main/resources/static");
	}

	@PostConstruct
	public void syncAndCleanupUiDirectory() {
		try {
			Path projectRoot = resolveProjectRoot();
			Path uiDir = projectRoot.resolve("UI");
			Path staticRoot = resolveStaticRoot();
			Path targetStaticRoot = projectRoot.resolve("UI/forge-node/target/classes/static");

			// 1. Ensure Forge Node Studio Icon.png is copied into forge-node static directories
			Path iconSrc = staticRoot.resolve("Forge Node Studio Icon.png");
			if (!Files.exists(iconSrc)) {
				Path legacyIcon = uiDir.resolve("Forge Node Studio Icon.png");
				if (!Files.exists(legacyIcon)) {
					legacyIcon = uiDir.resolve("script-editor/Forge Node Studio Icon.png");
				}
				if (Files.exists(legacyIcon)) {
					iconSrc = legacyIcon;
				}
			}

			if (Files.exists(iconSrc)) {
				for (Path root : new Path[] { staticRoot, targetStaticRoot }) {
					if (Files.exists(root.getParent())) {
						Files.createDirectories(root);
						Files.createDirectories(root.resolve("script-editor"));
						Files.createDirectories(root.resolve("mqtt-monitor"));
						Files.copy(iconSrc, root.resolve("Forge Node Studio Icon.png"), StandardCopyOption.REPLACE_EXISTING);
						Files.copy(iconSrc, root.resolve("script-editor/Forge Node Studio Icon.png"), StandardCopyOption.REPLACE_EXISTING);
						Files.copy(iconSrc, root.resolve("mqtt-monitor/Forge Node Studio Icon.png"), StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}

			// 2. Remove temporary certs directory inside static/mqtt-monitor/certs (certificates live in SHARED/)
			deleteDirectoryRecursively(staticRoot.resolve("mqtt-monitor/certs"));
			deleteDirectoryRecursively(targetStaticRoot.resolve("mqtt-monitor/certs"));

			// 3. Clean up redundant legacy files/folders in UI/ outside forge-node once icon is inside forge-node
			if (Files.exists(staticRoot.resolve("Forge Node Studio Icon.png"))) {
				Files.deleteIfExists(uiDir.resolve("index.html"));
				Files.deleteIfExists(uiDir.resolve("Forge Node Studio Icon.png"));
				deleteDirectoryRecursively(uiDir.resolve("script-editor"));
				deleteDirectoryRecursively(uiDir.resolve("mqtt-monitor"));
			}
		} catch (Exception ignored) {
		}
	}

	private void deleteDirectoryRecursively(Path dir) {
		if (dir == null || !Files.exists(dir)) {
			return;
		}
		try {
			Files.walkFileTree(dir, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.deleteIfExists(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
					Files.deleteIfExists(d);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (Exception ignored) {
		}
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("forward:/index.html");
		registry.addViewController("/login").setViewName("forward:/login.html");
		registry.addViewController("/aau-setup").setViewName("forward:/aau-setup.html");
		registry.addViewController("/script-editor").setViewName("forward:/script-editor/index.html");
		registry.addViewController("/script-editor/").setViewName("forward:/script-editor/index.html");
		registry.addViewController("/mqtt-monitor").setViewName("forward:/mqtt-monitor/index.html");
		registry.addViewController("/mqtt-monitor/").setViewName("forward:/mqtt-monitor/index.html");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path staticRoot = resolveStaticRoot();
		String staticRootUri = staticRoot.toUri().toString();
		if (!staticRootUri.endsWith("/")) {
			staticRootUri = staticRootUri + "/";
		}
		registry.addResourceHandler("/**")
				.addResourceLocations(staticRootUri, "classpath:/static/")
				.setCacheControl(CacheControl.noStore());
	}
}
