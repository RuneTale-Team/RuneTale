package org.runetale.skills.service;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.runetale.skills.config.ToolingConfig;
import org.runetale.skills.domain.RequirementCheckResult;
import org.runetale.skills.domain.ToolTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Evaluates held-tool constraints from break-event item-in-hand data.
 */
public class ToolRequirementEvaluator {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
	private final ToolingConfig toolingConfig;

	public ToolRequirementEvaluator(@Nonnull ToolingConfig toolingConfig) {
		this.toolingConfig = toolingConfig;
	}

	/**
	 * Validates required tool keyword and minimum tier from the held item.
	 */
	@Nonnull
	public RequirementCheckResult evaluate(@Nullable ItemStack heldItem, @Nonnull String requiredToolKeyword,
			@Nonnull ToolTier minimumTier) {
		if (heldItem == null || heldItem.isEmpty()) {
			LOGGER.atFine().log("Tool requirement failed: no held item");
			return RequirementCheckResult.failure(ToolTier.NONE, "<empty>");
		}

		String itemId = heldItem.getItemId();
		String normalizedItemId = normalizeToken(itemId);
		String normalizedKeyword = normalizeToken(requiredToolKeyword);

		if (!matchesToolFamily(normalizedItemId, normalizedKeyword)) {
			LOGGER.atFine().log("Tool requirement failed: item=%s missing keyword=%s", itemId, requiredToolKeyword);
			return RequirementCheckResult.failure(ToolTier.NONE, itemId);
		}

		ToolTier detected = detectTier(normalizedItemId);
		boolean success = detected.rank() >= minimumTier.rank();
		LOGGER.atFine().log("Tool requirement check: item=%s detected=%s required=%s success=%s",
				itemId, detected, minimumTier, success);
		return success ? RequirementCheckResult.success(detected, itemId)
				: RequirementCheckResult.failure(detected, itemId);
	}

	private boolean matchesToolFamily(@Nonnull String normalizedItemId, @Nonnull String normalizedKeyword) {
		return this.toolingConfig.matchesToolFamily(normalizedItemId, normalizedKeyword);
	}

	@Nonnull
	private ToolTier detectTier(@Nonnull String normalizedItemId) {
		return this.toolingConfig.detectTier(normalizedItemId);
	}

	@Nonnull
	private static String normalizeToken(@Nullable String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}

		String lowered = raw.toLowerCase(Locale.ROOT);
		String normalized = NON_ALNUM.matcher(lowered).replaceAll("_");
		normalized = normalized.replaceAll("_+", "_");
		if (normalized.startsWith("_")) {
			normalized = normalized.substring(1);
		}
		if (normalized.endsWith("_")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}
}
