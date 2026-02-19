package org.runetale.skills.gathering.config;

import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.skills.config.SkillsPathLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GatheringExternalConfigBootstrap {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String NODE_INDEX_RESOURCE = "Skills/Nodes/index.list";

	private GatheringExternalConfigBootstrap() {
	}

	public static void seedMissingDefaults(@Nonnull SkillsPathLayout pathLayout) {
		Path configRoot = pathLayout.pluginConfigRoot();
		LOGGER.atInfo().log("[Skills Gathering] Seeding external config defaults configRoot=%s", configRoot);

		try {
			Files.createDirectories(configRoot);
		} catch (IOException e) {
			LOGGER.atWarning().withCause(e).log("[Skills Gathering] Failed to initialize config directory=%s", configRoot);
			return;
		}

		int copied = 0;
		for (String resourcePath : defaultResourcePaths()) {
			if (copyClasspathResourceIfMissing(pathLayout, resourcePath)) {
				copied++;
			}
		}

		LOGGER.atInfo().log("[Skills Gathering] External config bootstrap complete copied=%d root=%s", copied, configRoot);
	}

	@Nonnull
	private static List<String> defaultResourcePaths() {
		List<String> resources = new ArrayList<>();
		resources.add("Skills/Config/tooling.properties");
		resources.add("Skills/Config/heuristics.properties");
		resources.add("Skills/tool-tier-defaults.properties");
		resources.add("Skills/xp-profile-defaults.properties");
		resources.add(NODE_INDEX_RESOURCE);

		for (String nodePath : readNodeIndexFromClasspath()) {
			resources.add("Skills/Nodes/" + nodePath);
		}
		LOGGER.atFine().log("[Skills Gathering] Bootstrap resource manifest contains %d resource(s)", resources.size());
		return resources;
	}

	@Nonnull
	private static List<String> readNodeIndexFromClasspath() {
		List<String> files = new ArrayList<>();
		try (InputStream input = openClasspathResource(NODE_INDEX_RESOURCE)) {
			if (input == null) {
				LOGGER.atWarning().log("[Skills Gathering] Missing bundled node index resource=%s during bootstrap", NODE_INDEX_RESOURCE);
				return files;
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String trimmed = line.trim();
					if (trimmed.isEmpty() || trimmed.startsWith("#")) {
						continue;
					}
					files.add(trimmed);
				}
			}
		} catch (IOException e) {
			LOGGER.atWarning().withCause(e).log("[Skills Gathering] Failed reading bundled node index during bootstrap");
		}

		LOGGER.atFine().log("[Skills Gathering] Bootstrap node index resolved %d node resource(s)", files.size());
		return files;
	}

	private static boolean copyClasspathResourceIfMissing(
			@Nonnull SkillsPathLayout pathLayout,
			@Nonnull String resourcePath) {
		Path outputPath = pathLayout.resolveConfigResourcePath(resourcePath);
		if (Files.exists(outputPath)) {
			LOGGER.atFine().log("[Skills Gathering] Bootstrap skipped existing config file: %s", outputPath);
			return false;
		}

		try {
			Path parent = outputPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
		} catch (IOException e) {
			LOGGER.atWarning().withCause(e).log("[Skills Gathering] Failed to create config directory for %s", outputPath);
			return false;
		}

		try (InputStream input = openClasspathResource(resourcePath)) {
			if (input == null) {
				LOGGER.atWarning().log("[Skills Gathering] Missing bundled bootstrap resource=%s", resourcePath);
				return false;
			}

			Files.copy(input, outputPath);
			LOGGER.atInfo().log("[Skills Gathering] Seeded default config file: %s", outputPath);
			return true;
		} catch (IOException e) {
			LOGGER.atWarning().withCause(e).log("[Skills Gathering] Failed writing bootstrap resource=%s path=%s", resourcePath,
					outputPath);
			return false;
		}
	}

	@Nullable
	private static InputStream openClasspathResource(@Nonnull String resourcePath) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader selectedClassLoader = GatheringExternalConfigBootstrap.class.getClassLoader();
		if (selectedClassLoader == null) {
			selectedClassLoader = contextClassLoader;
		}
		if (selectedClassLoader == null) {
			return null;
		}

		InputStream selectedInput = selectedClassLoader.getResourceAsStream(resourcePath);
		if (selectedInput == null && contextClassLoader != null && contextClassLoader != selectedClassLoader) {
			selectedInput = contextClassLoader.getResourceAsStream(resourcePath);
		}
		return selectedInput;
	}
}
