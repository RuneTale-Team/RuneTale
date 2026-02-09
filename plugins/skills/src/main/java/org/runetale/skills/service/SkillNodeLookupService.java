package org.runetale.skills.service;

import com.hypixel.hytale.logger.HytaleLogger;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
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
		LOGGER.atInfo().log("[Skills] Initializing skill-node definitions from resources");
		loadAndLogOptionalSharedDefaults();

		int loadedFromResources = loadNodesFromResources();
		if (loadedFromResources <= 0) {
			LOGGER.atWarning().log(
					"[Skills] No node definitions loaded from resources. Falling back to in-memory safety defaults.");
			registerFallbackDefaults();
			return;
		}

		LOGGER.atInfo().log("[Skills] Node resource bootstrap completed with %d definition(s)", loadedFromResources);
	}

	/**
	 * Loads optional shared defaults and logs key metadata to verify packaging and
	 * discoverability.
	 */
	private void loadAndLogOptionalSharedDefaults() {
		Properties xpProfileDefaults = loadPropertiesResource(XP_PROFILE_DEFAULTS_RESOURCE);
		if (!xpProfileDefaults.isEmpty()) {
			LOGGER.atInfo().log("[Skills] XP defaults discovered: profileId=%s curveModel=%s maxLevel=%s",
					value(xpProfileDefaults, "profileId", "<missing>"),
					value(xpProfileDefaults, "curveModel", "<missing>"),
					value(xpProfileDefaults, "maxLevel", "<missing>"));
		}

		Properties toolTierDefaults = loadPropertiesResource(TOOL_TIER_DEFAULTS_RESOURCE);
		if (!toolTierDefaults.isEmpty()) {
			LOGGER.atInfo().log("[Skills] Tool-tier defaults discovered: keyword.default=%s tiers=%s",
					value(toolTierDefaults, "keyword.default", "<missing>"),
					value(toolTierDefaults, "tiers", "<missing>"));
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
			LOGGER.atWarning().log("[Skills] Node index empty or missing: resource=%s", NODE_INDEX_RESOURCE);
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

		LOGGER.atInfo().log(
				"[Skills][Diag] Node index loader selectedCL=%s contextVisible=%s serviceVisible=%s resource=%s",
				describeClassLoader(classLoader),
				probeResourceVisible(contextClassLoader, NODE_INDEX_RESOURCE),
				probeResourceVisible(serviceClassLoader, NODE_INDEX_RESOURCE),
				NODE_INDEX_RESOURCE);

		if (classLoader == null) {
			LOGGER.atSevere().log(
					"[Skills] Cannot read node index because no classloader is available for the current runtime thread");
			return files;
		}

		InputStream selectedInput = classLoader == null ? null : classLoader.getResourceAsStream(NODE_INDEX_RESOURCE);
		if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
			classLoader = contextClassLoader;
			selectedInput = classLoader.getResourceAsStream(NODE_INDEX_RESOURCE);
		}

		if (selectedInput == null) {
			LOGGER.atWarning().log(
					"[Skills][Diag] Node index not found via selectedCL=%s resource=%s contextVisible=%s serviceVisible=%s",
					describeClassLoader(classLoader),
					NODE_INDEX_RESOURCE,
					probeResourceVisible(contextClassLoader, NODE_INDEX_RESOURCE),
					probeResourceVisible(serviceClassLoader, NODE_INDEX_RESOURCE));
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
			LOGGER.atWarning().withCause(e).log("[Skills] Failed to read node index resource=%s", NODE_INDEX_RESOURCE);
		}

		LOGGER.atFine().log("[Skills] Node index resolved %d file(s) from %s", files.size(), NODE_INDEX_RESOURCE);
		return files;
	}

	private boolean loadSingleNodeResource(@Nonnull String fileName) {
		String resourcePath = NODE_RESOURCE_PREFIX + fileName;
		Properties properties = loadPropertiesResource(resourcePath);
		if (properties.isEmpty()) {
			LOGGER.atWarning().log("[Skills] Node resource is missing or empty: %s", resourcePath);
			return false;
		}

		String id = value(properties, "id", fileName.replace(".properties", ""));
		String label = optionalValue(properties, "label");
		String rawSkill = optionalValue(properties, "skill");
		SkillType skillType = SkillType.tryParseStrict(rawSkill);
		if (skillType == null) {
			LOGGER.atWarning().log(
					"[Skills] Skipping node resource=%s id=%s because skill is missing or invalid: %s",
					resourcePath,
					id,
					rawSkill == null ? "<missing>" : rawSkill);
			return false;
		}
		List<String> blockIds = resolveBlockIds(properties);
		String primaryBlockId = blockIds.get(0);
		int requiredSkillLevel = integerValue(properties, "requiredSkillLevel", 1);
		ToolTier requiredToolTier = ToolTier.fromString(value(properties, "requiredToolTier", ToolTier.NONE.name()));
		String requiredToolKeyword = value(properties, "requiredToolKeyword", "axe");
		double experienceReward = doubleValue(properties, "experienceReward", 0.0D);

		SkillNodeDefinition definition = new SkillNodeDefinition(id, label, skillType, primaryBlockId, requiredSkillLevel,
				requiredToolTier, requiredToolKeyword, experienceReward);
		register(definition, blockIds);

		LOGGER.atInfo().log(
				"[Skills] Loaded node resource=%s id=%s skill=%s blocks=%s level=%d tier=%s keyword=%s xp=%.2f",
				resourcePath, id, skillType, blockIds, requiredSkillLevel, requiredToolTier,
				requiredToolKeyword,
				experienceReward);
		return true;
	}

	/**
	 * Minimal hardcoded safety defaults so runtime never starts with an empty
	 * registry.
	 */
	private void registerFallbackDefaults() {
		register(new SkillNodeDefinition("oak_tree", SkillType.WOODCUTTING, "OakLog", 1, ToolTier.WOOD,
				"Tool_Hatchet", 25.0D));
		register(new SkillNodeDefinition("birch_tree", SkillType.WOODCUTTING, "BirchLog", 15, ToolTier.IRON,
				"Tool_Hatchet",
				37.5D));
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
				LOGGER.atWarning().log("[Skills] Replaced node mapping for block=%s oldNode=%s newNode=%s",
						rawBlockId,
						previous.getId(),
						definition.getId());
			}
		}

		LOGGER.atFine().log("[Skills] Registered node definition id=%s blocks=%s", definition.getId(), blockIds);
	}

	/**
	 * Returns a node definition for the given block id, or null when unknown.
	 */
	@Nullable
	public SkillNodeDefinition findByBlockId(@Nullable String blockId) {
		if (blockId == null || blockId.isBlank()) {
			LOGGER.atFiner().log("[Skills] Node lookup skipped: missing block id");
			return null;
		}

		SkillNodeDefinition def = byBlockId.get(normalize(blockId));
		if (def == null) {
			LOGGER.atFiner().log("[Skills] Node lookup miss for block=%s", blockId);
		}
		return def;
	}

	@Nonnull
	public List<SkillNodeDefinition> listAllDefinitions() {
		LinkedHashSet<SkillNodeDefinition> deduped = new LinkedHashSet<>(this.byBlockId.values());
		List<SkillNodeDefinition> definitions = new ArrayList<>(deduped);
		definitions.sort(Comparator
				.comparingInt(SkillNodeDefinition::getRequiredSkillLevel)
				.thenComparing(SkillNodeDefinition::getId));
		return definitions;
	}

	@Nonnull
	public List<SkillNodeDefinition> listDefinitionsForSkill(@Nonnull SkillType skillType) {
		List<SkillNodeDefinition> filtered = new ArrayList<>();
		for (SkillNodeDefinition definition : listAllDefinitions()) {
			if (definition.getSkillType() == skillType) {
				filtered.add(definition);
			}
		}
		return filtered;
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
			LOGGER.atWarning().log("[Skills] Invalid integer for key=%s value=%s; using default=%d", key, raw,
					defaultValue);
			return defaultValue;
		}
	}

	@Nullable
	private static String optionalValue(@Nonnull Properties properties, @Nonnull String key) {
		String raw = properties.getProperty(key);
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static double doubleValue(@Nonnull Properties properties, @Nonnull String key, double defaultValue) {
		String raw = properties.getProperty(key);
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}

		try {
			return Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			LOGGER.atWarning().log("[Skills] Invalid decimal for key=%s value=%s; using default=%f", key, raw,
					defaultValue);
			return defaultValue;
		}
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

	@Nonnull
	private static Properties loadPropertiesResource(@Nonnull String resourcePath) {
		Properties properties = new Properties();

		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader serviceClassLoader = SkillNodeLookupService.class.getClassLoader();
		ClassLoader classLoader = serviceClassLoader != null ? serviceClassLoader : contextClassLoader;

		if (classLoader == null) {
			LOGGER.atSevere().log("[Skills] Cannot read resource=%s because no classloader is available",
					resourcePath);
			return properties;
		}

		InputStream selectedInput = classLoader == null ? null : classLoader.getResourceAsStream(resourcePath);
		if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
			classLoader = contextClassLoader;
			selectedInput = classLoader.getResourceAsStream(resourcePath);
		}

		if (selectedInput == null) {
			LOGGER.atFine().log("[Skills] Optional resource not found: %s", resourcePath);
			LOGGER.atInfo().log(
					"[Skills][Diag] Resource miss via selectedCL=%s resource=%s contextVisible=%s serviceVisible=%s",
					describeClassLoader(classLoader),
					resourcePath,
					probeResourceVisible(contextClassLoader, resourcePath),
					probeResourceVisible(serviceClassLoader, resourcePath));
			return properties;
		}

		try (InputStream input = selectedInput) {
			properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
			LOGGER.atFine().log("[Skills] Loaded resource=%s entries=%d", resourcePath, properties.size());
		} catch (IOException e) {
			LOGGER.atWarning().withCause(e).log("[Skills] Failed loading resource=%s", resourcePath);
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
