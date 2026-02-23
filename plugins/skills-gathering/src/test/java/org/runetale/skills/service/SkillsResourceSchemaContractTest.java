package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.config.HeuristicsConfig;
import org.runetale.skills.config.ToolingConfig;
import org.runetale.testing.junit.ContractTest;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class SkillsResourceSchemaContractTest {

	@Test
	void requiredConfigResourcesResolveFromClasspath() {
		List<String> resources = List.of(
				"Skills/Config/gathering.json",
				"Skills/Nodes/nodes.json");

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (String resource : resources) {
			assertThat(classLoader.getResource(resource))
					.as("resource %s", resource)
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

	@Test
	void gatheringConfigSlicesLoadWithoutRuntimeContext() {
		ToolingConfig toolingConfig = ToolingConfig.load(Path.of("./non-existent-external-root"));
		HeuristicsConfig heuristicsConfig = HeuristicsConfig.load(Path.of("./non-existent-external-root"));

		assertThat(toolingConfig.defaultKeyword()).isNotBlank();
		assertThat(heuristicsConfig.nodeCandidateTokens()).isNotEmpty();
	}
}
