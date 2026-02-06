package org.runetale.skills.service;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.ToolTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Evaluates held-tool constraints from break-event item-in-hand data.
 */
public class ToolRequirementEvaluator {

	private static final Logger LOGGER = Logger.getLogger(ToolRequirementEvaluator.class.getName());

	/**
	 * Validates required tool keyword and minimum tier from the held item.
	 */
	@Nonnull
	public RequirementCheckResult evaluate(@Nullable ItemStack heldItem, @Nonnull String requiredToolKeyword,
			@Nonnull ToolTier minimumTier) {
		if (heldItem == null || heldItem.isEmpty()) {
			LOGGER.log(Level.FINE, "Tool requirement failed: no held item");
			return RequirementCheckResult.failure(ToolTier.NONE, "<empty>");
		}

		String itemId = heldItem.getItemId();
		String normalized = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
		String keyword = requiredToolKeyword.toLowerCase(Locale.ROOT);

		// Enforce strict tool family requirement first (e.g., contains 'axe').
		if (!normalized.contains(keyword)) {
			LOGGER.log(Level.FINE,
					String.format("Tool requirement failed: item=%s missing keyword=%s", itemId, requiredToolKeyword));
			return RequirementCheckResult.failure(ToolTier.NONE, itemId);
		}

		ToolTier detected = detectTier(normalized);
		boolean success = detected.rank() >= minimumTier.rank();
		LOGGER.log(Level.FINE, String.format("Tool requirement check: item=%s detected=%s required=%s success=%s",
				itemId, detected, minimumTier, success));
		return success ? RequirementCheckResult.success(detected, itemId)
				: RequirementCheckResult.failure(detected, itemId);
	}

	@Nonnull
	private ToolTier detectTier(@Nonnull String normalizedItemId) {
		if (normalizedItemId.contains("crystal"))
			return ToolTier.CRYSTAL;
		if (normalizedItemId.contains("dragon"))
			return ToolTier.DRAGON;
		if (normalizedItemId.contains("rune"))
			return ToolTier.RUNE;
		if (normalizedItemId.contains("adamant"))
			return ToolTier.ADAMANT;
		if (normalizedItemId.contains("mithril"))
			return ToolTier.MITHRIL;
		if (normalizedItemId.contains("steel"))
			return ToolTier.STEEL;
		if (normalizedItemId.contains("iron"))
			return ToolTier.IRON;
		if (normalizedItemId.contains("bronze"))
			return ToolTier.BRONZE;
		return ToolTier.NONE;
	}
}
