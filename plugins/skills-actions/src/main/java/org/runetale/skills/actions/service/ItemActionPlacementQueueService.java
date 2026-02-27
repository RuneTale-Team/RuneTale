package org.runetale.skills.actions.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemActionPlacementQueueService {

    private final Map<BlockPositionKey, PendingPlacement> placementsByPosition = new ConcurrentHashMap<>();

    public void queue(
            @Nonnull String worldName,
            int x,
            int y,
            int z,
            @Nullable String expectedCurrentBlockId,
            @Nonnull String replacementBlockId,
            long applyAtMillis) {
        BlockPositionKey key = new BlockPositionKey(worldName, x, y, z);
        this.placementsByPosition.put(
                key,
                new PendingPlacement(
                        key,
                        expectedCurrentBlockId,
                        replacementBlockId,
                        applyAtMillis));
    }

    @Nonnull
    public List<PendingPlacement> pollDueForWorld(@Nonnull String worldName, long nowMillis) {
        List<PendingPlacement> due = new ArrayList<>();
        for (Map.Entry<BlockPositionKey, PendingPlacement> entry : this.placementsByPosition.entrySet()) {
            PendingPlacement placement = entry.getValue();
            if (placement.applyAtMillis() > nowMillis || !placement.position().worldName().equals(worldName)) {
                continue;
            }
            due.add(placement);
            this.placementsByPosition.remove(entry.getKey(), placement);
        }
        return due;
    }

    public void clearAll() {
        this.placementsByPosition.clear();
    }

    public record BlockPositionKey(
            @Nonnull String worldName,
            int x,
            int y,
            int z) {
    }

    public record PendingPlacement(
            @Nonnull BlockPositionKey position,
            @Nullable String expectedCurrentBlockId,
            @Nonnull String replacementBlockId,
            long applyAtMillis) {
    }
}
