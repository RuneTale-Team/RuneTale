package org.runetale.skills.service;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import org.junit.jupiter.api.Test;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.ToolTier;
import org.runetale.testing.junit.ContractTest;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class SkillNodeBreakResolutionServiceContractTest {

	@Test
	void resolveMissingNodeNoopsForNonCandidateBlocks() {
		SkillNodeBreakResolutionService service = new SkillNodeBreakResolutionService();

		SkillNodeBreakResolutionResult result = service.resolveMissingNode("Sand");

		assertThat(result.shouldCancelBreak()).isFalse();
		assertThat(result.shouldNotifyPlayer()).isFalse();
		assertThat(result.shouldDispatchXp()).isFalse();
	}

	@Test
	void resolveMissingNodeWarnsForSkillNodeCandidates() {
		SkillNodeBreakResolutionService service = new SkillNodeBreakResolutionService();

		SkillNodeBreakResolutionResult result = service.resolveMissingNode("Ore_Mystery");

		assertThat(result.shouldCancelBreak()).isTrue();
		assertThat(result.shouldNotifyPlayer()).isTrue();
		assertThat(result.getNotificationStyle()).isEqualTo(NotificationStyle.Warning);
		assertThat(result.getPlayerMessage()).contains("not configured");
	}

	@Test
	void resolveConfiguredNodeWarnsWhenLevelIsInsufficient() {
		SkillNodeBreakResolutionService service = new SkillNodeBreakResolutionService();
		SkillNodeDefinition node = new SkillNodeDefinition("test_node", SkillType.MINING, "Ore_Test", 10,
				ToolTier.NONE, "Tool_Pickaxe", 12.0D);

		SkillNodeBreakResolutionResult result = service.resolveConfiguredNode(
				node,
				5,
				RequirementCheckResult.success(ToolTier.IRON, "Tool_Pickaxe_Iron"));

		assertThat(result.shouldCancelBreak()).isTrue();
		assertThat(result.shouldNotifyPlayer()).isTrue();
		assertThat(result.shouldDispatchXp()).isFalse();
		assertThat(result.getPlayerMessage()).contains("Mining");
	}

	@Test
	void resolveConfiguredNodeNoopsWhenToolRequirementFails() {
		SkillNodeBreakResolutionService service = new SkillNodeBreakResolutionService();
		SkillNodeDefinition node = new SkillNodeDefinition("test_node", SkillType.WOODCUTTING, "Wood_Test", 1,
				ToolTier.COPPER, "Tool_Hatchet", 7.0D);

		SkillNodeBreakResolutionResult result = service.resolveConfiguredNode(
				node,
				20,
				RequirementCheckResult.failure(ToolTier.WOOD, "Tool_Hatchet_Wood"));

		assertThat(result.shouldCancelBreak()).isFalse();
		assertThat(result.shouldNotifyPlayer()).isFalse();
		assertThat(result.shouldDispatchXp()).isFalse();
	}

	@Test
	void resolveConfiguredNodeDispatchesXpWhenAllRequirementsPass() {
		SkillNodeBreakResolutionService service = new SkillNodeBreakResolutionService();
		SkillNodeDefinition node = new SkillNodeDefinition("test_node", SkillType.WOODCUTTING, "Wood_Test", 1,
				ToolTier.NONE, "Tool_Hatchet", 9.5D);

		SkillNodeBreakResolutionResult result = service.resolveConfiguredNode(
				node,
				20,
				RequirementCheckResult.success(ToolTier.COPPER, "Tool_Hatchet_Copper"));

		assertThat(result.shouldCancelBreak()).isFalse();
		assertThat(result.shouldNotifyPlayer()).isFalse();
		assertThat(result.shouldDispatchXp()).isTrue();
		assertThat(result.getSkillType()).isEqualTo(SkillType.WOODCUTTING);
		assertThat(result.getExperience()).isEqualTo(9.5D);
		assertThat(result.getSourceTag()).isEqualTo("node:test_node");
	}
}
