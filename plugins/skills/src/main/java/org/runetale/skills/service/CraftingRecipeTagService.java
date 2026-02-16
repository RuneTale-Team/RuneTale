package org.runetale.skills.service;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
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
	 * {@code XpOnSuccessfulCraft}, {@code CraftingLevelRequirement}.
	 */
public class CraftingRecipeTagService {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String TAG_XP_ON_SUCCESSFUL_CRAFT = "XpOnSuccessfulCraft";
	private static final String TAG_SKILLS_REQUIRED = "SkillsRequired";
	private static final String TAG_SKILL_LEVELS_REQUIRED = "CraftingLevelRequirement";
	private static final String TAG_CRAFTING_LEVEL_REQUIRED = "CraftingLevelRequirement";
	private static final String RUNETALE_ANVIL_BENCH_ID = "RuneTale_Anvil";
	private static final String RUNETALE_FURNACE_BENCH_ID = "RuneTale_Furnace";

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
			Integer craftingLevelRequired = parseCraftingLevelRequired(recipe);
			if (craftingLevelRequired != null) {
				return List.of(new SkillRequirement(SkillType.SMITHING, craftingLevelRequired));
			}

			if (isRuneTaleSmithingBenchRecipe(recipe)) {
				return List.of(new SkillRequirement(SkillType.SMITHING, 1));
			}
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

		Integer craftingLevelRequired = parseCraftingLevelRequired(recipe);
		if (craftingLevelRequired != null && requirements.stream().noneMatch(req -> req.skillType() == SkillType.SMITHING)) {
			requirements.add(new SkillRequirement(SkillType.SMITHING, craftingLevelRequired));
		}

		return requirements;
	}

	@Nullable
	private Integer parseCraftingLevelRequired(@Nonnull CraftingRecipe recipe) {
		String[] values = getRawTagValues(recipe, TAG_CRAFTING_LEVEL_REQUIRED);
		if (values == null || values.length == 0) {
			return null;
		}

		try {
			return Math.max(1, Integer.parseInt(values[0].trim()));
		} catch (NumberFormatException e) {
			LOGGER.atWarning().log("Invalid %s value '%s' on recipe %s; ignoring",
					TAG_CRAFTING_LEVEL_REQUIRED,
					values[0],
					recipe.getId());
			return null;
		}
	}

	private boolean isRuneTaleSmithingBenchRecipe(@Nonnull CraftingRecipe recipe) {
		BenchRequirement[] benchRequirements = recipe.getBenchRequirement();
		if (benchRequirements == null || benchRequirements.length == 0) {
			return false;
		}

		for (BenchRequirement benchRequirement : benchRequirements) {
			if (benchRequirement == null || benchRequirement.type != BenchType.Crafting || benchRequirement.id == null) {
				continue;
			}

			if (RUNETALE_ANVIL_BENCH_ID.equals(benchRequirement.id)
					|| RUNETALE_FURNACE_BENCH_ID.equals(benchRequirement.id)) {
				return true;
			}
		}

		return false;
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
