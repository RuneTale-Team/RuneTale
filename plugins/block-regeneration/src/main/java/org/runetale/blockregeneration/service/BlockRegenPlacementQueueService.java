package org.runetale.blockregeneration.service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockRegenPlacementQueueService {

    private final Map<BlockRegenRuntimeService.BlockPositionKey, PendingPlacement> placementsByPosition = new ConcurrentHashMap<>();

    public void queue(@Nonnull String worldName, int x, int y, int z, @Nonnull String blockId, long applyAtMillis) {
        BlockRegenRuntimeService.BlockPositionKey key = new BlockRegenRuntimeService.BlockPositionKey(worldName, x, y, z);
        this.placementsByPosition.put(key, new PendingPlacement(key, blockId, applyAtMillis));
    }

    @Nonnull
    public List<PendingPlacement> pollDue(long nowMillis) {
        List<PendingPlacement> due = new ArrayList<>();
        for (Map.Entry<BlockRegenRuntimeService.BlockPositionKey, PendingPlacement> entry : this.placementsByPosition.entrySet()) {
            PendingPlacement placement = entry.getValue();
            if (placement.applyAtMillis() > nowMillis) {
                continue;
            }
            due.add(placement);
            this.placementsByPosition.remove(entry.getKey(), placement);
        }
        return due;
    }

    public void clearAt(@Nonnull String worldName, int x, int y, int z) {
        this.placementsByPosition.remove(new BlockRegenRuntimeService.BlockPositionKey(worldName, x, y, z));
    }

    public void clearAll() {
        this.placementsByPosition.clear();
    }

    public record PendingPlacement(
            @Nonnull BlockRegenRuntimeService.BlockPositionKey position,
            @Nonnull String blockId,
            long applyAtMillis) {
    }
}
