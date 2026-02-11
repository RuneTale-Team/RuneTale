package org.runetale.skills.page;

import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
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
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
	private static final long CRAFT_DURATION_MILLIS = 3000L;

	private final BlockPosition blockPosition;
	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;

	@Nonnull
	private SmithingMaterialTier selectedTier = SmithingMaterialTier.BRONZE;

	@Nullable
	private String selectedRecipeId;

	@Nonnull
	private final TimedCraftingPageState craftingState = new TimedCraftingPageState();

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
		CraftingPageSupport.initializeBenchBinding(ref, store, this.blockPosition, LOGGER, "smelting");
		commandBuilder.append(UI_PATH);
		CraftingPageSupport.bindTierTabs(eventBuilder);
		CraftingPageSupport.bindQuantityControls(eventBuilder);
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
			startCraft(ref, store);
		}

		// Handle recipe selection
		if (!this.craftingState.isCraftingInProgress() && data.recipeId != null) {
			this.selectedRecipeId = data.recipeId;
		}

		// Handle tier change — clears selection
		if (!this.craftingState.isCraftingInProgress() && data.tier != null) {
			SmithingMaterialTier parsed = CraftingPageSupport.parseTier(data.tier);
			if (parsed != null) {
				this.selectedTier = parsed;
				this.selectedRecipeId = null;
			}
		}

		if (!this.craftingState.isCraftingInProgress() && data.quantity != null) {
			this.craftingState.applyQuantitySelection(data.quantity);
		}

		if (!this.craftingState.isCraftingInProgress() && "SetQuantity".equalsIgnoreCase(data.action)) {
			this.craftingState.applyCustomQuantity(data.quantityInput);
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		UIEventBuilder eventBuilder = new UIEventBuilder();
		render(ref, store, commandBuilder, eventBuilder);
		this.sendUpdate(commandBuilder, eventBuilder, false);
	}

	@Override
	public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		this.craftingState.reset();
		CraftingPageSupport.clearBenchBinding(ref, store);
	}

	private boolean finishCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String recipeId) {
		return CraftingPageSupport.executeCraft(
				ref,
				store,
				recipeId,
				this.profileComponentType,
				this.craftingRecipeTagService,
				LOGGER,
				"Smelted",
				"smelt");
	}

	private void startCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		if (this.craftingState.isCraftingInProgress() || this.selectedRecipeId == null) {
			return;
		}

		CraftingRecipe selectedRecipe = CraftingPageSupport.resolveRecipe(this.selectedRecipeId);
		if (selectedRecipe == null) {
			return;
		}

		PlayerSkillProfileComponent profile = store.getComponent(ref, this.profileComponentType);
		Player player = store.getComponent(ref, Player.getComponentType());
		if (profile == null || player == null) {
			return;
		}

		int smithingLevel = profile.getLevel(SkillType.SMITHING);
		List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(selectedRecipe);
		int requiredLevel = CraftingPageSupport.getSmithingRequiredLevel(requirements);
		if (smithingLevel < requiredLevel || !CraftingPageSupport.hasRequiredMaterials(player, selectedRecipe)) {
			return;
		}

		int targetCraftCount = this.craftingState.isCraftAllSelected()
				? CraftingPageSupport.getMaxCraftableCount(player, selectedRecipe, TimedCraftingPageState.MAX_CRAFT_COUNT)
				: this.craftingState.getSelectedCraftQuantity();
		targetCraftCount = Math.max(0, targetCraftCount);
		if (targetCraftCount <= 0) {
			return;
		}

		this.craftingState.startCrafting(selectedRecipe.getId(), targetCraftCount, CRAFT_DURATION_MILLIS);
	}

	public void tickProgress(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, float deltaTime) {
		String activeRecipeId = this.craftingState.getCraftingRecipeId();
		if (!this.craftingState.isCraftingInProgress() || activeRecipeId == null) {
			return;
		}
		if (!this.craftingState.shouldEmitProgressUpdate()) {
			return;
		}

		if (this.craftingState.isCurrentCraftFinished()) {
			String recipeId = this.craftingState.completeCurrentCraftStep();
			if (recipeId == null) {
				return;
			}
			boolean crafted = finishCraft(ref, store, recipeId);
			CraftingRecipe recipe = CraftingRecipe.getAssetMap().getAsset(recipeId);
			Player player = store.getComponent(ref, Player.getComponentType());
			boolean canContinue = crafted && recipe != null && CraftingPageSupport.hasRequiredMaterials(player, recipe);
			this.craftingState.handleCraftStepResult(crafted, canContinue, CRAFT_DURATION_MILLIS);

			UICommandBuilder commandBuilder = new UICommandBuilder();
			UIEventBuilder eventBuilder = new UIEventBuilder();
			render(ref, store, commandBuilder, eventBuilder);
			this.sendUpdate(commandBuilder, eventBuilder, false);
			return;
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		appendProgressUi(commandBuilder);
		this.sendUpdate(commandBuilder, false);
	}

	private void appendProgressUi(@Nonnull UICommandBuilder commandBuilder) {
		float progress = this.craftingState.isCraftingInProgress() ? this.craftingState.getProgress() : 0.0F;
		int progressPercent = this.craftingState.getProgressPercent();
		int currentCraftIndex = this.craftingState.getCurrentCraftIndex();
		int totalCraftCount = this.craftingState.getTotalCraftCount();
		commandBuilder.set("#CraftProgressBar.Value", progress);
		commandBuilder.set(
				"#CraftProgressLabel.Text",
				this.craftingState.isCraftingInProgress()
						? String.format(Locale.ROOT, "Smelting... %d%% (%d/%d)", progressPercent, currentCraftIndex, totalCraftCount)
						: "Ready");
	}

	private void render(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		PlayerSkillProfileComponent profile = store.getComponent(ref, this.profileComponentType);
		Player player = store.getComponent(ref, Player.getComponentType());
		int smithingLevel = profile == null ? 1 : profile.getLevel(SkillType.SMITHING);

		// Update tier tab selection state
		for (SmithingMaterialTier tier : SmithingMaterialTier.values()) {
			String selector = "#Tier" + tier.getDisplayName();
			boolean selected = tier == this.selectedTier;
			commandBuilder.set(selector + ".Text", tier.getDisplayName());
			commandBuilder.set(selector + "Indicator.Visible", selected);
			commandBuilder.set(selector + "Selected.Visible", selected);
		}

		int selectedCraftQuantity = CraftingPageSupport.syncQuantityControls(
				commandBuilder,
				this.craftingState,
				player,
				this.selectedRecipeId);

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
			commandBuilder.set(selector + " #SelectedFrame.Visible", isSelected);

			// Bind click to select this recipe
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					selector + " #SelectBtn",
					EventData.of("RecipeId", recipe.getId()),
					false);

			// Output name
			commandBuilder.set(selector + " #RecipeName.TextSpans", CraftingPageSupport.getRecipeOutputLabel(recipe));

			CraftingPageSupport.configureOutputSlot(commandBuilder, selector + " #RecipeOutputSlot", recipe);

			// Ingredients
			CraftingPageSupport.configureIngredientSlots(commandBuilder, selector, recipe);
			commandBuilder.set(selector + " #RecipeIngredients.TextSpans", CraftingPageSupport.formatIngredientsLabel(recipe));

			// XP reward
			String xpText = CraftingPageSupport.getXpText(recipe, this.craftingRecipeTagService);
			commandBuilder.set(selector + " #RecipeXp.Text", xpText);

			// Level requirement check
			List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(recipe);
			int requiredLevel = CraftingPageSupport.getSmithingRequiredLevel(requirements);
			boolean unlocked = smithingLevel >= requiredLevel;
			boolean hasMaterials = CraftingPageSupport.hasRequiredMaterials(player, recipe);

			if (!unlocked) {
				commandBuilder.set(selector + " #RecipeStatus.Text", "Requires Lv " + requiredLevel + " Smithing");
				commandBuilder.set(selector + " #RecipeStatus.Style.TextColor", "#d7a6a6");
				commandBuilder.set(selector + " #LockOverlay.Visible", true);
			} else if (!hasMaterials) {
				commandBuilder.set(selector + " #RecipeStatus.Text", "Materials required");
				commandBuilder.set(selector + " #RecipeStatus.Style.TextColor", "#d8c187");
				commandBuilder.set(selector + " #LockOverlay.Visible", true);
			} else {
				commandBuilder.set(selector + " #RecipeStatus.Text", "Unlocked");
				commandBuilder.set(selector + " #RecipeStatus.Style.TextColor", "#99afc6");
				commandBuilder.set(selector + " #LockOverlay.Visible", false);
			}
		}

		if (recipes.isEmpty()) {
			commandBuilder.append("#RecipeList", RECIPE_ROW_TEMPLATE);
			commandBuilder.set("#RecipeList[0] #RecipeName.Text", "No recipes available");
			commandBuilder.set("#RecipeList[0] #RecipeIngredients.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeXp.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeStatus.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeOutputSlot.Visible", false);
			commandBuilder.set("#RecipeList[0] #IngredientSlot0.Visible", false);
			commandBuilder.set("#RecipeList[0] #IngredientSlot1.Visible", false);
			commandBuilder.set("#RecipeList[0] #IngredientSlot2.Visible", false);
			commandBuilder.set("#RecipeList[0] #SelectedFrame.Visible", false);
			commandBuilder.set("#RecipeList[0] #LockOverlay.Visible", false);
		}

		CraftingRecipe selectedPreviewRecipe = CraftingPageSupport.resolveRecipe(this.selectedRecipeId);
		CraftingPageSupport.syncSelectedRecipePreview(commandBuilder, selectedPreviewRecipe);

		CraftingRecipe selectedRecipe = CraftingPageSupport.resolveRecipe(this.selectedRecipeId);
		boolean selectedUnlocked = false;
		if (selectedRecipe != null) {
			int selectedRequiredLevel = CraftingPageSupport.getSmithingRequiredLevel(this.craftingRecipeTagService.getSkillRequirements(selectedRecipe));
			selectedUnlocked = smithingLevel >= selectedRequiredLevel;
		}
		boolean canCraftSelected = selectedRecipe != null
				&& selectedUnlocked
				&& CraftingPageSupport.hasRequiredMaterials(player, selectedRecipe);
		if (this.craftingState.isCraftingInProgress()) {
			commandBuilder.set("#StartCraftingButton.Text", "Smelting...");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
		} else if (selectedRecipe != null && !selectedUnlocked) {
			commandBuilder.set("#StartCraftingButton.Text", "Level Required");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
		} else if (!CraftingPageSupport.updateCraftButtonState(commandBuilder, selectedRecipe, canCraftSelected, selectedCraftQuantity)) {
			this.selectedRecipeId = null;
		}

		appendProgressUi(commandBuilder);
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


	public static class SmeltingPageEventData {
		private static final String KEY_ACTION = "Action";
		private static final String KEY_TIER = "Tier";
		private static final String KEY_QUANTITY = "Quantity";
		private static final String KEY_QUANTITY_INPUT = "@QuantityInput";
		private static final String KEY_RECIPE_ID = "RecipeId";

		public static final BuilderCodec<SmeltingPageEventData> CODEC = BuilderCodec
				.builder(SmeltingPageEventData.class, SmeltingPageEventData::new)
				.append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (entry, value) -> entry.action = value, entry -> entry.action)
				.add()
				.append(new KeyedCodec<>(KEY_TIER, Codec.STRING), (entry, value) -> entry.tier = value, entry -> entry.tier)
				.add()
				.append(new KeyedCodec<>(KEY_QUANTITY, Codec.STRING), (entry, value) -> entry.quantity = value, entry -> entry.quantity)
				.add()
				.append(new KeyedCodec<>(KEY_QUANTITY_INPUT, Codec.STRING), (entry, value) -> entry.quantityInput = value, entry -> entry.quantityInput)
				.add()
				.append(new KeyedCodec<>(KEY_RECIPE_ID, Codec.STRING), (entry, value) -> entry.recipeId = value, entry -> entry.recipeId)
				.add()
				.build();

		private String action;
		private String tier;
		private String quantity;
		private String quantityInput;
		private String recipeId;
	}
}
