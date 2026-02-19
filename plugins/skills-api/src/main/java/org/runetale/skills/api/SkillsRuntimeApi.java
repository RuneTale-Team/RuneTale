package org.runetale.skills.api;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;

public interface SkillsRuntimeApi {

    boolean hasSkillProfile(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> playerRef);

    int getSkillLevel(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull SkillType skillType);

    long getSkillExperience(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull SkillType skillType);

    boolean grantSkillXp(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull SkillType skillType,
            double experience,
            @Nonnull String source,
            boolean notifyPlayer);

    int getMaxLevel();

    long xpForLevel(int level);

    boolean isDebugEnabled(@Nonnull String pluginKey);
}
