package org.runetale.skills.page;

import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom UI page for smelting at the RuneTale furnace.
 */
public class SmeltingPage extends AbstractTimedCraftingPage<SmeltingPage.SmeltingPageEventData> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String UI_PATH = "SkillsPlugin/Smelting.ui";
	private static final String RECIPE_ROW_TEMPLATE = "SkillsPlugin/SmeltingRecipeRow.ui";
	private static final String BENCH_ID = "RuneTale_Furnace";
	private static final long CRAFT_DURATION_MILLIS = 3000L;

	public SmeltingPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull BlockPosition blockPosition,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService) {
		super(
				playerRef,
				blockPosition,
				profileComponentType,
				craftingRecipeTagService,
				UI_PATH,
				"smelting",
				"Smelting",
				CRAFT_DURATION_MILLIS,
				SmeltingPageEventData.CODEC);
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
				profileComponentType(),
				craftingRecipeTagService(),
				LOGGER,
				"Smelted",
				"smelt");
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
		PlayerSkillProfileComponent profile = store.getComponent(ref, profileComponentType());
		Player player = store.getComponent(ref, Player.getComponentType());
		int smithingLevel = profile == null ? 1 : profile.getLevel(SkillType.SMITHING);

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

		commandBuilder.set("#SectionTitle.Text", selectedTier().getSectionTitle("Bars"));

		commandBuilder.clear("#RecipeList");
		List<CraftingRecipe> recipes = getRecipesForTier(selectedTier());

		for (int i = 0; i < recipes.size(); i++) {
			CraftingRecipe recipe = recipes.get(i);
			String selector = "#RecipeList[" + i + "]";

			commandBuilder.append("#RecipeList", RECIPE_ROW_TEMPLATE);

			boolean isSelected = recipe.getId().equals(selectedRecipeId());
			commandBuilder.set(selector + " #SelectedFrame.Visible", isSelected);

			eventBuilder.addEventBinding(
					com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType.Activating,
					selector + " #SelectBtn",
					EventData.of(TimedCraftingEventData.KEY_RECIPE_ID, recipe.getId()),
					false);

			commandBuilder.set(selector + " #RecipeName.TextSpans", CraftingPageSupport.getRecipeOutputLabel(recipe));
			CraftingPageSupport.configureOutputSlot(commandBuilder, selector + " #RecipeOutputSlot", recipe);
			CraftingPageSupport.configureIngredientSlots(commandBuilder, selector, recipe);
			commandBuilder.set(selector + " #RecipeIngredients.TextSpans", CraftingPageSupport.formatIngredientsLabel(recipe));
			commandBuilder.set(selector + " #RecipeXp.Text", CraftingPageSupport.getXpText(recipe, craftingRecipeTagService()));

			List<SkillRequirement> requirements = craftingRecipeTagService().getSkillRequirements(recipe);
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

		CraftingRecipe selectedPreviewRecipe = CraftingPageSupport.resolveRecipe(selectedRecipeId());
		CraftingPageSupport.syncSelectedRecipePreview(commandBuilder, selectedPreviewRecipe);

		CraftingRecipe selectedRecipe = CraftingPageSupport.resolveRecipe(selectedRecipeId());
		boolean selectedUnlocked = false;
		if (selectedRecipe != null) {
			int selectedRequiredLevel = CraftingPageSupport.getSmithingRequiredLevel(craftingRecipeTagService().getSkillRequirements(selectedRecipe));
			selectedUnlocked = smithingLevel >= selectedRequiredLevel;
		}
		boolean canCraftSelected = selectedRecipe != null
				&& selectedUnlocked
				&& CraftingPageSupport.hasRequiredMaterials(player, selectedRecipe);
		if (craftingState().isCraftingInProgress()) {
			commandBuilder.set("#StartCraftingButton.Text", "Smelting...");
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

	public static class SmeltingPageEventData extends TimedCraftingEventData {
		public static final com.hypixel.hytale.codec.builder.BuilderCodec<SmeltingPageEventData> CODEC =
				TimedCraftingEventData.createCodec(SmeltingPageEventData.class, SmeltingPageEventData::new);
	}
}
