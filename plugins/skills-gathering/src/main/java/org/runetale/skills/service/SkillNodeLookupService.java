package org.runetale.skills.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.skills.config.SkillsPathLayout;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

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
	private static final String NODES_RESOURCE = "Skills/Nodes/nodes.json";
	private static final String GATHERING_CONFIG_RESOURCE = "Skills/Config/gathering.json";

	/**
	 * Keyed by block id for fast break-event matching.
	 */
	private final Map<String, SkillNodeDefinition> byBlockId = new ConcurrentHashMap<>();
	private final List<WildcardBlockMapping> wildcardBlockMappings = new CopyOnWriteArrayList<>();
	@Nullable
	private final Path externalConfigRoot;

	public SkillNodeLookupService() {
		this(null);
	}

	public SkillNodeLookupService(@Nullable Path externalConfigRoot) {
		this.externalConfigRoot = externalConfigRoot;
	}

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
		JsonObject gatheringConfig = loadJsonObjectResource(GATHERING_CONFIG_RESOURCE);
		if (!gatheringConfig.entrySet().isEmpty()) {
			JsonObject xpProfileDefaults = objectValue(gatheringConfig, "xpProfileDefaults");
			LOGGER.atInfo().log("[Skills] XP defaults discovered: profileId=%s curveModel=%s maxLevel=%s",
					value(xpProfileDefaults, "profileId", "<missing>"),
					value(xpProfileDefaults, "curveModel", "<missing>"),
					value(xpProfileDefaults, "maxLevel", "<missing>"));

			JsonObject toolTierDefaults = objectValue(gatheringConfig, "toolTierDefaults");
			LOGGER.atInfo().log("[Skills] Tool-tier defaults discovered: keyword.default=%s tiers=%s",
					value(toolTierDefaults, "keywordDefault", "<missing>"),
					joinStringArray(toolTierDefaults.get("tiers")));
		}
	}

	/**
	 * Loads node definitions from grouped JSON resources.
	 */
	private int loadNodesFromResources() {
		JsonObject root = loadJsonObjectResource(NODES_RESOURCE);
		if (root.entrySet().isEmpty()) {
			LOGGER.atWarning().log("[Skills] Node definition resource empty or missing: resource=%s", NODES_RESOURCE);
			return 0;
		}

		int count = 0;
		for (Map.Entry<String, JsonElement> skillEntry : root.entrySet()) {
			String groupedSkill = skillEntry.getKey();
			JsonElement entryValue = skillEntry.getValue();
			if (entryValue == null || !entryValue.isJsonArray()) {
				LOGGER.atWarning().log("[Skills] Skipping node group=%s because it is not an array", groupedSkill);
				continue;
			}

			JsonArray definitions = entryValue.getAsJsonArray();
			int index = 0;
			for (JsonElement definitionElement : definitions) {
				if (definitionElement != null && definitionElement.isJsonObject()) {
					if (loadSingleNodeResource(groupedSkill, index, definitionElement.getAsJsonObject())) {
						count++;
					}
				}
				index++;
			}
		}
		return count;
	}

	private boolean loadSingleNodeResource(
			@Nonnull String groupedSkill,
			int index,
			@Nonnull JsonObject object) {
		String sourceLabel = NODES_RESOURCE + "#" + groupedSkill + "[" + index + "]";
		String id = value(object, "id", groupedSkill + "_" + index);
		String label = optionalValue(object, "label");
		String rawSkill = optionalValue(object, "skill");
		if (rawSkill == null || rawSkill.isBlank()) {
			rawSkill = groupedSkill;
		}
		SkillType skillType = SkillType.tryParseStrict(rawSkill);
		if (skillType == null) {
			LOGGER.atWarning().log(
					"[Skills] Skipping node resource=%s id=%s because skill is missing or invalid: %s",
					sourceLabel,
					id,
					rawSkill == null ? "<missing>" : rawSkill);
			return false;
		}
		List<String> blockIds = resolveBlockIds(object);
		String primaryBlockId = blockIds.get(0);
		int requiredSkillLevel = integerValue(object, "requiredSkillLevel", 1);
		ToolTier requiredToolTier = ToolTier.fromString(value(object, "requiredToolTier", ToolTier.NONE.name()));
		String requiredToolKeyword = value(object, "requiredToolKeyword", "axe");
		double experienceReward = doubleValue(object, "experienceReward", 0.0D);

		SkillNodeDefinition definition = new SkillNodeDefinition(id, label, skillType, primaryBlockId, requiredSkillLevel,
				requiredToolTier, requiredToolKeyword, experienceReward);
		register(definition, blockIds);

		LOGGER.atInfo().log(
				"[Skills] Loaded node resource=%s id=%s skill=%s blocks=%s level=%d tier=%s keyword=%s xp=%.2f",
				sourceLabel, id, skillType, blockIds, requiredSkillLevel, requiredToolTier,
				requiredToolKeyword,
				experienceReward);
		return true;
	}

	/**
	 * Minimal hardcoded safety defaults so runtime never starts with an empty
	 * registry.
	 */
	private void registerFallbackDefaults() {
		register(new SkillNodeDefinition("oak_tree", SkillType.WOODCUTTING, "OakLog", 1, ToolTier.BRONZE,
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
			if (isWildcardPattern(normalized)) {
				registerWildcard(definition, rawBlockId, normalized);
				continue;
			}

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

		String normalizedBlockId = normalize(blockId);
		SkillNodeDefinition def = byBlockId.get(normalizedBlockId);
		if (def == null) {
			String simplifiedBlockId = simplifyBlockId(normalizedBlockId);
			if (!simplifiedBlockId.equals(normalizedBlockId)) {
				def = byBlockId.get(simplifiedBlockId);
			}
		}
		if (def == null) {
			def = findByWildcardBlockId(normalizedBlockId, blockId);
		}

		if (def == null) {
			LOGGER.atFiner().log("[Skills] Node lookup miss for block=%s", blockId);
		}
		return def;
	}

	private void registerWildcard(@Nonnull SkillNodeDefinition definition, @Nonnull String rawPattern,
			@Nonnull String normalizedPattern) {
		WildcardBlockMapping previous = removeWildcardMapping(normalizedPattern);
		if (previous != null && previous.definition() != definition) {
			LOGGER.atWarning().log("[Skills] Replaced node wildcard mapping pattern=%s oldNode=%s newNode=%s",
					rawPattern,
					previous.definition().getId(),
					definition.getId());
		}

		this.wildcardBlockMappings
				.add(new WildcardBlockMapping(normalizedPattern, createWildcardPattern(normalizedPattern), definition));
		LOGGER.atFine().log("[Skills] Registered node wildcard id=%s pattern=%s", definition.getId(), rawPattern);
	}

	@Nullable
	private WildcardBlockMapping removeWildcardMapping(@Nonnull String normalizedPattern) {
		for (WildcardBlockMapping mapping : this.wildcardBlockMappings) {
			if (mapping.rawPattern().equals(normalizedPattern)) {
				this.wildcardBlockMappings.remove(mapping);
				return mapping;
			}
		}
		return null;
	}

	@Nullable
	private SkillNodeDefinition findByWildcardBlockId(@Nonnull String normalizedBlockId,
			@Nonnull String originalBlockId) {
		String simplifiedBlockId = simplifyBlockId(normalizedBlockId);
		SkillNodeDefinition firstMatch = null;
		String firstPattern = null;
		for (WildcardBlockMapping mapping : this.wildcardBlockMappings) {
			if (!matchesWildcard(mapping.pattern(), normalizedBlockId, simplifiedBlockId)) {
				continue;
			}

			if (firstMatch == null) {
				firstMatch = mapping.definition();
				firstPattern = mapping.rawPattern();
				continue;
			}

			LOGGER.atWarning().log(
					"[Skills] Block id=%s matched multiple wildcard node mappings; using first pattern=%s node=%s over pattern=%s node=%s",
					originalBlockId,
					firstPattern,
					firstMatch.getId(),
					mapping.rawPattern(),
					mapping.definition().getId());
			break;
		}

		return firstMatch;
	}

	private static boolean matchesWildcard(@Nonnull Pattern pattern, @Nonnull String normalizedBlockId,
			@Nonnull String simplifiedBlockId) {
		if (pattern.matcher(normalizedBlockId).matches()) {
			return true;
		}
		return !simplifiedBlockId.equals(normalizedBlockId) && pattern.matcher(simplifiedBlockId).matches();
	}

	@Nonnull
	private static String simplifyBlockId(@Nonnull String normalizedBlockId) {
		String simplified = normalizedBlockId;

		int namespaceSeparator = simplified.lastIndexOf(':');
		if (namespaceSeparator >= 0 && namespaceSeparator + 1 < simplified.length()) {
			simplified = simplified.substring(namespaceSeparator + 1);
		}

		int pathSeparator = simplified.lastIndexOf('/');
		if (pathSeparator >= 0 && pathSeparator + 1 < simplified.length()) {
			simplified = simplified.substring(pathSeparator + 1);
		}

		return simplified;
	}

	private static boolean isWildcardPattern(@Nonnull String token) {
		return token.indexOf('*') >= 0;
	}

	@Nonnull
	private static Pattern createWildcardPattern(@Nonnull String normalizedPattern) {
		StringBuilder regexBuilder = new StringBuilder(normalizedPattern.length() + 8);
		regexBuilder.append('^');
		int segmentStart = 0;
		for (int i = 0; i < normalizedPattern.length(); i++) {
			if (normalizedPattern.charAt(i) != '*') {
				continue;
			}

			if (segmentStart < i) {
				String literalSegment = normalizedPattern.substring(segmentStart, i);
				regexBuilder.append(Pattern.quote(literalSegment));
			}
			regexBuilder.append(".*");
			segmentStart = i + 1;
		}

		if (segmentStart < normalizedPattern.length()) {
			String literalSegment = normalizedPattern.substring(segmentStart);
			regexBuilder.append(Pattern.quote(literalSegment));
		}
		regexBuilder.append('$');
		return Pattern.compile(regexBuilder.toString());
	}

	private record WildcardBlockMapping(@Nonnull String rawPattern, @Nonnull Pattern pattern,
			@Nonnull SkillNodeDefinition definition) {
	}

	@Nonnull
	public List<SkillNodeDefinition> listAllDefinitions() {
		LinkedHashSet<SkillNodeDefinition> deduped = new LinkedHashSet<>(this.byBlockId.values());
		for (WildcardBlockMapping mapping : this.wildcardBlockMappings) {
			deduped.add(mapping.definition());
		}
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
		return input.trim().toLowerCase(Locale.ROOT);
	}

	@Nonnull
	private static String value(@Nonnull JsonObject object, @Nonnull String key, @Nonnull String defaultValue) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return defaultValue;
		}
		String trimmed;
		try {
			trimmed = element.getAsString().trim();
		} catch (RuntimeException ignored) {
			return defaultValue;
		}
		return trimmed.isEmpty() ? defaultValue : trimmed;
	}

	private static int integerValue(@Nonnull JsonObject object, @Nonnull String key, int defaultValue) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return defaultValue;
		}

		try {
			return element.getAsInt();
		} catch (RuntimeException e) {
			LOGGER.atWarning().log("[Skills] Invalid integer for key=%s; using default=%d", key, defaultValue);
			return defaultValue;
		}
	}

	@Nullable
	private static String optionalValue(@Nonnull JsonObject object, @Nonnull String key) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return null;
		}
		String raw;
		try {
			raw = element.getAsString();
		} catch (RuntimeException ignored) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static double doubleValue(@Nonnull JsonObject object, @Nonnull String key, double defaultValue) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return defaultValue;
		}

		try {
			return element.getAsDouble();
		} catch (RuntimeException e) {
			LOGGER.atWarning().log("[Skills] Invalid decimal for key=%s; using default=%f", key, defaultValue);
			return defaultValue;
		}
	}

	@Nonnull
	private static List<String> resolveBlockIds(@Nonnull JsonObject object) {
		JsonElement blockIdsElement = object.get("blockIds");
		if (blockIdsElement == null || blockIdsElement.isJsonNull()) {
			return List.of(value(object, "blockId", "Empty"));
		}

		LinkedHashSet<String> parsed = new LinkedHashSet<>();
		if (blockIdsElement.isJsonArray()) {
			for (JsonElement tokenElement : blockIdsElement.getAsJsonArray()) {
				if (tokenElement == null || tokenElement.isJsonNull()) {
					continue;
				}
				String trimmed = tokenElement.getAsString().trim();
				if (!trimmed.isEmpty()) {
					parsed.add(trimmed);
				}
			}
		} else {
			for (String token : blockIdsElement.getAsString().split(",")) {
				String trimmed = token.trim();
				if (!trimmed.isEmpty()) {
					parsed.add(trimmed);
				}
			}
		}

		if (parsed.isEmpty()) {
			return List.of(value(object, "blockId", "Empty"));
		}

		return List.copyOf(parsed);
	}

	@Nonnull
	private static JsonObject objectValue(@Nonnull JsonObject object, @Nonnull String key) {
		JsonElement element = object.get(key);
		if (element != null && element.isJsonObject()) {
			return element.getAsJsonObject();
		}
		return new JsonObject();
	}

	@Nonnull
	private static String joinStringArray(@Nullable JsonElement element) {
		if (element == null || !element.isJsonArray()) {
			return "<missing>";
		}

		List<String> values = new ArrayList<>();
		for (JsonElement value : element.getAsJsonArray()) {
			if (value == null || value.isJsonNull()) {
				continue;
			}
			values.add(value.getAsString());
		}
		return values.isEmpty() ? "<missing>" : String.join(",", values);
	}

	@Nonnull
	private JsonObject loadJsonObjectResource(@Nonnull String resourcePath) {
		try (InputStream input = openResourceInput(resourcePath)) {
			if (input == null) {
				LOGGER.atFine().log("[Skills] Optional resource not found: %s", resourcePath);
				ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
				ClassLoader serviceClassLoader = SkillNodeLookupService.class.getClassLoader();
				LOGGER.atInfo().log(
						"[Skills][Diag] Resource miss via selectedCL=%s resource=%s contextVisible=%s serviceVisible=%s",
						describeClassLoader(serviceClassLoader),
						resourcePath,
						probeResourceVisible(contextClassLoader, resourcePath),
						probeResourceVisible(serviceClassLoader, resourcePath));
				return new JsonObject();
			}

			try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
				JsonElement parsed = JsonParser.parseReader(reader);
				if (parsed != null && parsed.isJsonObject()) {
					JsonObject object = parsed.getAsJsonObject();
					LOGGER.atFine().log("[Skills] Loaded resource=%s entries=%d", resourcePath, object.size());
					return object;
				}
				LOGGER.atWarning().log("[Skills] Resource root must be JSON object: %s", resourcePath);
				return new JsonObject();
			}
		} catch (IOException e) {
			LOGGER.atWarning().withCause(e).log("[Skills] Failed loading resource=%s", resourcePath);
			return new JsonObject();
		} catch (RuntimeException e) {
			LOGGER.atWarning().withCause(e).log("[Skills] Failed parsing JSON resource=%s", resourcePath);
			return new JsonObject();
		}
	}

	@Nullable
	private InputStream openResourceInput(@Nonnull String resourcePath) {
		Path externalPath = resolveExternalPath(resourcePath);
		if (externalPath != null && Files.isRegularFile(externalPath)) {
			try {
				LOGGER.atInfo().log("[Skills] Loading external resource file=%s", externalPath);
				return Files.newInputStream(externalPath);
			} catch (IOException e) {
				LOGGER.atWarning().withCause(e).log("[Skills] Failed opening external resource file=%s", externalPath);
			}
		}

		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader serviceClassLoader = SkillNodeLookupService.class.getClassLoader();
		ClassLoader classLoader = serviceClassLoader != null ? serviceClassLoader : contextClassLoader;
		if (classLoader == null) {
			return null;
		}

		InputStream selectedInput = classLoader.getResourceAsStream(resourcePath);
		if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
			selectedInput = contextClassLoader.getResourceAsStream(resourcePath);
		}
		return selectedInput;
	}

	@Nullable
	private Path resolveExternalPath(@Nonnull String resourcePath) {
		if (this.externalConfigRoot == null) {
			return null;
		}

		String relative = SkillsPathLayout.externalRelativeResourcePath(resourcePath);
		return this.externalConfigRoot.resolve(relative.replace('/', File.separatorChar));
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
