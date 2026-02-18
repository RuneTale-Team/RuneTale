package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.testing.junit.ContractTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class SkillsResourceSchemaContractTest {

	@Test
	void nodeIndexEntriesResolveToClasspathResources() throws IOException {
		List<String> entries = readNodeIndexEntries();

		assertThat(entries).isNotEmpty();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (String entry : entries) {
			assertThat(classLoader.getResource("Skills/Nodes/" + entry))
					.as("resource Skills/Nodes/%s", entry)
					.isNotNull();
		}
	}

	@Test
	void initializedNodeDefinitionsHaveRequiredFieldsAndUniqueIds() {
		SkillNodeLookupService service = new SkillNodeLookupService();
		service.initializeDefaults();

		List<SkillNodeDefinition> definitions = service.listAllDefinitions();

		assertThat(definitions).isNotEmpty();
		assertThat(definitions).allSatisfy(definition -> {
			assertThat(definition.getId()).isNotBlank();
			assertThat(definition.getSkillType()).isNotNull();
			assertThat(definition.getBlockId()).isNotBlank();
			assertThat(definition.getRequiredSkillLevel()).isGreaterThanOrEqualTo(0);
			assertThat(definition.getExperienceReward()).isGreaterThanOrEqualTo(0.0D);
		});

		Set<String> ids = definitions.stream().map(SkillNodeDefinition::getId).collect(Collectors.toSet());
		assertThat(ids).hasSameSizeAs(definitions);
		assertThat(service.findByBlockId("mymod:Ore_Copper_Surface_A")).isNotNull();
	}

	private static List<String> readNodeIndexEntries() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = classLoader.getResourceAsStream("Skills/Nodes/index.list")) {
			assertThat(input).isNotNull();

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
}
