package org.runetale.skills.page;

import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.CraftingConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.SmithingMaterialTier;
import org.runetale.skills.service.CraftingPageTrackerService;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom UI page for smithing at the RuneTale anvil.
 */
public class SmithingPage extends AbstractTimedCraftingPage<SmithingPage.SmithingPageEventData> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String UI_PATH = "SkillsPlugin/Smithing.ui";
	private static final String RECIPE_CARD_TEMPLATE = "SkillsPlugin/SmithingRecipeGridCard.ui";
	private static final String CARD_ROW_INLINE = "Group { LayoutMode: Left; Anchor: (Bottom: 10); }";
	private static final String CARD_COLUMN_SPACER_INLINE = "Group { Anchor: (Width: 10); }";

	private final CraftingConfig craftingConfig;

	public SmithingPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull BlockPosition blockPosition,
			@Nonnull SkillsRuntimeApi runtimeApi,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService,
			@Nonnull CraftingPageTrackerService craftingPageTrackerService,
			@Nonnull CraftingConfig craftingConfig) {
		super(
				playerRef,
				blockPosition,
				runtimeApi,
				craftingRecipeTagService,
				craftingPageTrackerService,
				craftingConfig,
				UI_PATH,
				"smithing",
				"Smithing",
				craftingConfig.smithingCraftDurationMillis(),
				SmithingPageEventData.CODEC);
		this.craftingConfig = craftingConfig;
	}

	@Override
	protected boolean finishCraft(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull String recipeId) {
		return CraftingPageSupport.executeCraft(
				ref,
				store,
				recipeId,
				runtimeApi(),
				craftingRecipeTagService(),
				LOGGER,
				"Crafted",
				"smith");
	}

	@Override
	protected @Nonnull HytaleLogger getLogger() {
		return LOGGER;
	}

	@Override
	protected void renderPage(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		Player player = store.getComponent(ref, Player.getComponentType());
		int smithingLevel = runtimeApi().getSkillLevel(store, ref, SkillType.SMITHING);

		for (SmithingMaterialTier tier : SmithingMaterialTier.values()) {
			String selector = "#Tier" + tier.getDisplayName();
			boolean selected = tier == selectedTier();
			commandBuilder.set(selector + ".Text", tier.getDisplayName());
			commandBuilder.set(selector + "Indicator.Visible", selected);
			commandBuilder.set(selector + "Selected.Visible", selected);
		}

		int selectedCraftQuantity = CraftingPageSupport.syncQuantityControls(
				commandBuilder,
				craftingState(),
				player,
				selectedRecipeId());

		commandBuilder.set("#SectionTitle.Text", selectedTier().getSectionTitle("Equipment"));

		commandBuilder.clear("#RecipeGrid");
		List<CraftingRecipe> recipes = new ArrayList<>(CraftingPlugin.getBenchRecipes(
				BenchType.Crafting, this.craftingConfig.anvilBenchId(), selectedTier().getAnvilCategory()));
		recipes.sort(Comparator.comparingInt(recipe ->
				CraftingPageSupport.getSmithingRequiredLevel(craftingRecipeTagService().getSkillRequirements(recipe))));

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

			boolean isSelected = recipe.getId().equals(selectedRecipeId());
			commandBuilder.set(cardSelector + " #SelectedFrame.Visible", isSelected);

			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					cardSelector + " #SelectBtn",
					EventData.of(TimedCraftingEventData.KEY_RECIPE_ID, recipe.getId()),
					false);

			commandBuilder.set(cardSelector + " #CardName.TextSpans", CraftingPageSupport.getRecipeOutputLabel(recipe));
			CraftingPageSupport.configureOutputSlot(commandBuilder, cardSelector + " #CardOutputSlot", recipe);
			int outputQuantity = CraftingPageSupport.getPrimaryOutputQuantity(recipe);
			commandBuilder.set(cardSelector + " #CardOutputQuantity.Text", outputQuantity > 1 ? "x" + outputQuantity : "");
			CraftingPageSupport.configureIngredientSlots(commandBuilder, cardSelector, recipe);
			commandBuilder.set(cardSelector + " #CardIngredients.TextSpans", CraftingPageSupport.formatIngredientsLabel(recipe));
			commandBuilder.set(cardSelector + " #CardXp.Text", CraftingPageSupport.getXpText(recipe, craftingRecipeTagService()));

			List<SkillRequirement> requirements = craftingRecipeTagService().getSkillRequirements(recipe);
			int requiredLevel = CraftingPageSupport.getSmithingRequiredLevel(requirements);
			boolean unlocked = smithingLevel >= requiredLevel;
			boolean hasMaterials = CraftingPageSupport.hasRequiredMaterials(player, recipe);

			if (!unlocked) {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Requires Lv " + requiredLevel + " Smithing");
				commandBuilder.set(cardSelector + " #CardStatus.Style.TextColor", "#d7a6a6");
				commandBuilder.set(cardSelector + " #LockOverlay.Visible", true);
				commandBuilder.set(cardSelector + " #MissingMaterialsOutline.Visible", false);
			} else if (!hasMaterials) {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Unlocked");
				commandBuilder.set(cardSelector + " #CardStatus.Style.TextColor", "#99afc6");
				commandBuilder.set(cardSelector + " #LockOverlay.Visible", false);
				commandBuilder.set(cardSelector + " #MissingMaterialsOutline.Visible", true);
			} else {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Unlocked");
				commandBuilder.set(cardSelector + " #CardStatus.Style.TextColor", "#99afc6");
				commandBuilder.set(cardSelector + " #LockOverlay.Visible", false);
				commandBuilder.set(cardSelector + " #MissingMaterialsOutline.Visible", false);
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
			commandBuilder.set("#RecipeGrid[0][0] #CardOutputQuantity.Text", "");
			commandBuilder.set("#RecipeGrid[0][0] #IngredientSlot0.Visible", false);
			commandBuilder.set("#RecipeGrid[0][0] #IngredientSlot1.Visible", false);
			commandBuilder.set("#RecipeGrid[0][0] #IngredientSlot2.Visible", false);
			commandBuilder.set("#RecipeGrid[0][0] #SelectedFrame.Visible", false);
			commandBuilder.set("#RecipeGrid[0][0] #LockOverlay.Visible", false);
			commandBuilder.set("#RecipeGrid[0][0] #MissingMaterialsOutline.Visible", false);
		}

		CraftingRecipe selectedRecipe = CraftingPageSupport.resolveRecipe(selectedRecipeId());
		CraftingPageSupport.syncSelectedRecipePreview(commandBuilder, selectedRecipe);

		boolean selectedUnlocked = false;
		if (selectedRecipe != null) {
			int selectedRequiredLevel = CraftingPageSupport.getSmithingRequiredLevel(craftingRecipeTagService().getSkillRequirements(selectedRecipe));
			selectedUnlocked = smithingLevel >= selectedRequiredLevel;
		}
		boolean canCraftSelected = selectedRecipe != null
				&& selectedUnlocked
				&& CraftingPageSupport.hasRequiredMaterials(player, selectedRecipe);
		if (craftingState().isCraftingInProgress()) {
			commandBuilder.set("#StartCraftingButton.Text", "Smithing...");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
		} else if (selectedRecipe != null && !selectedUnlocked) {
			commandBuilder.set("#StartCraftingButton.Text", "Level Required");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
		} else if (!CraftingPageSupport.updateCraftButtonState(commandBuilder, selectedRecipe, canCraftSelected, selectedCraftQuantity)) {
			clearSelectedRecipe();
		}

		appendProgressUi(commandBuilder);
	}

	public static class SmithingPageEventData extends TimedCraftingEventData {
		public static final com.hypixel.hytale.codec.builder.BuilderCodec<SmithingPageEventData> CODEC =
				TimedCraftingEventData.createCodec(SmithingPageEventData.class, SmithingPageEventData::new);
	}
}
