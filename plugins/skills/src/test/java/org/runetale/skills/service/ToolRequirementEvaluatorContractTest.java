package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.ToolTier;
import org.runetale.testing.junit.ContractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContractTest
class ToolRequirementEvaluatorContractTest {

	@Test
	void evaluateAcceptsMatchingToolFamilyAtRequiredTier() {
		ToolRequirementEvaluator evaluator = new ToolRequirementEvaluator();
		var heldItem = mockHeldItem("Tool_Hatchet_Copper");

		RequirementCheckResult result = evaluator.evaluate(heldItem, "Tool_Hatchet", ToolTier.COPPER);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getDetectedTier()).isEqualTo(ToolTier.COPPER);
		assertThat(result.getHeldItemId()).isEqualTo("Tool_Hatchet_Copper");
	}

	@Test
	void evaluateRejectsInsufficientTierWithinMatchingFamily() {
		ToolRequirementEvaluator evaluator = new ToolRequirementEvaluator();
		var heldItem = mockHeldItem("Tool_Hatchet_Wood");

		RequirementCheckResult result = evaluator.evaluate(heldItem, "Tool_Hatchet", ToolTier.IRON);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getDetectedTier()).isEqualTo(ToolTier.WOOD);
	}

	@Test
	void evaluateRejectsNonMatchingToolFamily() {
		ToolRequirementEvaluator evaluator = new ToolRequirementEvaluator();
		var heldItem = mockHeldItem("Tool_Pickaxe_Copper");

		RequirementCheckResult result = evaluator.evaluate(heldItem, "Tool_Hatchet", ToolTier.NONE);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getDetectedTier()).isEqualTo(ToolTier.NONE);
		assertThat(result.getHeldItemId()).isEqualTo("Tool_Pickaxe_Copper");
	}

	private static com.hypixel.hytale.server.core.inventory.ItemStack mockHeldItem(String itemId) {
		com.hypixel.hytale.server.core.inventory.ItemStack heldItem = mock(com.hypixel.hytale.server.core.inventory.ItemStack.class);
		when(heldItem.isEmpty()).thenReturn(false);
		when(heldItem.getItemId()).thenReturn(itemId);
		return heldItem;
	}
}
