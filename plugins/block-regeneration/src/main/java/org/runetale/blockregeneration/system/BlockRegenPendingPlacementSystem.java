package org.runetale.blockregeneration.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.blockregeneration.service.BlockRegenCoordinatorService;
import org.runetale.blockregeneration.service.BlockRegenPlacementQueueService;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockRegenPendingPlacementSystem extends DelayedSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float POLL_INTERVAL_SECONDS = 0.05F;

    private final BlockRegenCoordinatorService coordinatorService;

    public BlockRegenPendingPlacementSystem(@Nonnull BlockRegenCoordinatorService coordinatorService) {
        super(POLL_INTERVAL_SECONDS);
        this.coordinatorService = coordinatorService;
    }

    @Override
    public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
        long nowMillis = System.currentTimeMillis();
        World world = store.getExternalData().getWorld();
        List<BlockRegenPlacementQueueService.PendingPlacement> due = this.coordinatorService.pollDuePlacements(
                world.getName(),
                nowMillis);
        if (due.isEmpty()) {
            return;
        }
        for (BlockRegenPlacementQueueService.PendingPlacement placement : due) {
            try {
                world.execute(() -> world.setBlock(
                        placement.position().x(),
                        placement.position().y(),
                        placement.position().z(),
                        placement.blockId()));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log(
                        "[BlockRegen] Failed pending placement world=%s pos=%d,%d,%d block=%s",
                        placement.position().worldName(),
                        placement.position().x(),
                        placement.position().y(),
                        placement.position().z(),
                        placement.blockId());
            }
        }
    }
}
