package org.runetale.skills.service;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.skills.config.ToolingConfig;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.ToolTier;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolRequirementEvaluatorTest {

	@Test
	void evaluateAcceptsMatchingToolFamilyAtRequiredTier(@TempDir Path tempDir) {
		ToolRequirementEvaluator evaluator = new ToolRequirementEvaluator(ToolingConfig.load(tempDir));
		ItemStack heldItem = mockHeldItem("Tool_Hatchet_Bronze");

		RequirementCheckResult result = evaluator.evaluate(heldItem, "Tool_Hatchet", ToolTier.BRONZE);

		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getDetectedTier()).isEqualTo(ToolTier.BRONZE);
		assertThat(result.getHeldItemId()).isEqualTo("Tool_Hatchet_Bronze");
	}

	@Test
	void evaluateRejectsInsufficientTierWithinMatchingFamily(@TempDir Path tempDir) {
		ToolRequirementEvaluator evaluator = new ToolRequirementEvaluator(ToolingConfig.load(tempDir));
		ItemStack heldItem = mockHeldItem("Tool_Hatchet_Bronze");

		RequirementCheckResult result = evaluator.evaluate(heldItem, "Tool_Hatchet", ToolTier.IRON);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getDetectedTier()).isEqualTo(ToolTier.BRONZE);
	}

	@Test
	void evaluateRejectsNonMatchingToolFamily(@TempDir Path tempDir) {
		ToolRequirementEvaluator evaluator = new ToolRequirementEvaluator(ToolingConfig.load(tempDir));
		ItemStack heldItem = mockHeldItem("Tool_Pickaxe_Bronze");

		RequirementCheckResult result = evaluator.evaluate(heldItem, "Tool_Hatchet", ToolTier.NONE);

		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getDetectedTier()).isEqualTo(ToolTier.NONE);
		assertThat(result.getHeldItemId()).isEqualTo("Tool_Pickaxe_Bronze");
	}

	private static ItemStack mockHeldItem(String itemId) {
		ItemStack heldItem = mock(ItemStack.class);
		when(heldItem.isEmpty()).thenReturn(false);
		when(heldItem.getItemId()).thenReturn(itemId);
		return heldItem;
	}
}
