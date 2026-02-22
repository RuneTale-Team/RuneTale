package org.runetale.blockregeneration.service;

import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.blockregeneration.domain.BlockRegenConfig;
import org.runetale.blockregeneration.domain.BlockRegenDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockRegenCoordinatorService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final BlockRegenConfigService configService;
    private final BlockRegenDefinitionService definitionService;
    private final BlockRegenRuntimeService runtimeService;
    private final BlockRegenPlacementQueueService placementQueueService;

    @Nonnull
    private volatile BlockRegenConfig currentConfig;

    public BlockRegenCoordinatorService(
            @Nonnull BlockRegenConfigService configService,
            @Nonnull BlockRegenDefinitionService definitionService,
            @Nonnull BlockRegenRuntimeService runtimeService,
            @Nonnull BlockRegenPlacementQueueService placementQueueService) {
        this.configService = configService;
        this.definitionService = definitionService;
        this.runtimeService = runtimeService;
        this.placementQueueService = placementQueueService;
        this.currentConfig = BlockRegenConfig.defaults();
    }

    @Nonnull
    public ReloadResult initialize() {
        return reload();
    }

    @Nonnull
    public ReloadResult reload() {
        BlockRegenConfig loaded = this.configService.load();
        this.currentConfig = loaded;
        this.definitionService.load(loaded);
        this.runtimeService.clearAll();
        this.placementQueueService.clearAll();
        LOGGER.atInfo().log("[BlockRegen] Reloaded config enabled=%s definitions=%d", loaded.enabled(), loaded.definitions().size());
        return new ReloadResult(loaded.enabled(), loaded.definitions().size());
    }

    public boolean isEnabled() {
        return this.currentConfig.enabled();
    }

    public long notifyCooldownMillis() {
        return this.currentConfig.notifyCooldownMillis();
    }

    public long respawnTickMillis() {
        return this.currentConfig.respawnTickMillis();
    }

    @Nonnull
    public HandleOutcome handleSuccessfulInteraction(
            @Nonnull String interactionKind,
            @Nonnull String worldName,
            int x,
            int y,
            int z,
            @Nonnull String blockId,
            long nowMillis) {
        if (!this.currentConfig.enabled()) {
            return HandleOutcome.notMatched();
        }

        BlockRegenDefinition definition = this.definitionService.findByBlockId(blockId);
        if (definition == null) {
            return HandleOutcome.notMatched();
        }

        BlockRegenRuntimeService.GatherResult gatherResult = this.runtimeService.recordSuccessfulGather(
                worldName,
                x,
                y,
                z,
                blockId,
                definition,
                nowMillis);
        return HandleOutcome.matched(interactionKind, definition, gatherResult);
    }

    public boolean shouldBlockWaiting(@Nonnull String worldName, int x, int y, int z) {
        return this.currentConfig.enabled() && this.runtimeService.shouldBlockInteractionWhileWaiting(worldName, x, y, z);
    }

    @Nonnull
    public List<BlockRegenRuntimeService.RespawnAction> pollDueRespawns(long nowMillis) {
        if (!this.currentConfig.enabled()) {
            return List.of();
        }
        return this.runtimeService.pollDueRespawns(nowMillis);
    }

    @Nullable
    public BlockRegenRuntimeService.RuntimeSnapshot inspectState(@Nonnull String worldName, int x, int y, int z) {
        return this.runtimeService.inspect(worldName, x, y, z);
    }

    @Nullable
    public BlockRegenDefinition findDefinition(@Nullable String blockId) {
        if (!this.currentConfig.enabled() || blockId == null || blockId.isBlank()) {
            return null;
        }
        return this.definitionService.findByBlockId(blockId);
    }

    @Nullable
    public BlockRegenDefinition findPlaceholderDefinition(@Nullable String blockId) {
        if (!this.currentConfig.enabled() || blockId == null || blockId.isBlank()) {
            return null;
        }
        return this.definitionService.findByPlaceholderBlockId(blockId);
    }

    @Nonnull
    public BlockRegenRuntimeService.MetricsSnapshot metricsSnapshot() {
        return this.runtimeService.metricsSnapshot();
    }

    public void clearRuntimeState() {
        this.runtimeService.clearAll();
        this.placementQueueService.clearAll();
    }

    public void clearRuntimeStateAt(@Nonnull String worldName, int x, int y, int z) {
        this.runtimeService.clearAt(worldName, x, y, z);
        this.placementQueueService.clearAt(worldName, x, y, z);
    }

    public void queueImmediatePlacement(@Nonnull String worldName, int x, int y, int z, @Nonnull String blockId, long nowMillis) {
        this.placementQueueService.queue(worldName, x, y, z, blockId, nowMillis + 1L);
    }

    @Nonnull
    public List<BlockRegenPlacementQueueService.PendingPlacement> pollDuePlacements(long nowMillis) {
        return this.placementQueueService.pollDue(nowMillis);
    }

    public record ReloadResult(boolean enabled, int definitionsLoaded) {
    }

    public record HandleOutcome(
            boolean matched,
            @Nonnull String interactionKind,
            @Nullable BlockRegenDefinition definition,
            @Nullable BlockRegenRuntimeService.GatherResult result) {

        @Nonnull
        public static HandleOutcome notMatched() {
            return new HandleOutcome(false, "", null, null);
        }

        @Nonnull
        public static HandleOutcome matched(
                @Nonnull String interactionKind,
                @Nonnull BlockRegenDefinition definition,
                @Nonnull BlockRegenRuntimeService.GatherResult result) {
            return new HandleOutcome(true, interactionKind, definition, result);
        }
    }
}
