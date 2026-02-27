package org.runetale.skills.actions.service;

import org.runetale.skills.config.ItemActionsConfig;

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
        queue(
                worldName,
                x,
                y,
                z,
                expectedCurrentBlockId,
                replacementBlockId,
                ItemActionsConfig.BlockApplyMode.SET_BLOCK,
                applyAtMillis);
    }

    public void queue(
            @Nonnull String worldName,
            int x,
            int y,
            int z,
            @Nullable String expectedCurrentBlockId,
            @Nullable String replacementBlockId,
            @Nonnull ItemActionsConfig.BlockApplyMode applyMode,
            long applyAtMillis) {
        BlockPositionKey key = new BlockPositionKey(worldName, x, y, z);
        this.placementsByPosition.put(
                key,
                new PendingPlacement(
                        key,
                        expectedCurrentBlockId,
                        replacementBlockId,
                        applyMode,
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
            @Nullable String replacementBlockId,
            @Nonnull ItemActionsConfig.BlockApplyMode applyMode,
            long applyAtMillis) {

        public PendingPlacement {
            applyMode = applyMode == null ? ItemActionsConfig.BlockApplyMode.SET_BLOCK : applyMode;
            replacementBlockId = replacementBlockId == null ? "" : replacementBlockId.trim();
        }
    }
}
