package org.runetale.blockregeneration.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRegenPlacementQueueServiceTest {

    @Test
    void pollDueReturnsOnlyExpiredPlacements() {
        BlockRegenPlacementQueueService service = new BlockRegenPlacementQueueService();
        service.queue("world", 1, 2, 3, "Tree_Oak", 100L);
        service.queue("world", 4, 5, 6, "Tree_Oak_Stump", 200L);

        assertThat(service.pollDue(99L)).isEmpty();
        assertThat(service.pollDue(100L)).hasSize(1);
        assertThat(service.pollDue(150L)).isEmpty();
        assertThat(service.pollDue(200L)).hasSize(1);
    }

    @Test
    void clearAtRemovesPendingPlacement() {
        BlockRegenPlacementQueueService service = new BlockRegenPlacementQueueService();
        service.queue("world", 1, 2, 3, "Tree_Oak", 100L);

        service.clearAt("world", 1, 2, 3);

        assertThat(service.pollDue(1000L)).isEmpty();
    }
}
