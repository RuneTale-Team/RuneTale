package org.runetale.skills.page;

import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom UI page for smelting at the RuneTale furnace.
 *
 * <p>
 * Displays recipes organized by material tier tabs. Locked recipes
 * (insufficient smithing level) appear dimmed with requirement labels.
 * Clicking a recipe selects it; the bottom button then executes the craft.
 */
public class SmeltingPage extends InteractiveCustomUIPage<SmeltingPage.SmeltingPageEventData> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String UI_PATH = "SkillsPlugin/Smelting.ui";
	private static final String RECIPE_ROW_TEMPLATE = "SkillsPlugin/SmeltingRecipeRow.ui";
	private static final String BENCH_ID = "RuneTale_Furnace";

	private static final String COLOR_SELECTED = "#243a56";
	private static final String COLOR_NORMAL = "#1a2a3e";

	private final BlockPosition blockPosition;
	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;

	@Nonnull
	private SmithingMaterialTier selectedTier = SmithingMaterialTier.BRONZE;

	@Nullable
	private String selectedRecipeId;

	public SmeltingPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull BlockPosition blockPosition,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		super(playerRef, CustomPageLifetime.CanDismiss, SmeltingPageEventData.CODEC);
		this.blockPosition = blockPosition;
		this.profileComponentType = profileComponentType;
		this.craftingRecipeTagService = craftingRecipeTagService;
	}

	@Override
	public void build(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			@Nonnull Store<EntityStore> store) {
		initializeBenchBinding(ref, store);
		commandBuilder.append(UI_PATH);
		bindTierTabs(eventBuilder);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#StartCraftingButton",
				EventData.of("Action", "Craft"),
				false);
		render(ref, store, commandBuilder, eventBuilder);
	}

	@Override
	public void handleDataEvent(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull SmeltingPageEventData data) {

		// Handle craft action
		if ("Craft".equalsIgnoreCase(data.action)) {
			executeCraft(ref, store);
		}

		// Handle recipe selection
		if (data.recipeId != null) {
			this.selectedRecipeId = data.recipeId;
		}

		// Handle tier change — clears selection
		if (data.tier != null) {
			SmithingMaterialTier parsed = parseTier(data.tier);
			if (parsed != null) {
				this.selectedTier = parsed;
				this.selectedRecipeId = null;
			}
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		UIEventBuilder eventBuilder = new UIEventBuilder();
		render(ref, store, commandBuilder, eventBuilder);
		this.sendUpdate(commandBuilder, eventBuilder, false);
	}

	@Override
	public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		clearBenchBinding(ref, store);
	}

	private void executeCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		if (this.selectedRecipeId == null) {
			return;
		}

		CraftingRecipe recipe = CraftingRecipe.getAssetMap().getAsset(this.selectedRecipeId);
		if (recipe == null) {
			LOGGER.atWarning().log("Selected recipe not found: %s", this.selectedRecipeId);
			return;
		}

		PlayerSkillProfileComponent profile = store.getComponent(ref, this.profileComponentType);
		if (profile == null) {
			return;
		}

		List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(recipe);
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
			return;
		}

		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		Player player = store.getComponent(ref, Player.getComponentType());
		if (craftingManager == null || player == null) {
			LOGGER.atWarning().log("Cannot smelt %s because crafting context is unavailable", recipe.getId());
			return;
		}

		boolean crafted = craftingManager.craftItem(
				ref,
				store,
				recipe,
				1,
				player.getInventory().getCombinedBackpackStorageHotbar());
		if (crafted && playerRef != null) {
			String outputName = getRecipeOutputName(recipe);
			NotificationUtil.sendNotification(
					playerRef.getPacketHandler(),
					Message.raw("[Skills] Smelted " + outputName),
					NotificationStyle.Default);
		}
	}

	private void render(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		PlayerSkillProfileComponent profile = store.getComponent(ref, this.profileComponentType);
		int smithingLevel = profile == null ? 1 : profile.getLevel(SkillType.SMITHING);

		// Update tier tab selection state
		for (SmithingMaterialTier tier : SmithingMaterialTier.values()) {
			String selector = "#Tier" + tier.getDisplayName();
			boolean selected = tier == this.selectedTier;
			commandBuilder.set(selector + ".Text",
					tier.getDisplayName() + (selected ? " [Selected]" : ""));
		}

		// Update section title
		commandBuilder.set("#SectionTitle.Text", this.selectedTier.getSectionTitle("Bars"));

		// Build recipe list
		commandBuilder.clear("#RecipeList");
		List<CraftingRecipe> recipes = getRecipesForTier(this.selectedTier);

		for (int i = 0; i < recipes.size(); i++) {
			CraftingRecipe recipe = recipes.get(i);
			String selector = "#RecipeList[" + i + "]";

			commandBuilder.append("#RecipeList", RECIPE_ROW_TEMPLATE);

			// Highlight selected recipe
			boolean isSelected = recipe.getId().equals(this.selectedRecipeId);
			commandBuilder.set(selector + ".Background", isSelected ? COLOR_SELECTED : COLOR_NORMAL);

			// Bind click to select this recipe
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					selector + " #SelectBtn",
					EventData.of("RecipeId", recipe.getId()),
					false);

			// Output name
			String outputName = getRecipeOutputName(recipe);
			commandBuilder.set(selector + " #RecipeName.Text", outputName);

			// Ingredients
			String ingredients = formatIngredients(recipe);
			commandBuilder.set(selector + " #RecipeIngredients.Text", ingredients);

			// XP reward
			String xpText = this.craftingRecipeTagService.getXpOnSuccessfulCraft(recipe)
					.map(xp -> "+" + String.format(Locale.ROOT, "%.1f", xp) + " XP")
					.orElse("");
			commandBuilder.set(selector + " #RecipeXp.Text", xpText);

			// Level requirement check
			List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(recipe);
			int requiredLevel = getSmithingRequiredLevel(requirements);
			boolean unlocked = smithingLevel >= requiredLevel;

			if (unlocked) {
				commandBuilder.set(selector + " #RecipeStatus.Text", "Unlocked");
				commandBuilder.set(selector + " #LockOverlay.Visible", false);
			} else {
				commandBuilder.set(selector + " #RecipeStatus.Text", "Requires Lv " + requiredLevel + " Smithing");
				commandBuilder.set(selector + " #LockOverlay.Visible", true);
			}
		}

		if (recipes.isEmpty()) {
			commandBuilder.append("#RecipeList", RECIPE_ROW_TEMPLATE);
			commandBuilder.set("#RecipeList[0] #RecipeName.Text", "No recipes available");
			commandBuilder.set("#RecipeList[0] #RecipeIngredients.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeXp.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeStatus.Text", "");
			commandBuilder.set("#RecipeList[0] #LockOverlay.Visible", false);
		}

		// Update craft button text
		if (this.selectedRecipeId != null) {
			CraftingRecipe selectedRecipe = CraftingRecipe.getAssetMap().getAsset(this.selectedRecipeId);
			if (selectedRecipe != null) {
				commandBuilder.set("#StartCraftingButton.Text", "Craft " + getRecipeOutputName(selectedRecipe));
			} else {
				commandBuilder.set("#StartCraftingButton.Text", "Select a Recipe");
				this.selectedRecipeId = null;
			}
		} else {
			commandBuilder.set("#StartCraftingButton.Text", "Select a Recipe");
		}
	}

	private void bindTierTabs(@Nonnull UIEventBuilder eventBuilder) {
		for (SmithingMaterialTier tier : SmithingMaterialTier.values()) {
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					"#Tier" + tier.getDisplayName(),
					EventData.of("Tier", tier.name()),
					false);
		}
	}

	@Nonnull
	private List<CraftingRecipe> getRecipesForTier(@Nonnull SmithingMaterialTier tier) {
		String categoryId = BENCH_ID + "_" + tier.getDisplayName();
		List<CraftingRecipe> categoryRecipes = CraftingPlugin.getBenchRecipes(BenchType.Crafting, BENCH_ID, categoryId);
		if (!categoryRecipes.isEmpty()) {
			return categoryRecipes;
		}

		List<CraftingRecipe> allRecipes = CraftingPlugin.getBenchRecipes(BenchType.Crafting, BENCH_ID);
		List<CraftingRecipe> filtered = new ArrayList<>();
		String barSubstring = tier.getBarSubstring().toLowerCase(Locale.ROOT);

		for (CraftingRecipe recipe : allRecipes) {
			MaterialQuantity[] outputs = recipe.getOutputs();
			if (outputs != null) {
				for (MaterialQuantity output : outputs) {
					String itemId = output.getItemId();
					if (itemId != null && itemId.toLowerCase(Locale.ROOT).contains(barSubstring)) {
						filtered.add(recipe);
						break;
					}
				}
			}
		}

		return filtered;
	}

	private void initializeBenchBinding(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		if (craftingManager == null) {
			LOGGER.atWarning().log("Cannot initialize smelting bench binding: missing CraftingManager component");
			return;
		}

		if (craftingManager.hasBenchSet()) {
			craftingManager.clearBench(ref, store);
		}

		World world = store.getExternalData().getWorld();
		BlockType blockType = world.getBlockType(this.blockPosition.x, this.blockPosition.y, this.blockPosition.z);
		if (blockType == null || blockType.getBench() == null) {
			LOGGER.atWarning().log(
					"Cannot initialize smelting bench binding: invalid block at %d,%d,%d",
					this.blockPosition.x,
					this.blockPosition.y,
					this.blockPosition.z);
			return;
		}

		try {
			craftingManager.setBench(this.blockPosition.x, this.blockPosition.y, this.blockPosition.z, blockType);
		} catch (IllegalArgumentException e) {
			LOGGER.atWarning().withCause(e).log("Failed to bind smelting page to crafting bench");
		}
	}

	private void clearBenchBinding(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		if (craftingManager != null && craftingManager.hasBenchSet()) {
			craftingManager.clearBench(ref, store);
		}
	}

	@Nonnull
	private String getRecipeOutputName(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] outputs = recipe.getOutputs();
		if (outputs != null && outputs.length > 0) {
			String itemId = outputs[0].getItemId();
			if (itemId != null) {
				return formatItemId(itemId);
			}
		}
		return recipe.getId();
	}

	@Nonnull
	private String formatIngredients(@Nonnull CraftingRecipe recipe) {
		MaterialQuantity[] inputs = recipe.getInput();
		if (inputs == null || inputs.length == 0) {
			return "No ingredients";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < inputs.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			String itemId = inputs[i].getItemId();
			int quantity = inputs[i].getQuantity();
			String name = itemId != null ? formatItemId(itemId) : "Unknown";
			if (quantity > 1) {
				sb.append(quantity).append("x ");
			}
			sb.append(name);
		}
		return sb.toString();
	}

	@Nonnull
	private String formatItemId(@Nonnull String itemId) {
		int colonIndex = itemId.lastIndexOf(':');
		String name = colonIndex >= 0 ? itemId.substring(colonIndex + 1) : itemId;
		return name.replace('_', ' ');
	}

	private int getSmithingRequiredLevel(@Nonnull List<SkillRequirement> requirements) {
		for (SkillRequirement req : requirements) {
			if (req.skillType() == SkillType.SMITHING) {
				return req.requiredLevel();
			}
		}
		return 1;
	}

	@Nullable
	private SmithingMaterialTier parseTier(@Nullable String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return SmithingMaterialTier.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	public static class SmeltingPageEventData {
		private static final String KEY_ACTION = "Action";
		private static final String KEY_TIER = "Tier";
		private static final String KEY_RECIPE_ID = "RecipeId";

		public static final BuilderCodec<SmeltingPageEventData> CODEC = BuilderCodec
				.builder(SmeltingPageEventData.class, SmeltingPageEventData::new)
				.append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (entry, value) -> entry.action = value, entry -> entry.action)
				.add()
				.append(new KeyedCodec<>(KEY_TIER, Codec.STRING), (entry, value) -> entry.tier = value, entry -> entry.tier)
				.add()
				.append(new KeyedCodec<>(KEY_RECIPE_ID, Codec.STRING), (entry, value) -> entry.recipeId = value, entry -> entry.recipeId)
				.add()
				.build();

		private String action;
		private String tier;
		private String recipeId;
	}
}
