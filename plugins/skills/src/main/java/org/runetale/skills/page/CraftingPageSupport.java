package org.runetale.skills.page;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.SmithingMaterialTier;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

final class CraftingPageSupport {

	private CraftingPageSupport() {
	}

	static void bindTierTabs(@Nonnull UIEventBuilder eventBuilder) {
		for (SmithingMaterialTier tier : SmithingMaterialTier.values()) {
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					"#Tier" + tier.getDisplayName(),
					EventData.of("Tier", tier.name()),
					false);
		}
	}

	@Nullable
	static SmithingMaterialTier parseTier(@Nullable String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return SmithingMaterialTier.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	static void initializeBenchBinding(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull BlockPosition blockPosition,
			@Nonnull HytaleLogger logger,
			@Nonnull String contextName) {
		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		if (craftingManager == null) {
			logger.atWarning().log("Cannot initialize %s bench binding: missing CraftingManager component", contextName);
			return;
		}

		if (craftingManager.hasBenchSet()) {
			craftingManager.clearBench(ref, store);
		}

		World world = store.getExternalData().getWorld();
		BlockType blockType = world.getBlockType(blockPosition.x, blockPosition.y, blockPosition.z);
		if (blockType == null || blockType.getBench() == null) {
			logger.atWarning().log(
					"Cannot initialize %s bench binding: invalid block at %d,%d,%d",
					contextName,
					blockPosition.x,
					blockPosition.y,
					blockPosition.z);
			return;
		}

		try {
			craftingManager.setBench(blockPosition.x, blockPosition.y, blockPosition.z, blockType);
		} catch (IllegalArgumentException e) {
			logger.atWarning().withCause(e).log("Failed to bind %s page to crafting bench", contextName);
		}
	}

	static void clearBenchBinding(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		if (craftingManager != null && craftingManager.hasBenchSet()) {
			craftingManager.clearBench(ref, store);
		}
	}

	static boolean executeCraft(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nullable String selectedRecipeId,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService,
			@Nonnull HytaleLogger logger,
			@Nonnull String craftVerb,
			@Nonnull String contextName) {
		if (selectedRecipeId == null) {
			return false;
		}

		CraftingRecipe recipe = CraftingRecipe.getAssetMap().getAsset(selectedRecipeId);
		if (recipe == null) {
			logger.atWarning().log("Selected recipe not found: %s", selectedRecipeId);
			return false;
		}

		PlayerSkillProfileComponent profile = store.getComponent(ref, profileComponentType);
		if (profile == null) {
			return false;
		}

		List<SkillRequirement> requirements = craftingRecipeTagService.getSkillRequirements(recipe);
		PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
		int requiredLevel = getSmithingRequiredLevel(requirements);
		int playerLevel = profile.getLevel(SkillType.SMITHING);
		if (playerLevel < requiredLevel) {
			if (playerRef != null) {
				NotificationUtil.sendNotification(
						playerRef.getPacketHandler(),
						Message.raw("[Skills] You need Smithing level " + requiredLevel),
						NotificationStyle.Danger);
			}
			return false;
		}

		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		Player player = store.getComponent(ref, Player.getComponentType());
		if (craftingManager == null || player == null) {
			logger.atWarning().log("Cannot %s %s because crafting context is unavailable", contextName, recipe.getId());
			return false;
		}

		boolean crafted = craftingManager.craftItem(
				ref,
				store,
				recipe,
				1,
				player.getInventory().getCombinedBackpackStorageHotbar());
		if (crafted && playerRef != null) {
			NotificationUtil.sendNotification(
					playerRef.getPacketHandler(),
					Message.raw("[Skills] " + craftVerb + " " + getRecipeOutputName(recipe)),
					NotificationStyle.Default);
		}

		return crafted;
	}

	@Nonnull
	static String getRecipeOutputName(@Nonnull CraftingRecipe recipe) {
		String itemId = getPrimaryOutputItemId(recipe);
		if (itemId != null) {
			return formatItemId(itemId);
		}
		return recipe.getId();
	}

	@Nullable
	static String getPrimaryOutputItemId(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] outputs = recipe.getOutputs();
		if (outputs == null || outputs.length == 0) {
			return null;
		}

		String itemId = outputs[0].getItemId();
		if (itemId == null || itemId.isBlank()) {
			return null;
		}
		return itemId;
	}

	static int getPrimaryOutputQuantity(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] outputs = recipe.getOutputs();
		if (outputs == null || outputs.length == 0) {
			return 1;
		}
		return Math.max(1, outputs[0].getQuantity());
	}

	@Nonnull
	static String formatIngredients(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] inputs = recipe.getInput();
		if (inputs == null || inputs.length == 0) {
			return "No ingredients";
		}

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < inputs.length; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			String itemId = inputs[i].getItemId();
			int quantity = inputs[i].getQuantity();
			String name = itemId != null ? formatItemId(itemId) : "Unknown";
			if (quantity > 1) {
				builder.append(quantity).append("x ");
			}
			builder.append(name);
		}
		return builder.toString();
	}

	@Nonnull
	static String getXpText(@Nonnull CraftingRecipe recipe, @Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		return craftingRecipeTagService.getXpOnSuccessfulCraft(recipe)
				.map(xp -> "+" + String.format(Locale.ROOT, "%.1f", xp) + " XP")
				.orElse("");
	}

	static int getSmithingRequiredLevel(@Nonnull List<SkillRequirement> requirements) {
		for (SkillRequirement req : requirements) {
			if (req.skillType() == SkillType.SMITHING) {
				return req.requiredLevel();
			}
		}
		return 1;
	}

	static boolean updateCraftButtonLabel(@Nonnull UICommandBuilder commandBuilder, @Nullable String selectedRecipeId) {
		if (selectedRecipeId != null) {
			CraftingRecipe selectedRecipe = CraftingRecipe.getAssetMap().getAsset(selectedRecipeId);
			if (selectedRecipe != null) {
				commandBuilder.set("#StartCraftingButton.Text", "Craft " + getRecipeOutputName(selectedRecipe));
				return true;
			}
		}

		commandBuilder.set("#StartCraftingButton.Text", "Select a Recipe");
		return false;
	}

	@Nonnull
	private static String formatItemId(@Nonnull String itemId) {
		int colonIndex = itemId.lastIndexOf(':');
		String name = colonIndex >= 0 ? itemId.substring(colonIndex + 1) : itemId;
		return name.replace('_', ' ');
	}
}
