package org.runetale.skills.crafting;

import org.runetale.skills.config.CraftingConfig;
import org.runetale.skills.service.CraftingPageTrackerService;
import org.runetale.skills.service.CraftingRecipeTagService;

import javax.annotation.Nonnull;

public final class CraftingRuntimeState {

    private final CraftingConfig craftingConfig;
    private final CraftingRecipeTagService craftingRecipeTagService;
    private final CraftingPageTrackerService craftingPageTrackerService;

    public CraftingRuntimeState(
            @Nonnull CraftingConfig craftingConfig,
            @Nonnull CraftingRecipeTagService craftingRecipeTagService,
            @Nonnull CraftingPageTrackerService craftingPageTrackerService) {
        this.craftingConfig = craftingConfig;
        this.craftingRecipeTagService = craftingRecipeTagService;
        this.craftingPageTrackerService = craftingPageTrackerService;
    }

    @Nonnull
    public CraftingConfig craftingConfig() {
        return this.craftingConfig;
    }

    @Nonnull
    public CraftingRecipeTagService craftingRecipeTagService() {
        return this.craftingRecipeTagService;
    }

    @Nonnull
    public CraftingPageTrackerService craftingPageTrackerService() {
        return this.craftingPageTrackerService;
    }
}
