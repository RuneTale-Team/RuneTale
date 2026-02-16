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
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
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
	private static final int MAX_INGREDIENT_ICONS = 3;

	private CraftingPageSupport() {
	}

	static void bindTierTabs(@Nonnull UIEventBuilder eventBuilder, @Nonnull List<SmithingMaterialTier> tiers) {
		for (SmithingMaterialTier tier : tiers) {
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					"#Tier" + tier.getDisplayName(),
					EventData.of("Tier", tier.name()),
					false);
		}
	}

	static void bindQuantityControls(@Nonnull UIEventBuilder eventBuilder) {
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#Qty1",
				EventData.of("Quantity", "1"),
				false);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#Qty5",
				EventData.of("Quantity", "5"),
				false);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#Qty10",
				EventData.of("Quantity", "10"),
				false);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#QtyAll",
				EventData.of("Quantity", "ALL"),
				false);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#QtyCustomApply",
				EventData.of("Action", "SetQuantity").append("@QuantityInput", "#QtyCustomInput.Value"),
				false);
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

		PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

		CraftingRecipe recipe = CraftingRecipe.getAssetMap().getAsset(selectedRecipeId);
		if (recipe == null) {
			logger.atWarning().log("Selected recipe not found: %s", selectedRecipeId);
			sendDanger(playerRef, "[Skills] Recipe not found. Re-select and try again.");
			return false;
		}

		PlayerSkillProfileComponent profile = store.getComponent(ref, profileComponentType);
		if (profile == null) {
			sendDanger(playerRef, "[Skills] Could not verify your skill profile. Try reopening the station.");
			return false;
		}

		List<SkillRequirement> requirements = craftingRecipeTagService.getSkillRequirements(recipe);
		int requiredLevel = getSmithingRequiredLevel(requirements);
		int playerLevel = profile.getLevel(SkillType.SMITHING);
		if (playerLevel < requiredLevel) {
			sendDanger(playerRef, "[Skills] You need Smithing level " + requiredLevel);
			return false;
		}

		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		Player player = store.getComponent(ref, Player.getComponentType());
		if (craftingManager == null || player == null) {
			logger.atWarning().log("Cannot %s %s because crafting context is unavailable", contextName, recipe.getId());
			sendDanger(playerRef, "[Skills] Crafting station context unavailable. Close and reopen it.");
			return false;
		}

		if (!hasRequiredMaterials(player, recipe)) {
			sendDanger(playerRef, "[Skills] Materials required.");
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
		} else if (!crafted) {
			sendDanger(playerRef, "[Skills] Craft failed. Verify recipe unlock, station, and materials.");
		}

		return crafted;
	}

	private static void sendDanger(@Nullable PlayerRef playerRef, @Nonnull String text) {
		if (playerRef == null) {
			return;
		}

		NotificationUtil.sendNotification(
				playerRef.getPacketHandler(),
				Message.raw(text),
				NotificationStyle.Danger);
	}

	static boolean hasRequiredMaterials(@Nullable Player player, @Nonnull CraftingRecipe recipe) {
		if (player == null) {
			return false;
		}

		MaterialQuantity[] inputs = recipe.getInput();
		if (inputs == null || inputs.length == 0) {
			return true;
		}

		for (MaterialQuantity input : inputs) {
			if (input == null) {
				continue;
			}
			if (!player.getInventory().getCombinedBackpackStorageHotbar().canRemoveMaterial(input, true, true)) {
				return false;
			}
		}

		return true;
	}

	static int getMaxCraftableCount(@Nullable Player player, @Nonnull CraftingRecipe recipe, int maxCap) {
		if (player == null) {
			return 0;
		}

		int cap = Math.max(1, maxCap);
		MaterialQuantity[] inputs = recipe.getInput();
		if (inputs == null || inputs.length == 0) {
			return cap;
		}

		ItemContainer container = player.getInventory().getCombinedBackpackStorageHotbar();
		int maxCraftable = cap;
		for (MaterialQuantity input : inputs) {
			if (input == null) {
				continue;
			}
			int byInput = maxCraftableForMaterial(container, input, cap);
			maxCraftable = Math.min(maxCraftable, byInput);
			if (maxCraftable <= 0) {
				return 0;
			}
		}

		return maxCraftable;
	}

	static int syncQuantityControls(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull TimedCraftingPageState craftingState,
			@Nullable Player player,
			@Nullable String selectedRecipeId) {
		int displayedCraftCount = craftingState.getSelectedCraftQuantity();
		if (craftingState.isCraftAllSelected() && selectedRecipeId != null) {
			CraftingRecipe allRecipe = CraftingRecipe.getAssetMap().getAsset(selectedRecipeId);
			if (allRecipe != null) {
				displayedCraftCount = Math.max(
						1,
						getMaxCraftableCount(player, allRecipe, TimedCraftingPageState.MAX_CRAFT_COUNT));
			}
		}
		craftingState.setDisplayedCraftQuantity(displayedCraftCount);

		int selectedCraftQuantity = craftingState.getSelectedCraftQuantity();
		boolean craftAllSelected = craftingState.isCraftAllSelected();
		commandBuilder.set("#Qty1Selected.Visible", !craftAllSelected && selectedCraftQuantity == 1);
		commandBuilder.set("#Qty5Selected.Visible", !craftAllSelected && selectedCraftQuantity == 5);
		commandBuilder.set("#Qty10Selected.Visible", !craftAllSelected && selectedCraftQuantity == 10);
		commandBuilder.set("#QtyAllSelected.Visible", craftAllSelected);
		commandBuilder.set("#Qty1.Disabled", false);
		commandBuilder.set("#Qty5.Disabled", false);
		commandBuilder.set("#Qty10.Disabled", false);
		commandBuilder.set("#QtyAll.Disabled", false);
		commandBuilder.set("#QtyCustomApply.Disabled", false);
		commandBuilder.set("#QtyCustomInput.Value", String.valueOf(selectedCraftQuantity));
		return selectedCraftQuantity;
	}

	private static int maxCraftableForMaterial(@Nonnull ItemContainer container, @Nonnull MaterialQuantity material, int maxCap) {
		int perCraft = Math.max(1, material.getQuantity());

		int low = 0;
		int high = 1;
		while (high < maxCap && canRemoveScaledMaterial(container, material, perCraft, high)) {
			low = high;
			high = Math.min(maxCap, high * 2);
		}

		while (low < high) {
			int mid = low + ((high - low + 1) / 2);
			if (canRemoveScaledMaterial(container, material, perCraft, mid)) {
				low = mid;
			} else {
				high = mid - 1;
			}
		}

		return low;
	}

	private static boolean canRemoveScaledMaterial(
			@Nonnull ItemContainer container,
			@Nonnull MaterialQuantity material,
			int perCraft,
			int craftCount) {
		if (craftCount <= 0) {
			return true;
		}

		long total = (long) perCraft * craftCount;
		if (total > Integer.MAX_VALUE) {
			return false;
		}

		MaterialQuantity required = material.clone((int) total);
		return container.canRemoveMaterial(required, true, true);
	}

	@Nonnull
	static String getRecipeOutputName(@Nonnull CraftingRecipe recipe) {
		String itemId = getPrimaryOutputItemId(recipe);
		if (itemId != null) {
			return getItemDisplayName(itemId);
		}
		return recipe.getId();
	}

	@Nonnull
	static Message getRecipeOutputLabel(@Nonnull CraftingRecipe recipe) {
		String itemId = getPrimaryOutputItemId(recipe);
		if (itemId != null) {
			return getItemDisplayMessage(itemId);
		}
		return Message.raw(recipe.getId());
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

	@Nullable
	static CraftingRecipe resolveRecipe(@Nullable String recipeId) {
		if (recipeId == null || recipeId.isBlank()) {
			return null;
		}
		return CraftingRecipe.getAssetMap().getAsset(recipeId);
	}

	static int getPrimaryOutputQuantity(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] outputs = recipe.getOutputs();
		if (outputs == null || outputs.length == 0) {
			return 1;
		}
		return Math.max(1, outputs[0].getQuantity());
	}

	static void configureOutputSlot(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull String slotSelector,
			@Nullable CraftingRecipe recipe) {
		if (recipe == null) {
			commandBuilder.set(slotSelector + ".Visible", false);
			return;
		}

		String outputItemId = getPrimaryOutputItemId(recipe);
		if (outputItemId == null) {
			commandBuilder.set(slotSelector + ".Visible", false);
			return;
		}

		int outputQuantity = getPrimaryOutputQuantity(recipe);
		commandBuilder.set(slotSelector + ".ItemId", outputItemId);
		commandBuilder.set(slotSelector + ".Quantity", outputQuantity);
		commandBuilder.set(slotSelector + ".ShowQuantity", outputQuantity > 1);
		commandBuilder.set(slotSelector + ".Visible", true);
	}

	static void syncSelectedRecipePreview(
			@Nonnull UICommandBuilder commandBuilder,
			@Nullable CraftingRecipe selectedRecipe) {
		configureOutputSlot(commandBuilder, "#SelectedOutputSlot", selectedRecipe);
		if (selectedRecipe != null) {
			commandBuilder.set("#SelectedOutputName.TextSpans", getRecipeOutputLabel(selectedRecipe));
			configureFlowGraph(commandBuilder, selectedRecipe);
			return;
		}

		commandBuilder.set("#SelectedOutputName.Text", "Select a recipe to preview");
		configureFlowGraph(commandBuilder, null);
	}

	@Nonnull
	static Message formatIngredientsLabel(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] inputs = recipe.getInput();
		if (inputs == null || inputs.length == 0) {
			return Message.raw("No ingredients");
		}

		Message message = Message.empty();
		for (int i = 0; i < inputs.length; i++) {
			MaterialQuantity input = inputs[i];
			if (input == null) {
				continue;
			}
			if (i > 0) {
				message.insert(", ");
			}

			int quantity = Math.max(1, input.getQuantity());
			if (quantity > 1) {
				message.insert(quantity + "x ");
			}
			message.insert(getMaterialDisplayMessage(input));
		}

		return message;
	}

	static void configureIngredientSlots(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull String parentSelector,
			@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] inputs = recipe.getInput();
		for (int i = 0; i < MAX_INGREDIENT_ICONS; i++) {
			String slotSelector = parentSelector + " #IngredientSlot" + i;
			if (inputs != null && i < inputs.length && inputs[i] != null && inputs[i].getItemId() != null) {
				String itemId = inputs[i].getItemId();
				if (itemId != null && !itemId.isBlank()) {
					int quantity = Math.max(1, inputs[i].getQuantity());
					commandBuilder.set(slotSelector + ".ItemId", itemId);
					commandBuilder.set(slotSelector + ".Quantity", quantity);
					commandBuilder.set(slotSelector + ".ShowQuantity", quantity > 1);
					commandBuilder.set(slotSelector + ".Visible", true);
					continue;
				}
			}
			commandBuilder.set(slotSelector + ".Visible", false);
		}
	}

	static void configureFlowGraph(
			@Nonnull UICommandBuilder commandBuilder,
			@Nullable CraftingRecipe recipe) {
		if (recipe == null) {
			for (int i = 0; i < MAX_INGREDIENT_ICONS; i++) {
				commandBuilder.set("#FlowIngredientSlot" + i + ".Visible", false);
			}
			commandBuilder.set("#FlowOutputSlot.Visible", false);
			return;
		}

		MaterialQuantity[] inputs = recipe.getInput();
		for (int i = 0; i < MAX_INGREDIENT_ICONS; i++) {
			String slotSelector = "#FlowIngredientSlot" + i;
			if (inputs != null && i < inputs.length && inputs[i] != null) {
				String itemId = inputs[i].getItemId();
				if (itemId != null && !itemId.isBlank()) {
					int quantity = Math.max(1, inputs[i].getQuantity());
					commandBuilder.set(slotSelector + ".ItemId", itemId);
					commandBuilder.set(slotSelector + ".Quantity", quantity);
					commandBuilder.set(slotSelector + ".ShowQuantity", quantity > 1);
					commandBuilder.set(slotSelector + ".Visible", true);
					continue;
				}
			}
			commandBuilder.set(slotSelector + ".Visible", false);
		}

		String outputItemId = getPrimaryOutputItemId(recipe);
		if (outputItemId != null) {
			int outputQuantity = getPrimaryOutputQuantity(recipe);
			commandBuilder.set("#FlowOutputSlot.ItemId", outputItemId);
			commandBuilder.set("#FlowOutputSlot.Quantity", outputQuantity);
			commandBuilder.set("#FlowOutputSlot.ShowQuantity", outputQuantity > 1);
			commandBuilder.set("#FlowOutputSlot.Visible", true);
		} else {
			commandBuilder.set("#FlowOutputSlot.Visible", false);
		}
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

	static boolean updateCraftButtonState(
			@Nonnull UICommandBuilder commandBuilder,
			@Nullable CraftingRecipe selectedRecipe,
			boolean hasRequiredMaterials,
			int craftCount) {
		if (selectedRecipe == null) {
			commandBuilder.set("#StartCraftingButton.Text", "Select a Recipe");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
			return false;
		}

		if (!hasRequiredMaterials) {
			commandBuilder.set("#StartCraftingButton.Text", "Materials Required");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
			return true;
		}

		int normalizedCount = Math.max(1, craftCount);
		commandBuilder.set(
				"#StartCraftingButton.TextSpans",
				Message.join(Message.raw("Craft x" + normalizedCount + " "), getRecipeOutputLabel(selectedRecipe)));
		commandBuilder.set("#StartCraftingButton.Disabled", false);
		return true;
	}

	@Nonnull
	private static Message getMaterialDisplayMessage(@Nonnull MaterialQuantity material) {
		String itemId = material.getItemId();
		if (itemId != null && !itemId.isBlank()) {
			return getItemDisplayMessage(itemId);
		}

		String resourceTypeId = material.getResourceTypeId();
		if (resourceTypeId != null && !resourceTypeId.isBlank()) {
			return Message.raw(formatDisplayId(resourceTypeId));
		}

		return Message.raw("Unknown");
	}

	@Nonnull
	static String getItemDisplayName(@Nonnull String itemId) {
		Item item = Item.getAssetMap().getAsset(itemId);
		if (item == null) {
			return formatDisplayId(itemId);
		}

		String translationKey = item.getTranslationKey();
		String translatedId = extractItemIdFromTranslationKey(translationKey);
		if (translatedId != null) {
			return formatDisplayId(translatedId);
		}

		String assetId = item.getId();
		if (assetId != null && !assetId.isBlank()) {
			return formatDisplayId(assetId);
		}

		return formatDisplayId(itemId);
	}

	@Nonnull
	static Message getItemDisplayMessage(@Nonnull String itemId) {
		Item item = Item.getAssetMap().getAsset(itemId);
		if (item == null) {
			return Message.raw(formatDisplayId(itemId));
		}

		String translationKey = item.getTranslationKey();
		if (translationKey != null && !translationKey.isBlank()) {
			return Message.translation(translationKey);
		}

		String assetId = item.getId();
		if (assetId != null && !assetId.isBlank()) {
			return Message.raw(formatDisplayId(assetId));
		}

		return Message.raw(formatDisplayId(itemId));
	}

	@Nullable
	private static String extractItemIdFromTranslationKey(@Nullable String translationKey) {
		if (translationKey == null || translationKey.isBlank()) {
			return null;
		}

		if (translationKey.startsWith("server.items.") && translationKey.endsWith(".name")) {
			return translationKey.substring("server.items.".length(), translationKey.length() - ".name".length());
		}

		return null;
	}

	@Nonnull
	private static String formatDisplayId(@Nonnull String rawId) {
		int colonIndex = rawId.lastIndexOf(':');
		String normalized = colonIndex >= 0 ? rawId.substring(colonIndex + 1) : rawId;
		normalized = normalized.replace('_', ' ').replace('-', ' ');
		return toTitleCase(normalized);
	}

	@Nonnull
	private static String toTitleCase(@Nonnull String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		String[] parts = lower.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return builder.length() > 0 ? builder.toString() : text;
	}
}
