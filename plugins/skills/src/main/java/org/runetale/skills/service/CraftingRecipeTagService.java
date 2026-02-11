package org.runetale.skills.service;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
	 * Stateless utility that extracts custom skill tags from crafted output items.
 *
	 * <p>
	 * Output item assets define skill integration through Tags in their asset JSON:
	 * {@code XpOnSuccessfulCraft}, {@code SkillsRequired}, {@code SkillLevelsRequired}.
	 */
public class CraftingRecipeTagService {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String TAG_XP_ON_SUCCESSFUL_CRAFT = "XpOnSuccessfulCraft";
	private static final String TAG_SKILLS_REQUIRED = "SkillsRequired";
	private static final String TAG_SKILL_LEVELS_REQUIRED = "SkillLevelsRequired";

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
	 * Returns the list of skill requirements for this recipe, parsed from the
	 * positionally-mapped {@code SkillsRequired} and {@code SkillLevelsRequired}
	 * tag arrays.
	 *
	 * <p>
	 * Entries with invalid skill names are skipped with a warning. Missing or
	 * unparseable level entries default to 1. Returns an empty list when no
	 * skills are configured.
	 */
	@Nonnull
	public List<SkillRequirement> getSkillRequirements(@Nonnull CraftingRecipe recipe) {
		String[] skillNames = getRawTagValues(recipe, TAG_SKILLS_REQUIRED);
		if (skillNames == null || skillNames.length == 0) {
			return Collections.emptyList();
		}

		String[] levelStrings = getRawTagValues(recipe, TAG_SKILL_LEVELS_REQUIRED);

		List<SkillRequirement> requirements = new ArrayList<>(skillNames.length);
		for (int i = 0; i < skillNames.length; i++) {
			SkillType skill = SkillType.tryParseStrict(skillNames[i]);
			if (skill == null) {
				LOGGER.atWarning().log("Invalid %s entry '%s' at index %d on recipe %s; skipping",
						TAG_SKILLS_REQUIRED, skillNames[i], i, recipe.getId());
				continue;
			}

			int level = 1;
			if (levelStrings != null && i < levelStrings.length) {
				try {
					level = Integer.parseInt(levelStrings[i].trim());
				} catch (NumberFormatException e) {
					LOGGER.atWarning().log("Invalid %s entry '%s' at index %d on recipe %s; defaulting to 1",
							TAG_SKILL_LEVELS_REQUIRED, levelStrings[i], i, recipe.getId());
				}
			}

			requirements.add(new SkillRequirement(skill, level));
		}

		return requirements;
	}

	@Nullable
	private String[] getRawTagValues(@Nonnull CraftingRecipe recipe, @Nonnull String tagKey) {
		Map<String, String[]> rawTags = getOutputItemRawTags(recipe);
		if (rawTags == null) {
			return null;
		}

		return rawTags.get(tagKey);
	}

	@Nullable
	private Map<String, String[]> getOutputItemRawTags(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity primaryOutput = recipe.getPrimaryOutput();
		if (primaryOutput == null) {
			return null;
		}

		String itemId = primaryOutput.getItemId();
		if (itemId == null || itemId.isBlank()) {
			return null;
		}

		Item outputItem = Item.getAssetMap().getAsset(itemId);
		if (outputItem == null) {
			LOGGER.atFine().log("Output item asset not found for recipe %s: %s", recipe.getId(), itemId);
			return null;
		}

		AssetExtraInfo.Data data = outputItem.getData();
		if (data == null) {
			return null;
		}

		return data.getRawTags();
	}
}
