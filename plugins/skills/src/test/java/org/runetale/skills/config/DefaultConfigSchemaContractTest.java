package org.runetale.skills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class DefaultConfigSchemaContractTest {

	private static final String RESOURCE_PATH = "Skills/Config/skills.json";

	private static final Set<String> REQUIRED_PATHS = Set.of(
			"xp.maxLevel",
			"xp.levelTermMultiplier",
			"xp.growthScale",
			"xp.growthBase",
			"xp.growthDivisor",
			"xp.pointsDivisor",
			"xp.roundingMode",
			"combat.xpPerDamage",
			"combat.source.ranged",
			"combat.source.melee.prefix",
			"combat.source.melee.accurate",
			"combat.source.melee.aggressive",
			"combat.source.melee.defensive",
			"combat.source.melee.controlled.attack",
			"combat.source.melee.controlled.strength",
			"combat.source.melee.controlled.defence",
			"combat.source.block.defence",
			"combat.projectileCauseTokens",
			"hud.toast.durationMillis",
			"hud.toast.fadeDurationMillis",
			"hud.toast.fade.rootBackground",
			"hud.toast.fade.innerBackground",
			"hud.toast.expiryTickSeconds");

	@Test
	void defaultSkillsConfigJsonHasRequiredPathsAndWarnsOnUnknownTopLevelSections(TestReporter reporter) throws IOException {
		JsonObject root = loadJsonObject(RESOURCE_PATH);

		for (String path : REQUIRED_PATHS) {
			JsonElement value = valueAt(root, path);
			assertThat(value)
					.as("required path %s in %s", path, RESOURCE_PATH)
					.isNotNull();
			assertThat(value.isJsonNull())
					.as("required path %s is non-null in %s", path, RESOURCE_PATH)
					.isFalse();
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
				assertThat(value.getAsString().trim())
						.as("required string path %s is non-blank in %s", path, RESOURCE_PATH)
						.isNotEmpty();
			}
		}

		Set<String> unknownTopLevelKeys = root.keySet().stream()
				.filter(key -> !Set.of("xp", "combat", "hud").contains(key))
				.collect(LinkedHashSet::new, Set::add, Set::addAll);

		if (!unknownTopLevelKeys.isEmpty()) {
			reporter.publishEntry(
					"config-schema-warning",
					"Unknown top-level section(s) in " + RESOURCE_PATH + ": " + String.join(", ", unknownTopLevelKeys));
		}
	}

	private static JsonObject loadJsonObject(String resourcePath) throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
			assertThat(input)
					.as("resource exists: %s", resourcePath)
					.isNotNull();

			try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
				JsonElement parsed = JsonParser.parseReader(reader);
				assertThat(parsed != null && parsed.isJsonObject())
						.as("resource root is object: %s", resourcePath)
						.isTrue();
				return parsed.getAsJsonObject();
			}
		}
	}

	private static JsonElement valueAt(JsonObject root, String dottedPath) {
		JsonObject currentObject = root;
		String[] segments = dottedPath.split("\\.");
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			JsonElement element = currentObject.get(segment);
			if (element == null) {
				return null;
			}
			if (i == segments.length - 1) {
				return element;
			}
			if (!element.isJsonObject()) {
				return null;
			}
			currentObject = element.getAsJsonObject();
		}
		return null;
	}
}
