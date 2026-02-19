package org.runetale.skills.crafting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CraftingRuntimeRegistry {

    @Nullable
    private static volatile CraftingRuntimeState runtimeState;

    private CraftingRuntimeRegistry() {
    }

    public static void register(@Nonnull CraftingRuntimeState state) {
        runtimeState = state;
    }

    public static void clear(@Nonnull CraftingRuntimeState state) {
        if (runtimeState == state) {
            runtimeState = null;
        }
    }

    @Nullable
    public static CraftingRuntimeState get() {
        return runtimeState;
    }
}
