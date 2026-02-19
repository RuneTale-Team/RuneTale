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
import org.runetale.skills.config.CraftingConfig;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.domain.SmithingMaterialTier;
import org.runetale.skills.service.CraftingPageTrackerService;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

abstract class AbstractTimedCraftingPage<TEventData extends TimedCraftingEventData>
		extends InteractiveCustomUIPage<TEventData> {

	private final BlockPosition blockPosition;
	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final CraftingRecipeTagService craftingRecipeTagService;
	private final CraftingPageTrackerService craftingPageTrackerService;

	private final String uiPath;
	private final String benchContextName;
	private final String progressVerb;
	private final long craftDurationMillis;

	@Nonnull
	private SmithingMaterialTier selectedTier = SmithingMaterialTier.BRONZE;

	@Nullable
	private String selectedRecipeId;

	@Nonnull
	private final TimedCraftingPageState craftingState;

	AbstractTimedCraftingPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull BlockPosition blockPosition,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull CraftingRecipeTagService craftingRecipeTagService,
			@Nonnull CraftingPageTrackerService craftingPageTrackerService,
			@Nonnull CraftingConfig craftingConfig,
			@Nonnull String uiPath,
			@Nonnull String benchContextName,
			@Nonnull String progressVerb,
			long craftDurationMillis,
			@Nonnull com.hypixel.hytale.codec.builder.BuilderCodec<TEventData> codec) {
		super(playerRef, CustomPageLifetime.CanDismiss, codec);
		this.blockPosition = blockPosition;
		this.profileComponentType = profileComponentType;
		this.craftingRecipeTagService = craftingRecipeTagService;
		this.craftingPageTrackerService = craftingPageTrackerService;
		this.uiPath = uiPath;
		this.benchContextName = benchContextName;
		this.progressVerb = progressVerb;
		this.craftDurationMillis = craftDurationMillis;
		this.craftingState = new TimedCraftingPageState(
				craftingConfig.maxCraftCount(),
				craftingConfig.quantityAllToken(),
				craftingConfig.quantityPresets());
	}

	@Override
	public final void build(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			@Nonnull Store<EntityStore> store) {
		this.craftingPageTrackerService.trackOpenPage(this.playerRef.getUuid(), ref);
		CraftingPageSupport.initializeBenchBinding(ref, store, this.blockPosition, getLogger(), this.benchContextName);
		commandBuilder.append(this.uiPath);
		CraftingPageSupport.bindTierTabs(eventBuilder, availableTiers());
		CraftingPageSupport.bindQuantityControls(eventBuilder, this.craftingState);
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
		boolean shouldRerender = false;

		if ("Craft".equalsIgnoreCase(data.action)) {
			shouldRerender = startCraft(ref, store) || shouldRerender;
		}

		if (!this.craftingState.isCraftingInProgress() && data.recipeId != null) {
			if (!data.recipeId.equals(this.selectedRecipeId)) {
				this.selectedRecipeId = data.recipeId;
				shouldRerender = true;
			}
		}

		if (!this.craftingState.isCraftingInProgress() && data.tier != null) {
			SmithingMaterialTier parsed = CraftingPageSupport.parseTier(data.tier);
			if (parsed != null) {
				if (this.selectedTier != parsed || this.selectedRecipeId != null) {
					this.selectedTier = parsed;
					this.selectedRecipeId = null;
					shouldRerender = true;
				}
			}
		}

		if (!this.craftingState.isCraftingInProgress() && data.quantity != null) {
			int previousQuantity = this.craftingState.getSelectedCraftQuantity();
			boolean previousAll = this.craftingState.isCraftAllSelected();
			this.craftingState.applyQuantitySelection(data.quantity);
			if (previousQuantity != this.craftingState.getSelectedCraftQuantity()
					|| previousAll != this.craftingState.isCraftAllSelected()) {
				shouldRerender = true;
			}
		}

		if (!this.craftingState.isCraftingInProgress() && "SetQuantity".equalsIgnoreCase(data.action)) {
			int previousQuantity = this.craftingState.getSelectedCraftQuantity();
			boolean previousAll = this.craftingState.isCraftAllSelected();
			this.craftingState.applyCustomQuantity(data.quantityInput);
			if (previousQuantity != this.craftingState.getSelectedCraftQuantity()
					|| previousAll != this.craftingState.isCraftAllSelected()) {
				shouldRerender = true;
			}
		}

		if (!shouldRerender) {
			return;
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		UIEventBuilder eventBuilder = new UIEventBuilder();
		renderPage(ref, store, commandBuilder, eventBuilder);
		this.sendUpdate(commandBuilder, eventBuilder, false);
	}

	@Override
	public final void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		this.craftingPageTrackerService.untrackOpenPage(this.playerRef.getUuid());
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

	protected final boolean startCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		if (this.craftingState.isCraftingInProgress() || this.selectedRecipeId == null) {
			return false;
		}

		CraftingRecipe selectedRecipe = CraftingPageSupport.resolveRecipe(this.selectedRecipeId);
		if (selectedRecipe == null) {
			return false;
		}

		PlayerSkillProfileComponent profile = store.getComponent(ref, this.profileComponentType);
		Player player = store.getComponent(ref, Player.getComponentType());
		if (profile == null || player == null) {
			return false;
		}

		int smithingLevel = profile.getLevel(SkillType.SMITHING);
		List<SkillRequirement> requirements = this.craftingRecipeTagService.getSkillRequirements(selectedRecipe);
		int requiredLevel = CraftingPageSupport.getSmithingRequiredLevel(requirements);
		if (smithingLevel < requiredLevel || !CraftingPageSupport.hasRequiredMaterials(player, selectedRecipe)) {
			return false;
		}

		int targetCraftCount = this.craftingState.isCraftAllSelected()
				? CraftingPageSupport.getMaxCraftableCount(player, selectedRecipe, this.craftingState.getMaxCraftCount())
				: this.craftingState.getSelectedCraftQuantity();
		targetCraftCount = Math.max(0, targetCraftCount);
		if (targetCraftCount <= 0) {
			return false;
		}

		this.craftingState.startCrafting(selectedRecipe.getId(), targetCraftCount, this.craftDurationMillis);
		return true;
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

	@Nonnull
	protected List<SmithingMaterialTier> availableTiers() {
		return List.of(SmithingMaterialTier.values());
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
