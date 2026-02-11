package org.runetale.skills.page;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
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

abstract class AbstractTimedCraftingPage<TEventData extends TimedCraftingEventData>
		extends InteractiveCustomUIPage<TEventData> {

	private final BlockPosition blockPosition;
	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;

	private final String uiPath;
	private final String benchContextName;
	private final String progressVerb;
	private final long craftDurationMillis;

	@Nonnull
	private SmithingMaterialTier selectedTier = SmithingMaterialTier.BRONZE;

	@Nullable
	private String selectedRecipeId;

	@Nonnull
	private final TimedCraftingPageState craftingState = new TimedCraftingPageState();

	AbstractTimedCraftingPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull BlockPosition blockPosition,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService,
			@Nonnull String uiPath,
			@Nonnull String benchContextName,
			@Nonnull String progressVerb,
			long craftDurationMillis,
			@Nonnull com.hypixel.hytale.codec.builder.BuilderCodec<TEventData> codec) {
		super(playerRef, CustomPageLifetime.CanDismiss, codec);
		this.blockPosition = blockPosition;
		this.profileComponentType = profileComponentType;
		this.craftingRecipeTagService = craftingRecipeTagService;
		this.uiPath = uiPath;
		this.benchContextName = benchContextName;
		this.progressVerb = progressVerb;
		this.craftDurationMillis = craftDurationMillis;
	}

	@Override
	public final void build(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			@Nonnull Store<EntityStore> store) {
		CraftingPageSupport.initializeBenchBinding(ref, store, this.blockPosition, getLogger(), this.benchContextName);
		commandBuilder.append(this.uiPath);
		CraftingPageSupport.bindTierTabs(eventBuilder);
		CraftingPageSupport.bindQuantityControls(eventBuilder);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				"#StartCraftingButton",
				EventData.of(TimedCraftingEventData.KEY_ACTION, "Craft"),
				false);
		renderPage(ref, store, commandBuilder, eventBuilder);
	}

	@Override
	public final void handleDataEvent(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull TEventData data) {
		if ("Craft".equalsIgnoreCase(data.action)) {
			startCraft(ref, store);
		}

		if (!this.craftingState.isCraftingInProgress() && data.recipeId != null) {
			this.selectedRecipeId = data.recipeId;
		}

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
		renderPage(ref, store, commandBuilder, eventBuilder);
		this.sendUpdate(commandBuilder, eventBuilder, false);
	}

	@Override
	public final void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		this.craftingState.reset();
		CraftingPageSupport.clearBenchBinding(ref, store);
	}

	public final void tickProgress(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, float deltaTime) {
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
			CraftingRecipe recipe = CraftingPageSupport.resolveRecipe(recipeId);
			Player player = store.getComponent(ref, Player.getComponentType());
			boolean canContinue = crafted && recipe != null && CraftingPageSupport.hasRequiredMaterials(player, recipe);
			this.craftingState.handleCraftStepResult(crafted, canContinue, this.craftDurationMillis);

			UICommandBuilder commandBuilder = new UICommandBuilder();
			UIEventBuilder eventBuilder = new UIEventBuilder();
			renderPage(ref, store, commandBuilder, eventBuilder);
			this.sendUpdate(commandBuilder, eventBuilder, false);
			return;
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		appendProgressUi(commandBuilder);
		this.sendUpdate(commandBuilder, false);
	}

	protected final void startCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
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

		this.craftingState.startCrafting(selectedRecipe.getId(), targetCraftCount, this.craftDurationMillis);
	}

	protected final void appendProgressUi(@Nonnull UICommandBuilder commandBuilder) {
		float progress = this.craftingState.isCraftingInProgress() ? this.craftingState.getProgress() : 0.0F;
		int progressPercent = this.craftingState.getProgressPercent();
		int currentCraftIndex = this.craftingState.getCurrentCraftIndex();
		int totalCraftCount = this.craftingState.getTotalCraftCount();
		commandBuilder.set("#CraftProgressBar.Value", progress);
		commandBuilder.set(
				"#CraftProgressLabel.Text",
				this.craftingState.isCraftingInProgress()
						? String.format(java.util.Locale.ROOT, "%s... %d%% (%d/%d)", this.progressVerb, progressPercent, currentCraftIndex, totalCraftCount)
						: "Ready");
	}

	@Nonnull
	protected final SmithingMaterialTier selectedTier() {
		return this.selectedTier;
	}

	protected final void clearSelectedRecipe() {
		this.selectedRecipeId = null;
	}

	@Nullable
	protected final String selectedRecipeId() {
		return this.selectedRecipeId;
	}

	@Nonnull
	protected final TimedCraftingPageState craftingState() {
		return this.craftingState;
	}

	@Nonnull
	protected final CraftingRecipeTagService craftingRecipeTagService() {
		return this.craftingRecipeTagService;
	}

	@Nonnull
	protected final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType() {
		return this.profileComponentType;
	}

	protected abstract void renderPage(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder);

	protected abstract boolean finishCraft(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull String recipeId);

	@Nonnull
	protected abstract HytaleLogger getLogger();
}
