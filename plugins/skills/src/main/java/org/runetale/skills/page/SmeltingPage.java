package org.runetale.skills.page;

import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.builtin.crafting.state.BenchState;
import com.hypixel.hytale.builtin.crafting.window.SimpleCraftingWindow;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SmithingMaterialTier;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom UI page for smelting at the RuneTale furnace.
 *
 * <p>
 * Displays recipes organized by material tier tabs. Locked recipes
 * (insufficient smithing level) appear dimmed with requirement labels.
 * "Start Crafting" opens the native bench window for actual crafting execution.
 */
public class SmeltingPage extends InteractiveCustomUIPage<SmeltingPage.SmeltingPageEventData> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static final String UI_PATH = "SkillsPlugin/Smelting.ui";
	private static final String RECIPE_ROW_TEMPLATE = "SkillsPlugin/SmeltingRecipeRow.ui";
	private static final String BENCH_ID = "RuneTale_Furnace";

	private final BlockPosition blockPosition;
	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;

	@Nonnull
	private SmithingMaterialTier selectedTier = SmithingMaterialTier.BRONZE;

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
		commandBuilder.append(UI_PATH);
		bindTierTabs(eventBuilder);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#StartCraftingButton",
				EventData.of("Action", "StartCrafting"),
				false);
		render(ref, store, commandBuilder, eventBuilder);
	}

	@Override
	public void handleDataEvent(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull SmeltingPageEventData data) {
		if ("StartCrafting".equalsIgnoreCase(data.action)) {
			openNativeBench(ref, store);
			return;
		}

		if (data.tier != null) {
			SmithingMaterialTier parsed = parseTier(data.tier);
			if (parsed != null) {
				this.selectedTier = parsed;
			}
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		UIEventBuilder eventBuilder = new UIEventBuilder();
		render(ref, store, commandBuilder, eventBuilder);
		this.sendUpdate(commandBuilder, eventBuilder, false);
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

			// Output name
			String outputName = getRecipeOutputName(recipe);
			commandBuilder.set(selector + " #RecipeName.Text", outputName);

			// Ingredients
			String ingredients = formatIngredients(recipe);
			commandBuilder.set(selector + " #RecipeIngredients.Text", ingredients);

			// XP reward
			Optional<Double> xpReward = this.craftingRecipeTagService.getXpOnSuccessfulCraft(recipe);
			String xpText = xpReward.map(xp -> "+" + String.format(Locale.ROOT, "%.1f", xp) + " XP")
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
		List<CraftingRecipe> allRecipes = CraftingPlugin.getBenchRecipes(BenchType.Crafting, BENCH_ID);
		List<CraftingRecipe> filtered = new ArrayList<>();
		String barSubstring = tier.getBarSubstring();

		for (CraftingRecipe recipe : allRecipes) {
			MaterialQuantity[] outputs = recipe.getOutputs();
			if (outputs != null) {
				for (MaterialQuantity output : outputs) {
					String itemId = output.getItemId();
					if (itemId != null && itemId.contains(barSubstring)) {
						filtered.add(recipe);
						break;
					}
				}
			}
		}

		return filtered;
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
		// Strip mod prefix (e.g. "RuneTale.Surface_Ores:Bronze_Bar" -> "Bronze Bar")
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

	private void openNativeBench(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		CraftingManager craftingManager = store.getComponent(ref, CraftingManager.getComponentType());
		if (craftingManager == null) {
			LOGGER.atWarning().log("CraftingManager not found on player entity; cannot open bench");
			return;
		}

		if (craftingManager.hasBenchSet()) {
			return;
		}

		World world = store.getExternalData().getWorld();
		if (!(world.getState(blockPosition.x, blockPosition.y, blockPosition.z, true) instanceof BenchState benchState)) {
			LOGGER.atWarning().log("No BenchState at %d,%d,%d", blockPosition.x, blockPosition.y, blockPosition.z);
			return;
		}

		SimpleCraftingWindow window = new SimpleCraftingWindow(benchState);
		UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
		if (uuidComponent == null) {
			return;
		}

		UUID uuid = uuidComponent.getUuid();
		if (benchState.getWindows().putIfAbsent(uuid, window) == null) {
			window.registerCloseEvent(event -> benchState.getWindows().remove(uuid, window));
		}

		player.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, window);
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

		public static final BuilderCodec<SmeltingPageEventData> CODEC = BuilderCodec
				.builder(SmeltingPageEventData.class, SmeltingPageEventData::new)
				.append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (entry, value) -> entry.action = value, entry -> entry.action)
				.add()
				.append(new KeyedCodec<>(KEY_TIER, Codec.STRING), (entry, value) -> entry.tier = value, entry -> entry.tier)
				.add()
				.build();

		private String action;
		private String tier;
	}
}
