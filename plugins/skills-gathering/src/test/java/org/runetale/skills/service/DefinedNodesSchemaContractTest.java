package org.runetale.skills.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class DefinedNodesSchemaContractTest {

	private static final String NODES_RESOURCE = "Skills/Nodes/nodes.json";

	private static final Set<String> REQUIRED_KEYS = Set.of(
			"id",
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

	@Test
	void groupedNodeDefinitionsExistAndContainArrays() throws IOException {
		JsonObject root = loadRoot();

		assertThat(root.keySet()).isNotEmpty();
		for (String group : root.keySet()) {
			JsonElement groupElement = root.get(group);
			assertThat(groupElement != null && groupElement.isJsonArray())
					.as("group %s is an array", group)
					.isTrue();
		}
	}

	@Test
	void definedNodesHaveRequiredKeysAndUniqueIds(TestReporter reporter) throws IOException {
		JsonObject root = loadRoot();
		LinkedHashMap<String, String> pathByNodeId = new LinkedHashMap<>();

		for (String groupedSkill : root.keySet()) {
			JsonArray entries = root.getAsJsonArray(groupedSkill);
			int index = 0;
			for (JsonElement entryElement : entries) {
				assertThat(entryElement != null && entryElement.isJsonObject())
						.as("node entry is object at %s[%d]", groupedSkill, index)
						.isTrue();
				JsonObject node = entryElement.getAsJsonObject();
				String nodePath = groupedSkill + "[" + index + "]";

				for (String requiredKey : REQUIRED_KEYS) {
					JsonElement value = node.get(requiredKey);
					assertThat(value)
							.as("required key %s in %s", requiredKey, nodePath)
							.isNotNull();
					assertThat(value.isJsonNull())
							.as("required key %s is non-null in %s", requiredKey, nodePath)
							.isFalse();
					if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
						assertThat(value.getAsString().trim())
								.as("required key %s is non-blank in %s", requiredKey, nodePath)
								.isNotEmpty();
					}
				}

				Set<String> unknownKeys = node.keySet().stream()
						.filter(key -> !KNOWN_KEYS.contains(key))
						.collect(LinkedHashSet::new, Set::add, Set::addAll);
				if (!unknownKeys.isEmpty()) {
					reporter.publishEntry(
							"node-schema-warning",
							"Unknown key(s) in " + nodePath + ": " + String.join(", ", unknownKeys));
				}

				validateNodeValueSemantics(groupedSkill, nodePath, node);

				String nodeId = node.get("id").getAsString().trim();
				String previousPath = pathByNodeId.putIfAbsent(nodeId, nodePath);
				assertThat(previousPath)
						.as("duplicate node id %s across %s and %s", nodeId, previousPath, nodePath)
						.isNull();

				index++;
			}
		}
	}

	private static void validateNodeValueSemantics(String groupedSkill, String nodePath, JsonObject node) {
		String skillRaw = stringValue(node, "skill");
		if (skillRaw == null || skillRaw.isBlank()) {
			skillRaw = groupedSkill;
		}

		assertThat(SkillType.tryParseStrict(skillRaw))
				.as("skill is valid in %s", nodePath)
				.isNotNull();

		int requiredSkillLevel = intValue(node, "requiredSkillLevel", Integer.MIN_VALUE);
		assertThat(requiredSkillLevel)
				.as("requiredSkillLevel is non-negative in %s", nodePath)
				.isGreaterThanOrEqualTo(0);

		double experienceReward = doubleValue(node, "experienceReward", Double.NaN);
		assertThat(experienceReward)
				.as("experienceReward is non-negative in %s", nodePath)
				.isGreaterThanOrEqualTo(0.0D);

		String rawToolTier = stringValue(node, "requiredToolTier");
		assertThat(rawToolTier)
				.as("requiredToolTier exists in %s", nodePath)
				.isNotBlank();
		ToolTier parsedToolTier = ToolTier.fromString(rawToolTier);
		boolean recognized = "NONE".equalsIgnoreCase(rawToolTier) || parsedToolTier != ToolTier.NONE;
		assertThat(recognized)
				.as("requiredToolTier is recognized in %s", nodePath)
				.isTrue();

		List<String> blockMappings = resolveBlockMappings(node);
		assertThat(blockMappings)
				.as("node has at least one block mapping in %s", nodePath)
				.isNotEmpty();

		Set<String> deduped = blockMappings.stream()
				.map(token -> token.toLowerCase(Locale.ROOT))
				.collect(LinkedHashSet::new, Set::add, Set::addAll);
		assertThat(deduped)
				.as("node block mappings are unique in %s", nodePath)
				.hasSameSizeAs(blockMappings);
	}

	private static List<String> resolveBlockMappings(JsonObject node) {
		JsonElement blockIds = node.get("blockIds");
		if (blockIds != null && !blockIds.isJsonNull()) {
			if (blockIds.isJsonArray()) {
				LinkedHashSet<String> parsed = new LinkedHashSet<>();
				for (JsonElement element : blockIds.getAsJsonArray()) {
					if (element == null || element.isJsonNull()) {
						continue;
					}
					String trimmed = element.getAsString().trim();
					if (!trimmed.isEmpty()) {
						parsed.add(trimmed);
					}
				}
				if (!parsed.isEmpty()) {
					return List.copyOf(parsed);
				}
			} else {
				String raw = blockIds.getAsString();
				if (raw != null && !raw.isBlank()) {
					String[] split = raw.split(",");
					LinkedHashSet<String> parsed = new LinkedHashSet<>();
					for (String token : split) {
						String trimmed = token.trim();
						if (!trimmed.isEmpty()) {
							parsed.add(trimmed);
						}
					}
					if (!parsed.isEmpty()) {
						return List.copyOf(parsed);
					}
				}
			}
		}

		String singleBlock = stringValue(node, "blockId");
		if (singleBlock == null || singleBlock.isBlank()) {
			return List.of();
		}
		return List.of(singleBlock.trim());
	}

	private static JsonObject loadRoot() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(NODES_RESOURCE)) {
			assertThat(input).as("nodes resource exists").isNotNull();

			try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
				JsonElement parsed = JsonParser.parseReader(reader);
				assertThat(parsed != null && parsed.isJsonObject())
						.as("nodes root is object")
						.isTrue();
				return parsed.getAsJsonObject();
			}
		}
	}

	private static String stringValue(JsonObject object, String key) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return null;
		}
		try {
			return element.getAsString();
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private static int intValue(JsonObject object, String key, int fallback) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return fallback;
		}
		try {
			return element.getAsInt();
		} catch (RuntimeException ignored) {
			return fallback;
		}
	}

	private static double doubleValue(JsonObject object, String key, double fallback) {
		JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {
			return fallback;
		}
		try {
			return element.getAsDouble();
		} catch (RuntimeException ignored) {
			return fallback;
		}
	}
}
