package org.runetale.skills.actions;

import org.runetale.skills.config.ItemActionsConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ItemActionsRuntimeRegistry {

    @Nullable
    private static volatile ItemActionsConfig config;

    private ItemActionsRuntimeRegistry() {
    }

    public static void register(@Nonnull ItemActionsConfig actionsConfig) {
        config = actionsConfig;
    }

    @Nullable
    public static ItemActionsConfig get() {
        return config;
    }

    public static void clear(@Nonnull ItemActionsConfig actionsConfig) {
        if (config == actionsConfig) {
            config = null;
        }
    }
}
