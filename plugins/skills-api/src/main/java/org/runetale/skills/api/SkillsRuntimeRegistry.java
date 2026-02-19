package org.runetale.skills.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SkillsRuntimeRegistry {

    @Nullable
    private static volatile SkillsRuntimeApi runtimeApi;

    private SkillsRuntimeRegistry() {
    }

    public static void register(@Nonnull SkillsRuntimeApi runtime) {
        runtimeApi = runtime;
    }

    public static void clear(@Nonnull SkillsRuntimeApi runtime) {
        if (runtimeApi == runtime) {
            runtimeApi = null;
        }
    }

    @Nullable
    public static SkillsRuntimeApi get() {
        return runtimeApi;
    }
}
