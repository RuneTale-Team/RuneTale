package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;
import org.runetale.testing.junit.ContractTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class DefinedNodesSchemaContractTest {

	private static final String NODE_INDEX_RESOURCE = "Skills/Nodes/index.list";
	private static final String NODE_RESOURCE_PREFIX = "Skills/Nodes/";
	private static final String EXAMPLE_NODE_FILE = "example.properties";

	private static final Set<String> REQUIRED_KEYS = Set.of(
			"id",
			"skill",
			"requiredSkillLevel",
			"requiredToolKeyword",
			"requiredToolTier",
			"experienceReward");

	private static final Set<String> KNOWN_KEYS = Set.of(
			"id",
			"label",
			"skill",
			"requiredSkillLevel",
			"requiredToolKeyword",
			"requiredToolTier",
			"experienceReward",
			"blockIds",
			"blockId");

	private static final Set<String> RECOGNIZED_TOOL_TIER_TOKENS = recognizedToolTierTokens();

	@Test
	void indexEntriesAreUniqueAndResolveToResources() throws IOException {
		List<String> indexEntries = readNodeIndexEntries();

		assertThat(indexEntries).isNotEmpty();
		assertThat(new LinkedHashSet<>(indexEntries))
				.as("index entries are unique")
				.hasSameSizeAs(indexEntries);

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (String entry : indexEntries) {
			assertThat(classLoader.getResource(NODE_RESOURCE_PREFIX + entry))
					.as("indexed node resource exists: %s", entry)
					.isNotNull();
		}
	}

	@Test
	void allRealNodeResourcesAreIndexed() throws IOException, URISyntaxException {
		Set<String> indexEntries = new LinkedHashSet<>(readNodeIndexEntries());
		Set<String> realNodeResources = listAllNodePropertyResources();

		assertThat(indexEntries)
				.as("all real node resources are indexed")
				.containsAll(realNodeResources);
	}

	@Test
	void definedNodeFilesHaveRequiredKeysAndWarnOnUnknown(TestReporter reporter) throws IOException {
		List<String> indexEntries = readNodeIndexEntries();
		LinkedHashMap<String, String> pathByNodeId = new LinkedHashMap<>();

		for (String entry : indexEntries) {
			Properties properties = loadProperties(NODE_RESOURCE_PREFIX + entry);
			Set<String> presentKeys = properties.stringPropertyNames();

			assertThat(presentKeys)
					.as("required keys present in %s", entry)
					.containsAll(REQUIRED_KEYS);

			for (String requiredKey : REQUIRED_KEYS) {
				String rawValue = properties.getProperty(requiredKey);
				assertThat(rawValue)
						.as("required key %s in %s", requiredKey, entry)
						.isNotNull();
				assertThat(rawValue.trim())
						.as("required key %s is non-blank in %s", requiredKey, entry)
						.isNotEmpty();
			}

			String blockIdsRaw = properties.getProperty("blockIds");
			String blockIdRaw = properties.getProperty("blockId");
			boolean hasBlockIds = blockIdsRaw != null && !blockIdsRaw.isBlank();
			boolean hasBlockId = blockIdRaw != null && !blockIdRaw.isBlank();
			assertThat(hasBlockIds || hasBlockId)
					.as("node %s defines blockIds or blockId", entry)
					.isTrue();

			Set<String> unknownKeys = presentKeys.stream()
					.filter(key -> !KNOWN_KEYS.contains(key))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			if (!unknownKeys.isEmpty()) {
				reporter.publishEntry(
						"node-schema-warning",
						"Unknown key(s) in " + entry + ": " + String.join(", ", unknownKeys));
			}

			validateNodeValueSemantics(entry, properties);

			String nodeId = properties.getProperty("id").trim();
			String previousPath = pathByNodeId.putIfAbsent(nodeId, entry);
			assertThat(previousPath)
					.as("duplicate node id %s across %s and %s", nodeId, previousPath, entry)
					.isNull();
		}
	}

	private static void validateNodeValueSemantics(String entry, Properties properties) {
		String skillRaw = properties.getProperty("skill").trim();
		assertThat(SkillType.tryParseStrict(skillRaw))
				.as("skill is valid in %s", entry)
				.isNotNull();

		int requiredSkillLevel = parseInt(entry, "requiredSkillLevel", properties.getProperty("requiredSkillLevel"));
		assertThat(requiredSkillLevel)
				.as("requiredSkillLevel is non-negative in %s", entry)
				.isGreaterThanOrEqualTo(0);

		double experienceReward = parseDouble(entry, "experienceReward", properties.getProperty("experienceReward"));
		assertThat(experienceReward)
				.as("experienceReward is non-negative in %s", entry)
				.isGreaterThanOrEqualTo(0.0D);

		String rawToolTier = properties.getProperty("requiredToolTier").trim();
		assertThat(RECOGNIZED_TOOL_TIER_TOKENS.contains(rawToolTier.toUpperCase(Locale.ROOT)))
				.as("requiredToolTier is recognized in %s", entry)
				.isTrue();

		List<String> blockMappings = resolveBlockMappings(properties);
		assertThat(blockMappings)
				.as("node has at least one block mapping in %s", entry)
				.isNotEmpty();

		Set<String> deduped = blockMappings.stream()
				.map(token -> token.toLowerCase(Locale.ROOT))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		assertThat(deduped)
				.as("node block mappings are unique in %s", entry)
				.hasSameSizeAs(blockMappings);
	}

	private static List<String> resolveBlockMappings(Properties properties) {
		String blockIdsRaw = properties.getProperty("blockIds");
		if (blockIdsRaw != null && !blockIdsRaw.isBlank()) {
			List<String> parsed = splitCsv(blockIdsRaw);
			if (!parsed.isEmpty()) {
				return parsed;
			}
		}

		String blockIdRaw = properties.getProperty("blockId");
		if (blockIdRaw == null || blockIdRaw.isBlank()) {
			return List.of();
		}
		return List.of(blockIdRaw.trim());
	}

	private static List<String> splitCsv(String raw) {
		List<String> parsed = new ArrayList<>();
		for (String token : raw.split(",")) {
			String trimmed = token.trim();
			if (!trimmed.isEmpty()) {
				parsed.add(trimmed);
			}
		}
		return parsed;
	}

	private static int parseInt(String entry, String key, String raw) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (RuntimeException e) {
			throw new AssertionError("Invalid integer key=" + key + " in " + entry + ": " + raw, e);
		}
	}

	private static double parseDouble(String entry, String key, String raw) {
		try {
			return Double.parseDouble(raw.trim());
		} catch (RuntimeException e) {
			throw new AssertionError("Invalid decimal key=" + key + " in " + entry + ": " + raw, e);
		}
	}

	private static List<String> readNodeIndexEntries() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(NODE_INDEX_RESOURCE)) {
			assertThat(input).as("node index exists").isNotNull();

			List<String> entries = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String trimmed = line.trim();
					if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
						entries.add(trimmed);
					}
				}
			}
			return entries;
		}
	}

	private static Properties loadProperties(String resourcePath) throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
			assertThat(input).as("resource exists: %s", resourcePath).isNotNull();

			Properties properties = new Properties();
			properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
			return properties;
		}
	}

	private static Set<String> listAllNodePropertyResources() throws IOException, URISyntaxException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		var nodesRootUrl = classLoader.getResource("Skills/Nodes");
		assertThat(nodesRootUrl).as("node resource root exists").isNotNull();
		Path nodesRoot = Path.of(nodesRootUrl.toURI());
		try (var stream = Files.walk(nodesRoot)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".properties"))
					.map(path -> nodesRoot.relativize(path).toString().replace('\\', '/'))
					.filter(path -> !path.equals(EXAMPLE_NODE_FILE))
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
	}

	private static Set<String> recognizedToolTierTokens() {
		Set<String> tokens = new LinkedHashSet<>();
		for (ToolTier tier : ToolTier.values()) {
			tokens.add(tier.name());
		}
		tokens.add("BRONZE");
		tokens.add("STEEL");
		tokens.add("ADAMANT");
		tokens.add("RUNE");
		tokens.add("DRAGON");
		tokens.add("CRYSTAL");
		return Set.copyOf(tokens);
	}
}
