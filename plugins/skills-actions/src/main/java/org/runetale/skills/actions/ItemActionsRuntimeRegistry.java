package org.runetale.skills.actions;

import org.runetale.skills.actions.service.ItemActionPlacementQueueService;
import org.runetale.skills.config.ItemActionsConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ItemActionsRuntimeRegistry {

    @Nullable
    private static volatile ItemActionsConfig config;
    @Nullable
    private static volatile ItemActionPlacementQueueService placementQueueService;

    private ItemActionsRuntimeRegistry() {
    }

    public static void register(@Nonnull ItemActionsConfig actionsConfig) {
        config = actionsConfig;
    }

    @Nullable
    public static ItemActionsConfig get() {
        return config;
    }

    public static void registerPlacementQueueService(@Nonnull ItemActionPlacementQueueService queueService) {
        placementQueueService = queueService;
    }

    @Nullable
    public static ItemActionPlacementQueueService getPlacementQueueService() {
        return placementQueueService;
    }

    public static void clear(@Nonnull ItemActionsConfig actionsConfig) {
        if (config == actionsConfig) {
            config = null;
        }
        placementQueueService = null;
    }
}
