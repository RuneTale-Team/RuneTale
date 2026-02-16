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
import org.runetale.skills.config.CraftingConfig;
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

	private final CraftingConfig craftingConfig;

	public SmeltingPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull BlockPosition blockPosition,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService,
			@Nonnull CraftingConfig craftingConfig) {
		super(
				playerRef,
				blockPosition,
				profileComponentType,
				craftingRecipeTagService,
				craftingConfig,
				UI_PATH,
				"smelting",
				"Smelting",
				craftingConfig.smeltingCraftDurationMillis(),
				SmeltingPageEventData.CODEC);
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

		commandBuilder.set("#TierBronze.Text", "Bars");
		commandBuilder.set("#TierBronzeIndicator.Visible", true);
		commandBuilder.set("#TierBronzeSelected.Visible", true);

		int selectedCraftQuantity = CraftingPageSupport.syncQuantityControls(
				commandBuilder,
				craftingState(),
				player,
				selectedRecipeId());

		commandBuilder.set("#SectionTitle.Text", "Bars");

		commandBuilder.clear("#RecipeList");
		List<CraftingRecipe> recipes = getBarRecipes();

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
			int outputQuantity = CraftingPageSupport.getPrimaryOutputQuantity(recipe);
			commandBuilder.set(selector + " #RecipeOutputQuantity.Text", outputQuantity > 1 ? "x" + outputQuantity : "");
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
				commandBuilder.set(selector + " #MissingMaterialsOutline.Visible", false);
			} else if (!hasMaterials) {
				commandBuilder.set(selector + " #RecipeStatus.Text", "Unlocked");
				commandBuilder.set(selector + " #RecipeStatus.Style.TextColor", "#99afc6");
				commandBuilder.set(selector + " #LockOverlay.Visible", false);
				commandBuilder.set(selector + " #MissingMaterialsOutline.Visible", true);
			} else {
				commandBuilder.set(selector + " #RecipeStatus.Text", "Unlocked");
				commandBuilder.set(selector + " #RecipeStatus.Style.TextColor", "#99afc6");
				commandBuilder.set(selector + " #LockOverlay.Visible", false);
				commandBuilder.set(selector + " #MissingMaterialsOutline.Visible", false);
			}
		}

		if (recipes.isEmpty()) {
			commandBuilder.append("#RecipeList", RECIPE_ROW_TEMPLATE);
			commandBuilder.set("#RecipeList[0] #RecipeName.Text", "No recipes available");
			commandBuilder.set("#RecipeList[0] #RecipeIngredients.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeXp.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeStatus.Text", "");
			commandBuilder.set("#RecipeList[0] #RecipeOutputSlot.Visible", false);
			commandBuilder.set("#RecipeList[0] #RecipeOutputQuantity.Text", "");
			commandBuilder.set("#RecipeList[0] #IngredientSlot0.Visible", false);
			commandBuilder.set("#RecipeList[0] #IngredientSlot1.Visible", false);
			commandBuilder.set("#RecipeList[0] #IngredientSlot2.Visible", false);
			commandBuilder.set("#RecipeList[0] #SelectedFrame.Visible", false);
			commandBuilder.set("#RecipeList[0] #LockOverlay.Visible", false);
			commandBuilder.set("#RecipeList[0] #MissingMaterialsOutline.Visible", false);
		}

		CraftingRecipe selectedPreviewRecipe = CraftingPageSupport.resolveRecipe(selectedRecipeId());
		CraftingPageSupport.syncSelectedRecipePreview(commandBuilder, selectedPreviewRecipe);
		commandBuilder.set("#SelectedOutputName.Text", "");
		if (selectedPreviewRecipe == null) {
			commandBuilder.set("#SelectedOutputLargeName.Text", "");
		} else {
			commandBuilder.set("#SelectedOutputLargeName.Text", CraftingPageSupport.getRecipeOutputName(selectedPreviewRecipe));
		}

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
	@Override
	protected List<SmithingMaterialTier> availableTiers() {
		return List.of(SmithingMaterialTier.BRONZE);
	}

	@Nonnull
	private List<CraftingRecipe> getBarRecipes() {
		List<CraftingRecipe> allRecipes = CraftingPlugin.getBenchRecipes(BenchType.Crafting, this.craftingConfig.furnaceBenchId());
		List<CraftingRecipe> filtered = new ArrayList<>();

		for (CraftingRecipe recipe : allRecipes) {
			MaterialQuantity[] outputs = recipe.getOutputs();
			if (outputs != null) {
				for (MaterialQuantity output : outputs) {
					String itemId = output.getItemId();
					if (itemId != null && itemId.toLowerCase(Locale.ROOT)
							.contains(this.craftingConfig.smeltingOutputContainsToken().toLowerCase(Locale.ROOT))) {
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
