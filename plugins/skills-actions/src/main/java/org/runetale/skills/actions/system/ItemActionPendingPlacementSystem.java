package org.runetale.skills.actions.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.actions.service.ItemActionPlacementQueueService;
import org.runetale.skills.config.ItemActionsConfig;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemActionPendingPlacementSystem extends DelayedSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float POLL_INTERVAL_SECONDS = 0.05F;

    private final ItemActionPlacementQueueService placementQueueService;

    public ItemActionPendingPlacementSystem(@Nonnull ItemActionPlacementQueueService placementQueueService) {
        super(POLL_INTERVAL_SECONDS);
        this.placementQueueService = placementQueueService;
    }

    @Override
    public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
        long nowMillis = System.currentTimeMillis();
        World world = store.getExternalData().getWorld();
        List<ItemActionPlacementQueueService.PendingPlacement> due = this.placementQueueService.pollDueForWorld(
                world.getName(),
                nowMillis);
        if (due.isEmpty()) {
            return;
        }

        for (ItemActionPlacementQueueService.PendingPlacement placement : due) {
            try {
                if (!targetStillMatches(world, placement)) {
                    continue;
                }

                world.setBlock(
                        placement.position().x(),
                        placement.position().y(),
                        placement.position().z(),
                        placement.replacementBlockId());
            } catch (Exception exception) {
                LOGGER.atWarning().withCause(exception).log(
                        "[Skills Actions] Failed pending placement world=%s pos=%d,%d,%d block=%s",
                        placement.position().worldName(),
                        placement.position().x(),
                        placement.position().y(),
                        placement.position().z(),
                        placement.replacementBlockId());
            }
        }
    }

    private boolean targetStillMatches(
            @Nonnull World world,
            @Nonnull ItemActionPlacementQueueService.PendingPlacement placement) {
        String expectedCurrentBlockId = placement.expectedCurrentBlockId();
        if (expectedCurrentBlockId == null || expectedCurrentBlockId.isBlank()) {
            return true;
        }

        BlockType currentBlockType = world.getBlockType(
                placement.position().x(),
                placement.position().y(),
                placement.position().z());
        if (currentBlockType == null) {
            return false;
        }

        String actualBlockId = currentBlockType.getId();
        boolean matches = ItemActionsConfig.ItemXpActionDefinition.idsMatch(expectedCurrentBlockId, actualBlockId);
        if (!matches) {
            LOGGER.atFine().log(
                    "[Skills Actions] Skipped replacement because target block changed expected=%s actual=%s pos=%d,%d,%d",
                    expectedCurrentBlockId,
                    actualBlockId,
                    placement.position().x(),
                    placement.position().y(),
                    placement.position().z());
        }
        return matches;
    }
}
