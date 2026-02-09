package org.runetale.skills.service;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Stateless utility that extracts custom skill tags from crafting recipe assets.
 *
 * <p>
 * Recipes define skill integration through Tags in their asset JSON:
 * {@code XpOnSuccessfulCraft}, {@code CraftingLevelRequired}, {@code SkillRequired}.
 */
public class CraftingRecipeTagService {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String TAG_XP_ON_SUCCESSFUL_CRAFT = "XpOnSuccessfulCraft";
	private static final String TAG_CRAFTING_LEVEL_REQUIRED = "CraftingLevelRequired";
	private static final String TAG_SKILL_REQUIRED = "SkillRequired";

	/**
	 * Returns the XP reward for a successful craft, if configured.
	 */
	@Nonnull
	public Optional<Double> getXpOnSuccessfulCraft(@Nonnull CraftingRecipe recipe) {
		String[] values = getRawTagValues(recipe, TAG_XP_ON_SUCCESSFUL_CRAFT);
		if (values == null || values.length == 0) {
			return Optional.empty();
		}

		try {
			double xp = Double.parseDouble(values[0].trim());
			if (xp <= 0.0) {
				return Optional.empty();
			}
			return Optional.of(xp);
		} catch (NumberFormatException e) {
			LOGGER.atWarning().log("Invalid %s value '%s' on recipe %s; ignoring",
					TAG_XP_ON_SUCCESSFUL_CRAFT, values[0], recipe.getId());
			return Optional.empty();
		}
	}

	/**
	 * Returns the minimum skill level required to unlock or benefit from
	 * this recipe. Defaults to 1 when absent or invalid.
	 */
	public int getCraftingLevelRequired(@Nonnull CraftingRecipe recipe) {
		String[] values = getRawTagValues(recipe, TAG_CRAFTING_LEVEL_REQUIRED);
		if (values == null || values.length == 0) {
			return 1;
		}

		try {
			return Math.max(1, Integer.parseInt(values[0].trim()));
		} catch (NumberFormatException e) {
			LOGGER.atWarning().log("Invalid %s value '%s' on recipe %s; defaulting to 1",
					TAG_CRAFTING_LEVEL_REQUIRED, values[0], recipe.getId());
			return 1;
		}
	}

	/**
	 * Returns the skill type this recipe is associated with, if configured.
	 */
	@Nonnull
	public Optional<SkillType> getSkillRequired(@Nonnull CraftingRecipe recipe) {
		String[] values = getRawTagValues(recipe, TAG_SKILL_REQUIRED);
		if (values == null || values.length == 0) {
			return Optional.empty();
		}

		SkillType parsed = SkillType.tryParseStrict(values[0]);
		if (parsed == null) {
			LOGGER.atWarning().log("Invalid %s value '%s' on recipe %s; ignoring",
					TAG_SKILL_REQUIRED, values[0], recipe.getId());
			return Optional.empty();
		}
		return Optional.of(parsed);
	}

	@Nullable
	private String[] getRawTagValues(@Nonnull CraftingRecipe recipe, @Nonnull String tagKey) {
		AssetExtraInfo.Data data = CraftingRecipe.CODEC.getData(recipe);
		if (data == null) {
			return null;
		}

		Map<String, String[]> rawTags = data.getRawTags();
		return rawTags.get(tagKey);
	}
}
