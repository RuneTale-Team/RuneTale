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

		SkillNodeDefinition oak = service.findByBlockId("  wood_oak_trunk  ");
		assertThat(oak).isNotNull();
		assertThat(oak.getId()).isEqualTo("woodcutting_oak_tree");
		assertThat(oak.getSkillType()).isEqualTo(SkillType.WOODCUTTING);
		assertThat(oak.getRequiredToolTier()).isEqualTo(ToolTier.NONE);
	}

	@Test
	void wildcardLookupMatchesNamespacedBlockIdsUsingSuffixSimplification() {
		SkillNodeLookupService service = new SkillNodeLookupService();
		service.initializeDefaults();

		SkillNodeDefinition node = service.findByBlockId("mymod:Ore_Copper_Surface_A");

		assertThat(node).isNotNull();
		assertThat(node.getId()).isEqualTo("mining_copper");
		assertThat(node.getSkillType()).isEqualTo(SkillType.MINING);
	}

	@Test
	void listDefinitionsForSkillReturnsOnlyRequestedSkill() {
		SkillNodeLookupService service = new SkillNodeLookupService();
		service.initializeDefaults();

		List<SkillNodeDefinition> miningDefinitions = service.listDefinitionsForSkill(SkillType.MINING);

		assertThat(miningDefinitions).isNotEmpty();
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
