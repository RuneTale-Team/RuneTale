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
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom UI page for all fletching stations (knife, bowstring, arrow shaft, headless arrow, spinning wheel).
 *
 * <p>
 * A single tabless page configurable per station via {@code stationTitle}, {@code benchId}, and {@code categories}.
 */
public class FletchingPage extends AbstractTimedCraftingPage<FletchingPage.FletchingPageEventData> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String UI_PATH = "SkillsPlugin/Fletching.ui";
	private static final String RECIPE_CARD_TEMPLATE = "SkillsPlugin/SmithingRecipeGridCard.ui";
	private static final String CARD_ROW_INLINE = "Group { LayoutMode: Left; Anchor: (Bottom: 10); }";
	private static final String CARD_COLUMN_SPACER_INLINE = "Group { Anchor: (Width: 10); }";

	private final String stationTitle;
	private final String benchId;
	private final List<String> categories;

	public FletchingPage(
			@Nonnull PlayerRef playerRef,
			@Nullable BlockPosition blockPosition,
			@Nonnull SkillsRuntimeApi runtimeApi,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService,
			@Nonnull CraftingPageTrackerService craftingPageTrackerService,
			@Nonnull CraftingConfig craftingConfig,
			@Nonnull String stationTitle,
			@Nonnull String benchId,
			@Nonnull List<String> categories) {
		super(
				playerRef,
				blockPosition,
				runtimeApi,
				craftingRecipeTagService,
				craftingPageTrackerService,
				craftingConfig,
				UI_PATH,
				"fletching",
				"Fletching",
				craftingConfig.fletchingCraftDurationMillis(),
				SkillType.FLETCHING,
				FletchingPageEventData.CODEC);
		this.stationTitle = stationTitle;
		this.benchId = benchId;
		this.categories = List.copyOf(categories);
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
				"Fletched",
				"fletch",
				SkillType.FLETCHING);
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
		int fletchingLevel = runtimeApi().getSkillLevel(store, ref, SkillType.FLETCHING);

		int selectedCraftQuantity = CraftingPageSupport.syncQuantityControls(
				commandBuilder,
				craftingState(),
				player,
				selectedRecipeId());

		commandBuilder.set("#SectionTitle.Text", this.stationTitle);

		commandBuilder.clear("#RecipeList");
		List<CraftingRecipe> recipes = getRecipes();

		for (int i = 0; i < recipes.size(); i++) {
			CraftingRecipe recipe = recipes.get(i);

			int row = i / 2;
			int col = i % 2;
			if (col == 0) {
				commandBuilder.appendInline("#RecipeList", CARD_ROW_INLINE);
			} else {
				commandBuilder.appendInline("#RecipeList[" + row + "]", CARD_COLUMN_SPACER_INLINE);
			}

			int uiCol = col == 0 ? 0 : 2;
			String cardSelector = "#RecipeList[" + row + "][" + uiCol + "]";
			commandBuilder.append("#RecipeList[" + row + "]", RECIPE_CARD_TEMPLATE);

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
			int requiredLevel = CraftingPageSupport.getRequiredLevel(requirements, SkillType.FLETCHING);
			boolean unlocked = fletchingLevel >= requiredLevel;
			boolean hasMaterials = CraftingPageSupport.hasRequiredMaterials(player, recipe);
			boolean requirementsMet = unlocked && hasMaterials;

			if (!unlocked) {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Requires Lv " + requiredLevel + " Fletching");
				commandBuilder.set(cardSelector + " #CardStatus.Style.TextColor", "#d7a6a6");
			} else if (!hasMaterials) {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Materials Required");
				commandBuilder.set(cardSelector + " #CardStatus.Style.TextColor", "#d7a6a6");
			} else {
				commandBuilder.set(cardSelector + " #CardStatus.Text", "Unlocked");
				commandBuilder.set(cardSelector + " #CardStatus.Style.TextColor", "#99afc6");
			}
			commandBuilder.set(cardSelector + " #LockOverlay.Visible", !unlocked);
			commandBuilder.set(cardSelector + " #RequirementDimOverlay.Visible", !requirementsMet);
			commandBuilder.set(cardSelector + " #MissingMaterialsOutline.Visible", !requirementsMet);
		}

		if (recipes.isEmpty()) {
			commandBuilder.appendInline("#RecipeList", CARD_ROW_INLINE);
			commandBuilder.append("#RecipeList[0]", RECIPE_CARD_TEMPLATE);
			commandBuilder.set("#RecipeList[0][0] #CardName.Text", "No recipes available");
			commandBuilder.set("#RecipeList[0][0] #CardIngredients.Text", "");
			commandBuilder.set("#RecipeList[0][0] #CardXp.Text", "");
			commandBuilder.set("#RecipeList[0][0] #CardStatus.Text", "");
			commandBuilder.set("#RecipeList[0][0] #CardOutputSlot.Visible", false);
			commandBuilder.set("#RecipeList[0][0] #CardOutputQuantity.Text", "");
			commandBuilder.set("#RecipeList[0][0] #IngredientSlot0.Visible", false);
			commandBuilder.set("#RecipeList[0][0] #IngredientSlot1.Visible", false);
			commandBuilder.set("#RecipeList[0][0] #IngredientSlot2.Visible", false);
			commandBuilder.set("#RecipeList[0][0] #SelectedFrame.Visible", false);
			commandBuilder.set("#RecipeList[0][0] #LockOverlay.Visible", false);
			commandBuilder.set("#RecipeList[0][0] #MissingMaterialsOutline.Visible", false);
		}

		CraftingRecipe selectedRecipe = CraftingPageSupport.resolveRecipe(selectedRecipeId());
		CraftingPageSupport.syncSelectedRecipePreview(commandBuilder, selectedRecipe);

		boolean selectedUnlocked = false;
		if (selectedRecipe != null) {
			int selectedRequiredLevel = CraftingPageSupport.getRequiredLevel(
					craftingRecipeTagService().getSkillRequirements(selectedRecipe), SkillType.FLETCHING);
			selectedUnlocked = fletchingLevel >= selectedRequiredLevel;
		}
		boolean canCraftSelected = selectedRecipe != null
				&& selectedUnlocked
				&& CraftingPageSupport.hasRequiredMaterials(player, selectedRecipe);
		if (craftingState().isCraftingInProgress()) {
			commandBuilder.set("#StartCraftingButton.Text", "Fletching...");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
		} else if (selectedRecipe != null && !selectedUnlocked) {
			commandBuilder.set("#StartCraftingButton.Text", "Level Required");
			commandBuilder.set("#StartCraftingButton.Disabled", true);
		} else if (!CraftingPageSupport.updateCraftButtonState(commandBuilder, selectedRecipe, canCraftSelected, selectedCraftQuantity)) {
			clearSelectedRecipe();
		}

		appendProgressUi(commandBuilder);
	}

	@Nonnull
	@Override
	protected List<SmithingMaterialTier> availableTiers() {
		return List.of();
	}

	@Nonnull
	private List<CraftingRecipe> getRecipes() {
		List<CraftingRecipe> recipes = new ArrayList<>();
		for (String category : this.categories) {
			List<CraftingRecipe> categoryRecipes = CraftingPlugin.getBenchRecipes(
					BenchType.Crafting, this.benchId, category);
			recipes.addAll(categoryRecipes);
		}
		recipes.sort(Comparator.comparingInt(recipe ->
				CraftingPageSupport.getRequiredLevel(craftingRecipeTagService().getSkillRequirements(recipe), SkillType.FLETCHING)));
		return recipes;
	}

	public static class FletchingPageEventData extends TimedCraftingEventData {
		public static final com.hypixel.hytale.codec.builder.BuilderCodec<FletchingPageEventData> CODEC =
				TimedCraftingEventData.createCodec(FletchingPageEventData.class, FletchingPageEventData::new);
	}
}
