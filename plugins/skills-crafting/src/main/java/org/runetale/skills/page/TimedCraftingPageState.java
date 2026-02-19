package org.runetale.skills.page;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

final class TimedCraftingPageState {

	private final int maxCraftCount;
	private final String quantityAllToken;
	private final List<Integer> quantityPresets;

	private int selectedCraftQuantity = 1;
	private boolean craftAllSelected;

	private int queuedCraftCount;
	private int totalCraftCount;

	private boolean craftingInProgress;
	private long craftingStartedAtMillis;
	private long craftingDurationMillis;
	private int lastProgressPercent = -1;

	@Nullable
	private String craftingRecipeId;

	TimedCraftingPageState(int maxCraftCount, @Nonnull String quantityAllToken, @Nonnull List<Integer> quantityPresets) {
		this.maxCraftCount = Math.max(1, maxCraftCount);
		this.quantityAllToken = quantityAllToken;
		this.quantityPresets = quantityPresets.isEmpty() ? List.of(1, 5, 10) : List.copyOf(quantityPresets);
		this.selectedCraftQuantity = this.quantityPresets.get(0);
	}

	void reset() {
		this.craftingInProgress = false;
		this.craftingRecipeId = null;
		this.lastProgressPercent = -1;
		this.queuedCraftCount = 0;
		this.totalCraftCount = 0;
		this.craftAllSelected = false;
	}

	boolean isCraftingInProgress() {
		return this.craftingInProgress;
	}

	@Nullable
	String getCraftingRecipeId() {
		return this.craftingRecipeId;
	}

	boolean isCraftAllSelected() {
		return this.craftAllSelected;
	}

	int getSelectedCraftQuantity() {
		return this.selectedCraftQuantity;
	}

	int getMaxCraftCount() {
		return this.maxCraftCount;
	}

	@Nonnull
	String getQuantityAllToken() {
		return this.quantityAllToken;
	}

	@Nonnull
	List<Integer> getQuantityPresets() {
		return this.quantityPresets;
	}

	void setDisplayedCraftQuantity(int craftQuantity) {
		this.selectedCraftQuantity = Math.max(1, Math.min(this.maxCraftCount, craftQuantity));
	}

	void applyQuantitySelection(@Nonnull String quantityRaw) {
		if (this.quantityAllToken.equalsIgnoreCase(quantityRaw)) {
			this.craftAllSelected = true;
			return;
		}

		int parsed = parseCraftQuantity(quantityRaw);
		if (parsed > 0) {
			this.selectedCraftQuantity = parsed;
			this.craftAllSelected = false;
		}
	}

	void applyCustomQuantity(@Nullable String quantityInputRaw) {
		if (quantityInputRaw == null) {
			return;
		}

		int parsed = parseCraftQuantity(quantityInputRaw);
		if (parsed > 0) {
			this.selectedCraftQuantity = parsed;
			this.craftAllSelected = false;
		}
	}

	void startCrafting(@Nonnull String recipeId, int craftCount, long durationMillis) {
		int normalizedCount = Math.max(1, craftCount);
		this.craftingInProgress = true;
		this.craftingRecipeId = recipeId;
		this.craftingStartedAtMillis = System.currentTimeMillis();
		this.craftingDurationMillis = Math.max(1L, durationMillis);
		this.lastProgressPercent = -1;
		this.totalCraftCount = normalizedCount;
		this.queuedCraftCount = normalizedCount;
	}

	float getProgress() {
		if (!this.craftingInProgress || this.craftingDurationMillis <= 0L) {
			return 0.0F;
		}

		long elapsed = Math.max(0L, System.currentTimeMillis() - this.craftingStartedAtMillis);
		return Math.min(1.0F, elapsed / (float) this.craftingDurationMillis);
	}

	int getProgressPercent() {
		float progress = getProgress();
		return Math.min(100, Math.max(0, (int) Math.floor(progress * 100.0F)));
	}

	boolean shouldEmitProgressUpdate() {
		if (!this.craftingInProgress) {
			return false;
		}

		int progressPercent = getProgressPercent();
		if (progressPercent == this.lastProgressPercent && progressPercent < 100) {
			return false;
		}

		this.lastProgressPercent = progressPercent;
		return true;
	}

	boolean isCurrentCraftFinished() {
		return this.craftingInProgress && getProgress() >= 1.0F;
	}

	@Nullable
	String completeCurrentCraftStep() {
		if (!this.craftingInProgress || this.craftingRecipeId == null) {
			return null;
		}

		String recipeId = this.craftingRecipeId;
		this.craftingInProgress = false;
		this.lastProgressPercent = -1;
		return recipeId;
	}

	void handleCraftStepResult(boolean crafted, boolean canContinue, long durationMillis) {
		if (crafted) {
			this.queuedCraftCount = Math.max(0, this.queuedCraftCount - 1);
		} else {
			this.queuedCraftCount = 0;
		}

		if (crafted && this.queuedCraftCount > 0 && this.craftingRecipeId != null && canContinue) {
			this.craftingInProgress = true;
			this.craftingStartedAtMillis = System.currentTimeMillis();
			this.craftingDurationMillis = Math.max(1L, durationMillis);
			this.lastProgressPercent = -1;
			return;
		}

		this.craftingInProgress = false;
		this.craftingRecipeId = null;
		this.queuedCraftCount = 0;
		this.totalCraftCount = 0;
	}

	int getCurrentCraftIndex() {
		if (this.totalCraftCount <= 0) {
			return 1;
		}

		return Math.max(1, (this.totalCraftCount - this.queuedCraftCount) + 1);
	}

	int getTotalCraftCount() {
		return this.totalCraftCount;
	}

	private int parseCraftQuantity(@Nonnull String raw) {
		try {
			int value = Integer.parseInt(raw.trim());
			if (value > 0) {
				return Math.min(this.maxCraftCount, value);
			}
		} catch (NumberFormatException ignored) {
		}
		return -1;
	}
}
