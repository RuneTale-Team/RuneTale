package org.runetale.blockregeneration.service;

import org.runetale.blockregeneration.domain.BlockRegenDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BlockRegenRuntimeService {

    private final Random random;
    private final Map<BlockPositionKey, NodeState> statesByPosition = new ConcurrentHashMap<>();

    private final AtomicLong matchedInteractions = new AtomicLong();
    private final AtomicLong blockedInteractions = new AtomicLong();
    private final AtomicLong depletions = new AtomicLong();
    private final AtomicLong respawns = new AtomicLong();

    public BlockRegenRuntimeService() {
        this(new Random());
    }

    public BlockRegenRuntimeService(@Nonnull Random random) {
        this.random = random;
    }

    @Nonnull
    public GatherResult recordSuccessfulGather(
            @Nonnull String worldName,
            int x,
            int y,
            int z,
            @Nonnull BlockRegenDefinition definition,
            long nowMillis) {
        this.matchedInteractions.incrementAndGet();

        BlockPositionKey key = new BlockPositionKey(worldName, x, y, z);
        NodeState state = this.statesByPosition.computeIfAbsent(key, unused -> createActiveState(definition));

        if (state.phase == Phase.WAITING_RESPAWN) {
            this.blockedInteractions.incrementAndGet();
            return GatherResult.blockedWaiting(definition.interactedBlockId(), state.respawnDueMillis);
        }

        if (!state.definitionId.equals(definition.id())) {
            state = createActiveState(definition);
            this.statesByPosition.put(key, state);
        }

        state.currentGatherCount += 1;
        if (state.currentGatherCount >= state.currentThreshold) {
            state.phase = Phase.WAITING_RESPAWN;
            state.respawnDueMillis = nowMillis + definition.respawnDelay().sampleDelayMillis(this.random);
            state.currentGatherCount = 0;
            state.currentThreshold = definition.gatheringTrigger().sampleThreshold(this.random);
            this.depletions.incrementAndGet();
            return GatherResult.depletedToWaiting(definition.interactedBlockId(), state.respawnDueMillis);
        }

        return GatherResult.restoredSource(definition.blockIdPattern(), state.currentGatherCount, state.currentThreshold);
    }

    public boolean shouldBlockInteractionWhileWaiting(@Nonnull String worldName, int x, int y, int z) {
        BlockPositionKey key = new BlockPositionKey(worldName, x, y, z);
        NodeState state = this.statesByPosition.get(key);
        if (state == null || state.phase != Phase.WAITING_RESPAWN) {
            return false;
        }
        this.blockedInteractions.incrementAndGet();
        return true;
    }

    @Nonnull
    public List<RespawnAction> pollDueRespawns(long nowMillis) {
        List<RespawnAction> due = new ArrayList<>();
        for (Map.Entry<BlockPositionKey, NodeState> entry : this.statesByPosition.entrySet()) {
            NodeState state = entry.getValue();
            if (state.phase != Phase.WAITING_RESPAWN || state.respawnDueMillis > nowMillis) {
                continue;
            }
            BlockPositionKey key = entry.getKey();
            due.add(new RespawnAction(
                    key.worldName(),
                    key.x(),
                    key.y(),
                    key.z(),
                    state.originalBlockId,
                    state.interactedBlockId,
                    state.definitionId));
            this.statesByPosition.remove(key, state);
            this.respawns.incrementAndGet();
        }
        return due;
    }

    public void clearAll() {
        this.statesByPosition.clear();
    }

    @Nullable
    public RuntimeSnapshot inspect(@Nonnull String worldName, int x, int y, int z) {
        BlockPositionKey key = new BlockPositionKey(worldName, x, y, z);
        NodeState state = this.statesByPosition.get(key);
        if (state == null) {
            return null;
        }
        return new RuntimeSnapshot(
                state.definitionId,
                state.phase,
                state.currentGatherCount,
                state.currentThreshold,
                state.respawnDueMillis,
                state.originalBlockId,
                state.interactedBlockId);
    }

    @Nonnull
    public MetricsSnapshot metricsSnapshot() {
        return new MetricsSnapshot(
                this.matchedInteractions.get(),
                this.blockedInteractions.get(),
                this.depletions.get(),
                this.respawns.get(),
                this.statesByPosition.size());
    }

    @Nonnull
    private NodeState createActiveState(@Nonnull BlockRegenDefinition definition) {
        int threshold = definition.gatheringTrigger().sampleThreshold(this.random);
        return new NodeState(
                definition.id(),
                definition.blockIdPattern(),
                definition.interactedBlockId(),
                Phase.ACTIVE,
                0,
                threshold,
                -1L);
    }

    public record GatherResult(
            @Nonnull Action action,
            @Nonnull String blockToSet,
            long respawnDueMillis,
            int gatherCount,
            int gatherThreshold) {

        @Nonnull
        private static GatherResult blockedWaiting(@Nonnull String blockToSet, long respawnDueMillis) {
            return new GatherResult(Action.BLOCKED_WAITING, blockToSet, respawnDueMillis, 0, 0);
        }

        @Nonnull
        private static GatherResult depletedToWaiting(@Nonnull String blockToSet, long respawnDueMillis) {
            return new GatherResult(Action.DEPLETED_TO_WAITING, blockToSet, respawnDueMillis, 0, 0);
        }

        @Nonnull
        private static GatherResult restoredSource(@Nonnull String blockToSet, int gatherCount, int gatherThreshold) {
            return new GatherResult(Action.RESTORE_SOURCE, blockToSet, -1L, gatherCount, gatherThreshold);
        }
    }

    public enum Action {
        RESTORE_SOURCE,
        DEPLETED_TO_WAITING,
        BLOCKED_WAITING
    }

    public enum Phase {
        ACTIVE,
        WAITING_RESPAWN
    }

    public record RuntimeSnapshot(
            @Nonnull String definitionId,
            @Nonnull Phase phase,
            int gatherCount,
            int gatherThreshold,
            long respawnDueMillis,
            @Nonnull String originalBlockId,
            @Nonnull String interactedBlockId) {
    }

    public record MetricsSnapshot(
            long matchedInteractions,
            long blockedInteractions,
            long depletions,
            long respawns,
            int activeStates) {
    }

    public record RespawnAction(
            @Nonnull String worldName,
            int x,
            int y,
            int z,
            @Nonnull String sourceBlockId,
            @Nonnull String interactedBlockId,
            @Nonnull String definitionId) {
    }

    public record BlockPositionKey(
            @Nonnull String worldName,
            int x,
            int y,
            int z) {
    }

    private static final class NodeState {
        @Nonnull
        private final String definitionId;
        @Nonnull
        private final String originalBlockId;
        @Nonnull
        private final String interactedBlockId;
        @Nonnull
        private Phase phase;
        private int currentGatherCount;
        private int currentThreshold;
        private long respawnDueMillis;

        private NodeState(
                @Nonnull String definitionId,
                @Nonnull String originalBlockId,
                @Nonnull String interactedBlockId,
                @Nonnull Phase phase,
                int currentGatherCount,
                int currentThreshold,
                long respawnDueMillis) {
            this.definitionId = definitionId;
            this.originalBlockId = originalBlockId;
            this.interactedBlockId = interactedBlockId;
            this.phase = phase;
            this.currentGatherCount = currentGatherCount;
            this.currentThreshold = currentThreshold;
            this.respawnDueMillis = respawnDueMillis;
        }
    }
}
