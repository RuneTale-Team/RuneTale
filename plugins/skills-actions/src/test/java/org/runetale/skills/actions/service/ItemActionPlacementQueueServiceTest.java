package org.runetale.skills.actions.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.config.ItemActionsConfig;

import static org.assertj.core.api.Assertions.assertThat;

class ItemActionPlacementQueueServiceTest {

    @Test
    void pollDueForWorldReturnsAndRemovesOnlyDuePlacements() {
        ItemActionPlacementQueueService service = new ItemActionPlacementQueueService();
        service.queue("World_A", 10, 64, 10, "RuneTale_Log", "Furniture_Crude_Brazier", 1_000L);
        service.queue("World_A", 11, 64, 10, "RuneTale_Oak_Log", "Furniture_Crude_Brazier", 3_000L);
        service.queue("World_B", 10, 64, 10, "RuneTale_Log", "Furniture_Crude_Brazier", 1_000L);

        assertThat(service.pollDueForWorld("World_A", 2_000L))
                .hasSize(1)
                .first()
                .satisfies(placement -> {
                    assertThat(placement.position().worldName()).isEqualTo("World_A");
                    assertThat(placement.position().x()).isEqualTo(10);
                    assertThat(placement.expectedCurrentBlockId()).isEqualTo("RuneTale_Log");
                });

        assertThat(service.pollDueForWorld("World_A", 2_000L)).isEmpty();
        assertThat(service.pollDueForWorld("World_A", 4_000L)).hasSize(1);
        assertThat(service.pollDueForWorld("World_B", 2_000L)).hasSize(1);
    }

    @Test
    void queueSupportsNaturalRemoveApplyMode() {
        ItemActionPlacementQueueService service = new ItemActionPlacementQueueService();
        service.queue(
                "World_A",
                1,
                64,
                1,
                "RuneTale_Fire",
                "",
                ItemActionsConfig.BlockApplyMode.NATURAL_REMOVE,
                1_000L);

        assertThat(service.pollDueForWorld("World_A", 2_000L))
                .hasSize(1)
                .first()
                .satisfies(placement -> {
                    assertThat(placement.applyMode()).isEqualTo(ItemActionsConfig.BlockApplyMode.NATURAL_REMOVE);
                    assertThat(placement.replacementBlockId()).isBlank();
                });
    }
}
