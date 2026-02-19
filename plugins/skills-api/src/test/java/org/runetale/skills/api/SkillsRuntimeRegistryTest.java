package org.runetale.skills.api;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsRuntimeRegistryTest {

    @AfterEach
    void cleanupRegistry() {
        SkillsRuntimeApi current = SkillsRuntimeRegistry.get();
        if (current != null) {
            SkillsRuntimeRegistry.clear(current);
        }
    }

    @Test
    void registerStoresRuntimeApiReference() {
        SkillsRuntimeApi runtimeApi = new TestRuntimeApi();

        SkillsRuntimeRegistry.register(runtimeApi);

        assertThat(SkillsRuntimeRegistry.get()).isSameAs(runtimeApi);
    }

    @Test
    void clearIgnoresDifferentRuntimeReference() {
        SkillsRuntimeApi activeRuntime = new TestRuntimeApi();
        SkillsRuntimeApi otherRuntime = new TestRuntimeApi();
        SkillsRuntimeRegistry.register(activeRuntime);

        SkillsRuntimeRegistry.clear(otherRuntime);

        assertThat(SkillsRuntimeRegistry.get()).isSameAs(activeRuntime);
    }

    @Test
    void clearRemovesMatchingRuntimeReference() {
        SkillsRuntimeApi runtimeApi = new TestRuntimeApi();
        SkillsRuntimeRegistry.register(runtimeApi);

        SkillsRuntimeRegistry.clear(runtimeApi);

        assertThat(SkillsRuntimeRegistry.get()).isNull();
    }

    private static final class TestRuntimeApi implements SkillsRuntimeApi {

        @Override
        public boolean hasSkillProfile(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
            return false;
        }

        @Override
        public int getSkillLevel(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef, SkillType skillType) {
            return 1;
        }

        @Override
        public long getSkillExperience(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef, SkillType skillType) {
            return 0L;
        }

        @Override
        public boolean grantSkillXp(
                ComponentAccessor<EntityStore> accessor,
                Ref<EntityStore> playerRef,
                SkillType skillType,
                double experience,
                String source,
                boolean notifyPlayer) {
            return false;
        }

        @Override
        public int getMaxLevel() {
            return 99;
        }

        @Override
        public long xpForLevel(int level) {
            return 0L;
        }

        @Override
        public boolean isDebugEnabled(String pluginKey) {
            return false;
        }
    }
}
