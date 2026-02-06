package org.runetale.skills.service;

import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves skill-node definitions from block identifiers.
 *
 * <p>
 * Current implementation uses an in-memory registry with bootstrap defaults.
 * This is intentionally a lookup hook so external asset loading can be plugged
 * in later
 * without changing runtime systems.
 */
public class SkillNodeLookupService {

	private static final Logger LOGGER = Logger.getLogger(SkillNodeLookupService.class.getName());
	private static final String NODE_INDEX_RESOURCE = "Skills/Nodes/index.list";
	private static final String NODE_RESOURCE_PREFIX = "Skills/Nodes/";
	private static final String XP_PROFILE_DEFAULTS_RESOURCE = "Skills/xp-profile-defaults.properties";
	private static final String TOOL_TIER_DEFAULTS_RESOURCE = "Skills/tool-tier-defaults.properties";

	/**
	 * Keyed by block id for fast break-event matching.
	 */
	private final Map<String, SkillNodeDefinition> byBlockId = new ConcurrentHashMap<>();

	/**
	 * Registers baseline defaults so runtime is operational even without external
	 * data.
	 */
	public void initializeDefaults() {
		LOGGER.log(Level.INFO, "[Skills] Initializing skill-node definitions from resources");
		loadAndLogOptionalSharedDefaults();

		int loadedFromResources = loadNodesFromResources();
		if (loadedFromResources <= 0) {
			LOGGER.log(Level.WARNING,
					"[Skills] No node definitions loaded from resources. Falling back to in-memory safety defaults.");
			registerFallbackDefaults();
			return;
		}

		LOGGER.log(Level.INFO,
				String.format("[Skills] Node resource bootstrap completed with %d definition(s)", loadedFromResources));
	}

	/**
	 * Loads optional shared defaults and logs key metadata to verify packaging and
	 * discoverability.
	 */
	private void loadAndLogOptionalSharedDefaults() {
		Properties xpProfileDefaults = loadPropertiesResource(XP_PROFILE_DEFAULTS_RESOURCE);
		if (!xpProfileDefaults.isEmpty()) {
			LOGGER.log(Level.INFO,
					String.format("[Skills] XP defaults discovered: profileId=%s curveModel=%s maxLevel=%s",
							value(xpProfileDefaults, "profileId", "<missing>"),
							value(xpProfileDefaults, "curveModel", "<missing>"),
							value(xpProfileDefaults, "maxLevel", "<missing>")));
		}

		Properties toolTierDefaults = loadPropertiesResource(TOOL_TIER_DEFAULTS_RESOURCE);
		if (!toolTierDefaults.isEmpty()) {
			LOGGER.log(Level.INFO,
					String.format("[Skills] Tool-tier defaults discovered: keyword.default=%s tiers=%s",
							value(toolTierDefaults, "keyword.default", "<missing>"),
							value(toolTierDefaults, "tiers", "<missing>")));
		}
	}

	/**
	 * Loads node definitions from classpath resources.
	 *
	 * <p>
	 * Loading is intentionally index-driven because classpath directory enumeration
	 * is not reliable across runtime containers/JAR layouts.
	 */
	private int loadNodesFromResources() {
		List<String> nodeFiles = readNodeIndex();
		if (nodeFiles.isEmpty()) {
			LOGGER.log(Level.WARNING,
					String.format("[Skills] Node index empty or missing: resource=%s", NODE_INDEX_RESOURCE));
			return 0;
		}

		int count = 0;
		for (String fileName : nodeFiles) {
			if (loadSingleNodeResource(fileName)) {
				count++;
			}
		}
		return count;
	}

	@Nonnull
	private List<String> readNodeIndex() {
		List<String> files = new ArrayList<>();
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader serviceClassLoader = SkillNodeLookupService.class.getClassLoader();
		ClassLoader classLoader = serviceClassLoader != null ? serviceClassLoader : contextClassLoader;

		LOGGER.log(Level.INFO,
				String.format(
						"[Skills][Diag] Node index loader selectedCL=%s contextVisible=%s serviceVisible=%s resource=%s",
						describeClassLoader(classLoader),
						probeResourceVisible(contextClassLoader, NODE_INDEX_RESOURCE),
						probeResourceVisible(serviceClassLoader, NODE_INDEX_RESOURCE),
						NODE_INDEX_RESOURCE));

		if (classLoader == null) {
			LOGGER.log(Level.SEVERE,
					"[Skills] Cannot read node index because no classloader is available for the current runtime thread");
			return files;
		}

		InputStream selectedInput = classLoader == null ? null : classLoader.getResourceAsStream(NODE_INDEX_RESOURCE);
		if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
			classLoader = contextClassLoader;
			selectedInput = classLoader.getResourceAsStream(NODE_INDEX_RESOURCE);
		}

		if (selectedInput == null) {
			LOGGER.log(Level.WARNING,
					String.format(
							"[Skills][Diag] Node index not found via selectedCL=%s resource=%s contextVisible=%s serviceVisible=%s",
							describeClassLoader(classLoader),
							NODE_INDEX_RESOURCE,
							probeResourceVisible(contextClassLoader, NODE_INDEX_RESOURCE),
							probeResourceVisible(serviceClassLoader, NODE_INDEX_RESOURCE)));
			return files;
		}

		try (InputStream input = selectedInput) {

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String trimmed = line.trim();
					// Support comments and blank lines to keep index human-editable.
					if (trimmed.isEmpty() || trimmed.startsWith("#")) {
						continue;
					}
					files.add(trimmed);
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING,
					String.format("[Skills] Failed to read node index resource=%s", NODE_INDEX_RESOURCE), e);
		}

		LOGGER.log(Level.FINE,
				String.format("[Skills] Node index resolved %d file(s) from %s", files.size(), NODE_INDEX_RESOURCE));
		return files;
	}

	private boolean loadSingleNodeResource(@Nonnull String fileName) {
		String resourcePath = NODE_RESOURCE_PREFIX + fileName;
		Properties properties = loadPropertiesResource(resourcePath);
		if (properties.isEmpty()) {
			LOGGER.log(Level.WARNING,
					String.format("[Skills] Node resource is missing or empty: %s", resourcePath));
			return false;
		}

		String id = value(properties, "id", fileName.replace(".properties", ""));
		SkillType skillType = SkillType.fromString(value(properties, "skill", SkillType.WOODCUTTING.name()));
		List<String> blockIds = resolveBlockIds(properties);
		String primaryBlockId = blockIds.get(0);
		int requiredSkillLevel = integerValue(properties, "requiredSkillLevel", 1);
		ToolTier requiredToolTier = ToolTier.fromString(value(properties, "requiredToolTier", ToolTier.NONE.name()));
		String requiredToolKeyword = value(properties, "requiredToolKeyword", "axe");
		double experienceReward = doubleValue(properties, "experienceReward", 0.0D);
		double depletionChance = clamp01(doubleValue(properties, "depletionChance", 1.0D));
		boolean depletes = booleanValue(properties, "depletes", true);
		int respawnSeconds = Math.max(0, integerValue(properties, "respawnSeconds", 5));

		SkillNodeDefinition definition = new SkillNodeDefinition(id, skillType, primaryBlockId, requiredSkillLevel,
				requiredToolTier, requiredToolKeyword, experienceReward, depletionChance, depletes, respawnSeconds);
		register(definition, blockIds);

		LOGGER.log(Level.INFO,
				String.format(
						"[Skills] Loaded node resource=%s id=%s skill=%s blocks=%s level=%d tier=%s keyword=%s xp=%.2f depleteChance=%.2f respawn=%ds",
						resourcePath, id, skillType, blockIds, requiredSkillLevel, requiredToolTier,
						requiredToolKeyword,
						experienceReward, depletionChance, respawnSeconds));
		return true;
	}

	/**
	 * Minimal hardcoded safety defaults so runtime never starts with an empty
	 * registry.
	 */
	private void registerFallbackDefaults() {
		register(new SkillNodeDefinition("oak_tree", SkillType.WOODCUTTING, "OakLog", 1, ToolTier.BRONZE, "axe", 25.0D,
				1.0D, true, 8));
		register(new SkillNodeDefinition("birch_tree", SkillType.WOODCUTTING, "BirchLog", 15, ToolTier.IRON, "axe",
				37.5D, 1.0D, true, 10));
	}

	/**
	 * Registers or replaces a node definition.
	 */
	public void register(@Nonnull SkillNodeDefinition definition) {
		register(definition, List.of(definition.getBlockId()));
	}

	/**
	 * Registers or replaces a node definition for all mapped block ids.
	 */
	public void register(@Nonnull SkillNodeDefinition definition, @Nonnull List<String> blockIds) {
		for (String rawBlockId : blockIds) {
			String normalized = normalize(rawBlockId);
			SkillNodeDefinition previous = byBlockId.put(normalized, definition);
			if (previous != null && previous != definition) {
				LOGGER.log(Level.WARNING,
						String.format(
								"[Skills] Replaced node mapping for block=%s oldNode=%s newNode=%s",
								rawBlockId,
								previous.getId(),
								definition.getId()));
			}
		}

		LOGGER.log(Level.FINE,
				String.format("[Skills] Registered node definition id=%s blocks=%s", definition.getId(), blockIds));
	}

	/**
	 * Returns a node definition for the given block id, or null when unknown.
	 */
	@Nullable
	public SkillNodeDefinition findByBlockId(@Nullable String blockId) {
		if (blockId == null || blockId.isBlank()) {
			LOGGER.log(Level.FINER, "[Skills] Node lookup skipped: missing block id");
			return null;
		}

		SkillNodeDefinition def = byBlockId.get(normalize(blockId));
		if (def == null) {
			LOGGER.log(Level.FINER, String.format("[Skills] Node lookup miss for block=%s", blockId));
		}
		return def;
	}

	@Nonnull
	private static String normalize(@Nonnull String input) {
		return input.trim().toLowerCase();
	}

	@Nonnull
	private static String value(@Nonnull Properties properties, @Nonnull String key, @Nonnull String defaultValue) {
		String raw = properties.getProperty(key);
		if (raw == null) {
			return defaultValue;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? defaultValue : trimmed;
	}

	private static int integerValue(@Nonnull Properties properties, @Nonnull String key, int defaultValue) {
		String raw = properties.getProperty(key);
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING,
					String.format("[Skills] Invalid integer for key=%s value=%s; using default=%d", key, raw,
							defaultValue));
			return defaultValue;
		}
	}

	private static double doubleValue(@Nonnull Properties properties, @Nonnull String key, double defaultValue) {
		String raw = properties.getProperty(key);
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}

		try {
			return Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING,
					String.format("[Skills] Invalid decimal for key=%s value=%s; using default=%f", key, raw,
							defaultValue));
			return defaultValue;
		}
	}

	private static boolean booleanValue(@Nonnull Properties properties, @Nonnull String key, boolean defaultValue) {
		String raw = properties.getProperty(key);
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}
		return Boolean.parseBoolean(raw.trim());
	}

	@Nonnull
	private static List<String> resolveBlockIds(@Nonnull Properties properties) {
		String rawBlockIds = properties.getProperty("blockIds");
		if (rawBlockIds == null || rawBlockIds.isBlank()) {
			return List.of(value(properties, "blockId", "Empty"));
		}

		LinkedHashSet<String> parsed = new LinkedHashSet<>();
		for (String token : rawBlockIds.split(",")) {
			String trimmed = token.trim();
			if (!trimmed.isEmpty()) {
				parsed.add(trimmed);
			}
		}

		if (parsed.isEmpty()) {
			return List.of(value(properties, "blockId", "Empty"));
		}

		return List.copyOf(parsed);
	}

	private static double clamp01(double value) {
		return Math.max(0.0D, Math.min(1.0D, value));
	}

	@Nonnull
	private static Properties loadPropertiesResource(@Nonnull String resourcePath) {
		Properties properties = new Properties();

		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader serviceClassLoader = SkillNodeLookupService.class.getClassLoader();
		ClassLoader classLoader = serviceClassLoader != null ? serviceClassLoader : contextClassLoader;

		if (classLoader == null) {
			LOGGER.log(Level.SEVERE,
					String.format("[Skills] Cannot read resource=%s because no classloader is available",
							resourcePath));
			return properties;
		}

		InputStream selectedInput = classLoader == null ? null : classLoader.getResourceAsStream(resourcePath);
		if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
			classLoader = contextClassLoader;
			selectedInput = classLoader.getResourceAsStream(resourcePath);
		}

		if (selectedInput == null) {
			LOGGER.log(Level.FINE, String.format("[Skills] Optional resource not found: %s", resourcePath));
			LOGGER.log(Level.INFO,
					String.format(
							"[Skills][Diag] Resource miss via selectedCL=%s resource=%s contextVisible=%s serviceVisible=%s",
							describeClassLoader(classLoader),
							resourcePath,
							probeResourceVisible(contextClassLoader, resourcePath),
							probeResourceVisible(serviceClassLoader, resourcePath)));
			return properties;
		}

		try (InputStream input = selectedInput) {
			properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
			LOGGER.log(Level.FINE,
					String.format("[Skills] Loaded resource=%s entries=%d", resourcePath, properties.size()));
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, String.format("[Skills] Failed loading resource=%s", resourcePath), e);
		}

		return properties;
	}

	@Nonnull
	private static String describeClassLoader(@Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			return "<null>";
		}
		return classLoader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(classLoader));
	}

	private static boolean probeResourceVisible(@Nullable ClassLoader classLoader, @Nonnull String resourcePath) {
		if (classLoader == null) {
			return false;
		}
		return classLoader.getResource(resourcePath) != null;
	}
}
