package org.runetale.skills.crafting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.runetale.skills.config.CraftingConfig;
import org.runetale.skills.service.CraftingPageTrackerService;
import org.runetale.skills.service.CraftingRecipeTagService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CraftingRuntimeRegistryTest {

    @AfterEach
    void cleanupRegistry() {
        CraftingRuntimeState current = CraftingRuntimeRegistry.get();
        if (current != null) {
            CraftingRuntimeRegistry.clear(current);
        }
    }

    @Test
    void registerStoresCraftingRuntimeState() {
        CraftingRuntimeState runtimeState = runtimeState();

        CraftingRuntimeRegistry.register(runtimeState);

        assertThat(CraftingRuntimeRegistry.get()).isSameAs(runtimeState);
    }

    @Test
    void clearIgnoresDifferentRuntimeState() {
        CraftingRuntimeState activeState = runtimeState();
        CraftingRuntimeState otherState = runtimeState();
        CraftingRuntimeRegistry.register(activeState);

        CraftingRuntimeRegistry.clear(otherState);

        assertThat(CraftingRuntimeRegistry.get()).isSameAs(activeState);
    }

    @Test
    void clearRemovesMatchingRuntimeState() {
        CraftingRuntimeState runtimeState = runtimeState();
        CraftingRuntimeRegistry.register(runtimeState);

        CraftingRuntimeRegistry.clear(runtimeState);

        assertThat(CraftingRuntimeRegistry.get()).isNull();
    }

    private static CraftingRuntimeState runtimeState() {
        return new CraftingRuntimeState(
                mock(CraftingConfig.class),
                mock(CraftingRecipeTagService.class),
                mock(CraftingPageTrackerService.class));
    }
}
