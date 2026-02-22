package org.runetale.blockregeneration.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRegenPlacementQueueServiceTest {

    @Test
    void pollDueReturnsOnlyExpiredPlacements() {
        BlockRegenPlacementQueueService service = new BlockRegenPlacementQueueService();
        service.queue("world", 1, 2, 3, "Tree_Oak", 100L);
        service.queue("world", 4, 5, 6, "Tree_Oak_Stump", 200L);

        assertThat(service.pollDueForWorld("world", 99L)).isEmpty();
        assertThat(service.pollDueForWorld("world", 100L)).hasSize(1);
        assertThat(service.pollDueForWorld("world", 150L)).isEmpty();
        assertThat(service.pollDueForWorld("world", 200L)).hasSize(1);
    }

    @Test
    void clearAtRemovesPendingPlacement() {
        BlockRegenPlacementQueueService service = new BlockRegenPlacementQueueService();
        service.queue("world", 1, 2, 3, "Tree_Oak", 100L);

        service.clearAt("world", 1, 2, 3);

        assertThat(service.pollDueForWorld("world", 1000L)).isEmpty();
    }

    @Test
    void pollDueForWorldDoesNotConsumeOtherWorldPlacements() {
        BlockRegenPlacementQueueService service = new BlockRegenPlacementQueueService();
        service.queue("world-a", 1, 2, 3, "Tree_Oak", 100L);
        service.queue("world-b", 1, 2, 3, "Tree_Oak", 100L);

        assertThat(service.pollDueForWorld("world-a", 100L)).hasSize(1);
        assertThat(service.pollDueForWorld("world-a", 100L)).isEmpty();
        assertThat(service.pollDueForWorld("world-b", 100L)).hasSize(1);
    }
}
