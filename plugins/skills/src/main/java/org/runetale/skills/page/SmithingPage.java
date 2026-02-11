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
import java.util.List;
import java.util.Locale;

/**
 * Custom UI page for smithing at the RuneTale anvil.
 *
 * <p>
 * Displays recipes organized by material tier tabs with a card grid layout.
 * Locked recipes (insufficient smithing level) appear dimmed with requirement
 * labels. Clicking a recipe selects it; the bottom button then executes the craft.
 */
public class SmithingPage extends InteractiveCustomUIPage<SmithingPage.SmithingPageEventData> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String UI_PATH = "SkillsPlugin/Smithing.ui";
	private static final String RECIPE_CARD_TEMPLATE = "SkillsPlugin/SmithingRecipeCard.ui";
	private static final String BENCH_ID = "RuneTale_Anvil";
	private static final String CARD_ROW_INLINE = "Group { LayoutMode: Left; Anchor: (Bottom: 10); }";
	private static final String CARD_COLUMN_SPACER_INLINE = "Group { Anchor: (Width: 10); }";

	private final BlockPosition blockPosition;
	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;

	@Nonnull
	private SmithingMaterialTier selectedTier = SmithingMaterialTier.BRONZE;

	@Nullable
	private String selectedRecipeId;

	public SmithingPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull BlockPosition blockPosition,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		super(playerRef, CustomPageLifetime.CanDismiss, SmithingPageEventData.CODEC);
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
		CraftingPageSupport.initializeBenchBinding(ref, store, this.blockPosition, LOGGER, "smithing");
		commandBuilder.append(UI_PATH);
		CraftingPageSupport.bindTierTabs(eventBuilder);
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
			@Nonnull SmithingPageEventData data) {

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
			SmithingMaterialTier parsed = CraftingPageSupport.parseTier(data.tier);
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
		CraftingPageSupport.clearBenchBinding(ref, store);
	}

	private void executeCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		CraftingPageSupport.executeCraft(
				ref,
				store,
				this.selectedRecipeId,
				this.profileComponentType,
				this.craftingRecipeTagService,
				LOGGER,
				"Crafted",
				"smith");
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
			commandBuilder.set(selector + ".Text", tier.getDisplayName());
			commandBuilder.set(selector + "Indicator.Visible", selected);
			commandBuilder.set(selector + "Selected.Visible", selected);
		}

		// Update section title
		commandBuilder.set("#SectionTitle.Text", this.selectedTier.getSectionTitle("Equipment"));

		// Build recipe card grid
		commandBuilder.clear("#RecipeGrid");
		List<CraftingRecipe> recipes = CraftingPlugin.getBenchRecipes(
				BenchType.Crafting, BENCH_ID, this.selectedTier.getAnvilCategory());

		for (int i = 0; i < recipes.size(); i++) {
			CraftingRecipe recipe = recipes.get(i);

			int row = i / 2;
			int col = i % 2;
			if (col == 0) {
				commandBuilder.appendInline("#RecipeGrid", CARD_ROW_INLINE);
			} else {
				commandBuilder.appendInline("#RecipeGrid[" + row + "]", CARD_COLUMN_SPACER_INLINE);
			}

			int uiCol = col == 0 ? 0 : 2;
			String cardSelector = "#RecipeGrid[" + row + "][" + uiCol + "]";
			commandBuilder.append("#RecipeGrid[" + row + "]", RECIPE_CARD_TEMPLATE);

			// Highlight selected recipe
			boolean isSelected = recipe.getId().equals(this.selectedRecipeId);
			commandBuilder.set(cardSelector + " #SelectedFrame.Visible", isSelected);

			// Bind click to select this recipe
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					cardSelector + " #SelectBtn",
					EventData.of("RecipeId", recipe.getId()),
					false);

			// Output name
			String outputName = CraftingPageSupport.getRecipeOutputName(recipe);
			commandBuilder.set(cardSelector + " #CardName.Text", outputName);

			String outputItemId = CraftingPageSupport.getPrimaryOutputItemId(recipe);
			if (outputItemId != null) {
				int outputQuantity = CraftingPageSupport.getPrimaryOutputQuantity(recipe);
				commandBuilder.set(cardSelector + " #CardOutputSlot.ItemId", outputItemId);
				commandBuilder.set(cardSelector + " #CardOutputSlot.Quantity", outputQuantity);
				commandBuilder.set(cardSelector + " #CardOutputSlot.ShowQuantity", outputQuantity > 1);
				commandBuilder.set(cardSelector + " #CardOutputSlot.Visible", true);
			} else {
				commandBuilder.set(cardSelector + " #CardOutputSlot.Visible", false);
			}

			// Ingredients
			String ingredients = CraftingPageSupport.formatIngredients(recipe);
			commandBuilder.set(cardSelector + " #CardIngredients.Text", ingredients);

			// XP reward
			String xpText = CraftingPageSupport.getXpText(recipe, this.craftingRecipeTagService);
			commandBuilder.set(cardSelector + " #CardXp.Text", xpText);

			// Level requirement check
			List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(recipe);
			int requiredLevel = CraftingPageSupport.getSmithingRequiredLevel(requirements);
			boolean unlocked = smithingLevel >= requiredLevel;

			if (unlocked) {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Unlocked");
				commandBuilder.set(cardSelector + " #LockOverlay.Visible", false);
			} else {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Requires Lv " + requiredLevel + " Smithing");
				commandBuilder.set(cardSelector + " #LockOverlay.Visible", true);
			}
		}

		if (recipes.isEmpty()) {
			commandBuilder.appendInline("#RecipeGrid", CARD_ROW_INLINE);
			commandBuilder.append("#RecipeGrid[0]", RECIPE_CARD_TEMPLATE);
			commandBuilder.set("#RecipeGrid[0][0] #CardName.Text", "No recipes available");
			commandBuilder.set("#RecipeGrid[0][0] #CardIngredients.Text", "");
			commandBuilder.set("#RecipeGrid[0][0] #CardXp.Text", "");
			commandBuilder.set("#RecipeGrid[0][0] #CardStatus.Text", "");
			commandBuilder.set("#RecipeGrid[0][0] #CardOutputSlot.Visible", false);
			commandBuilder.set("#RecipeGrid[0][0] #SelectedFrame.Visible", false);
			commandBuilder.set("#RecipeGrid[0][0] #LockOverlay.Visible", false);
		}

		if (this.selectedRecipeId != null) {
			CraftingRecipe selectedRecipe = CraftingRecipe.getAssetMap().getAsset(this.selectedRecipeId);
			if (selectedRecipe != null) {
				String outputItemId = CraftingPageSupport.getPrimaryOutputItemId(selectedRecipe);
				if (outputItemId != null) {
					int outputQuantity = CraftingPageSupport.getPrimaryOutputQuantity(selectedRecipe);
					commandBuilder.set("#SelectedOutputSlot.ItemId", outputItemId);
					commandBuilder.set("#SelectedOutputSlot.Quantity", outputQuantity);
					commandBuilder.set("#SelectedOutputSlot.ShowQuantity", outputQuantity > 1);
					commandBuilder.set("#SelectedOutputSlot.Visible", true);
					commandBuilder.set("#SelectedOutputName.Text", CraftingPageSupport.getRecipeOutputName(selectedRecipe));
				} else {
					commandBuilder.set("#SelectedOutputSlot.Visible", false);
					commandBuilder.set("#SelectedOutputName.Text", "Select a recipe to preview");
				}
			} else {
				commandBuilder.set("#SelectedOutputSlot.Visible", false);
				commandBuilder.set("#SelectedOutputName.Text", "Select a recipe to preview");
			}
		} else {
			commandBuilder.set("#SelectedOutputSlot.Visible", false);
			commandBuilder.set("#SelectedOutputName.Text", "Select a recipe to preview");
		}

		if (!CraftingPageSupport.updateCraftButtonLabel(commandBuilder, this.selectedRecipeId)) {
			this.selectedRecipeId = null;
		}
	}

	public static class SmithingPageEventData {
		private static final String KEY_ACTION = "Action";
		private static final String KEY_TIER = "Tier";
		private static final String KEY_RECIPE_ID = "RecipeId";

		public static final BuilderCodec<SmithingPageEventData> CODEC = BuilderCodec
				.builder(SmithingPageEventData.class, SmithingPageEventData::new)
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
