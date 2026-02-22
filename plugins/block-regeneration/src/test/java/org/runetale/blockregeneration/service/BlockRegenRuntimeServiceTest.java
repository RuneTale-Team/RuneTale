package org.runetale.blockregeneration.service;

import org.junit.jupiter.api.Test;
import org.runetale.blockregeneration.domain.BlockRegenDefinition;
import org.runetale.blockregeneration.domain.GatheringTrigger;
import org.runetale.blockregeneration.domain.RespawnDelay;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRegenRuntimeServiceTest {

    @Test
    void specificGatheringDepletesAfterExactAmountAndRespawns() {
        BlockRegenRuntimeService service = new BlockRegenRuntimeService(new Random(42L));
        BlockRegenDefinition definition = definition(
                "oak",
                new GatheringTrigger(GatheringTrigger.Type.SPECIFIC, 2, 2, 2),
                new RespawnDelay(RespawnDelay.Type.SET, 5000L, 5000L, 5000L));

        BlockRegenRuntimeService.GatherResult first = service.recordSuccessfulGather("world", 10, 20, 30, "Tree_Oak", definition, 1000L);
        BlockRegenRuntimeService.GatherResult second = service.recordSuccessfulGather("world", 10, 20, 30, "Tree_Oak", definition, 2000L);

        assertThat(first.action()).isEqualTo(BlockRegenRuntimeService.Action.RESTORE_SOURCE);
        assertThat(second.action()).isEqualTo(BlockRegenRuntimeService.Action.DEPLETED_TO_WAITING);
        assertThat(second.respawnDueMillis()).isEqualTo(7000L);
        assertThat(service.shouldBlockInteractionWhileWaiting("world", 10, 20, 30)).isTrue();

        List<BlockRegenRuntimeService.RespawnAction> noDue = service.pollDueRespawns(6999L);
        List<BlockRegenRuntimeService.RespawnAction> due = service.pollDueRespawns(7000L);

        assertThat(noDue).isEmpty();
        assertThat(due).hasSize(1);
        assertThat(service.shouldBlockInteractionWhileWaiting("world", 10, 20, 30)).isFalse();
    }

    @Test
    void randomGatheringSamplesThresholdPerCycle() {
        BlockRegenRuntimeService service = new BlockRegenRuntimeService(new Random(4L));
        BlockRegenDefinition definition = definition(
                "iron",
                new GatheringTrigger(GatheringTrigger.Type.RANDOM, 0, 2, 3),
                new RespawnDelay(RespawnDelay.Type.SET, 1000L, 1000L, 1000L));

        BlockRegenRuntimeService.GatherResult first = service.recordSuccessfulGather("world", 1, 1, 1, "Tree_Oak", definition, 0L);
        assertThat(first.action()).isEqualTo(BlockRegenRuntimeService.Action.RESTORE_SOURCE);
        assertThat(first.gatherThreshold()).isBetween(2, 3);

        int needed = first.gatherThreshold() - first.gatherCount();
        BlockRegenRuntimeService.GatherResult result = first;
        for (int i = 0; i < needed; i++) {
            result = service.recordSuccessfulGather("world", 1, 1, 1, "Tree_Oak", definition, 0L);
        }

        assertThat(result.action()).isEqualTo(BlockRegenRuntimeService.Action.DEPLETED_TO_WAITING);
    }

    @Test
    void clearAllRemovesRuntimeState() {
        BlockRegenRuntimeService service = new BlockRegenRuntimeService(new Random(1L));
        BlockRegenDefinition definition = definition(
                "oak",
                new GatheringTrigger(GatheringTrigger.Type.SPECIFIC, 1, 1, 1),
                new RespawnDelay(RespawnDelay.Type.SET, 5000L, 5000L, 5000L));

        service.recordSuccessfulGather("world", 1, 2, 3, "Tree_Oak", definition, 10L);
        assertThat(service.inspect("world", 1, 2, 3)).isNotNull();

        service.clearAll();

        assertThat(service.inspect("world", 1, 2, 3)).isNull();
        assertThat(service.metricsSnapshot().activeStates()).isEqualTo(0);
    }

    @Test
    void clearAtRemovesSingleRuntimeStateAndPreventsRespawn() {
        BlockRegenRuntimeService service = new BlockRegenRuntimeService(new Random(2L));
        BlockRegenDefinition definition = definition(
                "oak",
                new GatheringTrigger(GatheringTrigger.Type.SPECIFIC, 1, 1, 1),
                new RespawnDelay(RespawnDelay.Type.SET, 5000L, 5000L, 5000L));

        service.recordSuccessfulGather("world", 4, 5, 6, "Tree_Oak", definition, 100L);
        assertThat(service.inspect("world", 4, 5, 6)).isNotNull();

        service.clearAt("world", 4, 5, 6);

        assertThat(service.inspect("world", 4, 5, 6)).isNull();
        assertThat(service.pollDueRespawns(10000L)).isEmpty();
    }

    private static BlockRegenDefinition definition(
            String id,
            GatheringTrigger gathering,
            RespawnDelay respawnDelay) {
        return new BlockRegenDefinition(
                id,
                true,
                "Tree_Oak",
                "Tree_Oak_Stump",
                gathering,
                respawnDelay);
    }
}
