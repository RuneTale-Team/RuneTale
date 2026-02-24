package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;
import org.runetale.testing.junit.ContractTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class SkillNodeLookupServiceContractTest {

	@Test
	void initializeDefaultsLoadsNodeMappingsFromResources() {
		SkillNodeLookupService service = new SkillNodeLookupService();

		service.initializeDefaults();

		List<SkillNodeDefinition> definitions = service.listAllDefinitions();
		assertThat(definitions).isNotEmpty();
		assertThat(definitions).allSatisfy(definition -> {
			assertThat(definition.getId()).isNotBlank();
			assertThat(definition.getSkillType()).isNotNull();
			assertThat(definition.getBlockId()).isNotBlank();
			assertThat(service.findByBlockId(definition.getBlockId())).isNotNull();
		});
	}

	@Test
	void wildcardLookupMatchesNamespacedBlockIdsUsingSuffixSimplification() {
		SkillNodeLookupService service = new SkillNodeLookupService();
		SkillNodeDefinition wildcard = new SkillNodeDefinition(
				"wildcard_lookup_node",
				SkillType.MINING,
				"Ore_Test_*",
				1,
				ToolTier.NONE,
				"Tool_Pickaxe",
				7.0D);
		service.register(wildcard, List.of("Ore_Test_*"));

		SkillNodeDefinition node = service.findByBlockId("mymod:Ore_Test_Surface_A");

		assertThat(node).isNotNull();
		assertThat(node.getId()).isEqualTo("wildcard_lookup_node");
		assertThat(node.getSkillType()).isEqualTo(SkillType.MINING);
	}

	@Test
	void listDefinitionsForSkillReturnsOnlyRequestedSkill() {
		SkillNodeLookupService service = new SkillNodeLookupService();
		SkillNodeDefinition mining = new SkillNodeDefinition(
				"mining_custom",
				SkillType.MINING,
				"Ore_Custom",
				1,
				ToolTier.NONE,
				"Tool_Pickaxe",
				10.0D);
		SkillNodeDefinition woodcutting = new SkillNodeDefinition(
				"woodcutting_custom",
				SkillType.WOODCUTTING,
				"Tree_Custom",
				1,
				ToolTier.NONE,
				"Tool_Hatchet",
				10.0D);
		service.register(mining);
		service.register(woodcutting);

		List<SkillNodeDefinition> miningDefinitions = service.listDefinitionsForSkill(SkillType.MINING);

		assertThat(miningDefinitions).isNotEmpty();
		assertThat(miningDefinitions).anySatisfy(definition -> assertThat(definition.getId()).isEqualTo("mining_custom"));
		assertThat(miningDefinitions).allMatch(definition -> definition.getSkillType() == SkillType.MINING);
	}

	@Test
	void exactMappingTakesPrecedenceOverWildcardMapping() {
		SkillNodeLookupService service = new SkillNodeLookupService();
		SkillNodeDefinition wildcard = new SkillNodeDefinition(
				"wildcard_node",
				SkillType.MINING,
				"Ore_Copper_*",
				1,
				ToolTier.NONE,
				"Tool_Pickaxe",
				7.0D);
		SkillNodeDefinition exact = new SkillNodeDefinition(
				"exact_node",
				SkillType.MINING,
				"mymod:Ore_Copper_Surface_A",
				99,
				ToolTier.MITHRIL,
				"Tool_Pickaxe",
				50.0D);

		service.register(wildcard, List.of("Ore_Copper_*"));
		service.register(exact, List.of("mymod:Ore_Copper_Surface_A"));

		SkillNodeDefinition resolved = service.findByBlockId("mymod:Ore_Copper_Surface_A");

		assertThat(resolved).isNotNull();
		assertThat(resolved.getId()).isEqualTo("exact_node");
	}

	@Test
	void registerReplacesExistingBlockMapping() {
		SkillNodeLookupService service = new SkillNodeLookupService();
		SkillNodeDefinition first = new SkillNodeDefinition("first_node", SkillType.WOODCUTTING, "Custom_Block", 1,
				ToolTier.NONE, "Tool_Hatchet", 10.0D);
		SkillNodeDefinition second = new SkillNodeDefinition("second_node", SkillType.MINING, "Custom_Block", 20,
				ToolTier.COPPER, "Tool_Pickaxe", 20.0D);

		service.register(first);
		service.register(second);

		SkillNodeDefinition resolved = service.findByBlockId("custom_block");
		assertThat(resolved).isNotNull();
		assertThat(resolved.getId()).isEqualTo("second_node");
		assertThat(resolved.getSkillType()).isEqualTo(SkillType.MINING);
	}
}
