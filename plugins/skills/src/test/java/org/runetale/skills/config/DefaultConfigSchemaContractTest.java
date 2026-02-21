package org.runetale.skills.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class DefaultConfigSchemaContractTest {

	private static final List<ConfigSchema> CONFIG_SCHEMAS = List.of(
			ConfigSchema.exact(
					"Skills/Config/xp.properties",
					Set.of("maxLevel", "levelTermMultiplier", "growthScale", "growthBase", "growthDivisor",
							"pointsDivisor", "roundingMode")),
			ConfigSchema.exact(
					"Skills/Config/combat.properties",
					Set.of("xpPerDamage", "source.ranged", "source.melee.prefix", "source.melee.accurate",
							"source.melee.aggressive", "source.melee.defensive", "source.melee.controlled.attack",
							"source.melee.controlled.strength", "source.melee.controlled.defence",
							"source.block.defence", "projectileCauseTokens")),
			ConfigSchema.exact(
					"Skills/Config/hud.properties",
					Set.of("toast.durationMillis", "toast.fadeDurationMillis", "toast.fade.rootBackground",
							"toast.fade.innerBackground", "toast.expiryTickSeconds")));

	@Test
	void defaultConfigFilesHaveRequiredKeysAndWarnOnUnknown(TestReporter reporter) throws IOException {
		for (ConfigSchema schema : CONFIG_SCHEMAS) {
			Properties properties = loadProperties(schema.resourcePath());
			Set<String> presentKeys = properties.stringPropertyNames();

			assertThat(presentKeys)
					.as("required keys present in %s", schema.resourcePath())
					.containsAll(schema.requiredExactKeys());

			for (String requiredKey : schema.requiredExactKeys()) {
				String rawValue = properties.getProperty(requiredKey);
				assertThat(rawValue)
						.as("required key %s in %s", requiredKey, schema.resourcePath())
						.isNotNull();
				assertThat(rawValue.trim())
						.as("required key %s is non-blank in %s", requiredKey, schema.resourcePath())
						.isNotEmpty();
			}

			for (String requiredPrefix : schema.requiredPrefixes()) {
				assertThat(presentKeys.stream().anyMatch(key -> key.startsWith(requiredPrefix)))
						.as("required prefix %s is present in %s", requiredPrefix, schema.resourcePath())
						.isTrue();
			}

			Set<String> allowedExactKeys = new LinkedHashSet<>(schema.requiredExactKeys());
			allowedExactKeys.addAll(schema.allowedExactKeys());

			Set<String> unknownKeys = presentKeys.stream()
					.filter(key -> !allowedExactKeys.contains(key))
					.filter(key -> schema.allowedPrefixes().stream().noneMatch(key::startsWith))
					.collect(LinkedHashSet::new, Set::add, Set::addAll);

			if (!unknownKeys.isEmpty()) {
				reporter.publishEntry(
						"config-schema-warning",
						"Unknown key(s) in " + schema.resourcePath() + ": " + String.join(", ", unknownKeys));
			}
		}
	}

	private static Properties loadProperties(String resourcePath) throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
			assertThat(input)
					.as("resource exists: %s", resourcePath)
					.isNotNull();

			Properties properties = new Properties();
			properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
			return properties;
		}
	}

	private record ConfigSchema(
			String resourcePath,
			Set<String> requiredExactKeys,
			Set<String> allowedExactKeys,
			List<String> requiredPrefixes,
			List<String> allowedPrefixes) {

		private static ConfigSchema exact(String resourcePath, Set<String> requiredExactKeys) {
			return new ConfigSchema(resourcePath, Set.copyOf(requiredExactKeys), Set.of(), List.of(), List.of());
		}

	}
}
